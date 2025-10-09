package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.resources.C
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for GreetingRobo composable */
@RunWith(AndroidJUnit4::class)
class GreetingRoboTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greetingRobo_displaysCorrectText() {
    // When
    composeTestRule.setContent { MaterialTheme { GreetingRobo("Test") } }

    // Then
    composeTestRule.onNodeWithText("Hello Test!").assertIsDisplayed()
  }

  @Test
  fun greetingRobo_withRobolectric() {
    // When
    composeTestRule.setContent { MaterialTheme { GreetingRobo("Robolectric") } }

    // Then
    composeTestRule.onNodeWithText("Hello Robolectric!").assertIsDisplayed()
  }

  @Test
  fun greetingRobo_hasCorrectTestTag() {
    // When
    composeTestRule.setContent { MaterialTheme { GreetingRobo("Tag") } }

    // Then
    composeTestRule.onNodeWithTag(C.Tag.greeting_robo).assertExists()
  }

  @Test
  fun greetingRobo_withEmptyString() {
    // When
    composeTestRule.setContent { MaterialTheme { GreetingRobo("") } }

    // Then
    composeTestRule.onNodeWithText("Hello !").assertExists()
  }

  @Test
  fun greetingRobo_withSpecialCharacters() {
    // When
    composeTestRule.setContent { MaterialTheme { GreetingRobo("@#$%") } }

    // Then
    composeTestRule.onNodeWithText("Hello @#$%!").assertExists()
  }

  @Test
  fun greetingRobo_textIsVisible() {
    // When
    composeTestRule.setContent { MaterialTheme { GreetingRobo("Visible") } }

    // Then
    composeTestRule.onNodeWithText("Hello Visible!").assertIsDisplayed()
  }
}
