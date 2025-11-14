package ch.onepass.onepass.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.onepass.onepass.BuildConfig
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.event.EventCardViewModel
import ch.onepass.onepass.ui.eventfilters.EventFilterDialogTestTags
import ch.onepass.onepass.ui.eventfilters.EventFilterViewModel
import ch.onepass.onepass.ui.eventfilters.FilterUIState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.mapbox.common.MapboxOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockMapViewModel: MapViewModel
  private lateinit var mockFilterViewModel: EventFilterViewModel
  private lateinit var eventCardViewModel: EventCardViewModel

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
          location = Location(GeoPoint(47.3769, 8.5417), "Zurich HB", "Zurich"),
          startTime = Timestamp.now(),
          ticketsRemaining = 30)

  @Before
  fun setUp() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

    mockMapViewModel = mockk(relaxed = true)
    mockFilterViewModel = mockk(relaxed = true)
    eventCardViewModel = EventCardViewModel.getInstance()

    every { mockMapViewModel.uiState } returns
        MutableStateFlow(
            MapUIState(
                events = listOf(testEvent1, testEvent2),
                selectedEvent = null,
                showFilterDialog = false))
    every { mockMapViewModel.allEvents } returns MutableStateFlow(listOf(testEvent1, testEvent2))
    every { mockFilterViewModel.currentFilters } returns MutableStateFlow(EventFilters())
    every { mockFilterViewModel.uiState } returns MutableStateFlow(FilterUIState())
  }

  @Test
  fun mapScreen_displaysAllMainComponentsWhenLoaded() {
    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }
    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.FILTER_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.ACTIVE_FILTERS_BAR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.FILTER_DIALOG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(C.Tag.event_card).assertDoesNotExist()
  }

  @Test
  fun mapScreen_showsActiveFiltersBarWhenFiltersAreActive() {
    every { mockFilterViewModel.currentFilters } returns
        MutableStateFlow(EventFilters(region = "Vaud", hideSoldOut = true))

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.ACTIVE_FILTERS_BAR).assertIsDisplayed()
  }

  @Test
  fun mapScreen_hidesActiveFiltersBarWhenNoFiltersAreActive() {
    every { mockFilterViewModel.currentFilters } returns MutableStateFlow(EventFilters())

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.ACTIVE_FILTERS_BAR).assertDoesNotExist()
  }

  @Test
  fun mapScreen_hidesEventCardWhenNoEventIsSelected() {
    every { mockMapViewModel.uiState } returns
        MutableStateFlow(
            MapUIState(
                events = listOf(testEvent1, testEvent2),
                selectedEvent = null,
                showFilterDialog = false))

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(C.Tag.event_card).assertDoesNotExist()
  }

  @Test
  fun mapScreen_clickingFilterButtonOpensFilterDialog() {
    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.FILTER_BUTTON).performClick()

    verify { mockMapViewModel.setShowFilterDialog(true) }
  }

  @Test
  fun mapScreen_filterDialogShowsWhenShowFilterDialogIsTrue() {
    every { mockMapViewModel.uiState } returns
        MutableStateFlow(
            MapUIState(
                events = listOf(testEvent1, testEvent2),
                selectedEvent = null,
                showFilterDialog = true))

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.FILTER_DIALOG).assertIsDisplayed()
  }

  @Test
  fun mapScreen_filterDialogHidesWhenShowFilterDialogIsFalse() {
    every { mockMapViewModel.uiState } returns
        MutableStateFlow(
            MapUIState(
                events = listOf(testEvent1, testEvent2),
                selectedEvent = null,
                showFilterDialog = false))

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.FILTER_DIALOG).assertDoesNotExist()
  }

  @Test
  fun mapScreen_clickingRecenterButtonTriggersRecenterAction() {
    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).performClick()

    verify { mockMapViewModel.recenterCamera() }
  }

  @Test
  fun mapScreen_clickingMapAreaClearsSelectedEvent() {
    every { mockMapViewModel.uiState } returns
        MutableStateFlow(
            MapUIState(
                events = listOf(testEvent1, testEvent2),
                selectedEvent = testEvent1,
                showFilterDialog = false))

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(C.Tag.event_card).assertIsDisplayed()

    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).performClick()

    verify { mockMapViewModel.clearSelectedEvent() }
  }

  @Test
  fun mapScreen_appliesFiltersWhenCurrentFiltersChange() {
    var filtersApplied = false
    every { mockMapViewModel.applyFiltersToCurrentEvents(any()) } answers { filtersApplied = true }

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.waitUntil(2000L) { filtersApplied }
    verify { mockMapViewModel.applyFiltersToCurrentEvents(any()) }
  }

  @Test
  fun mapScreen_handlesLocationPermissionGranted() {
    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
    verify { mockMapViewModel.onMapReady(any(), true) }
  }

  @Test
  fun mapScreen_handlesLocationPermissionDenied() {
    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = false)
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
    verify { mockMapViewModel.onMapReady(any(), false) }
  }

  @Test
  fun mapScreen_displaysCorrectlyWithEmptyEvents() {
    every { mockMapViewModel.uiState } returns
        MutableStateFlow(
            MapUIState(events = emptyList(), selectedEvent = null, showFilterDialog = false))
    every { mockMapViewModel.allEvents } returns MutableStateFlow(emptyList())

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.FILTER_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.Tag.event_card).assertDoesNotExist()
  }

  @Test
  fun mapScreen_eventCardCloseButtonWorksCorrectly() {
    every { mockMapViewModel.uiState } returns
        MutableStateFlow(
            MapUIState(
                events = listOf(testEvent1, testEvent2),
                selectedEvent = testEvent1,
                showFilterDialog = false))

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(C.Tag.event_card).assertIsDisplayed()

    composeTestRule.onNodeWithTag(C.Tag.event_card_close_button).performClick()

    verify { mockMapViewModel.clearSelectedEvent() }
  }

  @Test
  fun mapScreen_filterDialogApplyButtonClosesDialog() {
    every { mockMapViewModel.uiState } returns
        MutableStateFlow(
            MapUIState(
                events = listOf(testEvent1, testEvent2),
                selectedEvent = null,
                showFilterDialog = true))

    every { mockFilterViewModel.currentFilters } returns MutableStateFlow(EventFilters())
    every { mockFilterViewModel.uiState } returns
        MutableStateFlow(FilterUIState(localFilters = EventFilters(region = "Vaud")))

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.FILTER_DIALOG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON).performClick()

    verify { mockMapViewModel.setShowFilterDialog(false) }
    verify { mockFilterViewModel.applyFilters(any()) }
  }

  @Test
  fun mapScreen_filterDialogDismissClosesDialog() {
    every { mockMapViewModel.uiState } returns
        MutableStateFlow(
            MapUIState(
                events = listOf(testEvent1, testEvent2),
                selectedEvent = null,
                showFilterDialog = true))

    every { mockFilterViewModel.currentFilters } returns MutableStateFlow(EventFilters())
    every { mockFilterViewModel.uiState } returns
        MutableStateFlow(FilterUIState(localFilters = EventFilters(region = "Vaud")))

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.FILTER_DIALOG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventFilterDialogTestTags.RESET_FILTERS_BUTTON)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.RESET_FILTERS_BUTTON).performClick()

    verify { mockFilterViewModel.resetLocalFilters() }
  }

  @Test
  fun mapScreen_eventCardLikeButtonTriggersLikeAction() {
    every { mockMapViewModel.uiState } returns
        MutableStateFlow(
            MapUIState(
                events = listOf(testEvent1, testEvent2),
                selectedEvent = testEvent1,
                showFilterDialog = false))

    composeTestRule.setContent {
      MapScreen(
          mapViewModel = mockMapViewModel,
          filterViewModel = mockFilterViewModel,
          isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(C.Tag.event_card).assertExists()

    composeTestRule.onNodeWithTag(C.Tag.event_card_like_button).assertExists()
  }
}
