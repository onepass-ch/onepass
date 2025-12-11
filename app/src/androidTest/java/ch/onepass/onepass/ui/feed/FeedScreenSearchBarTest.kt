package ch.onepass.onepass.ui.feed

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Rule
import org.junit.Test

class FeedScreenSearchBarTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun setContent() {
    composeTestRule.setContent { OnePassTheme { FeedScreen() } }
  }

  @Test
  fun searchBar_isVisible_andClickable() {
    setContent()

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun searchBar_acceptsTextInput() {
    setContent()

    val query = "concert"

    val searchNode =
        composeTestRule.onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)

    searchNode.performClick()
    searchNode.performTextClearance()
    searchNode.performClick()
    searchNode.performTextInput(query)

    searchNode.assertTextContains(query, substring = false)
  }

  @Test
  fun searchBar_clearThenRetype_works() {
    setContent()

    val firstQuery = "rock"
    val secondQuery = "jazz"

    val searchNode =
        composeTestRule.onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)

    searchNode.performClick()
    searchNode.performTextInput(firstQuery)
    searchNode.assertTextContains(firstQuery, substring = false)

    searchNode.performTextClearance()
    searchNode.performClick()
    searchNode.performTextInput(secondQuery)

    searchNode.assertTextContains(secondQuery, substring = false)
  }
}
