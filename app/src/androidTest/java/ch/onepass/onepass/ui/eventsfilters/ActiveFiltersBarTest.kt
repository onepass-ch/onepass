package ch.onepass.onepass.ui.eventsfilters

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.EventTag
import ch.onepass.onepass.model.eventfilters.DateRangePresets
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.ui.eventfilters.ActiveFiltersBar
import ch.onepass.onepass.ui.eventfilters.formatDateRange
import ch.onepass.onepass.ui.theme.OnePassTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveFiltersBarTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
    val tags = EventTag.entries.take(2).map { it.displayValue }.toSet()
    val filters =
        EventFilters(
            region = "Bern",
            dateRange = DateRangePresets.getNext7DaysRange(),
            selectedTags = tags,
            hideSoldOut = true,
        )
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }
    composeTestRule.onNodeWithText("Bern").assertIsDisplayed()
    composeTestRule.onNodeWithText("Next 7 Days").assertIsDisplayed() // Updated from "Date Range"
    tags.forEach { tag -> composeTestRule.onNodeWithText(tag).assertIsDisplayed() }
    composeTestRule.onNodeWithText("Available Only").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_displaysMultipleTagsWithCountChip() {
    val allTags = EventTag.entries.map { it.displayValue }
    val selectedTags = allTags.take(5).toSet() // 5 tags should show count chip

    val filters = EventFilters(selectedTags = selectedTags)

    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }

    // Should show first 3 tags
    selectedTags.take(3).forEach { tag -> composeTestRule.onNodeWithText(tag).assertIsDisplayed() }

    // Should show count chip for remaining tags
    val remainingCount = selectedTags.size - 3
    composeTestRule.onNodeWithText("+$remainingCount more").assertIsDisplayed()

    // Should NOT show tags beyond the first 3 as individual chips
    selectedTags.drop(3).forEach { tag -> composeTestRule.onNodeWithText(tag).assertDoesNotExist() }
  }

  @Test
  fun activeFiltersBar_doesNotShowCountChipForThreeOrLessTags() {
    // Test with 3 tags (exact boundary)
    val threeTags = EventTag.entries.take(3).map { it.displayValue }.toSet()
    val filters = EventFilters(selectedTags = threeTags)

    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }

    // All 3 tags should be displayed
    threeTags.forEach { tag -> composeTestRule.onNodeWithText(tag).assertIsDisplayed() }

    // No count chip should be shown
    composeTestRule.onNodeWithText("+").assertDoesNotExist()
  }

  @Test
  fun activeFiltersBar_integrationWithOtherFilters() {
    // Test mixed filters including tags
    val tags = setOf(EventTag.TECH.displayValue, EventTag.FREE.displayValue)
    val filters =
        EventFilters(
            region = "Zurich",
            dateRange = DateRangePresets.getTodayRange(),
            selectedTags = tags,
            hideSoldOut = false // Test with false too
            )

    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }

    // Verify all filter types appear together
    composeTestRule.onNodeWithText("Zurich").assertIsDisplayed()
    composeTestRule.onNodeWithText("Today").assertIsDisplayed()
    tags.forEach { tag -> composeTestRule.onNodeWithText(tag).assertIsDisplayed() }
    composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()

    // Verify hideSoldOut doesn't appear when false
    composeTestRule.onNodeWithText("Available Only").assertDoesNotExist()
  }

  @Test
  fun activeFiltersBar_emptyStateShowsNoTags() {
    val filters = EventFilters() // No filters active

    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }

    // Should show Clear All button but no filter chips
    composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()

    // Should NOT show any tags (using actual EventTag values)
    EventTag.entries.forEach { tag ->
      composeTestRule.onNodeWithText(tag.displayValue).assertDoesNotExist()
    }
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
  fun activeFiltersBar_clearAllButton_behavior() {
    var callbackCount = 0
    val filters = EventFilters(region = "Zurich", hideSoldOut = true)
    composeTestRule.setContent {
      OnePassTheme { ActiveFiltersBar(filters = filters, onClearFilters = { callbackCount++ }) }
    }

    val clearNode = composeTestRule.onNodeWithText("Clear All")
    clearNode.assertIsDisplayed().assertHasClickAction()

    clearNode.performClick()
    assertEquals(1, callbackCount)

    repeat(2) { clearNode.performClick() }
    assertEquals(3, callbackCount)
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
