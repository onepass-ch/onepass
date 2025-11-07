package ch.onepass.onepass.ui.scan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.pass.Pass
import ch.onepass.onepass.model.scan.ScanDecision
import ch.onepass.onepass.model.scan.TicketScanRepository
import ch.onepass.onepass.model.scan.TicketScanRepositoryFirebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

data class ScannerUiState(
    val isProcessing: Boolean = false,
    val message: String = "Scan a pass…",
    val lastTicketId: String? = null,
    val lastScannedAt: Long? = null,
    val remaining: Int? = null, // Tickets remaining for this event
    val status: Status = Status.IDLE,
) {
    enum class Status { IDLE, ACCEPTED, REJECTED, ERROR }
}

/** One-shot UI effects (sound, haptics, color flash). */
sealed interface ScannerEffect {
    data class Accepted(val message: String) : ScannerEffect
    data class Rejected(val message: String) : ScannerEffect
    data class Error(val message: String) : ScannerEffect
}

open class ScannerViewModel(
    private val eventId: String,
    private val repo: TicketScanRepository = TicketScanRepositoryFirebase(),
    private val clock: () -> Long = { System.currentTimeMillis() } // Injectable for testing
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerUiState())
    open val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ScannerEffect>(replay = 0, extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    // Client-side dedupe: ignore same uid within 2s window
    private val recentScans = ConcurrentHashMap<String, Long>()
    private val dedupeWindowMs = 2.seconds.inWholeMilliseconds

    init {
        // Periodically clean up old entries to prevent memory leak
        viewModelScope.launch {
            while (true) {
                delay(10.seconds)
                val now = clock()
                recentScans.entries.removeIf { (_, timestamp) ->
                    (now - timestamp) > dedupeWindowMs
                }
            }
        }
    }

    /**
     * Entry point called by the camera layer when a QR string is read.
     * Parses the QR, performs client-side dedupe, then delegates to backend for validation.
     */
    fun onQrScanned(qrText: String) {
        viewModelScope.launch {
            val qr = qrText.trim()

            // Parse the QR to extract and validate format
            val parseResult = Pass.parseFromQr(qr)
            if (parseResult.isFailure) {
                Log.w(TAG, "Invalid QR format: ${parseResult.exceptionOrNull()?.message}")
                reject("Invalid QR format")
                return@launch
            }

            val pass = parseResult.getOrThrow()
            val uid = pass.uid

            // Dedupe by uid (not entire QR string)
            val now = clock()
            val last = recentScans[uid]
            if (last != null && (now - last) < dedupeWindowMs) {
                Log.d(TAG, "Ignoring duplicate scan for uid=$uid within ${dedupeWindowMs}ms")
                return@launch
            }
            recentScans[uid] = now

            _state.value = _state.value.copy(isProcessing = true, message = "Validating…")

            // Backend validation (signature check + ticket lookup)
            val result = repo.validateByPass(qr, eventId)
            _state.value = _state.value.copy(isProcessing = false)

            result.onSuccess { decision ->
                when (decision) {
                    is ScanDecision.Accepted -> {
                        Log.d(TAG, "Accepted: uid=$uid, ticket=${decision.ticketId}")
                        _state.value = _state.value.copy(
                            status = ScannerUiState.Status.ACCEPTED,
                            message = "Access Granted",
                            lastTicketId = decision.ticketId,
                            lastScannedAt = decision.scannedAtSeconds,
                            remaining = decision.remaining
                        )
                        _effects.tryEmit(ScannerEffect.Accepted("Access Granted"))
                    }
                    is ScanDecision.Rejected -> {
                        val msg = when (decision.reason) {
                            ScanDecision.Reason.UNREGISTERED -> "User not registered"
                            ScanDecision.Reason.ALREADY_SCANNED -> "Already scanned"
                            ScanDecision.Reason.BAD_SIGNATURE -> "Invalid signature"
                            ScanDecision.Reason.REVOKED -> "Pass revoked"
                            else -> "Rejected"
                        }
                        Log.w(TAG, "Rejected: uid=$uid, reason=${decision.reason}")
                        _state.value = _state.value.copy(
                            status = ScannerUiState.Status.REJECTED,
                            message = msg
                        )
                        _effects.tryEmit(ScannerEffect.Rejected(msg))
                    }
                }
            }.onFailure { error ->
                val msg = "Network or server error"
                Log.e(TAG, "Validation failed for uid=$uid: ${error.message}", error)
                _state.value = _state.value.copy(
                    status = ScannerUiState.Status.ERROR,
                    message = msg
                )
                _effects.tryEmit(ScannerEffect.Error(msg))
            }
        }
    }

    private fun reject(msg: String) {
        _state.value = _state.value.copy(status = ScannerUiState.Status.REJECTED, message = msg)
        _effects.tryEmit(ScannerEffect.Rejected(msg))
    }

    companion object {
        private const val TAG = "ScannerViewModel"
    }
}