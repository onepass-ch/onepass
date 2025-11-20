package ch.onepass.onepass.ui.myevents

import android.content.Context
import androidx.annotation.ColorRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.pass.PassRepository
import ch.onepass.onepass.model.ticket.TicketRepository
import ch.onepass.onepass.model.ticket.TicketRepositoryFirebase
import ch.onepass.onepass.model.ticket.toUiTicket
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Extension property for DataStore (can be used elsewhere in the app).
 *
 * SECURITY NOTE: Cache stores QR in plain text. If a device is compromised, cached QRs could be
 * extracted and replayed until server-side revocation.
 */
val Context.passDataStore: DataStore<Preferences> by preferencesDataStore(name = "onepass_cache")

/** Data class representing a ticket in the UI. */
data class Ticket(
    val title: String,
    val status: TicketStatus,
    val dateTime: String,
    val location: String,
)

/** Enum representing the status of a ticket for UI display purposes. */
enum class TicketStatus(@ColorRes val colorRes: Int) {
  CURRENTLY(R.color.status_currently),
  UPCOMING(R.color.status_upcoming),
  EXPIRED(R.color.status_expired)
}

/**
 * Unified ViewModel for the "My Events" screen:
 * - Manages tickets (current & expired) enriched with their events
 * - Manages the user's pass (QR) with DataStore cache and offline fallback
 *
 * SECURITY NOTE: Cache stores QR in plain text. If a device is compromised, cached QRs could be
 * extracted and replayed until server-side revocation.
 *
 * @param dataStore DataStore for caching QR codes (injected, not via Application)
 * @param passRepository Repository for signed passes
 * @param ticketRepo Repository for tickets (Firebase by default)
 * @param eventRepo Repository for events (Firebase by default)
 * @param userId Optional: if null, FirebaseAuth.currentUser?.uid is used
 */
class MyEventsViewModel(
    private val dataStore: DataStore<Preferences>,
    private val passRepository: PassRepository,
    private val ticketRepo: TicketRepository = TicketRepositoryFirebase(),
    private val eventRepo: EventRepository = EventRepositoryFirebase(),
    private val userId: String? = null
) : ViewModel() {

  // ---------- Companion Object ----------
  companion object {
    /** Per-user cache key to avoid leaks between users/tests */
    private fun qrKey(uid: String) = stringPreferencesKey("cached_qr_text_$uid")
  }

  // ---------- QR / Pass State ----------

  private val _userQrData = MutableStateFlow<String?>(null)
  /** Current user's QR code data (null if not loaded or unavailable) */
  val userQrData: StateFlow<String?> = _userQrData

  private val _isLoading = MutableStateFlow(false)
  /** True when fetching QR code from server */
  val isLoading: StateFlow<Boolean> = _isLoading

  private val _error = MutableStateFlow<String?>(null)
  /** Error message if QR code loading failed (null if no error) */
  val error: StateFlow<String?> = _error

  // ---------- User ID Resolution ----------

  /** Resolved user ID: either from constructor or Firebase Auth */
  private val resolvedUserId: String?
    get() = userId ?: FirebaseAuth.getInstance().currentUser?.uid

  // ---------- Tickets State ----------

  /**
   * Enriches a list of tickets with their associated event details.
   *
   * @param tickets List of tickets to enrich
   * @return Flow emitting enriched tickets with event information
   */
  private fun enrichTickets(
      tickets: List<ch.onepass.onepass.model.ticket.Ticket>
  ): Flow<List<Ticket>> {
    if (tickets.isEmpty()) return flowOf(emptyList())

    val flows =
        tickets.map { ticket ->
          eventRepo.getEventById(ticket.eventId).map { event -> ticket.toUiTicket(event) }
        }
    return combine(flows) { it.toList() }
  }

  /** StateFlow of the user's current (active) tickets enriched with event details */
  val currentTickets: StateFlow<List<Ticket>> =
      (resolvedUserId?.let { uid ->
            ticketRepo.getActiveTickets(uid).flatMapLatest { enrichTickets(it) }
          } ?: flowOf(emptyList()))
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  /** StateFlow of the user's expired or redeemed tickets enriched with event details */
  val expiredTickets: StateFlow<List<Ticket>> =
      (resolvedUserId?.let { uid ->
            ticketRepo.getExpiredTickets(uid).flatMapLatest { enrichTickets(it) }
          } ?: flowOf(emptyList()))
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  // ---------- Initialization ----------

  init {
    // Load cached QR at startup for offline support
    viewModelScope.launch { loadCachedQr() }
  }

  // ---------- Pass / QR API ----------

  /**
   * Loads the user's pass from the server (or creates it if missing). On success: updates state and
   * saves to cache. On failure: falls back to cached data for offline support.
   */
  fun loadUserPass() {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null

      try {
        val authUid = resolvedUserId
        require(!authUid.isNullOrBlank()) { "User not authenticated" }

        val result = passRepository.getOrCreateSignedPass(authUid)

        when {
          result.isSuccess -> {
            val pass = result.getOrThrow()
            _userQrData.value = pass.qrText
            // Await cache save completion
            saveCachedQr(pass.qrText)
          }
          result.isFailure -> {
            _error.value = result.exceptionOrNull()?.message ?: "Failed to load pass"
            // Fallback to cache for offline mode
            loadCachedQr()
          }
        }
      } catch (t: Throwable) {
        _error.value = t.message ?: "Authentication error"
        loadCachedQr()
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Alias to reload the pass (e.g., for pull-to-refresh) */
  fun refreshPass() = loadUserPass()

  // ---------- DataStore Cache Helpers ----------

  /**
   * Saves QR text to DataStore cache for offline access. Note: Suspending function - must complete
   * before returning.
   */
  private suspend fun saveCachedQr(qrText: String) {
    val uid = resolvedUserId ?: return

    try {
      dataStore.edit { prefs -> prefs[qrKey(uid)] = qrText }
    } catch (e: Exception) {
      // Silently fail - cache is optional, don't disrupt user experience
      _error.value = "Failed to cache QR code: ${e.message}"
    }
  }

  /**
   * Loads QR text from DataStore cache (for offline mode). Sets state to null if no cache exists or
   * user is not authenticated.
   */
  private suspend fun loadCachedQr() {
    val uid =
        resolvedUserId
            ?: run {
              _userQrData.value = null
              return
            }

    try {
      val prefs = dataStore.data.first()
      _userQrData.value = prefs[qrKey(uid)]
    } catch (e: Exception) {
      // Cache read failed - not critical, just leave state null
      _userQrData.value = null
    }
  }

  /**
   * Clears the cached QR code for the current user. Useful for logout or security-sensitive
   * operations.
   */
  fun clearCache() {
    viewModelScope.launch {
      val uid = resolvedUserId ?: return@launch

      try {
        dataStore.edit { prefs -> prefs.remove(qrKey(uid)) }
        _userQrData.value = null
      } catch (e: Exception) {}
    }
  }
}
