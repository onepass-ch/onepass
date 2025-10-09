package ch.onepass.onepass.ui.feed

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedScreenComposeTest {
  // THIS IS A VIRTUAL TEST FOR AN EMPTY CLASS. REMOVE DURING DEVELOPMENT.
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun renderFeedScreen() {
    composeTestRule.setContent { FeedScreen() }
    composeTestRule.onNodeWithText("FeedScreen").assertIsDisplayed()
  }
}
