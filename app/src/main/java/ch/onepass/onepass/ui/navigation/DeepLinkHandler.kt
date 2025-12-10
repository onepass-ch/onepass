package ch.onepass.onepass.ui.navigation

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.navigation.NavController
import ch.onepass.onepass.model.notification.NotificationRepositoryFirebase
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DeepLinkHandler {
  private const val TAG = "DeepLinkHandler"
  private val notificationRepository = NotificationRepositoryFirebase()

  private var currentListener: INotificationClickListener? = null

  fun setupNotificationClickListener(navController: NavController) {
    currentListener?.let { OneSignal.Notifications.removeClickListener(it) }

    val listener =
        object : INotificationClickListener {
          override fun onClick(event: INotificationClickEvent) {
            val notification = event.notification
            val additionalData = notification.additionalData

            Log.d(TAG, "Notification clicked: ${notification.title}")

            val firestoreId = additionalData?.optString("firestoreId")
            if (!firestoreId.isNullOrEmpty()) {
              CoroutineScope(Dispatchers.IO).launch {
                notificationRepository
                    .markAsRead(firestoreId)
                    .onSuccess { Log.d(TAG, "Marked notification as read: $firestoreId") }
                    .onFailure { e -> Log.e(TAG, "Failed to mark notification as read", e) }
              }
            }

            // Handle navigation (MAIN Thread)
            val deepLink = additionalData?.optString("deepLink")
            if (!deepLink.isNullOrEmpty()) {
              // 2. Fix: Ensure navigation happens on Main Thread
              CoroutineScope(Dispatchers.Main).launch { handleDeepLink(deepLink, navController) }
            }
          }
        }

    // Register and save reference
    currentListener = listener
    OneSignal.Notifications.addClickListener(listener)
  }

  fun handleDeepLink(deepLink: String, navController: NavController) {
    Log.d(TAG, "Handling deep link: $deepLink")

    try {
      val uri = Uri.parse(deepLink)
      val path = uri.path ?: ""

      when {
        // Event detail
        path.contains("/event/") -> {
          val eventId = uri.lastPathSegment
          if (!eventId.isNullOrEmpty()) {
            Log.d(TAG, "Navigating to event: $eventId")
            navController.navigate(NavigationDestinations.Screen.EventDetail.route(eventId))
          }
        }

        // Organization dashboard
        path.contains("/organization/") -> {
          val orgId = uri.lastPathSegment
          if (!orgId.isNullOrEmpty()) {
            Log.d(TAG, "Navigating to organization: $orgId")
            navController.navigate(NavigationDestinations.Screen.OrganizationDashboard.route(orgId))
          }
        }

        // Notifications
        deepLink.endsWith("notifications") -> {
          Log.d(TAG, "Navigating to notifications screen")
          navController.navigate(NavigationDestinations.Screen.Notification.route)
        }

        // Invitations
        deepLink.endsWith("invitations") -> {
          Log.d(TAG, "Navigating to invitations screen")
          navController.navigate(NavigationDestinations.Screen.MyInvitations.route)
        }

        // Tickets
        deepLink.endsWith("tickets") -> {
          Log.d(TAG, "Navigating to tickets screen")
          navController.navigate(NavigationDestinations.Screen.Tickets.route)
        }
        else -> {
          Log.w(TAG, "Unknown deep link format: $deepLink")
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error handling deep link: $deepLink", e)
    }
  }

  fun handleIntent(intent: Intent?, navController: NavController) {
    intent?.data?.let { uri ->
      val deepLink = uri.toString()
      Log.d(TAG, "Intent received with deep link: $deepLink")
      if (deepLink.startsWith("onepass://")) {
        handleDeepLink(deepLink, navController)
      }
    }
  }
}
