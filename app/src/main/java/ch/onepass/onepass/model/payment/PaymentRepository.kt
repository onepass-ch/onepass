package ch.onepass.onepass.model.payment

interface PaymentRepository {
  /** Creates a payment intent for purchasing event tickets directly from an event. */
  suspend fun createPaymentIntent(
      amount: Long,
      eventId: String,
      ticketTypeId: String? = null,
      quantity: Int = 1,
      description: String? = null
  ): Result<PaymentIntentResponse>

  /**
   * Creates a payment intent for purchasing a listed ticket from the marketplace. This reserves the
   * ticket to prevent race conditions.
   *
   * @param ticketId The ID of the listed ticket to purchase.
   * @param description Optional description for the payment.
   * @return PaymentIntentResponse containing the client secret for the payment sheet.
   */
  suspend fun createMarketplacePaymentIntent(
      ticketId: String,
      description: String? = null
  ): Result<MarketplacePaymentIntentResponse>

  /**
   * Cancels a marketplace ticket reservation. Called when the user abandons the payment flow.
   *
   * @param ticketId The ID of the ticket to cancel reservation for.
   */
  suspend fun cancelMarketplaceReservation(ticketId: String): Result<Unit>
}

/** Response from the marketplace payment intent creation. */
data class MarketplacePaymentIntentResponse(
    val clientSecret: String,
    val paymentIntentId: String,
    val ticketId: String,
    val eventName: String,
    val amount: Double,
    val currency: String
)
