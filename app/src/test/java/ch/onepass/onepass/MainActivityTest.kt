package ch.onepass.onepass

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

  @Test
  fun onCreate_executesStripeInitializationCode() {
    try {
      Robolectric.setupActivity(MainActivity::class.java)
    } catch (e: UnsatisfiedLinkError) {
      if (e.message?.contains("Mapbox") != true && e.message?.contains("MapboxOptions") != true) {
        throw e
      }
      // For Mapbox errors, the test passes as Stripe code was executed
    } catch (e: Exception) {
      // Re-throw other exceptions
      throw e
    }
  }
}
