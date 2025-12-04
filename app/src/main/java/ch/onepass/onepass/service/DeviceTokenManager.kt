package ch.onepass.onepass.service

import android.content.Context
import android.os.Build
import android.util.Log
import ch.onepass.onepass.model.device.DeviceToken
import ch.onepass.onepass.model.device.DeviceTokenRepository

/**
 * Manages OneSignal device token registration and storage in Firestore. Handles retry logic when
 * OneSignal is still initializing.
 */
class DeviceTokenManager(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val context: Context,
    private val playerIdProvider: () -> String,
    private val currentUserIdProvider: () -> String?
) {
  private var tokenStoreRetries = 0
  private val maxRetries = 3

  companion object {
    private const val TAG = "DeviceTokenManager"
  }

  /**
   * Attempts to store device token. Retries if player ID is not yet available. Returns true if
   * stored successfully, false if max retries reached.
   */
  suspend fun storeDeviceToken(): Boolean {
    val playerId = playerIdProvider()
    val userId = currentUserIdProvider()

    // Check if player ID is available
    if (playerId.isEmpty()) {
      return handleEmptyPlayerId()
    }

    // Check if user is authenticated
    if (userId == null) {
      Log.w(TAG, "User not authenticated, cannot store device token")
      return false
    }

    // Success - reset retry counter and store token
    tokenStoreRetries = 0
    return storeToken(userId, playerId)
  }

  private suspend fun handleEmptyPlayerId(): Boolean {
    return if (tokenStoreRetries < maxRetries) {
      tokenStoreRetries++
      Log.w(TAG, "Player ID empty, retry $tokenStoreRetries/$maxRetries")
      storeDeviceToken() // Recursive retry
    } else {
      Log.e(TAG, "Max retries reached, could not store token")
      false
    }
  }

  private suspend fun storeToken(userId: String, playerId: String): Boolean {
    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    val appVersion = getAppVersion()

    val deviceToken =
        DeviceToken(
            oneSignalPlayerId = playerId,
            platform = "android",
            deviceModel = deviceModel,
            appVersion = appVersion,
            isActive = true)

    return deviceTokenRepository
        .saveDeviceToken(userId, deviceToken)
        .onSuccess { Log.d(TAG, "Device token saved: $playerId") }
        .onFailure { e -> Log.e(TAG, "Failed to save device token", e) }
        .isSuccess
  }

  fun resetRetries() {
    tokenStoreRetries = 0
  }

  private fun getAppVersion(): String {
    return try {
      context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (_: Exception) {
      "unknown"
    }
  }
}
