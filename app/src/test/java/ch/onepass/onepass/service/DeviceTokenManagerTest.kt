package ch.onepass.onepass.service

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.provider.Settings
import ch.onepass.onepass.model.device.DeviceTokenRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DeviceTokenManagerTest {

  private lateinit var mockRepository: DeviceTokenRepository
  private lateinit var mockContext: Context
  private lateinit var mockPackageManager: PackageManager
  private var playerIdCallCount = 0

  @Before
  fun setup() {
    mockRepository = mockk()
    mockContext = mockk(relaxed = true)
    mockPackageManager = mockk()

    // Mock Settings.Secure
    mockkStatic(Settings.Secure::class)
    every { Settings.Secure.getString(any(), Settings.Secure.ANDROID_ID) } returns "test-device-123"

    // Mock package manager
    every { mockContext.packageManager } returns mockPackageManager
    val packageInfo = PackageInfo().apply { versionName = "1.0.0" }
    every { mockPackageManager.getPackageInfo(any<String>(), any<Int>()) } returns packageInfo
    every { mockContext.packageName } returns "ch.onepass.onepass"

    playerIdCallCount = 0
  }

  @Test
  fun storeDeviceToken_success_returnsTrue() = runTest {
    coEvery { mockRepository.saveDeviceToken(any(), any()) } returns Result.success(Unit)

    val manager =
        DeviceTokenManager(
            deviceTokenRepository = mockRepository,
            context = mockContext,
            playerIdProvider = { "player123" },
            currentUserIdProvider = { "user456" })

    val result = manager.storeDeviceToken()

    assertTrue(result)
    coVerify { mockRepository.saveDeviceToken("user456", any()) }
  }

  @Test
  fun storeDeviceToken_emptyPlayerId_retriesUpToThreeTimes() = runTest {
    coEvery { mockRepository.saveDeviceToken(any(), any()) } returns Result.success(Unit)

    val manager =
        DeviceTokenManager(
            deviceTokenRepository = mockRepository,
            context = mockContext,
            playerIdProvider = {
              playerIdCallCount++
              if (playerIdCallCount < 3) "" else "player123"
            },
            currentUserIdProvider = { "user456" })

    val result = manager.storeDeviceToken()

    assertTrue(result)
    assertEquals(3, playerIdCallCount)
    coVerify { mockRepository.saveDeviceToken("user456", any()) }
  }

  @Test
  fun storeDeviceToken_maxRetriesReached_returnsFalse() = runTest {
    val manager =
        DeviceTokenManager(
            deviceTokenRepository = mockRepository,
            context = mockContext,
            playerIdProvider = { "" }, // Always empty
            currentUserIdProvider = { "user456" })

    val result = manager.storeDeviceToken()

    assertFalse(result)
    coVerify(exactly = 0) { mockRepository.saveDeviceToken(any(), any()) }
  }

  @Test
  fun storeDeviceToken_noAuthenticatedUser_returnsFalse() = runTest {
    val manager =
        DeviceTokenManager(
            deviceTokenRepository = mockRepository,
            context = mockContext,
            playerIdProvider = { "player123" },
            currentUserIdProvider = { null } // Not authenticated
            )

    val result = manager.storeDeviceToken()

    assertFalse(result)
    coVerify(exactly = 0) { mockRepository.saveDeviceToken(any(), any()) }
  }

  @Test
  fun storeDeviceToken_repositoryFailure_returnsFalse() = runTest {
    coEvery { mockRepository.saveDeviceToken(any(), any()) } returns
        Result.failure(Exception("Network error"))

    val manager =
        DeviceTokenManager(
            deviceTokenRepository = mockRepository,
            context = mockContext,
            playerIdProvider = { "player123" },
            currentUserIdProvider = { "user456" })

    val result = manager.storeDeviceToken()

    assertFalse(result)
  }

  @Test
  fun resetRetries_allowsNewAttempts() = runTest {
    coEvery { mockRepository.saveDeviceToken(any(), any()) } returns Result.success(Unit)

    val manager =
        DeviceTokenManager(
            deviceTokenRepository = mockRepository,
            context = mockContext,
            playerIdProvider = { "" },
            currentUserIdProvider = { "user456" })

    // Exhaust retries
    manager.storeDeviceToken()

    // Reset and try again with valid player ID
    manager.resetRetries()

    val managerWithPlayerId =
        DeviceTokenManager(
            deviceTokenRepository = mockRepository,
            context = mockContext,
            playerIdProvider = { "player123" },
            currentUserIdProvider = { "user456" })

    val result = managerWithPlayerId.storeDeviceToken()
    assertTrue(result)
  }
}
