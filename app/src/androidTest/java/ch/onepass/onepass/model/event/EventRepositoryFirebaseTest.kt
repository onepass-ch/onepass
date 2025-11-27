package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for EventRepositoryFirebase using Firebase Emulator.
 *
 * These tests verify CRUD operations and queries against a real Firestore instance running in the
 * emulator. This ensures the repository implementation works correctly with actual Firebase APIs.
 *
 * Prerequisites:
 * - Firebase emulators must be running (firebase emulators:start)
 * - Test uses anonymous authentication
 *
 * @see FirestoreTestBase for setup/teardown logic
 */
class EventRepositoryFirebaseTest : FirestoreTestBase() {

  private lateinit var testEvent1: Event
  private lateinit var testEvent2: Event
  private lateinit var testEvent3: Event
  private lateinit var userId: String

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      // Sign in anonymously for testing
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"

      // Create test events with different dates and properties
      val calendar = Calendar.getInstance()
      calendar.set(2025, Calendar.DECEMBER, 25, 18, 0, 0)
      val timestamp1 = Timestamp(calendar.time)

      calendar.set(2025, Calendar.DECEMBER, 31, 20, 0, 0)
      val timestamp2 = Timestamp(calendar.time)

      calendar.set(2026, Calendar.JANUARY, 15, 14, 0, 0)
      val timestamp3 = Timestamp(calendar.time)

      testEvent1 =
          Event(
              eventId = "test-event-1",
              title = "Test Event 1",
              description = "Description for test event 1",
              organizerId = userId,
              organizerName = "Test Organizer",
              status = EventStatus.PUBLISHED,
              location =
                  Location(coordinates = GeoPoint(46.5191, 6.5668), name = "EPFL", region = "Vaud"),
              startTime = timestamp1,
              endTime = Timestamp(calendar.apply { add(Calendar.HOUR, 1) }.time),
              capacity = 100,
              ticketsRemaining = 100,
              ticketsIssued = 0,
              ticketsRedeemed = 0,
              pricingTiers =
                  listOf(PricingTier("General", 25.0, 80, 80), PricingTier("VIP", 50.0, 20, 20)),
              images = listOf("https://example.com/image1.jpg"),
              tags = listOf("tech", "networking"))

      testEvent2 =
          Event(
              eventId = "test-event-2",
              title = "Test Event 2",
              description = "Description for test event 2",
              organizerId = userId,
              organizerName = "Test Organizer",
              status = EventStatus.DRAFT,
              location =
                  Location(
                      coordinates = GeoPoint(46.5210, 6.5790),
                      name = "Lausanne Center",
                      region = "Vaud"),
              startTime = timestamp2,
              endTime = Timestamp(calendar.apply { add(Calendar.HOUR, 2) }.time),
              capacity = 50,
              ticketsRemaining = 50,
              ticketsIssued = 0,
              ticketsRedeemed = 0,
              pricingTiers = listOf(PricingTier("Standard", 15.0, 50, 50)),
              tags = listOf("music", "concert"))

      testEvent3 =
          Event(
              eventId = "test-event-3",
              title = "Test Event 3",
              description = "Description for test event 3",
              organizerId = userId,
              organizerName = "Test Organizer",
              status = EventStatus.PUBLISHED,
              location =
                  Location(
                      coordinates = GeoPoint(46.5200, 6.5800),
                      name = "City Library",
                      region = "Vaud"),
              startTime = timestamp3,
              endTime = Timestamp(calendar.apply { add(Calendar.HOUR, 1) }.time),
              capacity = 30,
              ticketsRemaining = 15,
              ticketsIssued = 15,
              ticketsRedeemed = 5,
              pricingTiers = listOf(PricingTier("Free", 0.0, 30, 15)),
              tags = listOf("education", "workshop"))
    }
  }

  @Test
  fun canAddEventToRepository() = runTest {
    val result = repository.createEvent(testEvent1)
    assertTrue("Create event should succeed", result.isSuccess)

    val eventId = result.getOrNull()
    assertNotNull("Event ID should not be null", eventId)

    assertEquals("Should have 1 event in repository", 1, getEventsCount())
    val events = getAllEventsFromFirestore()
    assertEquals("Should retrieve 1 event", 1, events.size)

    val storedEvent = events.first()
    assertEquals("Title should match", testEvent1.title, storedEvent.title)
    assertEquals("Description should match", testEvent1.description, storedEvent.description)
    assertEquals("Organizer ID should match", testEvent1.organizerId, storedEvent.organizerId)
  }

  @Test
  fun createEventWithCorrectID() = runTest {
    val result = repository.createEvent(testEvent1)
    assertTrue("Create should succeed", result.isSuccess)

    val eventId = result.getOrNull()
    assertNotNull("Event ID should not be null", eventId)

    val storedEvent = repository.getEventById(eventId!!).first()
    assertNotNull("Stored event should not be null", storedEvent)
    assertEquals("Titles should match", testEvent1.title, storedEvent?.title)
  }

  @Test
  fun canAddMultipleEventsToRepository() = runTest {
    repository.createEvent(testEvent1)
    repository.createEvent(testEvent2)
    repository.createEvent(testEvent3)

    assertEquals("Should have 3 events", 3, getEventsCount())
    val events = getAllEventsFromFirestore()
    assertEquals("Should retrieve 3 events", 3, events.size)

    val titles = events.map { it.title }.toSet()
    assertTrue("Should contain event 1", titles.contains(testEvent1.title))
    assertTrue("Should contain event 2", titles.contains(testEvent2.title))
    assertTrue("Should contain event 3", titles.contains(testEvent3.title))
  }

  @Test
  fun canRetrieveEventById() = runTest {
    val result = repository.createEvent(testEvent1)
    val eventId = result.getOrNull()!!

    val storedEvent = repository.getEventById(eventId).first()
    assertNotNull("Event should be found", storedEvent)
    assertEquals("Title should match", testEvent1.title, storedEvent?.title)
    assertEquals("Organizer should match", testEvent1.organizerId, storedEvent?.organizerId)
  }

  @Test
  fun canUpdateEvent() = runTest {
    val result = repository.createEvent(testEvent1)
    val eventId = result.getOrNull()!!

    val updatedEvent =
        testEvent1.copy(
            eventId = eventId,
            title = "Updated Event Title",
            description = "Updated description",
            capacity = 150)

    val updateResult = repository.updateEvent(updatedEvent)
    assertTrue("Update should succeed", updateResult.isSuccess)

    val storedEvent = repository.getEventById(eventId).first()
    assertEquals("Title should be updated", "Updated Event Title", storedEvent?.title)
    assertEquals("Description should be updated", "Updated description", storedEvent?.description)
    assertEquals("Capacity should be updated", 150, storedEvent?.capacity)
  }

  @Test
  fun canDeleteEvent() = runTest {
    repository.createEvent(testEvent1)
    repository.createEvent(testEvent2)

    assertEquals("Should start with 2 events", 2, getEventsCount())

    val events = getAllEventsFromFirestore()
    val eventToDelete = events.first { it.title == testEvent1.title }

    val deleteResult = repository.deleteEvent(eventToDelete.eventId)
    assertTrue("Delete should succeed", deleteResult.isSuccess)

    assertEquals("Should have 1 event remaining", 1, getEventsCount())
    val remainingEvents = getAllEventsFromFirestore()
    assertEquals("Should retrieve 1 event", 1, remainingEvents.size)
    assertEquals(
        "Remaining event should be event 2", testEvent2.title, remainingEvents.first().title)
  }

  @Test
  fun canGetEventsByOrganization() = runTest {
    repository.createEvent(testEvent1)
    repository.createEvent(testEvent2)
    repository.createEvent(testEvent3)

    val events = repository.getEventsByOrganization(userId).first()
    assertEquals("Should get all 3 events for organizer", 3, events.size)
  }

  @Test
  fun canGetEventsByStatus() = runTest {
    repository.createEvent(testEvent1)
    repository.createEvent(testEvent2)
    repository.createEvent(testEvent3)

    val publishedEvents = repository.getEventsByStatus(EventStatus.PUBLISHED).first()
    assertEquals("Should have 2 published events", 2, publishedEvents.size)
    assertTrue(
        "All should be published", publishedEvents.all { it.status == EventStatus.PUBLISHED })

    val draftEvents = repository.getEventsByStatus(EventStatus.DRAFT).first()
    assertEquals("Should have 1 draft event", 1, draftEvents.size)
    assertEquals("Should be draft status", EventStatus.DRAFT, draftEvents.first().status)
  }

  @Test
  fun canGetEventsByTag() = runTest {
    repository.createEvent(testEvent1)
    repository.createEvent(testEvent2)
    repository.createEvent(testEvent3)

    val techEvents = repository.getEventsByTag("tech").first()
    assertEquals("Should find 1 tech event", 1, techEvents.size)
    assertEquals("Should be event 1", testEvent1.title, techEvents.first().title)

    val musicEvents = repository.getEventsByTag("music").first()
    assertEquals("Should find 1 music event", 1, musicEvents.size)
    assertEquals("Should be event 2", testEvent2.title, musicEvents.first().title)
  }

  @Test
  fun canSearchEvents() = runTest {
    repository.createEvent(testEvent1)
    repository.createEvent(testEvent2)
    repository.createEvent(testEvent3)

    val searchResults = repository.searchEvents("Test Event").first()
    assertEquals("Should find all 3 events", 3, searchResults.size)

    val specificSearch = repository.searchEvents("Test Event 2").first()
    assertEquals("Should find 1 specific event", 1, specificSearch.size)
    assertEquals("Should be event 2", testEvent2.title, specificSearch.first().title)
  }

  @Test
  fun getFeaturedEventsReturnsLimitedResults() = runTest {
    repository.createEvent(testEvent1)
    repository.createEvent(testEvent2)
    repository.createEvent(testEvent3)

    val featuredEvents = repository.getFeaturedEvents().first()
    assertTrue("Should return at most 3 events", featuredEvents.size <= 3)
  }

  @Test
  fun getAllEventsReturnsSortedByStartTime() = runTest {
    // Add in reverse order
    repository.createEvent(testEvent3) // Latest
    repository.createEvent(testEvent1) // Earliest
    repository.createEvent(testEvent2) // Middle

    val events = repository.getAllEvents().first()
    assertEquals("Should have 3 events", 3, events.size)

    // Events should be sorted by start time (ascending)
    assertEquals("First should be event 1", testEvent1.title, events[0].title)
    assertEquals("Second should be event 2", testEvent2.title, events[1].title)
    assertEquals("Third should be event 3", testEvent3.title, events[2].title)
  }

  @Test
  fun eventNotFoundReturnsNull() = runTest {
    val event = repository.getEventById("non-existent-id").first()
    assertNull("Should return null for non-existent event", event)
  }

  @Test
  fun canGetEventsByLocation() = runTest {
    repository.createEvent(testEvent1)
    repository.createEvent(testEvent2)

    // Search around EPFL location with 10km radius
    val nearbyEvents =
        repository
            .getEventsByLocation(Location(GeoPoint(46.5191, 6.5668), "EPFL Campus"), 10.0)
            .first()

    assertTrue("Should find nearby events", nearbyEvents.isNotEmpty())
    assertTrue("Should include event at EPFL", nearbyEvents.any { it.title == testEvent1.title })
    assertTrue("Should include Lausanne Center", nearbyEvents.any { it.title == testEvent2.title })
  }
}
