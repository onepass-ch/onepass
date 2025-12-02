package ch.onepass.onepass.model.device

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTokenRepositoryFirebaseTest {

  private lateinit var repository: DeviceTokenRepositoryFirebase
  private lateinit var firestore: FirebaseFirestore
  private val testUserId = "test_user_123"

  @Before
  fun setup() {
    FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).forEach { it.delete() }

    val context = ApplicationProvider.getApplicationContext<Context>()
    val options =
        FirebaseOptions.Builder()
            .setProjectId("demo-test-project")
            .setApplicationId("1:123456789:android:abcdef")
            .setApiKey("fake-api-key")
            .build()

    FirebaseApp.initializeApp(context, options)

    firestore = Firebase.firestore
    firestore.useEmulator("10.0.2.2", 8080)

    repository = DeviceTokenRepositoryFirebase()
  }

  @After
  fun tearDown() = runTest {
    firestore
        .collection("users")
        .document(testUserId)
        .collection("device_tokens")
        .get()
        .await()
        .documents
        .forEach { it.reference.delete().await() }
  }

  @Test
  fun saveDeviceToken_storesTokenInFirestore() = runTest {
    val token =
        DeviceToken(
            deviceId = "device_123",
            oneSignalPlayerId = "player_456",
            deviceModel = "Pixel 7",
            appVersion = "1.0.0")

    val result = repository.saveDeviceToken(testUserId, token)

    assertTrue(result.isSuccess)

    // Verify token exists in Firestore
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

    repository.saveDeviceToken(testUserId, activeToken)
    repository.saveDeviceToken(testUserId, inactiveToken)

    // Manually set one as inactive (since save always sets isActive=true)
    firestore
        .collection("users")
        .document(testUserId)
        .collection("device_tokens")
        .document("inactive_device")
        .update("isActive", false)
        .await()

    val result = repository.getDeviceTokens(testUserId)

    assertTrue(result.isSuccess)
    val tokens = result.getOrNull()!!
    assertEquals(1, tokens.size)
    assertEquals("active_device", tokens[0].deviceId)
  }

  @Test
  fun deactivateDeviceToken_setsIsActiveToFalse() = runTest {
    val token = DeviceToken(deviceId = "device_to_deactivate", oneSignalPlayerId = "player_999")

    repository.saveDeviceToken(testUserId, token)
    repository.deactivateDeviceToken(testUserId, "device_to_deactivate")

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

    repository.saveDeviceToken(testUserId, token1)
    repository.saveDeviceToken(testUserId, token2)

    val result = repository.getPlayerIds(testUserId)

    assertTrue(result.isSuccess)
    val playerIds = result.getOrNull()!!
    assertEquals(2, playerIds.size)
    assertTrue(playerIds.contains("player_active_1"))
    assertTrue(playerIds.contains("player_active_2"))
  }

  @Test
  fun saveDeviceToken_setsServerTimestamps() = runTest {
    val token = DeviceToken(deviceId = "timestamp_test", oneSignalPlayerId = "player_timestamp")

    repository.saveDeviceToken(testUserId, token)

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
    val result = repository.getDeviceTokens("user_with_no_tokens")

    assertTrue(result.isSuccess)
    assertTrue(result.getOrNull()!!.isEmpty())
  }

  @Test
  fun getPlayerIds_withNoTokens_returnsEmptyList() = runTest {
    val result = repository.getPlayerIds("user_with_no_tokens")

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

    repository.saveDeviceToken(testUserId, originalToken)
    repository.saveDeviceToken(testUserId, updatedToken)

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

    repository.saveDeviceToken(testUserId, tokenWithId)
    repository.saveDeviceToken(testUserId, tokenWithoutId)

    val result = repository.getPlayerIds(testUserId)

    assertTrue(result.isSuccess)
    val playerIds = result.getOrNull()!!
    assertEquals(1, playerIds.size)
    assertEquals("valid_player_id", playerIds[0])
  }

  @Test
  fun deactivateDeviceToken_withNonExistentToken_returnsFailure() = runTest {
    val result = repository.deactivateDeviceToken(testUserId, "non_existent_device")

    // Should fail since document doesn't exist
    assertTrue(result.isFailure)
  }

  @Test
  fun getDeviceTokens_isolatesUserData() = runTest {
    val user1Id = "user_1"
    val user2Id = "user_2"

    val token1 = DeviceToken(deviceId = "device_user1", oneSignalPlayerId = "player_user1")

    val token2 = DeviceToken(deviceId = "device_user2", oneSignalPlayerId = "player_user2")

    repository.saveDeviceToken(user1Id, token1)
    repository.saveDeviceToken(user2Id, token2)

    val user1Tokens = repository.getDeviceTokens(user1Id).getOrNull()!!
    val user2Tokens = repository.getDeviceTokens(user2Id).getOrNull()!!

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

    repository.saveDeviceToken(testUserId, token)

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
