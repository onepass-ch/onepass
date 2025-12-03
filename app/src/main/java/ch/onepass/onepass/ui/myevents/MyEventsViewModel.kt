package ch.onepass.onepass.ui.myevents

import androidx.annotation.ColorRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.ticket.TicketRepository
import ch.onepass.onepass.model.ticket.TicketRepositoryFirebase
import ch.onepass.onepass.model.ticket.toUiTicket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Data class representing a ticket in the UI.
 *
 * @param ticketId Unique identifier for the ticket.
 * @param title The title of the event.
 * @param status The status of the ticket (e.g., CURRENTLY, UPCOMING, EXPIRED).
 * @param dateTime The display date and time of the event.
 * @param location The display location of the event.
 */
data class Ticket(
    val ticketId: String,
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
 * @param ticketRepo The repository for ticket data (default is Firebase implementation).
 * @param eventRepo The repository for event data (default is Firebase implementation).
 * @param userId The ID of the current user whose tickets are being managed.
 */
class MyEventsViewModel(
    private val ticketRepo: TicketRepository = TicketRepositoryFirebase(),
    private val eventRepo: EventRepository = EventRepositoryFirebase(),
    private val userId: String
) : ViewModel() {

  /** Backing state for the UI state flow */
  private val _uiState = MutableStateFlow(MyEventsUiState())
  /** Publicly exposed UI state as a StateFlow */
  val uiState: StateFlow<MyEventsUiState> = _uiState

  /** Initializes the ViewModel by observing current and expired tickets */
  init {
    observeCurrentTickets()
    observeExpiredTickets()
  }

  /** Observes current tickets and updates UI state */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeCurrentTickets() {
    ticketRepo
        .getActiveTickets(userId)
        .flatMapLatest { enrichTickets(it) }
        .onEach { tickets -> _uiState.value = _uiState.value.copy(currentTickets = tickets) }
        .launchIn(viewModelScope)
  }

  /** Observes expired tickets and updates the UI state accordingly */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeExpiredTickets() {
    ticketRepo
        .getExpiredTickets(userId)
        .flatMapLatest { enrichTickets(it) }
        .onEach { tickets -> _uiState.value = _uiState.value.copy(expiredTickets = tickets) }
        .launchIn(viewModelScope)
  }

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

  /**
   * Enriches a list of tickets with their associated event data.
   *
   * @param tickets List of tickets to enrich.
   * @return Flow emitting a list of enriched tickets.
   */
  private fun enrichTickets(
      tickets: List<ch.onepass.onepass.model.ticket.Ticket>
  ): Flow<List<Ticket>> {
    return combine(
        tickets.map { ticket ->
          eventRepo.getEventById(ticket.eventId).map { event -> ticket.toUiTicket(event) }
        }) {
          it.toList()
        }
  }
}
