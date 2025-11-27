package ch.onepass.onepass.ui.map

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.repository.RepositoryProvider
import ch.onepass.onepass.ui.eventfilters.EventFilteringUtils.applyFiltersLocally
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
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
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUIState(
    val events: List<Event> = emptyList(),
    val selectedEvent: Event? = null,
    val showFilterDialog: Boolean = false,
    val hasLocationPermission: Boolean = false
)

private object AnnotationConfig {
  const val PIN_ICON = "purple-pin"
  const val PIN_SIZE = 0.6
}

/**
 * ViewModel for managing Mapbox map state and events.
 *
 * Responsibilities include:
 * - Fetching and filtering published events with valid coordinates
 * - Tracking the selected event when a map pin is clicked
 * - Handling Mapbox MapView lifecycle and plugins
 * - Enabling location tracking and recentering camera
 *
 * @param eventRepository Repository providing access to [Event] data.
 */
class MapViewModel(
    private val eventRepository: EventRepository = RepositoryProvider.eventRepository,
) : ViewModel() {
  companion object {
    object CameraConfig {
      const val DEFAULT_LATITUDE = 46.5197
      const val DEFAULT_LONGITUDE = 6.6323
      const val DEFAULT_ZOOM = 7.0
      const val RECENTER_ZOOM = 15.0
    }

    object AnimationConfig {
      const val DURATION_MS = 1000L
    }

    object MapStyle {
      const val URI = "mapbox://styles/walid-as/cmhmmxczk00ar01shdw6r8lel"
    }

    object CoordinateLimits {
      const val MIN_LATITUDE = -90.0
      const val MAX_LATITUDE = 90.0
      const val MIN_LONGITUDE = -180.0
      const val MAX_LONGITUDE = 180.0
    }
  }

  // --- UI state ---
  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  // --- All events ---
  private val _allEvents = MutableStateFlow<List<Event>>(emptyList())
  val allEvents: StateFlow<List<Event>> = _allEvents.asStateFlow()

  // --- Reusable PointAnnotationManager ---
  private var pointAnnotationManager: PointAnnotationManager? = null

  // --- Mapbox references ---
  /**
   * Holds reference to MapView for plugin access and camera control.
   *
   * **Memory Leak Safety:** StaticFieldLeak suppression is safe here because:
   * 1. Reference is explicitly nulled in onCleared() (line 285)
   * 2. All listeners are removed before clearing (lines 281-283)
   * 3. Annotation manager is cleaned up (line 284)
   *
   * This ensures no Context leaks persist beyond ViewModel lifecycle.
   *
   * @see onCleared for cleanup implementation
   */
  @SuppressLint("StaticFieldLeak") private var internalMapView: MapView? = null

  private var lastKnownPoint: Point? = null
  private var indicatorListener: OnIndicatorPositionChangedListener? = null
  private val defaultCenterPoint =
      Point.fromLngLat(CameraConfig.DEFAULT_LONGITUDE, CameraConfig.DEFAULT_LATITUDE)
  val initialCameraOptions: CameraOptions =
      CameraOptions.Builder()
          .center(defaultCenterPoint) // Lausanne
          .zoom(CameraConfig.DEFAULT_ZOOM)
          .build()

  init {
    fetchPublishedEvents()
  }

  // --- Event handling ---
  /**
   * Fetches all published events and updates [_uiState]. Filters out events with invalid or null
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
        _uiState.value = _uiState.value.copy(events = validEvents)
      }
    }
  }

  /**
   * Apply the given filters to the currently loaded events and update UI state.
   *
   * @param filters Filters to apply to the list of loaded events.
   */
  fun applyFiltersToCurrentEvents(filters: EventFilters) {
    val filteredEvents = applyFiltersLocally(_allEvents.value, filters)
    _uiState.value = _uiState.value.copy(events = filteredEvents)
  }

  /**
   * Sets the visibility of the filter dialog.
   *
   * @param show true to show the filter dialog, false to hide it.
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
   * Selects an event when a pin is clicked. Clicking the same event twice clears the selection.
   *
   * @param event The [Event] to select.
   */
  fun selectEvent(event: Event) {
    val currentSelectedEvent = _uiState.value.selectedEvent
    if (currentSelectedEvent?.eventId == event.eventId) {
      // Same event clicked twice - toggle it off
      clearSelectedEvent()
    } else {
      // Different event or no event selected - select this one
      _uiState.value = _uiState.value.copy(selectedEvent = event)
    }
  }

  /** Clears the currently selected event. */
  fun clearSelectedEvent() {
    _uiState.value = _uiState.value.copy(selectedEvent = null)
  }

  /** Refreshes events manually by fetching from repository. */
  fun refreshEvents() {
    fetchPublishedEvents()
  }

  /**
   * Return a single PointAnnotationManager for the given MapView, creating it once if needed.
   *
   * @param mapView The MapView to create or retrieve the PointAnnotationManager for.
   * @return The PointAnnotationManager instance.
   */
  fun getOrCreatePointAnnotationManager(mapView: MapView): PointAnnotationManager {
    // If we already created one earlier, reuse it. Otherwise, create and store it.
    return pointAnnotationManager
        ?: mapView.annotations.createPointAnnotationManager().also { manager ->
          pointAnnotationManager = manager
        }
  }

  /** Clears and releases the stored PointAnnotationManager, if any. */
  fun clearAnnotationManager() {
    pointAnnotationManager?.let {
      try {
        it.deleteAll()
      } catch (_: Throwable) {}
    }
    pointAnnotationManager = null
  }

  // --- Mapbox integration ---
  /**
   * Initializes MapView, loads style, configures plugins, and optionally enables location tracking.
   *
   * @param mapView The MapView instance to initialize.
   * @param hasLocationPermission whether location permission is currently granted.
   */
  open fun onMapReady(mapView: MapView, hasLocationPermission: Boolean) {
    if (internalMapView == mapView) return
    internalMapView = mapView

    mapView.mapboxMap.loadStyle(MapStyle.URI) {
      configurePlugins(mapView)

      if (hasLocationPermission) {
        enableLocationTracking(mapView)
      }

      mapView.mapboxMap.setCamera(initialCameraOptions)
    }
  }

  /**
   * Enables the location component on the map and sets up the indicator listener.
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
        OnIndicatorPositionChangedListener { point -> lastKnownPoint = point }
            .also { listener -> locationComponent.addOnIndicatorPositionChangedListener(listener) }
  }

  /**
   * Recenters the camera on the last known user location. Does nothing if no location is available
   * or coordinates are invalid.
   */
  open fun recenterCamera() {
    val mapView = internalMapView ?: return
    val mapboxMap = mapView.mapboxMap
    val point = lastKnownPoint ?: return

    // Ensure point has valid coordinates
    if (!isValidCoordinate(point.latitude(), point.longitude())) {
      return
    }

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
    }

    val compassPlugin = mapView.getPlugin<CompassPlugin>(MAPBOX_COMPASS_PLUGIN_ID)
    compassPlugin?.updateSettings { enabled = true }
  }

  /**
   * Cleans up listeners and releases MapView-related resources when ViewModel is cleared.
   *
   * This prevents memory leaks by:
   * 1. Removing location indicator listener to break callback chain
   * 2. Clearing annotation manager and all its annotations
   * 3. Nulling MapView reference to release Context
   */
  public override fun onCleared() {
    // Remove listener to break callback chain
    indicatorListener?.let { listener ->
      internalMapView?.location?.removeOnIndicatorPositionChangedListener(listener)
    }
    indicatorListener = null

    // Clear annotation manager and its resources
    clearAnnotationManager()

    // Release MapView reference (prevents Context leak)
    internalMapView = null

    super.onCleared()
  }

  /** Updates the location permission flag and enables/disables location tracking accordingly. */
  fun setLocationPermission(granted: Boolean) {
    _uiState.update { it.copy(hasLocationPermission = granted) }
    if (granted) {
      enableLocationTracking()
    } else {
      disableLocationTracking()
    }
  }

  /** Enables location tracking if permission is granted */
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
            OnIndicatorPositionChangedListener { point -> lastKnownPoint = point }
                .also { listener ->
                  locationComponent.addOnIndicatorPositionChangedListener(listener)
                }
      }
    }
  }

  /** Disables location tracking when permission is not granted */
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
    }
  }

  /** Improved annotation setup that ensures annotations are recreated */
  fun setupAnnotationsForEvents(events: List<Event>, mapView: MapView) {
    viewModelScope.launch {
      // Clear existing annotations first
      clearAnnotationManager()

      // Get or create annotation manager
      val pointAnnotationManager = getOrCreatePointAnnotationManager(mapView)

      // Create new annotations
      events.forEach { event ->
        val coords = event.location?.coordinates ?: return@forEach
        val point = Point.fromLngLat(coords.longitude, coords.latitude)

        val pin =
            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(AnnotationConfig.PIN_ICON)
                .withIconSize(AnnotationConfig.PIN_SIZE)
                .withData(JsonPrimitive(event.eventId))

        pointAnnotationManager.create(pin)
      }

      // Set up click listeners
      setupAnnotationClickListeners(pointAnnotationManager, events, mapView)
    }
  }

  /** Set up click listeners for annotations and map */
  private fun setupAnnotationClickListeners(
      pointAnnotationManager: PointAnnotationManager,
      events: List<Event>,
      mapView: MapView
  ) {
    // Remove existing click listeners first
    pointAnnotationManager.removeClickListener { true }
    mapView.gestures.removeOnMapClickListener { true }

    // Add click listener for pins
    pointAnnotationManager.addClickListener { annotation ->
      val eventIdJson = annotation.getData()
      val eventId = eventIdJson?.asString
      eventId?.let { id ->
        val selectedEvent = events.find { it.eventId == id }
        selectedEvent?.let { event -> selectEvent(event) }
      }
      true
    }

    // Add map click listener to clear selection
    mapView.gestures.addOnMapClickListener {
      clearSelectedEvent()
      true
    }
  }
}
