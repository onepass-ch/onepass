package ch.onepass.onepass.model.map

import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.utils.EventTestData
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/** Tests to verify pin interaction functionality - selecting events and showing cards. */
class MapPinInteractionTest : FirestoreTestBase() {

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
  fun selectingEventShowsEventCard() = runTest {
    // Arrange: Add a published event with location
    val event =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Tech Conference 2024",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    val eventId = repository.createEvent(event).getOrNull()!!
    mapViewModel.refreshEvents()

    // Wait for event to appear in UI state
    val uiStateBefore = mapViewModel.uiState.first { it.events.isNotEmpty() }
    val eventToSelect = uiStateBefore.events.first()

    // Act: Select the event
    mapViewModel.selectEvent(eventToSelect)

    // Assert: Event card should be shown with correct data
    val uiStateAfter = mapViewModel.uiState.first()
    assertNotNull("Selected event should not be null", uiStateAfter.selectedEvent)

    val selectedEvent = uiStateAfter.selectedEvent!!
    assertEquals("Tech Conference 2024", selectedEvent.title)
    assertEquals(event.organizerName, selectedEvent.organizerName)
    assertEquals(event.displayDateTime, selectedEvent.displayDateTime)
    assertEquals(event.displayLocation, selectedEvent.displayLocation)
    assertEquals(eventId, selectedEvent.eventId)
  }

  @Test
  fun clearingSelectionHidesEventCard() = runTest {
    // Arrange: Add and select an event
    val event =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    repository.createEvent(event)
    mapViewModel.refreshEvents()

    val eventToSelect = mapViewModel.uiState.first { it.events.isNotEmpty() }.events.first()
    mapViewModel.selectEvent(eventToSelect)

    // Verify event is selected initially
    var uiState = mapViewModel.uiState.first()
    assertNotNull("Event should be selected initially", uiState.selectedEvent)

    // Act: Clear selection
    mapViewModel.clearSelectedEvent()

    // Assert: Event card should be hidden
    uiState = mapViewModel.uiState.first()
    assertNull("Selected event should be null after clear", uiState.selectedEvent)
  }

  @Test
  fun selectingDifferentEventUpdatesCardContent() = runTest {
    // Arrange: Add multiple events with distinct titles
    val event1 =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "First Event - Music Festival",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(latitude = 46.5191, longitude = 6.5668),
        )

    val event2 =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Second Event - Art Exhibition",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(latitude = 46.2022, longitude = 6.1457),
        )

    repository.createEvent(event1)
    repository.createEvent(event2)
    mapViewModel.refreshEvents()

    val events = mapViewModel.uiState.first { it.events.size == 2 }.events

    // Act: Select first event, then second event
    val firstEvent = events.find { it.title == "First Event - Music Festival" }!!
    val secondEvent = events.find { it.title == "Second Event - Art Exhibition" }!!

    mapViewModel.selectEvent(firstEvent)

    var uiState = mapViewModel.uiState.first()
    assertEquals(
        "First event should be selected",
        "First Event - Music Festival",
        uiState.selectedEvent?.title,
    )

    mapViewModel.selectEvent(secondEvent)

    // Assert: Card should show second event's data
    uiState = mapViewModel.uiState.first()
    assertEquals(
        "Second event should be selected",
        "Second Event - Art Exhibition",
        uiState.selectedEvent?.title,
    )
  }

  @Test
  fun eventCardShowsCorrectEventDetails() = runTest {
    // Arrange: Create event with specific details
    val event =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Tech Conference 2024",
            organizerName = "Tech Community Organization",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(name = "EPFL Innovation Park"),
            capacity = 200,
            ticketsRemaining = 50,
        )

    repository.createEvent(event)
    mapViewModel.refreshEvents()

    // Act: Select the event
    val eventToSelect = mapViewModel.uiState.first { it.events.isNotEmpty() }.events.first()
    mapViewModel.selectEvent(eventToSelect)

    // Assert: Verify all card details are correct
    val selectedEvent = mapViewModel.uiState.first().selectedEvent!!

    assertEquals("Tech Conference 2024", selectedEvent.title)
    assertEquals("Tech Community Organization", selectedEvent.organizerName)
    assertEquals("EPFL Innovation Park", selectedEvent.displayLocation)
    assertNotNull("Event should have a display date time", selectedEvent.displayDateTime)
    assertFalse("Event should not be sold out", selectedEvent.isSoldOut)
    assertEquals(200, selectedEvent.capacity)
    assertEquals(50, selectedEvent.ticketsRemaining)
  }

  @Test
  fun selectedEventPersistsUntilCleared() = runTest {
    // Arrange: Add an event and select it
    val event =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Persistent Event",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    repository.createEvent(event)
    mapViewModel.refreshEvents()

    val eventToSelect = mapViewModel.uiState.first { it.events.isNotEmpty() }.events.first()
    mapViewModel.selectEvent(eventToSelect)

    // Verify event is selected
    var uiState = mapViewModel.uiState.first()
    assertNotNull("Event should be selected", uiState.selectedEvent)
    assertEquals("Persistent Event", uiState.selectedEvent?.title)

    // Act: Refresh events (selection should persist)
    mapViewModel.refreshEvents()

    // Assert: Selection should still be there
    uiState = mapViewModel.uiState.first()
    assertNotNull("Selected event should persist after refresh", uiState.selectedEvent)
    assertEquals("Persistent Event", uiState.selectedEvent?.title)
  }

  @Test
  fun selectingSameEventTwiceTogglesCardClosed() = runTest {
    // Arrange: Add an event
    val event =
        EventTestData.createTestEvent(
            organizerId = userId,
            title = "Single Event",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    repository.createEvent(event)
    mapViewModel.refreshEvents()

    val eventToSelect = mapViewModel.uiState.first { it.events.isNotEmpty() }.events.first()

    // Act: Select the same event twice
    mapViewModel.selectEvent(eventToSelect) // First click - card opens
    mapViewModel.selectEvent(eventToSelect) // Second click - card should close

    // Assert: Event card should be CLOSED after second click
    val uiState = mapViewModel.uiState.first()
    assertNull("Event should be deselected after second click", uiState.selectedEvent)
  }
}
