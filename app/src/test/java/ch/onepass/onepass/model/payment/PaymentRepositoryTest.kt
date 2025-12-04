package ch.onepass.onepass.model.payment

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PaymentRepository interface using MockK.
 *
 * These tests verify the contract of the PaymentRepository interface using mock implementations.
 */
class PaymentRepositoryTest {

  private lateinit var mockRepository: PaymentRepository

  @Before
  fun setUp() {
    mockRepository = mockk()
  }

  @Test
  fun createPaymentIntent_success_returnsPaymentIntentResponse() = runTest {
    val expectedResponse =
        PaymentIntentResponse(clientSecret = "pi_123_secret_456", paymentIntentId = "pi_123456789")

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 1000L,
          eventId = "event-123",
          ticketTypeId = "ticket-type-1",
          quantity = 2,
          description = "Test ticket purchase")
    } returns Result.success(expectedResponse)

    val result =
        mockRepository.createPaymentIntent(
            amount = 1000L,
            eventId = "event-123",
            ticketTypeId = "ticket-type-1",
            quantity = 2,
            description = "Test ticket purchase")

    assertTrue(result.isSuccess)
    assertEquals(expectedResponse, result.getOrNull())
    assertEquals("pi_123_secret_456", result.getOrNull()?.clientSecret)
    assertEquals("pi_123456789", result.getOrNull()?.paymentIntentId)

    coVerify(exactly = 1) {
      mockRepository.createPaymentIntent(
          amount = 1000L,
          eventId = "event-123",
          ticketTypeId = "ticket-type-1",
          quantity = 2,
          description = "Test ticket purchase")
    }
  }

  @Test
  fun createPaymentIntent_withDefaultParameters_success() = runTest {
    val expectedResponse =
        PaymentIntentResponse(clientSecret = "pi_abc_secret_def", paymentIntentId = "pi_abcdefgh")

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 2500L,
          eventId = "event-456",
          ticketTypeId = null,
          quantity = 1,
          description = null)
    } returns Result.success(expectedResponse)

    val result = mockRepository.createPaymentIntent(amount = 2500L, eventId = "event-456")

    assertTrue(result.isSuccess)
    assertEquals(expectedResponse, result.getOrNull())

    coVerify(exactly = 1) {
      mockRepository.createPaymentIntent(
          amount = 2500L,
          eventId = "event-456",
          ticketTypeId = null,
          quantity = 1,
          description = null)
    }
  }

  @Test
  fun createPaymentIntent_failure_returnsError() = runTest {
    val exception = Exception("Payment creation failed")

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 1000L,
          eventId = "event-123",
          ticketTypeId = null,
          quantity = 1,
          description = null)
    } returns Result.failure(exception)

    val result = mockRepository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    assertTrue(result.isFailure)
    assertEquals("Payment creation failed", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPaymentIntent_authenticationError_returnsError() = runTest {
    val exception = Exception("Please sign in to purchase ticket")

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 1000L,
          eventId = "event-123",
          ticketTypeId = null,
          quantity = 1,
          description = null)
    } returns Result.failure(exception)

    val result = mockRepository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    assertTrue(result.isFailure)
    assertEquals("Please sign in to purchase ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPaymentIntent_networkError_returnsError() = runTest {
    val exception = Exception("Network error occurred")

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 1500L,
          eventId = "event-789",
          ticketTypeId = "ticket-type-2",
          quantity = 3,
          description = "Group ticket")
    } returns Result.failure(exception)

    val result =
        mockRepository.createPaymentIntent(
            amount = 1500L,
            eventId = "event-789",
            ticketTypeId = "ticket-type-2",
            quantity = 3,
            description = "Group ticket")

    assertTrue(result.isFailure)
    assertEquals("Network error occurred", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPaymentIntent_invalidAmount_returnsError() = runTest {
    val exception = IllegalArgumentException("Amount must be positive")

    coEvery {
      mockRepository.createPaymentIntent(
          amount = -100L,
          eventId = "event-123",
          ticketTypeId = null,
          quantity = 1,
          description = null)
    } returns Result.failure(exception)

    val result = mockRepository.createPaymentIntent(amount = -100L, eventId = "event-123")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun createPaymentIntent_withMultipleQuantities() = runTest {
    val response1 = PaymentIntentResponse("secret1", "pi1")
    val response2 = PaymentIntentResponse("secret2", "pi2")
    val response3 = PaymentIntentResponse("secret3", "pi3")

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 1000L,
          eventId = "event-123",
          ticketTypeId = "ticket-type-1",
          quantity = 1,
          description = null)
    } returns Result.success(response1)

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 2000L,
          eventId = "event-123",
          ticketTypeId = "ticket-type-1",
          quantity = 2,
          description = null)
    } returns Result.success(response2)

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 5000L,
          eventId = "event-123",
          ticketTypeId = "ticket-type-1",
          quantity = 5,
          description = null)
    } returns Result.success(response3)

    val result1 = mockRepository.createPaymentIntent(1000L, "event-123", "ticket-type-1", 1)
    val result2 = mockRepository.createPaymentIntent(2000L, "event-123", "ticket-type-1", 2)
    val result3 = mockRepository.createPaymentIntent(5000L, "event-123", "ticket-type-1", 5)

    assertTrue(result1.isSuccess)
    assertTrue(result2.isSuccess)
    assertTrue(result3.isSuccess)
    assertEquals("pi1", result1.getOrNull()?.paymentIntentId)
    assertEquals("pi2", result2.getOrNull()?.paymentIntentId)
    assertEquals("pi3", result3.getOrNull()?.paymentIntentId)
  }

  @Test
  fun createPaymentIntent_withDifferentTicketTypes() = runTest {
    val generalResponse = PaymentIntentResponse("secret_general", "pi_general")
    val vipResponse = PaymentIntentResponse("secret_vip", "pi_vip")

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 1000L,
          eventId = "event-123",
          ticketTypeId = "general",
          quantity = 1,
          description = "General Admission")
    } returns Result.success(generalResponse)

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 5000L,
          eventId = "event-123",
          ticketTypeId = "vip",
          quantity = 1,
          description = "VIP Admission")
    } returns Result.success(vipResponse)

    val generalResult =
        mockRepository.createPaymentIntent(1000L, "event-123", "general", 1, "General Admission")
    val vipResult =
        mockRepository.createPaymentIntent(5000L, "event-123", "vip", 1, "VIP Admission")

    assertTrue(generalResult.isSuccess)
    assertTrue(vipResult.isSuccess)
    assertEquals("pi_general", generalResult.getOrNull()?.paymentIntentId)
    assertEquals("pi_vip", vipResult.getOrNull()?.paymentIntentId)
  }

  @Test
  fun createPaymentIntent_serverError_returnsError() = runTest {
    val exception = Exception("Invalid response from server")

    coEvery {
      mockRepository.createPaymentIntent(
          amount = 1000L,
          eventId = "event-123",
          ticketTypeId = null,
          quantity = 1,
          description = null)
    } returns Result.failure(exception)

    val result = mockRepository.createPaymentIntent(1000L, "event-123")

    assertTrue(result.isFailure)
    assertEquals("Invalid response from server", result.exceptionOrNull()?.message)
  }
}
