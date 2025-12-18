package ch.onepass.onepass.ui.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ch.onepass.onepass.R
import ch.onepass.onepass.model.notification.Notification
import ch.onepass.onepass.model.notification.NotificationType
import ch.onepass.onepass.ui.components.common.EmptyState
import ch.onepass.onepass.ui.components.common.LoadingState

/**
 * Composable screen responsible for displaying the list of user notifications.
 *
 * @param navController Controller for navigating between screens.
 * @param viewModel ViewModel that holds the state and logic for notifications.
 * @param onNavigateBack Lambda function to execute when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationsViewModel = viewModel(),
    onNavigateBack: () -> Unit = { navController.popBackStack() }
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.notifications_screen_title)) },
            navigationIcon = {
              IconButton(
                  onClick = onNavigateBack,
                  modifier = Modifier.testTag("notification_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription =
                            stringResource(R.string.notifications_back_description))
                  }
            },
            actions = {
              if (uiState.unreadCount > 0) {
                TextButton(onClick = { viewModel.markAllAsRead() }) {
                  Text(stringResource(R.string.notifications_mark_all_read))
                }
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
                  title = stringResource(R.string.notifications_empty_title),
                  message = stringResource(R.string.notifications_empty_message),
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
