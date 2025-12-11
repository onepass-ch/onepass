package ch.onepass.onepass.model.payment

/**
 * Fake implementation of PaymentRepository for testing purposes.
 *
 * This class provides a simple in-memory implementation that can be used in tests without requiring
 * Firebase or network connectivity.
 *
 * Usage:
 * ```
 * val fakeRepository = FakePaymentRepository()
 *
 * // Configure success response
 * fakeRepository.setCreatePaymentIntentResult(
 *     Result.success(PaymentIntentResponse("secret", "pi_123"))
 * )
 *
 * // Or configure failure
 * fakeRepository.setCreatePaymentIntentResult(
 *     Result.failure(Exception("Payment failed"))
 * )
 * ```
 */
class FakePaymentRepository : PaymentRepository {

  private var createPaymentIntentResult: Result<PaymentIntentResponse> =
      Result.success(
          PaymentIntentResponse(
              clientSecret = "pi_test_secret_123", paymentIntentId = "pi_test_123"))

  private var createMarketplacePaymentIntentResult: Result<MarketplacePaymentIntentResponse> =
      Result.success(
          MarketplacePaymentIntentResponse(
              clientSecret = "pi_marketplace_secret_123",
              paymentIntentId = "pi_marketplace_123",
              ticketId = "ticket_123",
              eventName = "Test Event",
              amount = 100.0,
              currency = "CHF"))

  private var cancelMarketplaceReservationResult: Result<Unit> = Result.success(Unit)

  private val callHistory = mutableListOf<CreatePaymentIntentCall>()
  private val marketplaceCallHistory = mutableListOf<CreateMarketplacePaymentIntentCall>()
  private val cancelReservationHistory = mutableListOf<String>()

  /** Set the result that will be returned by createPaymentIntent. */
  fun setCreatePaymentIntentResult(result: Result<PaymentIntentResponse>) {
    createPaymentIntentResult = result
  }

  /** Set the result that will be returned by createMarketplacePaymentIntent. */
  fun setCreateMarketplacePaymentIntentResult(result: Result<MarketplacePaymentIntentResponse>) {
    createMarketplacePaymentIntentResult = result
  }

  /** Set the result that will be returned by cancelMarketplaceReservation. */
  fun setCancelMarketplaceReservationResult(result: Result<Unit>) {
    cancelMarketplaceReservationResult = result
  }

  /** Get the history of all createPaymentIntent calls. */
  fun getCallHistory(): List<CreatePaymentIntentCall> = callHistory.toList()

  /** Get the history of all createMarketplacePaymentIntent calls. */
  fun getMarketplaceCallHistory(): List<CreateMarketplacePaymentIntentCall> =
      marketplaceCallHistory.toList()

  /** Get the history of all cancelMarketplaceReservation calls. */
  fun getCancelReservationHistory(): List<String> = cancelReservationHistory.toList()

  /** Clear the call history. */
  fun clearCallHistory() {
    callHistory.clear()
    marketplaceCallHistory.clear()
    cancelReservationHistory.clear()
  }

  /** Get the last call made to createPaymentIntent, or null if no calls were made. */
  fun getLastCall(): CreatePaymentIntentCall? = callHistory.lastOrNull()

  /** Get the last call made to createMarketplacePaymentIntent, or null if no calls were made. */
  fun getLastMarketplaceCall(): CreateMarketplacePaymentIntentCall? =
      marketplaceCallHistory.lastOrNull()

  override suspend fun createPaymentIntent(
      amount: Long,
      eventId: String,
      ticketTypeId: String?,
      quantity: Int,
      description: String?
  ): Result<PaymentIntentResponse> {
    callHistory.add(
        CreatePaymentIntentCall(
            amount = amount,
            eventId = eventId,
            ticketTypeId = ticketTypeId,
            quantity = quantity,
            description = description))
    return createPaymentIntentResult
  }

  override suspend fun createMarketplacePaymentIntent(
      ticketId: String,
      description: String?
  ): Result<MarketplacePaymentIntentResponse> {
    marketplaceCallHistory.add(
        CreateMarketplacePaymentIntentCall(ticketId = ticketId, description = description))
    return createMarketplacePaymentIntentResult
  }

  override suspend fun cancelMarketplaceReservation(ticketId: String): Result<Unit> {
    cancelReservationHistory.add(ticketId)
    return cancelMarketplaceReservationResult
  }

  /** Data class representing a call to createPaymentIntent. */
  data class CreatePaymentIntentCall(
      val amount: Long,
      val eventId: String,
      val ticketTypeId: String?,
      val quantity: Int,
      val description: String?
  )

  /** Data class representing a call to createMarketplacePaymentIntent. */
  data class CreateMarketplacePaymentIntentCall(val ticketId: String, val description: String?)
}
