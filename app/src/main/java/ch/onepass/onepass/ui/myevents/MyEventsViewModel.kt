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

data class Ticket(
    val title: String,
    val status: TicketStatus,
    val dateTime: String,
    val location: String,
)

enum class TicketStatus(@ColorRes val colorRes: Int) {
  CURRENTLY(R.color.status_currently),
  UPCOMING(R.color.status_upcoming),
  EXPIRED(R.color.status_expired)
}

class MyEventsViewModel(
    private val ticketRepo: TicketRepository = TicketRepositoryFirebase(),
    private val eventRepo: EventRepository = EventRepositoryFirebase(),
    private val userId: String
) : ViewModel() {

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

  val currentTickets: StateFlow<List<Ticket>> =
      ticketRepo
          .getActiveTickets(userId)
          .flatMapLatest { enrichTickets(it) }
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val expiredTickets: StateFlow<List<Ticket>> =
      ticketRepo
          .getExpiredTickets(userId)
          .flatMapLatest { enrichTickets(it) }
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
