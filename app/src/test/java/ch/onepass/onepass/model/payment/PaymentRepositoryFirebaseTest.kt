package ch.onepass.onepass.model.payment

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PaymentRepositoryFirebase using MockK.
 *
 * These tests verify the Firebase implementation of PaymentRepository with mocked dependencies.
 */
class PaymentRepositoryFirebaseTest {

  private lateinit var mockFunctions: FirebaseFunctions
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockCallableReference: HttpsCallableReference
  private lateinit var repository: PaymentRepositoryFirebase

  @Before
  fun setUp() {
    mockFunctions = mockk(relaxed = true)
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockCallableReference = mockk(relaxed = true)

    repository = PaymentRepositoryFirebase(mockFunctions, mockAuth)
  }

  @Test
  fun createPaymentIntent_success_returnsPaymentIntentResponse() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call
    every { mockFunctions.getHttpsCallable("createPaymentIntent") } returns mockCallableReference

    val responseData =
        mapOf("clientSecret" to "pi_123_secret_456", "paymentIntentId" to "pi_123456789")
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result =
        repository.createPaymentIntent(
            amount = 1000L,
            eventId = "event-123",
            ticketTypeId = "ticket-type-1",
            quantity = 2,
            description = "Test ticket")

    // Verify
    assertTrue(result.isSuccess)
    assertEquals("pi_123_secret_456", result.getOrNull()?.clientSecret)
    assertEquals("pi_123456789", result.getOrNull()?.paymentIntentId)

    verify { mockFunctions.getHttpsCallable("createPaymentIntent") }
  }

  @Test
  fun createPaymentIntent_userNotAuthenticated_returnsError() = runTest {
    // Mock no authenticated user
    every { mockAuth.currentUser } returns null

    // Execute
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Please sign in to purchase ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPaymentIntent_tokenRetrievalFails_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval failure
    val tokenTask = Tasks.forException<GetTokenResult>(Exception("Token retrieval failed"))
    every { mockUser.getIdToken(true) } returns tokenTask

    // Execute
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Please sign in to purchase ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPaymentIntent_tokenIsNull_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval with null token
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns null
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Execute
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Please sign in to purchase ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPaymentIntent_invalidResponseMissingClientSecret_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with invalid response
    every { mockFunctions.getHttpsCallable("createPaymentIntent") } returns mockCallableReference

    val responseData =
        mapOf(
            "paymentIntentId" to "pi_123456789"
            // Missing clientSecret
            )
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Invalid response from server", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPaymentIntent_invalidResponseMissingPaymentIntentId_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with invalid response
    every { mockFunctions.getHttpsCallable("createPaymentIntent") } returns mockCallableReference

    val responseData =
        mapOf(
            "clientSecret" to "pi_123_secret_456"
            // Missing paymentIntentId
            )
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Invalid response from server", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPaymentIntent_functionCallFails_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call failure
    every { mockFunctions.getHttpsCallable("createPaymentIntent") } returns mockCallableReference
    val callTask = Tasks.forException<HttpsCallableResult>(Exception("Network error"))
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    // Verify
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("Payment failed") == true)
  }

  @Test
  fun createPaymentIntent_authenticationError_returnsAuthError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with authentication error
    every { mockFunctions.getHttpsCallable("createPaymentIntent") } returns mockCallableReference
    val callTask = Tasks.forException<HttpsCallableResult>(Exception("User must be authenticated"))
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Please sign in to purchase ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPaymentIntent_unauthenticatedError_returnsAuthError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with unauthenticated error
    every { mockFunctions.getHttpsCallable("createPaymentIntent") } returns mockCallableReference
    val callTask = Tasks.forException<HttpsCallableResult>(Exception("Request is unauthenticated"))
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Please sign in to purchase ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPaymentIntent_withAllParameters_success() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call
    every { mockFunctions.getHttpsCallable("createPaymentIntent") } returns mockCallableReference

    val responseData =
        mapOf("clientSecret" to "pi_abc_secret_def", "paymentIntentId" to "pi_abcdefgh")
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute with all parameters
    val result =
        repository.createPaymentIntent(
            amount = 2500L,
            eventId = "event-456",
            ticketTypeId = "vip-ticket",
            quantity = 3,
            description = "VIP Group Tickets")

    // Verify
    assertTrue(result.isSuccess)
    assertEquals("pi_abc_secret_def", result.getOrNull()?.clientSecret)
    assertEquals("pi_abcdefgh", result.getOrNull()?.paymentIntentId)
  }

  @Test
  fun createPaymentIntent_withDefaultParameters_success() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call
    every { mockFunctions.getHttpsCallable("createPaymentIntent") } returns mockCallableReference

    val responseData =
        mapOf("clientSecret" to "pi_default_secret", "paymentIntentId" to "pi_default")
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute with minimal parameters
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    // Verify
    assertTrue(result.isSuccess)
    assertEquals("pi_default_secret", result.getOrNull()?.clientSecret)
    assertEquals("pi_default", result.getOrNull()?.paymentIntentId)
  }

  @Test
  fun createPaymentIntent_responseDataNotMap_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with non-map response
    every { mockFunctions.getHttpsCallable("createPaymentIntent") } returns mockCallableReference

    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns "invalid response"

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createPaymentIntent(amount = 1000L, eventId = "event-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Invalid response from server", result.exceptionOrNull()?.message)
  }

  // ============================================================================
  // Tests for createMarketplacePaymentIntent
  // ============================================================================

  @Test
  fun createMarketplacePaymentIntent_success_returnsMarketplacePaymentIntentResponse() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference

    val responseData =
        mapOf(
            "clientSecret" to "pi_marketplace_secret",
            "paymentIntentId" to "pi_marketplace_123",
            "ticketId" to "ticket-456",
            "eventName" to "Rock Concert 2024",
            "amount" to 75.0,
            "currency" to "chf")
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-456")

    // Verify
    assertTrue(result.isSuccess)
    val response = result.getOrNull()
    assertEquals("pi_marketplace_secret", response?.clientSecret)
    assertEquals("pi_marketplace_123", response?.paymentIntentId)
    assertEquals("ticket-456", response?.ticketId)
    assertEquals("Rock Concert 2024", response?.eventName)
    assertEquals(75.0, response?.amount, 0.01)
    assertEquals("chf", response?.currency)

    verify { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") }
  }

  @Test
  fun createMarketplacePaymentIntent_withDescription_success() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference

    val responseData =
        mapOf(
            "clientSecret" to "pi_secret",
            "paymentIntentId" to "pi_123",
            "ticketId" to "ticket-789",
            "eventName" to "Jazz Festival",
            "amount" to 50.0,
            "currency" to "chf")
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute with description
    val result =
        repository.createMarketplacePaymentIntent(
            ticketId = "ticket-789", description = "Urgent purchase for weekend event")

    // Verify
    assertTrue(result.isSuccess)
    assertEquals("pi_secret", result.getOrNull()?.clientSecret)
    assertEquals("pi_123", result.getOrNull()?.paymentIntentId)
  }

  @Test
  fun createMarketplacePaymentIntent_userNotAuthenticated_returnsError() = runTest {
    // Mock no authenticated user
    every { mockAuth.currentUser } returns null

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Please sign in to purchase ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createMarketplacePaymentIntent_tokenRetrievalFails_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval failure
    val tokenTask = Tasks.forException<GetTokenResult>(Exception("Token expired"))
    every { mockUser.getIdToken(true) } returns tokenTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Please sign in to purchase ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createMarketplacePaymentIntent_tokenIsNull_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval with null token
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns null
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Please sign in to purchase ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createMarketplacePaymentIntent_invalidResponseMissingClientSecret_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with invalid response (missing clientSecret)
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference

    val responseData =
        mapOf(
            "paymentIntentId" to "pi_123",
            "ticketId" to "ticket-456"
            // Missing clientSecret
            )
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-456")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Invalid response from server", result.exceptionOrNull()?.message)
  }

  @Test
  fun createMarketplacePaymentIntent_invalidResponseMissingPaymentIntentId_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with invalid response (missing paymentIntentId)
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference

    val responseData =
        mapOf(
            "clientSecret" to "pi_secret_123",
            "ticketId" to "ticket-456"
            // Missing paymentIntentId
            )
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-456")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Invalid response from server", result.exceptionOrNull()?.message)
  }

  @Test
  fun createMarketplacePaymentIntent_ticketNotAvailable_returnsSpecificError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call failure with "not available" error
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference
    val callTask =
        Tasks.forException<HttpsCallableResult>(
            Exception("Ticket is not available for purchase"))
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("This ticket is no longer available", result.exceptionOrNull()?.message)
  }

  @Test
  fun createMarketplacePaymentIntent_ticketNoLongerListed_returnsSpecificError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call failure with "no longer listed" error
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference
    val callTask =
        Tasks.forException<HttpsCallableResult>(Exception("Ticket is no longer listed for sale"))
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("This ticket is no longer available", result.exceptionOrNull()?.message)
  }

  @Test
  fun createMarketplacePaymentIntent_ticketReservedByAnother_returnsSpecificError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call failure with "reserved by another" error
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference
    val callTask =
        Tasks.forException<HttpsCallableResult>(
            Exception("Ticket is currently reserved by another buyer"))
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals(
        "This ticket is currently being purchased by another buyer",
        result.exceptionOrNull()?.message)
  }

  @Test
  fun createMarketplacePaymentIntent_cannotPurchaseOwnTicket_returnsSpecificError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call failure with "cannot purchase your own" error
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference
    val callTask =
        Tasks.forException<HttpsCallableResult>(Exception("You cannot purchase your own ticket"))
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("You cannot purchase your own ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createMarketplacePaymentIntent_unauthenticatedError_returnsAuthError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with authentication error
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference
    val callTask =
        Tasks.forException<HttpsCallableResult>(Exception("User must be authenticated"))
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Please sign in to purchase ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun createMarketplacePaymentIntent_genericError_returnsDetailedError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with generic error
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference
    val callTask = Tasks.forException<HttpsCallableResult>(Exception("Network timeout"))
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-123")

    // Verify
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("Purchase failed") == true)
    assertTrue(result.exceptionOrNull()?.message?.contains("Network timeout") == true)
  }

  @Test
  fun createMarketplacePaymentIntent_responseWithDefaultValues_success() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with minimal response (missing optional fields)
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference

    val responseData =
        mapOf(
            "clientSecret" to "pi_minimal_secret",
            "paymentIntentId" to "pi_minimal_123"
            // Missing ticketId, eventName, amount, currency
            )
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-original")

    // Verify - should use default values
    assertTrue(result.isSuccess)
    val response = result.getOrNull()
    assertEquals("pi_minimal_secret", response?.clientSecret)
    assertEquals("pi_minimal_123", response?.paymentIntentId)
    assertEquals("ticket-original", response?.ticketId) // Falls back to original ticketId
    assertEquals("Event Ticket", response?.eventName) // Default value
    assertEquals(0.0, response?.amount, 0.01) // Default value
    assertEquals("chf", response?.currency) // Default value
  }

  @Test
  fun createMarketplacePaymentIntent_responseDataNotMap_returnsError() = runTest {
    // Mock authenticated user
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "buyer-123"

    // Mock token retrieval
    val mockTokenResult = mockk<GetTokenResult>()
    every { mockTokenResult.token } returns "valid-token"
    val tokenTask = Tasks.forResult(mockTokenResult)
    every { mockUser.getIdToken(true) } returns tokenTask

    // Mock function call with non-map response
    every { mockFunctions.getHttpsCallable("createMarketplacePaymentIntent") } returns
        mockCallableReference

    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns "invalid response"

    val callTask = Tasks.forResult(mockResult)
    every { mockCallableReference.call(any()) } returns callTask

    // Execute
    val result = repository.createMarketplacePaymentIntent(ticketId = "ticket-123")

    // Verify
    assertTrue(result.isFailure)
    assertEquals("Invalid response from server", result.exceptionOrNull()?.message)
  }
}
