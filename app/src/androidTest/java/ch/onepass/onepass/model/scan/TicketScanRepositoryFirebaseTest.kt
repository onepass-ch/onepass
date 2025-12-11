package ch.onepass.onepass.model.scan

import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TicketScanRepositoryFirebaseTest {

  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockFunctions: FirebaseFunctions
  private lateinit var mockCallable: HttpsCallableReference
  private lateinit var repository: TicketScanRepositoryFirebase

  private val testQr = "onepass:user:v1.payload.signature"
  private val testEventId = "event-123"

  @Before
  fun setup() {
    mockkStatic(FirebaseAuth::class)
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser

    val mockTokenResult = mockk<GetTokenResult>(relaxed = true)
    every { mockUser.getIdToken(any()) } returns Tasks.forResult(mockTokenResult)

    mockkStatic(FirebaseFunctions::class)
    mockFunctions = mockk(relaxed = true)
    mockCallable = mockk(relaxed = true)

    every { FirebaseFunctions.getInstance() } returns mockFunctions
    every { mockFunctions.getHttpsCallable(any()) } returns mockCallable

    repository = TicketScanRepositoryFirebase()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun mockCloudFunctionResponse(responseData: Map<String, Any?>) {
    val mockResult = mockk<HttpsCallableResult>()
    every { mockResult.data } returns responseData
    every { mockCallable.call(any()) } returns Tasks.forResult(mockResult)
  }

  // ========== Auth & Validation ==========

  @Test
  fun shouldFailWhenUserNotAuthenticated() = runTest {
    every { mockAuth.currentUser } returns null
    val result = repository.validateByPass(testQr, testEventId)

    assertTrue(result.isFailure)
    assertEquals("Please login to scan tickets", result.exceptionOrNull()?.message)
  }

  @Test
  fun shouldFailOnBlankQrOrEventId() = runTest {
    assertTrue(repository.validateByPass("  ", testEventId).isFailure)
    assertTrue(repository.validateByPass(testQr, "  ").isFailure)
  }

  @Test
  fun shouldHandleNetworkErrorDuringTokenRefresh() = runTest {
    every { mockUser.getIdToken(any()) } returns
        Tasks.forException(FirebaseNetworkException("Network error"))
    val result = repository.validateByPass(testQr, testEventId)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("Network connection failed") == true)
  }

  // ========== Accepted Flow ==========

  @Test
  fun shouldReturnAcceptedWithAllFields() = runTest {
    mockCloudFunctionResponse(
        mapOf(
            "status" to "accepted",
            "ticketId" to "ticket-456",
            "scannedAt" to 1234567890L,
            "remaining" to 42))

    val result = repository.validateByPass(testQr, testEventId)

    assertTrue(result.isSuccess)
    val decision = result.getOrNull() as ScanDecision.Accepted
    assertEquals("ticket-456", decision.ticketId)
    assertEquals(1234567890L, decision.scannedAtSeconds)
    assertEquals(42, decision.remaining)
  }

  @Test
  fun shouldReturnAcceptedWithNullFields() = runTest {
    mockCloudFunctionResponse(mapOf("status" to "accepted"))

    val decision =
        repository.validateByPass(testQr, testEventId).getOrNull() as ScanDecision.Accepted

    assertNull(decision.ticketId)
    assertNull(decision.scannedAtSeconds)
    assertNull(decision.remaining)
  }

  // ========== Rejected Flow ==========

  @Test
  fun shouldReturnRejectedForAllReasons() = runTest {
    val testCases =
        mapOf(
            "UNREGISTERED" to ScanDecision.Reason.UNREGISTERED,
            "ALREADY_SCANNED" to ScanDecision.Reason.ALREADY_SCANNED,
            "BAD_SIGNATURE" to ScanDecision.Reason.BAD_SIGNATURE,
            "REVOKED" to ScanDecision.Reason.REVOKED,
            "UNKNOWN_REASON" to ScanDecision.Reason.UNKNOWN)

    for ((reasonStr, expectedReason) in testCases) {
      mockCloudFunctionResponse(mapOf("status" to "rejected", "reason" to reasonStr))

      val decision =
          repository.validateByPass(testQr, testEventId).getOrNull() as ScanDecision.Rejected
      assertEquals(expectedReason, decision.reason)
    }
  }

  @Test
  fun shouldHandleLowercaseReasonAndMissingReason() = runTest {
    mockCloudFunctionResponse(mapOf("status" to "rejected", "reason" to "unregistered"))
    var decision =
        repository.validateByPass(testQr, testEventId).getOrNull() as ScanDecision.Rejected
    assertEquals(ScanDecision.Reason.UNREGISTERED, decision.reason)

    mockCloudFunctionResponse(mapOf("status" to "rejected"))
    decision = repository.validateByPass(testQr, testEventId).getOrNull() as ScanDecision.Rejected
    assertEquals(ScanDecision.Reason.UNKNOWN, decision.reason)
  }

  @Test
  fun shouldIncludeScannedAtInRejectedResponse() = runTest {
    mockCloudFunctionResponse(
        mapOf("status" to "rejected", "reason" to "ALREADY_SCANNED", "scannedAt" to 1234567890L))

    val decision =
        repository.validateByPass(testQr, testEventId).getOrNull() as ScanDecision.Rejected
    assertEquals(1234567890L, decision.scannedAtSeconds)
  }

  // ========== Error Handling ==========

  @Test
  fun shouldFailOnInvalidResponseFormat() = runTest {
    val testCases = listOf("not a map", null, mapOf("noStatus" to "value"))

    for (invalidData in testCases) {
      val mockResult = mockk<HttpsCallableResult>()
      every { mockResult.data } returns invalidData
      every { mockCallable.call(any()) } returns Tasks.forResult(mockResult)

      assertTrue(repository.validateByPass(testQr, testEventId).isFailure)
    }
  }

  @Test
  fun shouldPreserveCloudFunctionExceptions() = runTest {
    val originalException = RuntimeException("CF error")
    every { mockCallable.call(any()) } returns Tasks.forException(originalException)

    val result = repository.validateByPass(testQr, testEventId)

    assertTrue(result.isFailure)
    assertEquals(originalException, result.exceptionOrNull())
  }

  // ========== Function Call Verification ==========

  @Test
  fun shouldCallCorrectFunctionWithCorrectParams() = runTest {
    mockCloudFunctionResponse(mapOf("status" to "accepted"))

    val payloadSlot = slot<Any>()
    every { mockCallable.call(capture(payloadSlot)) } returns
        Tasks.forResult(
            mockk<HttpsCallableResult>().apply {
              every { data } returns mapOf("status" to "accepted")
            })

    repository.validateByPass(testQr, testEventId)

    verify { mockFunctions.getHttpsCallable("validateEntryByPassV2") }

    val capturedPayload = payloadSlot.captured as Map<*, *>
    assertEquals(testQr, capturedPayload["qrText"])
    assertEquals(testEventId, capturedPayload["eventId"])

    verify { mockUser.getIdToken(true) }
  }

  // ========== Edge Cases ==========

  @Test
  fun shouldHandleNumberTypesForNumericFields() = runTest {
    mockCloudFunctionResponse(
        mapOf("status" to "accepted", "scannedAt" to 123, "remaining" to 10.0))

    val decision =
        repository.validateByPass(testQr, testEventId).getOrNull() as ScanDecision.Accepted

    assertEquals(123L, decision.scannedAtSeconds)
    assertEquals(10, decision.remaining)
  }
}
