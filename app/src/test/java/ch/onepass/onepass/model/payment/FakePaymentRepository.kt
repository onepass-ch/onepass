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

  private val callHistory = mutableListOf<CreatePaymentIntentCall>()

  /** Set the result that will be returned by createPaymentIntent. */
  fun setCreatePaymentIntentResult(result: Result<PaymentIntentResponse>) {
    createPaymentIntentResult = result
  }

  /** Get the history of all createPaymentIntent calls. */
  fun getCallHistory(): List<CreatePaymentIntentCall> = callHistory.toList()

  /** Clear the call history. */
  fun clearCallHistory() {
    callHistory.clear()
  }

  /** Get the last call made to createPaymentIntent, or null if no calls were made. */
  fun getLastCall(): CreatePaymentIntentCall? = callHistory.lastOrNull()

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

  /** Data class representing a call to createPaymentIntent. */
  data class CreatePaymentIntentCall(
      val amount: Long,
      val eventId: String,
      val ticketTypeId: String?,
      val quantity: Int,
      val description: String?
  )
}
