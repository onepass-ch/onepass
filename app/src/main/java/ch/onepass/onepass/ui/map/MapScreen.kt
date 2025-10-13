package ch.onepass.onepass.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap

object MapScreenTestTags {
  const val MAPBOX_MAP_SCREEN = "mapScreen"
  const val RECENTER_BUTTON = "recenterButton"
}

/**
 * A Composable function that displays a Mapbox map, covering the entire screen.
 *
 * @param mapViewModel The ViewModel responsible for the map's logic and state. It provides the
 *   initial camera options and handles map setup events. Defaults to a new instance provided by
 *   `viewModel()`.
 * @param isLocationPermissionGranted A boolean flag indicating whether the user has granted
 *   location permissions. This is passed to the ViewModel to determine if the user's location can
 *   be displayed on the map.
 */
/**
 * A Composable function that displays a Mapbox map, covering the entire screen. Includes a floating
 * action button to recenter the camera on the user's location puck.
 */
@Composable
fun MapScreen(mapViewModel: MapViewModel = viewModel(), isLocationPermissionGranted: Boolean) {
  Box(modifier = Modifier.fillMaxSize()) {

    // --- Mapbox Map Composable ---
    MapboxMap(
        modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.MAPBOX_MAP_SCREEN),
    ) {
      // MapEffect bridges the Compose Mapbox API with the MapView
      MapEffect(Unit) { mapView: MapView ->
        mapViewModel.onMapReady(mapView, isLocationPermissionGranted)
      }
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
  }
}
