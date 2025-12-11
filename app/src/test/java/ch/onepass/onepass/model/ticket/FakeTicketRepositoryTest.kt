package ch.onepass.onepass.model.ticket

import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FakeTicketRepository.
 *
 * These tests verify the fake repository implementation and demonstrate its usage.
 */
class FakeTicketRepositoryTest {

  private lateinit var repository: FakeTicketRepository
  private val testUserId = "test-user-123"
  private val testEventId = "test-event-456"

  @Before
  fun setUp() {
    repository = FakeTicketRepository()
  }

  private fun createTestTicket(
      ticketId: String = "ticket-${System.nanoTime()}",
      eventId: String = testEventId,
      ownerId: String = testUserId,
      state: TicketState = TicketState.ISSUED,
      purchasePrice: Double = 50.0,
      listingPrice: Double? = null,
      listedAt: Timestamp? = null,
      expiresAt: Timestamp? = null,
      transferLock: Boolean = false
  ): Ticket {
    return Ticket(
        ticketId = ticketId,
        eventId = eventId,
        ownerId = ownerId,
        state = state,
        tierId = "general",
        purchasePrice = purchasePrice,
        issuedAt = Timestamp.now(),
        listingPrice = listingPrice,
        listedAt = listedAt,
        expiresAt = expiresAt,
        transferLock = transferLock)
  }

  // -------------------- Basic Operations Tests --------------------

  @Test
  fun createTicket_success_returnsTicketId() = runTest {
    val ticket = createTestTicket(ticketId = "")

    val result = repository.createTicket(ticket)

    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull())

    val tickets = repository.getTicketsByUser(testUserId).first()
    assertEquals(1, tickets.size)
  }

  @Test
  fun createTicket_withExplicitId_usesProvidedId() = runTest {
    val ticket = createTestTicket(ticketId = "explicit-id-123")

    val result = repository.createTicket(ticket)

    assertTrue(result.isSuccess)
    assertEquals("explicit-id-123", result.getOrNull())

    val savedTicket = repository.getTicketById("explicit-id-123").first()
    assertNotNull(savedTicket)
    assertEquals("explicit-id-123", savedTicket?.ticketId)
  }

  @Test
  fun createTicket_failure_returnsError() = runTest {
    repository.setThrowOnCreate(true)
    val ticket = createTestTicket()

    val result = repository.createTicket(ticket)

    assertTrue(result.isFailure)
    assertEquals("Failed to create ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun updateTicket_success() = runTest {
    val ticket = createTestTicket(ticketId = "ticket-1")
    repository.addTicket(ticket)

    val updatedTicket = ticket.copy(state = TicketState.REDEEMED, version = 2)
    val result = repository.updateTicket(updatedTicket)

    assertTrue(result.isSuccess)

    val savedTicket = repository.getTicketById("ticket-1").first()
    assertEquals(TicketState.REDEEMED, savedTicket?.state)
    assertEquals(2, savedTicket?.version)
  }

  @Test
  fun updateTicket_notFound_returnsError() = runTest {
    val ticket = createTestTicket(ticketId = "non-existent")

    val result = repository.updateTicket(ticket)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
  }

  @Test
  fun deleteTicket_success_setsDeletedAt() = runTest {
    val ticket = createTestTicket(ticketId = "ticket-1")
    repository.addTicket(ticket)

    val result = repository.deleteTicket("ticket-1")

    assertTrue(result.isSuccess)

    val deletedTicket = repository.getTicketById("ticket-1").first()
    assertNotNull(deletedTicket?.deletedAt)
  }

  @Test
  fun deleteTicket_notFound_returnsError() = runTest {
    val result = repository.deleteTicket("non-existent")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  // -------------------- Query Tests --------------------

  @Test
  fun getTicketsByUser_returnsUserTickets() = runTest {
    val ticket1 = createTestTicket(ticketId = "ticket-1", ownerId = testUserId)
    val ticket2 = createTestTicket(ticketId = "ticket-2", ownerId = testUserId)
    val ticket3 = createTestTicket(ticketId = "ticket-3", ownerId = "other-user")

    repository.setTickets(listOf(ticket1, ticket2, ticket3))

    val tickets = repository.getTicketsByUser(testUserId).first()

    assertEquals(2, tickets.size)
    assertTrue(tickets.all { it.ownerId == testUserId })
  }

  @Test
  fun getActiveTickets_returnsOnlyActiveTickets() = runTest {
    val activeTicket = createTestTicket(ticketId = "active", state = TicketState.ISSUED)
    val listedTicket = createTestTicket(ticketId = "listed", state = TicketState.LISTED)
    val redeemedTicket = createTestTicket(ticketId = "redeemed", state = TicketState.REDEEMED)
    val expiredTicket =
        createTestTicket(
            ticketId = "expired",
            state = TicketState.ISSUED,
            expiresAt = Timestamp(Timestamp.now().seconds - 3600, 0))

    repository.setTickets(listOf(activeTicket, listedTicket, redeemedTicket, expiredTicket))

    val tickets = repository.getActiveTickets(testUserId).first()

    assertEquals(1, tickets.size)
    assertEquals("active", tickets[0].ticketId)
  }

  @Test
  fun getExpiredTickets_returnsExpiredAndRedeemedTickets() = runTest {
    val activeTicket = createTestTicket(ticketId = "active", state = TicketState.ISSUED)
    val redeemedTicket = createTestTicket(ticketId = "redeemed", state = TicketState.REDEEMED)
    val revokedTicket = createTestTicket(ticketId = "revoked", state = TicketState.REVOKED)
    val expiredTicket =
        createTestTicket(
            ticketId = "expired",
            state = TicketState.ISSUED,
            expiresAt = Timestamp(Timestamp.now().seconds - 3600, 0))

    repository.setTickets(listOf(activeTicket, redeemedTicket, revokedTicket, expiredTicket))

    val tickets = repository.getExpiredTickets(testUserId).first()

    assertEquals(3, tickets.size)
    assertTrue(tickets.any { it.ticketId == "redeemed" })
    assertTrue(tickets.any { it.ticketId == "revoked" })
    assertTrue(tickets.any { it.ticketId == "expired" })
    assertFalse(tickets.any { it.ticketId == "active" })
  }

  @Test
  fun getListedTicketsByUser_returnsUserListedTickets() = runTest {
    val listedTicket1 =
        createTestTicket(
            ticketId = "listed-1",
            state = TicketState.LISTED,
            listingPrice = 100.0,
            listedAt = Timestamp.now())
    val listedTicket2 =
        createTestTicket(
            ticketId = "listed-2",
            state = TicketState.LISTED,
            listingPrice = 150.0,
            listedAt = Timestamp.now())
    val activeTicket = createTestTicket(ticketId = "active", state = TicketState.ISSUED)

    repository.setTickets(listOf(listedTicket1, listedTicket2, activeTicket))

    val tickets = repository.getListedTicketsByUser(testUserId).first()

    assertEquals(2, tickets.size)
    assertTrue(tickets.all { it.state == TicketState.LISTED })
  }

  @Test
  fun getTicketById_returnsCorrectTicket() = runTest {
    val ticket = createTestTicket(ticketId = "specific-ticket")
    repository.addTicket(ticket)

    val result = repository.getTicketById("specific-ticket").first()

    assertNotNull(result)
    assertEquals("specific-ticket", result?.ticketId)
  }

  @Test
  fun getTicketById_notFound_returnsNull() = runTest {
    val result = repository.getTicketById("non-existent").first()

    assertNull(result)
  }

  // -------------------- Market Operations Tests --------------------

  @Test
  fun getListedTickets_returnsAllListedTickets() = runTest {
    val listed1 =
        createTestTicket(
            ticketId = "listed-1",
            ownerId = "user-1",
            state = TicketState.LISTED,
            listingPrice = 100.0,
            listedAt = Timestamp.now())
    val listed2 =
        createTestTicket(
            ticketId = "listed-2",
            ownerId = "user-2",
            state = TicketState.LISTED,
            listingPrice = 150.0,
            listedAt = Timestamp.now())
    val active = createTestTicket(ticketId = "active", state = TicketState.ISSUED)

    repository.setTickets(listOf(listed1, listed2, active))

    val tickets = repository.getListedTickets().first()

    assertEquals(2, tickets.size)
    assertTrue(tickets.all { it.state == TicketState.LISTED })
  }

  @Test
  fun getListedTicketsByEvent_returnsListedTicketsForEvent() = runTest {
    val event1Listed =
        createTestTicket(
            ticketId = "event1-listed",
            eventId = "event-1",
            state = TicketState.LISTED,
            listingPrice = 100.0,
            listedAt = Timestamp.now())
    val event2Listed =
        createTestTicket(
            ticketId = "event2-listed",
            eventId = "event-2",
            state = TicketState.LISTED,
            listingPrice = 150.0,
            listedAt = Timestamp.now())

    repository.setTickets(listOf(event1Listed, event2Listed))

    val tickets = repository.getListedTicketsByEvent("event-1").first()

    assertEquals(1, tickets.size)
    assertEquals("event-1", tickets[0].eventId)
  }

  @Test
  fun getListedTicketsByEvent_sortedByPrice() = runTest {
    val expensive =
        createTestTicket(
            ticketId = "expensive",
            state = TicketState.LISTED,
            listingPrice = 200.0,
            listedAt = Timestamp.now())
    val cheap =
        createTestTicket(
            ticketId = "cheap",
            state = TicketState.LISTED,
            listingPrice = 50.0,
            listedAt = Timestamp.now())
    val medium =
        createTestTicket(
            ticketId = "medium",
            state = TicketState.LISTED,
            listingPrice = 100.0,
            listedAt = Timestamp.now())

    repository.setTickets(listOf(expensive, cheap, medium))

    val tickets = repository.getListedTicketsByEvent(testEventId).first()

    assertEquals(3, tickets.size)
    assertEquals("cheap", tickets[0].ticketId)
    assertEquals("medium", tickets[1].ticketId)
    assertEquals("expensive", tickets[2].ticketId)
  }

  @Test
  fun listTicketForSale_success() = runTest {
    val ticket = createTestTicket(ticketId = "ticket-1", state = TicketState.ISSUED)
    repository.addTicket(ticket)

    val result = repository.listTicketForSale("ticket-1", 100.0)

    assertTrue(result.isSuccess)

    val listedTicket = repository.getTicketById("ticket-1").first()
    assertEquals(TicketState.LISTED, listedTicket?.state)
    assertEquals(100.0, listedTicket?.listingPrice)
    assertNotNull(listedTicket?.listedAt)
  }

  @Test
  fun listTicketForSale_negativePrice_returnsError() = runTest {
    val ticket = createTestTicket(ticketId = "ticket-1")
    repository.addTicket(ticket)

    val result = repository.listTicketForSale("ticket-1", -50.0)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("positive") == true)
  }

  @Test
  fun listTicketForSale_notIssued_returnsError() = runTest {
    val ticket = createTestTicket(ticketId = "ticket-1", state = TicketState.REDEEMED)
    repository.addTicket(ticket)

    val result = repository.listTicketForSale("ticket-1", 100.0)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("ISSUED") == true)
  }

  @Test
  fun listTicketForSale_transferLocked_returnsError() = runTest {
    val ticket =
        createTestTicket(ticketId = "ticket-1", state = TicketState.ISSUED, transferLock = true)
    repository.addTicket(ticket)

    val result = repository.listTicketForSale("ticket-1", 100.0)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("locked") == true)
  }

  @Test
  fun cancelTicketListing_success() = runTest {
    val ticket =
        createTestTicket(
            ticketId = "ticket-1",
            state = TicketState.LISTED,
            listingPrice = 100.0,
            listedAt = Timestamp.now())
    repository.addTicket(ticket)

    val result = repository.cancelTicketListing("ticket-1")

    assertTrue(result.isSuccess)

    val cancelledTicket = repository.getTicketById("ticket-1").first()
    assertEquals(TicketState.ISSUED, cancelledTicket?.state)
    assertNull(cancelledTicket?.listingPrice)
    assertNull(cancelledTicket?.listedAt)
  }

  @Test
  fun cancelTicketListing_notListed_returnsError() = runTest {
    val ticket = createTestTicket(ticketId = "ticket-1", state = TicketState.ISSUED)
    repository.addTicket(ticket)

    val result = repository.cancelTicketListing("ticket-1")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("not currently listed") == true)
  }

  @Test
  fun purchaseListedTicket_success() = runTest {
    val ticket =
        createTestTicket(
            ticketId = "ticket-1",
            ownerId = "seller-123",
            state = TicketState.LISTED,
            listingPrice = 100.0,
            listedAt = Timestamp.now())
    repository.addTicket(ticket)

    val buyerId = "buyer-456"
    val result = repository.purchaseListedTicket("ticket-1", buyerId)

    assertTrue(result.isSuccess)

    val purchasedTicket = repository.getTicketById("ticket-1").first()
    assertEquals(buyerId, purchasedTicket?.ownerId)
    assertEquals(TicketState.TRANSFERRED, purchasedTicket?.state)
    assertNull(purchasedTicket?.listingPrice)
    assertNull(purchasedTicket?.listedAt)
    assertEquals(2, purchasedTicket?.version)
  }

  @Test
  fun purchaseListedTicket_notListed_returnsError() = runTest {
    val ticket = createTestTicket(ticketId = "ticket-1", state = TicketState.ISSUED)
    repository.addTicket(ticket)

    val result = repository.purchaseListedTicket("ticket-1", "buyer-123")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("not listed") == true)
  }

  @Test
  fun purchaseListedTicket_ownTicket_returnsError() = runTest {
    val ticket =
        createTestTicket(
            ticketId = "ticket-1",
            ownerId = "user-123",
            state = TicketState.LISTED,
            listingPrice = 100.0,
            listedAt = Timestamp.now())
    repository.addTicket(ticket)

    val result = repository.purchaseListedTicket("ticket-1", "user-123")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("own ticket") == true)
  }

  @Test
  fun purchaseListedTicket_notFound_returnsError() = runTest {
    val result = repository.purchaseListedTicket("non-existent", "buyer-123")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
  }

  // -------------------- Repository State Tests --------------------

  @Test
  fun setTickets_replacesAllTickets() = runTest {
    val ticket1 = createTestTicket(ticketId = "ticket-1")
    repository.addTicket(ticket1)

    val ticket2 = createTestTicket(ticketId = "ticket-2")
    val ticket3 = createTestTicket(ticketId = "ticket-3")
    repository.setTickets(listOf(ticket2, ticket3))

    val allTickets = repository.getTicketsByUser(testUserId).first()

    assertEquals(2, allTickets.size)
    assertFalse(allTickets.any { it.ticketId == "ticket-1" })
    assertTrue(allTickets.any { it.ticketId == "ticket-2" })
    assertTrue(allTickets.any { it.ticketId == "ticket-3" })
  }

  @Test
  fun reset_clearsAllTicketsAndFlags() = runTest {
    val ticket = createTestTicket()
    repository.addTicket(ticket)
    repository.setThrowOnCreate(true)

    repository.reset()

    val tickets = repository.getTicketsByUser(testUserId).first()
    assertEquals(0, tickets.size)

    // Verify flags are reset
    val result = repository.createTicket(createTestTicket())
    assertTrue(result.isSuccess)
  }

  @Test
  fun throwOnUpdate_causesUpdateToFail() = runTest {
    val ticket = createTestTicket(ticketId = "ticket-1")
    repository.addTicket(ticket)
    repository.setThrowOnUpdate(true)

    val result = repository.updateTicket(ticket.copy(state = TicketState.REDEEMED))

    assertTrue(result.isFailure)
    assertEquals("Failed to update ticket", result.exceptionOrNull()?.message)
  }

  @Test
  fun throwOnDelete_causesDeleteToFail() = runTest {
    val ticket = createTestTicket(ticketId = "ticket-1")
    repository.addTicket(ticket)
    repository.setThrowOnDelete(true)

    val result = repository.deleteTicket("ticket-1")

    assertTrue(result.isFailure)
    assertEquals("Failed to delete ticket", result.exceptionOrNull()?.message)
  }
}
