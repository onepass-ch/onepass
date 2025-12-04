package ch.onepass.onepass.service

import android.util.Log
import androidx.annotation.VisibleForTesting
import ch.onepass.onepass.model.notification.Notification
import ch.onepass.onepass.model.notification.NotificationRepositoryFirebase
import ch.onepass.onepass.model.notification.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.notifications.INotificationReceivedEvent
import com.onesignal.notifications.INotificationServiceExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * OneSignal notification service extension for OnePass. Handles incoming notifications and stores
 * them in Firestore.
 */
class NotificationService : INotificationServiceExtension {

  private val notificationRepository by lazy { NotificationRepositoryFirebase() }
  private val TAG = "OnePassNotification"

  override fun onNotificationReceived(event: INotificationReceivedEvent) {
    val notification = event.notification
    val additionalData = notification.additionalData

    Log.d(TAG, "Notification received: ${notification.title}")

    // Store notification in Firestore for persistence
    CoroutineScope(Dispatchers.IO).launch {
      val userId = FirebaseAuth.getInstance().currentUser?.uid
      if (userId == null) {
        Log.w(TAG, "User not authenticated, skipping Firestore storage")
        event.notification.display()
        return@launch
      }

      val firestoreNotification =
          Notification(
              userId = userId,
              type = parseNotificationType(additionalData),
              title = notification.title ?: "",
              body = notification.body ?: "",
              eventId = additionalData?.optString("eventId"),
              organizationId = additionalData?.optString("organizationId"),
              deepLink = additionalData?.optString("deepLink"),
              isPushed = true)

      notificationRepository
          .createNotification(firestoreNotification)
          .onSuccess { notificationId ->
            Log.d(TAG, "Notification stored in Firestore: $notificationId")
            // Store Firestore ID in the notification for later reference
            additionalData?.put("firestoreId", notificationId)
          }
          .onFailure { e -> Log.e(TAG, "Failed to store notification in Firestore", e) }
    }

    // Display the notification
    event.notification.display()
  }

  companion object {
    /** Parse notification type from additional data. Made internal for testing. */
    @VisibleForTesting
    internal fun parseNotificationType(data: JSONObject?): NotificationType {
      return when (data?.optString("type")) {
        "EVENT_REMINDER" -> NotificationType.EVENT_REMINDER
        "EVENT_INVITATION" -> NotificationType.EVENT_INVITATION
        "ORGANIZATION_INVITATION" -> NotificationType.ORGANIZATION_INVITATION
        "TICKET_PURCHASED" -> NotificationType.TICKET_PURCHASED
        "TICKET_TRANSFER" -> NotificationType.TICKET_TRANSFER
        "EVENT_CANCELLED" -> NotificationType.EVENT_CANCELLED
        "NEW_MESSAGE" -> NotificationType.NEW_MESSAGE
        "SYSTEM_ANNOUNCEMENT" -> NotificationType.SYSTEM_ANNOUNCEMENT
        else -> {
          Log.w("OnePassNotification", "Unknown notification type: ${data?.optString("type")}")
          NotificationType.SYSTEM_ANNOUNCEMENT
        }
      }
    }
  }
}
