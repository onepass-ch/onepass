package ch.onepass.onepass.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.ui.event.EventCard
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures

object MapScreenTestTags {
  const val MAPBOX_MAP_SCREEN = "mapScreen"
  const val RECENTER_BUTTON = "recenterButton"
}

/**
 * A Composable function that displays a Mapbox map, covering the entire screen.Includes a floating
 * action button to recenter the camera on the user's location puck.
 *
 * @param mapViewModel The ViewModel responsible for the map's logic and state. It provides the
 *   initial camera options and handles map setup events. Defaults to a new instance provided by
 *   `viewModel()`.
 * @param isLocationPermissionGranted A boolean flag indicating whether the user has granted
 *   location permissions. This is passed to the ViewModel to determine if the user's location can
 *   be displayed on the map.
 */
@Composable
fun MapScreen(mapViewModel: MapViewModel = viewModel(), isLocationPermissionGranted: Boolean) {
  val uiState by mapViewModel.uiState.collectAsState()
  val events = uiState.events

  Box(modifier = Modifier.fillMaxSize()) {

    // --- Mapbox Map Composable ---
    MapboxMap(
        modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.MAPBOX_MAP_SCREEN),
    ) {
      // MapEffect bridges the Compose Mapbox API with the MapView
      MapEffect(Unit) { mapView: MapView ->
        mapViewModel.onMapReady(mapView, isLocationPermissionGranted)
      }
      MapEffect(events) { mapView: MapView -> setupAnnotations(mapView, events, mapViewModel) }
    }

    // --- Floating Recenter Button ---
    FloatingActionButton(
        onClick = { mapViewModel.recenterCamera() },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag(MapScreenTestTags.RECENTER_BUTTON),
    ) {
      Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Recenter")
    }

    uiState.selectedEvent?.let { event ->
      Box(modifier = Modifier.fillMaxSize().clickable { mapViewModel.clearSelectedEvent() }) {
        EventCard(
            event = event,
            onCardClick = { /* TODO: Navigate to full event page */},
            onDismiss = { mapViewModel.clearSelectedEvent() },
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
        )
      }
    }
  }
}

private fun setupAnnotations(mapView: MapView, events: List<Event>, viewModel: MapViewModel) {
  val annotationPlugin = mapView.annotations
  val pointAnnotationManager = annotationPlugin.createPointAnnotationManager()

  // Clear existing annotations
  pointAnnotationManager.deleteAll()

  // Create new annotations
  events.forEach { event ->
    val coords = event.location?.coordinates ?: return@forEach
    val point = Point.fromLngLat(coords.longitude, coords.latitude)

    val pin =
        PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("purple-pin")
            .withIconSize(0.6)
            .withData(JsonPrimitive(event.eventId))

    pointAnnotationManager.create(pin)
  }

  // Remove existing click listeners first
  pointAnnotationManager.removeClickListener { true } // Remove all
  // --- Add click listener for pins ---
  pointAnnotationManager.addClickListener { annotation ->
    val eventIdJson = annotation.getData()
    val eventId = eventIdJson?.asString
    eventId?.let { id ->
      val selectedEvent = events.find { it.eventId == id }
      selectedEvent?.let { event -> viewModel.selectEvent(event) }
    }
    true
  }

  // Remove existing map click listeners first
  mapView.gestures.removeOnMapClickListener { true } // Remove all
  // --- Add map click listener for pins ---
  mapView.gestures.addOnMapClickListener {
    viewModel.clearSelectedEvent()
    true
  }
}
