package ch.onepass.onepass.ui.map

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.utils.MockTimeProvider
import ch.onepass.onepass.utils.TimeProviderHolder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Date
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

  private val FIXED_TEST_TIME_MILLIS = 1704067200000L // Jan 1, 2024, 00:00:00 GMT
  private val FIXED_TEST_TIMESTAMP = Timestamp(Date(FIXED_TEST_TIME_MILLIS))

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    TimeProviderHolder.initialize(MockTimeProvider(FIXED_TEST_TIMESTAMP))

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
    assertNotNull("Event should be selected", mapViewModel.uiState.value.currentSelectedEvent)
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
    assertNotNull("Event selection should persist", mapViewModel.uiState.value.currentSelectedEvent)
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
    assertNull("Event selection should be cleared", mapViewModel.uiState.value.currentSelectedEvent)
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
    assertEquals("Event 1", mapViewModel.uiState.value.currentSelectedEvent?.title)

    // Disable tracking
    mapViewModel.disableCameraTracking()
    assertFalse(mapViewModel.isCameraTracking())

    // Switch to second event
    mapViewModel.selectEvent(events[1])
    assertFalse("Tracking should remain disabled", mapViewModel.isCameraTracking())
    assertEquals("Event 2", mapViewModel.uiState.value.currentSelectedEvent?.title)

    // Re-enable tracking
    mapViewModel.enableCameraTracking()
    assertTrue(mapViewModel.isCameraTracking())

    // Clear selection
    mapViewModel.clearSelectedEvent()
    assertTrue("Tracking should persist", mapViewModel.isCameraTracking())
    assertNull("Selection should be cleared", mapViewModel.uiState.value.currentSelectedEvent)
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
    assertNull("Event should be cleared", mapViewModel.uiState.value.currentSelectedEvent)
    assertEquals("Should have all events", 2, mapViewModel.uiState.value.events.size)
  }

  @Test
  fun cameraTracking_clearingEventMultipleTimes_doesNotAffectTracking() = runTest {
    val event =
        Event(
            eventId = "evt-multi-clear",
            title = "Event For Multiple Clears",
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

    // Clear multiple times
    mapViewModel.clearSelectedEvent()
    mapViewModel.clearSelectedEvent() // Second clear
    mapViewModel.clearSelectedEvent() // Third clear

    assertTrue(
        "Tracking should remain enabled after multiple clears", mapViewModel.isCameraTracking())
    assertNull("Event selection should be cleared", mapViewModel.uiState.value.currentSelectedEvent)
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
      flowOf(events.filter { it.title.contains(query.trim(), ignoreCase = true) })

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
