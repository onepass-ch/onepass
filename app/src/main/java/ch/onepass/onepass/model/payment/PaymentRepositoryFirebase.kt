package ch.onepass.onepass.model.payment

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class PaymentRepositoryFirebase(
    private val functions: FirebaseFunctions = Firebase.functions,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : PaymentRepository {

  init {
    android.util.Log.d(
        "PaymentRepository", "Initialized with functions region: ${functions.javaClass.name}")
  }

  override suspend fun createPaymentIntent(
      amount: Long,
      eventId: String,
      ticketTypeId: String?,
      quantity: Int,
      description: String?
  ): Result<PaymentIntentResponse> {
    val signInErrorMessage = "Please sign in to purchase ticket"
    return try {
      // Verify user is authenticated before making the call
      val currentUser = auth.currentUser
      if (currentUser == null) {
        android.util.Log.e("PaymentRepository", "❌ User not authenticated")
        return Result.failure(Exception(signInErrorMessage))
      }

      android.util.Log.d("PaymentRepository", "✓ User authenticated: ${currentUser.uid}")

      // Get a fresh auth token to ensure it's valid and available
      // This is critical - the token must be obtained before the call
      val idToken =
          try {
            val token = currentUser.getIdToken(true).await()
            android.util.Log.d("PaymentRepository", "✓ Auth token obtained successfully")
            token.token
          } catch (e: Exception) {
            android.util.Log.e("PaymentRepository", "❌ Failed to get auth token: ${e.message}", e)
            return Result.failure(Exception(signInErrorMessage))
          }

      if (idToken == null) {
        android.util.Log.e("PaymentRepository", "❌ Auth token is null")
        return Result.failure(Exception(signInErrorMessage))
      }

      val data =
          hashMapOf(
              "amount" to amount,
              "currency" to "chf",
              "eventId" to eventId,
              "ticketTypeId" to ticketTypeId,
              "quantity" to quantity,
              "description" to description)

      android.util.Log.d(
          "PaymentRepository",
          "🎫 Calling createPaymentIntent with: amount=$amount, eventId=$eventId, ticketTypeId=$ticketTypeId, quantity=$quantity")
      android.util.Log.d("PaymentRepository", "✓ Auth token is available: ${idToken.isNotBlank()}")

      // Firebase Functions automatically includes the auth token from currentUser
      // Both Firebase.functions and FirebaseAuth.getInstance() use the default Firebase app,
      // so they are already linked. The token we obtained above ensures it's fresh and available.
      // The SDK should automatically include the token in the Authorization header.
      val result: HttpsCallableResult =
          functions.getHttpsCallable("createPaymentIntent").call(data).await()

      android.util.Log.d("PaymentRepository", "✓ Cloud function returned successfully")

      val responseData = result.data as? Map<*, *>
      android.util.Log.d("PaymentRepository", "Response data keys: ${responseData?.keys}")

      val clientSecret = responseData?.get("clientSecret") as? String
      val paymentIntentId = responseData?.get("paymentIntentId") as? String

      if (clientSecret != null && paymentIntentId != null) {
        android.util.Log.d("PaymentRepository", "✅ Payment intent created: $paymentIntentId")
        Result.success(
            PaymentIntentResponse(clientSecret = clientSecret, paymentIntentId = paymentIntentId))
      } else {
        android.util.Log.e(
            "PaymentRepository",
            "❌ Invalid response: clientSecret=${clientSecret != null}, paymentIntentId=${paymentIntentId != null}")
        android.util.Log.e("PaymentRepository", "Response data: $responseData")
        Result.failure(Exception("Invalid response from server"))
      }
    } catch (e: Exception) {
      // Comprehensive error logging
      android.util.Log.e("PaymentRepository", "❌❌❌ Error creating payment intent", e)
      android.util.Log.e("PaymentRepository", "Error type: ${e.javaClass.simpleName}")
      android.util.Log.e("PaymentRepository", "Error message: ${e.message}")
      android.util.Log.e("PaymentRepository", "Error cause: ${e.cause?.message}")

      // Check if it's an authentication-related error
      val errorMessage = e.message ?: ""
      if (errorMessage.contains("unauthenticated", ignoreCase = true) == true ||
          errorMessage.contains("authentication", ignoreCase = true) == true ||
          errorMessage.contains("authenticated", ignoreCase = true) == true ||
          errorMessage.contains("must be authenticated", ignoreCase = true) == true ||
          e.cause?.message?.contains("unauthenticated", ignoreCase = true) == true) {
        android.util.Log.e("PaymentRepository", "🔐 Authentication error detected")
        Result.failure(Exception(signInErrorMessage))
      } else {
        // Return the actual error message to help with debugging
        val detailedMessage = buildString {
          append("Payment failed: ")
          append(e.message ?: "Unknown error")
          e.cause?.message?.let { append(" (Cause: $it)") }
        }
        android.util.Log.e("PaymentRepository", "Error details: $detailedMessage")
        Result.failure(Exception(detailedMessage))
      }
    }
  }

  override suspend fun createMarketplacePaymentIntent(
      ticketId: String,
      description: String?
  ): Result<MarketplacePaymentIntentResponse> {
    val signInErrorMessage = "Please sign in to purchase ticket"
    return try {
      // Verify user is authenticated
      val currentUser = auth.currentUser
      if (currentUser == null) {
        android.util.Log.e("PaymentRepository", "❌ User not authenticated")
        return Result.failure(Exception(signInErrorMessage))
      }

      android.util.Log.d("PaymentRepository", "✓ User authenticated: ${currentUser.uid}")

      // Get a fresh auth token
      val idToken =
          try {
            val token = currentUser.getIdToken(true).await()
            android.util.Log.d("PaymentRepository", "✓ Auth token obtained successfully")
            token.token
          } catch (e: Exception) {
            android.util.Log.e("PaymentRepository", "❌ Failed to get auth token: ${e.message}", e)
            return Result.failure(Exception(signInErrorMessage))
          }

      if (idToken == null) {
        android.util.Log.e("PaymentRepository", "❌ Auth token is null")
        return Result.failure(Exception(signInErrorMessage))
      }

      val data = hashMapOf("ticketId" to ticketId, "description" to description)

      android.util.Log.d(
          "PaymentRepository", "🛒 Calling createMarketplacePaymentIntent with ticketId=$ticketId")

      val result: HttpsCallableResult =
          functions.getHttpsCallable("createMarketplacePaymentIntent").call(data).await()

      android.util.Log.d(
          "PaymentRepository", "✓ Marketplace payment function returned successfully")

      val responseData = result.data as? Map<*, *>
      android.util.Log.d("PaymentRepository", "Response data keys: ${responseData?.keys}")

      val clientSecret = responseData?.get("clientSecret") as? String
      val paymentIntentId = responseData?.get("paymentIntentId") as? String
      val responseTicketId = responseData?.get("ticketId") as? String ?: ticketId
      val eventName = responseData?.get("eventName") as? String ?: "Event Ticket"
      val amount = (responseData?.get("amount") as? Number)?.toDouble() ?: 0.0
      val currency = responseData?.get("currency") as? String ?: "chf"

      if (clientSecret != null && paymentIntentId != null) {
        android.util.Log.d(
            "PaymentRepository", "✅ Marketplace payment intent created: $paymentIntentId")
        Result.success(
            MarketplacePaymentIntentResponse(
                clientSecret = clientSecret,
                paymentIntentId = paymentIntentId,
                ticketId = responseTicketId,
                eventName = eventName,
                amount = amount,
                currency = currency))
      } else {
        android.util.Log.e(
            "PaymentRepository",
            "❌ Invalid response: clientSecret=${clientSecret != null}, paymentIntentId=${paymentIntentId != null}")
        Result.failure(Exception("Invalid response from server"))
      }
    } catch (e: Exception) {
      android.util.Log.e("PaymentRepository", "❌❌❌ Error creating marketplace payment intent", e)
      android.util.Log.e("PaymentRepository", "Error message: ${e.message}")

      val errorMessage = e.message ?: ""
      when {
        errorMessage.contains("not available for purchase", ignoreCase = true) ||
            errorMessage.contains("no longer listed", ignoreCase = true) -> {
          Result.failure(Exception("This ticket is no longer available"))
        }
        errorMessage.contains("reserved by another", ignoreCase = true) -> {
          Result.failure(Exception("This ticket is currently being purchased by another buyer"))
        }
        errorMessage.contains("cannot purchase your own", ignoreCase = true) -> {
          Result.failure(Exception("You cannot purchase your own ticket"))
        }
        errorMessage.contains("unauthenticated", ignoreCase = true) ||
            errorMessage.contains("authentication", ignoreCase = true) ||
            errorMessage.contains("authenticated", ignoreCase = true) -> {
          Result.failure(Exception(signInErrorMessage))
        }
        else -> {
          val detailedMessage = buildString {
            append("Purchase failed: ")
            append(e.message ?: "Unknown error")
          }
          Result.failure(Exception(detailedMessage))
        }
      }
    }
  }

  override suspend fun cancelMarketplaceReservation(ticketId: String): Result<Unit> {
    return try {
      val currentUser = auth.currentUser
      if (currentUser == null) {
        android.util.Log.e("PaymentRepository", "❌ User not authenticated for reservation cancel")
        return Result.failure(Exception("Not authenticated"))
      }

      // Get a fresh auth token
      currentUser.getIdToken(true).await()

      val data = hashMapOf("ticketId" to ticketId)

      android.util.Log.d("PaymentRepository", "🔓 Cancelling reservation for ticket $ticketId")

      functions.getHttpsCallable("cancelMarketplaceReservation").call(data).await()

      android.util.Log.d("PaymentRepository", "✓ Reservation cancelled for ticket $ticketId")
      Result.success(Unit)
    } catch (e: Exception) {
      android.util.Log.e("PaymentRepository", "❌ Error cancelling reservation: ${e.message}")
      Result.failure(e)
    }
  }
}
