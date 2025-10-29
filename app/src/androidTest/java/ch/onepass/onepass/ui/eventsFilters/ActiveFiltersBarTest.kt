package ch.onepass.onepass.ui.eventFilters

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.eventFilters.DateRangePresets
import ch.onepass.onepass.model.eventFilters.EventFilters
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
      MaterialTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }

    composeTestRule.onNodeWithText("Zurich").assertIsDisplayed()
    composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_displaysDateRangeFilter() {
    val filters = EventFilters(dateRange = DateRangePresets.getTodayRange())
    composeTestRule.setContent {
      MaterialTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }

    composeTestRule.onNodeWithText("Date Range").assertIsDisplayed()
    composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_displaysHideSoldOutFilter() {
    val filters = EventFilters(hideSoldOut = true)
    composeTestRule.setContent {
      MaterialTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
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
      MaterialTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }

    composeTestRule.onNodeWithText("Bern").assertIsDisplayed()
    composeTestRule.onNodeWithText("Date Range").assertIsDisplayed()
    composeTestRule.onNodeWithText("Available Only").assertIsDisplayed()
    composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_hidesFiltersWhenNullOrFalse() {
    val filters = EventFilters(region = null, dateRange = null, hideSoldOut = false)
    composeTestRule.setContent {
      MaterialTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }

    composeTestRule.onNodeWithText("Zurich").assertDoesNotExist()
    composeTestRule.onNodeWithText("Date Range").assertDoesNotExist()
    composeTestRule.onNodeWithText("Available Only").assertDoesNotExist()
    composeTestRule.onNodeWithText("Clear All").assertIsDisplayed()
  }

  @Test
  fun activeFiltersBar_clearAllButton_clickTriggersCallback() {
    var callbackCount = 0
    val filters = EventFilters(region = "Zurich", hideSoldOut = true)

    composeTestRule.setContent {
      MaterialTheme { ActiveFiltersBar(filters = filters, onClearFilters = { callbackCount++ }) }
    }

    composeTestRule.onNodeWithText("Clear All").performClick()
    assertEquals(1, callbackCount)
  }

  @Test
  fun activeFiltersBar_clearAllButton_multipleClicks() {
    var callbackCount = 0
    val filters = EventFilters(region = "Zurich", dateRange = DateRangePresets.getTodayRange())

    composeTestRule.setContent {
      MaterialTheme { ActiveFiltersBar(filters = filters, onClearFilters = { callbackCount++ }) }
    }

    repeat(3) { composeTestRule.onNodeWithText("Clear All").performClick() }

    assertEquals(3, callbackCount)
  }

  @Test
  fun activeFiltersBar_clearAllButton_isClickable() {
    val filters = EventFilters(hideSoldOut = true)

    composeTestRule.setContent {
      MaterialTheme { ActiveFiltersBar(filters = filters, onClearFilters = {}) }
    }

    composeTestRule.onNodeWithText("Clear All").assertHasClickAction()
  }
}
