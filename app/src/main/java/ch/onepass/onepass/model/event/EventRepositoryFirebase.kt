package ch.onepass.onepass.model.event

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Firestore-backed implementation of [EventRepository]. */
class EventRepositoryFirebase : EventRepository {

  private val eventsCollection = Firebase.firestore.collection("events")

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
    // No dedicated featured flag yet — simulate by taking the 3 soonest events
    eventsCollection.orderBy("startTime", Query.Direction.ASCENDING).limit(3)
  }

  override fun getEventsByTag(tag: String): Flow<List<Event>> = snapshotFlow {
    eventsCollection.whereArrayContains("tags", tag)
  }

  override fun searchEvents(query: String): Flow<List<Event>> = snapshotFlow {
    // Firestore doesn't support full text search natively.
    // This implements a simple prefix-based search on title.
    eventsCollection.orderBy("title").startAt(query).endAt("$query\uf8ff")
  }

  override fun getEventsByLocation(lat: Double, lng: Double, radius: Double): Flow<List<Event>> =
      snapshotFlow {
        // Approximate bounding-box query on latitude
        val latDelta = radius / 111.0
        val lowerLat = lat - latDelta
        val upperLat = lat + latDelta

        eventsCollection
            .whereGreaterThanOrEqualTo("location.coordinates._latitude", lowerLat)
            .whereLessThanOrEqualTo("location.coordinates._latitude", upperLat)
      }

  override suspend fun createEvent(event: Event): Result<String> = runCatching {
    val docRef = eventsCollection.document()
    val eventWithMetadata =
        event.copy(eventId = docRef.id, createdAt = Timestamp.now(), updatedAt = Timestamp.now())
    docRef.set(eventWithMetadata).await()
    docRef.id
  }

  override suspend fun updateEvent(event: Event): Result<Unit> = runCatching {
    val updated = event.copy(updatedAt = Timestamp.now())
    eventsCollection.document(event.eventId).set(updated).await()
  }

  override suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
    eventsCollection.document(eventId).delete().await()
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
