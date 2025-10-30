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
  fun eventFilterViewModel_initialState_hasNoFilters() = runTest {
    val viewModel = EventFilterViewModel()

    val initialState = viewModel.currentFilters.value

    assertNull("Region should be null", initialState.region)
    assertNull("Date range should be null", initialState.dateRange)
    assertFalse("Hide sold out should be false", initialState.hideSoldOut)
    assertFalse("Should have no active filters", initialState.hasActiveFilters)
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
}
