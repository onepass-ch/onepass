package ch.onepass.onepass.model.payment

import androidx.activity.ComponentActivity
import ch.onepass.onepass.ui.payment.PaymentSheetResultHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StripePaymentHelper using MockK.
 *
 * These tests verify the Stripe payment helper functionality with mocked PaymentSheet.
 */
class StripePaymentHelperTest {

  private lateinit var mockPaymentSheet: PaymentSheet
  private lateinit var mockActivity: ComponentActivity

  @Before
  fun setUp() {
    mockPaymentSheet = mockk(relaxed = true)
    mockActivity = mockk(relaxed = true)
    // Clear any previous handler
    PaymentSheetResultHandler.clearHandler()
  }

  @Test
  fun constructor_withPaymentSheet_initializesCorrectly() {
    val helper = StripePaymentHelper(mockPaymentSheet)

    assertTrue(helper.isInitialized)
  }

  @Test
  fun constructor_withoutPaymentSheet_isNotInitialized() {
    val helper = StripePaymentHelper()

    assertFalse(helper.isInitialized)
  }

  @Test
  fun presentPaymentSheet_withoutInitialization_callsOnError() {
    val helper = StripePaymentHelper()
    var errorCalled = false
    var errorMessage = ""

    helper.presentPaymentSheet(
        clientSecret = "pi_123_secret_456",
        onSuccess = {},
        onError = { error ->
          errorCalled = true
          errorMessage = error
        })

    assertTrue(errorCalled)
    assertEquals("PaymentSheet not initialized. Please ensure Stripe is configured.", errorMessage)
  }

  @Test
  fun presentPaymentSheet_withInitialization_callsPaymentSheet() {
    val helper = StripePaymentHelper(mockPaymentSheet)
    val clientSecret = "pi_123_secret_456"

    helper.presentPaymentSheet(
        clientSecret = clientSecret, onSuccess = {}, onCancelled = {}, onError = {})

    verify {
      mockPaymentSheet.presentWithPaymentIntent(
          paymentIntentClientSecret = clientSecret, configuration = any())
    }
  }

  @Test
  fun presentPaymentSheet_setsConfiguration_withCorrectMerchantName() {
    val helper = StripePaymentHelper(mockPaymentSheet)
    val configSlot = slot<PaymentSheet.Configuration>()

    helper.presentPaymentSheet(
        clientSecret = "pi_123_secret_456", onSuccess = {}, onCancelled = {}, onError = {})

    verify {
      mockPaymentSheet.presentWithPaymentIntent(
          paymentIntentClientSecret = any(), configuration = capture(configSlot))
    }

    assertEquals("OnePass", configSlot.captured.merchantDisplayName)
    assertTrue(configSlot.captured.allowsDelayedPaymentMethods)
  }

  @Test
  fun presentPaymentSheet_withCustomerConfig_setsCustomerInConfiguration() {
    val helper = StripePaymentHelper(mockPaymentSheet)
    val customerConfig =
        PaymentSheet.CustomerConfiguration(id = "cus_123", ephemeralKeySecret = "ek_test_123")
    val configSlot = slot<PaymentSheet.Configuration>()

    helper.presentPaymentSheet(
        clientSecret = "pi_123_secret_456",
        customerConfig = customerConfig,
        onSuccess = {},
        onCancelled = {},
        onError = {})

    verify {
      mockPaymentSheet.presentWithPaymentIntent(
          paymentIntentClientSecret = any(), configuration = capture(configSlot))
    }

    assertNotNull(configSlot.captured.customer)
    assertEquals("cus_123", configSlot.captured.customer?.id)
  }

  @Test
  fun handlePaymentSheetResult_completed_callsOnSuccess() {
    val helper = StripePaymentHelper(mockPaymentSheet)
    var successCalled = false

    helper.presentPaymentSheet(
        clientSecret = "pi_123_secret_456",
        onSuccess = { successCalled = true },
        onCancelled = {},
        onError = {})

    // Simulate payment completion
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)

    assertTrue(successCalled)
  }

  @Test
  fun handlePaymentSheetResult_canceled_callsOnCancelled() {
    val helper = StripePaymentHelper(mockPaymentSheet)
    var cancelledCalled = false

    helper.presentPaymentSheet(
        clientSecret = "pi_123_secret_456",
        onSuccess = {},
        onCancelled = { cancelledCalled = true },
        onError = {})

    // Simulate payment cancellation
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Canceled)

    assertTrue(cancelledCalled)
  }

  @Test
  fun handlePaymentSheetResult_failed_callsOnError() {
    val helper = StripePaymentHelper(mockPaymentSheet)
    var errorCalled = false
    var errorMessage = ""

    helper.presentPaymentSheet(
        clientSecret = "pi_123_secret_456",
        onSuccess = {},
        onCancelled = {},
        onError = { error ->
          errorCalled = true
          errorMessage = error
        })

    // Simulate payment failure
    val mockError = mockk<Throwable>()
    every { mockError.message } returns "Payment failed"
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Failed(mockError))

    assertTrue(errorCalled)
    assertEquals("Payment failed", errorMessage)
  }

  @Test
  fun handlePaymentSheetResult_failedWithNullMessage_callsOnErrorWithDefaultMessage() {
    val helper = StripePaymentHelper(mockPaymentSheet)
    var errorCalled = false
    var errorMessage = ""

    helper.presentPaymentSheet(
        clientSecret = "pi_123_secret_456",
        onSuccess = {},
        onCancelled = {},
        onError = { error ->
          errorCalled = true
          errorMessage = error
        })

    // Simulate payment failure with null message
    val mockError = mockk<Throwable>()
    every { mockError.message } returns null
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Failed(mockError))

    assertTrue(errorCalled)
    assertEquals("Payment failed", errorMessage)
  }

  @Test
  fun createCustomerConfiguration_returnsCorrectConfiguration() {
    val helper = StripePaymentHelper(mockPaymentSheet)

    val config =
        helper.createCustomerConfiguration(customerId = "cus_123456", ephemeralKey = "ek_test_789")

    assertEquals("cus_123456", config.id)
    assertEquals("ek_test_789", config.ephemeralKeySecret)
  }

  @Test
  fun presentPaymentSheet_multipleCallbacks_allRegisteredCorrectly() {
    val helper = StripePaymentHelper(mockPaymentSheet)
    var successCount = 0
    var cancelledCount = 0
    var errorCount = 0

    helper.presentPaymentSheet(
        clientSecret = "pi_123_secret_456",
        onSuccess = { successCount++ },
        onCancelled = { cancelledCount++ },
        onError = { errorCount++ })

    // Test success
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)
    assertEquals(1, successCount)
    assertEquals(0, cancelledCount)
    assertEquals(0, errorCount)

    // Present again and test cancelled
    helper.presentPaymentSheet(
        clientSecret = "pi_123_secret_456",
        onSuccess = { successCount++ },
        onCancelled = { cancelledCount++ },
        onError = { errorCount++ })
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Canceled)
    assertEquals(1, successCount)
    assertEquals(1, cancelledCount)
    assertEquals(0, errorCount)

    // Present again and test error
    helper.presentPaymentSheet(
        clientSecret = "pi_123_secret_456",
        onSuccess = { successCount++ },
        onCancelled = { cancelledCount++ },
        onError = { errorCount++ })
    val mockError = mockk<Throwable>()
    every { mockError.message } returns "Error"
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Failed(mockError))
    assertEquals(1, successCount)
    assertEquals(1, cancelledCount)
    assertEquals(1, errorCount)
  }

  @Test
  fun presentPaymentSheet_withDefaultCallbacks_doesNotCrash() {
    val helper = StripePaymentHelper(mockPaymentSheet)

    // Present with only required callback
    helper.presentPaymentSheet(clientSecret = "pi_123_secret_456", onSuccess = {})

    // Simulate different results - should not crash
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Canceled)

    val mockError = mockk<Throwable>()
    every { mockError.message } returns "Error"
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Failed(mockError))
  }

  @Test
  fun isInitialized_returnsCorrectValue() {
    val initializedHelper = StripePaymentHelper(mockPaymentSheet)
    val uninitializedHelper = StripePaymentHelper()

    assertTrue(initializedHelper.isInitialized)
    assertFalse(uninitializedHelper.isInitialized)
  }

  @Test
  fun presentPaymentSheet_withDifferentClientSecrets_callsCorrectly() {
    val helper = StripePaymentHelper(mockPaymentSheet)
    val clientSecret1 = "pi_111_secret_111"
    val clientSecret2 = "pi_222_secret_222"

    helper.presentPaymentSheet(clientSecret = clientSecret1, onSuccess = {})

    verify {
      mockPaymentSheet.presentWithPaymentIntent(
          paymentIntentClientSecret = clientSecret1, configuration = any())
    }

    helper.presentPaymentSheet(clientSecret = clientSecret2, onSuccess = {})

    verify {
      mockPaymentSheet.presentWithPaymentIntent(
          paymentIntentClientSecret = clientSecret2, configuration = any())
    }
  }

  @Test
  fun presentPaymentSheet_allowsDelayedPaymentMethods_isTrue() {
    val helper = StripePaymentHelper(mockPaymentSheet)
    val configSlot = slot<PaymentSheet.Configuration>()

    helper.presentPaymentSheet(clientSecret = "pi_123_secret_456", onSuccess = {})

    verify {
      mockPaymentSheet.presentWithPaymentIntent(
          paymentIntentClientSecret = any(), configuration = capture(configSlot))
    }

    assertTrue(configSlot.captured.allowsDelayedPaymentMethods)
  }

  @Test
  fun createCustomerConfiguration_withDifferentValues_returnsCorrectConfigurations() {
    val helper = StripePaymentHelper(mockPaymentSheet)

    val config1 = helper.createCustomerConfiguration("cus_1", "ek_1")
    val config2 = helper.createCustomerConfiguration("cus_2", "ek_2")

    assertEquals("cus_1", config1.id)
    assertEquals("ek_1", config1.ephemeralKeySecret)
    assertEquals("cus_2", config2.id)
    assertEquals("ek_2", config2.ephemeralKeySecret)
  }
}
