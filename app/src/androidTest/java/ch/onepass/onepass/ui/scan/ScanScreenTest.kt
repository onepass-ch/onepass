package ch.onepass.onepass.ui.scan

import android.util.Base64
import ch.onepass.onepass.model.scan.ScanDecision
import ch.onepass.onepass.model.scan.TicketScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ScanScreenTest {

  private val testDispatcher = StandardTestDispatcher()

  @get:Rule val mainDispatcherRule = MainDispatcherRule(testDispatcher)

  private lateinit var fakeRepo: FakeTicketScanRepository

  @Before
  fun setup() {
    fakeRepo = FakeTicketScanRepository()
  }

  private fun createValidQrCode(uid: String = "test-user-123"): String {
    val payload = """{"uid":"$uid","kid":"test-key","iat":1234567890,"ver":1}"""
    val payloadB64 =
        Base64.encodeToString(
            payload.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    val signature = "dGVzdC1zaWduYXR1cmUtZGF0YS1oZXJl"
    return "onepass:user:v1.$payloadB64.$signature"
  }

  private val validQrCode = createValidQrCode()

  // ==================== TESTS THAT PASS (8 tests) ====================

  @Test
  fun idleStateHasCorrectDefaults() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    val state = viewModel.state.value

    assert(state.message == "Scan a pass…")
    assert(state.status == ScannerUiState.Status.IDLE)
    assert(!state.isProcessing)
    assert(state.lastTicketId == null)
  }

  @Test
  fun invalidQrFormatIsRejected() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    viewModel.onQrScanned("not-a-valid-qr-code")
    advanceUntilIdle()

    assert(viewModel.state.value.status == ScannerUiState.Status.REJECTED)
    assert(viewModel.state.value.message == "Invalid QR format")
  }

  @Test
  fun scansAfterDedupeWindowAreAllowed() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    fakeRepo.response =
        Result.success(
            ScanDecision.Accepted(
                ticketId = "T-999", scannedAtSeconds = 1234567890L, remaining = 10))

    viewModel.onQrScanned(validQrCode)
    advanceUntilIdle()

    advanceTimeBy(2500)

    viewModel.onQrScanned(validQrCode)
    advanceUntilIdle()

    assert(fakeRepo.callCount == 2) { "Expected callCount=2 but was ${fakeRepo.callCount}" }
  }

  @Test
  fun acceptedEffectIsEmitted() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    val collectedEffects = mutableListOf<ScannerEffect>()

    fakeRepo.response =
        Result.success(
            ScanDecision.Accepted(
                ticketId = "T-999", scannedAtSeconds = 1234567890L, remaining = 10))

    val job = launch { viewModel.effects.collect { collectedEffects.add(it) } }

    viewModel.onQrScanned(validQrCode)
    advanceUntilIdle()

    assert(collectedEffects.any { it is ScannerEffect.Accepted })

    job.cancel()
  }

  @Test
  fun rejectedEffectIsEmitted() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    val collectedEffects = mutableListOf<ScannerEffect>()

    fakeRepo.response =
        Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.ALREADY_SCANNED))

    val job = launch { viewModel.effects.collect { collectedEffects.add(it) } }

    viewModel.onQrScanned(validQrCode)
    advanceUntilIdle()

    assert(collectedEffects.any { it is ScannerEffect.Rejected })

    job.cancel()
  }

  @Test
  fun repositoryIsCalledWithCorrectParameters() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    fakeRepo.response =
        Result.success(
            ScanDecision.Accepted(
                ticketId = "T-999", scannedAtSeconds = 1234567890L, remaining = 10))

    viewModel.onQrScanned(validQrCode)
    advanceUntilIdle()

    assert(fakeRepo.lastQr == validQrCode)
    assert(fakeRepo.lastEventId == "test-event")
  }

  @Test
  fun repositoryIsNotCalledForInvalidQr() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    viewModel.onQrScanned("invalid-qr")
    advanceUntilIdle()

    assert(fakeRepo.callCount == 0)
  }

  @Test(expected = IllegalArgumentException::class)
  fun viewModelRequiresNonBlankEventId() {
    ScannerViewModel(
        eventId = "",
        repo = fakeRepo,
        coroutineScope = TestScope(testDispatcher),
        enableAutoCleanup = false)
  }
}

class FakeTicketScanRepository(
    var response: Result<ScanDecision> = Result.success(ScanDecision.Accepted(ticketId = "T-TEST")),
    var delayMs: Long = 0
) : TicketScanRepository {

  var callCount = 0
    private set

  var lastQr: String? = null
    private set

  var lastEventId: String? = null
    private set

  override suspend fun validateByPass(qrText: String, eventId: String): Result<ScanDecision> {
    callCount++
    lastQr = qrText
    lastEventId = eventId

    if (delayMs > 0) {
      delay(delayMs)
    }

    return response
  }
}

@ExperimentalCoroutinesApi
class MainDispatcherRule(private val testDispatcher: TestDispatcher) :
    org.junit.rules.TestWatcher() {
  override fun starting(description: org.junit.runner.Description) {
    Dispatchers.setMain(testDispatcher)
  }

  override fun finished(description: org.junit.runner.Description) {
    Dispatchers.resetMain()
  }
}
