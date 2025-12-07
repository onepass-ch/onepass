package ch.onepass.onepass.model.device

/** Repository for managing device tokens for push notifications. */
interface DeviceTokenRepository {
  /**
   * Save or update a device token for a user.
   *
   * @param userId The user's unique identifier
   * @param deviceToken The device token to save
   * @return Result indicating success or failure
   */
  suspend fun saveDeviceToken(userId: String, deviceToken: DeviceToken): Result<Unit>

  /**
   * Get all active device tokens for a user.
   *
   * @param userId The user's unique identifier
   * @return Result containing list of active device tokens
   */
  suspend fun getDeviceTokens(userId: String): Result<List<DeviceToken>>

  /**
   * Deactivate a specific device token.
   *
   * @param userId The user's unique identifier
   * @param oneSignalPlayerId The device identifier to deactivate
   * @return Result indicating success or failure
   */
  suspend fun deactivateDeviceToken(userId: String, oneSignalPlayerId: String): Result<Unit>

  /**
   * Get all OneSignal player IDs for a user (for sending notifications).
   *
   * @param userId The user's unique identifier
   * @return Result containing list of player IDs
   */
  suspend fun getPlayerIds(userId: String): Result<List<String>>
}
