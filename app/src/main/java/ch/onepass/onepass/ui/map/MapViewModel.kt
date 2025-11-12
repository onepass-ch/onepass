package ch.onepass.onepass.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
  import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.repository.RepositoryProvider
import ch.onepass.onepass.ui.eventfilters.EventFilteringUtils.applyFiltersLocally
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.Plugin.Companion.MAPBOX_COMPASS_PLUGIN_ID
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
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
    val showFilterDialog: Boolean = false
)

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
  private var internalMapView: MapView? = null
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

  /** Apply current filters to the loaded events */
  fun applyFiltersToCurrentEvents(filters: EventFilters) {
    val filteredEvents = applyFiltersLocally(_allEvents.value, filters)
    _uiState.value = _uiState.value.copy(events = filteredEvents)
  }

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
  private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
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

  /** Return a single PointAnnotationManager for the given MapView, creating it once if needed. */
  fun getOrCreatePointAnnotationManager(mapView: MapView): PointAnnotationManager {
    // If we already created one earlier, reuse it. Otherwise, create and store it.
    return pointAnnotationManager
        ?: mapView.annotations.createPointAnnotationManager().also { manager ->
          pointAnnotationManager = manager
        }
  }

  /** Optionally clear and release the manager. */
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

  /** Enables location tracking if permission is granted. */
  fun enableLocationTracking() {
    internalMapView?.let { enableLocationTracking(it) }
  }

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

  /** Configures gesture and compass plugins for the MapView. */
  private fun configurePlugins(mapView: MapView) {
    mapView.gestures.updateSettings {
      rotateEnabled = true
      pinchToZoomEnabled = true
    }

    val compassPlugin = mapView.getPlugin<CompassPlugin>(MAPBOX_COMPASS_PLUGIN_ID)
    compassPlugin?.updateSettings { enabled = true }
  }

  override fun onCleared() {
    indicatorListener?.let { listener ->
      internalMapView?.location?.removeOnIndicatorPositionChangedListener(listener)
    }
    clearAnnotationManager()
    internalMapView = null
    super.onCleared()
  }
}
