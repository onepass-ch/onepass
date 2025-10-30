package ch.onepass.onepass.model.scan

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TicketScanRepositoryFirebaseTest {

  private lateinit var functions: FirebaseFunctions
  private lateinit var callable: HttpsCallableReference
  private lateinit var result: HttpsCallableResult
  private lateinit var repo: TicketScanRepositoryFirebase

  @Before
  fun setUp() {
    functions = mockk(relaxed = true)
    callable = mockk(relaxed = true)
    result = mockk(relaxed = true)

    // Real repo; inject mocked FirebaseFunctions via reflection (no DI changes needed)
    repo = TicketScanRepositoryFirebase()
    repo.javaClass.getDeclaredField("functions").apply {
      isAccessible = true
      set(repo, functions)
    }

    every { functions.getHttpsCallable(any()) } returns callable
  }

  @Test
  fun returnsAcceptedDecision() = runTest {
    val data =
        mapOf("status" to "accepted", "ticketId" to "T123", "scannedAt" to 111L, "remaining" to 2)
    every { result.data } returns data
    every { callable.call(any()) } returns Tasks.forResult(result)

    val res = repo.validateByPass("qr", "event", "device")

    assertTrue(res.isSuccess)
    val accepted = res.getOrNull() as ScanDecision.Accepted
    assertEquals("T123", accepted.ticketId)
    assertEquals(111L, accepted.scannedAtSeconds)
    assertEquals(2, accepted.remaining)
  }

  @Test
  fun acceptedDecisionHandlesIntNumbersAndNullRemaining() = runTest {
    // scannedAt as Int, remaining absent
    val data =
        mapOf(
            "status" to "ACCEPTED", // uppercase to test lowercase() path
            "ticketId" to "T999",
            "scannedAt" to 321, // Int instead of Long
            // no remaining
        )
    every { result.data } returns data
    every { callable.call(any()) } returns Tasks.forResult(result)

    val res = repo.validateByPass("qr", "event", "device")

    assertTrue(res.isSuccess)
    val accepted = res.getOrNull() as ScanDecision.Accepted
    assertEquals("T999", accepted.ticketId)
    assertEquals(321L, accepted.scannedAtSeconds) // Int → Long
    assertNull(accepted.remaining) // missing → null
  }

  @Test
  fun returnsRejectedDecision() = runTest {
    val data = mapOf("status" to "rejected", "reason" to "UNREGISTERED", "scannedAt" to 222L)
    every { result.data } returns data
    every { callable.call(any()) } returns Tasks.forResult(result)

    val res = repo.validateByPass("qr", "event", "device")

    assertTrue(res.isSuccess)
    val rejected = res.getOrNull() as ScanDecision.Rejected
    assertEquals(ScanDecision.Reason.UNREGISTERED, rejected.reason)
    assertEquals(222L, rejected.scannedAtSeconds)
  }

  @Test
  fun rejectedDecisionMapsUnknownReason() = runTest {
    val data = mapOf("status" to "rejected", "reason" to "SOMETHING_WEIRD", "scannedAt" to 333L)
    every { result.data } returns data
    every { callable.call(any()) } returns Tasks.forResult(result)

    val res = repo.validateByPass("qr", "event", "device")

    assertTrue(res.isSuccess)
    val rejected = res.getOrNull() as ScanDecision.Rejected
    assertEquals(ScanDecision.Reason.UNKNOWN, rejected.reason)
    assertEquals(333L, rejected.scannedAtSeconds)
  }

  @Test
  fun failsOnEmptyQr() = runTest {
    val res = repo.validateByPass("", "event", "device")
    assertTrue(res.isFailure)
  }

  @Test
  fun failsOnEmptyEventId() = runTest {
    val res = repo.validateByPass("qr", "", "device")
    assertTrue(res.isFailure)
  }

  @Test
  fun failsOnEmptyDeviceId() = runTest {
    val res = repo.validateByPass("qr", "event", "")
    assertTrue(res.isFailure)
  }

  @Test
  fun failsWhenMissingStatusInResponse() = runTest {
    val data =
        mapOf( // deliberately no "status"
            "ticketId" to "T123")
    every { result.data } returns data
    every { callable.call(any()) } returns Tasks.forResult(result)

    val res = repo.validateByPass("qr", "event", "device")
    assertTrue(res.isFailure)
  }

  @Test
  fun returnsFailureWhenCloudFunctionThrows() = runTest {
    every { callable.call(any()) } returns Tasks.forException(RuntimeException("CF down"))

    val res = repo.validateByPass("qr", "event", "device")
    assertTrue(res.isFailure)
  }
}
