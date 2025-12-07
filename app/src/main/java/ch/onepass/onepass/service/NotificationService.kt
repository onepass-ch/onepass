package ch.onepass.onepass.service

import android.util.Log
import androidx.annotation.VisibleForTesting
import ch.onepass.onepass.model.notification.Notification
import ch.onepass.onepass.model.notification.NotificationRepository
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

    Log.d(TAG, "Notification received: ${notification.title}")

    // Process and store notification
    processNotification(
        userId = FirebaseAuth.getInstance().currentUser?.uid,
        title = notification.title ?: "",
        body = notification.body ?: "",
        additionalData = notification.additionalData,
        repository = notificationRepository)

    // Display the notification
    event.notification.display()
  }

  companion object {
    /** Processes incoming notification and stores in Firestore. Extracted for testing purposes. */
    @VisibleForTesting
    internal fun processNotification(
        userId: String?,
        title: String,
        body: String,
        additionalData: JSONObject?,
        repository: NotificationRepository
    ) {
      CoroutineScope(Dispatchers.IO).launch {
        if (userId == null) {
          Log.w("OnePassNotification", "User not authenticated, skipping Firestore storage")
          return@launch
        }

        val firestoreNotification =
            Notification(
                userId = userId,
                type = parseNotificationType(additionalData),
                title = title,
                body = body,
                eventId = additionalData?.optString("eventId"),
                organizationId = additionalData?.optString("organizationId"),
                deepLink = additionalData?.optString("deepLink"),
                isPushed = true)

        repository
            .createNotification(firestoreNotification)
            .onSuccess { notificationId ->
              Log.d("OnePassNotification", "Notification stored in Firestore: $notificationId")
              additionalData?.put("firestoreId", notificationId)
            }
            .onFailure { e ->
              Log.e("OnePassNotification", "Failed to store notification in Firestore", e)
            }
      }
    }

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
