package ch.onepass.onepass.model.notification

import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationRepositoryFirebaseTest : FirestoreTestBase() {

  private lateinit var notificationRepository: NotificationRepositoryFirebase
  private lateinit var userId: String

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      notificationRepository = NotificationRepositoryFirebase()

      // Sign in anonymously for testing
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"

      clearTestCollection()
    }
  }

  private suspend fun clearTestCollection() {
    val notifs = FirebaseEmulator.firestore.collection("notifications").get().await()
    if (notifs.isEmpty) return
    val batch = FirebaseEmulator.firestore.batch()
    notifs.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()
  }

  private fun createNotificationData(
      title: String = "Test Notification",
      body: String = "This is a test",
      isRead: Boolean = false
  ): Notification {
    return Notification(
        userId = userId,
        title = title,
        body = body,
        isRead = isRead,
        type = NotificationType.EVENT_REMINDER,
        createdAt = Timestamp(Date()))
  }

  @Test
  fun createNotification_addsDocumentToFirestore() = runTest {
    val notification = createNotificationData()
    val result = notificationRepository.createNotification(notification)

    assertTrue("Creation should succeed", result.isSuccess)
    val id = result.getOrNull()
    assertNotNull("Should return a notification ID", id)

    val doc = FirebaseEmulator.firestore.collection("notifications").document(id!!).get().await()
    assertTrue("Document should exist in Firestore", doc.exists())
    assertEquals("Title should match", notification.title, doc.getString("title"))
  }

  @Test
  fun getUserNotifications_retrievesUserSpecificData() = runTest {
    // Create notification for current user
    val myNotification = createNotificationData(title = "My Notification")
    notificationRepository.createNotification(myNotification)

    // Create notification for another user
    val otherNotification = myNotification.copy(userId = "other-user", title = "Other")
    notificationRepository.createNotification(otherNotification)

    val flow = notificationRepository.getUserNotifications(userId)
    val list = flow.first()

    assertEquals("Should retrieve 1 notification", 1, list.size)
    assertEquals("Should be my notification", "My Notification", list[0].title)
  }

  @Test
  fun getUnreadCount_countsOnlyUnread() = runTest {
    // Create 2 unread and 1 read notification
    notificationRepository.createNotification(createNotificationData(isRead = false))
    notificationRepository.createNotification(createNotificationData(isRead = false))
    notificationRepository.createNotification(createNotificationData(isRead = true))

    val flow = notificationRepository.getUnreadCount(userId)
    val count = flow.first { it == 2 }

    assertEquals("Unread count should be 2", 2, count)
  }

  @Test
  fun markAsRead_updatesFlagAndReadAt() = runTest {
    val id =
        notificationRepository
            .createNotification(createNotificationData(isRead = false))
            .getOrThrow()

    val result = notificationRepository.markAsRead(id)
    assertTrue("Mark as read should succeed", result.isSuccess)

    val doc = FirebaseEmulator.firestore.collection("notifications").document(id).get().await()
    assertTrue("isRead should be true", doc.getBoolean("isRead") == true)
    assertNotNull("readAt timestamp should be set", doc.getTimestamp("readAt"))
  }

  @Test
  fun markAllAsRead_updatesOnlyUserNotifications() = runTest {
    // Create 2 unread for me, 1 unread for other
    notificationRepository.createNotification(createNotificationData(isRead = false))
    notificationRepository.createNotification(createNotificationData(isRead = false))

    val otherNotification = createNotificationData(isRead = false).copy(userId = "other")
    val otherId = notificationRepository.createNotification(otherNotification).getOrThrow()

    val result = notificationRepository.markAllAsRead(userId)
    assertTrue(result.isSuccess)

    // Verify returned count matches the number of notifications updated (2)
    assertEquals("Should return count of updated notifications", 2, result.getOrNull())

    val countFlow = notificationRepository.getUnreadCount(userId)
    assertEquals("My unread count should be 0", 0, countFlow.first())

    val otherDoc =
        FirebaseEmulator.firestore.collection("notifications").document(otherId).get().await()
    assertEquals(
        "Other user notification should still be unread", false, otherDoc.getBoolean("isRead"))
  }

  @Test
  fun deleteNotification_removesDocument() = runTest {
    val id = notificationRepository.createNotification(createNotificationData()).getOrThrow()

    val result = notificationRepository.deleteNotification(id)
    assertTrue("Delete should succeed", result.isSuccess)

    val doc = FirebaseEmulator.firestore.collection("notifications").document(id).get().await()
    assertFalse("Document should not exist", doc.exists())
  }

  @Test
  fun updateNotificationPushStatus_updatesField() = runTest {
    val id = notificationRepository.createNotification(createNotificationData()).getOrThrow()

    val result = notificationRepository.updateNotificationPushStatus(id, true)
    assertTrue(result.isSuccess)

    val doc = FirebaseEmulator.firestore.collection("notifications").document(id).get().await()
    assertTrue("isPushed should be true", doc.getBoolean("isPushed") == true)
  }
}
