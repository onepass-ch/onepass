package ch.onepass.onepass.model.ticket

import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import ch.onepass.onepass.utils.TicketTestData
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Advanced integration tests for TicketRepositoryFirebase using test data helpers.
 *
 * This test class demonstrates more complex scenarios and edge cases using the TicketTestData
 * helper for cleaner and more maintainable tests.
 */
class TicketRepositoryAdvancedTest : FirestoreTestBase() {
  /** Repository under test */
  private lateinit var ticketRepository: TicketRepositoryFirebase
  /** Current test user ID */
  private lateinit var userId: String

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      ticketRepository = TicketRepositoryFirebase()
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
      clearTestCollection()
    }
  }

  /** Clears all documents in the "tickets" collection to ensure a clean state before each test. */
  private suspend fun clearTestCollection() {
    val tickets = FirebaseEmulator.firestore.collection("tickets").get().await()
    if (tickets.isEmpty) return

    val batch = FirebaseEmulator.firestore.batch()
    tickets.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()
  }

  @Test
  fun canCreateTicketsWithDifferentStates() = runTest {
    val tickets = TicketTestData.createTicketsWithDifferentStates(userId)

    tickets.forEach { ticket ->
      val result = ticketRepository.createTicket(ticket)
      assertTrue("Ticket creation should succeed", result.isSuccess)
    }

    // Verify each state has correct count
    val activeTickets = ticketRepository.getActiveTickets(userId).first()
    assertEquals("Should have 2 active tickets", 2, activeTickets.size)
    assertTrue(
        "Active tickets should only contain ISSUED or TRANSFERRED",
        activeTickets.all { it.state == TicketState.ISSUED || it.state == TicketState.TRANSFERRED })

    val expiredTickets = ticketRepository.getExpiredTickets(userId).first()
    assertEquals("Should have 2 expired tickets", 2, expiredTickets.size)

    val allTickets = ticketRepository.getTicketsByUser(userId).first()
    assertEquals("Should have 5 total tickets", 5, allTickets.size)
  }

  @Test
  fun transferLockedTicketCannotBeTransferred() = runTest {
    val transferLockedTicket =
        TicketTestData.createTestTicket(
            ownerId = userId, transferLock = true, state = TicketState.ISSUED)

    val result = ticketRepository.createTicket(transferLockedTicket)
    assertTrue("Should create transfer-locked ticket", result.isSuccess)

    val ticketId = result.getOrNull()!!
    val retrievedTicket = ticketRepository.getTicketById(ticketId).first()

    assertNotNull("Ticket should be retrieved", retrievedTicket)
    assertTrue("Ticket should be transfer locked", retrievedTicket?.transferLock == true)
  }

  @Test
  fun canHandleTicketsWithDifferentPricePoints() = runTest {
    val tickets = TicketTestData.createTicketsWithDifferentPrices(userId)
    tickets.forEach { ticketRepository.createTicket(it) }

    val userTickets = ticketRepository.getTicketsByUser(userId).first()
    assertEquals("Should have 3 tickets", 3, userTickets.size)

    val prices = userTickets.map { it.purchasePrice }
    assertTrue("Should include free ticket", prices.contains(0.0))
    assertTrue("Should include cheap ticket", prices.contains(15.0))
    assertTrue("Should include expensive ticket", prices.contains(100.0))
  }

  @Test
  fun ticketStateTransitionsWorkCorrectly() = runTest {
    val ticket = TicketTestData.createTestTicket(ownerId = userId, state = TicketState.ISSUED)
    val createResult = ticketRepository.createTicket(ticket)
    val ticketId = createResult.getOrNull()!!

    // Test ISSUED -> LISTED transition
    var currentTicket = ticketRepository.getTicketById(ticketId).first()!!
    var updatedTicket = currentTicket.copy(state = TicketState.LISTED)
    ticketRepository.updateTicket(updatedTicket)

    currentTicket = ticketRepository.getTicketById(ticketId).first()!!
    assertEquals("Should be LISTED state", TicketState.LISTED, currentTicket.state)

    // Test LISTED -> TRANSFERRED transition
    updatedTicket = currentTicket.copy(state = TicketState.TRANSFERRED)
    ticketRepository.updateTicket(updatedTicket)

    currentTicket = ticketRepository.getTicketById(ticketId).first()!!
    assertEquals("Should be TRANSFERRED state", TicketState.TRANSFERRED, currentTicket.state)

    // Test TRANSFERRED -> REDEEMED transition
    updatedTicket = currentTicket.copy(state = TicketState.REDEEMED)
    ticketRepository.updateTicket(updatedTicket)

    currentTicket = ticketRepository.getTicketById(ticketId).first()!!
    assertEquals("Should be REDEEMED state", TicketState.REDEEMED, currentTicket.state)
  }

  @Test
  fun versionIncrementOnUpdate() = runTest {
    val ticket = TicketTestData.createTestTicket(ownerId = userId, version = 1)
    val createResult = ticketRepository.createTicket(ticket)
    val ticketId = createResult.getOrNull()!!

    // First update
    var currentTicket = ticketRepository.getTicketById(ticketId).first()!!
    var updatedTicket =
        currentTicket.copy(state = TicketState.LISTED, version = currentTicket.version + 1)
    ticketRepository.updateTicket(updatedTicket)

    currentTicket = ticketRepository.getTicketById(ticketId).first()!!
    assertEquals("Version should be 2", 2, currentTicket.version)

    // Second update
    updatedTicket =
        currentTicket.copy(state = TicketState.REDEEMED, version = currentTicket.version + 1)
    ticketRepository.updateTicket(updatedTicket)

    currentTicket = ticketRepository.getTicketById(ticketId).first()!!
    assertEquals("Version should be 3", 3, currentTicket.version)
  }

  @Test
  fun multipleUsersCanHaveTicketsForSameEvent() = runTest {
    val eventId = "popular_event_123"
    val user1 = "user_1"
    val user2 = "user_2"

    val user1Ticket = TicketTestData.createTestTicket(ownerId = user1, eventId = eventId)
    val user2Ticket = TicketTestData.createTestTicket(ownerId = user2, eventId = eventId)

    ticketRepository.createTicket(user1Ticket)
    ticketRepository.createTicket(user2Ticket)

    val user1Tickets = ticketRepository.getTicketsByUser(user1).first()
    assertEquals("User1 should have 1 ticket", 1, user1Tickets.size)
    assertEquals("User1 ticket should be for the event", eventId, user1Tickets.first().eventId)

    val user2Tickets = ticketRepository.getTicketsByUser(user2).first()
    assertEquals("User2 should have 1 ticket", 1, user2Tickets.size)
    assertEquals("User2 ticket should be for the event", eventId, user2Tickets.first().eventId)
  }

  @Test
  fun expiredTicketsFilterCorrectlyBasedOnState() = runTest {
    val mixedTickets =
        listOf(
            TicketTestData.createTestTicket(
                ticketId = "active_1", ownerId = userId, state = TicketState.ISSUED),
            TicketTestData.createTestTicket(
                ticketId = "active_2", ownerId = userId, state = TicketState.LISTED),
            TicketTestData.createTestTicket(
                ticketId = "expired_1", ownerId = userId, state = TicketState.REDEEMED),
            TicketTestData.createTestTicket(
                ticketId = "expired_2", ownerId = userId, state = TicketState.REVOKED))

    mixedTickets.forEach { ticketRepository.createTicket(it) }

    val activeTickets = ticketRepository.getActiveTickets(userId).first()
    assertEquals("Should have 1 active ticket", 1, activeTickets.size)
    assertTrue(
        "All active tickets should be ISSUED or TRANSFERRED",
        activeTickets.all { it.state == TicketState.ISSUED || it.state == TicketState.TRANSFERRED })
    assertTrue(
        "Listed tickets should not be treated as active",
        activeTickets.none { it.state == TicketState.LISTED })

    val expiredTickets = ticketRepository.getExpiredTickets(userId).first()
    assertEquals("Should have 2 expired tickets", 2, expiredTickets.size)
    assertTrue(
        "All expired tickets should be REDEEMED or REVOKED",
        expiredTickets.all { it.state == TicketState.REDEEMED || it.state == TicketState.REVOKED })
  }
}
