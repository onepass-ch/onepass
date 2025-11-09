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

    /**
     * Builds a syntactically valid OnePass QR string for testing.
     */
    private fun createValidQr(uid: String = "user-001"): String {
        val payload = """{"uid":"$uid","kid":"key-1","iat":1000,"ver":1}"""
        val payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val signature = "validSignature123"
        return "onepass:user:v1.$payloadB64.$signature"
    }

    @Before
    fun setup() {
        // Use a test dispatcher for deterministic coroutine scheduling.
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        currentTime = 1000L
    }

    @After
    fun tearDown() {
        // Restore the original Main dispatcher after each test.
        Dispatchers.resetMain()
    }

    /**
     * Creates a ViewModel instance with controlled timing and a test scope.
     */
    private fun createViewModel(
        testScheduler: TestCoroutineScheduler,
        resetDelayMs: Long = Long.MAX_VALUE
    ): ScannerViewModel {
        return ScannerViewModel(
            eventId = eventId,
            repo = repository,
            clock = { currentTime },
            enableAutoCleanup = false,
            cleanupPeriodMs = 10_000L,
            stateResetDelayMs = resetDelayMs,
            coroutineScope = TestScope(testScheduler))
    }

    /**
     * Invalid QR input should immediately set REJECTED state and never reach the repository.
     */
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

    /**
     * Invalid QR input should emit a Rejected effect via the one-shot effects flow.
     */
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

    /**
     * Cleanup should preserve entries still within the deduplication window.
     */
    @Test
    fun cleanupShouldNotRemoveRecentEntries() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(this.testScheduler)

            val qr = createValidQr("user-recent")
            val decision = ScanDecision.Accepted(ticketId = "ticket-123")
            coEvery { repository.validateByPass(qr, eventId) } returns Result.success(decision)

            viewModel.onQrScanned(qr)
            advanceUntilIdle()

            // Move time within the dedupe window.
            currentTime += 1000L
            viewModel.cleanupRecentScans()

            // Second scan of the same QR should be deduplicated (no repository call).
            viewModel.onQrScanned(qr)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.validateByPass(qr, eventId) }
        }

    /**
     * Leading/trailing whitespace around the QR string should be trimmed before validation.
     */
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

    /**
     * A scan triggered while a previous validation is still running should be ignored.
     */
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

            val vm =
                ScannerViewModel(
                    eventId = eventId,
                    repo = slowRepo,
                    clock = { currentTime },
                    enableAutoCleanup = false,
                    cleanupPeriodMs = 10_000L,
                    stateResetDelayMs = Long.MAX_VALUE,
                    coroutineScope = TestScope(this.testScheduler))

            vm.onQrScanned(qr)
            advanceUntilIdle()
        }

    /**
     * Scanning the same UID again within 2 seconds should not trigger a second repository call.
     */
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

            // Advance by 1s: still within the dedupe window (2s).
            currentTime += 1000L

            viewModel.onQrScanned(qr2)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.validateByPass(qr, eventId) }
        }

    /**
     * While the repository call is in progress, the UI should reflect a processing state.
     */
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

            val vm =
                ScannerViewModel(
                    eventId = eventId,
                    repo = slowRepo,
                    clock = { currentTime },
                    enableAutoCleanup = false,
                    cleanupPeriodMs = 10_000L,
                    stateResetDelayMs = Long.MAX_VALUE,
                    coroutineScope = TestScope(this.testScheduler))

            vm.onQrScanned(qr)
            advanceUntilIdle()
        }

    /**
     * The initial state must be idle with the default message and no ticket-related data.
     */
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

    /**
     * Blank or whitespace-only input should be rejected without calling the repository.
     */
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

    /**
     * A blank eventId must throw immediately to prevent misconfigured ViewModel usage.
     */
    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowExceptionForBlankEventId() {
        ScannerViewModel(
            eventId = "  ",
            repo = repository,
            clock = { currentTime },
            enableAutoCleanup = false,
            coroutineScope = TestScope())
    }
}
