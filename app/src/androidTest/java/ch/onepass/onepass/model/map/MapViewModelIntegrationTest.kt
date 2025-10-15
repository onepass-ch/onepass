package ch.onepass.onepass.model.map

import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.utils.EventTestData
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/** Integration tests for MapViewModel with Firebase emulator, testing the complete flow. */
class MapViewModelIntegrationTest : FirestoreTestBase() {

  private lateinit var mapViewModel: MapViewModel
  private lateinit var userId: String

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
      mapViewModel = MapViewModel(repository)
    }
  }

  @Test
  fun viewModelAutomaticallyLoadsPublishedEventsOnInit() = runTest {
    // Arrange: Add events before ViewModel initialization
    val publishedEvent =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Auto-loaded Published Event",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    val draftEvent =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Auto-loaded Draft Event",
            status = EventStatus.DRAFT,
            location = EventTestData.createTestLocation(),
        )

    repository.createEvent(publishedEvent)
    repository.createEvent(draftEvent)

    // Act: Create new ViewModel (simulates app start)
    val newViewModel = MapViewModel(repository)

    // Assert: ViewModel should automatically load only published events
    val uiState = newViewModel.uiState.first { it.events.isNotEmpty() }
    assertTrue("Should load events automatically", uiState.events.isNotEmpty())
    assertTrue(
        "Should only load published events",
        uiState.events.all { it.status == EventStatus.PUBLISHED },
    )
    assertEquals("Should only have published events", 1, uiState.events.size)
    assertEquals("Auto-loaded Published Event", uiState.events.first().title)
  }

  @Test
  fun refreshEventsUpdatesLiveData() = runTest {
    // Arrange: Start with empty state
    var uiState = mapViewModel.uiState.first()
    assertTrue("Should start with no events", uiState.events.isEmpty())

    // Act: Add event and refresh
    val event =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Refresh Test Event",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    repository.createEvent(event)
    mapViewModel.refreshEvents()

    // Assert: Events should be updated
    uiState = mapViewModel.uiState.first { it.events.isNotEmpty() }
    assertTrue("Should have events after refresh", uiState.events.isNotEmpty())
    assertEquals("Refresh Test Event", uiState.events.first().title)
  }

  @Test
  fun mixedEventScenarioShowsCorrectPins() = runTest {
    // Arrange: Complex scenario with various event types
    val scenarios =
        listOf(
            // Should show pins (published with location)
            EventTestData.createTestEvent(
                organizerId = userId,
                title = "Published Event Lausanne",
                status = EventStatus.PUBLISHED,
                location = EventTestData.createTestLocation(latitude = 46.5191, longitude = 6.5668),
            ),
            EventTestData.createTestEvent(
                organizerId = userId,
                title = "Published Event Geneva",
                status = EventStatus.PUBLISHED,
                location = EventTestData.createTestLocation(latitude = 46.2022, longitude = 6.1457),
            ),

            // Should NOT show pins
            EventTestData.createTestEvent(
                organizerId = userId,
                title = "Draft Event Zurich",
                status = EventStatus.DRAFT,
                location = EventTestData.createTestLocation(latitude = 47.3769, longitude = 8.5417),
            ),
            EventTestData.createTestEvent(
                organizerId = userId,
                title = "Published Event No Location",
                status = EventStatus.PUBLISHED,
                location = null,
            ),
            EventTestData.createTestEvent(
                organizerId = userId,
                title = "Cancelled Event Lausanne",
                status = EventStatus.CANCELLED,
                location = EventTestData.createTestLocation(latitude = 46.5200, longitude = 6.5800),
            ),
        )

    // Act: Add all events
    scenarios.forEach { repository.createEvent(it) }
    mapViewModel.refreshEvents()

    // Assert: Only the correct events should be visible
    val uiState = mapViewModel.uiState.first { it.events.size == 2 }
    assertEquals("Should only show 2 published events with locations", 2, uiState.events.size)

    val visibleEventTitles = uiState.events.map { it.title }.toSet()
    assertTrue(
        "Should contain Published Event Lausanne",
        visibleEventTitles.contains("Published Event Lausanne"),
    )
    assertTrue(
        "Should contain Published Event Geneva",
        visibleEventTitles.contains("Published Event Geneva"),
    )
    assertFalse("Should NOT contain draft event", visibleEventTitles.contains("Draft Event Zurich"))
    assertFalse(
        "Should NOT contain event without location",
        visibleEventTitles.contains("Published Event No Location"),
    )
    assertFalse(
        "Should NOT contain cancelled event",
        visibleEventTitles.contains("Cancelled Event Lausanne"),
    )
  }

  @Test
  fun eventsAreFilteredByStatusAndLocation() = runTest {
    // Arrange: Create events with different combinations
    val validEvents =
        listOf(
            EventTestData.createTestEvent(
                organizerId = userId,
                status = EventStatus.PUBLISHED,
                location = EventTestData.createTestLocation(latitude = 46.5191, longitude = 6.5668),
            ),
            EventTestData.createTestEvent(
                organizerId = userId,
                status = EventStatus.PUBLISHED,
                location = EventTestData.createTestLocation(latitude = 46.5210, longitude = 6.5790),
            ),
        )

    val invalidEvents =
        listOf(
            EventTestData.createTestEvent(
                organizerId = userId,
                status = EventStatus.DRAFT,
                location = EventTestData.createTestLocation(),
            ),
            EventTestData.createTestEvent(
                organizerId = userId,
                status = EventStatus.PUBLISHED,
                location = null,
            ),
            EventTestData.createTestEvent(
                organizerId = userId,
                status = EventStatus.CLOSED,
                location = EventTestData.createTestLocation(),
            ),
        )

    // Act: Add all events
    validEvents.forEach { repository.createEvent(it) }
    invalidEvents.forEach { repository.createEvent(it) }
    mapViewModel.refreshEvents()

    // Assert: Only valid events should be visible
    val uiState = mapViewModel.uiState.first { it.events.size == 2 }
    assertEquals("Should show exactly 2 valid events", 2, uiState.events.size)

    // All visible events should be published and have locations
    uiState.events.forEach { event ->
      assertEquals(EventStatus.PUBLISHED, event.status)
      assertTrue("Event should have coordinates", event.location?.coordinates != null)
    }
  }

  @Test
  fun uiStatePreservesEventsWhenNoChanges() = runTest {
    // Arrange: Add some events
    val events =
        listOf(
            EventTestData.createTestEvent(
                organizerId = userId,
                title = "Event One",
                status = EventStatus.PUBLISHED,
                location = EventTestData.createTestLocation(),
            ),
            EventTestData.createTestEvent(
                organizerId = userId,
                title = "Event Two",
                status = EventStatus.PUBLISHED,
                location = EventTestData.createTestLocation(),
            ),
        )

    events.forEach { repository.createEvent(it) }
    mapViewModel.refreshEvents()

    // Get initial state
    val initialUiState = mapViewModel.uiState.first { it.events.size == 2 }
    val initialEventIds = initialUiState.events.map { it.eventId }.toSet()

    // Act: Refresh without changes
    mapViewModel.refreshEvents()

    // Assert: Events should remain the same
    val finalUiState = mapViewModel.uiState.first()
    assertEquals("Should have same number of events", 2, finalUiState.events.size)

    val finalEventIds = finalUiState.events.map { it.eventId }.toSet()
    assertEquals("Event IDs should remain the same", initialEventIds, finalEventIds)
  }

  @Test
  fun selectedEventClearedOnNewEventSelection() = runTest {
    // Arrange: Add multiple events
    val event1 =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Event Alpha",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    val event2 =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Event Beta",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    repository.createEvent(event1)
    repository.createEvent(event2)
    mapViewModel.refreshEvents()

    val events = mapViewModel.uiState.first { it.events.size == 2 }.events

    // Act: Select first event, then second event
    val firstEvent = events.find { it.title == "Event Alpha" }!!
    val secondEvent = events.find { it.title == "Event Beta" }!!

    mapViewModel.selectEvent(firstEvent)
    var uiState = mapViewModel.uiState.first()
    assertEquals("First event should be selected", "Event Alpha", uiState.selectedEvent?.title)

    mapViewModel.selectEvent(secondEvent)

    // Assert: Only second event should be selected
    uiState = mapViewModel.uiState.first()
    assertEquals("Second event should be selected", "Event Beta", uiState.selectedEvent?.title)
    assertFalse("First event should not be selected", uiState.selectedEvent?.title == "Event Alpha")
  }
}
