package ch.onepass.onepass.model.device

import kotlin.test.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class DeviceTokenTest {

  @Test
  fun `default values are set correctly`() {
    val token = DeviceToken()

    assertEquals("", token.oneSignalPlayerId)
    assertEquals("android", token.platform)
    assertTrue(token.isActive)
    assertNull(token.createdAt)
  }

  @Test
  fun `can create token with custom values`() {
    val token =
        DeviceToken(
            oneSignalPlayerId = "player456",
            platform = "android",
            deviceModel = "Pixel 7",
            appVersion = "1.0.0")

    assertEquals("player456", token.oneSignalPlayerId)
    assertEquals("Pixel 7", token.deviceModel)
  }
}
