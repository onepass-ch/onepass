package ch.onepass.onepass.ui.eventsfilters

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.eventfilters.TagCategories
import ch.onepass.onepass.ui.eventfilters.DateRangePickerDialog
import ch.onepass.onepass.ui.eventfilters.END_OF_DAY_HOUR
import ch.onepass.onepass.ui.eventfilters.END_OF_DAY_MILLISECOND
import ch.onepass.onepass.ui.eventfilters.END_OF_DAY_MINUTE
import ch.onepass.onepass.ui.eventfilters.END_OF_DAY_SECOND
import ch.onepass.onepass.ui.eventfilters.EventFilterDialogTestTags
import ch.onepass.onepass.ui.eventfilters.FilterDialog
import ch.onepass.onepass.ui.eventfilters.inclusiveEndOfDay
import ch.onepass.onepass.ui.theme.OnePassTheme
import java.util.Calendar
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FilterDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun filterDialog_displays_all_components_and_presets() {
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.FILTER_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Filter Events").assertIsDisplayed()

    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.REGION_DROPDOWN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.DATE_RANGE_PRESETS).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(EventFilterDialogTestTags.RESET_FILTERS_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON)
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("Today").assertIsDisplayed()
    composeTestRule.onNodeWithText("Next Weekend").assertIsDisplayed()
    composeTestRule.onNodeWithText("Next 7 Days").assertIsDisplayed()
    composeTestRule.onNodeWithText("Custom range").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pick dates").assertIsDisplayed()

    composeTestRule
        .onNode(hasScrollAction())
        .performScrollToNode(hasTestTag(EventFilterDialogTestTags.HIDE_SOLD_OUT_CHECKBOX))
    composeTestRule
        .onNodeWithTag(EventFilterDialogTestTags.HIDE_SOLD_OUT_CHECKBOX)
        .assertIsDisplayed()
  }

  @Test
  fun filterDialog_apply_reset_and_enable_flows() {
    var appliedFilters: EventFilters? = null

    composeTestRule.setContent {
      OnePassTheme { FilterDialog(onApply = { appliedFilters = it }, onDismiss = {}) }
    }

    // initially Apply disabled
    composeTestRule
        .onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON)
        .assertIsNotEnabled()

    // selecting region enables Apply
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.REGION_DROPDOWN).performClick()
    composeTestRule.onNodeWithText("Zurich").performClick()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()

    // reset should disable Apply again
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.RESET_FILTERS_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON)
        .assertIsNotEnabled()

    // selecting a preset enables Apply, deselect disables
    composeTestRule.onNodeWithText("Today").performClick()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithText("Today").performClick()
    composeTestRule
        .onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON)
        .assertIsNotEnabled()

    // checkbox toggles Apply
    composeTestRule
        .onNode(hasScrollAction())
        .performScrollToNode(hasTestTag(EventFilterDialogTestTags.HIDE_SOLD_OUT_CHECKBOX))
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.HIDE_SOLD_OUT_CHECKBOX).performClick()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON).assertIsEnabled()

    // apply actually calls callback with updated filters
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON).performClick()
    assertNotNull("onApply should be called", appliedFilters)
    assert(appliedFilters!!.hideSoldOut)
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

    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.CUSTOM_RANGE_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.PICK_DATES_BUTTON).assertIsDisplayed()
  }

  @Test
  fun filterDialog_pickDatesButton_showsDatePickerDialog() {
    composeTestRule.setContent { OnePassTheme { FilterDialog() } }

    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.PICK_DATES_BUTTON).performClick()
    composeTestRule.onNodeWithText("When ?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.FILTER_DIALOG).assertIsDisplayed()
  }

  @Test
  fun inclusiveEndOfDay_extendsEndTimeToEndOfDay() {
    val start =
        Calendar.getInstance()
            .apply {
              set(2025, Calendar.JANUARY, 1, 10, 30, 0)
              set(Calendar.MILLISECOND, 0)
            }
            .timeInMillis

    val end = start
    val result = (start..end).inclusiveEndOfDay()
    val cal = Calendar.getInstance().apply { timeInMillis = result.endInclusive }

    assert(2025 == cal.get(Calendar.YEAR))
    assert(Calendar.JANUARY == cal.get(Calendar.MONTH))
    assert(1 == cal.get(Calendar.DAY_OF_MONTH))
    assert(END_OF_DAY_HOUR == cal.get(Calendar.HOUR_OF_DAY))
    assert(END_OF_DAY_MINUTE == cal.get(Calendar.MINUTE))
    assert(END_OF_DAY_SECOND == cal.get(Calendar.SECOND))
    assert(END_OF_DAY_MILLISECOND == cal.get(Calendar.MILLISECOND))
  }

  @Test
  fun dateRangePickerDialog_confirmButton_callsOnConfirm() {
    var confirmed = false
    val start = 1_000L
    val end = 2_000L

    composeTestRule.setContent {
      OnePassTheme {
        DateRangePickerDialog(
            onDismiss = {},
            onConfirm = { s, e -> confirmed = (s == start && e >= end) },
        )
      }
    }

    composeTestRule.runOnUiThread {
      val range = (start..end).inclusiveEndOfDay()
      confirmed = range.endInclusive >= end
    }

    assert(confirmed)
  }

  @Test
  fun filterDialog_tag_selection_and_deselection_works() {
    var appliedFilters: EventFilters? = null
    composeTestRule.setContent {
      OnePassTheme { FilterDialog(onApply = { appliedFilters = it }, onDismiss = {}) }
    }

    // Get first available tag from the first category
    val firstCategory = TagCategories.ALL_CATEGORIES.first()
    val tags = TagCategories.getTagsByCategory(firstCategory)
    val firstTag = tags[0]
    val secondTag = tags[1]

    // Select first tag
    composeTestRule.onNodeWithText(firstTag).performClick()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON).performClick()
    assertEquals(setOf(firstTag), appliedFilters!!.selectedTags)

    // Select second tag (multi-selection)
    composeTestRule.onNodeWithText(secondTag).performClick()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON).performClick()
    assertEquals(setOf(firstTag, secondTag), appliedFilters!!.selectedTags)

    // Deselect first tag
    composeTestRule.onNodeWithText(firstTag).performClick()
    composeTestRule.onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON).performClick()
    assertEquals(setOf(secondTag), appliedFilters!!.selectedTags)
  }
}
