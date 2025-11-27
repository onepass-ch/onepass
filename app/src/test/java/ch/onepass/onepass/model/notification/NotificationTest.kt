package ch.onepass.onepass.model.notification

import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTest {

  @Test
  fun notificationData_defaultValues() {
    val notification = Notification()
    assertEquals("", notification.notificationId)
    assertEquals("", notification.userId)
    assertEquals(NotificationType.EVENT_REMINDER, notification.type)
    assertEquals("", notification.title)
    assertEquals("", notification.body)
    assertNull(notification.eventId)
    assertNull(notification.organizationId)
    assertNull(notification.deepLink)
    assertFalse(notification.isRead)
    assertFalse(notification.isPushed)
    assertNull(notification.createdAt)
    assertNull(notification.readAt)
  }

  @Test
  fun notificationData_customValues() {
    val timestamp = Timestamp(Date())
    val notification =
        Notification(
            notificationId = "1",
            userId = "user1",
            type = NotificationType.NEW_MESSAGE,
            title = "Title",
            body = "Body",
            eventId = "event1",
            organizationId = "org1",
            deepLink = "link",
            isRead = true,
            isPushed = true,
            createdAt = timestamp,
            readAt = timestamp)

    assertEquals("1", notification.notificationId)
    assertEquals("user1", notification.userId)
    assertEquals(NotificationType.NEW_MESSAGE, notification.type)
    assertEquals("Title", notification.title)
    assertEquals("Body", notification.body)
    assertEquals("event1", notification.eventId)
    assertEquals("org1", notification.organizationId)
    assertEquals("link", notification.deepLink)
    assertTrue(notification.isRead)
    assertTrue(notification.isPushed)
    assertEquals(timestamp, notification.createdAt)
    assertEquals(timestamp, notification.readAt)
  }

  @Test
  fun notificationType_values() {
    // Verify all enum entries exist to ensure coverage if enum logic expands
    val types = NotificationType.entries.toTypedArray()
    assertTrue(types.contains(NotificationType.EVENT_REMINDER))
    assertTrue(types.contains(NotificationType.EVENT_INVITATION))
    assertTrue(types.contains(NotificationType.ORGANIZATION_INVITATION))
    assertTrue(types.contains(NotificationType.TICKET_PURCHASED))
    assertTrue(types.contains(NotificationType.TICKET_TRANSFER))
    assertTrue(types.contains(NotificationType.EVENT_CANCELLED))
    assertTrue(types.contains(NotificationType.NEW_MESSAGE))
    assertTrue(types.contains(NotificationType.SYSTEM_ANNOUNCEMENT))
  }
}
