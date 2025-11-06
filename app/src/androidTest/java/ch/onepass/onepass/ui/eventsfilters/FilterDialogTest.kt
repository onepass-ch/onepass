package ch.onepass.onepass.ui.eventfilters

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class FilterDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun filterDialog_displaysAllComponents() {
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

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
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

    composeTestRule.onNodeWithText("Today").assertIsDisplayed()
    composeTestRule.onNodeWithText("Next Weekend").assertIsDisplayed()
    composeTestRule.onNodeWithText("Next 7 Days").assertIsDisplayed()
    composeTestRule.onNodeWithText("Custom range").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pick dates").assertIsDisplayed()
  }

  @Test
  fun filterDialog_applyButton_initiallyDisabled_untilFilterChange() {
    composeTestRule.setContent { OnePassTheme { FilterDialog(onApply = {}, onDismiss = {}) } }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsNotEnabled()

    // Select region to enable
    composeTestRule.onNodeWithTag(FeedScreenTestTags.REGION_DROPDOWN).performClick()
    composeTestRule.onNodeWithText("Zurich").performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()
  }

  @Test
  fun filterDialog_resetButton_enabledAfterFilterChange() {
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

    // Select "Today" preset
    composeTestRule.onNodeWithText("Today").performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.RESET_FILTERS_BUTTON).assertIsEnabled()

    // Reset filters should disable Apply again
    composeTestRule.onNodeWithTag(FeedScreenTestTags.RESET_FILTERS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun filterDialog_dateRangeSelection_enablesApplyButton() {
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

    // Select "Next 7 Days"
    composeTestRule.onNodeWithText("Next 7 Days").performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()

    // Deselect same chip
    composeTestRule.onNodeWithText("Next 7 Days").performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun filterDialog_hideSoldOutSelection_enablesApplyButton() {
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.HIDE_SOLD_OUT_CHECKBOX).performClick()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()
  }

  @Test
  fun filterDialog_applyButton_callsOnApplyWithUpdatedFilters() {
    var appliedFilters: EventFilters? = null

    composeTestRule.setContent {
      OnePassTheme {
        FilterDialog(
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
  fun filterDialog_dateRangeChip_toggleBehavior() {
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

    // Select date range
    composeTestRule.onNodeWithText("Today").performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()

    // Deselect date range
    composeTestRule.onNodeWithText("Today").performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun filterDialog_datePickerDialog_showsAndDismisses() {
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

    composeTestRule.onNodeWithText("Pick dates").performClick()
    composeTestRule.onNodeWithText("When ?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.onNodeWithText("Filter Events").assertIsDisplayed()
  }

  @Test
  fun filterDialog_customRangeText_andPickDatesButton_displayed() {
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.CUSTOM_RANGE_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.PICK_DATES_BUTTON).assertIsDisplayed()
  }

  @Test
  fun filterDialog_pickDatesButton_showsDatePickerDialog() {
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.PICK_DATES_BUTTON).performClick()
    composeTestRule.onNodeWithText("When ?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FILTER_DIALOG).assertIsDisplayed()
  }
  
  @Test
  fun inclusiveEndOfDay_extendsEndTimeToEndOfDay() {
    val start = Calendar.getInstance().apply {
      set(2025, Calendar.JANUARY, 1, 10, 30, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val end = start
    val result = (start..end).inclusiveEndOfDay()
    val cal = Calendar.getInstance().apply { timeInMillis = result.endInclusive }

    assert(2025==cal.get(Calendar.YEAR))
    assert(Calendar.JANUARY== cal.get(Calendar.MONTH))
    assert(1== cal.get(Calendar.DAY_OF_MONTH))
    assert(END_OF_DAY_HOUR==cal.get(Calendar.HOUR_OF_DAY))
    assert(END_OF_DAY_MINUTE== cal.get(Calendar.MINUTE))
    assert(END_OF_DAY_SECOND== cal.get(Calendar.SECOND))
    assert(END_OF_DAY_MILLISECOND==cal.get(Calendar.MILLISECOND))
  }
}
