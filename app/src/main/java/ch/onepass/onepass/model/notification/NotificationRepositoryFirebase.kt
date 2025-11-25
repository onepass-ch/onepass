package ch.onepass.onepass.model.notification

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of the [NotificationRepository]. Handles data operations with the
 * Firestore "notifications" collection.
 */
class NotificationRepositoryFirebase : NotificationRepository {
  private val notificationsCollection = Firebase.firestore.collection("notifications")

  /**
   * Retrieves a real-time stream of the user's latest 100 notifications.
   *
   * @param userId The unique identifier of the user.
   * @return A [Flow] emitting a list of [Notification] ordered by creation date descending.
   */
  override fun getUserNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
    val listener =
        notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }
              val notifications =
                  snapshot?.documents?.mapNotNull { it.toObject(Notification::class.java) }
                      ?: emptyList()
              trySend(notifications)
            }
    awaitClose { listener.remove() }
  }

  /**
   * Retrieves a real-time stream of the count of unread notifications for a user.
   *
   * @param userId The unique identifier of the user.
   * @return A [Flow] emitting the integer count of unread notifications.
   */
  override fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
    val listener =
        notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }
              trySend(snapshot?.size() ?: 0)
            }
    awaitClose { listener.remove() }
  }

  /**
   * Marks a notification as read and sets the read timestamp.
   *
   * @param notificationId The unique identifier of the notification.
   * @return A [Result] indicating success or failure.
   */
  override suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
    notificationsCollection
        .document(notificationId)
        .update(mapOf("isRead" to true, "readAt" to FieldValue.serverTimestamp()))
        .await()
  }

  /**
   * Marks all unread notifications for a user as read using a batch update.
   *
   * @param userId The unique identifier of the user.
   * @return A [Result] indicating success or failure.
   */
  override suspend fun markAllAsRead(userId: String): Result<Unit> = runCatching {
    val snapshot =
        notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

    val batch = Firebase.firestore.batch()
    snapshot.documents.forEach { doc ->
      batch.update(doc.reference, mapOf("isRead" to true, "readAt" to FieldValue.serverTimestamp()))
    }
    batch.commit().await()
  }

  /**
   * Deletes a notification document from Firestore.
   *
   * @param notificationId The unique identifier of the notification.
   * @return A [Result] indicating success or failure.
   */
  override suspend fun deleteNotification(notificationId: String): Result<Unit> = runCatching {
    notificationsCollection.document(notificationId).delete().await()
  }

  /**
   * Creates a new notification document in Firestore. The document ID is generated automatically
   * and assigned to the notification object.
   *
   * @param notification The [Notification] to create.
   * @return A [Result] containing the generated notification ID.
   */
  override suspend fun createNotification(notification: Notification): Result<String> =
      runCatching {
        val docRef = notificationsCollection.document()
        val withId = notification.copy(notificationId = docRef.id)
        docRef.set(withId).await()
        docRef.id
      }

  /**
   * Updates the 'isPushed' status of a notification document.
   *
   * @param notificationId The unique identifier of the notification.
   * @param pushed The new pushed status.
   * @return A [Result] indicating success or failure.
   */
  override suspend fun updateNotificationPushStatus(
      notificationId: String,
      pushed: Boolean
  ): Result<Unit> = runCatching {
    notificationsCollection.document(notificationId).update("isPushed", pushed).await()
  }
}
