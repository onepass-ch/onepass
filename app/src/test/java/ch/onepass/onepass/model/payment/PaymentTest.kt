package ch.onepass.onepass.model.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for Payment data classes and enums.
 *
 * These tests verify the structure and behavior of payment-related data classes.
 */
class PaymentTest {

  @Test
  fun payment_hasCorrectDefaults() {
    val payment = Payment(amount = 1000L, eventId = "event-123", userId = "user-456")

    assertEquals("", payment.id)
    assertEquals(1000L, payment.amount)
    assertEquals("chf", payment.currency)
    assertEquals(PaymentStatus.PENDING, payment.status)
    assertEquals("event-123", payment.eventId)
    assertEquals("user-456", payment.userId)
    assertNull(payment.description)
    assertNull(payment.stripePaymentIntentId)
    // createdAt should be set to current time, just verify it's not 0
    assert(payment.createdAt > 0)
  }

  @Test
  fun payment_canBeCreatedWithAllFields() {
    val createdAt = System.currentTimeMillis()
    val payment =
        Payment(
            id = "payment-123",
            amount = 2500L,
            currency = "usd",
            status = PaymentStatus.SUCCEEDED,
            eventId = "event-456",
            userId = "user-789",
            createdAt = createdAt,
            description = "VIP Ticket Purchase",
            stripePaymentIntentId = "pi_123456789")

    assertEquals("payment-123", payment.id)
    assertEquals(2500L, payment.amount)
    assertEquals("usd", payment.currency)
    assertEquals(PaymentStatus.SUCCEEDED, payment.status)
    assertEquals("event-456", payment.eventId)
    assertEquals("user-789", payment.userId)
    assertEquals(createdAt, payment.createdAt)
    assertEquals("VIP Ticket Purchase", payment.description)
    assertEquals("pi_123456789", payment.stripePaymentIntentId)
  }

  @Test
  fun payment_canBeCopied() {
    val original =
        Payment(
            id = "payment-123",
            amount = 1000L,
            eventId = "event-123",
            userId = "user-456",
            status = PaymentStatus.PENDING)

    val updated = original.copy(status = PaymentStatus.SUCCEEDED)

    assertEquals("payment-123", updated.id)
    assertEquals(1000L, updated.amount)
    assertEquals(PaymentStatus.SUCCEEDED, updated.status)
  }

  @Test
  fun paymentStatus_hasAllExpectedValues() {
    val statuses = PaymentStatus.values()
    assertEquals(6, statuses.size)
    assert(statuses.contains(PaymentStatus.PENDING))
    assert(statuses.contains(PaymentStatus.PROCESSING))
    assert(statuses.contains(PaymentStatus.SUCCEEDED))
    assert(statuses.contains(PaymentStatus.FAILED))
    assert(statuses.contains(PaymentStatus.CANCELLED))
    assert(statuses.contains(PaymentStatus.REFUNDED))
  }

  @Test
  fun paymentStatus_canBeCompared() {
    assertEquals(PaymentStatus.PENDING, PaymentStatus.PENDING)
    assert(PaymentStatus.SUCCEEDED != PaymentStatus.FAILED)
  }

  @Test
  fun createPaymentIntentRequest_hasCorrectDefaults() {
    val request = CreatePaymentIntentRequest(amount = 1500L, eventId = "event-789")

    assertEquals(1500L, request.amount)
    assertEquals("chf", request.currency)
    assertEquals("event-789", request.eventId)
    assertNull(request.description)
  }

  @Test
  fun createPaymentIntentRequest_canBeCreatedWithAllFields() {
    val request =
        CreatePaymentIntentRequest(
            amount = 3000L,
            currency = "eur",
            eventId = "event-999",
            description = "Early Bird Ticket")

    assertEquals(3000L, request.amount)
    assertEquals("eur", request.currency)
    assertEquals("event-999", request.eventId)
    assertEquals("Early Bird Ticket", request.description)
  }

  @Test
  fun createPaymentIntentRequest_canBeCopied() {
    val original = CreatePaymentIntentRequest(amount = 1000L, eventId = "event-123")

    val updated = original.copy(currency = "usd")

    assertEquals(1000L, updated.amount)
    assertEquals("usd", updated.currency)
    assertEquals("event-123", updated.eventId)
  }

  @Test
  fun paymentIntentResponse_canBeCreated() {
    val response =
        PaymentIntentResponse(clientSecret = "pi_123_secret_456", paymentIntentId = "pi_123456789")

    assertEquals("pi_123_secret_456", response.clientSecret)
    assertEquals("pi_123456789", response.paymentIntentId)
  }

  @Test
  fun paymentIntentResponse_canBeCopied() {
    val original = PaymentIntentResponse(clientSecret = "secret_1", paymentIntentId = "pi_1")

    val updated = original.copy(clientSecret = "secret_2")

    assertEquals("secret_2", updated.clientSecret)
    assertEquals("pi_1", updated.paymentIntentId)
  }

  @Test
  fun payment_withDifferentCurrencies() {
    val chfPayment = Payment(amount = 1000L, eventId = "e1", userId = "u1", currency = "chf")
    val usdPayment = Payment(amount = 1000L, eventId = "e1", userId = "u1", currency = "usd")
    val eurPayment = Payment(amount = 1000L, eventId = "e1", userId = "u1", currency = "eur")

    assertEquals("chf", chfPayment.currency)
    assertEquals("usd", usdPayment.currency)
    assertEquals("eur", eurPayment.currency)
  }

  @Test
  fun payment_withDifferentStatuses() {
    val basePayment = Payment(amount = 1000L, eventId = "e1", userId = "u1")

    val pending = basePayment.copy(status = PaymentStatus.PENDING)
    val processing = basePayment.copy(status = PaymentStatus.PROCESSING)
    val succeeded = basePayment.copy(status = PaymentStatus.SUCCEEDED)
    val failed = basePayment.copy(status = PaymentStatus.FAILED)
    val cancelled = basePayment.copy(status = PaymentStatus.CANCELLED)
    val refunded = basePayment.copy(status = PaymentStatus.REFUNDED)

    assertEquals(PaymentStatus.PENDING, pending.status)
    assertEquals(PaymentStatus.PROCESSING, processing.status)
    assertEquals(PaymentStatus.SUCCEEDED, succeeded.status)
    assertEquals(PaymentStatus.FAILED, failed.status)
    assertEquals(PaymentStatus.CANCELLED, cancelled.status)
    assertEquals(PaymentStatus.REFUNDED, refunded.status)
  }

  @Test
  fun payment_amountInCents() {
    // Test that amounts are correctly stored in cents
    val tenDollars = Payment(amount = 1000L, eventId = "e1", userId = "u1")
    val fiftyDollars = Payment(amount = 5000L, eventId = "e1", userId = "u1")
    val oneHundredDollars = Payment(amount = 10000L, eventId = "e1", userId = "u1")

    assertEquals(1000L, tenDollars.amount)
    assertEquals(5000L, fiftyDollars.amount)
    assertEquals(10000L, oneHundredDollars.amount)
  }

  @Test
  fun payment_withOptionalDescription() {
    val withDescription =
        Payment(amount = 1000L, eventId = "e1", userId = "u1", description = "Concert ticket")
    val withoutDescription = Payment(amount = 1000L, eventId = "e1", userId = "u1")

    assertEquals("Concert ticket", withDescription.description)
    assertNull(withoutDescription.description)
  }

  @Test
  fun payment_withOptionalStripePaymentIntentId() {
    val withIntentId =
        Payment(amount = 1000L, eventId = "e1", userId = "u1", stripePaymentIntentId = "pi_123456")
    val withoutIntentId = Payment(amount = 1000L, eventId = "e1", userId = "u1")

    assertEquals("pi_123456", withIntentId.stripePaymentIntentId)
    assertNull(withoutIntentId.stripePaymentIntentId)
  }

  @Test
  fun payment_equality() {
    val payment1 =
        Payment(
            id = "p1",
            amount = 1000L,
            eventId = "e1",
            userId = "u1",
            currency = "chf",
            status = PaymentStatus.SUCCEEDED)
    val payment2 =
        Payment(
            id = "p1",
            amount = 1000L,
            eventId = "e1",
            userId = "u1",
            currency = "chf",
            status = PaymentStatus.SUCCEEDED)
    val payment3 =
        Payment(
            id = "p2",
            amount = 1000L,
            eventId = "e1",
            userId = "u1",
            currency = "chf",
            status = PaymentStatus.SUCCEEDED)

    assertEquals(payment1, payment2)
    assert(payment1 != payment3)
  }
}
