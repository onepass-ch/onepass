package ch.onepass.onepass.model.payment

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class PaymentRepositoryFirebase(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) : PaymentRepository {

    override suspend fun createPaymentIntent(
        amount: Long,
        eventId: String,
        ticketTypeId: String?,
        quantity: Int,
        description: String?
    ): Result<PaymentIntentResponse> {
        return try {
            val data = hashMapOf(
                "amount" to amount,
                "currency" to "chf",
                "eventId" to eventId,
                "ticketTypeId" to ticketTypeId,
                "quantity" to quantity,
                "description" to description
            )

            val result = functions
                .getHttpsCallable("createPaymentIntent")
                .call(data)
                .await()

            val responseData = result.data as? Map<*, *>
            val clientSecret = responseData?.get("clientSecret") as? String
            val paymentIntentId = responseData?.get("paymentIntentId") as? String
            val customerId = responseData?.get("customerId") as? String

            if (clientSecret != null && paymentIntentId != null) {
                Result.success(
                    PaymentIntentResponse(
                        clientSecret = clientSecret,
                        paymentIntentId = paymentIntentId
                    )
                )
            } else {
                Result.failure(Exception("Invalid response from server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
