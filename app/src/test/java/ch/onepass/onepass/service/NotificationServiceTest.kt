package ch.onepass.onepass.service

import ch.onepass.onepass.model.notification.NotificationRepository
import ch.onepass.onepass.model.notification.NotificationType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
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

  private lateinit var mockRepository: NotificationRepository

  @Before
  fun setup() {
    mockRepository = mockk()
  }

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

  @Test
  fun processNotification_authenticatedUser_storesInFirestore() = runTest {
    coEvery { mockRepository.createNotification(any()) } returns Result.success("notif123")

    val additionalData = JSONObject()
    additionalData.put("type", "EVENT_REMINDER")
    additionalData.put("eventId", "evt456")

    NotificationService.processNotification(
        userId = "user123",
        title = "Test Event",
        body = "Event starts soon",
        additionalData = additionalData,
        repository = mockRepository)

    delay(100)

    coVerify {
      mockRepository.createNotification(
          match {
            it.userId == "user123" &&
                it.title == "Test Event" &&
                it.body == "Event starts soon" &&
                it.type == NotificationType.EVENT_REMINDER &&
                it.eventId == "evt456"
          })
    }
  }

  @Test
  fun processNotification_nullUserId_doesNotStore() = runTest {
    NotificationService.processNotification(
        userId = null,
        title = "Test Event",
        body = "Event starts soon",
        additionalData = JSONObject(),
        repository = mockRepository)

    delay(100)

    coVerify(exactly = 0) { mockRepository.createNotification(any()) }
  }

  @Test
  fun processNotification_extractsEventId() = runTest {
    coEvery { mockRepository.createNotification(any()) } returns Result.success("notif123")

    val additionalData = JSONObject()
    additionalData.put("type", "EVENT_INVITATION")
    additionalData.put("eventId", "evt789")

    NotificationService.processNotification(
        userId = "user456",
        title = "You're Invited",
        body = "Join our event",
        additionalData = additionalData,
        repository = mockRepository)

    delay(100)

    coVerify { mockRepository.createNotification(match { it.eventId == "evt789" }) }
  }

  @Test
  fun processNotification_extractsOrganizationId() = runTest {
    coEvery { mockRepository.createNotification(any()) } returns Result.success("notif123")

    val additionalData = JSONObject()
    additionalData.put("type", "ORGANIZATION_INVITATION")
    additionalData.put("organizationId", "org999")

    NotificationService.processNotification(
        userId = "user789",
        title = "Staff Invitation",
        body = "Join as staff",
        additionalData = additionalData,
        repository = mockRepository)

    delay(100)

    coVerify { mockRepository.createNotification(match { it.organizationId == "org999" }) }
  }

  @Test
  fun processNotification_extractsDeepLink() = runTest {
    coEvery { mockRepository.createNotification(any()) } returns Result.success("notif123")

    val additionalData = JSONObject()
    additionalData.put("type", "EVENT_REMINDER")
    additionalData.put("deepLink", "onepass://event/evt123")

    NotificationService.processNotification(
        userId = "user321",
        title = "Reminder",
        body = "Event tomorrow",
        additionalData = additionalData,
        repository = mockRepository)

    delay(100)

    coVerify {
      mockRepository.createNotification(match { it.deepLink == "onepass://event/evt123" })
    }
  }

  @Test
  fun processNotification_setsIsPushedTrue() = runTest {
    coEvery { mockRepository.createNotification(any()) } returns Result.success("notif123")

    NotificationService.processNotification(
        userId = "user111",
        title = "Test",
        body = "Body",
        additionalData = JSONObject(),
        repository = mockRepository)

    delay(100)

    coVerify { mockRepository.createNotification(match { it.isPushed == true }) }
  }

  @Test
  fun processNotification_repositoryFailure_logsError() = runTest {
    coEvery { mockRepository.createNotification(any()) } returns
        Result.failure(Exception("Network error"))

    NotificationService.processNotification(
        userId = "user222",
        title = "Test",
        body = "Body",
        additionalData = JSONObject(),
        repository = mockRepository)

    delay(100)

    coVerify { mockRepository.createNotification(any()) }
  }

  @Test
  fun processNotification_nullAdditionalData_stillProcesses() = runTest {
    coEvery { mockRepository.createNotification(any()) } returns Result.success("notif123")

    NotificationService.processNotification(
        userId = "user333",
        title = "Test",
        body = "Body",
        additionalData = null,
        repository = mockRepository)

    delay(100)

    coVerify {
      mockRepository.createNotification(
          match {
            it.eventId == null &&
                it.organizationId == null &&
                it.deepLink == null &&
                it.type == NotificationType.SYSTEM_ANNOUNCEMENT
          })
    }
  }
}
