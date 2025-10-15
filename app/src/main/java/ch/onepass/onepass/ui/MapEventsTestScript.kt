package ch.onepass.onepass.ui

import android.util.Log
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.repository.RepositoryProvider
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar
import kotlin.collections.forEach
import kotlinx.coroutines.runBlocking

/** Quick script to populate the map with test events for manual testing. */
object MapEventsTestScript {

  /**
   * Populates the map with a variety of test events in different locations. This is useful for
   * manual testing of map pins and event cards.
   */
  private const val TAG = "MapEventsTestScript"

  fun populateTestEvents() {
    Log.d(TAG, "🚀 Starting to populate test events...")

    runBlocking {
      try {
        val repository = RepositoryProvider.eventRepository
        Log.d(TAG, "📡 Repository initialized: ${repository::class.simpleName}")

        val testEvents = createTestEvents()
        Log.d(TAG, "📝 Created ${testEvents.size} test events")

        var successCount = 0
        testEvents.forEach { event ->
          val result = repository.createEvent(event)
          if (result.isSuccess) {
            successCount++
            Log.d(
                TAG,
                "✅ Added event: '${event.title}' at ${event.location?.name} (ID: ${result.getOrNull()})",
            )
          } else {
            Log.e(
                TAG,
                "❌ Failed to add event: '${event.title}' - ${result.exceptionOrNull()?.message}",
            )
          }
        }

        Log.d(TAG, "🎉 Successfully added $successCount/${testEvents.size} test events!")
      } catch (e: Exception) {
        Log.e(TAG, "💥 Error populating test events: ${e.message}", e)
      }
    }
  }

  /** Clears all test events (use with caution in production!) */
  fun clearTestEvents() {
    runBlocking {
      try {
        println("⚠️  Clear functionality needs to be implemented based on your repository")
      } catch (e: Exception) {
        println("❌ Error clearing test events: ${e.message}")
      }
    }
  }

  private fun createTestEvents(): List<Event> {
    val calendar = Calendar.getInstance()

    return listOf(
        // Lausanne Events
        Event(
            eventId = "test-lausanne-tech",
            title = "Lausanne Tech Meetup",
            description = "Monthly technology meetup at EPFL",
            organizerId = "test-organizer",
            organizerName = "Lausanne Tech Community",
            status = EventStatus.PUBLISHED,
            location =
                Location(
                    coordinates = GeoPoint(46.5191, 6.5668),
                    name = "EPFL, Lausanne",
                    region = "Vaud",
                ),
            startTime = createFutureTimestamp(daysFromNow = 7, hour = 18),
            endTime = createFutureTimestamp(daysFromNow = 7, hour = 21),
            capacity = 100,
            ticketsRemaining = 75,
            pricingTiers = listOf(PricingTier("General", 0.0, 100, 75)),
            tags = listOf("tech", "networking", "epfl"),
        ),
        Event(
            eventId = "test-lausanne-music",
            title = "Lausanne Summer Festival",
            description = "Open air music festival by the lake",
            organizerId = "test-organizer",
            organizerName = "Lausanne Events",
            status = EventStatus.PUBLISHED,
            location =
                Location(
                    coordinates = GeoPoint(46.5076, 6.6323),
                    name = "Lakeside Park, Lausanne",
                    region = "Vaud",
                ),
            startTime = createFutureTimestamp(daysFromNow = 14, hour = 16),
            endTime = createFutureTimestamp(daysFromNow = 14, hour = 23),
            capacity = 500,
            ticketsRemaining = 250,
            pricingTiers =
                listOf(
                    PricingTier("Early Bird", 25.0, 100, 50),
                    PricingTier("General", 35.0, 400, 200),
                ),
            tags = listOf("music", "festival", "summer"),
        ),

        // Geneva Events
        Event(
            eventId = "test-geneva-art",
            title = "Geneva Art Exhibition",
            description = "Contemporary art exhibition",
            organizerId = "test-organizer",
            organizerName = "Geneva Art Museum",
            status = EventStatus.PUBLISHED,
            location =
                Location(
                    coordinates = GeoPoint(46.2022, 6.1457),
                    name = "Geneva Art Center",
                    region = "Geneva",
                ),
            startTime = createFutureTimestamp(daysFromNow = 3, hour = 10),
            endTime = createFutureTimestamp(daysFromNow = 30, hour = 18),
            capacity = 200,
            ticketsRemaining = 150,
            pricingTiers = listOf(PricingTier("Admission", 15.0, 200, 150)),
            tags = listOf("art", "exhibition", "culture"),
        ),

        // Zurich Events
        Event(
            eventId = "test-zurich-food",
            title = "Zurich Food Festival",
            description = "Taste the best of Swiss cuisine",
            organizerId = "test-organizer",
            organizerName = "Zurich Food Association",
            status = EventStatus.PUBLISHED,
            location =
                Location(
                    coordinates = GeoPoint(47.3769, 8.5417),
                    name = "Zurich Main Station",
                    region = "Zurich",
                ),
            startTime = createFutureTimestamp(daysFromNow = 21, hour = 11),
            endTime = createFutureTimestamp(daysFromNow = 21, hour = 20),
            capacity = 300,
            ticketsRemaining = 100,
            pricingTiers = listOf(PricingTier("Tasting Pass", 40.0, 300, 100)),
            tags = listOf("food", "festival", "culinary"),
        ),

        // Bern Event
        Event(
            eventId = "test-bern-history",
            title = "Bern Historical Tour",
            description = "Guided tour of Bern's old town",
            organizerId = "test-organizer",
            organizerName = "Bern Tourism",
            status = EventStatus.PUBLISHED,
            location =
                Location(
                    coordinates = GeoPoint(46.9480, 7.4474),
                    name = "Bern Old Town",
                    region = "Bern",
                ),
            startTime = createFutureTimestamp(daysFromNow = 5, hour = 14),
            endTime = createFutureTimestamp(daysFromNow = 5, hour = 16),
            capacity = 25,
            ticketsRemaining = 10,
            pricingTiers = listOf(PricingTier("Tour", 20.0, 25, 10)),
            tags = listOf("history", "tour", "culture"),
        ),

        // Basel Event
        Event(
            eventId = "test-basel-science",
            title = "Basel Science Fair",
            description = "Interactive science exhibition for all ages",
            organizerId = "test-organizer",
            organizerName = "Basel University",
            status = EventStatus.PUBLISHED,
            location =
                Location(
                    coordinates = GeoPoint(47.5596, 7.5886),
                    name = "Basel University Campus",
                    region = "Basel",
                ),
            startTime = createFutureTimestamp(daysFromNow = 10, hour = 9),
            endTime = createFutureTimestamp(daysFromNow = 10, hour = 17),
            capacity = 400,
            ticketsRemaining = 350,
            pricingTiers = listOf(PricingTier("Free Entry", 0.0, 400, 350)),
            tags = listOf("science", "education", "family"),
        ),

        // DRAFT Event (should not appear on map)
        Event(
            eventId = "test-draft-event",
            title = "DRAFT: Secret Event",
            description = "This should not appear on the map",
            organizerId = "test-organizer",
            organizerName = "Test Organizer",
            status = EventStatus.DRAFT,
            location =
                Location(
                    coordinates = GeoPoint(46.5200, 6.5800),
                    name = "Secret Location",
                    region = "Vaud",
                ),
            startTime = createFutureTimestamp(daysFromNow = 60),
            capacity = 50,
            ticketsRemaining = 50,
            tags = listOf("draft", "test"),
        ),
    )
  }

  private fun createFutureTimestamp(daysFromNow: Int = 0, hour: Int = 12): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, daysFromNow)
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return Timestamp(calendar.time)
  }
}
