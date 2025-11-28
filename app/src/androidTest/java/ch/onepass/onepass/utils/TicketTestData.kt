package ch.onepass.onepass.utils

import ch.onepass.onepass.model.ticket.Ticket
import ch.onepass.onepass.model.ticket.TicketState
import com.google.firebase.Timestamp
import java.util.Calendar

/** Utility object to create test data for Ticket-related unit tests. */
object TicketTestData {
  /** Creates a Ticket instance with specified or default parameters. */
  fun createTestTicket(
      ticketId: String = "ticket_${System.currentTimeMillis()}",
      eventId: String = "event_test_1",
      ownerId: String = "user_test_1",
      state: TicketState = TicketState.ISSUED,
      tierId: String = "general",
      purchasePrice: Double = 25.0,
      issuedAt: Timestamp? = Timestamp.Companion.now(),
      expiresAt: Timestamp? = createFutureTimestamp(daysFromNow = 7),
      transferLock: Boolean = false,
      version: Int = 1,
      deletedAt: Timestamp? = null
  ): Ticket {
    return Ticket(
        ticketId = ticketId,
        eventId = eventId,
        ownerId = ownerId,
        state = state,
        tierId = tierId,
        purchasePrice = purchasePrice,
        issuedAt = issuedAt,
        expiresAt = expiresAt,
        transferLock = transferLock,
        version = version,
        deletedAt = deletedAt)
  }

  /** Creates a list of tickets covering all possible states for a given owner. */
  fun createTicketsWithDifferentStates(ownerId: String): List<Ticket> {
    return listOf(
        createTestTicket(ticketId = "ticket_issued", ownerId = ownerId, state = TicketState.ISSUED),
        createTestTicket(ticketId = "ticket_listed", ownerId = ownerId, state = TicketState.LISTED),
        createTestTicket(
            ticketId = "ticket_transferred", ownerId = ownerId, state = TicketState.TRANSFERRED),
        createTestTicket(
            ticketId = "ticket_redeemed", ownerId = ownerId, state = TicketState.REDEEMED),
        createTestTicket(
            ticketId = "ticket_revoked", ownerId = ownerId, state = TicketState.REVOKED))
  }

  /** Creates tickets for multiple users to test user-specific queries. */
  fun createTicketsForMultipleUsers(): List<Ticket> {
    return listOf(
        createTestTicket(
            ticketId = "ticket_user1_1", ownerId = "user_1", state = TicketState.ISSUED),
        createTestTicket(
            ticketId = "ticket_user1_2", ownerId = "user_1", state = TicketState.LISTED),
        createTestTicket(
            ticketId = "ticket_user2_1", ownerId = "user_2", state = TicketState.ISSUED),
        createTestTicket(
            ticketId = "ticket_user2_2", ownerId = "user_2", state = TicketState.REDEEMED))
  }

  /** Creates tickets with varying purchase prices for a given owner. */
  fun createTicketsWithDifferentPrices(ownerId: String): List<Ticket> {
    return listOf(
        createTestTicket(ticketId = "ticket_free", ownerId = ownerId, purchasePrice = 0.0),
        createTestTicket(ticketId = "ticket_cheap", ownerId = ownerId, purchasePrice = 15.0),
        createTestTicket(ticketId = "ticket_expensive", ownerId = ownerId, purchasePrice = 100.0))
  }

  /** Creates a future timestamp by adding specified days to the current date. */
  fun createFutureTimestamp(daysFromNow: Int): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, daysFromNow)
    return Timestamp(calendar.time)
  }

  /** Creates a past timestamp by subtracting specified days from the current date. */
  fun createPastTimestamp(daysAgo: Int): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return Timestamp(calendar.time)
  }
}
