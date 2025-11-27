package ch.onepass.onepass.model.notification

import kotlinx.coroutines.flow.Flow

/** Repository interface for managing notification data operations. */
interface NotificationRepository {

  /**
   * Retrieves a stream of notifications for a specific user.
   *
   * @param userId The unique identifier of the user.
   * @return A [Flow] emitting a list of [Notification].
   */
  fun getUserNotifications(userId: String): Flow<List<Notification>>

  /**
   * Retrieves a stream of the unread notification count for a specific user.
   *
   * @param userId The unique identifier of the user.
   * @return A [Flow] emitting the count of unread notifications.
   */
  fun getUnreadCount(userId: String): Flow<Int>

  /**
   * Marks a specific notification as read.
   *
   * @param notificationId The unique identifier of the notification to mark as read.
   * @return A [Result] indicating success or failure.
   */
  suspend fun markAsRead(notificationId: String): Result<Unit>

  /**
   * Marks all unread notifications for a specific user as read.
   *
   * @param userId The unique identifier of the user.
   * @return A [Result] containing the number of updated notifications on success.
   */
  suspend fun markAllAsRead(userId: String): Result<Int>

  /**
   * Deletes a specific notification.
   *
   * @param notificationId The unique identifier of the notification to delete.
   * @return A [Result] indicating success or failure.
   */
  suspend fun deleteNotification(notificationId: String): Result<Unit>

  /**
   * Creates a new notification in the data source.
   *
   * @param notification The [Notification] object to create.
   * @return A [Result] containing the ID of the created notification on success.
   */
  suspend fun createNotification(notification: Notification): Result<String>

  /**
   * Updates the push status of a notification.
   *
   * @param notificationId The unique identifier of the notification.
   * @param pushed The new push status boolean value.
   * @return A [Result] indicating success or failure.
   */
  suspend fun updateNotificationPushStatus(notificationId: String, pushed: Boolean): Result<Unit>
}
