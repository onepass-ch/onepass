package ch.onepass.onepass.ui.eventsfilters

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.eventfilters.DateRangePresets
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.ui.eventfilters.EventFilterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class EventFilterViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initial_uiState_and_currentFilters_areDefault() = runTest {
    val viewModel = EventFilterViewModel()
    Assert.assertEquals(EventFilters(), viewModel.uiState.value.localFilters)
    Assert.assertEquals(EventFilters(), viewModel.currentFilters.value)
    Assert.assertFalse(viewModel.uiState.value.expandedRegion)
    Assert.assertFalse(viewModel.uiState.value.expandedDateRangePresets)
    Assert.assertFalse(viewModel.uiState.value.showDatePicker)
  }

  @Test
  fun eventFilterViewModel_applyFilters_updatesState() = runTest {
    val viewModel = EventFilterViewModel()
    val newFilters =
        EventFilters(
            region = "Vaud",
            dateRange = DateRangePresets.getTodayRange(),
            hideSoldOut = true,
        )

    viewModel.applyFilters(newFilters)
    testDispatcher.scheduler.advanceUntilIdle()

    val updatedState = viewModel.currentFilters.value
    Assert.assertEquals("Region should be updated", "Vaud", updatedState.region)
    Assert.assertEquals(
        "Date range should be updated", newFilters.dateRange, updatedState.dateRange)
    Assert.assertTrue("Hide sold out should be true", updatedState.hideSoldOut)
    Assert.assertTrue("Should have active filters", updatedState.hasActiveFilters)
  }

  @Test
  fun updateLocalFilters_changesOnlyUiState() = runTest {
    val viewModel = EventFilterViewModel()
    val filters = EventFilters(region = "Bern", hideSoldOut = true)
    viewModel.updateLocalFilters(filters)
    Assert.assertEquals(filters, viewModel.uiState.value.localFilters)
    Assert.assertEquals(EventFilters(), viewModel.currentFilters.value)
  }

  @Test
  fun toggleRegionDropdown_and_toggleDatePicker_workCorrectly() = runTest {
    val viewModel = EventFilterViewModel()

    viewModel.toggleRegionDropdown(true)
    Assert.assertTrue(viewModel.uiState.value.expandedRegion)

    viewModel.toggleRegionDropdown(false)
    Assert.assertFalse(viewModel.uiState.value.expandedRegion)

    viewModel.toggleDatePicker(true)
    Assert.assertTrue(viewModel.uiState.value.showDatePicker)

    viewModel.toggleDatePicker(false)
    Assert.assertFalse(viewModel.uiState.value.showDatePicker)
  }

  @Test
  fun resetLocalFilters_resetsLocalStateOnly() = runTest {
    val viewModel = EventFilterViewModel()
    viewModel.updateLocalFilters(EventFilters(region = "Vaud", hideSoldOut = true))
    viewModel.resetLocalFilters()
    Assert.assertEquals(EventFilters(), viewModel.uiState.value.localFilters)
  }

  @Test
  fun eventFilterViewModel_clearFilters_resetsToDefault() = runTest {
    val viewModel = EventFilterViewModel()
    val initialFilters =
        EventFilters(
            region = "Zurich",
            dateRange = DateRangePresets.getNext7DaysRange(),
            hideSoldOut = true,
        )

    viewModel.applyFilters(initialFilters)
    viewModel.clearFilters()
    testDispatcher.scheduler.advanceUntilIdle()

    val clearedState = viewModel.currentFilters.value
    Assert.assertNull("Region should be cleared", clearedState.region)
    Assert.assertNull("Date range should be cleared", clearedState.dateRange)
    Assert.assertFalse("Hide sold out should be false", clearedState.hideSoldOut)
    Assert.assertFalse("Should have no active filters", clearedState.hasActiveFilters)
  }

  @Test
  fun eventFilterViewModel_hasActiveFilters_returnsCorrectly() = runTest {
    val viewModel = EventFilterViewModel()

    // Test no filters
    Assert.assertFalse(
        "No filters should return false", viewModel.currentFilters.value.hasActiveFilters)

    // Test region filter only
    viewModel.applyFilters(EventFilters(region = "Bern"))
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        "Region filter should return true", viewModel.currentFilters.value.hasActiveFilters)

    // Test date range filter only
    viewModel.applyFilters(EventFilters(dateRange = DateRangePresets.getNextWeekendRange()))
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        "Date range filter should return true",
        viewModel.currentFilters.value.hasActiveFilters,
    )

    // Test hide sold out only
    viewModel.applyFilters(EventFilters(hideSoldOut = true))
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        "Hide sold out filter should return true",
        viewModel.currentFilters.value.hasActiveFilters,
    )

    // Test all filters
    viewModel.applyFilters(
        EventFilters(
            region = "Vaud",
            dateRange = DateRangePresets.getTodayRange(),
            hideSoldOut = true,
        ))
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        "All filters should return true", viewModel.currentFilters.value.hasActiveFilters)
  }

  @Test
  fun confirmDateRange_updatesLocalFilters_andHidesDatePicker_whenValidRange() = runTest {
    val viewModel = EventFilterViewModel()
    val start = 1000L
    val end = 5000L

    viewModel.toggleDatePicker(true)
    Assert.assertTrue(viewModel.uiState.value.showDatePicker)

    viewModel.confirmDateRange(start, end)

    val updatedRange = viewModel.uiState.value.localFilters.dateRange
    Assert.assertNotNull(updatedRange)
    Assert.assertEquals(start, updatedRange!!.start)
    Assert.assertEquals(end, updatedRange.endInclusive)
    Assert.assertFalse(viewModel.uiState.value.showDatePicker)
  }

  @Test
  fun confirmDateRangeRejectsWhenStartGreaterThanEnd() = runTest {
    val vm = EventFilterViewModel()
    val start = 2000L
    val end = 1000L

    vm.confirmDateRange(start, end)

    // Expect range to still be the default (no update) since start > end
    Assert.assertNull(vm.uiState.value.localFilters.dateRange)
  }

  @Test
  fun confirmDateRangeDoesNotUpdateWhenEndIsBeforeStart() = runTest {
    val viewModel = EventFilterViewModel()
    val start = 2000L
    val end = 1000L

    val initialState = viewModel.uiState.value
    viewModel.confirmDateRange(start, end)

    val newState = viewModel.uiState.value
    // should not have changed
    Assert.assertEquals(initialState.localFilters, newState.localFilters)
    Assert.assertEquals(initialState.showDatePicker, newState.showDatePicker)
  }

  @Test
  fun cancelingDatePickerClearsTemporarySelection() = runTest {
    val vm = EventFilterViewModel()
    vm.toggleDatePicker(true)
    Assert.assertTrue(vm.uiState.value.showDatePicker)

    // simulate cancel (dismiss)
    vm.toggleDatePicker(false)
    Assert.assertFalse(vm.uiState.value.showDatePicker)

    Assert.assertNull(vm.uiState.value.localFilters.dateRange)
  }

  @Test
  fun regionDropdown_dismissMenuWithoutSelection() {
    val viewModel = EventFilterViewModel()

    Assert.assertFalse(viewModel.uiState.value.expandedRegion)

    viewModel.toggleRegionDropdown(true)
    Assert.assertTrue(viewModel.uiState.value.expandedRegion)

    viewModel.toggleRegionDropdown(false)
    Assert.assertFalse(viewModel.uiState.value.expandedRegion)
  }
}
