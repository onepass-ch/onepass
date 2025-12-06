package ch.onepass.onepass.model.payment

/**
 * Data class representing a payment in OnePass.
 *
 * @property id Unique identifier for the payment
 * @property amount Amount in cents (e.g., 1000 = $10.00)
 * @property currency Currency code (e.g., "usd", "eur", "chf")
 * @property status Payment status (requires_payment_method, requires_confirmation, succeeded, etc.)
 * @property eventId Associated event ID
 * @property userId User who made the payment
 * @property createdAt Timestamp when the payment was created
 * @property description Optional description of the payment
 */
data class Payment(
    val id: String = "",
    val amount: Long,
    val currency: String = "chf", // Swiss Franc as default
    val status: PaymentStatus = PaymentStatus.PENDING,
    val eventId: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val description: String? = null,
    val stripePaymentIntentId: String? = null
)

/** Enum representing the possible states of a payment. */
enum class PaymentStatus {
  PENDING,
  PROCESSING,
  SUCCEEDED,
  FAILED,
  CANCELLED,
  REFUNDED
}

/**
 * Data class for creating a payment intent request to the backend.
 *
 * @property amount Amount in cents
 * @property currency Currency code
 * @property eventId Associated event ID
 * @property description Optional payment description
 */
data class CreatePaymentIntentRequest(
    val amount: Long,
    val currency: String = "chf",
    val eventId: String,
    val description: String? = null
)

/**
 * Data class for the payment intent response from the backend.
 *
 * @property clientSecret The client secret to use with Stripe SDK
 * @property paymentIntentId The ID of the payment intent
 */
data class PaymentIntentResponse(val clientSecret: String, val paymentIntentId: String)
