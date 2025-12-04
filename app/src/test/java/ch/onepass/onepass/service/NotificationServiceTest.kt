package ch.onepass.onepass.service

import ch.onepass.onepass.model.notification.NotificationType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for NotificationService.
 *
 * Note: Only tests the parseNotificationType logic, as testing onNotificationReceived would require
 * mocking OneSignal SDK, Firebase Auth, and Firestore, which is overly complex for the value it
 * provides. Manual testing is preferred for full notification flow.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationServiceTest {

  @Test
  fun parseNotificationType_eventReminder_returnsCorrectType() {
    val data = JSONObject().put("type", "EVENT_REMINDER")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.EVENT_REMINDER, result)
  }

  @Test
  fun parseNotificationType_eventInvitation_returnsCorrectType() {
    val data = JSONObject().put("type", "EVENT_INVITATION")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.EVENT_INVITATION, result)
  }

  @Test
  fun parseNotificationType_organizationInvitation_returnsCorrectType() {
    val data = JSONObject().put("type", "ORGANIZATION_INVITATION")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.ORGANIZATION_INVITATION, result)
  }

  @Test
  fun parseNotificationType_ticketPurchased_returnsCorrectType() {
    val data = JSONObject().put("type", "TICKET_PURCHASED")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.TICKET_PURCHASED, result)
  }

  @Test
  fun parseNotificationType_ticketTransfer_returnsCorrectType() {
    val data = JSONObject().put("type", "TICKET_TRANSFER")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.TICKET_TRANSFER, result)
  }

  @Test
  fun parseNotificationType_eventCancelled_returnsCorrectType() {
    val data = JSONObject().put("type", "EVENT_CANCELLED")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.EVENT_CANCELLED, result)
  }

  @Test
  fun parseNotificationType_newMessage_returnsCorrectType() {
    val data = JSONObject().put("type", "NEW_MESSAGE")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.NEW_MESSAGE, result)
  }

  @Test
  fun parseNotificationType_systemAnnouncement_returnsCorrectType() {
    val data = JSONObject().put("type", "SYSTEM_ANNOUNCEMENT")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.SYSTEM_ANNOUNCEMENT, result)
  }

  @Test
  fun parseNotificationType_unknownType_returnsSystemAnnouncement() {
    val data = JSONObject().put("type", "UNKNOWN_NOTIFICATION_TYPE")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.SYSTEM_ANNOUNCEMENT, result)
  }

  @Test
  fun parseNotificationType_emptyString_returnsSystemAnnouncement() {
    val data = JSONObject().put("type", "")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.SYSTEM_ANNOUNCEMENT, result)
  }

  @Test
  fun parseNotificationType_nullData_returnsSystemAnnouncement() {
    val result = NotificationService.parseNotificationType(null)

    assertEquals(NotificationType.SYSTEM_ANNOUNCEMENT, result)
  }

  @Test
  fun parseNotificationType_missingTypeField_returnsSystemAnnouncement() {
    val data = JSONObject().put("otherField", "someValue")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.SYSTEM_ANNOUNCEMENT, result)
  }

  @Test
  fun parseNotificationType_emptyJsonObject_returnsSystemAnnouncement() {
    val data = JSONObject()

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.SYSTEM_ANNOUNCEMENT, result)
  }

  @Test
  fun parseNotificationType_caseSensitive_unknownIfLowercase() {
    val data = JSONObject().put("type", "event_reminder") // lowercase

    val result = NotificationService.parseNotificationType(data)

    // Should return SYSTEM_ANNOUNCEMENT because matching is case-sensitive
    assertEquals(NotificationType.SYSTEM_ANNOUNCEMENT, result)
  }

  @Test
  fun parseNotificationType_extraFields_doesNotAffectParsing() {
    val data =
        JSONObject()
            .put("type", "EVENT_INVITATION")
            .put("eventId", "evt123")
            .put("deepLink", "onepass://event/evt123")

    val result = NotificationService.parseNotificationType(data)

    assertEquals(NotificationType.EVENT_INVITATION, result)
  }
}
