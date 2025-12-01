package ch.onepass.onepass.ui.map

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MapCameraTrackingIntegrationTest {

  private lateinit var fakeRepo: TestEventRepository
  private lateinit var mapViewModel: MapViewModel

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())

    fakeRepo = TestEventRepository()
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
        Event(
            eventId = "evt-tech-2024",
            title = "Tech Conference 2024",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    fakeRepo.setEvents(listOf(event))
    mapViewModel.refreshEvents()

    val uiStateBefore = mapViewModel.uiState.value
    val eventToSelect = uiStateBefore.events.first()

    mapViewModel.selectEvent(eventToSelect)

    val uiStateAfter = mapViewModel.uiState.value
    assertNotNull("Selected event should not be null", uiStateAfter.selectedEvent)

    val selectedEvent = uiStateAfter.selectedEvent!!
    assertEquals("Tech Conference 2024", selectedEvent.title)
  }

  @Test
  fun clearingSelectionHidesEventCard() = runTest {
    val event =
        Event(
            eventId = "evt-clear-1",
            title = "Event to Clear",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    fakeRepo.setEvents(listOf(event))
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
        Event(
            eventId = "evt-1",
            title = "First Event - Music Festival",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    val event2 =
        Event(
            eventId = "evt-2",
            title = "Second Event - Art Exhibition",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.2022, 6.1457), "Zurich", "Zurich"),
            startTime = Timestamp.now(),
            ticketsRemaining = 30)

    fakeRepo.setEvents(listOf(event1, event2))
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
        Event(
            eventId = "evt-details-1",
            title = "Tech Conference 2024",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL Innovation Park", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    fakeRepo.setEvents(listOf(event))
    mapViewModel.refreshEvents()

    val eventToSelect = mapViewModel.uiState.value.events.first()
    mapViewModel.selectEvent(eventToSelect)

    val selectedEvent = mapViewModel.uiState.value.selectedEvent!!

    assertEquals("Tech Conference 2024", selectedEvent.title)
    assertFalse("Event should not be sold out", selectedEvent.isSoldOut)
    assertEquals(50, selectedEvent.ticketsRemaining)
  }

  @Test
  fun selectedEventPersistsUntilCleared() = runTest {
    val event =
        Event(
            eventId = "evt-persist-1",
            title = "Persistent Event",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    fakeRepo.setEvents(listOf(event))
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
        Event(
            eventId = "evt-toggle-1",
            title = "Single Event",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    fakeRepo.setEvents(listOf(event))
    mapViewModel.refreshEvents()

    val eventToSelect = mapViewModel.uiState.value.events.first()

    mapViewModel.selectEvent(eventToSelect) // open
    mapViewModel.selectEvent(eventToSelect) // close

    val uiState = mapViewModel.uiState.value
    assertNull("Event should be deselected after second click", uiState.selectedEvent)
  }

  @Test
  fun cameraTracking_initialStateIsFalse() = runTest {
    val initialState = mapViewModel.uiState.value
    assertFalse("Tracking should be disabled initially", initialState.isCameraTracking)
  }

  @Test
  fun cameraTracking_canBeEnabledAndDisabled() = runTest {
    assertFalse(mapViewModel.isCameraTracking())

    mapViewModel.enableCameraTracking()
    assertTrue("Tracking should be enabled", mapViewModel.isCameraTracking())

    mapViewModel.disableCameraTracking()
    assertFalse("Tracking should be disabled", mapViewModel.isCameraTracking())
  }

  @Test
  fun cameraTracking_enableAndDisableMultipleTimes() = runTest {
    repeat(5) { i ->
      mapViewModel.enableCameraTracking()
      assertTrue("Iteration $i: Tracking should be enabled", mapViewModel.isCameraTracking())

      mapViewModel.disableCameraTracking()
      assertFalse("Iteration $i: Tracking should be disabled", mapViewModel.isCameraTracking())
    }
  }

  @Test
  fun cameraTracking_persistsWithEventSelection() = runTest {
    val event =
        Event(
            eventId = "evt-tracking-1",
            title = "Event With Tracking",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    fakeRepo.setEvents(listOf(event))
    mapViewModel.refreshEvents()

    mapViewModel.enableCameraTracking()
    assertTrue("Tracking should be enabled", mapViewModel.isCameraTracking())

    val eventToSelect = mapViewModel.uiState.value.events.first()
    mapViewModel.selectEvent(eventToSelect)

    assertTrue("Tracking should persist after event selection", mapViewModel.isCameraTracking())
    assertNotNull("Event should be selected", mapViewModel.uiState.value.selectedEvent)
  }

  @Test
  fun cameraTracking_canBeDisabledDuringEventSelection() = runTest {
    val event =
        Event(
            eventId = "evt-disable-tracking-1",
            title = "Event With Tracking Change",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    fakeRepo.setEvents(listOf(event))
    mapViewModel.refreshEvents()

    mapViewModel.enableCameraTracking()
    assertTrue(mapViewModel.isCameraTracking())

    val eventToSelect = mapViewModel.uiState.value.events.first()
    mapViewModel.selectEvent(eventToSelect)

    mapViewModel.disableCameraTracking()
    assertFalse("Tracking should be disabled", mapViewModel.isCameraTracking())
    assertNotNull("Event selection should persist", mapViewModel.uiState.value.selectedEvent)
  }

  @Test
  fun cameraTracking_clearingEventSelectionDoesNotAffectTracking() = runTest {
    val event =
        Event(
            eventId = "evt-tracking-clear-1",
            title = "Event With Tracking Persistence",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    fakeRepo.setEvents(listOf(event))
    mapViewModel.refreshEvents()

    mapViewModel.enableCameraTracking()

    val eventToSelect = mapViewModel.uiState.value.events.first()
    mapViewModel.selectEvent(eventToSelect)

    mapViewModel.clearSelectedEvent()

    assertTrue("Tracking should persist after clearing event", mapViewModel.isCameraTracking())
    assertNull("Event selection should be cleared", mapViewModel.uiState.value.selectedEvent)
  }

  @Test
  fun cameraTracking_multipleEventsCycle() = runTest {
    val event1 =
        Event(
            eventId = "evt-tracking-multi-1",
            title = "Event 1",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    val event2 =
        Event(
            eventId = "evt-tracking-multi-2",
            title = "Event 2",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.2022, 6.1457), "Zurich", "Zurich"),
            startTime = Timestamp.now(),
            ticketsRemaining = 30)

    fakeRepo.setEvents(listOf(event1, event2))
    mapViewModel.refreshEvents()

    val events = mapViewModel.uiState.value.events

    // Enable tracking
    mapViewModel.enableCameraTracking()
    assertTrue(mapViewModel.isCameraTracking())

    // Select first event
    mapViewModel.selectEvent(events[0])
    assertTrue(mapViewModel.isCameraTracking())
    assertEquals("Event 1", mapViewModel.uiState.value.selectedEvent?.title)

    // Disable tracking
    mapViewModel.disableCameraTracking()
    assertFalse(mapViewModel.isCameraTracking())

    // Switch to second event
    mapViewModel.selectEvent(events[1])
    assertFalse("Tracking should remain disabled", mapViewModel.isCameraTracking())
    assertEquals("Event 2", mapViewModel.uiState.value.selectedEvent?.title)

    // Re-enable tracking
    mapViewModel.enableCameraTracking()
    assertTrue(mapViewModel.isCameraTracking())

    // Clear selection
    mapViewModel.clearSelectedEvent()
    assertTrue("Tracking should persist", mapViewModel.isCameraTracking())
    assertNull("Selection should be cleared", mapViewModel.uiState.value.selectedEvent)
  }

  @Test
  fun cameraTracking_isValidCoordinateHelper() = runTest {
    assertTrue("Valid EPFL location", mapViewModel.isValidCoordinate(46.5191, 6.5668))
    assertTrue("Valid Zurich location", mapViewModel.isValidCoordinate(47.3769, 8.5417))
    assertFalse("Invalid latitude", mapViewModel.isValidCoordinate(100.0, 50.0))
    assertFalse("Invalid longitude", mapViewModel.isValidCoordinate(50.0, 200.0))
    assertFalse("NaN latitude", mapViewModel.isValidCoordinate(Double.NaN, 50.0))
    assertFalse("NaN longitude", mapViewModel.isValidCoordinate(50.0, Double.NaN))
  }

  @Test
  fun cameraTracking_stateConsistencyWithMultipleOperations() = runTest {
    val event1 =
        Event(
            eventId = "evt-complex-1",
            title = "Complex Event 1",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)
    val event2 =
        Event(
            eventId = "evt-complex-2",
            title = "Complex Event 2",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.2022, 6.1457), "Zurich", "Zurich"),
            startTime = Timestamp.now(),
            ticketsRemaining = 30)

    fakeRepo.setEvents(listOf(event1, event2))
    mapViewModel.refreshEvents()

    val events = mapViewModel.uiState.value.events

    // Sequence of operations
    mapViewModel.enableCameraTracking()
    mapViewModel.selectEvent(events[0])
    mapViewModel.disableCameraTracking()
    mapViewModel.selectEvent(events[1])
    mapViewModel.enableCameraTracking()
    mapViewModel.clearSelectedEvent()

    // Verify final state consistency
    assertTrue("Tracking should be enabled in final state", mapViewModel.isCameraTracking())
    assertNull("Event should be cleared", mapViewModel.uiState.value.selectedEvent)
    assertEquals("Should have all events", 2, mapViewModel.uiState.value.events.size)
  }

  @Test
  fun cameraTracking_independentOfPermissionState() = runTest {
    mapViewModel.setLocationPermission(true)
    assertFalse("Should not affect initial tracking state", mapViewModel.isCameraTracking())

    mapViewModel.enableCameraTracking()
    assertTrue(mapViewModel.isCameraTracking())

    mapViewModel.setLocationPermission(false)
  }

  @Test
  fun cameraTracking_eventInteractionCycle() = runTest {
    val event =
        Event(
            eventId = "evt-interaction-cycle",
            title = "Interaction Event",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    fakeRepo.setEvents(listOf(event))
    mapViewModel.refreshEvents()

    // Start with tracking enabled
    mapViewModel.enableCameraTracking()
    val eventToSelect = mapViewModel.uiState.value.events.first()

    // Select event - tracking persists
    mapViewModel.selectEvent(eventToSelect)
    assertTrue("Tracking persists with event selected", mapViewModel.isCameraTracking())
    assertNotNull("Event is selected", mapViewModel.uiState.value.selectedEvent)

    // Clear event - tracking persists
    mapViewModel.clearSelectedEvent()
    assertTrue("Tracking persists after event cleared", mapViewModel.isCameraTracking())
    assertNull("Event is cleared", mapViewModel.uiState.value.selectedEvent)
  }
}

class TestEventRepository : EventRepository {
  private var events: List<Event> = emptyList()

  fun setEvents(eventsList: List<Event>) {
    events = eventsList
  }

  override fun getEventById(eventId: String): Flow<Event> =
      flowOf(events.first { it.eventId == eventId })

  override fun getAllEvents(): Flow<List<Event>> = flowOf(events)

  override fun searchEvents(query: String): Flow<List<Event>> =
      flowOf(events.filter { it.title.contains(query, ignoreCase = true) })

  override fun getEventsByOrganization(orgId: String): Flow<List<Event>> = flowOf(events)

  override fun getEventsByLocation(center: Location, radiusKm: Double): Flow<List<Event>> =
      flowOf(events)

  override fun getEventsByTag(tag: String): Flow<List<Event>> = flowOf(events)

  override fun getFeaturedEvents(): Flow<List<Event>> = flowOf(events)

  override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> = flowOf(events)

  override suspend fun createEvent(event: Event) = Result.success(event.eventId)

  override suspend fun updateEvent(event: Event) = Result.success(Unit)

  override suspend fun deleteEvent(eventId: String) = Result.success(Unit)

  override suspend fun addEventImage(eventId: String, imageUrl: String) = Result.success(Unit)

  override suspend fun removeEventImage(eventId: String, imageUrl: String) = Result.success(Unit)

  override suspend fun updateEventImages(eventId: String, imageUrls: List<String>) =
      Result.success(Unit)
}
