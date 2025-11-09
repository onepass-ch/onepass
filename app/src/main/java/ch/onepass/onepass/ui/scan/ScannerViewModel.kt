package ch.onepass.onepass.ui.scan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.pass.Pass
import ch.onepass.onepass.model.scan.ScanDecision
import ch.onepass.onepass.model.scan.TicketScanRepository
import ch.onepass.onepass.model.scan.TicketScanRepositoryFirebase
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Immutable UI state for the scanner screen. */
data class ScannerUiState(
    val isProcessing: Boolean = false,
    val message: String = "Scan a pass…",
    val lastTicketId: String? = null,
    val lastScannedAt: Long? = null,
    val remaining: Int? = null,
    val status: Status = Status.IDLE,
) {
  enum class Status {
    IDLE,
    ACCEPTED,
    REJECTED,
    ERROR
  }
}

/** One-shot UI effects (sound, haptics, animations, snackbars). */
sealed interface ScannerEffect {
  data class Accepted(val message: String) : ScannerEffect

  data class Rejected(val message: String) : ScannerEffect

  data class Error(val message: String) : ScannerEffect
}

/**
 * ViewModel responsible for OnePass QR scanning logic.
 *
 * Responsibilities:
 * - Parse and validate scanned QR codes.
 * - Deduplicate repeated scans in a short time window.
 * - Expose UI state (processing, messages, status).
 * - Emit one-shot effects for user feedback.
 */
open class ScannerViewModel(
    private val eventId: String,
    private val repo: TicketScanRepository = TicketScanRepositoryFirebase(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val enableAutoCleanup: Boolean = true,
    private val cleanupPeriodMs: Long = 10_000L,
    private val stateResetDelayMs: Long = 3_000L,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

  init {
    require(eventId.isNotBlank()) { "eventId required" }
  }

  // Observable UI state.
  private val _state = MutableStateFlow(ScannerUiState())
  open val state: StateFlow<ScannerUiState> = _state.asStateFlow()

  // One-shot effects (sound, vibration, animation).
  // replay=1 ensures late collectors still receive the latest effect.
  private val _effects = MutableSharedFlow<ScannerEffect>(replay = 1, extraBufferCapacity = 1)
  val effects = _effects.asSharedFlow()

  // Recent scans cache to prevent duplicates (uid -> timestamp).
  private val recentScans = ConcurrentHashMap<String, Long>()
  private val dedupeWindowMs = 2.seconds.inWholeMilliseconds

  private val scope: CoroutineScope = coroutineScope ?: viewModelScope
  private var cleanupJob: Job? = null
  private var resetJob: Job? = null

  init {
    // Periodically clean up expired dedupe entries.
    if (enableAutoCleanup) {
      cleanupJob =
          scope.launch {
            while (isActive) {
              delay(cleanupPeriodMs)
              cleanupRecentScans()
            }
          }
    }
  }

  /** Removes expired entries from the deduplication cache. */
  fun cleanupRecentScans() {
    val now = clock()
    recentScans.entries.removeIf { (_, timestamp) -> (now - timestamp) > dedupeWindowMs }
  }

  /** Main entrypoint: called when a QR code is scanned. */
  fun onQrScanned(qrText: String) {
    scope.launch {
      // Guard: ignore scans while a previous one is being processed.
      if (_state.value.isProcessing) {
        Log.d(TAG, "Scan already in progress, ignoring")
        return@launch
      }

      val qr = qrText.trim()

      // 1) Parse: extract data from the QR string.
      val pass =
          Pass.parseFromQr(qr).getOrElse { error ->
            Log.w(TAG, "Invalid QR format: ${error.message}")
            showRejection("Invalid QR format")
            return@launch
          }

      val uid = pass.uid

      // 2) Deduplicate: ignore repeated scans within the time window.
      if (isDuplicate(uid)) return@launch

      // 3) Validate: call backend for cryptographic validation and ticket check.
      processValidation(qr, uid)
    }
  }

  /** Returns true if this uid was scanned recently and should be ignored. */
  private fun isDuplicate(uid: String): Boolean {
    val now = clock()
    val last = recentScans[uid]
    return if (last != null && (now - last) < dedupeWindowMs) {
      Log.d(TAG, "Duplicate scan ignored for uid=$uid within ${dedupeWindowMs}ms")
      true
    } else {
      recentScans[uid] = now
      cleanupRecentScans() // Opportunistic cleanup.
      false
    }
  }

  /** Sends the QR to the backend for signature validation and ticket verification. */
  private suspend fun processValidation(qr: String, uid: String) {
    // Show "Validating…" while the network call is in flight.
    _state.value = _state.value.copy(isProcessing = true, message = "Validating…")

    val result = repo.validateByPass(qr, eventId)

    result.fold(
        onSuccess = { decision -> handleDecision(decision, uid) },
        onFailure = { error -> handleError(error, uid) })

    // Schedule an automatic state reset after a delay.
    scheduleStateReset()
  }

  /** Handles the backend decision (Accepted or Rejected). */
  private suspend fun handleDecision(decision: ScanDecision, uid: String) {
    when (decision) {
      is ScanDecision.Accepted -> {
        Log.d(TAG, "Accepted: uid=$uid, ticket=${decision.ticketId}")
        _state.value =
            _state.value.copy(
                isProcessing = false,
                status = ScannerUiState.Status.ACCEPTED,
                message = "Access Granted",
                lastTicketId = decision.ticketId,
                lastScannedAt = decision.scannedAtSeconds,
                remaining = decision.remaining)
        _effects.emit(ScannerEffect.Accepted("Access Granted"))
      }
      is ScanDecision.Rejected -> {
        val msg =
            when (decision.reason) {
              ScanDecision.Reason.UNREGISTERED -> "User not registered"
              ScanDecision.Reason.ALREADY_SCANNED -> "Already scanned"
              ScanDecision.Reason.BAD_SIGNATURE -> "Invalid signature"
              ScanDecision.Reason.REVOKED -> "Pass revoked"
              else -> "Access denied"
            }
        Log.w(TAG, "Rejected: uid=$uid, reason=${decision.reason}")
        showRejection(msg)
      }
    }
  }

  /** Handles network or server errors. */
  private suspend fun handleError(error: Throwable, uid: String) {
    val msg = error.message?.let { "Error: $it" } ?: "Network or server error"

    Log.e(TAG, "Validation failed for uid=$uid", error)

    _state.value =
        _state.value.copy(isProcessing = false, status = ScannerUiState.Status.ERROR, message = msg)
    _effects.emit(ScannerEffect.Error(msg))
  }

  /** Applies a rejected state and emits the corresponding effect. */
  private suspend fun showRejection(msg: String) {
    _state.value =
        _state.value.copy(
            isProcessing = false, status = ScannerUiState.Status.REJECTED, message = msg)
    _effects.emit(ScannerEffect.Rejected(msg))
  }

  /** Schedules an automatic state reset to the initial state (IDLE). */
  private fun scheduleStateReset() {
    resetJob?.cancel()
    resetJob =
        scope.launch {
          delay(stateResetDelayMs)
          _state.value = ScannerUiState()
        }
  }

  override fun onCleared() {
    cleanupJob?.cancel()
    resetJob?.cancel()
    super.onCleared()
  }

  companion object {
    private const val TAG = "ScannerViewModel"
  }
}
