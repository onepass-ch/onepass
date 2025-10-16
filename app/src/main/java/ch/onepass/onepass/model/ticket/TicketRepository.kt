package ch.onepass.onepass.model.ticket

import kotlinx.coroutines.flow.Flow

/** Repository interface defining operations for managing tickets. */
interface TicketRepository {
  /** Get all tickets for a specific user (the owner). */
  fun getTicketsByUser(userId: String): Flow<List<Ticket>>

  /** Get only active (not expired/redeemed/revoked) tickets. */
  fun getActiveTickets(userId: String): Flow<List<Ticket>>

  /** Get expired or redeemed tickets. */
  fun getExpiredTickets(userId: String): Flow<List<Ticket>>

  /** Get a single ticket by ID. */
  fun getTicketById(ticketId: String): Flow<Ticket?>

  /** Create a ticket document (when purchased). */
  suspend fun createTicket(ticket: Ticket): Result<String>

  /** Update ticket (e.g., state change). */
  suspend fun updateTicket(ticket: Ticket): Result<Unit>

  /** Delete ticket (soft delete). */
  suspend fun deleteTicket(ticketId: String): Result<Unit>
}
