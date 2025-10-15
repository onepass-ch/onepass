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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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

  /**
   * Enriches a list of tickets with their associated event details.
   *
   * @param tickets List of tickets to enrich.
   * @return A Flow emitting a list of enriched tickets with event details.
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

  /** StateFlow of the user's current (active) tickets enriched with event details. */
  val currentTickets: StateFlow<List<Ticket>> =
      ticketRepo
          .getActiveTickets(userId)
          .flatMapLatest { enrichTickets(it) }
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  /** StateFlow of the user's expired or redeemed tickets enriched with event details. */
  val expiredTickets: StateFlow<List<Ticket>> =
      ticketRepo
          .getExpiredTickets(userId)
          .flatMapLatest { enrichTickets(it) }
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
