package ch.onepass.onepass.ui.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import ch.onepass.onepass.model.notification.Notification
import ch.onepass.onepass.model.notification.NotificationType
import ch.onepass.onepass.ui.theme.OnePassTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class NotificationScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val mockViewModel = mockk<NotificationsViewModel>(relaxed = true)
  private val mockNavController = mockk<NavController>(relaxed = true)
  private val uiStateFlow = MutableStateFlow(NotificationsUiState())

  init {
    every { mockViewModel.uiState } returns uiStateFlow
  }

  @Test
  fun notificationScreen_loadingState() {
    uiStateFlow.value = NotificationsUiState(isLoading = true)

    composeTestRule.setContent {
      OnePassTheme { NotificationsScreen(mockNavController, mockViewModel) }
    }

    composeTestRule.onNodeWithTag("notification_loading").assertIsDisplayed()
  }

  @Test
  fun notificationScreen_emptyState() {
    uiStateFlow.value = NotificationsUiState(isLoading = false, notifications = emptyList())

    composeTestRule.setContent {
      OnePassTheme { NotificationsScreen(mockNavController, mockViewModel) }
    }

    composeTestRule.onNodeWithTag("notification_empty").assertIsDisplayed()
    composeTestRule.onNodeWithText("No notifications yet").assertIsDisplayed()
  }

  @Test
  fun notificationScreen_contentState() {
    val notifications =
        listOf(
            Notification(notificationId = "1", title = "Title 1", body = "Body 1"),
            Notification(notificationId = "2", title = "Title 2", body = "Body 2"))
    uiStateFlow.value = NotificationsUiState(isLoading = false, notifications = notifications)

    composeTestRule.setContent {
      OnePassTheme { NotificationsScreen(mockNavController, mockViewModel) }
    }

    composeTestRule.onNodeWithText("Title 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Body 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Title 2").assertIsDisplayed()
  }

  @Test
  fun notificationScreen_markAllReadAction() {
    uiStateFlow.value = NotificationsUiState(unreadCount = 5)

    composeTestRule.setContent {
      OnePassTheme { NotificationsScreen(mockNavController, mockViewModel) }
    }

    composeTestRule.onNodeWithText("Mark all as read").performClick()
    verify { mockViewModel.markAllAsRead() }
  }

  @Test
  fun notificationScreen_interactions_deepLink() {
    val notif =
        Notification(notificationId = "1", title = "Deep Link", deepLink = "onepass://event/123")
    uiStateFlow.value = NotificationsUiState(notifications = listOf(notif))

    composeTestRule.setContent {
      OnePassTheme { NotificationsScreen(mockNavController, mockViewModel) }
    }

    composeTestRule.onNodeWithText("Deep Link").performClick()

    verify { mockViewModel.markAsRead("1") }
    verify { mockNavController.navigate("event_details/123") }
  }

  @Test
  fun notificationScreen_interactions_eventCancelled() {
    val notif =
        Notification(
            notificationId = "2",
            title = "Cancelled",
            type = NotificationType.EVENT_CANCELLED,
            eventId = "ev1")
    uiStateFlow.value = NotificationsUiState(notifications = listOf(notif))

    composeTestRule.setContent {
      OnePassTheme { NotificationsScreen(mockNavController, mockViewModel) }
    }

    composeTestRule.onNodeWithText("Cancelled").performClick()
    verify { mockNavController.navigate("event_details/ev1") }
  }

  @Test
  fun notificationScreen_interactions_orgInvitation() {
    val notif =
        Notification(
            notificationId = "3",
            title = "Invite",
            type = NotificationType.ORGANIZATION_INVITATION,
            organizationId = "org1")
    uiStateFlow.value = NotificationsUiState(notifications = listOf(notif))

    composeTestRule.setContent {
      OnePassTheme { NotificationsScreen(mockNavController, mockViewModel) }
    }

    composeTestRule.onNodeWithText("Invite").performClick()
    verify { mockNavController.navigate("organization_details/org1") }
  }

  @Test
  fun notificationScreen_interactions_ticketPurchased() {
    val notif =
        Notification(
            notificationId = "4", title = "Ticket", type = NotificationType.TICKET_PURCHASED)
    uiStateFlow.value = NotificationsUiState(notifications = listOf(notif))

    composeTestRule.setContent {
      OnePassTheme { NotificationsScreen(mockNavController, mockViewModel) }
    }

    composeTestRule.onNodeWithText("Ticket").performClick()
    verify { mockNavController.navigate("tickets") }
  }

  @Test
  fun notificationScreen_interactions_delete() {
    val notif = Notification(notificationId = "del", title = "Delete Me")
    uiStateFlow.value = NotificationsUiState(notifications = listOf(notif))

    composeTestRule.setContent {
      OnePassTheme { NotificationsScreen(mockNavController, mockViewModel) }
    }

    composeTestRule.onNodeWithContentDescription("Delete notification").performClick()
    verify { mockViewModel.deleteNotification("del") }
  }
}
