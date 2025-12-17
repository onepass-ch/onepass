package ch.onepass.onepass.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import ch.onepass.onepass.BuildConfig
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.eventfilters.EventFilterDialogTestTags
import ch.onepass.onepass.ui.eventfilters.EventFilterViewModel
import ch.onepass.onepass.ui.eventfilters.FilterUIState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.mapbox.common.MapboxOptions
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockMapViewModel: MapViewModel
  private lateinit var mockFilterViewModel: EventFilterViewModel

  private val testEvent1 =
      Event(
          eventId = "test-event-1",
          title = "Test Event",
          status = EventStatus.PUBLISHED,
          location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
          startTime = Timestamp.now(),
          ticketsRemaining = 50)

  private val testEvent2 =
      Event(
          eventId = "test-event-2",
          title = "Test Event 2",
          status = EventStatus.PUBLISHED,
          location = Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
          startTime = Timestamp.now(),
          ticketsRemaining = 50)

  @Before
  fun setUp() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

    mockMapViewModel = mockk(relaxed = true)
    mockFilterViewModel = mockk(relaxed = true)

    mockkStatic(ContextCompat::class)
    every {
      ContextCompat.checkSelfPermission(any(), eq(Manifest.permission.ACCESS_FINE_LOCATION))
    } returns PackageManager.PERMISSION_GRANTED
  }

  private fun setContent(
      uiState: MapUIState = MapUIState(events = listOf(testEvent1)),
      currentFilters: EventFilters = EventFilters()
  ) {
    every { mockMapViewModel.uiState } returns MutableStateFlow(uiState)
    every { mockMapViewModel.allEvents } returns MutableStateFlow(uiState.events)
    every { mockFilterViewModel.currentFilters } returns MutableStateFlow(currentFilters)
    every { mockFilterViewModel.uiState } returns MutableStateFlow(FilterUIState())

    composeTestRule.setContent {
      MapScreen(mapViewModel = mockMapViewModel, filterViewModel = mockFilterViewModel)
    }
  }

  @Test
  fun mapScreen_mainComponents_areDisplayed() {
    setContent()

    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertExists()
    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(MapScreenTestTags.FILTER_BUTTON).assertExists()
  }

  @Test
  fun mapScreen_whenEventSelected_showsEventCard() {
    // Set state with a selected group containing one event
    setContent(
        uiState =
            MapUIState(
                events = listOf(testEvent1),
                selectedEventGroup = listOf(testEvent1),
                selectedEventIndex = 0))

    composeTestRule.onNodeWithTag(C.Tag.event_card).assertExists()
    // Single event -> No navigation controls
    composeTestRule.onNodeWithTag(MapScreenTestTags.CLUSTER_NAV_PREV).assertDoesNotExist()
  }

  @Test
  fun mapScreen_whenClusterSelected_showsNavigationControls() {
    // Set state with selected group containing TWO events (cluster/stack)
    setContent(
        uiState =
            MapUIState(
                events = listOf(testEvent1, testEvent2),
                selectedEventGroup = listOf(testEvent1, testEvent2),
                selectedEventIndex = 0))

    composeTestRule.onNodeWithTag(C.Tag.event_card).assertExists()

    // Check for navigation controls
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.CLUSTER_NAV_PREV, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.CLUSTER_NAV_NEXT, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.CLUSTER_NAV_LABEL, useUnmergedTree = true)
        .assertExists()

    // Verify interactions call ViewModel
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.CLUSTER_NAV_NEXT, useUnmergedTree = true)
        .performClick()
    verify { mockMapViewModel.selectNextEvent() }

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.CLUSTER_NAV_PREV, useUnmergedTree = true)
        .performClick()
    verify { mockMapViewModel.selectPreviousEvent() }
  }

  @Test
  fun mapScreen_whenPermissionGranted_callsSetLocationPermission() {
    setContent()

    composeTestRule.waitForIdle()
    verify { mockMapViewModel.setLocationPermission(true) }
  }

  @Test
  fun mapScreen_whenComposed_clearsSelectedEvent() {
    setContent()

    composeTestRule.waitForIdle()
    verify { mockMapViewModel.clearSelectedEvent() }
  }

  @Test
  fun mapScreen_clickingFilterButton_showsFilterDialog() {
    setContent()

    composeTestRule.onNodeWithTag(MapScreenTestTags.FILTER_BUTTON).performClick()
    verify { mockMapViewModel.setShowFilterDialog(true) }
  }

  @Test
  fun mapScreen_clickingRecenterButton_callsRecenterCamera() {
    setContent()

    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).performClick()
    verify { mockMapViewModel.recenterCamera() }
  }

  @Test
  fun mapScreen_whenFiltersActive_showsActiveFiltersBar() {
    setContent(currentFilters = EventFilters(region = "Vaud", hideSoldOut = true))

    composeTestRule.onNodeWithTag(MapScreenTestTags.ACTIVE_FILTERS_BAR).assertExists()
  }

  @Test
  fun mapScreen_whenNoFilters_activeFiltersBarNotShown() {
    setContent(currentFilters = EventFilters())

    composeTestRule.onNodeWithTag(MapScreenTestTags.ACTIVE_FILTERS_BAR).assertDoesNotExist()
  }

  @Test
  fun mapScreen_whenFilterDialogShown_displaysFilterDialog() {
    every { mockFilterViewModel.updateLocalFilters(any()) } just Runs

    setContent(uiState = MapUIState(events = listOf(testEvent1), showFilterDialog = true))

    composeTestRule.waitUntil(2000) {
      composeTestRule
          .onAllNodesWithTag(EventFilterDialogTestTags.FILTER_DIALOG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.FILTER_DIALOG).assertExists()
  }

  @Test
  fun mapScreen_whenCameraNotTracking_trackingIndicatorNotShown() {
    setContent(uiState = MapUIState(events = listOf(testEvent1), isCameraTracking = false))

    composeTestRule.onNodeWithTag(MapScreenTestTags.TRACKING_INDICATOR).assertDoesNotExist()
  }

  @Test
  fun mapScreen_clickingRecenterWithTracking_callsRecenterCamera() {
    setContent(uiState = MapUIState(events = listOf(testEvent1), isCameraTracking = true))

    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).performClick()
    verify { mockMapViewModel.recenterCamera() }
  }
}
