package com.android.sample

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.resources.C
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Comprehensive tests for SecondActivity */
@RunWith(AndroidJUnit4::class)
class SecondActivityTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<SecondActivity>()

  @Test
  fun secondActivity_displaysGreeting() {
    // Then - Verify greeting is displayed
    composeTestRule.onNodeWithText("Hello Robolectric!").assertIsDisplayed()
  }

  @Test
  fun secondActivity_hasSecondScreenContainer() {
    // Then - Verify second screen container exists
    composeTestRule.onNodeWithTag(C.Tag.second_screen_container).assertExists()
  }

  @Test
  fun secondActivity_greetingHasCorrectTag() {
    // Then - Verify greeting has correct test tag
    composeTestRule.onNodeWithTag(C.Tag.greeting_robo).assertExists()
    composeTestRule.onNodeWithTag(C.Tag.greeting_robo).assertTextEquals("Hello Robolectric!")
  }

  @Test
  fun secondActivity_containerIsDisplayed() {
    // Then - Verify container is displayed
    composeTestRule.onNodeWithTag(C.Tag.second_screen_container).assertIsDisplayed()
  }

  @Test
  fun secondActivity_greetingTextIsCorrect() {
    // Then - Verify exact text content
    composeTestRule.onNodeWithText("Hello Robolectric!", substring = false).assertExists()
  }

  @Test
  fun secondActivity_screenComponentsExist() {
    // Then - Verify all key components exist
    composeTestRule.onNodeWithTag(C.Tag.second_screen_container).assertExists()
    composeTestRule.onNodeWithTag(C.Tag.greeting_robo).assertExists()
    composeTestRule.onNodeWithText("Hello Robolectric!").assertExists()
  }
}
