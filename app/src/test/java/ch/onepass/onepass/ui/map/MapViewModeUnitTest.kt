package ch.onepass.onepass.ui.map

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import io.mockk.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelUnitTest {
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  private lateinit var viewModel: MapViewModel
  private lateinit var mockEventRepository: EventRepository
  private lateinit var mockMapView: MapView
  private lateinit var mockAnnotationManager: PointAnnotationManager
  private lateinit var mockLocationComponent: LocationComponentPlugin
  private lateinit var mockGesturesPlugin: GesturesPlugin
  private lateinit var mockMapboxMap: com.mapbox.maps.MapboxMap
  private lateinit var mockCameraAnimationsPlugin: CameraAnimationsPlugin

  private lateinit var validEvent1: Event
  private lateinit var validEvent2: Event

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    // Initialize test events
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

    // Create mock repository
    mockEventRepository = mockk(relaxed = true)
    val allValidEvents = listOf(validEvent1, validEvent2)
    coEvery { mockEventRepository.getEventsByStatus(EventStatus.PUBLISHED) } returns
        MutableStateFlow(allValidEvents)

    // Create mock MapView and dependencies
    mockMapView = mockk(relaxed = true)
    mockAnnotationManager = mockk(relaxed = true)
    mockLocationComponent = mockk(relaxed = true)
    mockGesturesPlugin = mockk(relaxed = true)
    mockMapboxMap = mockk(relaxed = true)
    mockCameraAnimationsPlugin = mockk(relaxed = true)

    // Setup mock behaviors
    every { mockMapView.annotations.createPointAnnotationManager() } returns mockAnnotationManager
    every { mockMapView.location } returns mockLocationComponent
    every { mockMapView.gestures } returns mockGesturesPlugin
    every { mockMapView.mapboxMap } returns mockMapboxMap

    every { mockMapboxMap.loadStyle(any<String>(), any()) } answers
        {
          val callback = secondArg<((Style) -> Unit)>()
          val mockStyle = mockk<Style>(relaxed = true)
          callback(mockStyle)
        }
    every { mockMapboxMap.setCamera(any<CameraOptions>()) } just Runs
    every { mockMapboxMap.pixelForCoordinate(any<Point>()) } returns
        com.mapbox.maps.ScreenCoordinate(100.0, 100.0)

    val mockCancelable = mockk<Cancelable>(relaxed = true)
    coEvery {
      mockCameraAnimationsPlugin.easeTo(any<CameraOptions>(), any<MapAnimationOptions>())
    } returns mockCancelable

    every { mockGesturesPlugin.updateSettings(any()) } just Runs

    every { mockGesturesPlugin.removeOnMapClickListener(any()) } returns Unit
    every { mockGesturesPlugin.addOnMapClickListener(any()) } returns Unit

    every { mockLocationComponent.updateSettings(any()) } just Runs
    every { mockLocationComponent.addOnIndicatorPositionChangedListener(any()) } just Runs
    every { mockLocationComponent.removeOnIndicatorPositionChangedListener(any()) } returns Unit

    every { mockAnnotationManager.removeClickListener(any()) } returns true
    every { mockAnnotationManager.addClickListener(any()) } returns true
    every { mockAnnotationManager.deleteAll() } just Runs
    every { mockAnnotationManager.create(any<PointAnnotationOptions>()) } returns
        mockk(relaxed = true)

    viewModel = MapViewModel(mockEventRepository)

    every { mockMapboxMap.loadStyle(any<String>(), any()) } just Runs
    every { mockMapboxMap.setCamera(any<CameraOptions>()) } just Runs
    every { mockMapboxMap.pixelForCoordinate(any<Point>()) } returns
        com.mapbox.maps.ScreenCoordinate(100.0, 100.0)
    every { mockLocationComponent.updateSettings(any()) } just Runs
    every { mockLocationComponent.addOnIndicatorPositionChangedListener(any()) } just Runs
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    testScope.cancel()
  }

  @Test
  fun setLocationPermission_updatesStateAndEnablesTrackingWhenGranted() = runTest {
    assertFalse(viewModel.uiState.value.hasLocationPermission)

    viewModel.setLocationPermission(true)

    assertTrue(viewModel.uiState.value.hasLocationPermission)
  }

  @Test
  fun setLocationPermission_updatesStateWhenDenied() = runTest {
    viewModel.setLocationPermission(true)
    assertTrue(viewModel.uiState.value.hasLocationPermission)

    viewModel.setLocationPermission(false)
    assertFalse(viewModel.uiState.value.hasLocationPermission)
  }

  @Test
  fun setupAnnotationsForEvents_createsAnnotations() = runTest {
    val events = listOf(validEvent1, validEvent2)

    viewModel.setupAnnotationsForEvents(events, mockMapView)
    advanceUntilIdle()

    verify(atLeast = 2) { mockAnnotationManager.create(any<PointAnnotationOptions>()) }
    verify { mockAnnotationManager.addClickListener(any()) }
    verify { mockGesturesPlugin.addOnMapClickListener(any()) }
  }

  @Test
  fun setupAnnotationsForEvents_handlesEmptyEventsList() = runTest {
    viewModel.setupAnnotationsForEvents(emptyList(), mockMapView)
    advanceUntilIdle()

    verify(exactly = 0) { mockAnnotationManager.create(any<PointAnnotationOptions>()) }
    verify { mockAnnotationManager.addClickListener(any()) }
  }

  @Test
  fun enableLocationTracking_withMapView_setsUpLocationComponent() {
    viewModel.internalMapView = mockMapView

    viewModel.enableLocationTracking()

    verify { mockLocationComponent.updateSettings(any()) }
    verify { mockLocationComponent.addOnIndicatorPositionChangedListener(any()) }
  }

  @Test
  fun enableLocationTracking_withoutMapView_doesNothing() {
    viewModel.internalMapView = null

    viewModel.enableLocationTracking()

    // Should not throw any exceptions
    verify(exactly = 0) { mockLocationComponent.updateSettings(any()) }
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
    assertFalse(viewModel.isValidCoordinate(100.0, 50.0))
    assertFalse(viewModel.isValidCoordinate(-100.0, 50.0))
    assertFalse(viewModel.isValidCoordinate(50.0, 200.0))
    assertFalse(viewModel.isValidCoordinate(50.0, -200.0))
    assertFalse(viewModel.isValidCoordinate(Double.NaN, 50.0))
    assertFalse(viewModel.isValidCoordinate(50.0, Double.NaN))
  }

  @Test
  fun setShowFilterDialogUpdatesDialogVisibility() {
    viewModel.setShowFilterDialog(true)
    assertTrue(viewModel.uiState.value.showFilterDialog)

    viewModel.setShowFilterDialog(false)
    assertFalse(viewModel.uiState.value.showFilterDialog)
  }

  @Test
  fun applyFiltersToCurrentEventsFiltersEventsByRegion() = runTest {
    advanceUntilIdle()
    assertEquals(2, viewModel.uiState.value.events.size)

    val filters = EventFilters(region = "Vaud")
    viewModel.applyFiltersToCurrentEvents(filters)

    val vaudEvents = viewModel.uiState.value.events
    assertEquals(1, vaudEvents.size)
    assertEquals("Vaud", vaudEvents[0].location?.region)
  }

  @Test
  fun selectEvent_togglesSelection() {
    viewModel.selectEvent(validEvent1)
    assertEquals(validEvent1, viewModel.uiState.value.selectedEvent)

    viewModel.selectEvent(validEvent1)
    assertNull(viewModel.uiState.value.selectedEvent)

    viewModel.selectEvent(validEvent2)
    assertEquals(validEvent2, viewModel.uiState.value.selectedEvent)
  }

  @Test
  fun clearSelectedEvent_clearsSelection() {
    viewModel.selectEvent(validEvent1)
    assertNotNull(viewModel.uiState.value.selectedEvent)

    viewModel.clearSelectedEvent()
    assertNull(viewModel.uiState.value.selectedEvent)
  }

  @Test
  fun getOrCreatePointAnnotationManager_createsNewManager() {
    val manager = viewModel.getOrCreatePointAnnotationManager(mockMapView)

    assertNotNull(manager)
    assertNotNull(viewModel.pointAnnotationManager)
  }

  @Test
  fun getOrCreatePointAnnotationManager_reusesExistingManager() {
    val manager1 = viewModel.getOrCreatePointAnnotationManager(mockMapView)
    val manager2 = viewModel.getOrCreatePointAnnotationManager(mockMapView)

    assertEquals(manager1, manager2)
    verify(exactly = 1) { mockMapView.annotations.createPointAnnotationManager() }
  }

  @Test
  fun clearAnnotationManager_clearsManager() {
    viewModel.getOrCreatePointAnnotationManager(mockMapView)
    assertNotNull(viewModel.pointAnnotationManager)

    viewModel.clearAnnotationManager()
    assertNull(viewModel.pointAnnotationManager)
  }

  @Test
  fun recenterCamera_withInvalidLocation_doesNothing() {
    viewModel.internalMapView = mockMapView
    viewModel.lastKnownPoint = Point.fromLngLat(200.0, 100.0)

    viewModel.recenterCamera()

    coVerify(exactly = 0) {
      mockCameraAnimationsPlugin.easeTo(any<CameraOptions>(), any<MapAnimationOptions>())
    }
  }

  @Test
  fun recenterCamera_withoutMapView_doesNothing() {
    viewModel.internalMapView = null
    viewModel.lastKnownPoint = Point.fromLngLat(6.5668, 46.5191)

    viewModel.recenterCamera()

    coVerify(exactly = 0) {
      mockCameraAnimationsPlugin.easeTo(any<CameraOptions>(), any<MapAnimationOptions>())
    }
  }

  @Test
  fun refreshEvents_fetchesEventsAgain() = runTest {
    viewModel.refreshEvents()
    advanceUntilIdle()

    // Should have been called once in init and once in refreshEvents
    verify(exactly = 2) { mockEventRepository.getEventsByStatus(EventStatus.PUBLISHED) }
  }

  @Test
  fun disableLocationTracking_disablesLocationComponent() {
    viewModel.internalMapView = mockMapView
    viewModel.setLocationPermission(true)

    // Now test disable (called internally when permission is denied)
    viewModel.setLocationPermission(false)

    verify { mockLocationComponent.updateSettings(any()) }
    verify { mockLocationComponent.removeOnIndicatorPositionChangedListener(any()) }
  }

  @Test
  fun initialCameraOptions_areSetCorrectly() {
    val cameraOptions = viewModel.initialCameraOptions

    assertNotNull(cameraOptions.center)
    assertEquals(46.5197, cameraOptions.center!!.latitude(), 0.001)
    assertEquals(6.6323, cameraOptions.center!!.longitude(), 0.001)
    assertEquals(7.0, cameraOptions.zoom!!, 0.001)
  }

  @Test
  fun applyFiltersToCurrentEvents_withHideSoldOut_showsOnlyAvailableEvents() = runTest {
    advanceUntilIdle()
    assertEquals(2, viewModel.uiState.value.events.size)

    val soldOutEvent =
        Event(
            eventId = "soldout",
            title = "Sold Out Event",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5200, 6.5800), "City Library", "Vaud"),
            startTime = Timestamp.now(),
            ticketsRemaining = 0)

    // Add sold out event to the repository flow
    val allEvents = listOf(validEvent1, validEvent2, soldOutEvent)
    coEvery { mockEventRepository.getEventsByStatus(EventStatus.PUBLISHED) } returns
        MutableStateFlow(allEvents)
    viewModel.refreshEvents()
    advanceUntilIdle()

    assertEquals(3, viewModel.uiState.value.events.size)

    val filters = EventFilters(hideSoldOut = true)
    viewModel.applyFiltersToCurrentEvents(filters)

    val availableEvents = viewModel.uiState.value.events
    assertEquals(2, availableEvents.size)
    availableEvents.forEach { event -> assertTrue(event.ticketsRemaining > 0) }
  }

  @Test
  fun initialStateHasCorrectDefaultValues() {
    val initialState = viewModel.uiState.value

    assertTrue(initialState.events.isEmpty())
    assertNull(initialState.selectedEvent)
    assertFalse(initialState.showFilterDialog)
    assertFalse(initialState.hasLocationPermission)
    assertFalse(initialState.isCameraTracking)
  }

  @Test
  fun setupAnnotationsForEvents_removesExistingClickListeners() = runTest {
    val events = listOf(validEvent1, validEvent2)

    viewModel.setupAnnotationsForEvents(events, mockMapView)
    advanceUntilIdle()

    verify { mockAnnotationManager.removeClickListener(any()) }
    verify { mockGesturesPlugin.removeOnMapClickListener(any()) }
  }

  @Test
  fun setupAnnotationsForEvents_annotationClick_selectsEvent() = runTest {
    val events = listOf(validEvent1, validEvent2)

    clearMocks(mockAnnotationManager)

    val annotationClickListenerSlot =
        slot<com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener>()
    every { mockAnnotationManager.addClickListener(capture(annotationClickListenerSlot)) } returns
        true

    viewModel.setupAnnotationsForEvents(events, mockMapView)
    advanceUntilIdle()

    val mockAnnotation = mockk<com.mapbox.maps.plugin.annotation.generated.PointAnnotation>()
    every { mockAnnotation.getData() } returns com.google.gson.JsonPrimitive(validEvent1.eventId)

    annotationClickListenerSlot.captured.onAnnotationClick(mockAnnotation)

    assertEquals(validEvent1, viewModel.uiState.value.selectedEvent)
  }

  @Test
  fun setupAnnotationsForEvents_mapClick_clearsSelectedEvent() = runTest {
    val events = listOf(validEvent1, validEvent2)

    viewModel.selectEvent(validEvent1)
    assertEquals(validEvent1, viewModel.uiState.value.selectedEvent)

    val mapClickListenerSlot = slot<com.mapbox.maps.plugin.gestures.OnMapClickListener>()
    every { mockGesturesPlugin.addOnMapClickListener(capture(mapClickListenerSlot)) } returns Unit

    viewModel.setupAnnotationsForEvents(events, mockMapView)
    advanceUntilIdle()

    mapClickListenerSlot.captured.onMapClick(com.mapbox.geojson.Point.fromLngLat(0.0, 0.0))

    assertNull(viewModel.uiState.value.selectedEvent)
  }

  @Test
  fun setupAnnotationsForEvents_annotationClickWithInvalidEventId_doesNothing() = runTest {
    val events = listOf(validEvent1, validEvent2)

    clearMocks(mockAnnotationManager)

    val annotationClickListenerSlot =
        slot<com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener>()
    every { mockAnnotationManager.addClickListener(capture(annotationClickListenerSlot)) } returns
        true

    viewModel.setupAnnotationsForEvents(events, mockMapView)
    advanceUntilIdle()

    val mockAnnotation = mockk<com.mapbox.maps.plugin.annotation.generated.PointAnnotation>()
    every { mockAnnotation.getData() } returns com.google.gson.JsonPrimitive("invalid-event-id")

    annotationClickListenerSlot.captured.onAnnotationClick(mockAnnotation)

    assertNull(viewModel.uiState.value.selectedEvent)
  }

  @Test
  fun setupAnnotationsForEvents_annotationClickWithNullData_doesNothing() = runTest {
    val events = listOf(validEvent1, validEvent2)

    clearMocks(mockAnnotationManager)

    val annotationClickListenerSlot =
        slot<com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener>()
    every { mockAnnotationManager.addClickListener(capture(annotationClickListenerSlot)) } returns
        true

    viewModel.setupAnnotationsForEvents(events, mockMapView)
    advanceUntilIdle()

    val mockAnnotation = mockk<com.mapbox.maps.plugin.annotation.generated.PointAnnotation>()
    every { mockAnnotation.getData() } returns null

    annotationClickListenerSlot.captured.onAnnotationClick(mockAnnotation)

    assertNull(viewModel.uiState.value.selectedEvent)
  }

  @Test
  fun enableCameraTracking_updatesPulsingToTrue() {
    viewModel.internalMapView = mockMapView
    viewModel.enableCameraTracking()
    verify { mockLocationComponent.updateSettings(any()) }
  }

  @Test
  fun disableCameraTracking_updatesPulsingToFalse() {
    viewModel.internalMapView = mockMapView
    viewModel.enableCameraTracking()
    viewModel.disableCameraTracking()
    verify(atLeast = 2) { mockLocationComponent.updateSettings(any()) }
  }

  @Test
  fun updateLocationPuckPulsing_withoutMapView_doesNothing() {
    viewModel.internalMapView = null
    viewModel.enableCameraTracking()
    verify(exactly = 0) { mockLocationComponent.updateSettings(any()) }
  }

  @Test
  fun setupGestureListeners_addsOnMoveListener() {
    viewModel.internalMapView = mockMapView
    viewModel.enableLocationTracking()
    verify { mockGesturesPlugin.addOnMoveListener(any()) }
  }

  @Test
  fun recenterCamera_enablesTrackingBeforeAnimation() {
    viewModel.internalMapView = mockMapView
    viewModel.lastKnownPoint = Point.fromLngLat(6.5668, 46.5191)
    viewModel.disableCameraTracking()

    assertFalse(viewModel.isCameraTracking())

    viewModel.recenterCamera()

    assertTrue(viewModel.isCameraTracking())
  }

  @Test
  fun cameraTracking_stateToggleWorks() {
    assertFalse(viewModel.isCameraTracking())

    viewModel.enableCameraTracking()
    assertTrue(viewModel.isCameraTracking())

    viewModel.disableCameraTracking()
    assertFalse(viewModel.isCameraTracking())

    viewModel.enableCameraTracking()
    assertTrue(viewModel.isCameraTracking())
  }

  @Test
  fun indicatorListener_whenTrackingEnabled_updatesCamera() {
    viewModel.internalMapView = mockMapView
    val trackingPoint = Point.fromLngLat(6.5668, 46.5191)

    val indicatorListenerSlot =
        slot<com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener>()
    every {
      mockLocationComponent.addOnIndicatorPositionChangedListener(capture(indicatorListenerSlot))
    } just Runs

    viewModel.enableCameraTracking()
    viewModel.enableLocationTracking()

    indicatorListenerSlot.captured.onIndicatorPositionChanged(trackingPoint)

    verify { mockMapboxMap.cameraAnimationsPlugin(any()) }

    assertEquals(trackingPoint, viewModel.lastKnownPoint)
  }

  @Test
  fun indicatorListener_whenTrackingDisabled_skipsCamera() {
    viewModel.internalMapView = mockMapView
    val trackingPoint = Point.fromLngLat(6.5668, 46.5191)

    val indicatorListenerSlot =
        slot<com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener>()
    every {
      mockLocationComponent.addOnIndicatorPositionChangedListener(capture(indicatorListenerSlot))
    } just Runs

    viewModel.disableCameraTracking()
    viewModel.enableLocationTracking()

    indicatorListenerSlot.captured.onIndicatorPositionChanged(trackingPoint)

    assertEquals(trackingPoint, viewModel.lastKnownPoint)
  }

  @Test
  fun recenterCamera_setsUpFocalPointAndReEnablesTracking() {
    viewModel.internalMapView = mockMapView
    viewModel.lastKnownPoint = Point.fromLngLat(6.5668, 46.5191)
    viewModel.disableCameraTracking()

    assertFalse(viewModel.isCameraTracking())

    viewModel.recenterCamera()

    assertTrue(viewModel.isCameraTracking())

    verify { mockMapboxMap.pixelForCoordinate(viewModel.lastKnownPoint!!) }

    verify { mockGesturesPlugin.focalPoint = any<com.mapbox.maps.ScreenCoordinate>() }

    verify { mockMapboxMap.cameraAnimationsPlugin(any()) }
  }

  @Test
  fun onCleared_removesAndNullsMoveListener() {
    viewModel.internalMapView = mockMapView
    viewModel.enableLocationTracking()

    viewModel.onCleared()

    verify { mockGesturesPlugin.removeOnMoveListener(any()) }

    verify { mockLocationComponent.removeOnIndicatorPositionChangedListener(any()) }

    assertNull(viewModel.internalMapView)
    assertNull(viewModel.pointAnnotationManager)
  }

  @Test
  fun enableLocationTracking_setsUpGesturesAndEnablesTracking() {
    viewModel.internalMapView = mockMapView

    viewModel.enableLocationTracking()

    verify { mockLocationComponent.updateSettings(any()) }

    verify { mockLocationComponent.addOnIndicatorPositionChangedListener(any()) }

    verify { mockGesturesPlugin.addOnMoveListener(any()) }

    assertTrue(viewModel.isCameraTracking())
  }
}
