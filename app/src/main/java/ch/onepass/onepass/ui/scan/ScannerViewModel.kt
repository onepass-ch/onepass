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

/** État de l'interface du scanner */
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

/** Effets one-shot pour déclencher des animations/sons dans l'UI */
sealed interface ScannerEffect {
  data class Accepted(val message: String) : ScannerEffect

  data class Rejected(val message: String) : ScannerEffect

  data class Error(val message: String) : ScannerEffect
}

/**
 * ViewModel pour gérer le scan de QR codes OnePass
 *
 * Responsabilités:
 * - Valider les QR codes scannés
 * - Déduplication des scans répétés
 * - Gestion de l'état UI (processing, messages, status)
 * - Émission d'effets one-shot pour feedback visuel/sonore
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

  // État observable de l'UI (scan en cours, message, statut)
  private val _state = MutableStateFlow(ScannerUiState())
  open val state: StateFlow<ScannerUiState> = _state.asStateFlow()

  // Effets one-shot (sons, vibrations, animations)
  // replay=1 garantit qu'un collecteur tardif reçoit le dernier effet
  private val _effects = MutableSharedFlow<ScannerEffect>(replay = 1, extraBufferCapacity = 1)
  val effects = _effects.asSharedFlow()

  // Cache pour éviter les scans dupliqués (uid -> timestamp)
  private val recentScans = ConcurrentHashMap<String, Long>()
  private val dedupeWindowMs = 2.seconds.inWholeMilliseconds

  private val scope: CoroutineScope = coroutineScope ?: viewModelScope
  private var cleanupJob: Job? = null
  private var resetJob: Job? = null

  init {
    // Nettoyage périodique du cache de déduplication
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

  /** Nettoie les entrées expirées du cache de déduplication */
  fun cleanupRecentScans() {
    val now = clock()
    recentScans.entries.removeIf { (_, timestamp) -> (now - timestamp) > dedupeWindowMs }
  }

  /** Point d'entrée principal: appelé quand un QR code est scanné */
  fun onQrScanned(qrText: String) {
    scope.launch {
      // Guard: évite les scans concurrents
      if (_state.value.isProcessing) {
        Log.d(TAG, "Scan already in progress, ignoring...")
        return@launch
      }

      val qr = qrText.trim()

      // 1) PARSE: Extraction des données du QR
      val pass =
          Pass.parseFromQr(qr).getOrElse { error ->
            Log.w(TAG, "Invalid QR format: ${error.message}")
            showRejection("Invalid QR format")
            return@launch
          }

      val uid = pass.uid

      // 2) DEDUPLICATE: Évite les scans répétés accidentels
      if (isDuplicate(uid)) return@launch

      // 3) VALIDATE: Appel backend pour vérifier signature + ticket
      processValidation(qr, uid)
    }
  }

  /** Vérifie si ce uid a déjà été scanné récemment */
  private fun isDuplicate(uid: String): Boolean {
    val now = clock()
    val last = recentScans[uid]
    return if (last != null && (now - last) < dedupeWindowMs) {
      Log.d(TAG, "Duplicate scan ignored for uid=$uid within ${dedupeWindowMs}ms")
      true
    } else {
      recentScans[uid] = now
      cleanupRecentScans() // Nettoyage opportuniste
      false
    }
  }

  /** Envoie le QR au backend pour validation cryptographique + vérification ticket */
  private suspend fun processValidation(qr: String, uid: String) {
    // Affiche "Validating…" pendant l'appel réseau
    _state.value = _state.value.copy(isProcessing = true, message = "Validating…")

    val result = repo.validateByPass(qr, eventId)

    result.fold(
        onSuccess = { decision -> handleDecision(decision, uid) },
        onFailure = { error -> handleError(error, uid) })

    // Reset automatique de l'état après 3 secondes
    scheduleStateReset()
  }

  /** Traite la décision du backend (Accepted ou Rejected) */
  private suspend fun handleDecision(decision: ScanDecision, uid: String) {
    when (decision) {
      is ScanDecision.Accepted -> {
        Log.d(TAG, "✅ Accepted: uid=$uid, ticket=${decision.ticketId}")
        _state.value =
            _state.value.copy(
                isProcessing = false, // ✅ Force à false
                status = ScannerUiState.Status.ACCEPTED,
                message = "Access Granted",
                lastTicketId = decision.ticketId,
                lastScannedAt = decision.scannedAtSeconds,
                remaining = decision.remaining)
        // Émission garantie (suspend, pas tryEmit)
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
        Log.w(TAG, "❌ Rejected: uid=$uid, reason=${decision.reason}")
        showRejection(msg)
      }
    }
  }

  /** Gère les erreurs réseau/serveur */
  private suspend fun handleError(error: Throwable, uid: String) {
    val msg = error.message?.let { "Error: $it" } ?: "Network or server error"

    Log.e(TAG, "⚠️ Validation failed for uid=$uid", error)

    _state.value =
        _state.value.copy(
            isProcessing = false, // ✅ Force à false
            status = ScannerUiState.Status.ERROR,
            message = msg)
    _effects.emit(ScannerEffect.Error(msg))
  }

  /** Affiche un rejet (format invalide ou refusé par backend) */
  private suspend fun showRejection(msg: String) {
    _state.value =
        _state.value.copy(
            isProcessing = false, // ✅ Force à false
            status = ScannerUiState.Status.REJECTED,
            message = msg)
    _effects.emit(ScannerEffect.Rejected(msg))
  }

  /** Planifie un reset automatique de l'état (retour à IDLE) */
  private fun scheduleStateReset() {
    resetJob?.cancel() // ✅ Annule le reset précédent
    resetJob =
        scope.launch {
          delay(stateResetDelayMs)
          _state.value = ScannerUiState() // Retour à l'état initial
        }
  }

  override fun onCleared() {
    cleanupJob?.cancel()
    resetJob?.cancel() // ✅ Cleanup du reset job
    super.onCleared()
  }

  companion object {
    private const val TAG = "ScannerViewModel"
  }
}
