package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.listeners.GeoQueryDataEventListener

/** Firestore-backed implementation of [EventRepository]. */
class EventRepositoryFirebase : EventRepository {

  private val eventsCollection = Firebase.firestore.collection("events")
  private val geoFirestore = GeoFirestore(eventsCollection)

  companion object {
    /** Maximum number of featured events to return */
    private const val FEATURED_EVENTS_LIMIT = 3L
  }

  override fun getAllEvents(): Flow<List<Event>> = snapshotFlow {
    eventsCollection.orderBy("startTime", Query.Direction.ASCENDING)
  }

  override fun getEventById(eventId: String): Flow<Event?> = callbackFlow {
    val listener =
        eventsCollection.document(eventId).addSnapshotListener { snap, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          trySend(snap?.toObject(Event::class.java))
        }
    awaitClose { listener.remove() }
  }

  override fun getEventsByOrganization(orgId: String): Flow<List<Event>> = snapshotFlow {
    eventsCollection
        .whereEqualTo("organizerId", orgId)
        .orderBy("startTime", Query.Direction.ASCENDING)
  }

  override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> = snapshotFlow {
    eventsCollection
        .whereEqualTo("status", status.name)
        .orderBy("startTime", Query.Direction.ASCENDING)
  }

  override fun getFeaturedEvents(): Flow<List<Event>> = snapshotFlow {
    eventsCollection
        .whereEqualTo("status", EventStatus.PUBLISHED.name)
        .orderBy("startTime", Query.Direction.ASCENDING)
        .limit(FEATURED_EVENTS_LIMIT)
  }

  override fun getEventsByTag(tag: String): Flow<List<Event>> = snapshotFlow {
    eventsCollection.whereArrayContains("tags", tag)
  }

  override fun searchEvents(query: String): Flow<List<Event>> = snapshotFlow {
    val lowerQuery = query.lowercase()
    eventsCollection.orderBy("titleLower").startAt(lowerQuery).endAt("$lowerQuery\uFFFF")
  }

  override fun getEventsByLocation(center: Location, radiusKm: Double): Flow<List<Event>> =
      callbackFlow {
        val centerPoint =
            center.coordinates
                ?: run {
                  trySend(emptyList())
                  close()
                  return@callbackFlow
                }
        val geoQuery = geoFirestore.queryAtLocation(centerPoint, radiusKm)

        val nearbyEvents = mutableMapOf<String, Event>()
        var isQueryReady = false

        val listener =
            object : GeoQueryDataEventListener {
              override fun onDocumentEntered(
                  documentSnapshot: DocumentSnapshot,
                  location: GeoPoint
              ) {
                val event = documentSnapshot.toObject(Event::class.java) ?: return
                nearbyEvents[documentSnapshot.id] = event
                // Only send updates if the initial query is already finished
                if (isQueryReady) {
                  trySend(nearbyEvents.values.sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE })
                }
              }

              override fun onDocumentExited(documentSnapshot: DocumentSnapshot) {
                nearbyEvents.remove(documentSnapshot.id)
                if (isQueryReady) {
                  trySend(nearbyEvents.values.sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE })
                }
              }

              override fun onDocumentMoved(documentSnapshot: DocumentSnapshot, location: GeoPoint) {
                val event = documentSnapshot.toObject(Event::class.java) ?: return
                nearbyEvents[documentSnapshot.id] = event
                if (isQueryReady) {
                  trySend(nearbyEvents.values.sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE })
                }
              }

              override fun onDocumentChanged(
                  documentSnapshot: DocumentSnapshot,
                  location: GeoPoint
              ) {
                val event = documentSnapshot.toObject(Event::class.java) ?: return
                nearbyEvents[documentSnapshot.id] = event
                if (isQueryReady) {
                  trySend(nearbyEvents.values.sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE })
                }
              }

              override fun onGeoQueryReady() {
                isQueryReady = true
                trySend(nearbyEvents.values.sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE })
              }

              override fun onGeoQueryError(exception: Exception) {
                close(exception)
              }
            }

        geoQuery.addGeoQueryDataEventListener(listener)
        awaitClose { geoQuery.removeAllListeners() }
      }

  override suspend fun createEvent(event: Event): Result<String> = runCatching {
    val docRef = eventsCollection.document()
    val eventWithMetadata =
        event.copy(eventId = docRef.id, createdAt = Timestamp.now(), updatedAt = Timestamp.now())
    docRef.set(eventWithMetadata).await()
    event.location?.coordinates?.let { geoFirestore.setLocation(docRef.id, it) }
    docRef.id
  }

  override suspend fun updateEvent(event: Event): Result<Unit> = runCatching {
    val updated = event.copy(updatedAt = Timestamp.now())
    eventsCollection.document(event.eventId).set(updated).await()
    event.location?.coordinates?.let { geoFirestore.setLocation(event.eventId, it) }
  }

  override suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
    eventsCollection.document(eventId).delete().await()
    geoFirestore.removeLocation(eventId)
  }

  private fun snapshotFlow(queryBuilder: () -> Query): Flow<List<Event>> = callbackFlow {
    val query = queryBuilder()
    val listener =
        query.addSnapshotListener { snap, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          val list = snap?.documents?.mapNotNull { it.toObject(Event::class.java) } ?: emptyList()
          trySend(list)
        }
    awaitClose { listener.remove() }
  }
}
