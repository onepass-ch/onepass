package ch.onepass.onepass.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.repository.RepositoryProvider
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.Plugin.Companion.MAPBOX_COMPASS_PLUGIN_ID
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.compass.CompassPlugin
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUIState(val events: List<Event> = emptyList(), val selectedEvent: Event? = null)

class MapViewModel(
    private val eventRepository: EventRepository = RepositoryProvider.repository,
) : ViewModel() {
  companion object {
    // Default camera configuration
    private const val DEFAULT_LATITUDE = 46.5197
    private const val DEFAULT_LONGITUDE = 6.6323
    private const val DEFAULT_ZOOM = 7.0
  }

  // --- UI state ---
  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  // --- Mapbox references ---
  private var internalMapView: MapView? = null
  private var lastKnownPoint: Point? = null
  private var indicatorListener: OnIndicatorPositionChangedListener? = null
  private val defaultCenterPoint = Point.fromLngLat(DEFAULT_LONGITUDE, DEFAULT_LATITUDE)
  val initialCameraOptions: CameraOptions =
      CameraOptions.Builder()
          .center(defaultCenterPoint) // Lausanne
          .zoom(DEFAULT_ZOOM)
          .build()

  init {
    fetchPublishedEvents()
  }

  // --- Event handling ---
  /** Fetches all published events and updates the UI state. */
  private fun fetchPublishedEvents() {
    viewModelScope.launch {
      eventRepository.getEventsByStatus(EventStatus.PUBLISHED).collect { events ->
        // Filter events with valid coordinates
        val validEvents =
            events.filter { event ->
              val coords = event.location?.coordinates
              coords != null && isValidCoordinate(coords.latitude, coords.longitude)
            }
        _uiState.value = _uiState.value.copy(events = validEvents)
      }
    }
  }

  // Coordinate validation helper
  private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
    return !latitude.isNaN() &&
        !longitude.isNaN() &&
        latitude in -90.0..90.0 &&
        longitude in -180.0..180.0
  }

  /**
   * Sets the selected event when a pin is clicked, or clears it if the same event is clicked again.
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

  /** Clears the selected event (e.g., when the card is dismissed). */
  fun clearSelectedEvent() {
    _uiState.value = _uiState.value.copy(selectedEvent = null)
  }

  /** Refresh events manually. */
  fun refreshEvents() {
    fetchPublishedEvents()
  }

  // --- Mapbox integration ---
  open fun onMapReady(mapView: MapView, hasLocationPermission: Boolean) {
    if (internalMapView == mapView) return
    internalMapView = mapView

    mapView.mapboxMap.loadStyleUri("mapbox://styles/walid-as/cmghzwo3h001501s358d677ye") {
      configurePlugins(mapView)

      if (hasLocationPermission) {
        enableLocationTracking(mapView)
      }

      mapView.mapboxMap.setCamera(initialCameraOptions)
    }
  }

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

  open fun recenterCamera() {
    val mapView = internalMapView ?: return
    val mapboxMap = mapView.mapboxMap
    val point = lastKnownPoint ?: return

    // Ensure point has valid coordinates
    if (!isValidCoordinate(point.latitude(), point.longitude())) {
      return
    }

    mapboxMap.easeTo(
        CameraOptions.Builder().center(point).zoom(15.0).build(),
        MapAnimationOptions.mapAnimationOptions { duration(1000L) },
    )
  }

  private fun configurePlugins(mapView: MapView) {
    mapView.gestures?.updateSettings {
      rotateEnabled = true
      pinchToZoomEnabled = true
    }

    val compassPlugin = mapView.getPlugin<CompassPlugin>(MAPBOX_COMPASS_PLUGIN_ID)
    compassPlugin?.updateSettings { enabled = true }
  }

  // --- Map lifecycle delegation ---
  fun onMapStart() = internalMapView?.onStart()

  fun onMapStop() = internalMapView?.onStop()

  fun onMapLowMemory() = internalMapView?.onLowMemory()

  override fun onCleared() {
    indicatorListener?.let { listener ->
      internalMapView?.location?.removeOnIndicatorPositionChangedListener(listener)
    }
    internalMapView?.onDestroy()
    internalMapView = null
    super.onCleared()
  }
}
