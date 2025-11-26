package ch.onepass.onepass.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.notification.Notification
import ch.onepass.onepass.model.notification.NotificationRepository
import ch.onepass.onepass.model.notification.NotificationRepositoryFirebase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Data class representing the UI state of the notifications screen.
 *
 * @param notifications List of notifications to display.
 * @param unreadCount The count of unread notifications.
 * @param isLoading Flag indicating if data is currently being loaded.
 * @param error Optional error message if an operation fails.
 */
data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the Notifications screen. Manages the UI state and interactions with the
 * [NotificationRepository].
 *
 * @property notificationRepository Repository to fetch and update notification data.
 * @property auth FirebaseAuth instance to identify the current user.
 */
class NotificationsViewModel(
    private val notificationRepository: NotificationRepository = NotificationRepositoryFirebase(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {
  private val _uiState = MutableStateFlow(NotificationsUiState())
  val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

  init {
    loadNotifications()
  }

  /**
   * Initializes the data streams. We do NOT call this again manually; the repository flows are
   * reactive and will emit updates automatically when data changes (e.g. after a delete).
   */
  fun loadNotifications() {
    val userId = auth.currentUser?.uid ?: return

    // 1. Collect Notifications Flow
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      notificationRepository
          .getUserNotifications(userId)
          .catch { e ->
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false, error = e.message ?: "Failed to load notifications")
          }
          .collect { notifications ->
            _uiState.value =
                _uiState.value.copy(
                    notifications = notifications,
                    isLoading = false,
                    error = null // Clear previous errors on success
                    )
          }
    }

    // 2. Collect Unread Count Flow
    viewModelScope.launch {
      notificationRepository
          .getUnreadCount(userId)
          .catch { e ->
            // Log error or handle silently, usually main flow error covers it
          }
          .collect { count -> _uiState.value = _uiState.value.copy(unreadCount = count) }
    }
  }

  /**
   * Marks a specific notification as read.
   *
   * @param notificationId The unique identifier of the notification.
   */
  fun markAsRead(notificationId: String) {
    viewModelScope.launch {
      try {
        notificationRepository.markAsRead(notificationId)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(error = "Failed to mark as read")
      }
    }
  }

  /** Marks all notifications for the current user as read. */
  fun markAllAsRead() {
    val userId = auth.currentUser?.uid ?: return
    viewModelScope.launch {
      try {
        notificationRepository.markAllAsRead(userId)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(error = "Failed to mark all as read")
      }
    }
  }

  /**
   * Deletes a notification and refreshes the list (though the flow updates automatically).
   *
   * @param notificationId The unique identifier of the notification to delete.
   */
  fun deleteNotification(notificationId: String) {
    viewModelScope.launch {
      try {
        notificationRepository.deleteNotification(notificationId)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(error = "Failed to delete notification")
      }
    }
  }
}
