package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.utils.EventTestData
import com.google.firebase.firestore.GeoPoint
import java.util.UUID
import kotlin.math.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeEventRepository(initialEvents: List<Event> = emptyList()) : EventRepository {

  private val eventsFlow = MutableStateFlow(initialEvents.toList())
  private var throwOnLoad = false

  fun setEvents(events: List<Event>) {
    eventsFlow.value = events.toList()
  }

  fun addEvent(event: Event) {
    eventsFlow.value = eventsFlow.value + event
  }

  fun reset() {
    eventsFlow.value = emptyList()
    throwOnLoad = false
  }

  fun setThrowOnLoad(value: Boolean) {
    throwOnLoad = value
  }

  override fun getAllEvents(): Flow<List<Event>> =
      eventsFlow.map { list -> list.sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE } }

  override fun searchEvents(query: String): Flow<List<Event>> {
    val lower = query.lowercase()
    return eventsFlow.map { list ->
      list
          .filter { it.title.lowercase().startsWith(lower) }
          .sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE }
    }
  }

  override fun getEventById(eventId: String): Flow<Event?> =
      eventsFlow.map { list -> list.find { it.eventId == eventId } }

  override fun getEventsByOrganization(orgId: String): Flow<List<Event>> =
      eventsFlow.map { list ->
        list.filter { it.organizerId == orgId }.sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE }
      }

  override fun getEventsByLocation(center: Location, radiusKm: Double): Flow<List<Event>> =
      eventsFlow.map { list ->
        val centerGeo = center.coordinates
        if (centerGeo == null) return@map emptyList<Event>()
        list
            .filter { ev ->
              val evGeo = ev.location?.coordinates ?: return@filter false
              distanceKm(centerGeo, evGeo) <= radiusKm
            }
            .sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE }
      }

  override fun getEventsByTag(tag: String): Flow<List<Event>> =
      eventsFlow.map { list -> list.filter { it.tags.contains(tag) } }

  override fun getFeaturedEvents(): Flow<List<Event>> =
      eventsFlow.map { list ->
        list
            .filter { it.status == EventStatus.PUBLISHED }
            .sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE }
            .take(3)
      }

  override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> =
      eventsFlow.map { list ->
        list.filter { it.status == status }.sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE }
      }

  override suspend fun createEvent(event: Event): Result<String> {
    if (throwOnLoad) return Result.failure(RuntimeException("boom"))
    val id = UUID.randomUUID().toString()
    val toStore = event.copy(eventId = id)
    eventsFlow.value = eventsFlow.value + toStore
    return Result.success(id)
  }

  override suspend fun updateEvent(event: Event): Result<Unit> {
    if (throwOnLoad) return Result.failure(RuntimeException("boom"))
    val found = eventsFlow.value.any { it.eventId == event.eventId }
    return if (!found) {
      Result.failure(IllegalArgumentException("event not found: ${event.eventId}"))
    } else {
      eventsFlow.value = eventsFlow.value.map { if (it.eventId == event.eventId) event else it }
      Result.success(Unit)
    }
  }

  override suspend fun deleteEvent(eventId: String): Result<Unit> {
    if (throwOnLoad) return Result.failure(RuntimeException("boom"))
    eventsFlow.value = eventsFlow.value.filterNot { it.eventId == eventId }
    return Result.success(Unit)
  }

  override suspend fun addEventImage(eventId: String, imageUrl: String): Result<Unit> {
    if (throwOnLoad) return Result.failure(RuntimeException("boom"))
    val event =
        eventsFlow.value.find { it.eventId == eventId }
            ?: return Result.failure(IllegalArgumentException("event not found: $eventId"))
    val updatedEvent = event.copy(images = event.images + imageUrl)
    eventsFlow.value = eventsFlow.value.map { if (it.eventId == eventId) updatedEvent else it }
    return Result.success(Unit)
  }

  override suspend fun removeEventImage(eventId: String, imageUrl: String): Result<Unit> {
    if (throwOnLoad) return Result.failure(RuntimeException("boom"))
    val event =
        eventsFlow.value.find { it.eventId == eventId }
            ?: return Result.failure(IllegalArgumentException("event not found: $eventId"))
    val updatedEvent = event.copy(images = event.images.filterNot { it == imageUrl })
    eventsFlow.value = eventsFlow.value.map { if (it.eventId == eventId) updatedEvent else it }
    return Result.success(Unit)
  }

  override suspend fun updateEventImages(eventId: String, imageUrls: List<String>): Result<Unit> {
    if (throwOnLoad) return Result.failure(RuntimeException("boom"))
    val event =
        eventsFlow.value.find { it.eventId == eventId }
            ?: return Result.failure(IllegalArgumentException("event not found: $eventId"))
    val updatedEvent = event.copy(images = imageUrls)
    eventsFlow.value = eventsFlow.value.map { if (it.eventId == eventId) updatedEvent else it }
    return Result.success(Unit)
  }

  // --- Utility: haversine distance between two GeoPoints (km) ---
  private fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
    val lat1 = Math.toRadians(a.latitude)
    val lon1 = Math.toRadians(a.longitude)
    val lat2 = Math.toRadians(b.latitude)
    val lon2 = Math.toRadians(b.longitude)
    val dLat = lat2 - lat1
    val dLon = lon2 - lon1
    val r = 6371.0 // Earth radius km
    val hav = sin(dLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
    return 2 * r * asin(sqrt(hav))
  }

  companion object {
    /** Convenience factory seeded with EventTestData for quick test setups. */
    fun withSampleEvents(
        count: Int = 5,
        organizerId: String = "test-organizer"
    ): FakeEventRepository {
      val events = EventTestData.createTestEvents(count, organizerId = organizerId)
      return FakeEventRepository(events)
    }
  }
}
