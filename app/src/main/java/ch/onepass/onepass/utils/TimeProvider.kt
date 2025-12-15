package ch.onepass.onepass.utils

import android.os.SystemClock
import com.google.firebase.Timestamp
import com.google.firebase.functions.FirebaseFunctions
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Universal interface for providing the current time. Using this interface instead of
 * System.currentTimeMillis() allows for:
 * 1. Synchronizing with server time to prevent tampering.
 * 2. Mocking time in unit tests.
 */
interface TimeProvider {
  /** Returns the current trusted timestamp. */
  fun now(): Timestamp

  /** Returns the current trusted Date. */
  fun currentDate(): Date

  /** Synchronizes the local clock offset with the server. Should be called on app startup. */
  suspend fun syncWithServer()
}

/**
 * Implementation that calculates the offset between device time and server time. If sync fails, it
 * gracefully falls back to device time.
 */
@Singleton
class ServerSyncedTimeProvider @Inject constructor(private val functions: FirebaseFunctions) :
    TimeProvider {

  // The difference in milliseconds: ServerTime - DeviceTime
  private var clockOffset: Long = 0
  private var isSynced: Boolean = false

  override fun now(): Timestamp {
    val adjustedTimeMillis = System.currentTimeMillis() + clockOffset
    return Timestamp(Date(adjustedTimeMillis))
  }

  override fun currentDate(): Date {
    return now().toDate()
  }

  override suspend fun syncWithServer() {
    try {
      val startTime = SystemClock.elapsedRealtime()

      // Call the Cloud Function
      val result = functions.getHttpsCallable("getServerTime").call().await()

      val endTime = SystemClock.elapsedRealtime()
      val latency = (endTime - startTime) / 2 // Approximate one-way latency

      val data = result.data as? Map<String, Any>
      val serverTimeMillis =
          (data?.get("timestamp") as? Number)?.toLong()
              ?: throw IllegalStateException("Invalid server response")

      // Calculate offset: (Server Time + Latency) - Device Time at request start
      // We use the time right now to set the offset for future calls
      val currentDeviceTime = System.currentTimeMillis()

      // The server time corresponds roughly to 'endTime - latency'
      // But simpler: Offset = ServerTime - CurrentDeviceTime
      // We adjust server time by latency to be more precise
      val adjustedServerTime = serverTimeMillis + latency

      clockOffset = adjustedServerTime - currentDeviceTime
      isSynced = true

      android.util.Log.i("TimeProvider", "Time synced. Offset: $clockOffset ms")
    } catch (e: Exception) {
      android.util.Log.e("TimeProvider", "Failed to sync time: ${e.message}. Using device time.")
      // Fallback: clockOffset remains 0 (Device Time)
      isSynced = false
    }
  }
}

/** A TimeProvider implementation for testing that always returns a fixed, predictable time. */
class MockTimeProvider(private val fixedTime: Timestamp) : TimeProvider {

  /** Returns the fixed time as a Firebase Timestamp. */
  override fun now(): Timestamp = fixedTime

  /** Returns the fixed time as a Java Date object. */
  override fun currentDate(): Date = fixedTime.toDate()

  override suspend fun syncWithServer() {
    // No-op (Do nothing). The time is already fixed by 'fixedTime'.
  }
}
