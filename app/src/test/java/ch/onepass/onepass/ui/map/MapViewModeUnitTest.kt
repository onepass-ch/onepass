package ch.onepass.onepass.ui.map

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.ui.map.MapViewModel.Companion.CameraConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.mapbox.maps.MapView
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelUnitTest {
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  private lateinit var viewModel: MapViewModel
  private lateinit var mockEventRepository: EventRepository

  private lateinit var validEvent1: Event
  private lateinit var validEvent2: Event
  private lateinit var soldOutEvent: Event
  private lateinit var availableEvent: Event
  private lateinit var zurichEvent: Event
  private lateinit var genevaEvent: Event

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    // Initialize all test events
    validEvent1 =
        Event(
            eventId = "event1",
            title = "Test Event 1",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    validEvent2 =
        Event(
            eventId = "event2",
            title = "Test Event 2",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(47.3769, 8.5417), "Zurich HB", "Zurich"),
            startTime = Timestamp.now(),
            ticketsRemaining = 30)

    soldOutEvent =
        Event(
            eventId = "soldout",
            title = "Sold Out Event",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5200, 6.5800), "City Library", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 0)

    availableEvent =
        Event(
            eventId = "available",
            title = "Available Event",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5210, 6.5790), "Lausanne Center", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 50)

    zurichEvent =
        Event(
            eventId = "zurich-event",
            title = "Zurich Event",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(47.3769, 8.5417), "Zurich HB", "Zurich"),
            startTime = Timestamp.now(),
            ticketsRemaining = 40)

    genevaEvent =
        Event(
            eventId = "geneva-event",
            title = "Geneva Event",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.2044, 6.1432), "Geneva Airport", "Geneva"),
            startTime = Timestamp.now(),
            ticketsRemaining = 25)

    // Create mock repository with all valid events
    mockEventRepository = mockk(relaxed = true)
    val allValidEvents =
        listOf(validEvent1, validEvent2, soldOutEvent, availableEvent, zurichEvent, genevaEvent)
    coEvery { mockEventRepository.getEventsByStatus(EventStatus.PUBLISHED) } returns
        MutableStateFlow(allValidEvents)

    viewModel = MapViewModel(mockEventRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    testScope.cancel()
  }

  @Test
  fun isValidCoordinateReturnsTrueForValidCoordinates() {
    assertTrue(viewModel.isValidCoordinate(46.5191, 6.5668))
    assertTrue(viewModel.isValidCoordinate(-45.0, -120.0))
    assertTrue(viewModel.isValidCoordinate(90.0, 180.0))
    assertTrue(viewModel.isValidCoordinate(-90.0, -180.0))
  }

  @Test
  fun isValidCoordinateReturnsFalseForInvalidCoordinates() {
    assertFalse(viewModel.isValidCoordinate(100.0, 50.0)) // latitude too high
    assertFalse(viewModel.isValidCoordinate(-100.0, 50.0)) // latitude too low
    assertFalse(viewModel.isValidCoordinate(50.0, 200.0)) // longitude too high
    assertFalse(viewModel.isValidCoordinate(50.0, -200.0)) // longitude too low
    assertFalse(viewModel.isValidCoordinate(Double.NaN, 50.0)) // NaN latitude
    assertFalse(viewModel.isValidCoordinate(50.0, Double.NaN)) // NaN longitude
  }

  @Test
  fun setShowFilterDialogUpdatesDialogVisibility() {
    viewModel.setShowFilterDialog(true)

    assertTrue(viewModel.uiState.value.showFilterDialog)

    viewModel.setShowFilterDialog(false)

    assertFalse(viewModel.uiState.value.showFilterDialog)
  }

  @Test
  fun setLocationPermissionUpdatesPermissionState() {
    assertFalse(viewModel.uiState.value.hasLocationPermission)

    viewModel.setLocationPermission(true)

    assertTrue(viewModel.uiState.value.hasLocationPermission)

    viewModel.setLocationPermission(false)

    assertFalse(viewModel.uiState.value.hasLocationPermission)
  }

  @Test
  fun setLocationPermissionCallsEnableLocationTrackingWhenGranted() {
    val mockMapView = mockk<MapView>(relaxed = true)
    viewModel.onMapReady(mockMapView, false)

    viewModel.setLocationPermission(true)

    assertTrue(viewModel.uiState.value.hasLocationPermission)
  }

  @Test
  fun applyFiltersToCurrentEventsFiltersEventsByRegion() = runTest {
    advanceUntilIdle()

    assertEquals(6, viewModel.uiState.value.events.size)

    val filters = EventFilters(region = "Vaud")
    viewModel.applyFiltersToCurrentEvents(filters)

    val vaudEvents = viewModel.uiState.value.events
    assertEquals(3, vaudEvents.size) // validEvent1, soldOutEvent, availableEvent are in Vaud
    vaudEvents.forEach { event -> assertEquals("Vaud", event.location?.region) }
  }

  @Test
  fun applyFiltersToCurrentEventsWithZurichRegionFilterShowsOnlyZurichEvents() = runTest {
    advanceUntilIdle()

    assertEquals(6, viewModel.uiState.value.events.size)

    viewModel.applyFiltersToCurrentEvents(EventFilters(region = "Zurich"))

    val zurichEvents = viewModel.uiState.value.events
    assertEquals(2, zurichEvents.size) // validEvent2 and zurichEvent are in Zurich
    zurichEvents.forEach { event -> assertEquals("Zurich", event.location?.region) }
  }

  @Test
  fun applyFiltersToCurrentEventsWithNoFiltersShowsAllEvents() = runTest {
    advanceUntilIdle()

    assertEquals(6, viewModel.uiState.value.events.size)

    viewModel.applyFiltersToCurrentEvents(EventFilters(region = "Vaud"))
    assertEquals(3, viewModel.uiState.value.events.size)

    viewModel.applyFiltersToCurrentEvents(EventFilters()) // No filters

    assertEquals(6, viewModel.uiState.value.events.size)
  }

  @Test
  fun applyFiltersToCurrentEventsWithHideSoldOutShowsOnlyAvailableEvents() = runTest {
    advanceUntilIdle()
    assertEquals(6, viewModel.uiState.value.events.size)

    viewModel.applyFiltersToCurrentEvents(EventFilters(hideSoldOut = true))

    assertEquals(5, viewModel.uiState.value.events.size)
    viewModel.uiState.value.events.forEach { event ->
      assertFalse("Event ${event.eventId} should not be sold out", event.isSoldOut)
    }
  }

  @Test
  fun initialStateHasCorrectDefaultValues() {
    val initialState = viewModel.uiState.value

    assertTrue(initialState.events.isEmpty())
    assertNull(initialState.selectedEvent)
    assertFalse(initialState.showFilterDialog)
    assertFalse(initialState.hasLocationPermission)
  }

  @Test
  fun initialCameraOptionsAreSetCorrectly() {
    val cameraOptions = viewModel.initialCameraOptions

    assertNotNull(cameraOptions.center)
    assertEquals(CameraConfig.DEFAULT_LATITUDE, cameraOptions.center!!.latitude(), 0.001)
    assertEquals(CameraConfig.DEFAULT_LONGITUDE, cameraOptions.center!!.longitude(), 0.001)
    assertEquals(CameraConfig.DEFAULT_ZOOM, cameraOptions.zoom!!, 0.001)
  }

  @Test
  fun applyFiltersToCurrentEventsWithMultipleFiltersCombinesThemCorrectly() = runTest {
    advanceUntilIdle()

    // When - apply Vaud region + hide sold out filters
    val filters = EventFilters(region = "Vaud", hideSoldOut = true)
    viewModel.applyFiltersToCurrentEvents(filters)

    // Then - only available Vaud events should be shown
    val filteredEvents = viewModel.uiState.value.events
    assertEquals(2, filteredEvents.size) // validEvent1 and availableEvent (excluding soldOutEvent)
    filteredEvents.forEach { event ->
      assertEquals("Vaud", event.location?.region)
      assertFalse(event.isSoldOut)
    }
  }

  @Test
  fun eventIsSoldOutPropertyWorksCorrectlyForVariousScenarios() = runTest {
    advanceUntilIdle()

    assertTrue(soldOutEvent.isSoldOut)
    assertFalse(availableEvent.isSoldOut)
    assertFalse(validEvent1.isSoldOut)
    assertFalse(validEvent2.isSoldOut)
    assertFalse(zurichEvent.isSoldOut)
    assertFalse(genevaEvent.isSoldOut)
  }
}
