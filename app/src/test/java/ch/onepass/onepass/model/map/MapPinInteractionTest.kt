// kotlin
package ch.onepass.onepass.model.map

import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.FakeEventRepository
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.utils.EventTestData
import ch.onepass.onepass.utils.MockTimeProvider
import ch.onepass.onepass.utils.TimeProviderHolder
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MapPinInteractionTest {

  private lateinit var fakeRepo: FakeEventRepository
  private lateinit var mapViewModel: MapViewModel
  private val userId: String = "test-user"

  private val FIXED_TEST_TIME_MILLIS = 1704067200000L // Jan 1, 2024, 00:00:00 GMT
  private val FIXED_TEST_TIMESTAMP = Timestamp(Date(FIXED_TEST_TIME_MILLIS))

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())

    TimeProviderHolder.initialize(MockTimeProvider(FIXED_TEST_TIMESTAMP))

    fakeRepo = FakeEventRepository()
    mapViewModel = MapViewModel(eventRepository = fakeRepo)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    mapViewModel.onCleared()
    Dispatchers.resetMain()
  }

  @Test
  fun selectingEventShowsEventCard() = runTest {
    val event =
        EventTestData.createTestEvent(
            eventId = "evt-tech-2024",
            organizerId = userId,
            title = "Tech Conference 2024",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    // preserve eventId
    fakeRepo.addEvent(event)
    mapViewModel.refreshEvents()

    val uiStateBefore = mapViewModel.uiState.value
    val eventToSelect = uiStateBefore.events.first()

    mapViewModel.selectEvent(eventToSelect)

    val uiStateAfter = mapViewModel.uiState.value
    assertNotNull("Selected event should not be null", uiStateAfter.selectedEvent)

    val selectedEvent = uiStateAfter.selectedEvent!!
    assertEquals("Tech Conference 2024", selectedEvent.title)
    assertEquals(event.organizerName, selectedEvent.organizerName)
    assertEquals(event.displayDateTime, selectedEvent.displayDateTime)
    assertEquals(event.displayLocation, selectedEvent.displayLocation)
    assertEquals(event.eventId, selectedEvent.eventId)
  }

  @Test
  fun clearingSelectionHidesEventCard() = runTest {
    val event =
        EventTestData.createTestEvent(
            eventId = "evt-clear-1",
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    fakeRepo.addEvent(event)
    mapViewModel.refreshEvents()

    val eventToSelect = mapViewModel.uiState.value.events.first()
    mapViewModel.selectEvent(eventToSelect)

    var uiState = mapViewModel.uiState.value
    assertNotNull("Event should be selected initially", uiState.selectedEvent)

    mapViewModel.clearSelectedEvent()

    uiState = mapViewModel.uiState.value
    assertNull("Selected event should be null after clear", uiState.selectedEvent)
  }

  @Test
  fun selectingDifferentEventUpdatesCardContent() = runTest {
    val event1 =
        EventTestData.createTestEvent(
            eventId = "evt-1",
            organizerId = userId,
            title = "First Event - Music Festival",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(latitude = 46.5191, longitude = 6.5668),
        )

    val event2 =
        EventTestData.createTestEvent(
            eventId = "evt-2",
            organizerId = userId,
            title = "Second Event - Art Exhibition",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(latitude = 46.2022, longitude = 6.1457),
        )

    fakeRepo.addEvent(event1)
    fakeRepo.addEvent(event2)
    mapViewModel.refreshEvents()

    val events = mapViewModel.uiState.value.events
    val firstEvent = events.find { it.title == "First Event - Music Festival" }!!
    val secondEvent = events.find { it.title == "Second Event - Art Exhibition" }!!

    mapViewModel.selectEvent(firstEvent)
    var uiState = mapViewModel.uiState.value
    assertEquals(
        "First event should be selected",
        "First Event - Music Festival",
        uiState.selectedEvent?.title)

    mapViewModel.selectEvent(secondEvent)
    uiState = mapViewModel.uiState.value
    assertEquals(
        "Second event should be selected",
        "Second Event - Art Exhibition",
        uiState.selectedEvent?.title)
  }

  @Test
  fun eventCardShowsCorrectEventDetails() = runTest {
    val event =
        EventTestData.createTestEvent(
            eventId = "evt-details-1",
            organizerId = userId,
            title = "Tech Conference 2024",
            organizerName = "Tech Community Organization",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(name = "EPFL Innovation Park"),
            capacity = 200,
            ticketsRemaining = 50,
        )

    fakeRepo.addEvent(event)
    mapViewModel.refreshEvents()

    val eventToSelect = mapViewModel.uiState.value.events.first()
    mapViewModel.selectEvent(eventToSelect)

    val selectedEvent = mapViewModel.uiState.value.selectedEvent!!

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
    val event =
        EventTestData.createTestEvent(
            eventId = "evt-persist-1",
            organizerId = userId,
            title = "Persistent Event",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    fakeRepo.addEvent(event)
    mapViewModel.refreshEvents()

    val eventToSelect = mapViewModel.uiState.value.events.first()
    mapViewModel.selectEvent(eventToSelect)

    var uiState = mapViewModel.uiState.value
    assertNotNull("Event should be selected", uiState.selectedEvent)
    assertEquals("Persistent Event", uiState.selectedEvent?.title)

    mapViewModel.refreshEvents()

    uiState = mapViewModel.uiState.value
    assertNotNull("Selected event should persist after refresh", uiState.selectedEvent)
    assertEquals("Persistent Event", uiState.selectedEvent?.title)
  }

  @Test
  fun selectingSameEventTwiceTogglesCardClosed() = runTest {
    val event =
        EventTestData.createTestEvent(
            eventId = "evt-toggle-1",
            organizerId = userId,
            title = "Single Event",
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    fakeRepo.addEvent(event)
    mapViewModel.refreshEvents()

    val eventToSelect = mapViewModel.uiState.value.events.first()

    mapViewModel.selectEvent(eventToSelect) // open
    mapViewModel.selectEvent(eventToSelect) // close

    val uiState = mapViewModel.uiState.value
    assertNull("Event should be deselected after second click", uiState.selectedEvent)
  }
}
