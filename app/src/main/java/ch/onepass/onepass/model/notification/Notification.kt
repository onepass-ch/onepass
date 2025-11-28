package ch.onepass.onepass.model.notification

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class representing a notification in the system.
 *
 * @param notificationId Unique identifier for the notification.
 * @param userId The ID of the user receiving the notification.
 * @param type The type of notification (e.g., EVENT_REMINDER, NEW_MESSAGE).
 * @param title The title of the notification.
 * @param body The main content/message of the notification.
 * @param eventId Optional ID of the related event.
 * @param organizationId Optional ID of the related organization.
 * @param deepLink Optional deep link URL for navigation.
 * @param isRead specific flag indicating if the notification has been read by the user.
 * @param isPushed specific flag indicating if the push notification was sent successfully.
 * @param createdAt Timestamp when the notification was created (server-side).
 * @param readAt Timestamp when the notification was read by the user.
 */
data class Notification(
    val notificationId: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.EVENT_REMINDER,
    val title: String = "",
    val body: String = "",
    val eventId: String? = null,
    val organizationId: String? = null,
    val deepLink: String? = null,
    @get:PropertyName("isRead") val isRead: Boolean = false,
    @get:PropertyName("isPushed") val isPushed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val readAt: Timestamp? = null
)

/** Enum class defining the various types of notifications supported by the system. */
enum class NotificationType {
  EVENT_REMINDER,
  EVENT_INVITATION,
  ORGANIZATION_INVITATION,
  TICKET_PURCHASED,
  TICKET_TRANSFER,
  EVENT_CANCELLED,
  NEW_MESSAGE,
  SYSTEM_ANNOUNCEMENT
}
