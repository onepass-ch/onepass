package ch.onepass.onepass.ui.eventfilters

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.eventfilters.DateRangePresets
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.ui.theme.OnePassTheme
import java.time.Instant
import junit.framework.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveFiltersBarTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun activeFiltersBar_displaysRegionFilter() {
    val filters = EventFilters(region = "Zurich")
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }
    composeTestRule.onNodeWithText("Zurich").assertIsDisplayed()
    composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_displaysDateRangeFilter_todayPreset() {
    val todayFilters = EventFilters(dateRange = DateRangePresets.getTodayRange())
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = todayFilters, onClearFilters = {}) }
    }
    composeTestRule.onNodeWithText("Today").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_displaysDateRangeFilter_next7DaysPreset() {
    val next7DaysFilters = EventFilters(dateRange = DateRangePresets.getNext7DaysRange())
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = next7DaysFilters, onClearFilters = {}) }
    }
    composeTestRule.onNodeWithText("Next 7 Days").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_displaysDateRangeFilter_weekendPreset() {
    val weekendFilters = EventFilters(dateRange = DateRangePresets.getNextWeekendRange())
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = weekendFilters, onClearFilters = {}) }
    }
    composeTestRule.onNodeWithText("Next Weekend").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_displaysHideSoldOutFilter() {
    val filters = EventFilters(hideSoldOut = true)
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }
    composeTestRule.onNodeWithText("Available Only").assertIsDisplayed()
    composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_displaysAllFiltersTogether() {
    val filters =
        EventFilters(
            region = "Bern",
            dateRange = DateRangePresets.getNext7DaysRange(),
            hideSoldOut = true,
        )
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }
    composeTestRule.onNodeWithText("Bern").assertIsDisplayed()
    composeTestRule.onNodeWithText("Next 7 Days").assertIsDisplayed() // Updated from "Date Range"
    composeTestRule.onNodeWithText("Available Only").assertIsDisplayed()
    composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_hidesFiltersWhenNullOrFalse() {
    val filters = EventFilters(region = null, dateRange = null, hideSoldOut = false)
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }
    composeTestRule.onNodeWithText("Zurich").assertDoesNotExist()
    composeTestRule.onNodeWithText("Today").assertDoesNotExist()
    composeTestRule.onNodeWithText("Next 7 Days").assertDoesNotExist()
    composeTestRule.onNodeWithText("This Weekend").assertDoesNotExist()
    composeTestRule.onNodeWithText("Available Only").assertDoesNotExist()
    composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_clearAllButton_clickTriggersCallback() {
    var callbackCount = 0
    val filters = EventFilters(region = "Zurich", hideSoldOut = true)
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = { callbackCount++ }) }
    }
    composeTestRule.onNodeWithText("Clear All").performClick()
    assertEquals(1, callbackCount)
  }

  @Test
  fun activeFiltersBar_clearAllButton_multipleClicks() {
    var callbackCount = 0
    val filters = EventFilters(region = "Zurich", dateRange = DateRangePresets.getTodayRange())
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = { callbackCount++ }) }
    }
    repeat(3) { composeTestRule.onNodeWithText("Clear All").performClick() }
    assertEquals(3, callbackCount)
  }

  @Test
  fun activeFiltersBar_clearAllButton_isClickable() {
    val filters = EventFilters(hideSoldOut = true)

    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }
    composeTestRule.onNodeWithText("Clear All").assertHasClickAction()
  }

  @Test
  fun activeFiltersBar_customDateRange_showsFormattedDates() {
    val customRange = 1672531200000L..1672617599000L
    val filters = EventFilters(dateRange = customRange)
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }
    val expectedText = formatDateRange(customRange)
    composeTestRule.onNodeWithText(expectedText!!).assertExists()
  }

  @Test
  fun formatDateRange_null_returnsNull() {
    val result = formatDateRange(null)
    assertNull(result)
  }

  @Test
  fun formatDateRange_sameDay_returnsSingleDayRange() {
    val start = Instant.parse("2025-01-01T10:00:00Z").toEpochMilli()
    val end = Instant.parse("2025-01-01T22:00:00Z").toEpochMilli()
    val expected = "Jan 01, 2025 - Jan 01, 2025"

    val result = formatDateRange(start..end)
    assertEquals(expected, result)
  }

  @Test
  fun formatDateRange_crossYear_returnsCorrectString() {
    val start = Instant.parse("2024-12-31T12:00:00Z").toEpochMilli()
    val end = Instant.parse("2025-01-01T12:00:00Z").toEpochMilli()
    val expected = "Dec 31, 2024 - Jan 01, 2025"

    val result = formatDateRange(start..end)
    assertEquals(expected, result)
  }
}
