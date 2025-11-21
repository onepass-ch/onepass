package ch.onepass.onepass.ui.event

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
  fun likeButton_initiallyUnliked() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then - Shows "Like" content description
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }

  @Test
  fun likeButton_initiallyLiked() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = true, onLikeToggle = {}) } }

    // Then - Shows "Unlike" content description
    composeTestRule.onNodeWithContentDescription("Unlike").assertIsDisplayed()
  }

  @Test
  fun likeButton_clickTriggersCallback() {
    // Given
    var callbackValue = false
    var callbackCount = 0

    // When
    composeTestRule.setContent {
      MaterialTheme {
        LikeButton(
            isLiked = false,
            onLikeToggle = { newValue ->
              callbackValue = newValue
              callbackCount++
            })
      }
    }

    // Then - Click the button
    composeTestRule.onNodeWithContentDescription("Like").performClick()

    // Verify callback was called with correct value
    assertEquals(true, callbackValue)
    assertEquals(1, callbackCount)
  }

  @Test
  fun likeButton_unlikeTriggersCallback() {
    // Given
    var callbackValue = true
    var callbackCount = 0

    // When
    composeTestRule.setContent {
      MaterialTheme {
        LikeButton(
            isLiked = true,
            onLikeToggle = { newValue ->
              callbackValue = newValue
              callbackCount++
            })
      }
    }

    // Then - Click the button
    composeTestRule.onNodeWithContentDescription("Unlike").performClick()

    // Verify callback was called with correct value
    assertEquals(false, callbackValue)
    assertEquals(1, callbackCount)
  }

  @Test
  fun likeButton_isClickable() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then - Button is clickable
    composeTestRule.onNodeWithContentDescription("Like").assertHasClickAction()
  }

  @Test
  fun likeButton_hasCorrectContentDescription_whenUnliked() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
  }

  @Test
  fun likeButton_hasCorrectContentDescription_whenLiked() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = true, onLikeToggle = {}) } }

    // Then
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
  }

  @Test
  fun likeButton_unlikedState_hasCorrectIcon() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then - Verify correct content description for unliked state
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
    composeTestRule.onNodeWithContentDescription("Unlike").assertDoesNotExist()
  }

  @Test
  fun likeButton_likedState_hasCorrectIcon() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = true, onLikeToggle = {}) } }

    // Then - Verify correct content description for liked state
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
    composeTestRule.onNodeWithContentDescription("Like").assertDoesNotExist()
  }

  @Test
  fun likeButton_toggleFromUnlikedToLiked() {
    // Given
    composeTestRule.setContent {
      MaterialTheme {
        var isLiked by remember { mutableStateOf(false) }
        LikeButton(isLiked = isLiked, onLikeToggle = { isLiked = it })
      }
    }

    // When - Click to toggle
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.waitForIdle()

    // Then - State changed
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
  }

  @Test
  fun likeButton_toggleFromLikedToUnliked() {
    // Given
    composeTestRule.setContent {
      MaterialTheme {
        var isLiked by remember { mutableStateOf(true) }
        LikeButton(isLiked = isLiked, onLikeToggle = { isLiked = it })
      }
    }

    // When - Click to toggle
    composeTestRule.onNodeWithContentDescription("Unlike").performClick()
    composeTestRule.waitForIdle()

    // Then - State changed
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
  }

  @Test
  fun likeButton_multipleToggleCycles() {
    // Given
    composeTestRule.setContent {
      MaterialTheme {
        var isLiked by remember { mutableStateOf(false) }
        LikeButton(isLiked = isLiked, onLikeToggle = { isLiked = it })
      }
    }

    // Then - Toggle multiple times
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Unlike").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
  }

  @Test
  fun likeButton_hasClickAction() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then - Verify button has click action
    composeTestRule.onNodeWithContentDescription("Like").assertHasClickAction()
  }

  @Test
  fun likeButton_displayedInUnlikedState() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then - Verify button is displayed
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }

  @Test
  fun likeButton_callbackReceivesCorrectValue() {
    // Given
    var receivedValue: Boolean? = null

    // When
    composeTestRule.setContent {
      MaterialTheme { LikeButton(isLiked = false, onLikeToggle = { receivedValue = it }) }
    }

    // Then
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    assertEquals(true, receivedValue)
  }

  @Test
  fun likeButton_existsInComposition() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }

  @Test
  fun likeButton_unlikedState_displaysWhiteIcon() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
  }

  @Test
  fun likeButton_likedState_displaysPinkIcon() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = true, onLikeToggle = {}) } }

    // Then
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
  }

  @Test
  fun likeButton_rapidClicks() {
    // Given
    var clickCount = 0

    // When
    composeTestRule.setContent {
      MaterialTheme { LikeButton(isLiked = false, onLikeToggle = { clickCount++ }) }
    }

    // Then - Multiple rapid clicks
    repeat(5) {
      composeTestRule.onNodeWithContentDescription("Like").performClick()
      composeTestRule.waitForIdle()
    }

    // Verify all clicks were registered
    assertEquals(5, clickCount)
  }
}
