package ch.onepass.onepass.model.payment

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FakePaymentRepository.
 *
 * These tests verify the fake repository implementation and demonstrate its usage.
 */
class FakePaymentRepositoryTest {

  private lateinit var repository: FakePaymentRepository

  @Before
  fun setUp() {
    repository = FakePaymentRepository()
  }

  @Test
  fun defaultBehavior_returnsSuccessWithTestData() = runTest {
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    assertTrue(result.isSuccess)
    assertEquals("pi_test_secret_123", result.getOrNull()?.clientSecret)
    assertEquals("pi_test_123", result.getOrNull()?.paymentIntentId)
  }

  @Test
  fun setCreatePaymentIntentResult_returnsConfiguredResult() = runTest {
    val customResponse =
        PaymentIntentResponse(clientSecret = "custom_secret", paymentIntentId = "custom_pi")
    repository.setCreatePaymentIntentResult(Result.success(customResponse))

    val result = repository.createPaymentIntent(amount = 2000L, eventId = "event-456")

    assertTrue(result.isSuccess)
    assertEquals("custom_secret", result.getOrNull()?.clientSecret)
    assertEquals("custom_pi", result.getOrNull()?.paymentIntentId)
  }

  @Test
  fun setCreatePaymentIntentResult_failure_returnsError() = runTest {
    repository.setCreatePaymentIntentResult(Result.failure(Exception("Payment failed")))

    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    assertTrue(result.isFailure)
    assertEquals("Payment failed", result.exceptionOrNull()?.message)
  }

  @Test
  fun callHistory_tracksAllCalls() = runTest {
    repository.createPaymentIntent(1000L, "event-1", "ticket-1", 1, "desc-1")
    repository.createPaymentIntent(2000L, "event-2", "ticket-2", 2, "desc-2")
    repository.createPaymentIntent(3000L, "event-3", null, 3, null)

    val history = repository.getCallHistory()

    assertEquals(3, history.size)

    assertEquals(1000L, history[0].amount)
    assertEquals("event-1", history[0].eventId)
    assertEquals("ticket-1", history[0].ticketTypeId)
    assertEquals(1, history[0].quantity)
    assertEquals("desc-1", history[0].description)

    assertEquals(2000L, history[1].amount)
    assertEquals("event-2", history[1].eventId)
    assertEquals("ticket-2", history[1].ticketTypeId)
    assertEquals(2, history[1].quantity)
    assertEquals("desc-2", history[1].description)

    assertEquals(3000L, history[2].amount)
    assertEquals("event-3", history[2].eventId)
    assertNull(history[2].ticketTypeId)
    assertEquals(3, history[2].quantity)
    assertNull(history[2].description)
  }

  @Test
  fun getLastCall_returnsLastCall() = runTest {
    repository.createPaymentIntent(1000L, "event-1")
    repository.createPaymentIntent(2000L, "event-2")
    repository.createPaymentIntent(3000L, "event-3")

    val lastCall = repository.getLastCall()

    assertEquals(3000L, lastCall?.amount)
    assertEquals("event-3", lastCall?.eventId)
  }

  @Test
  fun getLastCall_noCallsMade_returnsNull() {
    val lastCall = repository.getLastCall()
    assertNull(lastCall)
  }

  @Test
  fun clearCallHistory_removesAllCalls() = runTest {
    repository.createPaymentIntent(1000L, "event-1")
    repository.createPaymentIntent(2000L, "event-2")

    assertEquals(2, repository.getCallHistory().size)

    repository.clearCallHistory()

    assertEquals(0, repository.getCallHistory().size)
    assertNull(repository.getLastCall())
  }

  @Test
  fun callHistory_withDefaultParameters() = runTest {
    repository.createPaymentIntent(amount = 1500L, eventId = "event-999")

    val lastCall = repository.getLastCall()

    assertEquals(1500L, lastCall?.amount)
    assertEquals("event-999", lastCall?.eventId)
    assertNull(lastCall?.ticketTypeId)
    assertEquals(1, lastCall?.quantity)
    assertNull(lastCall?.description)
  }

  @Test
  fun multipleResults_canBeConfigured() = runTest {
    // First call - success
    repository.setCreatePaymentIntentResult(Result.success(PaymentIntentResponse("secret1", "pi1")))
    val result1 = repository.createPaymentIntent(1000L, "event-1")
    assertTrue(result1.isSuccess)
    assertEquals("pi1", result1.getOrNull()?.paymentIntentId)

    // Second call - failure
    repository.setCreatePaymentIntentResult(Result.failure(Exception("Error")))
    val result2 = repository.createPaymentIntent(2000L, "event-2")
    assertTrue(result2.isFailure)

    // Third call - success again
    repository.setCreatePaymentIntentResult(Result.success(PaymentIntentResponse("secret3", "pi3")))
    val result3 = repository.createPaymentIntent(3000L, "event-3")
    assertTrue(result3.isSuccess)
    assertEquals("pi3", result3.getOrNull()?.paymentIntentId)

    // All calls should be tracked
    assertEquals(3, repository.getCallHistory().size)
  }

  @Test
  fun callHistoryImmutable_returnsNewList() = runTest {
    repository.createPaymentIntent(1000L, "event-1")

    val history1 = repository.getCallHistory()
    repository.createPaymentIntent(2000L, "event-2")
    val history2 = repository.getCallHistory()

    // Original list should not be modified
    assertEquals(1, history1.size)
    assertEquals(2, history2.size)
  }

  @Test
  fun createPaymentIntentCall_dataClass_equality() {
    val call1 =
        FakePaymentRepository.CreatePaymentIntentCall(
            amount = 1000L,
            eventId = "event-1",
            ticketTypeId = "ticket-1",
            quantity = 1,
            description = "desc")
    val call2 =
        FakePaymentRepository.CreatePaymentIntentCall(
            amount = 1000L,
            eventId = "event-1",
            ticketTypeId = "ticket-1",
            quantity = 1,
            description = "desc")
    val call3 =
        FakePaymentRepository.CreatePaymentIntentCall(
            amount = 2000L,
            eventId = "event-1",
            ticketTypeId = "ticket-1",
            quantity = 1,
            description = "desc")

    assertEquals(call1, call2)
    assert(call1 != call3)
  }
}
