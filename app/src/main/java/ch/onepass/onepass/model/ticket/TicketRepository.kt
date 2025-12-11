package ch.onepass.onepass.model.ticket

import kotlinx.coroutines.flow.Flow

/** Repository interface defining operations for managing tickets. */
interface TicketRepository {
  /** Get all tickets for a specific user (the owner). */
  fun getTicketsByUser(userId: String): Flow<List<Ticket>>

  /** Get only active (not expired/redeemed/revoked/listed) tickets that can be used. */
  fun getActiveTickets(userId: String): Flow<List<Ticket>>

  /** Get expired or redeemed tickets. */
  fun getExpiredTickets(userId: String): Flow<List<Ticket>>

  /**
   * Get tickets that the user has listed for sale.
   *
   * @param userId The user's unique ID.
   * @return A [Flow] emitting a list of the user's listed tickets.
   */
  fun getListedTicketsByUser(userId: String): Flow<List<Ticket>>

  /** Get a single ticket by ID. */
  fun getTicketById(ticketId: String): Flow<Ticket?>

  /** Create a ticket document (when purchased). */
  suspend fun createTicket(ticket: Ticket): Result<String>

  /** Update ticket (e.g., state change). */
  suspend fun updateTicket(ticket: Ticket): Result<Unit>

  /** Delete ticket (soft delete). */
  suspend fun deleteTicket(ticketId: String): Result<Unit>

  // -------------------- Market Operations --------------------

  /**
   * Get all tickets listed for sale on the marketplace.
   *
   * @return A [Flow] emitting a list of all listed tickets.
   */
  fun getListedTickets(): Flow<List<Ticket>>

  /**
   * Get listed tickets for a specific event.
   *
   * @param eventId The event's unique ID.
   * @return A [Flow] emitting a list of listed tickets for the event.
   */
  fun getListedTicketsByEvent(eventId: String): Flow<List<Ticket>>

  /**
   * List a ticket for sale on the marketplace.
   *
   * @param ticketId The ticket's unique ID.
   * @param askingPrice The price at which to list the ticket.
   * @return A [Result] indicating success or failure.
   */
  suspend fun listTicketForSale(ticketId: String, askingPrice: Double): Result<Unit>

  /**
   * Cancel a ticket listing and return it to ISSUED state.
   *
   * @param ticketId The ticket's unique ID.
   * @return A [Result] indicating success or failure.
   */
  suspend fun cancelTicketListing(ticketId: String): Result<Unit>

  /**
   * Purchase a listed ticket from the marketplace.
   *
   * Transfers ownership from seller to buyer and updates ticket state.
   *
   * @param ticketId The ticket's unique ID.
   * @param buyerId The buyer's user ID.
   * @return A [Result] indicating success or failure.
   */
  suspend fun purchaseListedTicket(ticketId: String, buyerId: String): Result<Unit>
}
