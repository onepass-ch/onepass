package ch.onepass.onepass.ui.map

import androidx.compose.ui.test.*
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
  }

  private fun stubStates(
      uiState: MapUIState = MapUIState(events = listOf(testEvent1, testEvent2)),
      allEvents: List<Event> = listOf(testEvent1, testEvent2),
      currentFilters: EventFilters = EventFilters(),
      filterUiState: FilterUIState = FilterUIState()
  ) {
    every { mockMapViewModel.uiState } returns MutableStateFlow(uiState)
    every { mockMapViewModel.allEvents } returns MutableStateFlow(allEvents)
    every { mockFilterViewModel.currentFilters } returns MutableStateFlow(currentFilters)
    every { mockFilterViewModel.uiState } returns MutableStateFlow(filterUiState)
  }

  private fun setContent() {
    composeTestRule.setContent {
      MapScreen(mapViewModel = mockMapViewModel, filterViewModel = mockFilterViewModel)
    }
  }

  @Test
  fun mapScreen_mainComponents_and_variants() {
    val uiStateFlow = MutableStateFlow(MapUIState(events = listOf(testEvent1, testEvent2)))
    val allEventsFlow = MutableStateFlow(listOf(testEvent1, testEvent2))
    val currentFiltersFlow = MutableStateFlow(EventFilters())
    val filterUiStateFlow = MutableStateFlow(FilterUIState())

    every { mockMapViewModel.uiState } returns uiStateFlow
    every { mockMapViewModel.allEvents } returns allEventsFlow
    every { mockFilterViewModel.currentFilters } returns currentFiltersFlow
    every { mockFilterViewModel.uiState } returns filterUiStateFlow
    setContent()

    // Initial UI
    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.FILTER_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.Tag.event_card).assertDoesNotExist()

    // Flow update: active filters
    currentFiltersFlow.value = EventFilters(region = "Vaud", hideSoldOut = true)
    composeTestRule.waitUntil(2_000) {
      composeTestRule
          .onAllNodesWithTag(MapScreenTestTags.ACTIVE_FILTERS_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(MapScreenTestTags.ACTIVE_FILTERS_BAR).assertIsDisplayed()

    // Flow update: empty events
    uiStateFlow.value = MapUIState(events = emptyList())
    allEventsFlow.value = emptyList()
    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun mapScreen_onMapReady_called_with_permission_state_from_viewModel() {
    stubStates(
        uiState = MapUIState(events = listOf(testEvent1, testEvent2), hasLocationPermission = true))

    // Need to capture the actual call asynchronously before verify can succeed.
    var called = false
    every { mockMapViewModel.onMapReady(any(), any()) } answers
        {
          called = it.invocation.args[1] as Boolean
        }

    setContent()

    composeTestRule.waitUntil(5_000) { called }
    verify { mockMapViewModel.onMapReady(any(), true) }
  }

  @Test
  fun mapScreen_onMapReady_called_when_permission_denied_in_viewModel() {
    stubStates(
        uiState =
            MapUIState(events = listOf(testEvent1, testEvent2), hasLocationPermission = false))

    // onMapReady is called asynchronously, so we wait for the callback before verify
    var deniedCalled = false
    every { mockMapViewModel.onMapReady(any(), false) } answers { deniedCalled = true }

    setContent()

    composeTestRule.waitUntil(5_000) { deniedCalled }
    verify { mockMapViewModel.onMapReady(any(), false) }
  }

  @Test
  fun mapScreen_clickingButtons_triggersViewModelActions() {
    stubStates()
    setContent()

    composeTestRule.onNodeWithTag(MapScreenTestTags.FILTER_BUTTON).performClick()
    verify { mockMapViewModel.setShowFilterDialog(true) }

    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).performClick()
    verify { mockMapViewModel.recenterCamera() }
  }

  @Test
  fun mapScreen_filterDialog_apply_and_dismiss_flow() {
    val uiStateFlow =
        MutableStateFlow(
            MapUIState(events = listOf(testEvent1, testEvent2), showFilterDialog = true))
    val allEventsFlow = MutableStateFlow(listOf(testEvent1, testEvent2))
    val currentFiltersFlow = MutableStateFlow(EventFilters())
    val filterUiStateFlow =
        MutableStateFlow(FilterUIState(localFilters = EventFilters(region = "Vaud")))

    every { mockMapViewModel.uiState } returns uiStateFlow
    every { mockMapViewModel.allEvents } returns allEventsFlow
    every { mockFilterViewModel.currentFilters } returns currentFiltersFlow
    every { mockFilterViewModel.uiState } returns filterUiStateFlow

    setContent()

    composeTestRule.waitUntil(timeoutMillis = 2_000) {
      composeTestRule
          .onAllNodesWithTag(EventFilterDialogTestTags.FILTER_DIALOG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.FILTER_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON).performClick()
    verify { mockFilterViewModel.applyFilters(any()) }
    verify { mockMapViewModel.setShowFilterDialog(false) }

    uiStateFlow.value = uiStateFlow.value.copy(showFilterDialog = true)
    composeTestRule.waitUntil(timeoutMillis = 2_000) {
      composeTestRule
          .onAllNodesWithTag(EventFilterDialogTestTags.FILTER_DIALOG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(EventFilterDialogTestTags.RESET_FILTERS_BUTTON)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.RESET_FILTERS_BUTTON).performClick()
    verify { mockFilterViewModel.resetLocalFilters() }
  }

  @Test
  fun mapScreen_eventCard_selection_and_close_and_like() {
    val uiStateFlow =
        MutableStateFlow(
            MapUIState(events = listOf(testEvent1, testEvent2), selectedEvent = testEvent1))
    stubStates(uiState = uiStateFlow.value)
    setContent()

    composeTestRule.onNodeWithTag(C.Tag.event_card).assertIsDisplayed()

    // Click the background overlay that calls mapViewModel.clearSelectedEvent()
    composeTestRule.onNodeWithTag(MapScreenTestTags.EVENT_CARD).performClick()
    verify { mockMapViewModel.clearSelectedEvent() }

    // Re-display card
    uiStateFlow.value = uiStateFlow.value.copy(selectedEvent = testEvent1)
    composeTestRule.onNodeWithTag(C.Tag.event_card_like_button).performClick()
    composeTestRule.waitUntil(2_000) {
      eventCardViewModel.likedEvents.value.contains(testEvent1.eventId)
    }
    assert(eventCardViewModel.likedEvents.value.contains(testEvent1.eventId))
  }

  @Test
  fun mapScreen_appliesFiltersWhenCurrentFiltersChange() {
    var filtersApplied = false
    every { mockMapViewModel.applyFiltersToCurrentEvents(any()) } answers { filtersApplied = true }
    stubStates()
    setContent()

    composeTestRule.waitUntil(2_000) { filtersApplied }
    verify { mockMapViewModel.applyFiltersToCurrentEvents(any()) }
  }
}
