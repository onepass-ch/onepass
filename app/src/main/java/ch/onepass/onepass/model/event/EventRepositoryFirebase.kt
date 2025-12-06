package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.map.Location
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
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

  /**
   * Retrieves all events, sorted by start time in ascending order.
   *
   * @return A [Flow] emitting a list of all events.
   */
  override fun getAllEvents(): Flow<List<Event>> = snapshotFlow {
    eventsCollection.orderBy("startTime", Query.Direction.ASCENDING)
  }

  /**
   * Retrieves a specific event by its unique ID.
   *
   * @param eventId The unique identifier of the event.
   * @return A [Flow] emitting the event or null if not found.
   */
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

  /**
   * Retrieves all events organized by a specific organization/user.
   *
   * @param orgId The organizer's unique ID.
   * @return A [Flow] emitting a list of events by that organizer.
   */
  override fun getEventsByOrganization(orgId: String): Flow<List<Event>> = snapshotFlow {
    eventsCollection
        .whereEqualTo("organizerId", orgId)
        .orderBy("startTime", Query.Direction.ASCENDING)
  }

  /**
   * Retrieves all events with a specific status.
   *
   * @param status The [EventStatus] to filter by.
   * @return A [Flow] emitting a list of events with the given status.
   */
  override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> = snapshotFlow {
    eventsCollection
        .whereEqualTo("status", status.name)
        .orderBy("startTime", Query.Direction.ASCENDING)
  }

  /**
   * Retrieves a curated list of featured events (published and upcoming, limited to
   * [FEATURED_EVENTS_LIMIT]).
   *
   * @return A [Flow] emitting a limited list of featured events.
   */
  override fun getFeaturedEvents(): Flow<List<Event>> = snapshotFlow {
    eventsCollection
        .whereEqualTo("status", EventStatus.PUBLISHED.name)
        .orderBy("startTime", Query.Direction.ASCENDING)
        .limit(FEATURED_EVENTS_LIMIT)
  }

  /**
   * Retrieves all events associated with a specific tag.
   *
   * @param tag The tag to filter by.
   * @return A [Flow] emitting a list of events containing the tag.
   */
  override fun getEventsByTag(tag: String): Flow<List<Event>> = snapshotFlow {
    eventsCollection.whereArrayContains("tags", tag)
  }

  /**
   * Searches for events whose titles match the given query (case-insensitive prefix match using
   * [titleLower]).
   *
   * @param query The search term.
   * @return A [Flow] emitting a list of matching events.
   */
  override fun searchEvents(query: String): Flow<List<Event>> = snapshotFlow {
    val lowerQuery = query.lowercase()
    eventsCollection.orderBy("titleLower").startAt(lowerQuery).endAt("$lowerQuery\uFFFF")
  }

  /**
   * Retrieves events within a geographic radius of a given location using GeoFirestore.
   *
   * @param center The center [Location] for the search.
   * @param radiusKm Search radius in kilometers.
   * @return A [Flow] emitting a list of nearby events, sorted by start time.
   */
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

  /**
   * Creates a new event in Firestore.
   *
   * Automatically assigns a document ID, sets [createdAt] and [updatedAt], and stores location in
   * GeoFirestore.
   *
   * @param event The [Event] to create (eventId will be overwritten).
   * @return A [Result] containing the newly created event's ID on success, or an error.
   */
  override suspend fun createEvent(event: Event): Result<String> = runCatching {
    val docRef = eventsCollection.document()
    val eventWithMetadata = event.copy(eventId = docRef.id, createdAt = null, updatedAt = null)
    docRef.set(eventWithMetadata).await()
    event.location?.coordinates?.let { geoFirestore.setLocation(docRef.id, it) }
    docRef.id
  }

  /**
   * Updates an existing event in Firestore.
   *
   * Updates [updatedAt] and syncs location to GeoFirestore if present.
   *
   * @param event The updated [Event] (must include valid [eventId]).
   * @return A [Result] indicating success or failure.
   */
  override suspend fun updateEvent(event: Event): Result<Unit> = runCatching {
    val updated = event.copy(updatedAt = null)
    eventsCollection.document(event.eventId).set(updated).await()
    event.location?.coordinates?.let { geoFirestore.setLocation(event.eventId, it) }
  }

  /**
   * Deletes an event from Firestore and removes its location from GeoFirestore.
   *
   * @param eventId The unique identifier of the event to delete.
   * @return A [Result] indicating success or failure.
   */
  override suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
    eventsCollection.document(eventId).delete().await()
    geoFirestore.removeLocation(eventId)
  }

  /**
   * Adds an image URL to an event's images list using Firestore array union.
   *
   * @param eventId The event's unique ID.
   * @param imageUrl The image URL to add.
   * @return A [Result] indicating success or failure.
   */
  override suspend fun addEventImage(eventId: String, imageUrl: String): Result<Unit> =
      runCatching {
        eventsCollection
            .document(eventId)
            .update(
                mapOf(
                    "images" to FieldValue.arrayUnion(imageUrl),
                    "updatedAt" to FieldValue.serverTimestamp()))
            .await()
      }

  /**
   * Removes an image URL from an event's images list using Firestore array remove.
   *
   * @param eventId The event's unique ID.
   * @param imageUrl The image URL to remove.
   * @return A [Result] indicating success or failure.
   */
  override suspend fun removeEventImage(eventId: String, imageUrl: String): Result<Unit> =
      runCatching {
        eventsCollection
            .document(eventId)
            .update(
                mapOf(
                    "images" to FieldValue.arrayRemove(imageUrl),
                    "updatedAt" to FieldValue.serverTimestamp()))
            .await()
      }

  /**
   * Updates the entire images list for an event.
   *
   * @param eventId The event's unique ID.
   * @param imageUrls The new list of image URLs.
   * @return A [Result] indicating success or failure.
   */
  override suspend fun updateEventImages(eventId: String, imageUrls: List<String>): Result<Unit> =
      runCatching {
        eventsCollection
            .document(eventId)
            .update(mapOf("images" to imageUrls, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
      }

  /**
   * Helper function to create a [Flow] from a Firestore query using a snapshot listener.
   *
   * @param queryBuilder Lambda that returns a configured [Query].
   * @return A [Flow] emitting a list of [Event] objects.
   */
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
