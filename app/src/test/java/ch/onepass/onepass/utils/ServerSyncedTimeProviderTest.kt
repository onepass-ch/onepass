package ch.onepass.onepass.utils

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ServerSyncedTimeProviderTest {

  private lateinit var functions: FirebaseFunctions
  private lateinit var callable: HttpsCallableReference
  private lateinit var timeProvider: ServerSyncedTimeProvider
  private val FIXED_DEVICE_TIME_START = 1704067200000L

  @Before
  fun setUp() {
    functions = mockk()
    callable = mockk()
    every { functions.getHttpsCallable("getServerTime") } returns callable
    timeProvider = ServerSyncedTimeProvider(functions)
  }

  @Test
  fun `syncWithServer updates offset on success`() = runTest {
    // Arrange: Expected server time is 5 seconds ahead of our fixed device time
    val serverOffset = 5000L
    val expectedServerTime = FIXED_DEVICE_TIME_START + serverOffset
    val resultData = mapOf("timestamp" to expectedServerTime)

    val result = mockk<HttpsCallableResult>()
    every { result.data } returns resultData
    every { callable.call() } returns Tasks.forResult(result)

    timeProvider.syncWithServer()

    val providerTime = timeProvider.now().toDate().time
    val diff = providerTime - expectedServerTime
    assertTrue(
        "Provider time should match expected server time $expectedServerTime. Got $providerTime",
        Math.abs(diff) < 50)
  }

  @Test
  fun `syncWithServer keeps offset 0 on failure`() = runTest {
    // Arrange: Simulate a network error
    every { callable.call() } returns Tasks.forException(Exception("Network error"))

    // Act
    timeProvider.syncWithServer()

    // Assert: Time should still match device time (offset 0)
    val deviceTime = System.currentTimeMillis()
    val providerTime = timeProvider.now().toDate().time
    val diff = providerTime - deviceTime

    assertTrue("Time should remain close to device time on failure", Math.abs(diff) < 100)
  }

  @Test
  fun `syncWithServer handles invalid response data`() = runTest {
    // Arrange: Return data that doesn't contain the "timestamp" key
    val result = mockk<HttpsCallableResult>()
    every { result.data } returns mapOf("wrongKey" to 123)
    every { callable.call() } returns Tasks.forResult(result)

    // Act
    timeProvider.syncWithServer()

    // Assert: Should treat as failure and keep offset 0
    val deviceTime = System.currentTimeMillis()
    val providerTime = timeProvider.now().toDate().time
    val diff = providerTime - deviceTime

    assertTrue("Time should remain close to device time on invalid data", Math.abs(diff) < 100)
  }

  @Test
  fun `currentDate returns correct Date object`() {
    // Act
    val tolerance = 50L
    val date = timeProvider.currentDate()
    val nowTimestamp = timeProvider.now().toDate()

    // Assert
    assertEquals(
        "currentDate() should return the Date from now() within tolerance",
        nowTimestamp.time.toDouble(),
        date.time.toDouble(),
        tolerance.toDouble())
  }
}
