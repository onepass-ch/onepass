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

  @Test
  fun processingStateIsSetDuringScan() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    fakeRepo.delayMs = 100
    fakeRepo.response =
        Result.success(
            ScanDecision.Accepted(
                ticketId = "T-999", scannedAtSeconds = 1234567890L, remaining = 10))

    viewModel.onQrScanned(validQrCode)
    advanceTimeBy(50)

    assert(viewModel.state.value.isProcessing)
  }

  @Test
  fun emptyQrIsRejected() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    viewModel.onQrScanned("")
    advanceUntilIdle()

    assert(viewModel.state.value.status == ScannerUiState.Status.REJECTED)
  }

  @Test
  fun invalidPrefixIsRejected() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    viewModel.onQrScanned("invalid:prefix:v1.payload.sig")
    advanceUntilIdle()

    assert(viewModel.state.value.status == ScannerUiState.Status.REJECTED)
  }

  @Test
  fun errorEffectIsEmitted() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    val collectedEffects = mutableListOf<ScannerEffect>()
    fakeRepo.response = Result.failure(Exception("Error"))

    val job = launch { viewModel.effects.collect { collectedEffects.add(it) } }

    viewModel.onQrScanned(validQrCode)
    advanceUntilIdle()

    assert(collectedEffects.any { it is ScannerEffect.Error })

    job.cancel()
  }

  @Test
  fun concurrentScansAreIgnored() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    fakeRepo.delayMs = 500
    fakeRepo.response =
        Result.success(
            ScanDecision.Accepted(
                ticketId = "T-999", scannedAtSeconds = 1234567890L, remaining = 10))

    launch { viewModel.onQrScanned(validQrCode) }
    advanceTimeBy(100)
    launch { viewModel.onQrScanned(createValidQrCode("user2")) }

    advanceUntilIdle()

    assert(fakeRepo.callCount == 1) { "Expected callCount=1 but was ${fakeRepo.callCount}" }
  }

  @Test
  fun oldScansAreCleanedUpPeriodically() = runTest {
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

    viewModel.onQrScanned(createValidQrCode("user1"))
    advanceUntilIdle()

    advanceTimeBy(2500)

    viewModel.cleanupRecentScans()

    viewModel.onQrScanned(createValidQrCode("user1"))
    advanceUntilIdle()

    assert(fakeRepo.callCount == 2)
  }

  @Test
  fun multipleEffectsCanBeCollected() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    val collectedEffects = mutableListOf<ScannerEffect>()

    val job = launch { viewModel.effects.collect { collectedEffects.add(it) } }

    fakeRepo.response =
        Result.success(
            ScanDecision.Accepted(ticketId = "T-1", scannedAtSeconds = 1234567890L, remaining = 10))
    viewModel.onQrScanned(validQrCode)
    advanceUntilIdle()

    advanceTimeBy(2500)

    fakeRepo.response =
        Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.ALREADY_SCANNED))
    viewModel.onQrScanned(createValidQrCode("user2"))
    advanceUntilIdle()

    assert(collectedEffects.size == 2)
    assert(collectedEffects[0] is ScannerEffect.Accepted)
    assert(collectedEffects[1] is ScannerEffect.Rejected)

    job.cancel() //
  }

  @Test
  fun processingStateIsClearedAfterError() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    fakeRepo.delayMs = 100
    fakeRepo.response = Result.failure(Exception("Error"))

    viewModel.onQrScanned(validQrCode)
    advanceUntilIdle()

    assert(!viewModel.state.value.isProcessing)
  }

  @Test
  fun processingStateIsClearedAfterRejection() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    fakeRepo.delayMs = 100
    fakeRepo.response = Result.success(ScanDecision.Rejected(reason = ScanDecision.Reason.REVOKED))

    viewModel.onQrScanned(validQrCode)
    advanceUntilIdle()

    assert(!viewModel.state.value.isProcessing)
  }

  @Test
  fun differentUsersCanBeScannedSimultaneously() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    fakeRepo.response =
        Result.success(
            ScanDecision.Accepted(ticketId = "T-1", scannedAtSeconds = 1234567890L, remaining = 10))

    viewModel.onQrScanned(createValidQrCode("user1"))
    advanceUntilIdle()

    viewModel.onQrScanned(createValidQrCode("user2"))
    advanceUntilIdle()

    assert(fakeRepo.callCount == 2)
  }

  @Test
  fun processingStateIsClearedAfterAcceptedScan() = runTest {
    val viewModel =
        ScannerViewModel(
            eventId = "test-event",
            repo = fakeRepo,
            clock = { testScheduler.currentTime },
            coroutineScope = this,
            enableAutoCleanup = false)

    fakeRepo.delayMs = 100
    fakeRepo.response =
        Result.success(
            ScanDecision.Accepted(
                ticketId = "T-999", scannedAtSeconds = 1234567890L, remaining = 10))

    viewModel.onQrScanned(validQrCode)
    advanceUntilIdle()

    assert(!viewModel.state.value.isProcessing)
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
