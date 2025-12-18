package ch.onepass.onepass.ui.notification

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import ch.onepass.onepass.R
import ch.onepass.onepass.model.notification.Notification
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Rule
import org.junit.Test

class NotificationItemTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

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
    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.notifications_delete_description))
        .performClick()
    assert(deleted)
  }
}
