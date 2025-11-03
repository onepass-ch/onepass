package ch.onepass.onepass.ui.eventfilters

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class FilterDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun filterDialog_displaysAllComponents() {
    composeTestRule.setContent {
      OnePassTheme { FilterDialog(currentFilters = EventFilters(), onApply = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FILTER_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Filter Events").assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.REGION_DROPDOWN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.DATE_RANGE_PRESETS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.HIDE_SOLD_OUT_CHECKBOX).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.RESET_FILTERS_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsDisplayed()
  }

  @Test
  fun filterDialog_dateRangePresets_displayCorrectly() {
    composeTestRule.setContent {
      OnePassTheme { FilterDialog(currentFilters = EventFilters(), onApply = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithText("Today").assertIsDisplayed()
    composeTestRule.onNodeWithText("Next Weekend").assertIsDisplayed()
    composeTestRule.onNodeWithText("Next 7 Days").assertIsDisplayed()
    composeTestRule.onNodeWithText("Custom range").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pick dates").assertIsDisplayed()
  }

  @Test
  fun filterDialog_applyButton_initiallyDisabled() {
    composeTestRule.setContent {
      OnePassTheme { FilterDialog(currentFilters = EventFilters(), onApply = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun filterDialog_resetButton_initiallyDisabled() {
    composeTestRule.setContent {
      OnePassTheme { FilterDialog(currentFilters = EventFilters(), onApply = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.RESET_FILTERS_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun filterDialog_regionSelection_enablesApplyButton() {
    composeTestRule.setContent {
      OnePassTheme { FilterDialog(currentFilters = EventFilters(), onApply = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.REGION_DROPDOWN).performClick()
    composeTestRule.onNodeWithText("Uri").performClick()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()
  }

  @Test
  fun filterDialog_dateRangeSelection_enablesApplyButton() {
    composeTestRule.setContent {
      OnePassTheme { FilterDialog(currentFilters = EventFilters(), onApply = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithText("Today").performClick()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()
  }

  @Test
  fun filterDialog_hideSoldOutSelection_enablesApplyButton() {
    composeTestRule.setContent {
      OnePassTheme { FilterDialog(currentFilters = EventFilters(), onApply = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.HIDE_SOLD_OUT_CHECKBOX).performClick()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()
  }

  @Test
  fun filterDialog_resetButton_enabledWithActiveFilters() {
    composeTestRule.setContent {
      OnePassTheme {
        FilterDialog(currentFilters = EventFilters(region = "Zurich"), onApply = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.RESET_FILTERS_BUTTON).assertIsEnabled()
  }

  @Test
  fun filterDialog_applyButton_callsOnApply() {
    var appliedFilters: EventFilters? = null

    composeTestRule.setContent {
      OnePassTheme {
        FilterDialog(
            currentFilters = EventFilters(),
            onApply = { appliedFilters = it },
            onDismiss = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.HIDE_SOLD_OUT_CHECKBOX).performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).performClick()

    assertNotNull("onApply should be called", appliedFilters)
    assert(appliedFilters!!.hideSoldOut)
  }

  @Test
  fun filterDialog_resetButton_clearsAllFilters() {
    composeTestRule.setContent {
      OnePassTheme {
        FilterDialog(
            currentFilters = EventFilters(region = "Geneva", hideSoldOut = true),
            onApply = {},
            onDismiss = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.RESET_FILTERS_BUTTON).performClick()

    // After reset, apply button should be enabled (since filters changed from initial)ok
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()
  }

  @Test
  fun filterDialog_dateRangeChip_toggleBehavior() {
    composeTestRule.setContent {
      OnePassTheme { FilterDialog(currentFilters = EventFilters(), onApply = {}, onDismiss = {}) }
    }

    // Select date range
    composeTestRule.onNodeWithText("Today").performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()

    // Deselect date range
    composeTestRule.onNodeWithText("Today").performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun filterDialog_customDateRangeButton_showsPicker() {
    composeTestRule.setContent {
      OnePassTheme { FilterDialog(currentFilters = EventFilters(), onApply = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithText("Pick dates").performClick()

    composeTestRule.onNodeWithText("Select Date Range").assertIsDisplayed()
    composeTestRule.onNodeWithText("OK").assertIsDisplayed()
  }
}
