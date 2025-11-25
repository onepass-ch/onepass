package ch.onepass.onepass.ui.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ch.onepass.onepass.model.notification.Notification
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Rule
import org.junit.Test

class NotificationItemTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun notificationItem_displaysCorrectContent() {
    val notification = Notification(title = "Test Title", body = "Test Body", isRead = false)

    composeTestRule.setContent {
      OnePassTheme { NotificationItem(notification = notification, onClick = {}, onDelete = {}) }
    }

    composeTestRule.onNodeWithText("Test Title").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test Body").assertIsDisplayed()
  }

  @Test
  fun notificationItem_displaysTimestamp_whenPresent() {
    val now = Date()
    val timestamp = Timestamp(now)

    val notification =
        Notification(title = "Dated Notification", body = "Has a date", createdAt = timestamp)

    composeTestRule.setContent {
      OnePassTheme { NotificationItem(notification = notification, onClick = {}, onDelete = {}) }
    }

    composeTestRule.onNodeWithText("Dated Notification").assertIsDisplayed()
  }

  @Test
  fun notificationItem_clicksTriggersCallbacks() {
    var clicked = false
    var deleted = false
    val notification = Notification(title = "Test")

    composeTestRule.setContent {
      OnePassTheme {
        NotificationItem(
            notification = notification,
            onClick = { clicked = true },
            onDelete = { deleted = true })
      }
    }

    // Click the card content (title)
    composeTestRule.onNodeWithText("Test").performClick()
    assert(clicked)

    // Click delete button
    composeTestRule.onNodeWithContentDescription("Delete notification").performClick()
    assert(deleted)
  }
}
