package ch.onepass.onepass.ui.feed

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import ch.onepass.onepass.ui.theme.OnePassTheme
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test

class FeedScreenSearchBarBehaviorTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private var lastClick: GlobalSearchItemClick? = null
  private var lastNavigatedEventId: String? = null
  private var notificationsOpened = false

  private fun setContent() {
    lastClick = null
    lastNavigatedEventId = null
    notificationsOpened = false

    composeTestRule.setContent {
      OnePassTheme {
        FeedScreen(
            onNavigateToEvent = { id -> lastNavigatedEventId = id },
            globalSearchItemClickListener =
                GlobalSearchItemClickListener { click -> lastClick = click },
            onNavigateToNotifications = { notificationsOpened = true },
        )
      }
    }
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
  fun searchBar_acceptsTextInput_and_switchesToSearchBranch() {
    setContent()

    val query = "concert"

    val searchNode =
        composeTestRule.onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)

    searchNode.performClick()
    searchNode.performTextClearance()
    searchNode.performClick()
    searchNode.performTextInput(query)

    searchNode.assertTextContains(query, substring = false)

    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
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

  @Test
  fun whenNoSearchQuery_feedContentIsShownOrEmptyState() {
    setContent()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()

    composeTestRule.waitForIdle()

    var anyPossible = false

    try {
      composeTestRule.onAllNodes(hasTestTagStartingWith("eventItem_")).onFirst().assertIsDisplayed()
      anyPossible = true
    } catch (_: AssertionError) {}

    if (!anyPossible) {
      try {
        composeTestRule.onNodeWithTag(FeedScreenTestTags.LOADING_INDICATOR).assertExists()
        anyPossible = true
      } catch (_: AssertionError) {}

      try {
        composeTestRule.onNodeWithTag(FeedScreenTestTags.EMPTY_STATE).assertExists()
        anyPossible = true
      } catch (_: AssertionError) {}

      try {
        composeTestRule.onNodeWithTag(FeedScreenTestTags.ERROR_MESSAGE).assertExists()
        anyPossible = true
      } catch (_: AssertionError) {}
    }

    assert(anyPossible)
  }

  @Test
  fun notifications_button_triggersCallback() {
    setContent()

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.NOTIFICATION_BUTTON)
        .assertIsDisplayed()
        .performClick()

    assertEquals(true, notificationsOpened)
  }

  @Test
  fun filter_button_isClickable() {
    setContent()

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.FILTER_BUTTON)
        .assertIsDisplayed()
        .performClick()
  }

  @Test
  fun whenSearchQueryIsNonBlank_searchBranchIsUsed() {
    setContent()

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("anything")

    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
  }

  @Test
  fun tapOnEventInFeed_triggersNavigation_whenEventsExist() {
    setContent()

    val allEvents = composeTestRule.onAllNodesWithTagPrefix("eventItem_")
    if (allEvents.fetchSemanticsNodes().isNotEmpty()) {
      allEvents.onFirst().performClick()
      assert(lastNavigatedEventId != null)
    }
  }

  // Helper extension to find nodes whose tag starts with a prefix
  private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.onFirst() = this[0]

  private fun AndroidComposeTestRule<*, *>.onAllNodesWithTagPrefix(prefix: String) =
      this.onAllNodes(hasTestTagStartingWith(prefix))

  fun hasTestTagStartingWith(prefix: String): SemanticsMatcher =
      SemanticsMatcher("Has test tag starting with \"$prefix\"") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag != null && tag.startsWith(prefix)
      }
}
