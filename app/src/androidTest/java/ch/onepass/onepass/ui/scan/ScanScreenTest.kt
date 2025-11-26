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

// QR VALIDE : le même que dans tes tests unitaires de ScannerViewModel
private const val VALID_QR =
    "onepass:user:v1.eyJ1aWQiOiJ1c2VyMTIzIiwia2lkIjoia2V5MSIsImlhdCI6MTczMDAwMDAwMCwidmVyIjoxfQ.c2lnX3VzZXIxMjM"

/**
 * Tests Compose pour le scanner. On monte directement ScanContent(viewModel) pour :
 * - éviter les problèmes de permission caméra
 * - couvrir la majorité de ScanScreen.kt
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

  @Test
  fun initial_state_showsIdleHud() {
    val vm = createVM()

    compose.setContent { ScanContent(viewModel = vm) }

    // Racine de l'écran, HUD et cadre de scan
    compose.onNodeWithTag(ScanTestTags.SCREEN).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.HUD).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.SCAN_FRAME).assertIsDisplayed()

    // Message initial IDLE
    compose
        .onNodeWithTag(ScanTestTags.MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Scan a pass", substring = true)
  }

  @Test
  fun accepted_scan_showsStatsAndGreenState() {
    val repo =
        FakeRepo().apply {
          next = Result.success(ScanDecision.Accepted(ticketId = "T-123", remaining = 10))
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
    compose.onNodeWithTag(ScanTestTags.STATS_CARD).assertIsDisplayed()
    compose.onNodeWithTag(ScanTestTags.STATUS_ICON).assertIsDisplayed()
  }

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

  /** Fake simple du repository pour les tests UI. */
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
