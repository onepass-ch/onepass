package ch.onepass.onepass.ui.map

import androidx.lifecycle.ViewModel
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

open class MapViewModel : ViewModel() {

  companion object {
    // Default camera configuration
    private const val DEFAULT_LATITUDE = 46.5197
    private const val DEFAULT_LONGITUDE = 6.6323
    private const val DEFAULT_ZOOM = 13.0
  }

  private var internalMapView: MapView? = null
  private var lastKnownPoint: Point? = null
  private var indicatorListener: OnIndicatorPositionChangedListener? = null

  private val defaultCenterPoint = Point.fromLngLat(DEFAULT_LONGITUDE, DEFAULT_LATITUDE)
  val initialCameraOptions: CameraOptions =
      CameraOptions.Builder()
          .center(defaultCenterPoint) // Lausanne
          .zoom(DEFAULT_ZOOM)
          .build()

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
