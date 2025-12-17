package ch.onepass.onepass.ui.map

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.repository.RepositoryProvider
import ch.onepass.onepass.utils.EventFilteringUtils.applyFiltersLocally
import com.google.gson.JsonPrimitive
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraChanged
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.Plugin.Companion.MAPBOX_COMPASS_PLUGIN_ID
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.compass.CompassPlugin
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the [MapScreen].
 *
 * @property events The list of events currently displayed on the map (after filtering).
 * @property selectedEventGroup The group of events currently selected (e.g., a cluster or stack).
 * @property selectedEventIndex The index of the currently visible event within the
 *   [selectedEventGroup].
 * @property showFilterDialog Whether the filter dialog is currently visible.
 * @property hasLocationPermission Whether the user has granted fine location permission.
 * @property isCameraTracking Whether the camera is currently locked onto and following the user's
 *   location.
 */
data class MapUIState(
    val events: List<Event> = emptyList(),
    // Instead of a single event, we hold a group
    val selectedEventGroup: List<Event> = emptyList(),
    val selectedEventIndex: Int = 0,
    val showFilterDialog: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val isCameraTracking: Boolean = false,
) {
  /** Helper to get the currently visible event in the selection. */
  val currentSelectedEvent: Event?
    get() = selectedEventGroup.getOrNull(selectedEventIndex)
}

private object AnnotationConfig {
  const val PIN_ICON = "purple-pin"
  const val PIN_SIZE = 0.6
}

private const val TAG = "MapViewModel"

/**
 * ViewModel for managing Mapbox map state and events with automatic camera tracking.
 *
 * Responsibilities include:
 * - Fetching and filtering published events with valid coordinates
 * - Tracking the selected event when a map pin is clicked (single or cluster/stack)
 * - Handling Mapbox MapView lifecycle and plugins
 * - Enabling location tracking and recentering camera
 * - Managing automatic camera tracking mode
 * - Disabling tracking on user gestures and re-enabling on recenter
 * - Handling event clustering based on zoom level
 *
 * @param eventRepository Repository providing access to [Event] data.
 */
class MapViewModel(
    private val eventRepository: EventRepository = RepositoryProvider.eventRepository,
) : ViewModel() {
  companion object {
    /** Configuration constants for the map camera. */
    object CameraConfig {
      const val DEFAULT_LATITUDE = 46.5197
      const val DEFAULT_LONGITUDE = 6.6323
      const val DEFAULT_ZOOM = 7.0
      const val RECENTER_ZOOM = 15.0
      const val TRACKING_ZOOM = 15.0
      const val MAX_ZOOM_THRESHOLD = 16.0 // Zoom level to trigger cluster selection
    }

    /** Configuration constants for map animations. */
    object AnimationConfig {
      const val DURATION_MS = 1000L
      const val TRACKING_DURATION_MS = 500L
    }

    /** Map style URI. */
    object MapStyle {
      const val URI = "mapbox://styles/walid-as/cmhmmxczk00ar01shdw6r8lel"
    }

    /** Geocoordinate validation limits. */
    object CoordinateLimits {
      const val MIN_LATITUDE = -90.0
      const val MAX_LATITUDE = 90.0
      const val MIN_LONGITUDE = -180.0
      const val MAX_LONGITUDE = 180.0
    }
  }

  // --- UI state ---
  private val _uiState = MutableStateFlow(MapUIState())
  /** The public [StateFlow] representing the current UI state of the map screen. */
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  // --- All events ---
  private val _allEvents = MutableStateFlow<List<Event>>(emptyList())
  /** The public [StateFlow] holding all published events fetched from the repository. */
  val allEvents: StateFlow<List<Event>> = _allEvents.asStateFlow()

  // --- Reusable PointAnnotationManager ---
  /** Manages the display and interaction of point annotations (pins/clusters) on the map. */
  var pointAnnotationManager: PointAnnotationManager? = null

  // --- Mapbox references ---
  /**
   * Holds reference to MapView for plugin access and camera control.
   *
   * **Memory Leak Safety:** StaticFieldLeak suppression is safe here because:
   * 1. Reference is explicitly nulled in [onCleared].
   * 2. All listeners are removed before clearing.
   * 3. Annotation manager is cleaned up.
   *
   * This ensures no Context leaks persist beyond ViewModel lifecycle.
   */
  @SuppressLint("StaticFieldLeak") var internalMapView: MapView? = null

  /** The last known location point of the user. */
  var lastKnownPoint: Point? = null
  private var indicatorListener: OnIndicatorPositionChangedListener? = null
  private var moveListener: OnMoveListener? = null

  private val defaultCenterPoint =
      Point.fromLngLat(CameraConfig.DEFAULT_LONGITUDE, CameraConfig.DEFAULT_LATITUDE)

  private var currentZoomLevel: Double = CameraConfig.DEFAULT_ZOOM
  /** The initial camera position and zoom level when the map loads. */
  val initialCameraOptions: CameraOptions =
      CameraOptions.Builder().center(defaultCenterPoint).zoom(CameraConfig.DEFAULT_ZOOM).build()

  init {
    fetchPublishedEvents()
  }

  // --- Event handling ---
  /**
   * Fetches all published events and updates [_allEvents]. Filters out events with invalid or null
   * coordinates.
   */
  private fun fetchPublishedEvents() {
    viewModelScope.launch {
      eventRepository.getEventsByStatus(EventStatus.PUBLISHED).collect { events ->
        // Filter events with valid coordinates
        val validEvents =
            events.filter { event ->
              val coords = event.location?.coordinates
              coords != null && isValidCoordinate(coords.latitude, coords.longitude)
            }
        _allEvents.value = validEvents
        // Start with all valid events as the currently displayed set
        _uiState.value = _uiState.value.copy(events = validEvents)
        internalMapView?.let { updateAnnotations(it) }
      }
    }
  }

  /**
   * Applies the given [EventFilters] to the list of all fetched events and updates the list of
   * events displayed on the map.
   *
   * @param filters The filters to apply.
   */
  fun applyFiltersToCurrentEvents(filters: EventFilters) {
    val filteredEvents = applyFiltersLocally(_allEvents.value, filters)
    _uiState.value = _uiState.value.copy(events = filteredEvents)
    internalMapView?.let { updateAnnotations(it) }
  }

  /**
   * Sets the visibility state of the filter dialog.
   *
   * @param show If true, the filter dialog will be displayed.
   */
  fun setShowFilterDialog(show: Boolean) {
    _uiState.update { it.copy(showFilterDialog = show) }
  }

  /**
   * Checks if given latitude and longitude are valid coordinates.
   *
   * @param latitude The latitude value to validate
   * @param longitude The longitude value to validate
   * @return true if coordinates are within valid ranges, false otherwise
   */
  fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
    return !latitude.isNaN() &&
        !longitude.isNaN() &&
        latitude in CoordinateLimits.MIN_LATITUDE..CoordinateLimits.MAX_LATITUDE &&
        longitude in CoordinateLimits.MIN_LONGITUDE..CoordinateLimits.MAX_LONGITUDE
  }

  /**
   * Selects a single event, making its details visible in the floating card. If the event is
   * already selected, it clears the selection.
   *
   * @param event The single event to select.
   */
  fun selectEvent(event: Event) {
    val currentGroup = _uiState.value.selectedEventGroup
    // If the event is already selected as the single item, clear it
    if (currentGroup.size == 1 && currentGroup.first().eventId == event.eventId) {
      clearSelectedEvent()
    } else {
      _uiState.value =
          _uiState.value.copy(selectedEventGroup = listOf(event), selectedEventIndex = 0)
    }
  }

  /**
   * Selects a cluster/stack of events, making the first event's details visible in the floating
   * card and enabling cluster navigation controls.
   *
   * @param events The list of events in the cluster/stack.
   */
  fun selectEventGroup(events: List<Event>) {
    if (events.isEmpty()) return
    _uiState.value = _uiState.value.copy(selectedEventGroup = events, selectedEventIndex = 0)
  }

  /** Selects the next event in the current [selectedEventGroup], wrapping around if at the end. */
  fun selectNextEvent() {
    val state = _uiState.value
    if (state.selectedEventGroup.isEmpty()) return
    val nextIndex = (state.selectedEventIndex + 1) % state.selectedEventGroup.size
    _uiState.value = state.copy(selectedEventIndex = nextIndex)
  }

  /**
   * Selects the previous event in the current [selectedEventGroup], wrapping around if at the
   * start.
   */
  fun selectPreviousEvent() {
    val state = _uiState.value
    if (state.selectedEventGroup.isEmpty()) return
    val prevIndex =
        (state.selectedEventIndex - 1 + state.selectedEventGroup.size) %
            state.selectedEventGroup.size
    _uiState.value = state.copy(selectedEventIndex = prevIndex)
  }

  /** Clears the currently selected event or event group, dismissing the floating card. */
  fun clearSelectedEvent() {
    _uiState.value = _uiState.value.copy(selectedEventGroup = emptyList(), selectedEventIndex = 0)
  }

  /** Refreshes the list of published events from the repository. */
  fun refreshEvents() {
    fetchPublishedEvents()
  }

  /**
   * Returns the existing [PointAnnotationManager] or creates a new one if it is null.
   *
   * @param mapView The [MapView] instance to associate the manager with.
   * @return The [PointAnnotationManager] instance.
   */
  fun getOrCreatePointAnnotationManager(mapView: MapView): PointAnnotationManager {
    return pointAnnotationManager
        ?: mapView.annotations.createPointAnnotationManager().also { manager ->
          pointAnnotationManager = manager
        }
  }

  /** Clears all annotations from the map and releases the [PointAnnotationManager] reference. */
  fun clearAnnotationManager() {
    pointAnnotationManager?.let {
      try {
        it.deleteAll()
      } catch (_: Throwable) {}
    }
    pointAnnotationManager = null
  }

  // --- Camera tracking state management ---
  /**
   * Enables automatic camera tracking mode. The camera will follow the user's location puck. Called
   * on initial map load and when recenter button is clicked. Also enables pulsing effect on the
   * location puck to indicate active tracking.
   */
  fun enableCameraTracking() {
    _uiState.update { it.copy(isCameraTracking = true) }
    updateLocationPuckPulsing(pulsingEnabled = true)
  }

  /**
   * Disables automatic camera tracking mode. Called when user manually pans/drags the map or
   * performs other gestures. Also disables pulsing effect on the location puck.
   */
  fun disableCameraTracking() {
    _uiState.update { it.copy(isCameraTracking = false) }
    updateLocationPuckPulsing(pulsingEnabled = false)
  }

  /** Returns whether the camera is currently in tracking mode. */
  fun isCameraTracking(): Boolean = _uiState.value.isCameraTracking

  /**
   * Updates the pulsing state of the location puck based on tracking mode. Preserves all other
   * location component settings.
   *
   * @param pulsingEnabled whether to enable pulsing animation
   */
  private fun updateLocationPuckPulsing(pulsingEnabled: Boolean) {
    internalMapView?.let { mapView ->
      val locationComponent: LocationComponentPlugin = mapView.location
      locationComponent.updateSettings { this.pulsingEnabled = pulsingEnabled }
    } ?: Log.w(TAG, "updateLocationPuckPulsing: internalMapView is null, skipping pulsing update")
  }

  // --- Mapbox integration ---
  /**
   * Called when the MapView is ready. Initializes Mapbox plugins, loads the style, and sets up
   * location tracking and initial camera position.
   *
   * @param mapView The newly ready [MapView].
   * @param hasLocationPermission True if location permission has been granted.
   */
  open fun onMapReady(mapView: MapView, hasLocationPermission: Boolean) {
    if (internalMapView == mapView) return
    internalMapView = mapView

    // Listen for zoom changes to re-run clustering
    mapView.mapboxMap.subscribeCameraChanged { event: CameraChanged ->
      val newZoom = event.cameraState.zoom
      if (abs(newZoom - currentZoomLevel) > 0.5) {
        currentZoomLevel = newZoom
        updateAnnotations(mapView)
      }
    }

    mapView.mapboxMap.loadStyle(MapStyle.URI) {
      configurePlugins(mapView)

      if (hasLocationPermission) {
        enableLocationTracking(mapView)
        // Start with tracking enabled
        enableCameraTracking()
      }

      mapView.mapboxMap.setCamera(initialCameraOptions)
    }
  }

  /**
   * Enables the location component on the map and sets up the indicator listener. Also configures
   * gesture listeners to disable tracking on user interaction.
   *
   * @param mapView The MapView whose location component will be enabled.
   */
  private fun enableLocationTracking(mapView: MapView) {
    val locationComponent: LocationComponentPlugin = mapView.location
    locationComponent.updateSettings {
      enabled = true
      pulsingEnabled = true
      locationPuck = createDefault2DPuck(withBearing = true)
      puckBearing = PuckBearing.COURSE
      puckBearingEnabled = true
    }

    indicatorListener =
        OnIndicatorPositionChangedListener { point ->
              lastKnownPoint = point
              // Update camera if tracking is enabled
              if (isCameraTracking()) {
                updateCameraForTracking(mapView, point)
              }
            }
            .also { listener -> locationComponent.addOnIndicatorPositionChangedListener(listener) }

    // Set up gesture listeners to control tracking
    setupGestureListeners(mapView)
  }

  /**
   * Updates camera position to follow the location puck with smooth animation. Called continuously
   * when tracking is enabled.
   *
   * @param mapView The MapView to update
   * @param point The new location point to track
   */
  fun updateCameraForTracking(mapView: MapView, point: Point) {
    if (!isValidCoordinate(point.latitude(), point.longitude())) {
      return
    }

    // Get focal point at user location for smooth tracking
    val focalPoint = mapView.mapboxMap.pixelForCoordinate(point)
    mapView.gestures.focalPoint = focalPoint

    // Use shorter duration for smooth continuous tracking
    mapView.mapboxMap.easeTo(
        CameraOptions.Builder().center(point).zoom(CameraConfig.TRACKING_ZOOM).build(),
        MapAnimationOptions.mapAnimationOptions { duration(AnimationConfig.TRACKING_DURATION_MS) },
    )
  }

  /**
   * Sets up gesture listener to disable tracking on map manipulation. Disables tracking when:
   * - User drags/pans the map
   * - User pinches to zoom
   * - User rotates the map
   */
  fun setupGestureListeners(mapView: MapView) {
    // Listener for pan/drag gestures
    moveListener =
        object : OnMoveListener {
          override fun onMoveBegin(detector: MoveGestureDetector) {
            // Disable tracking when user starts panning
            disableCameraTracking()
          }

          override fun onMove(detector: MoveGestureDetector): Boolean {
            // No action needed, just return true to consume the event
            return false
          }

          override fun onMoveEnd(detector: MoveGestureDetector) {
            // Keep tracking disabled until recenter is clicked
          }
        }

    // Add move listener to gestures
    mapView.gestures.addOnMoveListener(moveListener!!)
  }

  /**
   * Recenters the camera on the last known user location and resumes automatic tracking. Does
   * nothing if no location is available or coordinates are invalid.
   */
  open fun recenterCamera() {
    val mapView = internalMapView ?: return
    val mapboxMap = mapView.mapboxMap
    val point = lastKnownPoint ?: return

    // Ensure point has valid coordinates
    if (!isValidCoordinate(point.latitude(), point.longitude())) {
      return
    }

    // Re-enable tracking mode
    enableCameraTracking()

    // Set focal point for smooth animation
    val focalPoint = mapboxMap.pixelForCoordinate(point)
    mapView.gestures.focalPoint = focalPoint

    mapboxMap.easeTo(
        CameraOptions.Builder().center(point).zoom(CameraConfig.RECENTER_ZOOM).build(),
        MapAnimationOptions.mapAnimationOptions { duration(AnimationConfig.DURATION_MS) },
    )
  }

  /**
   * Configures gesture and compass plugins for the MapView.
   *
   * @param mapView The MapView to configure.
   */
  private fun configurePlugins(mapView: MapView) {
    mapView.gestures.updateSettings {
      rotateEnabled = true
      pinchToZoomEnabled = true
      scrollEnabled = true
      doubleTapToZoomInEnabled = true
      doubleTouchToZoomOutEnabled = true
    }

    val compassPlugin = mapView.getPlugin<CompassPlugin>(MAPBOX_COMPASS_PLUGIN_ID)
    compassPlugin?.updateSettings { enabled = true }
  }

  /**
   * Cleans up listeners and releases MapView-related resources when ViewModel is cleared.
   *
   * This prevents memory leaks by:
   * 1. Removing gesture listener.
   * 2. Removing location indicator listener to break callback chain.
   * 3. Clearing annotation manager and all its annotations.
   * 4. Nulling MapView reference to release Context.
   */
  public override fun onCleared() {
    // Remove gesture listener
    moveListener?.let { listener -> internalMapView?.gestures?.removeOnMoveListener(listener) }
    moveListener = null

    // Remove location listener
    indicatorListener?.let { listener ->
      internalMapView?.location?.removeOnIndicatorPositionChangedListener(listener)
    }
    indicatorListener = null

    // Clear annotation manager
    clearAnnotationManager()

    // Release MapView reference (prevents Context leak)
    internalMapView = null

    super.onCleared()
  }

  /**
   * Updates the location permission flag in the UI state and enables/disables location tracking
   * accordingly.
   *
   * @param granted True if location permission is granted, false otherwise.
   */
  fun setLocationPermission(granted: Boolean) {
    _uiState.update { it.copy(hasLocationPermission = granted) }
    if (granted) enableLocationTracking() else disableLocationTracking()
  }

  /** Enables location tracking if permission is granted, and sets up listeners and tracking. */
  fun enableLocationTracking() {
    internalMapView?.let { mapView ->
      val locationComponent: LocationComponentPlugin = mapView.location
      locationComponent.updateSettings {
        enabled = true
        pulsingEnabled = true
        locationPuck = createDefault2DPuck(withBearing = true)
        puckBearing = PuckBearing.COURSE
        puckBearingEnabled = true
      }

      // Set up position listener if not already set
      if (indicatorListener == null) {
        indicatorListener =
            OnIndicatorPositionChangedListener { point ->
                  lastKnownPoint = point
                  if (isCameraTracking()) {
                    updateCameraForTracking(mapView, point)
                  }
                }
                .also { listener ->
                  locationComponent.addOnIndicatorPositionChangedListener(listener)
                }

        setupGestureListeners(mapView)
      }

      // Enable tracking when location becomes available
      enableCameraTracking()
    }
  }

  /** Disables location tracking when permission is not granted, and removes listeners. */
  private fun disableLocationTracking() {
    internalMapView?.let { mapView ->
      val locationComponent: LocationComponentPlugin = mapView.location
      locationComponent.updateSettings {
        enabled = false
        pulsingEnabled = false
      }

      // Remove listener
      indicatorListener?.let { listener ->
        locationComponent.removeOnIndicatorPositionChangedListener(listener)
      }
      indicatorListener = null

      disableCameraTracking()
    }
  }

  /**
   * Updates the annotations on the map by applying clustering logic based on the current zoom
   * level.
   *
   * @param mapView The current [MapView] instance.
   */
  fun updateAnnotations(mapView: MapView) {
    val events = _uiState.value.events
    if (events.isEmpty()) {
      clearAnnotationManager()
      return
    }

    viewModelScope.launch {
      // 1. Stack events at the exact same location
      val stackedItems = MapClustering.stackSameLocationEvents(events)
      // 2. Cluster stacks based on zoom level distance
      val itemsDisplay = MapClustering.clusterItemsForZoom(stackedItems, currentZoomLevel)

      clearAnnotationManager()
      val pointAnnotationManager = getOrCreatePointAnnotationManager(mapView)

      itemsDisplay.forEach { item ->
        val annotationOptions = PointAnnotationOptions().withPoint(item.point)

        when (item) {
          is MapRenderItem.Single -> {
            annotationOptions
                .withIconImage(AnnotationConfig.PIN_ICON)
                .withIconSize(AnnotationConfig.PIN_SIZE)
                .withData(JsonPrimitive(item.id)) // Store eventId
          }
          is MapRenderItem.Cluster -> {
            val bitmap = PinBitmapGenerator.generateClusterBitmap(item.count)
            annotationOptions
                .withIconImage(bitmap)
                .withData(JsonPrimitive(item.id)) // Store clusterId
          }
        }
        pointAnnotationManager.create(annotationOptions)
      }
      setupAnnotationClickListeners(pointAnnotationManager, itemsDisplay, mapView)
    }
  }

  /**
   * Set up click listeners for annotations and the map background.
   * - Annotation click handles selecting an event/cluster or zooming in on a cluster.
   * - Map click clears the selected event.
   *
   * @param pointAnnotationManager The manager for map annotations.
   * @param items The list of [MapRenderItem]s currently displayed.
   * @param mapView The current [MapView] instance.
   */
  @SuppressLint("ImplicitSamInstance")
  private fun setupAnnotationClickListeners(
      pointAnnotationManager: PointAnnotationManager,
      items: List<MapRenderItem>,
      mapView: MapView
  ) {
    // Remove previous click listeners before adding new ones
    pointAnnotationManager.removeClickListener { true }
    val mapClickListener = OnMapClickListener { _ ->
      clearSelectedEvent()
      true
    }
    mapView.gestures.removeOnMapClickListener(mapClickListener)

    pointAnnotationManager.addClickListener { annotation ->
      val idJson = annotation.getData()?.asString ?: return@addClickListener false
      val clickedItem = items.find { it.id == idJson }

      when (clickedItem) {
        is MapRenderItem.Single -> {
          selectEvent(clickedItem.event)
        }
        is MapRenderItem.Cluster -> {
          // Check if this cluster is a "Stack" (same location) OR if we are fully zoomed in
          val isStack = clickedItem.events.distinctBy { it.location?.coordinates }.size == 1
          val isMaxZoom = currentZoomLevel > CameraConfig.MAX_ZOOM_THRESHOLD

          if (isStack || isMaxZoom) {
            // Open selection menu for these events (stack/fully zoomed cluster)
            selectEventGroup(clickedItem.events)
          } else {
            // Zoom in further on the cluster location
            val newZoom = currentZoomLevel + 2.0
            mapView.mapboxMap.easeTo(
                CameraOptions.Builder().center(clickedItem.point).zoom(newZoom).build(),
                MapAnimationOptions.mapAnimationOptions { duration(500) })
          }
        }
        null -> {}
      }
      true
    }
    mapView.gestures.addOnMapClickListener(mapClickListener)
  }
}
