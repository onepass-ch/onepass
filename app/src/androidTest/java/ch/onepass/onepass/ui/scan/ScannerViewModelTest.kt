package ch.onepass.onepass.ui.scan

import ch.onepass.onepass.model.scan.ScanDecision
import ch.onepass.onepass.model.scan.TicketScanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

  private lateinit var repository: TicketScanRepository
  private val testDispatcher = StandardTestDispatcher()
  private val eventId = "event-123"

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createValidQr(uid: String = "user-001"): String {
    val payload = """{"uid":"$uid","kid":"key-1","iat":1000,"ver":1}"""
    val payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
    return "onepass:user:v1.$payloadB64.validSignature123"
  }

  private fun createViewModel(
      testScheduler: TestCoroutineScheduler,
      resetDelayMs: Long = Long.MAX_VALUE,
      customRepo: TicketScanRepository? = null,
      customClock: (() -> Long)? = null,
      enableAutoCleanup: Boolean = false
  ): ScannerViewModel =
      ScannerViewModel(
          eventId = eventId,
          repo = customRepo ?: repository,
          clock = customClock ?: { System.currentTimeMillis() },
          enableAutoCleanup = enableAutoCleanup,
          stateResetDelayMs = resetDelayMs,
          coroutineScope = TestScope(testScheduler))

  // ========== Basic Validation ==========

  @Test
  fun initialStateShouldBeIdle() =
      runTest(testDispatcher) {
        val vm = createViewModel(testScheduler)
        with(vm.state.value) {
          assertFalse(isProcessing)
          assertEquals("Scan a pass…", message)
          assertNull(lastTicketId)
          assertEquals(ScannerUiState.Status.IDLE, status)
        }
      }

  @Test(expected = IllegalArgumentException::class)
  fun shouldRejectBlankEventId() {
    ScannerViewModel(eventId = "  ", repo = repository, coroutineScope = TestScope())
  }

  @Test(expected = IllegalArgumentException::class)
  fun shouldRejectEmptyEventId() {
    ScannerViewModel(eventId = "", repo = repository, coroutineScope = TestScope())
  }

  @Test
  fun invalidQrShouldReject() =
      runTest(testDispatcher) {
        val vm = createViewModel(testScheduler)
        var effect: ScannerEffect? = null
        val job = launch { vm.effects.collect { effect = it } }

        vm.onQrScanned("bad-qr")
        advanceUntilIdle()

        assertTrue(effect is ScannerEffect.Rejected)
        assertEquals("Invalid QR format", (effect as ScannerEffect.Rejected).message)
        job.cancel()
      }

  @Test
  fun qrWithMissingFieldShouldReject() =
      runTest(testDispatcher) {
        val vm = createViewModel(testScheduler)
        val payload = """{"uid":"user-123","iat":1000,"ver":1}"""
        val payloadB64 =
            Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        vm.onQrScanned("onepass:user:v1.$payloadB64.signature")
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.REJECTED, vm.state.value.status)
      }

  // ========== Accepted Flow ==========

  @Test
  fun acceptedShouldUpdateStateAndEmitEffect() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> =
                  Result.success(
                      ScanDecision.Accepted(
                          ticketId = "ticket-456", scannedAtSeconds = 2000L, remaining = 5))
            }
        val vm = createViewModel(testScheduler, customRepo = repo)
        var effect: ScannerEffect? = null
        val job = launch { vm.effects.collect { effect = it } }

        vm.onQrScanned(createValidQr())
        advanceUntilIdle()

        with(vm.state.value) {
          assertEquals(ScannerUiState.Status.ACCEPTED, status)
          assertEquals("Access Granted", message)
          assertEquals("ticket-456", lastTicketId)
          assertEquals(2000L, lastScannedAt)
          assertEquals(5, remaining)
        }
        assertTrue(effect is ScannerEffect.Accepted)
        job.cancel()
      }

  @Test
  fun acceptedWithNullFieldsShouldWork() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> =
                  Result.success(
                      ScanDecision.Accepted(
                          ticketId = null, scannedAtSeconds = null, remaining = null))
            }
        val vm = createViewModel(testScheduler, customRepo = repo)
        vm.onQrScanned(createValidQr())
        advanceUntilIdle()

        with(vm.state.value) {
          assertEquals(ScannerUiState.Status.ACCEPTED, status)
          assertNull(lastTicketId)
          assertNull(lastScannedAt)
          assertNull(remaining)
        }
      }

  // ========== Rejected Flow ==========

  @Test
  fun rejectedReasonsShouldShowCorrectMessages() =
      runTest(testDispatcher) {
        val reasons: Map<ScanDecision.Reason, String> =
            mapOf(
                ScanDecision.Reason.UNREGISTERED to "User not registered",
                ScanDecision.Reason.ALREADY_SCANNED to "Already scanned",
                ScanDecision.Reason.BAD_SIGNATURE to "Invalid signature",
                ScanDecision.Reason.REVOKED to "Pass revoked",
                ScanDecision.Reason.UNKNOWN to "Access denied")

        for ((reason, expectedMsg) in reasons) {
          val repo =
              object : TicketScanRepository {
                override suspend fun validateByPass(
                    qrText: String,
                    eventId: String
                ): Result<ScanDecision> = Result.success(ScanDecision.Rejected(reason = reason))
              }
          val vm = createViewModel(testScheduler, customRepo = repo)
          vm.onQrScanned(createValidQr())
          advanceUntilIdle()

          assertEquals(ScannerUiState.Status.REJECTED, vm.state.value.status)
          assertEquals(expectedMsg, vm.state.value.message)
        }
      }

  @Test
  fun rejectedShouldEmitEffect() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> =
                  Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.UNREGISTERED))
            }
        val vm = createViewModel(testScheduler, customRepo = repo)
        var effect: ScannerEffect? = null
        val job = launch { vm.effects.collect { effect = it } }

        vm.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertTrue(effect is ScannerEffect.Rejected)
        job.cancel()
      }

  // ========== Error Handling ==========

  @Test
  fun errorWithMessageShouldShowIt() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> = Result.failure(RuntimeException("Network timeout"))
            }
        val vm = createViewModel(testScheduler, customRepo = repo)
        vm.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.ERROR, vm.state.value.status)
        assertEquals("Error: Network timeout", vm.state.value.message)
      }

  @Test
  fun errorWithoutMessageShouldShowGeneric() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> = Result.failure(RuntimeException())
            }
        val vm = createViewModel(testScheduler, customRepo = repo)
        vm.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertEquals("Network or server error", vm.state.value.message)
      }

  @Test
  fun errorShouldEmitEffect() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> = Result.failure(RuntimeException("Connection failed"))
            }
        val vm = createViewModel(testScheduler, customRepo = repo)
        var effect: ScannerEffect? = null
        val job = launch { vm.effects.collect { effect = it } }

        vm.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertTrue(effect is ScannerEffect.Error)
        assertEquals("Error: Connection failed", (effect as ScannerEffect.Error).message)
        job.cancel()
      }

  // ========== Deduplication ==========

  @Test
  fun duplicateScanWithin2sShouldBeIgnored() =
      runTest(testDispatcher) {
        var currentTime = 1000L
        val vm = createViewModel(testScheduler, customClock = { currentTime })
        val qr = createValidQr("user-same")
        val decision = ScanDecision.Accepted(ticketId = "ticket-123")
        coEvery { repository.validateByPass(qr, eventId) } returns Result.success(decision)

        vm.onQrScanned(qr)
        advanceUntilIdle()

        currentTime += 1000L
        vm.onQrScanned(qr)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.validateByPass(qr, eventId) }
      }

  @Test
  fun differentUsersShouldNotBeDeduplicated() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> = Result.success(ScanDecision.Accepted(ticketId = "test"))
            }
        val vm = createViewModel(testScheduler, customRepo = repo)

        vm.onQrScanned(createValidQr("user-1"))
        advanceUntilIdle()

        vm.onQrScanned(createValidQr("user-2"))
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.ACCEPTED, vm.state.value.status)
      }

  @Test
  fun cleanupShouldRemoveExpiredEntries() =
      runTest(testDispatcher) {
        var currentTime = 1000L
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> = Result.success(ScanDecision.Accepted(ticketId = "test"))
            }
        val vm = createViewModel(testScheduler, customRepo = repo, customClock = { currentTime })
        val qr = createValidQr("user-cleanup")

        vm.onQrScanned(qr)
        advanceUntilIdle()

        currentTime += 2001L
        vm.cleanupRecentScans()

        vm.onQrScanned(qr)
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.ACCEPTED, vm.state.value.status)
      }

  @Test
  fun cleanupShouldNotRemoveRecentEntries() =
      runTest(testDispatcher) {
        var currentTime = 1000L
        val vm = createViewModel(testScheduler, customClock = { currentTime })
        val qr = createValidQr("user-recent")
        val decision = ScanDecision.Accepted(ticketId = "ticket-123")
        coEvery { repository.validateByPass(qr, eventId) } returns Result.success(decision)

        vm.onQrScanned(qr)
        advanceUntilIdle()

        currentTime += 1000L
        vm.cleanupRecentScans()

        vm.onQrScanned(qr)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.validateByPass(qr, eventId) }
      }

  // ========== Concurrency ==========

  @Test
  fun concurrentScansShouldBeSerializedByMutex() =
      runTest(testDispatcher) {
        var callCount = 0
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> {
                callCount++
                kotlinx.coroutines.delay(500)
                return Result.success(ScanDecision.Accepted(ticketId = "test"))
              }
            }

        val vm = createViewModel(testScheduler, customRepo = repo)
        val qr = createValidQr()

        vm.onQrScanned(qr)
        testScheduler.advanceTimeBy(100)
        vm.onQrScanned(qr)

        advanceUntilIdle()
        assertEquals(1, callCount)
      }

  // ========== Processing State ==========

  @Test
  fun processingStateShouldBeSetDuringValidation() =
      runTest(testDispatcher) {
        var wasProcessing = false
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> {
                kotlinx.coroutines.delay(100)
                return Result.success(ScanDecision.Accepted(ticketId = "test"))
              }
            }
        val vm = createViewModel(testScheduler, customRepo = repo)
        val job = launch { vm.state.collect { if (it.isProcessing) wasProcessing = true } }

        vm.onQrScanned(createValidQr())
        testScheduler.advanceTimeBy(50)
        advanceUntilIdle()

        assertTrue(wasProcessing)
        assertFalse(vm.state.value.isProcessing)
        job.cancel()
      }

  @Test
  fun isProcessingShouldBeFalseAfterCompletion() =
      runTest(testDispatcher) {
        val scenarios: List<Result<ScanDecision>> =
            listOf(
                Result.success(ScanDecision.Accepted(ticketId = "test")),
                Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.REVOKED)),
                Result.failure(RuntimeException("Error")))

        for (result in scenarios) {
          val repo =
              object : TicketScanRepository {
                override suspend fun validateByPass(
                    qrText: String,
                    eventId: String
                ): Result<ScanDecision> = result
              }
          val vm = createViewModel(testScheduler, customRepo = repo)
          vm.onQrScanned(createValidQr())
          advanceUntilIdle()

          assertFalse(vm.state.value.isProcessing)
        }
      }

  // ========== Whitespace & Trimming ==========

  @Test
  fun qrWithWhitespaceShouldBeTrimmed() =
      runTest(testDispatcher) {
        val vm = createViewModel(testScheduler)
        val qr = createValidQr("user-trim")
        val qrWithWhitespace = "  $qr  \n"
        val decision = ScanDecision.Accepted(ticketId = "ticket-123")
        coEvery { repository.validateByPass(qr, eventId) } returns Result.success(decision)

        vm.onQrScanned(qrWithWhitespace)
        advanceUntilIdle()

        coVerify { repository.validateByPass(qr, eventId) }
      }

  @Test
  fun emptyQrAfterTrimShouldBeRejected() =
      runTest(testDispatcher) {
        val vm = createViewModel(testScheduler)

        vm.onQrScanned("   ")
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.REJECTED, vm.state.value.status)
        assertEquals("Invalid QR format", vm.state.value.message)
      }

  @Test
  fun qrWithOnlyNewlinesShouldBeRejected() =
      runTest(testDispatcher) {
        val vm = createViewModel(testScheduler)

        vm.onQrScanned("\n\n\n")
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.REJECTED, vm.state.value.status)
      }

  @Test
  fun qrWithTabsShouldBeRejected() =
      runTest(testDispatcher) {
        val vm = createViewModel(testScheduler)

        vm.onQrScanned("\t\t\t")
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.REJECTED, vm.state.value.status)
      }

  // ========== Multiple Scans ==========

  @Test
  fun multipleAcceptedScansShouldUpdateStateEachTime() =
      runTest(testDispatcher) {
        var currentTime = 1000L
        val repo =
            object : TicketScanRepository {
              private var counter = 0

              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> {
                counter++
                return Result.success(ScanDecision.Accepted(ticketId = "ticket-$counter"))
              }
            }

        val vm = createViewModel(testScheduler, customRepo = repo, customClock = { currentTime })

        vm.onQrScanned(createValidQr("user-1"))
        advanceUntilIdle()
        assertEquals("ticket-1", vm.state.value.lastTicketId)

        currentTime += 3000L
        vm.cleanupRecentScans()

        vm.onQrScanned(createValidQr("user-2"))
        advanceUntilIdle()
        assertEquals("ticket-2", vm.state.value.lastTicketId)
      }

  // ========== Lifecycle ==========

  @Test
  fun cancelledScopeShouldNotProcessScan() =
      runTest(testDispatcher) {
        var callCount = 0
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> {
                callCount++
                return Result.success(ScanDecision.Accepted(ticketId = "test"))
              }
            }

        val testScope = TestScope(testScheduler)
        val vm =
            ScannerViewModel(
                eventId = eventId,
                repo = repo,
                enableAutoCleanup = false,
                stateResetDelayMs = Long.MAX_VALUE,
                coroutineScope = testScope)

        // Cancel scope before scanning
        testScope.cancel()
        advanceUntilIdle()

        vm.onQrScanned(createValidQr())
        advanceUntilIdle()

        // Should not have called the repository
        assertEquals(0, callCount)
      }

  @Test
  fun scopeCancelledDuringValidationShouldDiscardResult() =
      runTest(testDispatcher) {
        val testScope = TestScope(testScheduler)
        var validationStarted = false
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> {
                validationStarted = true
                kotlinx.coroutines.delay(1000)
                return Result.success(ScanDecision.Accepted(ticketId = "test"))
              }
            }

        val vm =
            ScannerViewModel(
                eventId = eventId,
                repo = repo,
                enableAutoCleanup = false,
                stateResetDelayMs = Long.MAX_VALUE,
                coroutineScope = testScope)

        vm.onQrScanned(createValidQr())
        testScheduler.advanceTimeBy(500)
        advanceUntilIdle()

        assertTrue(validationStarted)

        // Cancel during validation
        testScope.cancel()
        advanceUntilIdle()

        // State should remain processing or unchanged
        val status = vm.state.value.status
        assertTrue(status == ScannerUiState.Status.IDLE || status == ScannerUiState.Status.ACCEPTED)
      }

  // ========== Duplicate with First Scan Null ==========

  @Test
  fun duplicateCheckShouldWorkWhenFirstScanIsNull() =
      runTest(testDispatcher) {
        var currentTime = 1000L
        val vm = createViewModel(testScheduler, customClock = { currentTime })
        val qr = createValidQr("user-null-check")
        val decision = ScanDecision.Accepted(ticketId = "ticket-123")
        coEvery { repository.validateByPass(qr, eventId) } returns Result.success(decision)

        vm.onQrScanned(qr)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.validateByPass(qr, eventId) }
      }

  // ========== Edge Cases for Deduplication ==========

  @Test
  fun scanExactlyAt2sWindowBoundaryShouldNotBeDeduplicated() =
      runTest(testDispatcher) {
        var currentTime = 1000L
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  qrText: String,
                  eventId: String
              ): Result<ScanDecision> = Result.success(ScanDecision.Accepted(ticketId = "test"))
            }
        val vm = createViewModel(testScheduler, customRepo = repo, customClock = { currentTime })
        val qr = createValidQr("user-boundary")

        vm.onQrScanned(qr)
        advanceUntilIdle()

        currentTime += 2000L
        vm.onQrScanned(qr)
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.ACCEPTED, vm.state.value.status)
      }

  @Test
  fun scanJustBefore2sWindowShouldBeDeduplicated() =
      runTest(testDispatcher) {
        var currentTime = 1000L
        val vm = createViewModel(testScheduler, customClock = { currentTime })
        val qr = createValidQr("user-just-before")
        val decision = ScanDecision.Accepted(ticketId = "ticket-123")
        coEvery { repository.validateByPass(qr, eventId) } returns Result.success(decision)

        vm.onQrScanned(qr)
        advanceUntilIdle()

        currentTime += 1999L
        vm.onQrScanned(qr)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.validateByPass(qr, eventId) }
      }
}
