package ch.onepass.onepass.ui.event

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.components.buttons.LikeButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for LikeButton component */
@RunWith(AndroidJUnit4::class)
class LikeButtonTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  @Test
  fun likeButton_initialStateAndAppearance_whenUnliked() {
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }
    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.button_like_description))
        .assertIsDisplayed()
        .assertHasClickAction()

    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.button_unlike_description))
        .assertDoesNotExist()
  }

  @Test
  fun likeButton_initialStateAndAppearance_whenLiked() {
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = true, onLikeToggle = {}) } }
    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.button_unlike_description))
        .assertIsDisplayed()
        .assertHasClickAction()

    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.button_like_description))
        .assertDoesNotExist()
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
    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.button_like_description))
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.button_unlike_description))
        .assertExists()
    assertEquals("Callback should be called with true", true, callbackValue)
  }

  @Test
  fun likeButton_togglesStateAndTriggersCallback_fromLikedToUnliked() {
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
    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.button_unlike_description))
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.button_like_description))
        .assertExists()
    assertEquals("Callback should be called with false", false, callbackValue)
  }

  @Test
  fun likeButton_handlesMultipleRapidToggleCycles() {
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

    val likeDesc = context.getString(R.string.button_like_description)
    val unlikeDesc = context.getString(R.string.button_unlike_description)

    // When toggling multiple times
    // Like -> Unlike
    composeTestRule.onNodeWithContentDescription(likeDesc).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription(unlikeDesc).assertExists()

    // Unlike -> Like
    composeTestRule.onNodeWithContentDescription(unlikeDesc).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription(likeDesc).assertExists()

    // Like -> Unlike
    composeTestRule.onNodeWithContentDescription(likeDesc).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription(unlikeDesc).assertExists()

    // Then all clicks should be registered
    assertEquals("Callback should be triggered 3 times", 3, clickCount)
  }
}
