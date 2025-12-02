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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Extension property for DataStore (can be used elsewhere in the app).
 *
 * SECURITY NOTE: Cache stores QR in plain text. If a device is compromised, cached QRs could be
 * extracted and replayed until server-side revocation.
 */
val Context.passDataStore: DataStore<Preferences> by preferencesDataStore(name = "onepass_cache")

/**
 * Data class representing a ticket in the UI.
 *
 * @param title The title of the event.
 * @param status The status of the ticket (e.g., CURRENTLY, UPCOMING, EXPIRED).
 * @param dateTime The display date and time of the event.
 * @param location The display location of the event.
 */
data class Ticket(
  val title: String,
  val status: TicketStatus,
  val dateTime: String,
  val location: String,
)

/**
 * Enum representing the status of a ticket for UI display purposes.
 *
 * @param colorRes The color resource associated with the status.
 */
enum class TicketStatus(@ColorRes val colorRes: Int) {
  CURRENTLY(R.color.status_currently),
  UPCOMING(R.color.status_upcoming),
  EXPIRED(R.color.status_expired)
}

/** Enum representing the tabs in the My Events screen. */
enum class TicketTab {
  CURRENT,
  EXPIRED
}

/**
 * Immutable UI state for the My Events screen.
 *
 * @property currentTickets List of current (active) tickets.
 * @property expiredTickets List of expired tickets.
 * @property selectedTab The currently selected tab (CURRENT or EXPIRED).
 */
data class MyEventsUiState(
  val currentTickets: List<Ticket> = emptyList(),
  val expiredTickets: List<Ticket> = emptyList(),
  val selectedTab: TicketTab = TicketTab.CURRENT,
  val isQrExpanded: Boolean = false
)

/**
 * ViewModel for managing and displaying the user's tickets.
 *
 * SECURITY NOTE: Cache stores QR in plain text. If a device is compromised, cached QRs could be
 * extracted and replayed until server-side revocation.
 *
 * @param dataStore DataStore for caching QR codes (injected, not via Application)
 * @param passRepository Repository for signed passes
 * @param ticketRepo The repository for ticket data (default is Firebase implementation).
 * @param eventRepo The repository for event data (default is Firebase implementation).
 * @param userId The ID of the current user whose tickets are being managed.
 */
class MyEventsViewModel(
  private val dataStore: DataStore<Preferences>,
  private val passRepository: PassRepository,
  private val ticketRepo: TicketRepository = TicketRepositoryFirebase(),
  private val eventRepo: EventRepository = EventRepositoryFirebase(),
  private val userId: String?
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

  // ---------- UI State ----------

  /** Backing state for the UI state flow */
  private val _uiState = MutableStateFlow(MyEventsUiState())
  /** Publicly exposed UI state as a StateFlow */
  val uiState: StateFlow<MyEventsUiState> = _uiState

  // ---------- Tickets State ----------

  /** StateFlow of the user's current (active) tickets enriched with event details. */
  val currentTickets: StateFlow<List<Ticket>> =
    (userId?.let { uid -> ticketRepo.getActiveTickets(uid).flatMapLatest { enrichTickets(it) } }
      ?: flowOf(emptyList()))
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  /** StateFlow of the user's expired or redeemed tickets enriched with event details. */
  val expiredTickets: StateFlow<List<Ticket>> =
    (userId?.let { uid -> ticketRepo.getExpiredTickets(uid).flatMapLatest { enrichTickets(it) } }
      ?: flowOf(emptyList()))
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  // ---------- Initialization ----------

  init {
    // Load cached QR at startup for offline support
    viewModelScope.launch { loadCachedQr() }
    // Observe tickets and update UI state
    observeCurrentTickets()
    observeExpiredTickets()
  }

  // ---------- Tickets Observation ----------

  /** Observes current tickets and updates UI state */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeCurrentTickets() {
    val uid = userId ?: return
    ticketRepo
      .getActiveTickets(uid)
      .flatMapLatest { enrichTickets(it) }
      .onEach { tickets -> _uiState.value = _uiState.value.copy(currentTickets = tickets) }
      .launchIn(viewModelScope)
  }

  /** Observes expired tickets and updates the UI state accordingly */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeExpiredTickets() {
    val uid = userId ?: return
    ticketRepo
      .getExpiredTickets(uid)
      .flatMapLatest { enrichTickets(it) }
      .onEach { tickets -> _uiState.value = _uiState.value.copy(expiredTickets = tickets) }
      .launchIn(viewModelScope)
  }

  // ---------- UI Actions ----------

  /**
   * Selects a tab in the UI.
   *
   * @param tab The tab to select (CURRENT or EXPIRED).
   */
  fun selectTab(tab: TicketTab) {
    _uiState.value = _uiState.value.copy(selectedTab = tab)
  }

  /** Toggles the expansion state of the user's QR code. */
  fun toggleQrExpansion() {
    _uiState.value = _uiState.value.copy(isQrExpanded = !_uiState.value.isQrExpanded)
  }

  // ---------- Ticket Enrichment ----------

  /**
   * Enriches a list of tickets with their associated event data.
   *
   * @param tickets List of tickets to enrich.
   * @return Flow emitting a list of enriched tickets.
   */
  private fun enrichTickets(
    tickets: List<ch.onepass.onepass.model.ticket.Ticket>
  ): Flow<List<Ticket>> {
    if (tickets.isEmpty()) return flowOf(emptyList())
    return combine(
      tickets.map { ticket ->
        eventRepo.getEventById(ticket.eventId).map { event -> ticket.toUiTicket(event) }
      }) {
      it.toList()
    }
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