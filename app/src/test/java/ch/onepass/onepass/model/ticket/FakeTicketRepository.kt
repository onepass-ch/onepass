package ch.onepass.onepass.model.ticket

import com.google.firebase.Timestamp
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of TicketRepository for testing purposes.
 *
 * This class provides a simple in-memory implementation that can be used in tests without requiring
 * Firebase or network connectivity.
 *
 * Usage:
 * ```
 * val fakeRepository = FakeTicketRepository()
 *
 * // Add test tickets
 * fakeRepository.setTickets(listOf(ticket1, ticket2))
 *
 * // Configure failure scenarios
 * fakeRepository.setThrowOnCreate(true)
 * ```
 */
class FakeTicketRepository(initialTickets: List<Ticket> = emptyList()) : TicketRepository {

  private val ticketsFlow = MutableStateFlow(initialTickets.toList())
  private var throwOnCreate = false
  private var throwOnUpdate = false
  private var throwOnDelete = false
  private var throwOnListForSale = false
  private var throwOnCancelListing = false
  private var throwOnPurchase = false

  /** Set the list of tickets in the repository. */
  fun setTickets(tickets: List<Ticket>) {
    ticketsFlow.value = tickets.toList()
  }

  /** Add a single ticket to the repository. */
  fun addTicket(ticket: Ticket) {
    ticketsFlow.value = ticketsFlow.value + ticket
  }

  /** Reset the repository to its initial state. */
  fun reset() {
    ticketsFlow.value = emptyList()
    throwOnCreate = false
    throwOnUpdate = false
    throwOnDelete = false
    throwOnListForSale = false
    throwOnCancelListing = false
    throwOnPurchase = false
  }

  /** Configure whether createTicket should throw an error. */
  fun setThrowOnCreate(value: Boolean) {
    throwOnCreate = value
  }

  /** Configure whether updateTicket should throw an error. */
  fun setThrowOnUpdate(value: Boolean) {
    throwOnUpdate = value
  }

  /** Configure whether deleteTicket should throw an error. */
  fun setThrowOnDelete(value: Boolean) {
    throwOnDelete = value
  }

  /** Configure whether listTicketForSale should throw an error. */
  fun setThrowOnListForSale(value: Boolean) {
    throwOnListForSale = value
  }

  /** Configure whether cancelTicketListing should throw an error. */
  fun setThrowOnCancelListing(value: Boolean) {
    throwOnCancelListing = value
  }

  /** Configure whether purchaseListedTicket should throw an error. */
  fun setThrowOnPurchase(value: Boolean) {
    throwOnPurchase = value
  }

  override fun getTicketsByUser(userId: String): Flow<List<Ticket>> =
      ticketsFlow.map { tickets ->
        tickets
            .filter { it.ownerId == userId }
            .sortedByDescending { it.issuedAt?.seconds ?: 0 }
      }

  override fun getActiveTickets(userId: String): Flow<List<Ticket>> =
      ticketsFlow.map { tickets ->
        val now = Timestamp.now()
        tickets
            .filter { ticket ->
              val expiryTime = ticket.expiresAt
              ticket.ownerId == userId &&
                  ticket.state in listOf(TicketState.ISSUED, TicketState.TRANSFERRED) &&
                  (expiryTime == null || now.seconds <= expiryTime.seconds)
            }
            .sortedByDescending { it.issuedAt?.seconds ?: 0 }
      }

  override fun getExpiredTickets(userId: String): Flow<List<Ticket>> =
      ticketsFlow.map { tickets ->
        val now = Timestamp.now()
        tickets
            .filter { ticket ->
              val expiryTime = ticket.expiresAt
              ticket.ownerId == userId &&
                  (ticket.state in listOf(TicketState.REDEEMED, TicketState.REVOKED) ||
                      (expiryTime != null && now.seconds > expiryTime.seconds))
            }
            .sortedByDescending { it.issuedAt?.seconds ?: 0 }
      }

  override fun getListedTicketsByUser(userId: String): Flow<List<Ticket>> =
      ticketsFlow.map { tickets ->
        tickets
            .filter { ticket -> ticket.ownerId == userId && ticket.state == TicketState.LISTED }
            .sortedByDescending { it.listedAt?.seconds ?: 0 }
      }

  override fun getTicketById(ticketId: String): Flow<Ticket?> =
      ticketsFlow.map { tickets -> tickets.find { it.ticketId == ticketId } }

  override suspend fun createTicket(ticket: Ticket): Result<String> {
    if (throwOnCreate) return Result.failure(RuntimeException("Failed to create ticket"))

    val id = ticket.ticketId.ifEmpty { UUID.randomUUID().toString() }
    val toStore =
        ticket.copy(ticketId = id, issuedAt = ticket.issuedAt ?: Timestamp.now())
    ticketsFlow.value = ticketsFlow.value + toStore
    return Result.success(id)
  }

  override suspend fun updateTicket(ticket: Ticket): Result<Unit> {
    if (throwOnUpdate) return Result.failure(RuntimeException("Failed to update ticket"))

    val found = ticketsFlow.value.any { it.ticketId == ticket.ticketId }
    return if (!found) {
      Result.failure(IllegalArgumentException("Ticket not found: ${ticket.ticketId}"))
    } else {
      ticketsFlow.value =
          ticketsFlow.value.map { if (it.ticketId == ticket.ticketId) ticket else it }
      Result.success(Unit)
    }
  }

  override suspend fun deleteTicket(ticketId: String): Result<Unit> {
    if (throwOnDelete) return Result.failure(RuntimeException("Failed to delete ticket"))

    val ticket = ticketsFlow.value.find { it.ticketId == ticketId }
    return if (ticket == null) {
      Result.failure(IllegalArgumentException("Ticket not found: $ticketId"))
    } else {
      ticketsFlow.value =
          ticketsFlow.value.map {
            if (it.ticketId == ticketId) it.copy(deletedAt = Timestamp.now()) else it
          }
      Result.success(Unit)
    }
  }

  // -------------------- Market Operations --------------------

  override fun getListedTickets(): Flow<List<Ticket>> =
      ticketsFlow.map { tickets ->
        tickets
            .filter { it.state == TicketState.LISTED && it.listingPrice != null }
            .sortedByDescending { it.listedAt?.seconds ?: 0 }
      }

  override fun getListedTicketsByEvent(eventId: String): Flow<List<Ticket>> =
      ticketsFlow.map { tickets ->
        tickets
            .filter {
              it.state == TicketState.LISTED && it.listingPrice != null && it.eventId == eventId
            }
            .sortedBy { it.listingPrice }
      }

  override suspend fun listTicketForSale(ticketId: String, askingPrice: Double): Result<Unit> {
    if (throwOnListForSale) return Result.failure(RuntimeException("Failed to list ticket"))

    if (askingPrice <= 0) {
      return Result.failure(IllegalArgumentException("Asking price must be positive"))
    }

    val ticket = ticketsFlow.value.find { it.ticketId == ticketId }
    return when {
      ticket == null -> Result.failure(IllegalArgumentException("Ticket not found: $ticketId"))
      ticket.state != TicketState.ISSUED ->
          Result.failure(IllegalStateException("Ticket must be in ISSUED state to be listed"))
      ticket.transferLock ->
          Result.failure(IllegalStateException("Ticket is locked and cannot be listed"))
      else -> {
        val updatedTicket =
            ticket.copy(
                state = TicketState.LISTED, listingPrice = askingPrice, listedAt = Timestamp.now())
        ticketsFlow.value =
            ticketsFlow.value.map { if (it.ticketId == ticketId) updatedTicket else it }
        Result.success(Unit)
      }
    }
  }

  override suspend fun cancelTicketListing(ticketId: String): Result<Unit> {
    if (throwOnCancelListing) return Result.failure(RuntimeException("Failed to cancel listing"))

    val ticket = ticketsFlow.value.find { it.ticketId == ticketId }
    return when {
      ticket == null -> Result.failure(IllegalArgumentException("Ticket not found: $ticketId"))
      ticket.state != TicketState.LISTED ->
          Result.failure(IllegalStateException("Ticket is not currently listed"))
      else -> {
        val updatedTicket =
            ticket.copy(state = TicketState.ISSUED, listingPrice = null, listedAt = null)
        ticketsFlow.value =
            ticketsFlow.value.map { if (it.ticketId == ticketId) updatedTicket else it }
        Result.success(Unit)
      }
    }
  }

  override suspend fun purchaseListedTicket(ticketId: String, buyerId: String): Result<Unit> {
    if (throwOnPurchase) return Result.failure(RuntimeException("Failed to purchase ticket"))

    val ticket = ticketsFlow.value.find { it.ticketId == ticketId }
    return when {
      ticket == null -> Result.failure(IllegalArgumentException("Ticket not found"))
      ticket.state != TicketState.LISTED ->
          Result.failure(IllegalStateException("Ticket is not listed for sale"))
      ticket.ownerId == buyerId ->
          Result.failure(IllegalArgumentException("Cannot purchase your own ticket"))
      else -> {
        val updatedTicket =
            ticket.copy(
                ownerId = buyerId,
                state = TicketState.TRANSFERRED,
                listingPrice = null,
                listedAt = null,
                version = ticket.version + 1)
        ticketsFlow.value =
            ticketsFlow.value.map { if (it.ticketId == ticketId) updatedTicket else it }
        Result.success(Unit)
      }
    }
  }
}
