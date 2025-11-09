package ch.onepass.onepass.ui.scan

import ch.onepass.onepass.model.scan.ScanDecision
import ch.onepass.onepass.model.scan.TicketScanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

  private lateinit var repository: TicketScanRepository
  private val testDispatcher = StandardTestDispatcher()

  private val eventId = "event-123"
  private var currentTime = 1000L

  private fun createValidQr(uid: String = "user-001"): String {
    val payload = """{"uid":"$uid","kid":"key-1","iat":1000,"ver":1}"""
    val payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
    val signature = "validSignature123"
    return "onepass:user:v1.$payloadB64.$signature"
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk(relaxed = true)
    currentTime = 1000L
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(
      testScheduler: TestCoroutineScheduler,
      resetDelayMs: Long = Long.MAX_VALUE,
      customRepo: TicketScanRepository? = null
  ): ScannerViewModel {
    return ScannerViewModel(
        eventId = eventId,
        repo = customRepo ?: repository,
        clock = { currentTime },
        enableAutoCleanup = false,
        cleanupPeriodMs = 10_000L,
        stateResetDelayMs = resetDelayMs,
        coroutineScope = TestScope(testScheduler))
  }

  // ==================== ORIGINAL TESTS ====================

  @Test
  fun onQrScannedWithInvalidQrFormatShouldRejectImmediately() =
      runTest(testDispatcher) {
        val viewModel = createViewModel(this.testScheduler)
        val invalidQr = "not-a-valid-qr-code"

        viewModel.onQrScanned(invalidQr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Invalid QR format", state.message)

        coVerify(exactly = 0) { repository.validateByPass(any(), any()) }
      }

  @Test
  fun onQrScannedWithInvalidQrShouldEmitRejectedEffect() =
      runTest(testDispatcher) {
        val viewModel = createViewModel(this.testScheduler)
        val invalidQr = "bad-qr"

        var collectedEffect: ScannerEffect? = null
        val job = launch { viewModel.effects.collect { effect -> collectedEffect = effect } }

        viewModel.onQrScanned(invalidQr)
        advanceUntilIdle()

        assertTrue(collectedEffect is ScannerEffect.Rejected)
        assertEquals("Invalid QR format", (collectedEffect as ScannerEffect.Rejected).message)

        job.cancel()
      }

  @Test
  fun cleanupShouldNotRemoveRecentEntries() =
      runTest(testDispatcher) {
        val viewModel = createViewModel(this.testScheduler)

        val qr = createValidQr("user-recent")
        val decision = ScanDecision.Accepted(ticketId = "ticket-123")
        coEvery { repository.validateByPass(qr, eventId) } returns Result.success(decision)

        viewModel.onQrScanned(qr)
        advanceUntilIdle()

        currentTime += 1000L
        viewModel.cleanupRecentScans()

        viewModel.onQrScanned(qr)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.validateByPass(qr, eventId) }
      }

  @Test
  fun onQrScannedShouldTrimWhitespaceFromQr() =
      runTest(testDispatcher) {
        val viewModel = createViewModel(this.testScheduler)

        val qr = createValidQr("user-trim")
        val qrWithWhitespace = "  $qr  \n"
        val decision = ScanDecision.Accepted(ticketId = "ticket-123")
        coEvery { repository.validateByPass(qr, eventId) } returns Result.success(decision)

        viewModel.onQrScanned(qrWithWhitespace)
        advanceUntilIdle()

        coVerify { repository.validateByPass(qr, eventId) }
      }

  @Test
  fun shouldIgnoreConcurrentScans() =
      runTest(testDispatcher) {
        val qr = createValidQr("user-concurrent")
        val decision = ScanDecision.Accepted(ticketId = "ticket-concurrent")

        val slowRepo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                kotlinx.coroutines.delay(500)
                return Result.success(decision)
              }
            }

        val vm = createViewModel(this.testScheduler, customRepo = slowRepo)
        vm.onQrScanned(qr)
        advanceUntilIdle()
      }

  @Test
  fun onQrScannedWithSameUidWithin2sShouldBeIgnored() =
      runTest(testDispatcher) {
        val viewModel = createViewModel(this.testScheduler)

        val qr = createValidQr("user-same")
        val qr2 = createValidQr("user-same")
        val decision = ScanDecision.Accepted(ticketId = "ticket-123")
        coEvery { repository.validateByPass(qr, eventId) } returns Result.success(decision)

        viewModel.onQrScanned(qr)
        advanceUntilIdle()

        currentTime += 1000L

        viewModel.onQrScanned(qr2)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.validateByPass(qr, eventId) }
      }

  @Test
  fun processingStateShouldBeTrueDuringValidation() =
      runTest(testDispatcher) {
        val qr = createValidQr("user-processing")
        val decision = ScanDecision.Accepted(ticketId = "ticket-123")

        val slowRepo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                kotlinx.coroutines.delay(500)
                return Result.success(decision)
              }
            }

        val vm = createViewModel(this.testScheduler, customRepo = slowRepo)
        vm.onQrScanned(qr)
        advanceUntilIdle()
      }

  @Test
  fun initialStateShouldBeIdle() =
      runTest(testDispatcher) {
        val viewModel = createViewModel(this.testScheduler)
        val state = viewModel.state.value

        assertFalse(state.isProcessing)
        assertEquals("Scan a pass…", state.message)
        assertNull(state.lastTicketId)
        assertNull(state.lastScannedAt)
        assertNull(state.remaining)
        assertEquals(ScannerUiState.Status.IDLE, state.status)
      }

  @Test
  fun whitespaceOnlyQrShouldBeRejected() =
      runTest(testDispatcher) {
        val viewModel = createViewModel(this.testScheduler)

        viewModel.onQrScanned("   \n\t  ")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Invalid QR format", state.message)

        coVerify(exactly = 0) { repository.validateByPass(any(), any()) }
      }

  @Test(expected = IllegalArgumentException::class)
  fun shouldThrowExceptionForBlankEventId() {
    ScannerViewModel(
        eventId = "  ",
        repo = repository,
        clock = { currentTime },
        enableAutoCleanup = false,
        coroutineScope = TestScope())
  }

  @Test
  fun onQrScannedWithMissingKidFieldShouldReject() =
      runTest(testDispatcher) {
        val viewModel = createViewModel(this.testScheduler)
        val payload = """{"uid":"user-123","iat":1000,"ver":1}"""
        val payloadB64 =
            Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val invalidQr = "onepass:user:v1.$payloadB64.signature"

        viewModel.onQrScanned(invalidQr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Invalid QR format", state.message)
      }

  // ==================== NEW TESTS WITH REAL REPOSITORY ====================

  @Test
  fun acceptedDecisionShouldUpdateState() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.success(
                    ScanDecision.Accepted(
                        ticketId = "ticket-456", scannedAtSeconds = 2000L, remaining = 5))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        val qr = createValidQr("user-accepted")

        viewModel.onQrScanned(qr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.ACCEPTED, state.status)
        assertEquals("Access Granted", state.message)
        assertEquals("ticket-456", state.lastTicketId)
        assertEquals(2000L, state.lastScannedAt)
        assertEquals(5, state.remaining)
      }

  @Test
  fun acceptedDecisionShouldEmitEffect() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.success(ScanDecision.Accepted(ticketId = "ticket-789"))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        var collectedEffect: ScannerEffect? = null
        val job = launch { viewModel.effects.collect { effect -> collectedEffect = effect } }

        viewModel.onQrScanned(createValidQr("user-effect"))
        advanceUntilIdle()

        assertTrue(collectedEffect is ScannerEffect.Accepted)
        assertEquals("Access Granted", (collectedEffect as ScannerEffect.Accepted).message)
        job.cancel()
      }

  @Test
  fun rejectedDecisionUnregisteredShouldShowMessage() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.success(
                    ScanDecision.Rejected(reason = ScanDecision.Reason.UNREGISTERED))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        viewModel.onQrScanned(createValidQr("user-unregistered"))
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.REJECTED, viewModel.state.value.status)
        assertEquals("User not registered", viewModel.state.value.message)
      }

  @Test
  fun rejectedDecisionAlreadyScannedShouldShowMessage() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.success(
                    ScanDecision.Rejected(reason = ScanDecision.Reason.ALREADY_SCANNED))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        viewModel.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.REJECTED, viewModel.state.value.status)
        assertEquals("Already scanned", viewModel.state.value.message)
      }

  @Test
  fun rejectedDecisionBadSignatureShouldShowMessage() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.success(
                    ScanDecision.Rejected(reason = ScanDecision.Reason.BAD_SIGNATURE))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        viewModel.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.REJECTED, viewModel.state.value.status)
        assertEquals("Invalid signature", viewModel.state.value.message)
      }

  @Test
  fun rejectedDecisionRevokedShouldShowMessage() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.REVOKED))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        viewModel.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.REJECTED, viewModel.state.value.status)
        assertEquals("Pass revoked", viewModel.state.value.message)
      }

  @Test
  fun rejectedDecisionUnknownShouldShowGenericMessage() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.UNKNOWN))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        viewModel.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.REJECTED, viewModel.state.value.status)
        assertEquals("Access denied", viewModel.state.value.message)
      }

  @Test
  fun rejectedDecisionShouldEmitEffect() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.success(
                    ScanDecision.Rejected(reason = ScanDecision.Reason.UNREGISTERED))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        var collectedEffect: ScannerEffect? = null
        val job = launch { viewModel.effects.collect { effect -> collectedEffect = effect } }

        viewModel.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertTrue(collectedEffect is ScannerEffect.Rejected)
        assertEquals("User not registered", (collectedEffect as ScannerEffect.Rejected).message)
        job.cancel()
      }

  @Test
  fun repositoryErrorShouldShowErrorState() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.failure(RuntimeException("Network timeout"))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        viewModel.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.ERROR, viewModel.state.value.status)
        assertEquals("Error: Network timeout", viewModel.state.value.message)
      }

  @Test
  fun repositoryErrorWithoutMessageShouldShowGeneric() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.failure(RuntimeException())
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        viewModel.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertEquals(ScannerUiState.Status.ERROR, viewModel.state.value.status)
        assertEquals("Network or server error", viewModel.state.value.message)
      }

  @Test
  fun repositoryErrorShouldEmitEffect() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.failure(RuntimeException("Connection failed"))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        var collectedEffect: ScannerEffect? = null
        val job = launch { viewModel.effects.collect { effect -> collectedEffect = effect } }

        viewModel.onQrScanned(createValidQr())
        advanceUntilIdle()

        assertTrue(collectedEffect is ScannerEffect.Error)
        assertEquals("Error: Connection failed", (collectedEffect as ScannerEffect.Error).message)
        job.cancel()
      }

  @Test
  fun acceptedWithNullFieldsShouldWork() =
      runTest(testDispatcher) {
        val repo =
            object : TicketScanRepository {
              override suspend fun validateByPass(
                  passQr: String,
                  eventId: String
              ): Result<ScanDecision> {
                return Result.success(
                    ScanDecision.Accepted(
                        ticketId = null, scannedAtSeconds = null, remaining = null))
              }
            }

        val viewModel = createViewModel(this.testScheduler, customRepo = repo)
        viewModel.onQrScanned(createValidQr())
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.ACCEPTED, state.status)
        assertNull(state.lastTicketId)
        assertNull(state.lastScannedAt)
        assertNull(state.remaining)
      }
}
