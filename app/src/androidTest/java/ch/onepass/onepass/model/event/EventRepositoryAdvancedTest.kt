package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.utils.EVENTS_COLLECTION_PATH
import ch.onepass.onepass.utils.EventTestData
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import com.google.firebase.firestore.GeoPoint
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Advanced integration tests for EventRepositoryFirebase using test data helpers.
 *
 * This test class demonstrates more complex scenarios and edge cases using the EventTestData helper
 * for cleaner and more maintainable tests.
 */
class EventRepositoryAdvancedTest : FirestoreTestBase() {

  private lateinit var userId: String

  private suspend fun clearTestCollection() {
    val events = FirebaseEmulator.firestore.collection(EVENTS_COLLECTION_PATH).get().await()

    if (events.isEmpty) return

    val batch = FirebaseEmulator.firestore.batch()
    events.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()
  }

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      clearTestCollection()
      // Sign in anonymously for testing
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
    }
  }

  @Test
  fun canCreateMultipleEventsWithDifferentStatuses() = runTest {
    // Create events with explicit unique IDs to avoid timestamp collisions
    val draftEvent = EventTestData.createDraftEvent(eventId = "draft-1", organizerId = userId)
    val publishedEvent =
        EventTestData.createPublishedEvent(eventId = "published-1", organizerId = userId)
    val closedEvent = EventTestData.createClosedEvent(eventId = "closed-1", organizerId = userId)
    val cancelledEvent =
        EventTestData.createCancelledEvent(eventId = "cancelled-1", organizerId = userId)

    val events = listOf(draftEvent, publishedEvent, closedEvent, cancelledEvent)

    events.forEach { event ->
      val result = repository.createEvent(event)
      assertTrue(result.isSuccess)
    }

    assertEquals(4, getEventsCount())

    // Verify each status has the correct count
    val draftEvents = repository.getEventsByStatus(EventStatus.DRAFT).first()
    assertEquals(1, draftEvents.size)

    val publishedEvents = repository.getEventsByStatus(EventStatus.PUBLISHED).first()
    assertEquals(1, publishedEvents.size)

    val closedEvents = repository.getEventsByStatus(EventStatus.CLOSED).first()
    assertEquals(1, closedEvents.size)

    val cancelledEvents = repository.getEventsByStatus(EventStatus.CANCELLED).first()
    assertEquals(1, cancelledEvents.size)
  }

  @Test
  fun soldOutEventHasCorrectTicketCounts() = runTest {
    val soldOutEvent = EventTestData.createSoldOutEvent(organizerId = userId, capacity = 50)

    val result = repository.createEvent(soldOutEvent)
    assertTrue(result.isSuccess)

    val eventId = result.getOrNull()!!
    val retrievedEvent = repository.getEventById(eventId).first()

    assertNotNull(retrievedEvent)
    assertEquals(0, retrievedEvent?.ticketsRemaining)
    assertEquals(50, retrievedEvent?.ticketsIssued)
    assertTrue(retrievedEvent?.isSoldOut ?: false)
  }

  @Test
  fun freeEventHasZeroPricing() = runTest {
    val freeEvent = EventTestData.createFreeEvent(organizerId = userId)

    val result = repository.createEvent(freeEvent)
    assertTrue(result.isSuccess)

    val eventId = result.getOrNull()!!
    val retrievedEvent = repository.getEventById(eventId).first()

    assertNotNull(retrievedEvent)
    assertEquals(0u, retrievedEvent?.lowestPrice)
    assertEquals(1, retrievedEvent?.pricingTiers?.size)
    assertEquals(0.0, retrievedEvent?.pricingTiers?.first()?.price)
  }

  @Test
  fun partiallyBookedEventTracksTicketsCorrectly() = runTest {
    val event =
        EventTestData.createPartiallyBookedEvent(
            organizerId = userId, capacity = 100, ticketsSold = 60, ticketsRedeemed = 30)

    val result = repository.createEvent(event)
    assertTrue(result.isSuccess)

    val eventId = result.getOrNull()!!
    val retrievedEvent = repository.getEventById(eventId).first()

    assertNotNull(retrievedEvent)
    assertEquals(40, retrievedEvent?.ticketsRemaining)
    assertEquals(60, retrievedEvent?.ticketsIssued)
    assertEquals(30, retrievedEvent?.ticketsRedeemed)
    assertTrue(retrievedEvent?.canResell ?: false) // Published with unredeemed tickets
  }

  @Test
  fun canQueryEventsByMultipleTags() = runTest {
    // Create events with overlapping tags
    val techEvent =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Tech Conference",
            tags = listOf("tech", "networking", "innovation"))

    val musicEvent =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Music Festival",
            tags = listOf("music", "entertainment", "outdoor"))

    val techMusicEvent =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Electronic Music Tech Showcase",
            tags = listOf("tech", "music", "innovation"))

    repository.createEvent(techEvent)
    repository.createEvent(musicEvent)
    repository.createEvent(techMusicEvent)

    // Query for tech events
    val techEvents = repository.getEventsByTag("tech").first()
    assertEquals(2, techEvents.size)
    assertTrue(techEvents.any { it.title == "Tech Conference" })
    assertTrue(techEvents.any { it.title == "Electronic Music Tech Showcase" })

    // Query for music events
    val musicEvents = repository.getEventsByTag("music").first()
    assertEquals(2, musicEvents.size)
    assertTrue(musicEvents.any { it.title == "Music Festival" })
    assertTrue(musicEvents.any { it.title == "Electronic Music Tech Showcase" })
  }

  @Test
  fun eventsSortedByStartTimeAcrossMultipleMonths() = runTest {
    // Create more distinct timestamps
    val events =
        listOf(
            EventTestData.createTestEvent(
                organizerId = userId,
                title = "Event in 60 days",
                startTime = EventTestData.createFutureTimestamp(daysFromNow = 60)),
            EventTestData.createTestEvent(
                organizerId = userId,
                title = "Event in 7 days",
                startTime = EventTestData.createFutureTimestamp(daysFromNow = 7)),
            EventTestData.createTestEvent(
                organizerId = userId,
                title = "Event in 30 days",
                startTime = EventTestData.createFutureTimestamp(daysFromNow = 30)))

    // Add in random order
    events.shuffled().forEach {
      val result = repository.createEvent(it)
      assertTrue("Event creation should succeed", result.isSuccess)
    }

    // Retrieve and verify sort order
    val retrievedEvents = repository.getAllEvents().first()
    assertEquals("Should have 3 events", 3, retrievedEvents.size)

    // Debug: print the actual order
    retrievedEvents.forEachIndexed { index, event ->
      println("Event $index: ${event.title} - ${event.startTime?.toDate()}")
    }

    // Verify they are in ascending order by start time
    for (i in 0 until retrievedEvents.size - 1) {
      val current = retrievedEvents[i].startTime
      val next = retrievedEvents[i + 1].startTime
      assertTrue(
          "Events should be sorted by start time",
          current == null || next == null || current <= next)
    }
  }

  @Test
  fun locationBasedQueryFindsNearbyEvents() = runTest {
    val eventsAtLocations = EventTestData.createEventsAtDifferentLocations(organizerId = userId)
    eventsAtLocations.forEach { repository.createEvent(it) }

    // Search around EPFL (46.5191, 6.5668) with 5km radius
    val nearbyEvents =
        repository
            .getEventsByLocation(Location(GeoPoint(46.5191, 6.5668), "EPFL Campus"), 5.0)
            .first()

    // Should find EPFL, Lausanne Center, and City Library (all within ~5km)
    assertTrue(nearbyEvents.size >= 2)
    assertTrue(nearbyEvents.any { it.location?.name == "EPFL" })
  }

  @Test
  fun searchEventsIsCaseInsensitiveForTitle() = runTest {
    val event = EventTestData.createTestEvent(organizerId = userId, title = "SwEnt Workshop")
    repository.createEvent(event)

    // Search with different cases
    val searchResults1 = repository.searchEvents("SwEnt").first()
    val searchResults2 = repository.searchEvents("swent").first()
    val searchResults3 = repository.searchEvents("SWENT").first()

    assertEquals(1, searchResults1.size)
    assertEquals(1, searchResults2.size)
    assertEquals(1, searchResults3.size)
  }

  @Test
  fun updatingEventPreservesUnmodifiedFields() = runTest {
    val originalEvent = EventTestData.createPublishedEvent(organizerId = userId, capacity = 100)

    val createResult = repository.createEvent(originalEvent)
    val eventId = createResult.getOrNull()!!

    // Update only the title
    val retrievedEvent = repository.getEventById(eventId).first()!!
    val updatedEvent = retrievedEvent.copy(title = "Updated Title")

    repository.updateEvent(updatedEvent)

    // Verify title changed but other fields preserved
    val finalEvent = repository.getEventById(eventId).first()
    assertEquals("Updated Title", finalEvent?.title)
    assertEquals(originalEvent.description, finalEvent?.description)
    assertEquals(originalEvent.capacity, finalEvent?.capacity)
    assertEquals(originalEvent.organizerId, finalEvent?.organizerId)
    assertEquals(originalEvent.pricingTiers.size, finalEvent?.pricingTiers?.size)
  }

  @Test
  fun canDeleteEventFromMiddleOfCollection() = runTest {
    // Create 5 events
    val events = EventTestData.createTestEvents(count = 5, organizerId = userId)
    events.forEach { repository.createEvent(it) }

    assertEquals(5, getEventsCount())

    // Delete the middle event (Event 3)
    val eventToDelete = repository.getAllEvents().first().find { it.title.contains("3") }
    assertNotNull(eventToDelete)

    repository.deleteEvent(eventToDelete!!.eventId)

    // Verify deletion
    assertEquals(4, getEventsCount())
    val remainingEvents = repository.getAllEvents().first()
    assertTrue(remainingEvents.none { it.eventId == eventToDelete.eventId })

    // Verify other events still exist
    assertTrue(remainingEvents.any { it.title.contains("1") })
    assertTrue(remainingEvents.any { it.title.contains("2") })
    assertTrue(remainingEvents.any { it.title.contains("4") })
    assertTrue(remainingEvents.any { it.title.contains("5") })
  }

  @Test
  fun getFeaturedEventsReturnsOnlyPublishedEvents() = runTest {
    // Create mix of draft and published events
    repository.createEvent(EventTestData.createDraftEvent(organizerId = userId))
    repository.createEvent(EventTestData.createPublishedEvent(organizerId = userId))
    repository.createEvent(
        EventTestData.createPublishedEvent(eventId = "pub-2", organizerId = userId))
    repository.createEvent(
        EventTestData.createDraftEvent(eventId = "draft-2", organizerId = userId))

    val featuredEvents = repository.getFeaturedEvents().first()

    // All featured events should be published
    assertTrue(featuredEvents.all { it.status == EventStatus.PUBLISHED })
  }

  @Test
  fun multipleUsersCanHaveEventsWithSameTitle() = runTest {
    val user1Event =
        EventTestData.createTestEvent(organizerId = userId, title = "Annual Conference")

    // Create another user's event with same title
    val user2Event =
        EventTestData.createTestEvent(
            organizerId = "different-user-id", title = "Annual Conference")

    val result1 = repository.createEvent(user1Event)
    val result2 = repository.createEvent(user2Event)

    assertTrue(result1.isSuccess)
    assertTrue(result2.isSuccess)

    // Both should exist with different IDs
    val user1Events = repository.getEventsByOrganization(userId).first()
    assertEquals(1, user1Events.size)
    assertEquals("Annual Conference", user1Events.first().title)
  }
}
