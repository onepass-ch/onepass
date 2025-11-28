package ch.onepass.onepass.ui.event

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.ui.components.buttons.LikeButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for LikeButton component */
@RunWith(AndroidJUnit4::class)
class LikeButtonTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun likeButton_initialStateAndAppearance_whenUnliked() {
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed().assertHasClickAction()

    composeTestRule.onNodeWithContentDescription("Unlike").assertDoesNotExist()
  }

  @Test
  fun likeButton_initialStateAndAppearance_whenLiked() {
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = true, onLikeToggle = {}) } }
    composeTestRule
        .onNodeWithContentDescription("Unlike")
        .assertIsDisplayed()
        .assertHasClickAction()

    composeTestRule.onNodeWithContentDescription("Like").assertDoesNotExist()
  }

  @Test
  fun likeButton_togglesStateAndTriggersCallback_fromUnlikedToLiked() {
    var callbackValue: Boolean? = null
    composeTestRule.setContent {
      MaterialTheme {
        var isLiked by remember { mutableStateOf(false) }
        LikeButton(
            isLiked = isLiked,
            onLikeToggle = {
              isLiked = it
              callbackValue = it
            })
      }
    }
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
    assertEquals("Callback should be called with true", true, callbackValue)
  }

  @Test
  fun likeButton_togglesStateAndTriggersCallback_fromLikedToUnliked() {
    // Given
    var callbackValue: Boolean? = null
    composeTestRule.setContent {
      MaterialTheme {
        var isLiked by remember { mutableStateOf(true) }
        LikeButton(
            isLiked = isLiked,
            onLikeToggle = {
              isLiked = it
              callbackValue = it
            })
      }
    }
    composeTestRule.onNodeWithContentDescription("Unlike").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
    assertEquals("Callback should be called with false", false, callbackValue)
  }

  @Test
  fun likeButton_handlesMultipleRapidToggleCycles() {
    // Given
    var clickCount = 0
    composeTestRule.setContent {
      MaterialTheme {
        var isLiked by remember { mutableStateOf(false) }
        LikeButton(
            isLiked = isLiked,
            onLikeToggle = {
              isLiked = it
              clickCount++
            })
      }
    }

    // When toggling multiple times
    // Like -> Unlike
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()

    // Unlike -> Like
    composeTestRule.onNodeWithContentDescription("Unlike").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Like").assertExists()

    // Like -> Unlike
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()

    // Then all clicks should be registered
    assertEquals("Callback should be triggered 3 times", 3, clickCount)
  }
}
