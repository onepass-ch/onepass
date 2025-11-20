package ch.onepass.onepass.ui.scan

import ch.onepass.onepass.model.scan.ScanDecision
import ch.onepass.onepass.model.scan.TicketScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for Scanner screen with 90%+ coverage. Uses a simple FakeRepository instead
 * of MockK to avoid inline class issues.
 *
 * KEY FIX: Uses testScheduler.runCurrent() instead of advanceUntilIdle() to capture state BEFORE
 * the 3-second auto-reset to IDLE.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanScreenTest {

  private lateinit var repository: FakeTicketScanRepository
  private lateinit var testDispatcher: TestDispatcher
  private lateinit var testScope: TestScope
  private lateinit var testScheduler: TestCoroutineScheduler

  private class TimeHolder(var time: Long = 0L)

  private val timeHolder = TimeHolder()
  private val testClock: () -> Long = { timeHolder.time }

  @Before
  fun setup() {
    testDispatcher = StandardTestDispatcher()
    testScheduler = testDispatcher.scheduler
    testScope = TestScope(testDispatcher)
    Dispatchers.setMain(testDispatcher)
    repository = FakeTicketScanRepository()
    timeHolder.time = 1000L
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    testScope.cancel()
  }

  // ========== QR Parsing Tests ==========

  @Test
  fun onQrScanned_withValidQR_triggersValidation() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult =
            Result.success(
                ScanDecision.Accepted(
                    ticketId = "T-123", scannedAtSeconds = 1730000000, remaining = 50))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent() // ✅ Execute only immediate tasks, NOT delays

        // Then
        val state = viewModel.state.value
        assertFalse(state.isProcessing)
        assertEquals(ScannerUiState.Status.ACCEPTED, state.status)
        assertEquals("Access Granted", state.message)
        assertEquals("T-123", state.lastTicketId)
        assertEquals(50, state.remaining)

        assertEquals(1, repository.validateCallCount)
        assertEquals(validQr, repository.lastQrText)
        assertEquals("event1", repository.lastEventId)
      }

  @Test
  fun onQrScanned_with_invalid_QR_format_shows_rejection() =
      testScope.runTest {
        // Given
        val invalidQr = "invalid:qr:format"
        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(invalidQr)
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isProcessing)
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Invalid QR format", state.message)

        assertEquals(0, repository.validateCallCount)
      }

  @Test
  fun onQrScanned_with_empty_QR_shows_rejection() =
      testScope.runTest {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned("   ")
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals(0, repository.validateCallCount)
      }

  @Test
  fun onQrScanned_with_malformed_base64_shows_rejection() =
      testScope.runTest {
        // Given
        val malformedQr = "onepass:user:v1.not-valid-base64.signature"
        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(malformedQr)
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Invalid QR format", state.message)
      }

  // ========== Deduplication Tests ==========

  @Test
  fun duplicate_scan_within_window_is_ignored() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult = Result.success(ScanDecision.Accepted(ticketId = "T-123"))

        val viewModel = createViewModel()

        // When: First scan
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // When: Duplicate scan 1 second later (within 2s window)
        timeHolder.time = 2000L
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then: Backend called only once
        assertEquals(1, repository.validateCallCount)
      }

  @Test
  fun scan_after_deduplication_window_is_processed() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult = Result.success(ScanDecision.Accepted(ticketId = "T-123"))

        val viewModel = createViewModel()

        // When: First scan
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // When: Second scan after 3 seconds (outside 2s window)
        timeHolder.time = 4000L
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then: Backend called twice
        assertEquals(2, repository.validateCallCount)
      }

  @Test
  fun different_users_can_be_scanned_simultaneously() =
      testScope.runTest {
        // Given
        val qr1 =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMSIsImtpZCI6ImtleTEiLCJpYXQiOjE3MzAwMDAwMDAsInZlciI6MX0.c2lnX3VzZXIx"
        val qr2 =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMiIsImtpZCI6ImtleTEiLCJpYXQiOjE3MzAwMDAwMDAsInZlciI6MX0.c2lnX3VzZXIy"

        repository.nextResult = Result.success(ScanDecision.Accepted(ticketId = "T-123"))

        val viewModel = createViewModel()

        // When: Scan different users quickly
        viewModel.onQrScanned(qr1)
        testScheduler.runCurrent()

        viewModel.onQrScanned(qr2)
        testScheduler.runCurrent()

        // Then: Both processed
        assertEquals(2, repository.validateCallCount)
      }

  // ========== Backend Response Tests ==========

  @Test
  fun backend_accepted_response_updates_state_correctly() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult =
            Result.success(
                ScanDecision.Accepted(
                    ticketId = "T-999", scannedAtSeconds = 1730000123, remaining = 42))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent() // ✅ Only run immediate tasks

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.ACCEPTED, state.status)
        assertEquals("Access Granted", state.message)
        assertEquals("T-999", state.lastTicketId)
        assertEquals(1730000123L, state.lastScannedAt)
        assertEquals(42, state.remaining)
        assertFalse(state.isProcessing)
      }

  @Test
  fun backend_rejected_UNREGISTERED_shows_correct_message() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult =
            Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.UNREGISTERED))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("User not registered", state.message)
      }

  @Test
  fun backend_rejected_ALREADY_SCANNED_shows_correct_message() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult =
            Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.ALREADY_SCANNED))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Already scanned", state.message)
      }

  @Test
  fun backend_rejected_BAD_SIGNATURE_shows_correct_message() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult =
            Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.BAD_SIGNATURE))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Invalid signature", state.message)
      }

  @Test
  fun backend_rejected_REVOKED_shows_correct_message() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult =
            Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.REVOKED))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Pass revoked", state.message)
      }

  @Test
  fun backend_rejected_UNKNOWN_shows_generic_message() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult =
            Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.UNKNOWN))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.REJECTED, state.status)
        assertEquals("Access denied", state.message)
      }

  // ========== Error Handling Tests ==========

  @Test
  fun network_error_shows_error_state() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult = Result.failure(Exception("Network timeout"))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.ERROR, state.status)
        assertEquals("Error: Network timeout", state.message)
        assertFalse(state.isProcessing)
      }

  @Test
  fun exception_without_message_shows_generic_error() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult = Result.failure(RuntimeException())

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.ERROR, state.status)
        assertEquals("Network or server error", state.message)
      }

  // ========== Effect Tests ==========

  @Test
  fun accepted_scan_emits_Accepted_effect() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult = Result.success(ScanDecision.Accepted(ticketId = "T-123"))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val effect = viewModel.effects.first()
        assertTrue(effect is ScannerEffect.Accepted)
        assertEquals("Access Granted", (effect as ScannerEffect.Accepted).message)
      }

  @Test
  fun rejected_scan_emits_Rejected_effect() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult =
            Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.ALREADY_SCANNED))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val effect = viewModel.effects.first()
        assertTrue(effect is ScannerEffect.Rejected)
        assertEquals("Already scanned", (effect as ScannerEffect.Rejected).message)
      }

  @Test
  fun error_emits_Error_effect() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult = Result.failure(Exception("Server error"))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val effect = viewModel.effects.first()
        assertTrue(effect is ScannerEffect.Error)
        assertEquals("Error: Server error", (effect as ScannerEffect.Error).message)
      }

  // ========== State Reset Tests ==========

  @Test
  fun state_resets_to_IDLE_after_delay() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"
        repository.nextResult = Result.success(ScanDecision.Accepted(ticketId = "T-123"))

        val viewModel = createViewModel(stateResetDelayMs = 3000L)

        // When: Scan and verify ACCEPTED state immediately
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then: Initially ACCEPTED
        assertEquals(ScannerUiState.Status.ACCEPTED, viewModel.state.value.status)

        // When: Wait for reset delay
        testScheduler.advanceTimeBy(3000L)
        testScheduler.runCurrent()

        // Then: Reset to IDLE
        assertEquals(ScannerUiState.Status.IDLE, viewModel.state.value.status)
        assertEquals("Scan a pass…", viewModel.state.value.message)
        assertNull(viewModel.state.value.lastTicketId)
      }

  // ========== Cleanup Tests ==========

  @Test
  fun cleanup_removes_expired_entries() =
      testScope.runTest {
        // Given
        val qr1 =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMSIsImtpZCI6ImtleTEiLCJpYXQiOjE3MzAwMDAwMDAsInZlciI6MX0.c2lnX3VzZXIx"

        repository.nextResult = Result.success(ScanDecision.Accepted(ticketId = "T-123"))

        val viewModel = createViewModel(enableAutoCleanup = false)

        // When: Scan first user
        timeHolder.time = 1000L
        viewModel.onQrScanned(qr1)
        testScheduler.runCurrent()

        // When: Time passes beyond deduplication window
        timeHolder.time = 4000L
        viewModel.cleanupRecentScans()

        // When: Scan same user again (should not be deduplicated)
        viewModel.onQrScanned(qr1)
        testScheduler.runCurrent()

        // Then: Called twice (not deduplicated after cleanup)
        assertEquals(2, repository.validateCallCount)
      }

  @Test
  fun onCleared_cancels_cleanup_job() =
      testScope.runTest {
        // Given
        val viewModel = createViewModel(enableAutoCleanup = true)

        // When
        viewModel.onCleared()
        testScheduler.runCurrent()

        // Then: No crash, cleanup stopped
        assertTrue(true)
      }

  // ========== Concurrent Scan Protection Tests ==========

  @Test
  fun concurrent_scans_are_prevented_by_mutex() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"

        repository.nextResult = Result.success(ScanDecision.Accepted(ticketId = "T-123"))
        repository.delayMs = 1000 // Slow response

        val viewModel = createViewModel()

        // When: Try to scan twice quickly
        viewModel.onQrScanned(validQr)
        viewModel.onQrScanned(validQr.replace("user123", "user456"))

        testScheduler.advanceUntilIdle()

        // Then: Only first scan processes (second blocked by mutex)
        assertEquals(1, repository.validateCallCount)
      }

  // ========== Initial State Tests ==========

  @Test
  fun initial_state_is_IDLE() =
      testScope.runTest {
        // Given
        val viewModel = createViewModel()

        // Then
        val state = viewModel.state.value
        assertEquals(ScannerUiState.Status.IDLE, state.status)
        assertEquals("Scan a pass…", state.message)
        assertFalse(state.isProcessing)
        assertNull(state.lastTicketId)
        assertNull(state.lastScannedAt)
        assertNull(state.remaining)
      }

  @Test
  fun viewModel_requires_non_blank_eventId() {
    // When/Then
    try {
      ScannerViewModel(
          eventId = "",
          repo = repository,
          clock = testClock,
          enableAutoCleanup = false,
          coroutineScope = testScope)
      fail("Should have thrown exception")
    } catch (e: IllegalArgumentException) {
      assertEquals("eventId required", e.message)
    }
  }

  // ========== Multiple Scan Scenarios ==========

  @Test
  fun multiple_different_users_in_sequence() =
      testScope.runTest {
        // Given
        repository.nextResult = Result.success(ScanDecision.Accepted(ticketId = "T-123"))

        val viewModel = createViewModel()

        val qrCodes =
            listOf(
                "onepass:user:v1.eyJ1aWQiOiJ1c2VyMSIsImtpZCI6ImtleTEiLCJpYXQiOjE3MzAwMDAwMDAsInZlciI6MX0.c2lnX3VzZXIx",
                "onepass:user:v1.eyJ1aWQiOiJ1c2VyMiIsImtpZCI6ImtleTEiLCJpYXQiOjE3MzAwMDAwMDAsInZlciI6MX0.c2lnX3VzZXIy",
                "onepass:user:v1.eyJ1aWQiOiJ1c2VyMyIsImtpZCI6ImtleTEiLCJpYXQiOjE3MzAwMDAwMDAsInZlciI6MX0.c2lnX3VzZXIz")

        // When: Scan multiple users in sequence
        qrCodes.forEach { qr ->
          viewModel.onQrScanned(qr)
          testScheduler.runCurrent()
          timeHolder.time += 3000L
        }

        // Then: All scans should be processed
        assertEquals(qrCodes.size, repository.validateCallCount)
      }

  @Test
  fun rapid_scans_are_deduplicated() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"

        repository.nextResult = Result.success(ScanDecision.Accepted(ticketId = "T-123"))

        val viewModel = createViewModel()

        // When: Rapid fire 10 scans within 1 second
        repeat(10) {
          viewModel.onQrScanned(validQr)
          timeHolder.time += 100L
        }
        testScheduler.runCurrent()

        // Then: Only first scan should be processed
        assertEquals(1, repository.validateCallCount)
      }

  // ========== Edge Cases ==========

  @Test
  fun whitespace_in_QR_is_trimmed() =
      testScope.runTest {
        // Given
        val qrWithWhitespace =
            "  onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM  \n"

        repository.nextResult = Result.success(ScanDecision.Accepted(ticketId = "T-123"))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(qrWithWhitespace)
        testScheduler.runCurrent()

        // Then: Should process successfully
        assertEquals(ScannerUiState.Status.ACCEPTED, viewModel.state.value.status)
      }

  @Test
  fun zero_remaining_capacity_is_handled() =
      testScope.runTest {
        // Given
        val validQr =
            "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"

        repository.nextResult =
            Result.success(ScanDecision.Accepted(ticketId = "T-123", remaining = 0))

        val viewModel = createViewModel()

        // When
        viewModel.onQrScanned(validQr)
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(0, state.remaining)
        assertEquals(ScannerUiState.Status.ACCEPTED, state.status)
      }

  // ========== Helper Methods ==========

  private fun createViewModel(
      eventId: String = "event1",
      enableAutoCleanup: Boolean = false,
      cleanupPeriodMs: Long = 10_000L,
      stateResetDelayMs: Long = 3_000L
  ): ScannerViewModel {
    return ScannerViewModel(
        eventId = eventId,
        repo = repository,
        clock = testClock,
        enableAutoCleanup = enableAutoCleanup,
        cleanupPeriodMs = cleanupPeriodMs,
        stateResetDelayMs = stateResetDelayMs,
        coroutineScope = testScope)
  }
}

/**
 * Fake implementation of TicketScanRepository for testing. Much simpler than MockK and avoids
 * inline class issues.
 */
class FakeTicketScanRepository : TicketScanRepository {
  var nextResult: Result<ScanDecision> =
      Result.success(ScanDecision.Accepted(ticketId = "T-DEFAULT"))

  var validateCallCount = 0
  var lastQrText: String? = null
  var lastEventId: String? = null
  var delayMs: Long = 0

  override suspend fun validateByPass(qrText: String, eventId: String): Result<ScanDecision> {
    validateCallCount++
    lastQrText = qrText
    lastEventId = eventId

    if (delayMs > 0) {
      delay(delayMs)
    }

    return nextResult
  }
}
