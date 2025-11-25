package ch.onepass.onepass.ui.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ch.onepass.onepass.model.notification.Notification
import ch.onepass.onepass.model.notification.NotificationType
import ch.onepass.onepass.ui.components.common.EmptyState
import ch.onepass.onepass.ui.components.common.LoadingState

/**
 * Composable screen responsible for displaying the list of user notifications.
 *
 * @param navController Controller for navigating between screens.
 * @param viewModel ViewModel that holds the state and logic for notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationsViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Notifications") },
            actions = {
              if (uiState.unreadCount > 0) {
                TextButton(onClick = { viewModel.markAllAsRead() }) { Text("Mark all as read") }
              }
            })
      }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
          when {
            uiState.isLoading -> {
              LoadingState(
                  modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                  testTag = "notification_loading")
            }
            uiState.notifications.isEmpty() -> {
              EmptyState(
                  title = "No notifications yet",
                  message = "We'll let you know when something updates!",
                  modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                  testTag = "notification_empty")
            }
            else -> {
              LazyColumn(
                  contentPadding = PaddingValues(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = uiState.notifications, key = { it.notificationId }) { notification
                      ->
                      NotificationItem(
                          notification = notification,
                          onClick = {
                            viewModel.markAsRead(notification.notificationId)
                            handleNotificationClick(notification, navController)
                          },
                          onDelete = { viewModel.deleteNotification(notification.notificationId) })
                    }
                  }
            }
          }
        }
      }
}

/**
 * Handles the logic for navigating based on the notification type and content.
 *
 * @param notification The selected notification object.
 * @param navController The navigation controller.
 */
private fun handleNotificationClick(notification: Notification, navController: NavController) {
  // Priority 1: Use deep link if present
  if (!notification.deepLink.isNullOrEmpty()) {
    // Parse the custom scheme deep link manually for internal navigation
    // format: onepass://event/{id}
    val uri = notification.deepLink.toUri()
    if (uri.scheme == "onepass") {
      val type = uri.host
      val id = uri.lastPathSegment
      if (type == "event" && id != null) {
        navController.navigate("event_details/$id")
        return
      }
    }
  }

  // Priority 2: Use specific IDs based on type
  when (notification.type) {
    NotificationType.EVENT_REMINDER,
    NotificationType.EVENT_INVITATION,
    NotificationType.EVENT_CANCELLED -> {
      notification.eventId?.let { id -> navController.navigate("event_details/$id") }
    }
    NotificationType.ORGANIZATION_INVITATION -> {
      notification.organizationId?.let { id -> navController.navigate("organization_details/$id") }
    }
    NotificationType.TICKET_PURCHASED,
    NotificationType.TICKET_TRANSFER -> {
      // Navigate to tickets tab or specific ticket
      navController.navigate("tickets")
    }
    else -> {
      // Just mark as read (handled in onClick)
    }
  }
}
