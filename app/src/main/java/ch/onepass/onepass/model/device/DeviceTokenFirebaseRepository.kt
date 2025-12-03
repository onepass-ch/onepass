package ch.onepass.onepass.model.device

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
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
        val docRef =
            firestore
                .collection("users")
                .document(userId)
                .collection("device_tokens")
                .document(deviceToken.deviceId)
        val exists = docRef.get().await().exists()

        val tokenData = buildMap {
          put("deviceId", deviceToken.deviceId)
          put("oneSignalPlayerId", deviceToken.oneSignalPlayerId)
          put("platform", deviceToken.platform)
          put("deviceModel", deviceToken.deviceModel)
          put("appVersion", deviceToken.appVersion)
          put("isActive", true)
          put("lastUpdated", FieldValue.serverTimestamp())
          if (!exists) {
            put("createdAt", FieldValue.serverTimestamp())
          }
        }
        docRef.set(tokenData, SetOptions.merge()).await()
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

    snapshot.documents.mapNotNull { it.getString("oneSignalPlayerId") }.filter { it.isNotBlank() }
  }
}
