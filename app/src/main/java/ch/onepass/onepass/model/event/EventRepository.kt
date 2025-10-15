package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.map.Location
import kotlinx.coroutines.flow.Flow

/** Repository interface defining operations for managing events. */
interface EventRepository {
  /**
   * Retrieves all events, sorted by start time in ascending order.
   *
   * @return A [Flow] emitting a list of all events.
   */
  fun getAllEvents(): Flow<List<Event>>

  /**
   * Searches for events whose titles match the given query (case-insensitive prefix match).
   *
   * @param query The search term.
   * @return A [Flow] emitting a list of matching events.
   */
  fun searchEvents(query: String): Flow<List<Event>>

  /**
   * Retrieves a specific event by its unique ID.
   *
   * @param eventId The unique identifier of the event.
   * @return A [Flow] emitting the event or null if not found.
   */
  fun getEventById(eventId: String): Flow<Event?>

  /**
   * Retrieves all events organized by a specific organization/user.
   *
   * @param orgId The organizer's unique ID.
   * @return A [Flow] emitting a list of events by that organizer.
   */
  fun getEventsByOrganization(orgId: String): Flow<List<Event>>

  /**
   * Retrieves events within a geographic radius of a given location.
   *
   * @param center The center [Location] for the search.
   * @param radiusKm Search radius in kilometers.
   * @return A [Flow] emitting a list of nearby events.
   */
  fun getEventsByLocation(center: Location, radiusKm: Double): Flow<List<Event>>

  /**
   * Retrieves all events associated with a specific tag.
   *
   * @param tag The tag to filter by.
   * @return A [Flow] emitting a list of events containing the tag.
   */
  fun getEventsByTag(tag: String): Flow<List<Event>>

  /**
   * Retrieves a curated list of featured events (typically published and upcoming).
   *
   * @return A [Flow] emitting a limited list of featured events.
   */
  fun getFeaturedEvents(): Flow<List<Event>>

  /**
   * Retrieves all events with a specific status.
   *
   * @param status The [EventStatus] to filter by.
   * @return A [Flow] emitting a list of events with the given status.
   */
  fun getEventsByStatus(status: EventStatus): Flow<List<Event>>

  /**
   * Creates a new event in the repository.
   *
   * @param event The [Event] to create.
   * @return A [Result] containing the newly created event's ID on success, or an error.
   */
  suspend fun createEvent(event: Event): Result<String>

  /**
   * Updates an existing event.
   *
   * @param event The updated [Event] (must include valid [eventId]).
   * @return A [Result] indicating success or failure.
   */
  suspend fun updateEvent(event: Event): Result<Unit>

  /**
   * Deletes an event by its ID (soft or hard delete depending on implementation).
   *
   * @param eventId The unique identifier of the event to delete.
   * @return A [Result] indicating success or failure.
   */
  suspend fun deleteEvent(eventId: String): Result<Unit>
}
