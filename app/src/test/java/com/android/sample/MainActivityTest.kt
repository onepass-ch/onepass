package com.android.sample

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.resources.C
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for MainActivity */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun mainActivity_displaysGreeting() {
    // Then - Verify greeting is displayed
    composeTestRule.onNodeWithText("Hello Android!").assertIsDisplayed()
  }

  @Test
  fun mainActivity_hasMainScreenContainer() {
    // Then - Verify main screen container exists
    composeTestRule.onNodeWithTag(C.Tag.main_screen_container).assertExists()
  }

  @Test
  fun mainActivity_greetingHasCorrectTag() {
    // Then - Verify greeting has correct test tag
    composeTestRule.onNodeWithTag(C.Tag.greeting).assertExists()
    composeTestRule.onNodeWithTag(C.Tag.greeting).assertTextEquals("Hello Android!")
  }

  @Test
  fun mainActivity_containerIsDisplayed() {
    // Then - Verify main container is displayed
    composeTestRule.onNodeWithTag(C.Tag.main_screen_container).assertIsDisplayed()
  }

  @Test
  fun mainActivity_greetingTextIsCorrect() {
    // Then - Verify exact text content
    composeTestRule.onNodeWithText("Hello Android!", substring = false).assertExists()
  }

  @Test
  fun mainActivity_allComponentsExist() {
    // Then - Verify all components exist
    composeTestRule.onNodeWithTag(C.Tag.main_screen_container).assertExists()
    composeTestRule.onNodeWithTag(C.Tag.greeting).assertExists()
    composeTestRule.onNodeWithText("Hello Android!").assertExists()
  }

  @Test
  fun mainActivity_greetingIsVisible() {
    // Then - Verify greeting is visible
    composeTestRule.onNodeWithTag(C.Tag.greeting).assertIsDisplayed()
  }
}
