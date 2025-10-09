package com.android.sample.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.resources.C
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Integration tests for LikeButton component */
@RunWith(AndroidJUnit4::class)
class LikeButtonIntegrationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun likeButton_startsInUnlikedState() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then - Verify button is displayed with correct content description
    composeTestRule.onNodeWithTag(C.Tag.like_button).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }

  @Test
  fun likeButton_startsInLikedState() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = true, onLikeToggle = {}) } }

    // Then - Verify button is displayed with correct content description
    composeTestRule.onNodeWithTag(C.Tag.like_button).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Unlike").assertIsDisplayed()
  }

  @Test
  fun likeButton_iconIsVisible() {
    // When
    composeTestRule.setContent { MaterialTheme { LikeButton(isLiked = false, onLikeToggle = {}) } }

    // Then - Verify button is visible (icon is part of the button)
    composeTestRule.onNodeWithTag(C.Tag.like_button).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
  }

  @Test
  fun likeButton_toggleStateWorks() {
    // Given
    var isLiked by mutableStateOf(false)

    // When
    composeTestRule.setContent {
      MaterialTheme { LikeButton(isLiked = isLiked, onLikeToggle = { isLiked = it }) }
    }

    // Then - Initially unliked
    composeTestRule.onNodeWithContentDescription("Like").assertExists()

    // When - Click to like
    composeTestRule.onNodeWithTag(C.Tag.like_button).performClick()

    // Then - Now liked
    composeTestRule.onNodeWithContentDescription("Unlike").assertExists()
  }
}
