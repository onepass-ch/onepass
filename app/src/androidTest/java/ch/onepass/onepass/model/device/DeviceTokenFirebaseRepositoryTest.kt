package ch.onepass.onepass.model.device

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.utils.FirebaseEmulator.firestore
import ch.onepass.onepass.utils.FirestoreTestBase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTokenRepositoryFirebaseTest : FirestoreTestBase() {

  private lateinit var deviceRepository: DeviceTokenRepositoryFirebase
  private val testUserId = "test_user_123"

  @Before
  override fun setUp() {
    super.setUp()
    deviceRepository = DeviceTokenRepositoryFirebase()
  }

  @After
  override fun tearDown() = runTest {
    // Clean up test data
    try {
      firestore
          .collection("users")
          .document(testUserId)
          .collection("device_tokens")
          .get()
          .await()
          .documents
          .forEach { it.reference.delete().await() }
    } catch (_: Exception) {}
  }

  @Test
  fun saveDeviceToken_storesTokenInFirestore() = runTest {
    val token =
        DeviceToken(
            deviceId = "device_123",
            oneSignalPlayerId = "player_456",
            deviceModel = "Pixel 7",
            appVersion = "1.0.0")

    val result = deviceRepository.saveDeviceToken(testUserId, token)

    assertTrue(result.isSuccess)

    val doc =
        firestore
            .collection("users")
            .document(testUserId)
            .collection("device_tokens")
            .document("device_123")
            .get()
            .await()

    assertTrue(doc.exists())
    assertEquals("player_456", doc.getString("oneSignalPlayerId"))
    assertEquals("Pixel 7", doc.getString("deviceModel"))
  }

  @Test
  fun getDeviceTokens_returnsOnlyActiveTokens() = runTest {
    // Create active token
    val activeToken =
        DeviceToken(deviceId = "active_device", oneSignalPlayerId = "player_1", isActive = true)

    // Create inactive token
    val inactiveToken =
        DeviceToken(deviceId = "inactive_device", oneSignalPlayerId = "player_2", isActive = false)

    deviceRepository.saveDeviceToken(testUserId, activeToken)
    deviceRepository.saveDeviceToken(testUserId, inactiveToken)

    // Manually set one as inactive (since save always sets isActive=true)
    firestore
        .collection("users")
        .document(testUserId)
        .collection("device_tokens")
        .document("inactive_device")
        .update("isActive", false)
        .await()

    val result = deviceRepository.getDeviceTokens(testUserId)

    assertTrue(result.isSuccess)
    val tokens = result.getOrNull()!!
    assertEquals(1, tokens.size)
    assertEquals("active_device", tokens[0].deviceId)
  }

  @Test
  fun deactivateDeviceToken_setsIsActiveToFalse() = runTest {
    val token = DeviceToken(deviceId = "device_to_deactivate", oneSignalPlayerId = "player_999")

    deviceRepository.saveDeviceToken(testUserId, token)
    deviceRepository.deactivateDeviceToken(testUserId, "device_to_deactivate")

    val doc =
        firestore
            .collection("users")
            .document(testUserId)
            .collection("device_tokens")
            .document("device_to_deactivate")
            .get()
            .await()

    assertFalse(doc.getBoolean("isActive") ?: true)
  }

  @Test
  fun getPlayerIds_returnsOnlyActiveNonEmptyIds() = runTest {
    val token1 = DeviceToken(deviceId = "device1", oneSignalPlayerId = "player_active_1")
    val token2 = DeviceToken(deviceId = "device2", oneSignalPlayerId = "player_active_2")

    deviceRepository.saveDeviceToken(testUserId, token1)
    deviceRepository.saveDeviceToken(testUserId, token2)

    val result = deviceRepository.getPlayerIds(testUserId)

    assertTrue(result.isSuccess)
    val playerIds = result.getOrNull()!!
    assertEquals(2, playerIds.size)
    assertTrue(playerIds.contains("player_active_1"))
    assertTrue(playerIds.contains("player_active_2"))
  }

  @Test
  fun saveDeviceToken_setsServerTimestamps() = runTest {
    val token = DeviceToken(deviceId = "timestamp_test", oneSignalPlayerId = "player_timestamp")

    deviceRepository.saveDeviceToken(testUserId, token)

    val doc =
        firestore
            .collection("users")
            .document(testUserId)
            .collection("device_tokens")
            .document("timestamp_test")
            .get()
            .await()

    assertNotNull(doc.getTimestamp("createdAt"))
    assertNotNull(doc.getTimestamp("lastUpdated"))
  }

  @Test
  fun getDeviceTokens_withNoTokens_returnsEmptyList() = runTest {
    val result = deviceRepository.getDeviceTokens("user_with_no_tokens")

    assertTrue(result.isSuccess)
    assertTrue(result.getOrNull()!!.isEmpty())
  }

  @Test
  fun getPlayerIds_withNoTokens_returnsEmptyList() = runTest {
    val result = deviceRepository.getPlayerIds("user_with_no_tokens")

    assertTrue(result.isSuccess)
    assertTrue(result.getOrNull()!!.isEmpty())
  }

  @Test
  fun saveDeviceToken_updatesExistingToken() = runTest {
    val originalToken =
        DeviceToken(
            deviceId = "device_456", oneSignalPlayerId = "old_player_id", deviceModel = "Pixel 6")

    val updatedToken =
        DeviceToken(
            deviceId = "device_456", // Same deviceId
            oneSignalPlayerId = "new_player_id",
            deviceModel = "Pixel 7")

    deviceRepository.saveDeviceToken(testUserId, originalToken)
    deviceRepository.saveDeviceToken(testUserId, updatedToken)

    val doc =
        firestore
            .collection("users")
            .document(testUserId)
            .collection("device_tokens")
            .document("device_456")
            .get()
            .await()

    // Should be updated, not duplicated
    assertEquals("new_player_id", doc.getString("oneSignalPlayerId"))
    assertEquals("Pixel 7", doc.getString("deviceModel"))
  }

  @Test
  fun getPlayerIds_filtersEmptyPlayerIds() = runTest {
    val tokenWithId = DeviceToken(deviceId = "device1", oneSignalPlayerId = "valid_player_id")

    val tokenWithoutId =
        DeviceToken(
            deviceId = "device2", oneSignalPlayerId = "" // Empty
            )

    deviceRepository.saveDeviceToken(testUserId, tokenWithId)
    deviceRepository.saveDeviceToken(testUserId, tokenWithoutId)

    val result = deviceRepository.getPlayerIds(testUserId)

    assertTrue(result.isSuccess)
    val playerIds = result.getOrNull()!!
    assertEquals(1, playerIds.size)
    assertEquals("valid_player_id", playerIds[0])
  }

  @Test
  fun deactivateDeviceToken_withNonExistentToken_returnsFailure() = runTest {
    val result = deviceRepository.deactivateDeviceToken(testUserId, "non_existent_device")

    // Should fail since document doesn't exist
    assertTrue(result.isFailure)
  }

  @Test
  fun getDeviceTokens_isolatesUserData() = runTest {
    val user1Id = "user_1"
    val user2Id = "user_2"

    val token1 = DeviceToken(deviceId = "device_user1", oneSignalPlayerId = "player_user1")

    val token2 = DeviceToken(deviceId = "device_user2", oneSignalPlayerId = "player_user2")

    deviceRepository.saveDeviceToken(user1Id, token1)
    deviceRepository.saveDeviceToken(user2Id, token2)

    val user1Tokens = deviceRepository.getDeviceTokens(user1Id).getOrNull()!!
    val user2Tokens = deviceRepository.getDeviceTokens(user2Id).getOrNull()!!

    assertEquals(1, user1Tokens.size)
    assertEquals(1, user2Tokens.size)
    assertEquals("device_user1", user1Tokens[0].deviceId)
    assertEquals("device_user2", user2Tokens[0].deviceId)
  }

  @Test
  fun saveDeviceToken_storesAllFieldsCorrectly() = runTest {
    val token =
        DeviceToken(
            deviceId = "full_device",
            oneSignalPlayerId = "full_player",
            platform = "android",
            deviceModel = "Samsung Galaxy",
            appVersion = "2.1.0",
            isActive = true)

    deviceRepository.saveDeviceToken(testUserId, token)

    val doc =
        firestore
            .collection("users")
            .document(testUserId)
            .collection("device_tokens")
            .document("full_device")
            .get()
            .await()

    assertEquals("full_device", doc.getString("deviceId"))
    assertEquals("full_player", doc.getString("oneSignalPlayerId"))
    assertEquals("android", doc.getString("platform"))
    assertEquals("Samsung Galaxy", doc.getString("deviceModel"))
    assertEquals("2.1.0", doc.getString("appVersion"))
    assertEquals(true, doc.getBoolean("isActive"))
  }
}
