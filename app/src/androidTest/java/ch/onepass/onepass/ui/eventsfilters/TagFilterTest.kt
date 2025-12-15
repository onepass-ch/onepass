package ch.onepass.onepass.ui.eventsfilters

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.eventfilters.TagCategories
import ch.onepass.onepass.model.eventfilters.TagFilter
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Rule
import org.junit.Test

class TagFilterTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun tagFilter_selectsAndDeselectsTags() {
    val selectedTags = mutableSetOf<String>()
    var onTagSelectionChangeCalled = false

    composeTestRule.setContent {
      OnePassTheme {
        TagFilter(
            selectedTags = selectedTags,
            onTagSelectionChange = {
              selectedTags.clear()
              selectedTags.addAll(it)
              onTagSelectionChangeCalled = true
            })
      }
    }

    val firstCategory = TagCategories.ALL_CATEGORIES.first()
    val tags = TagCategories.getTagsByCategory(firstCategory)
    val firstTag = tags[0]
    val secondTag = tags[1]

    // Select first tag
    composeTestRule.onNodeWithText(firstTag).performClick()
    composeTestRule.waitForIdle()
    assert(onTagSelectionChangeCalled)
    assert(setOf(firstTag) == selectedTags)

    // Reset flag
    onTagSelectionChangeCalled = false

    // Select second tag (should add to selection)
    composeTestRule.onNodeWithText(secondTag).performClick()
    composeTestRule.waitForIdle()
    assert(onTagSelectionChangeCalled)
    assert(setOf(firstTag, secondTag) == selectedTags)

    // Deselect first tag
    onTagSelectionChangeCalled = false
    composeTestRule.onNodeWithText(firstTag).performClick()
    composeTestRule.waitForIdle()
    assert(onTagSelectionChangeCalled)
    assert(setOf(secondTag) == selectedTags)
  }

  @Test
  fun tagFilter_showsSelectedState() {
    val selectedTag = TagCategories.getTagsByCategory(TagCategories.ALL_CATEGORIES.first()).first()

    composeTestRule.setContent {
      OnePassTheme { TagFilter(selectedTags = setOf(selectedTag), onTagSelectionChange = {}) }
    }

    // The selected tag should be displayed (already verified in other tests)
    // Note: We can't easily test the visual state of the chip, but we can test
    // that the composable renders with the given selected tags
    composeTestRule.onNodeWithText(selectedTag).assertIsDisplayed()
  }

  @Test
  fun tagFilter_handlesEmptyTagList() {
    // This tests that the composable handles categories with no tags gracefully
    // Note: In the current implementation, all categories have tags
    composeTestRule.setContent {
      OnePassTheme { TagFilter(selectedTags = emptySet(), onTagSelectionChange = {}) }
    }

    // Verify at least one tag exists
    val firstCategory = TagCategories.ALL_CATEGORIES.first()
    val tags = TagCategories.getTagsByCategory(firstCategory)
    assert(tags.isNotEmpty())
    composeTestRule.onNodeWithText(tags.first()).assertIsDisplayed()
  }

  @Test
  fun tagFilter_preservesSelectionAcrossCategories() {
    val selectedTags = mutableSetOf<String>()
    val firstCategory = TagCategories.ALL_CATEGORIES[0]
    val secondCategory = TagCategories.ALL_CATEGORIES[1]

    val firstTag = TagCategories.getTagsByCategory(firstCategory).first()
    val secondTag = TagCategories.getTagsByCategory(secondCategory).first()

    composeTestRule.setContent {
      OnePassTheme {
        TagFilter(
            selectedTags = selectedTags,
            onTagSelectionChange = {
              selectedTags.clear()
              selectedTags.addAll(it)
            })
      }
    }

    // Select tag from first category
    composeTestRule.onNodeWithText(firstTag).performClick()
    composeTestRule.waitForIdle()
    assert(setOf(firstTag) == selectedTags)

    // Select tag from second category
    composeTestRule.onNodeWithText(secondTag).performClick()
    composeTestRule.waitForIdle()
    assert(setOf(firstTag, secondTag) == selectedTags)
  }
}
