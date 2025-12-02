package ch.onepass.onepass.model.device

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of DeviceTokenRepository. Stores device tokens in:
 * users/{userId}/device_tokens/{deviceId}
 */
class DeviceTokenRepositoryFirebase : DeviceTokenRepository {
  private val firestore = Firebase.firestore

  override suspend fun saveDeviceToken(userId: String, deviceToken: DeviceToken): Result<Unit> =
      runCatching {
        val tokenData =
            mapOf(
                "deviceId" to deviceToken.deviceId,
                "oneSignalPlayerId" to deviceToken.oneSignalPlayerId,
                "platform" to deviceToken.platform,
                "deviceModel" to deviceToken.deviceModel,
                "appVersion" to deviceToken.appVersion,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "lastUpdated" to FieldValue.serverTimestamp())

        firestore
            .collection("users")
            .document(userId)
            .collection("device_tokens")
            .document(deviceToken.deviceId)
            .set(tokenData)
            .await()
      }

  override suspend fun getDeviceTokens(userId: String): Result<List<DeviceToken>> = runCatching {
    val snapshot =
        firestore
            .collection("users")
            .document(userId)
            .collection("device_tokens")
            .whereEqualTo("isActive", true)
            .get()
            .await()

    snapshot.documents.mapNotNull { it.toObject(DeviceToken::class.java) }
  }

  override suspend fun deactivateDeviceToken(userId: String, deviceId: String): Result<Unit> =
      runCatching {
        firestore
            .collection("users")
            .document(userId)
            .collection("device_tokens")
            .document(deviceId)
            .update(mapOf("isActive" to false))
            .await()
      }

  override suspend fun getPlayerIds(userId: String): Result<List<String>> = runCatching {
    val snapshot =
        firestore
            .collection("users")
            .document(userId)
            .collection("device_tokens")
            .whereEqualTo("isActive", true)
            .get()
            .await()

    snapshot.documents.mapNotNull { it.getString("oneSignalPlayerId") }.filter { it.isNotEmpty() }
  }
}
