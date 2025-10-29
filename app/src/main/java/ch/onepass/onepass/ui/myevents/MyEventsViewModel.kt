package ch.onepass.onepass.ui.myevents

import android.app.Application
import android.content.Context
import androidx.annotation.ColorRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
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

// ---------- DataStore extension (top-level) ----------
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
 * - Manages tickets (current & expired) enriched with their events.
 * - Manages the user's pass (QR), with DataStore cache and offline fallback.
 *
 * @param app Application for DataStore (AndroidViewModel)
 * @param passRepository Repository for signed passes
 * @param ticketRepo Repository for tickets (Firebase by default)
 * @param eventRepo Repository for events (Firebase by default)
 * @param userId Optional: if null, FirebaseAuth.currentUser?.uid is used
 */
class MyEventsViewModel(
    app: Application,
    private val passRepository: PassRepository,
    private val ticketRepo: TicketRepository = TicketRepositoryFirebase(),
    private val eventRepo: EventRepository = EventRepositoryFirebase(),
    private val userId: String? = null
) : AndroidViewModel(app) {

    // ---------- QR / Pass state ----------
    companion object {
        // Per-user cache key to avoid leaks between users/tests.
        private fun qrKey(uid: String) = stringPreferencesKey("cached_qr_text_$uid")
    }

    private val _userQrData = MutableStateFlow<String?>(null)
    val userQrData: StateFlow<String?> = _userQrData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ---------- Tickets state ----------
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

    private val resolvedUserId: String?
        get() = userId ?: FirebaseAuth.getInstance().currentUser?.uid

    /** Enriched active tickets */
    val currentTickets: StateFlow<List<Ticket>> =
        (resolvedUserId?.let { uid ->
            ticketRepo.getActiveTickets(uid).flatMapLatest { enrichTickets(it) }
        } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Enriched expired tickets */
    val expiredTickets: StateFlow<List<Ticket>> =
        (resolvedUserId?.let { uid ->
            ticketRepo.getExpiredTickets(uid).flatMapLatest { enrichTickets(it) }
        } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ---------- Init ----------
    init {
        // Load cached QR at startup (offline support)
        viewModelScope.launch { loadCachedQr() }
    }

    // ---------- Pass / QR API ----------
    fun loadUserPass() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val authUid = resolvedUserId
                require(!authUid.isNullOrBlank()) { "Not authenticated" }

                val result = passRepository.getOrCreateSignedPass(authUid)
                result
                    .onSuccess { pass ->
                        _userQrData.value = pass.qrText
                        saveCachedQr(pass.qrText)
                    }
                    .onFailure { e ->
                        _error.value = e.message ?: "Unknown error"
                        loadCachedQr() // cache fallback
                    }
            } catch (t: Throwable) {
                _error.value = t.message ?: "Authentication error"
                loadCachedQr()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Handy alias to reload the pass. */
    fun refreshPass() = loadUserPass()

    // ---------- DataStore helpers ----------
    // Note: no withContext(Dispatchers.IO). This makes tests deterministic with TestDispatcher.
    private suspend fun saveCachedQr(qrText: String) {
        val uid = resolvedUserId ?: return
        getApplication<Application>().passDataStore.edit { prefs ->
            prefs[qrKey(uid)] = qrText
        }
    }

    private suspend fun loadCachedQr() {
        val uid = resolvedUserId ?: run {
            _userQrData.value = null
            return
        }
        val prefs = getApplication<Application>().passDataStore.data.first()
        _userQrData.value = prefs[qrKey(uid)]
    }
}
