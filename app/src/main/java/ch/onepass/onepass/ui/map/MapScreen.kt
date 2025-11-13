package ch.onepass.onepass.ui.map

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.ui.event.EventCard
import ch.onepass.onepass.ui.event.EventCardViewModel
import ch.onepass.onepass.ui.eventfilters.ActiveFiltersBar
import ch.onepass.onepass.ui.eventfilters.EventFilterViewModel
import ch.onepass.onepass.ui.eventfilters.FilterDialog
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.gestures.gestures

object MapScreenTestTags {
  const val MAPBOX_MAP_SCREEN = "mapScreen"
  const val RECENTER_BUTTON = "recenterButton"
  const val FILTER_BUTTON = "filterButton"
  const val ACTIVE_FILTERS_BAR = "activeFiltersBar"
  const val EVENT_CARD = "eventCard"
}

private object AnnotationConfig {
  const val PIN_ICON = "purple-pin"
  const val PIN_SIZE = 0.6
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
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel(),
    filterViewModel: EventFilterViewModel = viewModel(),
    onNavigateToEvent: (String) -> Unit = {},
    isLocationPermissionGranted: Boolean
) {
  val uiState by mapViewModel.uiState.collectAsState()
  val eventCardViewModel = EventCardViewModel.getInstance()
  val likedEvents by eventCardViewModel.likedEvents.collectAsState()
  val currentFilters by filterViewModel.currentFilters.collectAsState()
  val showFilterDialog = uiState.showFilterDialog
  val allEvents by mapViewModel.allEvents.collectAsState()

  Box(modifier = modifier.fillMaxSize()) {
    // --- Mapbox Map Composable ---
    MapboxMap(
        modifier = modifier.fillMaxSize().testTag(MapScreenTestTags.MAPBOX_MAP_SCREEN),
    ) {
      // MapEffect bridges the Compose Mapbox API with the MapView
      MapEffect(Unit) { mapView: MapView ->
        mapViewModel.onMapReady(mapView, isLocationPermissionGranted)
        mapViewModel.getOrCreatePointAnnotationManager(mapView)
      }
      MapEffect(uiState.events) { mapView: MapView ->
        setupAnnotations(mapView, uiState.events, mapViewModel)
      }
    }

    LaunchedEffect(currentFilters, allEvents) {
      mapViewModel.applyFiltersToCurrentEvents(currentFilters)
    }

    // --- Floating Recenter Button ---
    FloatingActionButton(
        onClick = { mapViewModel.recenterCamera() },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier =
            modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag(MapScreenTestTags.RECENTER_BUTTON),
    ) {
      Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Recenter")
    }

    // --- Floating Filter Button ---
    FloatingActionButton(
        onClick = { mapViewModel.setShowFilterDialog(true) },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier =
            modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 88.dp, end = 16.dp)
                .size(55.dp)
                .testTag(MapScreenTestTags.FILTER_BUTTON),
    ) {
      Icon(
          painter = painterResource(id = R.drawable.filter_icon),
          contentDescription = "Filter events",
          modifier = modifier.padding(4.dp))
    }

    uiState.selectedEvent?.let { event ->
      Box(
          modifier =
              modifier
                  .fillMaxSize()
                  .clickable { mapViewModel.clearSelectedEvent() }
                  .testTag(MapScreenTestTags.EVENT_CARD)) {
            EventCard(
                event = event,
                onCardClick = { onNavigateToEvent(event.eventId) },
                onDismiss = { mapViewModel.clearSelectedEvent() },
                modifier = modifier.align(Alignment.TopCenter).padding(16.dp),
                isLiked = likedEvents.contains(event.eventId),
                onLikeToggle = { eventId -> eventCardViewModel.toggleLike(eventId) })
          }
    }

    // --- Filter Dialog ---
    if (showFilterDialog) {
      LaunchedEffect(Unit) { filterViewModel.updateLocalFilters(currentFilters) }

      FilterDialog(
          viewModel = filterViewModel,
          onApply = { newFilters ->
            filterViewModel.applyFilters(newFilters)
            mapViewModel.setShowFilterDialog(false)
          },
          onDismiss = { mapViewModel.setShowFilterDialog(false) },
      )
    }
    // --- Active Filters Bar ---
    if (currentFilters.hasActiveFilters) {
      ActiveFiltersBar(
          filters = currentFilters,
          onClearFilters = { filterViewModel.clearFilters() },
          modifier =
              modifier
                  .fillMaxWidth()
                  .padding(top = 56.dp)
                  .testTag(MapScreenTestTags.ACTIVE_FILTERS_BAR),
      )
    }
  }
}

/**
 * Sets up map annotations (pins) for events and configures click listeners.
 *
 * @param mapView The MapView instance to configure
 * @param events List of events to display as annotations
 * @param viewModel ViewModel for handling event selection
 */
@SuppressLint("ImplicitSamInstance")
private fun setupAnnotations(mapView: MapView, events: List<Event>, viewModel: MapViewModel) {
  val pointAnnotationManager = viewModel.getOrCreatePointAnnotationManager(mapView)

  // Clear existing annotations
  pointAnnotationManager.deleteAll()

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
