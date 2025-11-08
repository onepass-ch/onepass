package ch.onepass.onepass.ui.scan

import ch.onepass.onepass.model.scan.ScanDecision
import ch.onepass.onepass.model.scan.TicketScanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    private lateinit var viewModel: ScannerViewModel
    private lateinit var fakeRepo: FakeTicketScanRepository
    private lateinit var fakeClock: FakeClock

    private val testEventId = "test-event-123"
    private val validQr = "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5LTAwMSIsImlhdCI6MTcwMDAwMDAwMCwidmVyIjoxfQ.A_-0"

    @Before
    fun setUp() {
        fakeClock = FakeClock()
        fakeRepo = FakeTicketScanRepository()
        viewModel = ScannerViewModel(
            eventId = testEventId,
            repo = fakeRepo,
            clock = { fakeClock.now() }
        )
    }

    @Test
    fun initialStateIsIdleWithDefaultMessage() = runTest {
        val state = viewModel.state.value

        assertFalse(state.isProcessing)
        assertEquals("Scan a pass…", state.message)
        assertNull(state.lastTicketId)
        assertNull(state.lastScannedAt)
        assertNull(state.remaining)
        assertEquals(ScannerUiState.Status.IDLE, state.status)
    }

    @Test
    fun onQrScannedWithValidQrAndAcceptedResponseUpdatesStateCorrectly() = runTest {
        fakeRepo.setResponse(
            ScanDecision.Accepted(
                ticketId = "ticket-789",
                scannedAtSeconds = 1700000000L,
                remaining = 150
            )
        )

        viewModel.onQrScanned(validQr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isProcessing)
        assertEquals(ScannerUiState.Status.ACCEPTED, state.status)
        assertEquals("Access Granted", state.message)
        assertEquals("ticket-789", state.lastTicketId)
        assertEquals(1700000000L, state.lastScannedAt)
        assertEquals(150, state.remaining)
    }

    @Test
    fun onQrScannedWithUnregisteredRejectionShowsCorrectMessage() = runTest {
        fakeRepo.setResponse(
            ScanDecision.Rejected(reason = ScanDecision.Reason.UNREGISTERED)
        )

        viewModel.onQrScanned(validQr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("User not registered", state.message)
    }

    @Test
    fun onQrScannedWithAlreadyScannedRejectionShowsCorrectMessage() = runTest {
        fakeRepo.setResponse(
            ScanDecision.Rejected(reason = ScanDecision.Reason.ALREADY_SCANNED)
        )

        viewModel.onQrScanned(validQr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Already scanned", state.message)
    }

    @Test
    fun onQrScannedWithBadSignatureRejectionShowsCorrectMessage() = runTest {
        fakeRepo.setResponse(
            ScanDecision.Rejected(reason = ScanDecision.Reason.BAD_SIGNATURE)
        )

        viewModel.onQrScanned(validQr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Invalid signature", state.message)
    }

    @Test
    fun onQrScannedWithRevokedRejectionShowsCorrectMessage() = runTest {
        fakeRepo.setResponse(
            ScanDecision.Rejected(reason = ScanDecision.Reason.REVOKED)
        )

        viewModel.onQrScanned(validQr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Pass revoked", state.message)
    }

    @Test
    fun onQrScannedWithUnknownRejectionShowsGenericMessage() = runTest {
        fakeRepo.setResponse(
            ScanDecision.Rejected(reason = ScanDecision.Reason.UNKNOWN)
        )

        viewModel.onQrScanned(validQr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Rejected", state.message)
    }

    @Test
    fun onQrScannedWithInvalidQrFormatShowsError() = runTest {
        val invalidQr = "not-a-valid-qr"

        viewModel.onQrScanned(invalidQr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Invalid QR format", state.message)
    }

    @Test
    fun onQrScannedWithNetworkErrorShowsErrorMessage() = runTest {
        fakeRepo.setError(Exception("Network timeout"))

        viewModel.onQrScanned(validQr)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.ERROR, state.status)
        assertEquals("Network or server error", state.message)
    }

    @Test
    fun duplicateScanWithinTwoSecondsIsIgnored() = runTest {
        fakeRepo.setResponse(ScanDecision.Accepted(ticketId = "ticket-001"))

        viewModel.onQrScanned(validQr)
        advanceUntilIdle()
        assertEquals(1, fakeRepo.callCount)

        fakeClock.advance(1000)
        viewModel.onQrScanned(validQr)
        advanceUntilIdle()
        assertEquals(1, fakeRepo.callCount)
    }

    @Test
    fun scanAfterTwoSecondsIsProcessed() = runTest {
        fakeRepo.setResponse(ScanDecision.Accepted(ticketId = "ticket-001"))

        viewModel.onQrScanned(validQr)
        advanceUntilIdle()
        assertEquals(1, fakeRepo.callCount)

        fakeClock.advance(2100)
        viewModel.onQrScanned(validQr)
        advanceUntilIdle()
        assertEquals(2, fakeRepo.callCount)
    }

    @Test
    fun differentUsersAreNotDeduplicated() = runTest {
        val qr1 = "onepass:user:v1.eyJ1aWQiOiJ1c2VyMSIsImtpZCI6ImtleS0wMDEiLCJpYXQiOjE3MDAwMDAwMDAsInZlciI6MX0.A_-0"
        val qr2 = "onepass:user:v1.eyJ1aWQiOiJ1c2VyMiIsImtpZCI6ImtleS0wMDEiLCJpYXQiOjE3MDAwMDAwMDAsInZlciI6MX0.B_-1"

        fakeRepo.setResponse(ScanDecision.Accepted(ticketId = "ticket-001"))

        viewModel.onQrScanned(qr1)
        advanceUntilIdle()
        assertEquals(1, fakeRepo.callCount)

        viewModel.onQrScanned(qr2)
        advanceUntilIdle()
        assertEquals(2, fakeRepo.callCount)
    }

    @Test
    fun oldScanEntriesAreCleanedUpAfterTenSeconds() = runTest {
        fakeRepo.setResponse(ScanDecision.Accepted(ticketId = "ticket-001"))

        viewModel.onQrScanned(validQr)
        advanceUntilIdle()
        assertEquals(1, fakeRepo.callCount)

        fakeClock.advance(1000)
        viewModel.onQrScanned(validQr)
        advanceUntilIdle()
        assertEquals(1, fakeRepo.callCount)

        advanceTimeBy(10000)

        fakeClock.advance(10000)
        viewModel.onQrScanned(validQr)
        advanceUntilIdle()
        assertEquals(2, fakeRepo.callCount)
    }

    @Test
    fun emptyQrStringIsRejected() = runTest {
        viewModel.onQrScanned("")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
    }

    @Test
    fun whitespaceOnlyQrIsRejected() = runTest {
        viewModel.onQrScanned("   ")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
    }

    @Test
    fun qrWithLeadingAndTrailingWhitespaceIsTrimmed() = runTest {
        fakeRepo.setResponse(ScanDecision.Accepted(ticketId = "ticket-001"))

        viewModel.onQrScanned("  $validQr  ")
        advanceUntilIdle()

        assertEquals(1, fakeRepo.callCount)
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.ACCEPTED, state.status)
    }

    @Test
    fun multipleDifferentUsersCanBeScannedInSequence() = runTest {
        val qr1 = "onepass:user:v1.eyJ1aWQiOiJ1c2VyMSIsImtpZCI6ImtleS0wMDEiLCJpYXQiOjE3MDAwMDAwMDAsInZlciI6MX0.A_-0"
        val qr2 = "onepass:user:v1.eyJ1aWQiOiJ1c2VyMiIsImtpZCI6ImtleS0wMDEiLCJpYXQiOjE3MDAwMDAwMDAsInZlciI6MX0.B_-1"
        val qr3 = "onepass:user:v1.eyJ1aWQiOiJ1c2VyMyIsImtpZCI6ImtleS0wMDEiLCJpYXQiOjE3MDAwMDAwMDAsInZlciI6MX0.C_-2"

        fakeRepo.setResponse(ScanDecision.Accepted(ticketId = "ticket-001"))
        viewModel.onQrScanned(qr1)
        advanceUntilIdle()

        fakeRepo.setResponse(ScanDecision.Accepted(ticketId = "ticket-002"))
        viewModel.onQrScanned(qr2)
        advanceUntilIdle()

        fakeRepo.setResponse(ScanDecision.Rejected(reason = ScanDecision.Reason.UNREGISTERED))
        viewModel.onQrScanned(qr3)
        advanceUntilIdle()

        assertEquals(3, fakeRepo.callCount)
    }
}

class FakeTicketScanRepository : TicketScanRepository {

    private var response: ScanDecision? = null
    private var error: Exception? = null
    private var delayMs: Long = 0

    var callCount = 0
        private set

    fun setResponse(decision: ScanDecision) {
        response = decision
        error = null
    }

    fun setError(exception: Exception) {
        error = exception
        response = null
    }

    fun setDelay(ms: Long) {
        delayMs = ms
    }

    override suspend fun validateByPass(qrText: String, eventId: String): Result<ScanDecision> {
        callCount++

        if (delayMs > 0) {
            delay(delayMs)
        }

        return when {
            error != null -> Result.failure(error!!)
            response != null -> Result.success(response!!)
            else -> Result.failure(Exception("No response configured"))
        }
    }
}

class FakeClock {
    private var currentTime = 0L

    fun now(): Long = currentTime

    fun advance(ms: Long) {
        currentTime += ms
    }
}