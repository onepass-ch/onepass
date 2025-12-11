package ch.onepass.onepass.ui.scan

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.pass.Pass
import ch.onepass.onepass.model.scan.ScanDecision
import ch.onepass.onepass.model.scan.TicketScanRepository
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
import kotlinx.coroutines.sync.Mutex

/**
 * Immutable UI state for the scanner screen.
 *
 * @property isProcessing True when a validation request is in flight
 * @property message Display message for the user
 * @property lastTicketId Last validated ticket ID
 * @property lastScannedAt Last scan timestamp from server
 * @property remaining Remaining event capacity
 * @property validated Number of tickets validated in this session
 * @property status Current visual status
 */
data class ScannerUiState(
    val isProcessing: Boolean = false,
    val message: String = "Scan a pass…",
    val lastTicketId: String? = null,
    val lastScannedAt: Long? = null,
    val remaining: Int? = null,
    val validated: Int = 0,
    val status: Status = Status.IDLE,
) {
    enum class Status {
        IDLE,
        ACCEPTED,
        REJECTED,
        ERROR
    }
}

/** One-shot UI effects for user feedback. */
sealed interface ScannerEffect {
    data class Accepted(val message: String) : ScannerEffect

    data class Rejected(val message: String) : ScannerEffect

    data class Error(val message: String) : ScannerEffect
}

/**
 * ViewModel for OnePass QR code scanning logic.
 *
 * Responsibilities:
 * - Parse and validate scanned QR codes
 * - Deduplicate repeated scans in a short time window
 * - Expose UI state and emit one-shot effects
 *
 * Thread Safety:
 * - Uses Mutex to prevent concurrent scan processing
 * - All state updates check scope.isActive
 * - ConcurrentHashMap for deduplication cache
 *
 * @param eventId The event ID for scan validation
 * @param repo Repository for backend validation
 * @param clock Injectable clock for testing
 * @param enableAutoCleanup Enable periodic cleanup of deduplication cache
 * @param cleanupPeriodMs Cleanup period in milliseconds
 * @param stateResetDelayMs Delay before auto-reset to IDLE
 * @param coroutineScope Injectable scope for testing
 */
class ScannerViewModel(
    private val eventId: String,
    private val repo: TicketScanRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val enableAutoCleanup: Boolean = true,
    private val cleanupPeriodMs: Long = 10_000L,
    private val stateResetDelayMs: Long = 3_000L,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    init {
        require(eventId.isNotBlank()) { "eventId required" }
    }

    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ScannerEffect>(replay = 1, extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    private val recentScans = ConcurrentHashMap<String, Long>()
    private val dedupeWindowMs = 2.seconds.inWholeMilliseconds

    private val scanMutex = Mutex()

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope
    private var cleanupJob: Job? = null
    private var resetJob: Job? = null
    private var eventListenerJob: Job? = null

    init {
        if (enableAutoCleanup) {
            cleanupJob =
                scope.launch {
                    while (isActive) {
                        delay(cleanupPeriodMs)
                        cleanupRecentScans()
                    }
                }
        }

        // Listen to event's ticketsRedeemed in real-time
        startEventListener()
    }

    /** Starts listening to the event's ticketsRedeemed field in Firestore */
    private fun startEventListener() {
        eventListenerJob = scope.launch {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val eventRef = firestore.collection("events").document(eventId)

                eventRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to event", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val ticketsRedeemed = snapshot.getLong("ticketsRedeemed")?.toInt() ?: 0
                        _state.value = _state.value.copy(validated = ticketsRedeemed)
                        Log.d(TAG, "Event ticketsRedeemed updated: $ticketsRedeemed")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start event listener", e)
            }
        }
    }

    /** Removes expired entries from the deduplication cache. */
    @VisibleForTesting
    internal fun cleanupRecentScans() {
        val now = clock()
        recentScans.entries.removeIf { (_, timestamp) -> (now - timestamp) > dedupeWindowMs }
    }

    /**
     * Main entry point called when a QR code is scanned.
     *
     * @param qrText Raw QR code string
     */
    fun onQrScanned(qrText: String) {
        scope.launch {
            if (!scanMutex.tryLock()) {
                Log.d(TAG, "Scan already in progress, ignoring")
                return@launch
            }

            try {
                val qr = qrText.trim()

                val pass =
                    Pass.parseFromQr(qr).getOrElse { error ->
                        Log.w(TAG, "Invalid QR format: ${error.message}")
                        showRejection("Invalid QR format")
                        return@launch
                    }

                val uid = pass.uid

                if (isDuplicate(uid)) return@launch

                processValidation(qr, uid)
            } finally {
                scanMutex.unlock()
            }
        }
    }

    /**
     * Checks if this uid was scanned recently within the deduplication window.
     *
     * @param uid User identifier
     * @return true if this scan should be ignored
     */
    private fun isDuplicate(uid: String): Boolean {
        val now = clock()
        val last = recentScans[uid]
        return if (last != null && (now - last) < dedupeWindowMs) {
            Log.d(TAG, "Duplicate scan ignored for uid=$uid")
            true
        } else {
            recentScans[uid] = now
            cleanupRecentScans()
            false
        }
    }

    /**
     * Sends the QR to the backend for validation.
     *
     * @param qr Raw QR string
     * @param uid User identifier
     */
    private suspend fun processValidation(qr: String, uid: String) {
        if (!scope.isActive) {
            Log.d(TAG, "Scope cancelled, aborting validation")
            return
        }

        _state.value = _state.value.copy(isProcessing = true, message = "Validating…")

        repo
            .validateByPass(qr, eventId)
            .onSuccess { decision ->
                if (scope.isActive) {
                    handleDecision(decision, uid)
                    scheduleStateReset()
                }
            }
            .onFailure { error ->
                if (scope.isActive) {
                    handleError(error, uid)
                    scheduleStateReset()
                }
            }
    }

    /**
     * Handles the backend decision.
     *
     * @param decision The validation decision from backend
     * @param uid User identifier
     */
    private suspend fun handleDecision(decision: ScanDecision, uid: String) {
        if (!scope.isActive) return

        when (decision) {
            is ScanDecision.Accepted -> {
                Log.d(TAG, "Accepted: uid=$uid, ticket=${decision.ticketId}")
                // Note: validated count is now updated via Firestore listener
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

    /**
     * Handles network or server errors.
     *
     * @param error The exception that occurred
     * @param uid User identifier
     */
    private suspend fun handleError(error: Throwable, uid: String) {
        if (!scope.isActive) return

        val msg = error.message?.let { "Error: $it" } ?: "Network or server error"

        Log.e(TAG, "Validation failed for uid=$uid", error)

        _state.value =
            _state.value.copy(isProcessing = false, status = ScannerUiState.Status.ERROR, message = msg)
        _effects.emit(ScannerEffect.Error(msg))
    }

    /**
     * Updates state to rejected and emits corresponding effect.
     *
     * @param msg Rejection message
     */
    private suspend fun showRejection(msg: String) {
        if (!scope.isActive) return

        _state.value =
            _state.value.copy(
                isProcessing = false, status = ScannerUiState.Status.REJECTED, message = msg)
        _effects.emit(ScannerEffect.Rejected(msg))
    }

    /** Schedules automatic state reset to IDLE after configured delay. */
    private fun scheduleStateReset() {
        resetJob?.cancel()
        resetJob =
            scope.launch {
                delay(stateResetDelayMs)
                if (isActive) {
                    _state.value = ScannerUiState()
                }
            }
    }

    /** Manually reset state to IDLE (useful when dismissing error dialogs). */
    fun resetToIdle() {
        resetJob?.cancel()
        _state.value = ScannerUiState()
    }

    @VisibleForTesting
    public override fun onCleared() {
        cleanupJob?.cancel()
        resetJob?.cancel()
        eventListenerJob?.cancel()
        recentScans.clear()
        super.onCleared()
    }

    companion object {
        private const val TAG = "ScannerViewModel"
    }
}