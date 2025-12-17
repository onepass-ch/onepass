package ch.onepass.onepass.ui.scan

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.R
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
 * Comprehensive Compose tests for the scanner. Intentionally not covered (requires
 * hardware/system):
 * - Camera lifecycle and ML Kit analyzer
 * - vibrateForEffect() and MediaPlayer audio feedback
 * - Error handlers inside try-catch (system failures)
 */
@RunWith(AndroidJUnit4::class)
class ScanScreenTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  private fun createVM(repo: FakeRepo = FakeRepo()) =
      ScannerViewModel(
          eventId = "event1",
          repo = repo,
          clock = { 0L },
          enableAutoCleanup = false,
          coroutineScope = CoroutineScope(Dispatchers.Main))

  // ==================== INITIAL STATE ====================

  @Test
  fun initialState_showsAllElements() {
    compose.setContent { ScanContent(viewModel = createVM()) }

    compose.onNodeWithTag(ScanTestTags.SCREEN).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.HUD).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.SCAN_FRAME).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.CAMERA).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.BACK_BUTTON).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Scan a pass", substring = true)
    compose.onNodeWithText(context.getString(R.string.scan_instruction)).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertDoesNotExist()
  }

  @Test
  fun initialState_noStatusIconVisible() {
    compose.setContent { ScanContent(viewModel = createVM()) }

    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertDoesNotExist()
  }

  @Test
  fun initialState_noProgressIndicatorVisible() {
    compose.setContent { ScanContent(viewModel = createVM()) }

    compose.onNodeWithTag(ScanTestTags.PROGRESS).assertDoesNotExist()
  }

  // ==================== ACCEPTED SCANS ====================

  @Test
  fun acceptedScan_nullTicketId_doesNotCrash() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = null)) }
    val vm = createVM(repo)
    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }

    compose.waitUntil(timeoutMillis = 2000) {
      runCatching {
            compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Access Granted")
          }
          .isSuccess
    }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Access Granted")
  }

  @Test
  fun acceptedScan_emptyTicketId_doesNotCrash() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = "")) }
    val vm = createVM(repo)
    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertIsDisplayed()
  }

  // ==================== REJECTED SCANS ====================

  @Test
  fun rejectedScan_alreadyScanned_showsRejectedMessage() {
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
  fun rejectedScan_revoked_showsRevokedMessage() {
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
  fun rejectedScan_unregistered_showsUnregisteredMessage() {
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
  fun rejectedScan_badSignature_showsBadSignatureMessage() {
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

  // ==================== ERROR HANDLING & DIALOGS ====================

  @Test
  fun error_networkError_showsNetworkErrorDialog() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Network connection failed")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()
    compose.onNodeWithText(context.getString(R.string.scan_network_error_title)).assertIsDisplayed()
    compose.onNodeWithText(context.getString(R.string.scan_retry_button)).assertIsDisplayed()
    compose.onNodeWithText(context.getString(R.string.scan_back_button)).assertIsDisplayed()
  }

  @Test
  fun error_internetKeyword_showsNetworkErrorDialog() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("internet not available")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()
    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()
  }

  @Test
  fun error_timeoutKeyword_showsNetworkErrorDialog() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Request timeout")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()
  }

  @Test
  fun error_connectionKeyword_showsNetworkErrorDialog() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Connection lost")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()
  }

  @Test
  fun error_sessionExpired_showsSessionExpiredDialog() {
    val repo =
        FakeRepo().apply { next = Result.failure(Exception("Session expired, please login")) }
    val vm = createVM(repo)
    var backNavigated = false

    compose.setContent { ScanContent(viewModel = vm, onNavigateBack = { backNavigated = true }) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag("scan_session_expired_dialog").assertIsDisplayed()
    compose
        .onNodeWithText(context.getString(R.string.scan_session_expired_title))
        .assertIsDisplayed()
    compose
        .onNodeWithText(context.getString(R.string.scan_session_expired_message), substring = true)
        .assertIsDisplayed()

    compose.onNodeWithText(context.getString(R.string.scan_ok_button)).performClick()
    compose.waitForIdle()

    assert(backNavigated) { "Should have navigated back after session expired" }
  }

  @Test
  fun error_pleaseLoginKeyword_showsSessionExpiredDialog() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Please login again")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag("scan_session_expired_dialog").assertIsDisplayed()
  }

  @Test
  fun error_mixedCaseKeywords_showsCorrectDialog() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("SESSION EXPIRED")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag("scan_session_expired_dialog").assertIsDisplayed()
  }

  @Test
  fun genericError_showsInHudOnly() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Database error")) }
    val vm = createVM(repo)
    compose.setContent { ScanContent(viewModel = vm, onNavigateBack = {}) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()
    Thread.sleep(300)

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertDoesNotExist()
    compose.onNodeWithTag("scan_session_expired_dialog").assertDoesNotExist()
    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Error:", substring = true)
    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  // ==================== INVALID QR CODES ====================

  @Test
  fun invalidQr_showsInvalidFormatMessage() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned("invalid-qr-format") }
    compose.waitForIdle()

    compose
        .onNodeWithTag(ScanTestTags.MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Invalid QR format")
  }

  @Test
  fun invalidQr_emptyString_showsInvalidFormat() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned("") }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Invalid", substring = true)
  }

  @Test
  fun invalidQr_partialValidFormat_showsInvalidFormat() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned("onepass:user:") }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Invalid", substring = true)
  }

  @Test
  fun invalidQr_veryLongString_showsInvalidFormat() {
    val vm = createVM()
    val veryLongQr = "x".repeat(1000)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(veryLongQr) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Invalid", substring = true)
  }

  @Test
  fun invalidQr_specialCharacters_showsInvalidFormat() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned("!@#$%^&*(){}[]|\\:;\"'<>?,./") }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Invalid", substring = true)
  }

  @Test
  fun invalidQr_whitespaceOnly_showsInvalidFormat() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned("   ") }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Invalid", substring = true)
  }

  @Test
  fun invalidQr_nullCharacters_showsInvalidFormat() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned("test\u0000data") }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Invalid", substring = true)
  }

  // ==================== DIALOG INTERACTIONS ====================

  @Test
  fun networkErrorDialog_retryButton_dismissesDialog() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Network timeout")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()

    repo.next = Result.success(ScanDecision.Accepted(ticketId = "T-RETRY"))
    compose.onNodeWithText(context.getString(R.string.scan_retry_button)).performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertDoesNotExist()
  }

  @Test
  fun networkErrorDialog_backButton_navigatesBack() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Internet connection lost")) }
    val vm = createVM(repo)
    var backNavigated = false

    compose.setContent { ScanContent(viewModel = vm, onNavigateBack = { backNavigated = true }) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()

    compose.onNodeWithText(context.getString(R.string.scan_back_button)).performClick()
    compose.waitForIdle()

    assert(backNavigated) { "Back button should navigate back" }
    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertDoesNotExist()
  }

  @Test
  fun dismissNetworkDialog_resetsToIdle() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Network timeout")) }
    val vm = createVM(repo)
    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithText(context.getString(R.string.scan_back_button)).performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Scan a pass", substring = true)
  }

  @Test
  fun sessionExpiredDialog_okButton_navigatesBack() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Session expired")) }
    val vm = createVM(repo)
    var backNavigated = false

    compose.setContent { ScanContent(viewModel = vm, onNavigateBack = { backNavigated = true }) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag("scan_session_expired_dialog").assertIsDisplayed()

    compose.onNodeWithText(context.getString(R.string.scan_ok_button)).performClick()
    compose.waitForIdle()

    assert(backNavigated) { "OK button should navigate back" }
  }

  // ==================== PROCESSING STATE ====================

  @Test
  fun processing_showsProgressIndicatorAndValidatingMessage() {
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

  @Test
  fun processingState_longDelay_showsValidatingMessage() {
    val repo =
        FakeRepo().apply {
          delayMs = 1000
          next = Result.success(ScanDecision.Accepted(ticketId = "T-SLOW"))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Validating", substring = true)
    compose.onNodeWithTag(ScanTestTags.PROGRESS).assertIsDisplayed()

    compose.waitForIdle()
  }

  @Test
  fun processingState_noStatusIconVisible() {
    val repo =
        FakeRepo().apply {
          delayMs = 500
          next = Result.success(ScanDecision.Accepted(ticketId = "T-999"))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    Thread.sleep(100)

    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertDoesNotExist()

    compose.waitForIdle()
  }

  // ==================== PERMISSION DENIED SCREEN ====================

  @Test
  fun permissionDenied_showsPermissionScreen() {
    compose.setContent { PermissionDeniedScreen() }

    compose.onNodeWithTag(ScanTestTags.PERMISSION).assertIsDisplayed()
    compose.onNodeWithText(context.getString(R.string.scan_permission_title)).assertIsDisplayed()
    compose
        .onNodeWithText(context.getString(R.string.scan_permission_message), substring = true)
        .assertIsDisplayed()
  }

  // ==================== STATS CARD ====================

  @Test
  fun topStatsCard_displaysValidatedCount() {
    compose.setContent { TopStatsCard(validated = 42, eventTitle = null) }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithText("42").assertIsDisplayed()
    compose.onNodeWithText(context.getString(R.string.scan_validated_label)).assertIsDisplayed()
  }

  @Test
  fun topStatsCard_displaysEventTitle() {
    compose.setContent { TopStatsCard(validated = 15, eventTitle = "Summer Festival") }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithText("Summer Festival").assertIsDisplayed()
    compose.onNodeWithText("15").assertIsDisplayed()
    compose.onNodeWithText(context.getString(R.string.scan_validated_label)).assertIsDisplayed()
  }

  @Test
  fun topStatsCard_showsWhenEventTitlePresent() {
    compose.setContent { TopStatsCard(validated = 0, eventTitle = "Tech Conference") }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithText("Tech Conference").assertIsDisplayed()
  }

  @Test
  fun topStatsCard_handlesLargeNumbers() {
    compose.setContent { TopStatsCard(validated = 9999, eventTitle = "Mega Event") }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithText("9999").assertIsDisplayed()
  }

  @Test
  fun topStatsCard_handlesLongEventTitle() {
    val longTitle = "This is a very long event title that should be truncated properly in the UI"
    compose.setContent { TopStatsCard(validated = 10, eventTitle = longTitle) }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
  }

  @Test
  fun topStatsCard_withZeroValidations_displaysZero() {
    compose.setContent { TopStatsCard(validated = 0, eventTitle = "Test Event") }

    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithText("0").assertIsDisplayed()
    compose.onNodeWithText(context.getString(R.string.scan_validated_label)).assertIsDisplayed()
  }

  // ==================== BACK BUTTON ====================

  @Test
  fun backButton_callsOnNavigateBack() {
    val vm = createVM()
    var backCalled = false

    compose.setContent { ScanContent(viewModel = vm, onNavigateBack = { backCalled = true }) }

    compose.onNodeWithTag(ScanTestTags.BACK_BUTTON).performClick()
    compose.waitForIdle()

    assert(backCalled) { "Back button should trigger navigation callback" }
  }

  @Test
  fun backButton_remainsResponsiveDuringScan() {
    val repo =
        FakeRepo().apply {
          delayMs = 500
          next = Result.success(ScanDecision.Accepted(ticketId = "T-123"))
        }
    val vm = createVM(repo)
    var backCalled = false

    compose.setContent { ScanContent(viewModel = vm, onNavigateBack = { backCalled = true }) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }

    compose.onNodeWithTag(ScanTestTags.BACK_BUTTON).performClick()
    compose.waitForIdle()

    assert(backCalled) { "Back button should remain responsive during scan processing" }
  }

  @Test
  fun backButton_worksAfterFlash() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = "T-123")) }
    val vm = createVM(repo)
    var backCalled = false

    compose.setContent { ScanContent(viewModel = vm, onNavigateBack = { backCalled = true }) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.BACK_BUTTON).performClick()
    compose.waitForIdle()

    assert(backCalled) { "Flash overlay should not block back button" }
  }

  @Test
  fun backButton_worksAfterError() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Test error")) }
    val vm = createVM(repo)
    var backCalled = false

    compose.setContent { ScanContent(viewModel = vm, onNavigateBack = { backCalled = true }) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.BACK_BUTTON).performClick()
    compose.waitForIdle()

    assert(backCalled) { "Back button should work after error" }
  }

  // ==================== SCAN BLOCKING DURING DIALOGS ====================

  @Test
  fun scanBlocking_noFlashWhenNetworkDialogShown() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Network error")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()

    repo.next = Result.success(ScanDecision.Accepted(ticketId = "T-BLOCKED"))
    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()
  }

  @Test
  fun scanBlocking_noFlashWhenSessionDialogShown() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Session expired")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag("scan_session_expired_dialog").assertIsDisplayed()

    repo.next = Result.success(ScanDecision.Accepted(ticketId = "T-BLOCKED"))
    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag("scan_session_expired_dialog").assertIsDisplayed()
  }

  @Test
  fun dialogBlocking_scansDuringNetworkDialog_areIgnored() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Network error")) }
    val vm = createVM(repo)
    var scanCount = 0

    compose.setContent { ScanContent(viewModel = vm, onNavigateBack = {}) }

    compose.runOnIdle {
      vm.onQrScanned(VALID_QR)
      scanCount++
    }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()

    repo.next = Result.success(ScanDecision.Accepted(ticketId = "T-SHOULD-NOT-PROCESS"))
    repeat(3) {
      compose.runOnIdle {
        vm.onQrScanned("different-qr-$it")
        scanCount++
      }
      compose.waitForIdle()
    }

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()

    assert(scanCount == 4) { "Should have attempted 4 scans total" }
  }

  @Test
  fun dialogBlocking_scansDuringSessionDialog_areIgnored() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Session expired")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag("scan_session_expired_dialog").assertIsDisplayed()

    repo.next = Result.success(ScanDecision.Accepted(ticketId = "T-BLOCKED"))
    compose.runOnIdle { vm.onQrScanned("another-qr") }
    compose.waitForIdle()

    compose.onNodeWithTag("scan_session_expired_dialog").assertIsDisplayed()
  }

  // ==================== PREVIEW COMPOSABLES ====================

  @Test
  fun previewHudValidating_showsProgressIndicator() {
    compose.setContent { PreviewScanHudValidating() }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Validating", substring = true)
    compose.onNodeWithTag(ScanTestTags.PROGRESS).assertIsDisplayed()
  }

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
  fun previewWithEventTitle_showsTitleAndStats() {
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

  // ==================== MULTIPLE SCANS SEQUENCE ====================

  @Test
  fun networkDialog_retryWithLastScannedQr_retriesSameQr() {
    val repo = FakeRepo().apply { next = Result.failure(Exception("Network timeout")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertIsDisplayed()

    repo.next = Result.success(ScanDecision.Accepted(ticketId = "T-RETRY"))

    compose.onNodeWithText(context.getString(R.string.scan_retry_button)).performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.NETWORK_ERROR_DIALOG).assertDoesNotExist()
  }

  // ==================== REGRESSION TESTS ====================

  @Test
  fun multipleFastScans_doesNotCrash() {
    val repo = FakeRepo()
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    repeat(5) { i ->
      repo.next = Result.success(ScanDecision.Accepted(ticketId = "T-$i"))
      compose.runOnIdle { vm.onQrScanned("qr-$i") }
    }

    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertIsDisplayed()
  }

  @Test
  fun rapidSameQrScans_handledCorrectly() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = "T-123")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    repeat(3) { compose.runOnIdle { vm.onQrScanned(VALID_QR) } }

    compose.waitForIdle()
    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertIsDisplayed()
  }

  // ==================== SCANNING STATES & TRANSITIONS ====================

  @Test
  fun scanTransition_idleToProcessingToAccepted() {
    val repo =
        FakeRepo().apply {
          delayMs = 0
          next = Result.success(ScanDecision.Accepted(ticketId = "T-TRANS"))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Scan a pass", substring = true)

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }

    compose.waitForIdle()
    compose.waitUntil(timeoutMillis = 1000) {
      runCatching {
            compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Access Granted")
          }
          .isSuccess
    }

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Access Granted")
    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  @Test
  fun scanTransition_processingToAccepted_hidesSpinner() {
    val repo =
        FakeRepo().apply {
          delayMs = 500
          next = Result.success(ScanDecision.Accepted(ticketId = "T-123"))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }

    compose.waitUntil(timeoutMillis = 1000) {
      compose.onAllNodesWithTag(ScanTestTags.PROGRESS).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithTag(ScanTestTags.PROGRESS).assertIsDisplayed()

    compose.waitForIdle()

    compose.waitUntil(timeoutMillis = 1000) {
      compose.onAllNodesWithTag(ScanTestTags.PROGRESS).fetchSemanticsNodes().isEmpty()
    }

    compose.onNodeWithTag(ScanTestTags.PROGRESS).assertDoesNotExist()
    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

  // ==================== AUTO RESET BEHAVIOR ====================

  @Test
  fun acceptedScan_resetsToIdleAfterDelay() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = "T-123")) }
    val vm =
        ScannerViewModel(
            eventId = "event1",
            repo = repo,
            clock = { 0L },
            enableAutoCleanup = false,
            stateResetDelayMs = 2000L,
            coroutineScope = CoroutineScope(Dispatchers.Main))

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }

    // Wait for Access Granted (handling async Firestore call)
    compose.waitUntil(timeoutMillis = 3000) {
      runCatching {
            compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Access Granted")
          }
          .isSuccess
    }

    // Wait for auto-reset
    compose.waitUntil(timeoutMillis = 4000) {
      runCatching {
            compose
                .onNodeWithTag(ScanTestTags.MESSAGE)
                .assertTextContains("Scan a pass", substring = true)
          }
          .isSuccess
    }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Scan a pass", substring = true)
  }

  @Test
  fun rejectedScan_resetsToIdleAfterDelay() {
    val repo =
        FakeRepo().apply {
          next = Result.success(ScanDecision.Rejected(ScanDecision.Reason.ALREADY_SCANNED))
        }
    val vm =
        ScannerViewModel(
            eventId = "event1",
            repo = repo,
            clock = { 0L },
            enableAutoCleanup = false,
            stateResetDelayMs = 2000L,
            coroutineScope = CoroutineScope(Dispatchers.Main))

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Already scanned")

    compose.waitUntil(timeoutMillis = 3000) {
      runCatching {
            compose
                .onNodeWithTag(ScanTestTags.MESSAGE)
                .assertTextContains("Scan a pass", substring = true)
          }
          .isSuccess
    }
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Scan a pass", substring = true)
  }

  // ==================== INSTRUCTIONS TEXT ====================

  @Test
  fun instructions_reappearAfterReset() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = "T-123")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }

    // Wait for the scan to fully complete (including async Firestore calls)
    compose.waitUntil(timeoutMillis = 2000) {
      runCatching {
            compose.onNodeWithTag(ScanTestTags.MESSAGE).assertTextContains("Access Granted")
          }
          .isSuccess
    }

    compose.onNodeWithText(context.getString(R.string.scan_instruction)).assertDoesNotExist()

    vm.resetToIdle()
    compose.waitForIdle()

    compose.onNodeWithText(context.getString(R.string.scan_instruction)).assertIsDisplayed()
  }

  // ==================== PROGRESS INDICATOR ====================

  @Test
  fun progressIndicator_hiddenAfterSuccess() {
    val repo =
        FakeRepo().apply {
          delayMs = 100
          next = Result.success(ScanDecision.Accepted(ticketId = "T-123"))
        }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }
    compose.waitForIdle()

    compose.waitUntil(timeoutMillis = 1000) {
      compose.onAllNodesWithTag(ScanTestTags.PROGRESS).fetchSemanticsNodes().isEmpty()
    }

    compose.onNodeWithTag(ScanTestTags.PROGRESS).assertDoesNotExist()
  }

  // ==================== STATUS ICON ====================

  @Test
  fun statusIcon_hidesAfterResetToIdle() {
    val repo = FakeRepo().apply { next = Result.success(ScanDecision.Accepted(ticketId = "T-123")) }
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    compose.runOnIdle { vm.onQrScanned(VALID_QR) }

    compose.waitUntil(timeoutMillis = 2000) {
      compose.onAllNodesWithTag(ScanTestTags.STATUS_ICON).fetchSemanticsNodes().isNotEmpty()
    }

    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()

    vm.resetToIdle()
    compose.waitForIdle()

    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertDoesNotExist()
  }

  // ==================== RAPID STATE CHANGES ====================

  @Test
  fun rapidStateChanges_doesNotCrash() {
    val repo = FakeRepo()
    val vm = createVM(repo)

    compose.setContent { ScanContent(viewModel = vm) }

    repeat(10) { i ->
      repo.next =
          if (i % 2 == 0) {
            Result.success(ScanDecision.Accepted(ticketId = "T-$i"))
          } else {
            Result.success(ScanDecision.Rejected(ScanDecision.Reason.ALREADY_SCANNED))
          }
      compose.runOnIdle { vm.onQrScanned("qr-$i") }
    }

    compose.waitForIdle()
    compose.onNodeWithTag(ScanTestTags.MESSAGE).assertIsDisplayed()
  }

  // ==================== FAKE REPOSITORY ====================

  class FakeRepo : TicketScanRepository {
    var next: Result<ScanDecision> = Result.success(ScanDecision.Accepted(ticketId = "T-DEFAULT"))
    var delayMs: Long = 0

    override suspend fun validateByPass(qrText: String, eventId: String): Result<ScanDecision> {
      if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
      return next
    }
  }
}
