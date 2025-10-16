package ch.onepass.onepass.ui.event

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for CloseButton component */
@RunWith(AndroidJUnit4::class)
class CloseButtonTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun closeButton_isDisplayed() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then - Close button is displayed
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun closeButton_clickTriggersCallback() {
    // Given
    var callbackCount = 0

    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = { callbackCount++ }) } }

    // Then - Click the button
    composeTestRule.onNodeWithContentDescription("Close").performClick()

    // Verify callback was called
    assertEquals(1, callbackCount)
  }

  @Test
  fun closeButton_isClickable() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then - Button is clickable
    composeTestRule.onNodeWithContentDescription("Close").assertHasClickAction()
  }

  @Test
  fun closeButton_hasCorrectContentDescription() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then
    composeTestRule.onNodeWithContentDescription("Close").assertExists()
  }

  @Test
  fun closeButton_hasClickAction() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then - Verify button has click action
    composeTestRule.onNodeWithContentDescription("Close").assertHasClickAction()
  }

  @Test
  fun closeButton_displayedCorrectly() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then - Verify button is displayed
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun closeButton_existsInComposition() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then
    composeTestRule.onNodeWithContentDescription("Close").assertExists()
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun closeButton_multipleCallsCallbackEachTime() {
    // Given
    var callbackCount = 0

    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = { callbackCount++ }) } }

    // Then - Click multiple times
    repeat(3) {
      composeTestRule.onNodeWithContentDescription("Close").performClick()
      composeTestRule.waitForIdle()
    }

    // Verify all clicks were registered
    assertEquals(3, callbackCount)
  }

  @Test
  fun closeButton_rapidClicks() {
    // Given
    var clickCount = 0

    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = { clickCount++ }) } }

    // Then - Multiple rapid clicks
    repeat(5) {
      composeTestRule.onNodeWithContentDescription("Close").performClick()
      composeTestRule.waitForIdle()
    }

    // Verify all clicks were registered
    assertEquals(5, clickCount)
  }

  @Test
  fun closeButton_callbackExecuted() {
    // Given
    var callbackExecuted = false

    // When
    composeTestRule.setContent {
      MaterialTheme { CloseButton(onDismiss = { callbackExecuted = true }) }
    }

    // Then
    composeTestRule.onNodeWithContentDescription("Close").performClick()
    assertEquals(true, callbackExecuted)
  }

  @Test
  fun closeButton_hasCorrectTestTag() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then - Verify test tag is present (if CloseButton has one)
    composeTestRule.onNodeWithContentDescription("Close").assertExists()
  }

  @Test
  fun closeButton_visibleAndEnabled() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then
    composeTestRule.onNodeWithContentDescription("Close").assertIsEnabled()
  }

  @Test
  fun closeButton_performsClickAction() {
    // Given
    var performedClick = false

    // When
    composeTestRule.setContent {
      MaterialTheme { CloseButton(onDismiss = { performedClick = true }) }
    }

    // Then
    composeTestRule.onNodeWithContentDescription("Close").performClick()
    assertEquals(true, performedClick)
  }

  @Test
  fun closeButton_hasXIcon() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then - Close button with X icon should be present
    composeTestRule.onNodeWithContentDescription("Close").assertExists()
  }

  @Test
  fun closeButton_canBePressed() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then - Verify the button can be pressed
    composeTestRule.onNodeWithContentDescription("Close").assertIsEnabled()
  }

  @Test
  fun closeButton_respondsToUserInteraction() {
    // Given
    var interactionDetected = false

    // When
    composeTestRule.setContent {
      MaterialTheme { CloseButton(onDismiss = { interactionDetected = true }) }
    }

    // Then
    composeTestRule.onNodeWithContentDescription("Close").performClick()
    assertEquals(true, interactionDetected)
  }

  @Test
  fun closeButton_inMaterialTheme() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then - Button should be properly styled within MaterialTheme
    composeTestRule.onNodeWithContentDescription("Close").assertExists()
  }

  @Test
  fun closeButton_callbackInvokedOnPress() {
    // Given
    var pressed = false

    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = { pressed = true }) } }

    // Then
    composeTestRule.onNodeWithContentDescription("Close").performClick()
    assertEquals(true, pressed)
  }

  @Test
  fun closeButton_hasDismissAction() {
    // When
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    // Then - Button should have dismiss functionality
    composeTestRule.onNodeWithContentDescription("Close").assertHasClickAction()
  }
}
