package ch.onepass.onepass.model.payment

interface PaymentRepository {
    suspend fun createPaymentIntent(
        amount: Long,
        eventId: String,
        ticketTypeId: String? = null,
        quantity: Int = 1,
        description: String? = null
    ): Result<PaymentIntentResponse>
}
