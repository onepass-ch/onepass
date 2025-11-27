package ch.onepass.onepass.ui.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.model.notification.Notification
import ch.onepass.onepass.utils.DateTimeUtils.formatNotificationDate

/**
 * A single row item in the notifications list. Displays the title, body, and timestamp of the
 * notification, with visual indicators for read status.
 *
 * @param notification The notification data object to display.
 * @param onClick Callback triggered when the item is clicked.
 * @param onDelete Callback triggered when the delete button is clicked.
 */
@Composable
fun NotificationItem(notification: Notification, onClick: () -> Unit, onDelete: () -> Unit) {
  val backgroundColor =
      if (notification.isRead) {
        MaterialTheme.colorScheme.surface
      } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
      }

  Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = backgroundColor),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
              // Content
              Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                      Text(
                          text = notification.title,
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight =
                              if (!notification.isRead) FontWeight.Bold else FontWeight.Normal)

                      notification.createdAt?.let { timestamp ->
                        Text(
                            text = formatNotificationDate(timestamp.toDate()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              // Delete Action
              IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete notification",
                    tint = MaterialTheme.colorScheme.error)
              }
            }
      }
}
