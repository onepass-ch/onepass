package ch.onepass.onepass.model.event

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for event operations.
 */
interface EventRepository {
    fun getAllEvents(): Flow<List<Event>>
    fun searchEvents(query: String): Flow<List<Event>>
    fun getEventById(eventId: String): Flow<Event?>
    fun getEventsByOrganization(orgId: String): Flow<List<Event>>
    fun getEventsByLocation(lat: Double, lng: Double, radius: Double): Flow<List<Event>>
    fun getEventsByTag(tag: String): Flow<List<Event>>
    fun getFeaturedEvents(): Flow<List<Event>>
    fun getEventsByStatus(status: EventStatus): Flow<List<Event>>

    suspend fun createEvent(event: Event): Result<String>
    suspend fun updateEvent(event: Event): Result<Unit>
    suspend fun deleteEvent(eventId: String): Result<Unit>
}
