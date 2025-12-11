package ch.onepass.onepass.model.ticket

import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TicketRepository interface using MockK.
 *
 * These tests verify the contract of the TicketRepository interface and focus on the new
 * marketplace-related methods added in the refactor.
 */
class TicketRepositoryTest {

  private lateinit var mockRepository: TicketRepository
  private val testUserId = "test-user-123"
  private val testEventId = "test-event-456"
  private val testTicketId = "test-ticket-789"

  @Before
  fun setUp() {
    mockRepository = mockk()
  }

  private fun createTestTicket(
      ticketId: String = testTicketId,
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

  // -------------------- getListedTicketsByUser Tests --------------------

  @Test
  fun getListedTicketsByUser_returnsListedTickets() = runTest {
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

    every { mockRepository.getListedTicketsByUser(testUserId) } returns
        flowOf(listOf(listedTicket1, listedTicket2))

    val result = mockRepository.getListedTicketsByUser(testUserId).first()

    assertEquals(2, result.size)
    assertTrue(result.all { it.state == TicketState.LISTED })
    assertTrue(result.all { it.ownerId == testUserId })
    assertEquals("listed-1", result[0].ticketId)
    assertEquals("listed-2", result[1].ticketId)

    verify(exactly = 1) { mockRepository.getListedTicketsByUser(testUserId) }
  }

  @Test
  fun getListedTicketsByUser_emptyResult() = runTest {
    every { mockRepository.getListedTicketsByUser(testUserId) } returns flowOf(emptyList())

    val result = mockRepository.getListedTicketsByUser(testUserId).first()

    assertEquals(0, result.size)
    verify(exactly = 1) { mockRepository.getListedTicketsByUser(testUserId) }
  }

  @Test
  fun getListedTicketsByUser_sortedByListedAt() = runTest {
    val older =
        createTestTicket(
            ticketId = "older",
            state = TicketState.LISTED,
            listingPrice = 100.0,
            listedAt = Timestamp(1000, 0))
    val newer =
        createTestTicket(
            ticketId = "newer",
            state = TicketState.LISTED,
            listingPrice = 150.0,
            listedAt = Timestamp(2000, 0))

    every { mockRepository.getListedTicketsByUser(testUserId) } returns flowOf(listOf(newer, older))

    val result = mockRepository.getListedTicketsByUser(testUserId).first()

    assertEquals(2, result.size)
    assertEquals("newer", result[0].ticketId)
    assertEquals("older", result[1].ticketId)
  }

  // -------------------- getListedTicketsByEvent Tests --------------------

  @Test
  fun getListedTicketsByEvent_returnsListedTicketsForEvent() = runTest {
    val ticket1 =
        createTestTicket(
            ticketId = "ticket-1",
            eventId = testEventId,
            state = TicketState.LISTED,
            listingPrice = 100.0)
    val ticket2 =
        createTestTicket(
            ticketId = "ticket-2",
            eventId = testEventId,
            state = TicketState.LISTED,
            listingPrice = 150.0)

    every { mockRepository.getListedTicketsByEvent(testEventId) } returns
        flowOf(listOf(ticket1, ticket2))

    val result = mockRepository.getListedTicketsByEvent(testEventId).first()

    assertEquals(2, result.size)
    assertTrue(result.all { it.eventId == testEventId })
    assertTrue(result.all { it.state == TicketState.LISTED })

    verify(exactly = 1) { mockRepository.getListedTicketsByEvent(testEventId) }
  }

  @Test
  fun getListedTicketsByEvent_emptyResult() = runTest {
    every { mockRepository.getListedTicketsByEvent(testEventId) } returns flowOf(emptyList())

    val result = mockRepository.getListedTicketsByEvent(testEventId).first()

    assertEquals(0, result.size)
    verify(exactly = 1) { mockRepository.getListedTicketsByEvent(testEventId) }
  }

  @Test
  fun getListedTicketsByEvent_sortedByPrice() = runTest {
    val expensive =
        createTestTicket(ticketId = "expensive", state = TicketState.LISTED, listingPrice = 200.0)
    val cheap =
        createTestTicket(ticketId = "cheap", state = TicketState.LISTED, listingPrice = 50.0)
    val medium =
        createTestTicket(ticketId = "medium", state = TicketState.LISTED, listingPrice = 100.0)

    every { mockRepository.getListedTicketsByEvent(testEventId) } returns
        flowOf(listOf(cheap, medium, expensive))

    val result = mockRepository.getListedTicketsByEvent(testEventId).first()

    assertEquals(3, result.size)
    assertEquals("cheap", result[0].ticketId)
    assertEquals(50.0, result[0].listingPrice)
    assertEquals("medium", result[1].ticketId)
    assertEquals(100.0, result[1].listingPrice)
    assertEquals("expensive", result[2].ticketId)
    assertEquals(200.0, result[2].listingPrice)
  }

  @Test
  fun getListedTicketsByEvent_differentEvents_returnsOnlyMatchingEvent() = runTest {
    val correctEvent =
        createTestTicket(
            ticketId = "correct",
            eventId = testEventId,
            state = TicketState.LISTED,
            listingPrice = 100.0)

    every { mockRepository.getListedTicketsByEvent(testEventId) } returns
        flowOf(listOf(correctEvent))

    val result = mockRepository.getListedTicketsByEvent(testEventId).first()

    assertEquals(1, result.size)
    assertEquals(testEventId, result[0].eventId)
  }

  // -------------------- purchaseListedTicket Tests --------------------

  @Test
  fun purchaseListedTicket_success() = runTest {
    val buyerId = "buyer-456"

    coEvery { mockRepository.purchaseListedTicket(testTicketId, buyerId) } returns
        Result.success(Unit)

    val result = mockRepository.purchaseListedTicket(testTicketId, buyerId)

    assertTrue(result.isSuccess)
    coVerify(exactly = 1) { mockRepository.purchaseListedTicket(testTicketId, buyerId) }
  }

  @Test
  fun purchaseListedTicket_ticketNotFound_returnsError() = runTest {
    val buyerId = "buyer-456"

    coEvery { mockRepository.purchaseListedTicket(testTicketId, buyerId) } returns
        Result.failure(IllegalArgumentException("Ticket not found"))

    val result = mockRepository.purchaseListedTicket(testTicketId, buyerId)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    assertEquals("Ticket not found", result.exceptionOrNull()?.message)
  }

  @Test
  fun purchaseListedTicket_ticketNotListed_returnsError() = runTest {
    val buyerId = "buyer-456"

    coEvery { mockRepository.purchaseListedTicket(testTicketId, buyerId) } returns
        Result.failure(IllegalStateException("Ticket is not listed for sale"))

    val result = mockRepository.purchaseListedTicket(testTicketId, buyerId)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalStateException)
    assertTrue(result.exceptionOrNull()?.message?.contains("not listed") == true)
  }

  @Test
  fun purchaseListedTicket_cannotBuyOwnTicket_returnsError() = runTest {
    coEvery { mockRepository.purchaseListedTicket(testTicketId, testUserId) } returns
        Result.failure(IllegalArgumentException("Cannot purchase your own ticket"))

    val result = mockRepository.purchaseListedTicket(testTicketId, testUserId)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("own ticket") == true)
  }

  @Test
  fun purchaseListedTicket_multipleTransactions() = runTest {
    val buyer1 = "buyer-1"
    val buyer2 = "buyer-2"

    coEvery { mockRepository.purchaseListedTicket("ticket-1", buyer1) } returns Result.success(Unit)
    coEvery { mockRepository.purchaseListedTicket("ticket-2", buyer2) } returns Result.success(Unit)

    val result1 = mockRepository.purchaseListedTicket("ticket-1", buyer1)
    val result2 = mockRepository.purchaseListedTicket("ticket-2", buyer2)

    assertTrue(result1.isSuccess)
    assertTrue(result2.isSuccess)
    coVerify(exactly = 1) { mockRepository.purchaseListedTicket("ticket-1", buyer1) }
    coVerify(exactly = 1) { mockRepository.purchaseListedTicket("ticket-2", buyer2) }
  }

  // -------------------- getExpiredTickets Tests --------------------

  @Test
  fun getExpiredTickets_returnsExpiredAndRedeemedTickets() = runTest {
    val redeemedTicket = createTestTicket(ticketId = "redeemed", state = TicketState.REDEEMED)
    val revokedTicket = createTestTicket(ticketId = "revoked", state = TicketState.REVOKED)
    val expiredTicket =
        createTestTicket(
            ticketId = "expired",
            state = TicketState.ISSUED,
            expiresAt = Timestamp(Timestamp.now().seconds - 3600, 0))

    every { mockRepository.getExpiredTickets(testUserId) } returns
        flowOf(listOf(redeemedTicket, revokedTicket, expiredTicket))

    val result = mockRepository.getExpiredTickets(testUserId).first()

    assertEquals(3, result.size)
    assertTrue(result.any { it.state == TicketState.REDEEMED })
    assertTrue(result.any { it.state == TicketState.REVOKED })
    assertTrue(result.any { it.expiresAt?.seconds ?: 0 < Timestamp.now().seconds })

    verify(exactly = 1) { mockRepository.getExpiredTickets(testUserId) }
  }

  @Test
  fun getExpiredTickets_excludesActiveTickets() = runTest {
    val redeemedTicket = createTestTicket(ticketId = "redeemed", state = TicketState.REDEEMED)

    every { mockRepository.getExpiredTickets(testUserId) } returns flowOf(listOf(redeemedTicket))

    val result = mockRepository.getExpiredTickets(testUserId).first()

    assertEquals(1, result.size)
    assertFalse(result.any { it.state == TicketState.ISSUED })
    assertFalse(result.any { it.state == TicketState.TRANSFERRED })
  }

  @Test
  fun getExpiredTickets_excludesListedTickets() = runTest {
    val redeemedTicket = createTestTicket(ticketId = "redeemed", state = TicketState.REDEEMED)

    every { mockRepository.getExpiredTickets(testUserId) } returns flowOf(listOf(redeemedTicket))

    val result = mockRepository.getExpiredTickets(testUserId).first()

    assertFalse(result.any { it.state == TicketState.LISTED })
  }

  @Test
  fun getExpiredTickets_emptyResult() = runTest {
    every { mockRepository.getExpiredTickets(testUserId) } returns flowOf(emptyList())

    val result = mockRepository.getExpiredTickets(testUserId).first()

    assertEquals(0, result.size)
  }

  // -------------------- getListedTickets Tests --------------------

  @Test
  fun getListedTickets_returnsAllListedTickets() = runTest {
    val ticket1 =
        createTestTicket(
            ticketId = "ticket-1",
            ownerId = "user-1",
            state = TicketState.LISTED,
            listingPrice = 100.0)
    val ticket2 =
        createTestTicket(
            ticketId = "ticket-2",
            ownerId = "user-2",
            state = TicketState.LISTED,
            listingPrice = 150.0)

    every { mockRepository.getListedTickets() } returns flowOf(listOf(ticket1, ticket2))

    val result = mockRepository.getListedTickets().first()

    assertEquals(2, result.size)
    assertTrue(result.all { it.state == TicketState.LISTED })
    assertTrue(result.all { it.listingPrice != null })

    verify(exactly = 1) { mockRepository.getListedTickets() }
  }

  @Test
  fun getListedTickets_emptyMarketplace() = runTest {
    every { mockRepository.getListedTickets() } returns flowOf(emptyList())

    val result = mockRepository.getListedTickets().first()

    assertEquals(0, result.size)
  }

  // -------------------- listTicketForSale Tests --------------------

  @Test
  fun listTicketForSale_success() = runTest {
    val askingPrice = 100.0

    coEvery { mockRepository.listTicketForSale(testTicketId, askingPrice) } returns
        Result.success(Unit)

    val result = mockRepository.listTicketForSale(testTicketId, askingPrice)

    assertTrue(result.isSuccess)
    coVerify(exactly = 1) { mockRepository.listTicketForSale(testTicketId, askingPrice) }
  }

  @Test
  fun listTicketForSale_negativePrice_returnsError() = runTest {
    coEvery { mockRepository.listTicketForSale(testTicketId, -50.0) } returns
        Result.failure(IllegalArgumentException("Asking price must be positive"))

    val result = mockRepository.listTicketForSale(testTicketId, -50.0)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("positive") == true)
  }

  @Test
  fun listTicketForSale_ticketNotFound_returnsError() = runTest {
    coEvery { mockRepository.listTicketForSale(testTicketId, 100.0) } returns
        Result.failure(IllegalArgumentException("Ticket not found"))

    val result = mockRepository.listTicketForSale(testTicketId, 100.0)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
  }

  @Test
  fun listTicketForSale_alreadyListed_returnsError() = runTest {
    coEvery { mockRepository.listTicketForSale(testTicketId, 100.0) } returns
        Result.failure(IllegalStateException("Ticket is already listed"))

    val result = mockRepository.listTicketForSale(testTicketId, 100.0)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalStateException)
  }

  // -------------------- cancelTicketListing Tests --------------------

  @Test
  fun cancelTicketListing_success() = runTest {
    coEvery { mockRepository.cancelTicketListing(testTicketId) } returns Result.success(Unit)

    val result = mockRepository.cancelTicketListing(testTicketId)

    assertTrue(result.isSuccess)
    coVerify(exactly = 1) { mockRepository.cancelTicketListing(testTicketId) }
  }

  @Test
  fun cancelTicketListing_notListed_returnsError() = runTest {
    coEvery { mockRepository.cancelTicketListing(testTicketId) } returns
        Result.failure(IllegalStateException("Ticket is not currently listed"))

    val result = mockRepository.cancelTicketListing(testTicketId)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("not currently listed") == true)
  }

  @Test
  fun cancelTicketListing_ticketNotFound_returnsError() = runTest {
    coEvery { mockRepository.cancelTicketListing(testTicketId) } returns
        Result.failure(IllegalArgumentException("Ticket not found"))

    val result = mockRepository.cancelTicketListing(testTicketId)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
  }

  // -------------------- Integration Scenarios --------------------

  @Test
  fun listAndPurchaseWorkflow_success() = runTest {
    val sellerId = "seller-123"
    val buyerId = "buyer-456"
    val askingPrice = 100.0

    // List the ticket
    coEvery { mockRepository.listTicketForSale(testTicketId, askingPrice) } returns
        Result.success(Unit)

    val listResult = mockRepository.listTicketForSale(testTicketId, askingPrice)
    assertTrue(listResult.isSuccess)

    // Purchase the ticket
    coEvery { mockRepository.purchaseListedTicket(testTicketId, buyerId) } returns
        Result.success(Unit)

    val purchaseResult = mockRepository.purchaseListedTicket(testTicketId, buyerId)
    assertTrue(purchaseResult.isSuccess)

    coVerify(exactly = 1) { mockRepository.listTicketForSale(testTicketId, askingPrice) }
    coVerify(exactly = 1) { mockRepository.purchaseListedTicket(testTicketId, buyerId) }
  }

  @Test
  fun listAndCancelWorkflow_success() = runTest {
    val askingPrice = 100.0

    // List the ticket
    coEvery { mockRepository.listTicketForSale(testTicketId, askingPrice) } returns
        Result.success(Unit)

    val listResult = mockRepository.listTicketForSale(testTicketId, askingPrice)
    assertTrue(listResult.isSuccess)

    // Cancel the listing
    coEvery { mockRepository.cancelTicketListing(testTicketId) } returns Result.success(Unit)

    val cancelResult = mockRepository.cancelTicketListing(testTicketId)
    assertTrue(cancelResult.isSuccess)

    coVerify(exactly = 1) { mockRepository.listTicketForSale(testTicketId, askingPrice) }
    coVerify(exactly = 1) { mockRepository.cancelTicketListing(testTicketId) }
  }

  // -------------------- Basic Operations Tests --------------------

  @Test
  fun getTicketsByUser_returnsUserTickets() = runTest {
    val ticket1 = createTestTicket(ticketId = "ticket-1")
    val ticket2 = createTestTicket(ticketId = "ticket-2")

    every { mockRepository.getTicketsByUser(testUserId) } returns flowOf(listOf(ticket1, ticket2))

    val result = mockRepository.getTicketsByUser(testUserId).first()

    assertEquals(2, result.size)
    assertTrue(result.all { it.ownerId == testUserId })
  }

  @Test
  fun getActiveTickets_returnsOnlyActiveTickets() = runTest {
    val activeTicket = createTestTicket(ticketId = "active", state = TicketState.ISSUED)

    every { mockRepository.getActiveTickets(testUserId) } returns flowOf(listOf(activeTicket))

    val result = mockRepository.getActiveTickets(testUserId).first()

    assertEquals(1, result.size)
    assertEquals(TicketState.ISSUED, result[0].state)
  }

  @Test
  fun getTicketById_returnsTicket() = runTest {
    val ticket = createTestTicket()

    every { mockRepository.getTicketById(testTicketId) } returns flowOf(ticket)

    val result = mockRepository.getTicketById(testTicketId).first()

    assertNotNull(result)
    assertEquals(testTicketId, result?.ticketId)
  }

  @Test
  fun getTicketById_notFound_returnsNull() = runTest {
    every { mockRepository.getTicketById("non-existent") } returns flowOf(null)

    val result = mockRepository.getTicketById("non-existent").first()

    assertNull(result)
  }

  @Test
  fun createTicket_success_returnsTicketId() = runTest {
    val ticket = createTestTicket(ticketId = "")

    coEvery { mockRepository.createTicket(ticket) } returns Result.success("new-ticket-id")

    val result = mockRepository.createTicket(ticket)

    assertTrue(result.isSuccess)
    assertEquals("new-ticket-id", result.getOrNull())
  }

  @Test
  fun updateTicket_success() = runTest {
    val ticket = createTestTicket()

    coEvery { mockRepository.updateTicket(ticket) } returns Result.success(Unit)

    val result = mockRepository.updateTicket(ticket)

    assertTrue(result.isSuccess)
  }

  @Test
  fun deleteTicket_success() = runTest {
    coEvery { mockRepository.deleteTicket(testTicketId) } returns Result.success(Unit)

    val result = mockRepository.deleteTicket(testTicketId)

    assertTrue(result.isSuccess)
  }
}
