package ch.onepass.onepass.ui.myevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

object MyEventsTestTags {
  const val TABS_ROW = "TabsRow"
  const val TAB_CURRENT = "TabCurrent"
  const val TAB_EXPIRED = "TabExpired"
  const val QR_CODE_ICON = "QrCodeIcon"
  const val QR_CODE_DIALOG = "QrCodeDialog"
  const val TICKET_CARD = "TicketCard"
  const val TICKET_TITLE = "TicketTitle"
  const val TICKET_STATUS = "TicketStatus"
  const val TICKET_DATE = "TicketDate"
  const val TICKET_LOCATION = "TicketLocation"
  const val TICKET_DIALOG_TITLE = "TicketDialogTitle"
  const val TICKET_DIALOG_STATUS = "TicketDialogStatus"
  const val TICKET_DIALOG_DATE = "TicketDialogDate"
  const val TICKET_DIALOG_LOCATION = "TicketDialogLocation"
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
