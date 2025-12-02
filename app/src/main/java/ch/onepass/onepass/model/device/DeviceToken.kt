package ch.onepass.onepass.model.device

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a device registered for push notifications. Stored in:
 * users/{userId}/device_tokens/{deviceId}
 *
 * @param deviceId Unique device identifier (Android ID)
 * @param oneSignalPlayerId OneSignal player ID for this device
 * @param platform Platform type (android/ios)
 * @param deviceModel Device model name (e.g., "Pixel 7")
 * @param appVersion App version when token was registered
 * @param isActive Whether this device is currently active
 * @param createdAt Timestamp when device was first registered
 * @param lastUpdated Timestamp when token was last updated
 */
data class DeviceToken(
    val deviceId: String = "",
    val oneSignalPlayerId: String = "",
    val platform: String = "android",
    val deviceModel: String = "",
    val appVersion: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val lastUpdated: Timestamp? = null
)
