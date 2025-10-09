package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.resources.C
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for Greeting composable */
@RunWith(AndroidJUnit4::class)
class GreetingTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_displaysCorrectText() {
    // When
    composeTestRule.setContent { MaterialTheme { Greeting("World") } }

    // Then
    composeTestRule.onNodeWithText("Hello World!").assertIsDisplayed()
  }

  @Test
  fun greeting_withAlice() {
    composeTestRule.setContent { MaterialTheme { Greeting("Alice") } }
    composeTestRule.onNodeWithText("Hello Alice!").assertExists()
  }

  @Test
  fun greeting_withBob() {
    composeTestRule.setContent { MaterialTheme { Greeting("Bob") } }
    composeTestRule.onNodeWithText("Hello Bob!").assertExists()
  }

  @Test
  fun greeting_withEmptyString() {
    composeTestRule.setContent { MaterialTheme { Greeting("") } }
    composeTestRule.onNodeWithText("Hello !").assertExists()
  }

  @Test
  fun greeting_hasCorrectTestTag() {
    // When
    composeTestRule.setContent { MaterialTheme { Greeting("Test") } }

    // Then
    composeTestRule.onNodeWithTag(C.Tag.greeting).assertExists()
  }

  @Test
  fun greeting_textIsDisplayed() {
    // When
    composeTestRule.setContent { MaterialTheme { Greeting("Kotlin") } }

    // Then
    composeTestRule.onNodeWithText("Hello Kotlin!").assertIsDisplayed()
  }
}
