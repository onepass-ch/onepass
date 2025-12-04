package ch.onepass.onepass.ui.payment

import androidx.activity.ComponentActivity
import com.stripe.android.paymentsheet.PaymentSheetResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PaymentSheetProvider and PaymentSheetResultHandler.
 *
 * These tests verify the functionality of the payment sheet provider utilities.
 */
class PaymentSheetProviderTest {

  private lateinit var mockActivity: ComponentActivity

  @Before
  fun setUp() {
    mockActivity = mockk(relaxed = true)
    PaymentSheetResultHandler.clearHandler()
  }

  @Test
  fun createPaymentSheet_returnsPaymentSheetInstance() {
    val paymentSheet = createPaymentSheet(mockActivity)

    assertNotNull(paymentSheet)
  }

  @Test
  fun paymentSheetResultHandler_setHandler_setsHandlerCorrectly() {
    var handlerCalled = false

    PaymentSheetResultHandler.setHandler { result -> handlerCalled = true }

    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)

    assert(handlerCalled)
  }

  @Test
  fun paymentSheetResultHandler_clearHandler_clearsHandler() {
    var handlerCalled = false

    PaymentSheetResultHandler.setHandler { result -> handlerCalled = true }

    PaymentSheetResultHandler.clearHandler()
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)

    // Handler should not be called after clearing
    assert(!handlerCalled)
  }

  @Test
  fun paymentSheetResultHandler_handleResult_completed_callsHandler() {
    var resultReceived: PaymentSheetResult? = null

    PaymentSheetResultHandler.setHandler { result -> resultReceived = result }

    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)

    assertNotNull(resultReceived)
    assert(resultReceived is PaymentSheetResult.Completed)
  }

  @Test
  fun paymentSheetResultHandler_handleResult_canceled_callsHandler() {
    var resultReceived: PaymentSheetResult? = null

    PaymentSheetResultHandler.setHandler { result -> resultReceived = result }

    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Canceled)

    assertNotNull(resultReceived)
    assert(resultReceived is PaymentSheetResult.Canceled)
  }

  @Test
  fun paymentSheetResultHandler_handleResult_failed_callsHandler() {
    var resultReceived: PaymentSheetResult? = null
    val mockError = mockk<Throwable>()
    every { mockError.message } returns "Test error"

    PaymentSheetResultHandler.setHandler { result -> resultReceived = result }

    val failedResult = PaymentSheetResult.Failed(mockError)
    PaymentSheetResultHandler.handleResult(failedResult)

    assertNotNull(resultReceived)
    assert(resultReceived is PaymentSheetResult.Failed)
    assertEquals("Test error", (resultReceived as PaymentSheetResult.Failed).error.message)
  }

  @Test
  fun paymentSheetResultHandler_multipleHandlerUpdates_usesLatestHandler() {
    var firstHandlerCalled = false
    var secondHandlerCalled = false

    PaymentSheetResultHandler.setHandler { result -> firstHandlerCalled = true }

    PaymentSheetResultHandler.setHandler { result -> secondHandlerCalled = true }

    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)

    // Only the latest handler should be called
    assert(!firstHandlerCalled)
    assert(secondHandlerCalled)
  }

  @Test
  fun paymentSheetResultHandler_handleResultWithoutHandler_doesNotCrash() {
    // This should not crash even if no handler is set
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Canceled)

    val mockError = mockk<Throwable>()
    every { mockError.message } returns "Error"
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Failed(mockError))
  }

  @Test
  fun paymentSheetResultHandler_multipleResults_allHandled() {
    val results = mutableListOf<PaymentSheetResult>()

    PaymentSheetResultHandler.setHandler { result -> results.add(result) }

    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Canceled)

    val mockError = mockk<Throwable>()
    every { mockError.message } returns "Error"
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Failed(mockError))

    assertEquals(3, results.size)
    assert(results[0] is PaymentSheetResult.Completed)
    assert(results[1] is PaymentSheetResult.Canceled)
    assert(results[2] is PaymentSheetResult.Failed)
  }

  @Test
  fun paymentSheetResultHandler_setHandler_replacesExistingHandler() {
    var firstCount = 0
    var secondCount = 0

    PaymentSheetResultHandler.setHandler { firstCount++ }
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)

    PaymentSheetResultHandler.setHandler { secondCount++ }
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)

    assertEquals(1, firstCount)
    assertEquals(1, secondCount)
  }

  @Test
  fun paymentSheetResultHandler_clearHandler_afterMultipleSets_clearsCorrectly() {
    var handlerCalled = false

    PaymentSheetResultHandler.setHandler { handlerCalled = true }
    PaymentSheetResultHandler.setHandler { handlerCalled = true }
    PaymentSheetResultHandler.setHandler { handlerCalled = true }

    PaymentSheetResultHandler.clearHandler()
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)

    assert(!handlerCalled)
  }

  @Test
  fun paymentSheetResultHandler_handlerLifecycle_worksCorrectly() {
    // Test the lifecycle of setting and clearing handlers
    var firstHandlerCalled = false
    var secondHandlerCalled = false

    PaymentSheetResultHandler.setHandler { firstHandlerCalled = true }
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)
    assert(firstHandlerCalled)

    PaymentSheetResultHandler.clearHandler()
    PaymentSheetResultHandler.setHandler { secondHandlerCalled = true }
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)
    assert(secondHandlerCalled)
  }

  @Test
  fun paymentSheetResultHandler_failedWithDifferentErrors_handlesAllCorrectly() {
    val errors = mutableListOf<String?>()

    PaymentSheetResultHandler.setHandler { result ->
      if (result is PaymentSheetResult.Failed) {
        errors.add(result.error.message)
      }
    }

    val error1 = mockk<Throwable>()
    every { error1.message } returns "Network error"
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Failed(error1))

    val error2 = mockk<Throwable>()
    every { error2.message } returns "Card declined"
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Failed(error2))

    val error3 = mockk<Throwable>()
    every { error3.message } returns null
    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Failed(error3))

    assertEquals(3, errors.size)
    assertEquals("Network error", errors[0])
    assertEquals("Card declined", errors[1])
    assertNull(errors[2])
  }

  @Test
  fun paymentSheetResultHandler_concurrentHandlerCalls_handlesCorrectly() {
    var callCount = 0

    PaymentSheetResultHandler.setHandler { result -> callCount++ }

    // Simulate multiple rapid calls
    repeat(5) { PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed) }

    assertEquals(5, callCount)
  }

  @Test
  fun paymentSheetResultHandler_handlerWithException_doesNotBreakSubsequentCalls() {
    var firstCallCompleted = false
    var secondCallCompleted = false

    PaymentSheetResultHandler.setHandler { result ->
      firstCallCompleted = true
      throw RuntimeException("Handler exception")
    }

    try {
      PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)
    } catch (e: Exception) {
      // Expected
    }

    PaymentSheetResultHandler.setHandler { result -> secondCallCompleted = true }

    PaymentSheetResultHandler.handleResult(PaymentSheetResult.Completed)

    assert(firstCallCompleted)
    assert(secondCallCompleted)
  }
}
