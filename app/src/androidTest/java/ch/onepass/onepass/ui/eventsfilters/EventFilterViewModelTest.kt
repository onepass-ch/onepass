package ch.onepass.onepass.ui.eventfilters

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.eventfilters.DateRangePresets
import ch.onepass.onepass.model.eventfilters.EventFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
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
    assertEquals(EventFilters(), viewModel.uiState.value.localFilters)
    assertEquals(EventFilters(), viewModel.currentFilters.value)
    assertFalse(viewModel.uiState.value.expandedRegion)
    assertFalse(viewModel.uiState.value.expandedDateRangePresets)
    assertFalse(viewModel.uiState.value.showDatePicker)
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
    assertEquals("Region should be updated", "Vaud", updatedState.region)
    assertEquals("Date range should be updated", newFilters.dateRange, updatedState.dateRange)
    assertTrue("Hide sold out should be true", updatedState.hideSoldOut)
    assertTrue("Should have active filters", updatedState.hasActiveFilters)
  }

  @Test
  fun updateLocalFilters_changesOnlyUiState() = runTest {
    val viewModel = EventFilterViewModel()
    val filters = EventFilters(region = "Bern", hideSoldOut = true)
    viewModel.updateLocalFilters(filters)
    assertEquals(filters, viewModel.uiState.value.localFilters)
    assertEquals(EventFilters(), viewModel.currentFilters.value)
  }

  @Test
  fun toggleRegionDropdown_and_toggleDatePicker_workCorrectly() = runTest {
    val viewModel = EventFilterViewModel()

    viewModel.toggleRegionDropdown(true)
    assertTrue(viewModel.uiState.value.expandedRegion)

    viewModel.toggleRegionDropdown(false)
    assertFalse(viewModel.uiState.value.expandedRegion)

    viewModel.toggleDatePicker(true)
    assertTrue(viewModel.uiState.value.showDatePicker)

    viewModel.toggleDatePicker(false)
    assertFalse(viewModel.uiState.value.showDatePicker)
  }

  @Test
  fun resetLocalFilters_resetsLocalStateOnly() = runTest {
    val viewModel = EventFilterViewModel()
    viewModel.updateLocalFilters(EventFilters(region = "Vaud", hideSoldOut = true))
    viewModel.resetLocalFilters()
    assertEquals(EventFilters(), viewModel.uiState.value.localFilters)
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
    assertNull("Region should be cleared", clearedState.region)
    assertNull("Date range should be cleared", clearedState.dateRange)
    assertFalse("Hide sold out should be false", clearedState.hideSoldOut)
    assertFalse("Should have no active filters", clearedState.hasActiveFilters)
  }

  @Test
  fun eventFilterViewModel_hasActiveFilters_returnsCorrectly() = runTest {
    val viewModel = EventFilterViewModel()

    // Test no filters
    assertFalse("No filters should return false", viewModel.currentFilters.value.hasActiveFilters)

    // Test region filter only
    viewModel.applyFilters(EventFilters(region = "Bern"))
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue("Region filter should return true", viewModel.currentFilters.value.hasActiveFilters)

    // Test date range filter only
    viewModel.applyFilters(EventFilters(dateRange = DateRangePresets.getNextWeekendRange()))
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(
        "Date range filter should return true",
        viewModel.currentFilters.value.hasActiveFilters,
    )

    // Test hide sold out only
    viewModel.applyFilters(EventFilters(hideSoldOut = true))
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(
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
    assertTrue("All filters should return true", viewModel.currentFilters.value.hasActiveFilters)
  }

  @Test
  fun tempStartAndEndDate_setAndConfirm_updatesLocalFilters() = runTest {
    val viewModel = EventFilterViewModel()
    val start = System.currentTimeMillis()
    val end = start + 1000 * 60 * 60 * 24 // +1 day

    viewModel.setTempStartDate(start)
    assertEquals(start, viewModel.uiState.value.tempStartDate)
    assertNull(viewModel.uiState.value.tempEndDate)

    viewModel.setTempEndDate(end)
    assertEquals(end, viewModel.uiState.value.tempEndDate)

    viewModel.confirmDateRange()
    assertEquals(start..end, viewModel.uiState.value.localFilters.dateRange)
    assertNull(viewModel.uiState.value.tempStartDate)
    assertNull(viewModel.uiState.value.tempEndDate)
  }
}
