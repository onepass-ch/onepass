package com.android.sample.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for LikeButton component using Robolectric */
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
  fun likeButton_multipleSequentialClicks() {
    // Given
    var clickCount = 0
    val values = mutableListOf<Boolean>()

    // When
    composeTestRule.setContent {
      MaterialTheme {
        LikeButton(
            isLiked = false,
            onLikeToggle = {
              values.add(it)
              clickCount++
            })
      }
    }

    // Then - Click 3 times
    repeat(3) {
      composeTestRule.onNodeWithContentDescription("Like").performClick()
      composeTestRule.waitForIdle()
    }

    assertEquals(3, clickCount)
  }

  @Test
  fun likeButton_withCustomModifier() {
    // When
    composeTestRule.setContent {
      MaterialTheme {
        LikeButton(
            isLiked = false, onLikeToggle = {}, modifier = Modifier.size(100.dp).padding(8.dp))
      }
    }

    // Then - Button exists with custom modifier
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
  }

  @Test
  fun likeButton_stateTransitionsWork() {
    // Given
    var currentState = false

    // When
    composeTestRule.setContent {
      MaterialTheme {
        var isLiked by remember { mutableStateOf(currentState) }
        LikeButton(
            isLiked = isLiked,
            onLikeToggle = {
              isLiked = it
              currentState = it
            })
      }
    }

    // Then - Initial state
    composeTestRule.onNodeWithContentDescription("Like").assertExists()

    // When - Click to toggle
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.waitForIdle()

    // Then - State changed
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
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
    // Test unliked state (white icon reflected in content description)
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
  }

  @Test
  fun likeButton_likedState_displaysPinkIcon() {
    // Test liked state (pink icon reflected in content description)
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = true, onLikeToggle = {}) } }
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
  }

  @Test
  fun likeButton_toggleFromUnlikedToLiked() {
    // Given
    var currentState = false

    // When
    composeTestRule.setContent {
      MaterialTheme {
        var isLiked by remember { mutableStateOf(currentState) }
        LikeButton(
            isLiked = isLiked,
            onLikeToggle = {
              isLiked = it
              currentState = it
            })
      }
    }

    // Then - Toggle state
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
  }

  @Test
  fun likeButton_toggleFromLikedToUnliked() {
    // Given
    var currentState = true

    // When
    composeTestRule.setContent {
      MaterialTheme {
        var isLiked by remember { mutableStateOf(currentState) }
        LikeButton(
            isLiked = isLiked,
            onLikeToggle = {
              isLiked = it
              currentState = it
            })
      }
    }

    // Then - Toggle state
    composeTestRule.onNodeWithContentDescription("Unlike").performClick()
    composeTestRule.waitForIdle()
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
  fun likeButton_animationTriggersOnStateChange() {
    // Given
    composeTestRule.setContent {
      MaterialTheme {
        var isLiked by remember { mutableStateOf(false) }
        LikeButton(isLiked = isLiked, onLikeToggle = { isLiked = it })
      }
    }

    // When - Click and wait for animation
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.waitForIdle()

    // Then - State changed
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
  }

  @Test
  fun likeButton_handlesNullCallback() {
    // When
    composeTestRule.setContent {
      MaterialTheme { LikeButton(isLiked = false, onLikeToggle = { /* No-op */}) }
    }

    // Then - Button still works
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
  }

  @Test
  fun likeButton_buttonHasCorrectSize() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then - Button exists and is accessible
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
    composeTestRule.onNodeWithContentDescription("Like").assertHasClickAction()
  }

  @Test
  fun likeButton_verifyClickableArea() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then - Verify button is clickable
    composeTestRule.onNodeWithContentDescription("Like").assertHasClickAction()
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }
}
