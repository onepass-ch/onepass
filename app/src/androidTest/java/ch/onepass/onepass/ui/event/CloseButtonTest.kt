package ch.onepass.onepass.ui.event

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloseButtonTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun closeButton_renders_and_is_accessible() {
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = {}) } }

    val node = composeTestRule.onNodeWithContentDescription("Close")
    node.assertIsDisplayed()
    node.assertHasClickAction()
  }

  @Test
  fun closeButton_click_triggers_callback() {
    var called = false
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = { called = true }) } }

    composeTestRule.onNodeWithContentDescription("Close").performClick()
    composeTestRule.waitForIdle()
    assertTrue(called)
  }

  @Test
  fun closeButton_multiple_clicks_registered() {
    var count = 0
    composeTestRule.setContent { MaterialTheme { CloseButton(onDismiss = { count++ }) } }

    repeat(3) {
      composeTestRule.onNodeWithContentDescription("Close").performClick()
      composeTestRule.waitForIdle()
    }

    assertEquals(3, count)
  }
}
