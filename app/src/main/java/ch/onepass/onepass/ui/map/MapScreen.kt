package ch.onepass.onepass.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.event.EventCard
import ch.onepass.onepass.ui.event.EventCardViewModel
import ch.onepass.onepass.ui.eventfilters.ActiveFiltersBar
import ch.onepass.onepass.ui.eventfilters.EventFilterViewModel
import ch.onepass.onepass.ui.eventfilters.FilterDialog
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap

/** Wrapper for EventCardPopup state to satisfy SonarCloud parameter limits. */
private data class EventPopupData(
    val currentEvent: ch.onepass.onepass.model.event.Event?,
    val selectedEventGroup: List<ch.onepass.onepass.model.event.Event>,
    val selectedEventIndex: Int,
    val likedEvents: Set<String>
)

/** Wrapper for EventCardPopup callbacks to satisfy SonarCloud parameter limits. */
private data class EventPopupActions(
    val onNavigateToEvent: (String) -> Unit,
    val onLikeToggle: (String) -> Unit,
    val onClearSelection: () -> Unit,
    val onSelectNext: () -> Unit,
    val onSelectPrevious: () -> Unit
)

/** A set of test tags for the MapScreen composable elements. */
object MapScreenTestTags {
  const val MAPBOX_MAP_SCREEN = "mapScreen"
  const val RECENTER_BUTTON = "recenterButton"
  const val FILTER_BUTTON = "filterButton"
  const val ACTIVE_FILTERS_BAR = "activeFiltersBar"
  const val EVENT_CARD = "eventCard"
  const val TRACKING_INDICATOR = "trackingIndicator"
  const val CLUSTER_NAV_PREV = "clusterNavPrev"
  const val CLUSTER_NAV_NEXT = "clusterNavNext"
  const val CLUSTER_NAV_LABEL = "clusterNavLabel"
}

private object TrackingIndicatorDimensions {
  val SIZE = 10.dp
  val BORDER_WIDTH = 2.dp
  val OFFSET_X = 4.dp
  val OFFSET_Y = (-4).dp
}

/**
 * A Composable function that displays a Mapbox map, covering the entire screen with automatic
 * camera tracking and gesture handling. Includes floating action buttons to recenter the camera and
 * access filters.
 *
 * Features:
 * - Automatic camera tracking on initial load (when permission is granted)
 * - Camera follows user location with smooth animation
 * - Tracking disabled when user pans, zooms, or rotates the map
 * - Recenter button re-enables tracking and animates back to user location
 * - Visual indicator shows tracking state
 * - Displays a floating [EventCard] for a single event or the current event in a cluster.
 * - Displays a [FilterDialog] and [ActiveFiltersBar] to manage event filtering.
 *
 * @param modifier Optional modifier for the map screen.
 * @param mapViewModel The ViewModel responsible for the map's logic and state.
 * @param filterViewModel The ViewModel responsible for managing and applying event filters.
 * @param onNavigateToEvent Callback invoked when an event card is clicked, receives eventId.
 * @param eventCardViewModel The ViewModel responsible for managing event-card specific state, like
 *   liked status.
 */
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel(),
    filterViewModel: EventFilterViewModel = viewModel(),
    onNavigateToEvent: (String) -> Unit = {},
    eventCardViewModel: EventCardViewModel = viewModel()
) {
  val context = LocalContext.current
  val uiState by mapViewModel.uiState.collectAsState()
  val likedEvents by eventCardViewModel.likedEvents.collectAsState()
  val currentFilters by filterViewModel.currentFilters.collectAsState()
  val allEvents by mapViewModel.allEvents.collectAsState()

  // Location permission handling
  LocationPermissionHandler(mapViewModel = mapViewModel)

  // Clear selection and apply filters on mount
  LaunchedEffect(Unit) { mapViewModel.clearSelectedEvent() }
  LaunchedEffect(currentFilters, allEvents) {
    mapViewModel.applyFiltersToCurrentEvents(currentFilters)
  }

  Box(modifier = modifier.fillMaxSize()) {
    // Mapbox Map
    MapboxMap(
        modifier = modifier.fillMaxSize().testTag(MapScreenTestTags.MAPBOX_MAP_SCREEN),
    ) {
      MapEffect(Unit) { mapView: MapView ->
        mapViewModel.onMapReady(mapView, uiState.hasLocationPermission)
        mapViewModel.getOrCreatePointAnnotationManager(mapView)
        setupAnnotations(mapView, mapViewModel)
      }
      MapEffect(uiState.events) { mapView: MapView -> setupAnnotations(mapView, mapViewModel) }
    }

    // Floating action buttons
    RecenterButton(
        modifier = modifier,
        isCameraTracking = uiState.isCameraTracking,
        onRecenter = { mapViewModel.recenterCamera() })

    FilterButton(modifier = modifier, onFilterClick = { mapViewModel.setShowFilterDialog(true) })

    // Event card popup
    EventCardPopup(
        data =
            EventPopupData(
                currentEvent = uiState.currentSelectedEvent,
                selectedEventGroup = uiState.selectedEventGroup,
                selectedEventIndex = uiState.selectedEventIndex,
                likedEvents = likedEvents),
        actions =
            EventPopupActions(
                onNavigateToEvent = onNavigateToEvent,
                onLikeToggle = { eventId -> eventCardViewModel.toggleLike(eventId) },
                onClearSelection = { mapViewModel.clearSelectedEvent() },
                onSelectNext = { mapViewModel.selectNextEvent() },
                onSelectPrevious = { mapViewModel.selectPreviousEvent() }))

    // Filter dialog
    if (uiState.showFilterDialog) {
      LaunchedEffect(Unit) { filterViewModel.updateLocalFilters(currentFilters) }
      FilterDialog(
          viewModel = filterViewModel,
          onApply = { newFilters ->
            filterViewModel.applyFilters(newFilters)
            mapViewModel.setShowFilterDialog(false)
          },
          onDismiss = { mapViewModel.setShowFilterDialog(false) })
    }

    // Active filters bar
    if (currentFilters.hasActiveFilters) {
      ActiveFiltersBar(
          filters = currentFilters,
          onClearFilters = { filterViewModel.clearFilters() },
          modifier =
              modifier
                  .fillMaxWidth()
                  .padding(top = 56.dp)
                  .testTag(MapScreenTestTags.ACTIVE_FILTERS_BAR))
    }
  }
}

/**
 * Handles location permission request on initial load.
 *
 * @param mapViewModel The ViewModel to notify about permission status.
 */
@Composable
private fun LocationPermissionHandler(mapViewModel: MapViewModel) {
  val context = LocalContext.current
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        mapViewModel.setLocationPermission(isGranted)
      }

  LaunchedEffect(Unit) {
    val hasPermission =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    mapViewModel.setLocationPermission(hasPermission)
    if (!hasPermission) {
      launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
  }
}

/**
 * Recenter button with tracking indicator.
 *
 * @param modifier Modifier for the button.
 * @param isCameraTracking Whether camera tracking is currently enabled.
 * @param onRecenter Callback when button is clicked.
 */
@Composable
private fun BoxScope.RecenterButton(
    modifier: Modifier,
    isCameraTracking: Boolean,
    onRecenter: () -> Unit
) {
  FloatingActionButton(
      onClick = onRecenter,
      containerColor = colorScheme.primary,
      contentColor = colorScheme.onBackground,
      modifier =
          modifier
              .align(Alignment.BottomEnd)
              .padding(16.dp)
              .testTag(MapScreenTestTags.RECENTER_BUTTON)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Recenter")
          if (isCameraTracking) {
            Box(
                modifier =
                    Modifier.size(TrackingIndicatorDimensions.SIZE)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                        .border(
                            TrackingIndicatorDimensions.BORDER_WIDTH,
                            MaterialTheme.colorScheme.surface,
                            CircleShape)
                        .align(Alignment.TopEnd)
                        .offset(
                            x = TrackingIndicatorDimensions.OFFSET_X,
                            y = TrackingIndicatorDimensions.OFFSET_Y)
                        .testTag(MapScreenTestTags.TRACKING_INDICATOR))
          }
        }
      }
}

/**
 * Filter button.
 *
 * @param modifier Modifier for the button.
 * @param onFilterClick Callback when button is clicked.
 */
@Composable
private fun BoxScope.FilterButton(modifier: Modifier, onFilterClick: () -> Unit) {
  FloatingActionButton(
      onClick = onFilterClick,
      containerColor = colorScheme.primary,
      contentColor = colorScheme.onBackground,
      modifier =
          modifier
              .align(Alignment.BottomEnd)
              .padding(bottom = 88.dp, end = 16.dp)
              .size(55.dp)
              .testTag(MapScreenTestTags.FILTER_BUTTON)) {
        Icon(
            painter = painterResource(id = R.drawable.filter_icon),
            contentDescription = "Filter events",
            modifier = Modifier.size(24.dp).align(Alignment.Center))
      }
}

/**
 * Displays an overlay popup containing details of a selected event or cluster.
 *
 * Requires [BoxScope] to fill the screen and intercept clicks.
 *
 * @param data Container for state data (event details, cluster index, etc.).
 * @param actions Container for callback actions (navigation, selection cycling).
 */
@Composable
private fun BoxScope.EventCardPopup(data: EventPopupData, actions: EventPopupActions) {
  val currentEvent = data.currentEvent ?: return

  Box(
      modifier =
          Modifier.fillMaxSize()
              .clickable { actions.onClearSelection() }
              .testTag(MapScreenTestTags.EVENT_CARD)) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              EventCard(
                  event = currentEvent,
                  onCardClick = { actions.onNavigateToEvent(currentEvent.eventId) },
                  modifier = Modifier.fillMaxWidth(),
                  isLiked = data.likedEvents.contains(currentEvent.eventId),
                  onLikeToggle = actions.onLikeToggle)

              if (data.selectedEventGroup.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                ClusterControls(
                    currentIndex = data.selectedEventIndex,
                    totalCount = data.selectedEventGroup.size,
                    onPrev = actions.onSelectPrevious,
                    onNext = actions.onSelectNext)
              }
            }
      }
}

/**
 * Renders navigation controls for cycling through a cluster of events. Provides "Previous" and
 * "Next" buttons and displays the current index out of the total count.
 *
 * @param currentIndex The 0-based index of the currently displayed event in the cluster.
 * @param totalCount The total number of events in the cluster.
 * @param onPrev Callback invoked when the user requests the previous event.
 * @param onNext Callback invoked when the user requests the next event.
 */
@Composable
fun ClusterControls(currentIndex: Int, totalCount: Int, onPrev: () -> Unit, onNext: () -> Unit) {
  Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 4.dp,
      modifier = Modifier.wrapContentSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
              IconButton(
                  onClick = onPrev,
                  modifier = Modifier.testTag(MapScreenTestTags.CLUSTER_NAV_PREV)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
                  }

              Text(
                  text = "${currentIndex + 1} / $totalCount",
                  style = MaterialTheme.typography.labelLarge,
                  modifier =
                      Modifier.padding(horizontal = 16.dp)
                          .testTag(MapScreenTestTags.CLUSTER_NAV_LABEL))

              IconButton(
                  onClick = onNext,
                  modifier = Modifier.testTag(MapScreenTestTags.CLUSTER_NAV_NEXT)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                  }
            }
      }
}

/**
 * Helper function to call the view model's annotation update function.
 *
 * @param mapView The [MapView] instance.
 * @param viewModel The [MapViewModel] instance.
 */
@android.annotation.SuppressLint("ImplicitSamInstance")
private fun setupAnnotations(mapView: MapView, viewModel: MapViewModel) {
  viewModel.updateAnnotations(mapView)
}
