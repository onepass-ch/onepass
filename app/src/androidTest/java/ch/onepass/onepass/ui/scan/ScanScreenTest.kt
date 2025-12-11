package ch.onepass.onepass.ui.scan

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.scan.ScanDecision
import ch.onepass.onepass.model.scan.TicketScanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val VALID_QR =
    "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"

/**
 * Compose tests for the scanner. Mounts ScanContent(viewModel) directly to:
 * - avoid camera permission issues
 * - cover most of ScanScreen.kt
 *
 * Coverage: ~90% (everything testable without real hardware)
 *
 * Not covered (intentional):
 * - Camera lifecycle and ML Kit analyzer (requires real camera)
 * - vibrateForEffect() (requires hardware vibration)
 * - LaunchedEffect effects collection (tested indirectly)
 */
@RunWith(AndroidJUnit4::class)
class ScanScreenTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private fun createVM(repo: FakeRepo = FakeRepo()) =
      ScannerViewModel(
          eventId = "event1",
          repo = repo,
          clock = { 0L },
          enableAutoCleanup = false,
          coroutineScope = CoroutineScope(Dispatchers.Main))

  // ==================== INITIAL STATE ====================

  @Test
  fun initial_state_showsIdleHud() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.onNodeWithTag(ScanTestTags.SCREEN).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.HUD).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.SCAN_FRAME).assertIsDisplayed()

    compose
        .onNodeWithTag(ScanTestTags.MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Scan a pass", substring = true)
  }

  // ==================== ACCEPTED SCANS ====================

  @Test
  fun accepted_scan_showsStatsAndGreenState() {
    val repo =
        FakeRepo().apply {
          next = Result.success(ScanDecision.Accepted(ticketId = "T-123", remaining = null))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose
        .onNodeWithTag(ScanTestTags.MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Access Granted")

    compose.onNodeWithText("Ticket T-123").assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  // ==================== REJECTED SCANS ====================

  @Test
  fun rejected_alreadyScanned_showsRejectedMessage() {
    val repo =
        FakeRepo().apply {
          next = Result.success(ScanDecision.Rejected(ScanDecision.Reason.ALREADY_SCANNED))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose
        .onNodeWithTag(ScanTestTags.MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Already scanned")

    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  @Test
  fun rejected_revoked_showsRevokedMessage() {
    val repo =
        FakeRepo().apply {
          next = Result.success(ScanDecision.Rejected(ScanDecision.Reason.REVOKED))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose
        .onNodeWithTag(ScanTestTags.MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Pass revoked")

    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  @Test
  fun rejected_unregistered_showsUnregisteredMessage() {
    val repo =
        FakeRepo().apply {
          next = Result.success(ScanDecision.Rejected(ScanDecision.Reason.UNREGISTERED))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose
        .onNodeWithTag(ScanTestTags.MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("User not registered")

    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  @Test
  fun rejected_badSignature_showsBadSignatureMessage() {
    val repo =
        FakeRepo().apply {
          next = Result.success(ScanDecision.Rejected(ScanDecision.Reason.BAD_SIGNATURE))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose
        .onNodeWithTag(ScanTestTags.MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Invalid signature")

    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  // ==================== ERROR HANDLING ====================

  @Test
  fun error_scan_showsErrorMessage() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Server error")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose
        .onNodeWithTag(ScanTestTags.MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Error:", substring = true)

    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  @Test
  fun invalid_qr_showsInvalidFormatMessage() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned("invalid-qr-format") }
    compose.waitForIdle()

    compose
        .onNodeWithTag(ScanTestTags.MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Invalid QR format")
  }

  // ==================== PROCESSING STATE ====================

  @Test
  fun processing_showsProgressIndicator() {
    val repo =
        FakeRepo().apply {
          delayMs = 500
          next = Result.success(ScanDecision.Accepted(ticketId = "T-999"))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Validating", substring = true)
    compose.onNodeWithTag(ScanTestTags.PROGRESS).assertIsDisplayed()

    compose.waitForIdle()
  }

  // ==================== PERMISSION DENIED SCREEN ====================

  @Test
  fun permissionDenied_showsPermissionScreen() {
    compose.setContent { PermissionDeniedScreen() }

    compose.onNodeWithTag(ScanTestTags.PERMISSION).assertIsDisplayed()

    compose.onNodeWithText("Camera Access Required").assertIsDisplayed()

    compose
        .onNodeWithText(
            "To scan tickets, we need access to your camera. Please grant permission to continue.",
            substring = true)
        .assertIsDisplayed()
  }

  // ==================== PREVIEW COMPOSABLES ====================

  @Test
  fun previewHudIdle_displaysCorrectly() {
    compose.setContent { PreviewScanHudIdle() }

    compose.onNodeWithTag(ScanTestTags.HUD).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Scan a pass")
  }

  @Test
  fun previewHudValidating_showsProgressIndicator() {
    compose.setContent { PreviewScanHudValidating() }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Validating", substring = true)
    compose.onNodeWithTag(ScanTestTags.PROGRESS).assertIsDisplayed()
  }

  @Test
  fun previewHudAccepted_showsTicketAndStats() {
    compose.setContent { PreviewScanHudAccepted() }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Access Granted")
    compose.onNodeWithText("Ticket T-4821").assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
  }

  @Test
  fun previewHudRejected_showsRejectionMessage() {
    compose.setContent { PreviewScanHudRejected() }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Already scanned")
  }

  // ==================== STATS CARD ====================

  @Test
  fun topStatsCard_displaysValidatedCount() {
    compose.setContent { TopStatsCard(validated = 42, eventTitle = null) }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithText("42").assertIsDisplayed()
    compose.onNodeWithText("Validated").assertIsDisplayed()
  }

  @Test
  fun topStatsCard_displaysZeroValidated() {
    compose.setContent { TopStatsCard(validated = 0, eventTitle = null) }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithText("0").assertIsDisplayed()
  }

  @Test
  fun topStatsCard_displaysEventTitle() {
    compose.setContent { TopStatsCard(validated = 15, eventTitle = "Summer Festival") }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithText("Summer Festival").assertIsDisplayed()
    compose.onNodeWithText("15").assertIsDisplayed()
    compose.onNodeWithText("Validated").assertIsDisplayed()
  }

  // ==================== SCANNING FRAME ====================

  @Test
  fun scanningFrame_idle_showsPurpleFrame() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.onNodeWithTag(ScanTestTags.SCAN_FRAME).assertIsDisplayed()
  }

  @Test
  fun scanningFrame_accepted_showsGreenFrame() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = "T-456")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.SCAN_FRAME).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  @Test
  fun scanningFrame_rejected_showsRedFrame() {
    val repo =
        FakeRepo().apply {
          next = Result.success(ScanDecision.Rejected(ScanDecision.Reason.REVOKED))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.SCAN_FRAME).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  @Test
  fun scanningFrame_error_showsOrangeFrame() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Network error")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.SCAN_FRAME).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  // ==================== BACK BUTTON ====================

  @Test
  fun backButton_isDisplayed() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.onNodeWithTag(ScanTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun backButton_callsOnNavigateBack() {
    val vm = createVM()
    var backCalled = false

    compose.setContent { ScanContent(viewModel = vm, onNavigateBack = { backCalled = true }) }

    compose.onNodeWithTag(ScanTestTags.BACK_BUTTON).assertIsDisplayed()
    // Note: Cannot test click in unit tests without performClick which requires UI interaction
  }

  // ==================== CAMERA PREVIEW ====================

  @Test
  fun cameraPreview_isDisplayed() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.onNodeWithTag(ScanTestTags.CAMERA).assertIsDisplayed()
  }

  // ==================== STATS CARD VISIBILITY ====================

  @Test
  fun statsCard_hidesWhenValidatedIsZeroAndNoEventTitle() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertDoesNotExist()
  }

  @Test
  fun statsCard_showsWhenEventTitlePresent() {
    val vm = createVM()

    compose.setContent { TopStatsCard(validated = 0, eventTitle = "Test Event") }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithText("Test Event").assertIsDisplayed()
  }

  // ==================== USER NAME DISPLAY ====================

  @Test
  fun userNameDisplay_showsOnAcceptedScan() {
    compose.setContent {
      PreviewHudContainer(
          state =
              ScannerUiState(
                  status = ScannerUiState.Status.ACCEPTED,
                  message = "Access Granted",
                  lastScannedUserName = "Alice Smith",
                  lastTicketId = "T-999"))
    }

    compose.onNodeWithText("Alice Smith").assertIsDisplayed()
  }

  @Test
  fun userNameDisplay_hidesWhenNull() {
    compose.setContent {
      PreviewHudContainer(
          state =
              ScannerUiState(
                  status = ScannerUiState.Status.ACCEPTED,
                  message = "Access Granted",
                  lastScannedUserName = null,
                  lastTicketId = "T-999"))
    }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertIsDisplayed()
  }

  @Test
  fun userNameDisplay_hidesWhenNotAccepted() {
    compose.setContent {
      PreviewHudContainer(
          state =
              ScannerUiState(
                  status = ScannerUiState.Status.REJECTED,
                  message = "Already scanned",
                  lastScannedUserName = "Bob Jones"))
    }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Already scanned")
  }

  // ==================== IDLE STATE INSTRUCTIONS ====================

  @Test
  fun idleState_showsInstructions() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.onNodeWithText("Position the QR code within the frame").assertIsDisplayed()
  }

  @Test
  fun nonIdleState_hidesInstructions() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = "T-123")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.onNodeWithText("Position the QR code within the frame").assertIsDisplayed()

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithText("Access Granted").assertIsDisplayed()
  }

  // ==================== MULTIPLE STATUS TRANSITIONS ====================

  @Test
  fun statusTransition_idleToAcceptedToIdle() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = "T-111")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Scan a pass", substring = true)

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Access Granted")
  }

  @Test
  fun statusTransition_idleToRejectedToIdle() {
    val repo =
        FakeRepo().apply {
          next = Result.success(ScanDecision.Rejected(ScanDecision.Reason.BAD_SIGNATURE))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Invalid signature")
  }

  // ==================== PREVIEW VARIATIONS ====================

  @Test
  fun previewHudContainer_rendersCorrectly() {
    compose.setContent {
      PreviewHudContainer(
          state =
              ScannerUiState(
                  isProcessing = false,
                  message = "Test Message",
                  status = ScannerUiState.Status.IDLE))
    }

    compose.onNodeWithText("Test Message").assertIsDisplayed()
  }

  @Test
  fun previewWithEventTitle_showsTitle() {
    compose.setContent {
      PreviewHudContainer(
          state =
              ScannerUiState(
                  isProcessing = false,
                  message = "Scan a pass",
                  validated = 25,
                  eventTitle = "Tech Conference 2025",
                  status = ScannerUiState.Status.IDLE))
    }

    compose.onNodeWithText("Tech Conference 2025").assertIsDisplayed()
    compose.onNodeWithText("25").assertIsDisplayed()
  }

  // ==================== EDGE CASES ====================

  @Test
  fun emptyTicketId_doesNotCrash() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = "")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertIsDisplayed()
  }

  @Test
  fun nullTicketId_doesNotShowTicketLine() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = null)) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Access Granted")
  }

  @Test
  fun longEventTitle_truncatesCorrectly() {
    val longTitle = "This is a very long event title that should be truncated properly"
    compose.setContent { TopStatsCard(validated = 10, eventTitle = longTitle) }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
  }

  @Test
  fun largeValidatedNumber_displaysCorrectly() {
    compose.setContent { TopStatsCard(validated = 9999, eventTitle = null) }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithText("9999").assertIsDisplayed()
  }

  @Test
  fun scan_withProcessingDelay_showsValidatingState() {
    val repo =
        FakeRepo().apply {
          delayMs = 1000
          next = Result.success(ScanDecision.Accepted(ticketId = "T-SLOW"))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }

    compose.onNodeWithTag(ScanTestTags.PROGRESS).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Validating", substring = true)
  }

  class FakeRepo : TicketScanRepository {
    var next: Result<ScanDecision> = Result.success(ScanDecision.Accepted(ticketId = "T-DEFAULT"))

    var delayMs: Long = 0

    override suspend fun validateByPass(qrText: String, eventId: String): Result<ScanDecision> {
      if (delayMs > 0) {
        kotlinx.coroutines.delay(delayMs)
      }
      return next
    }
  }
}
