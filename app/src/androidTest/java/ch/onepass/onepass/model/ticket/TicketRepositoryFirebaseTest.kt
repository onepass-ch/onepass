package ch.onepass.onepass.model.ticket

import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import ch.onepass.onepass.utils.TicketTestData
import com.google.firebase.Timestamp
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for TicketRepositoryFirebase using Firebase Emulator.
 *
 * These tests verify CRUD operations and queries against a real Firestore instance running in the
 * emulator. This ensures the repository implementation works correctly with actual Firebase APIs.
 *
 * Prerequisites:
 * - Firebase emulators must be running (firebase emulators:start)
 * - Test uses anonymous authentication
 *
 * @see FirestoreTestBase for setup/teardown logic
 */
class TicketRepositoryFirebaseTest : FirestoreTestBase() {
  /** Repository under test */
  private lateinit var ticketRepository: TicketRepositoryFirebase
  /** Current test user ID */
  private lateinit var userId: String

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      ticketRepository = TicketRepositoryFirebase()

      // Sign in anonymously for testing
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"

      // Clear any existing test data
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

  /** Helper to get the current count of tickets in the test collection. */
  private suspend fun getTicketsCount(): Int {
    return FirebaseEmulator.firestore.collection("tickets").get().await().size()
  }

  @Test
  fun canCreateTicket() = runTest {
    val testTicket = TicketTestData.createTestTicket(ownerId = userId)

    val result = ticketRepository.createTicket(testTicket)
    assertTrue("Create ticket should succeed", result.isSuccess)

    val ticketId = result.getOrNull()
    assertNotNull("Ticket ID should not be null", ticketId)

    assertEquals("Should have 1 ticket in repository", 1, getTicketsCount())

    val storedTicket = ticketRepository.getTicketById(ticketId!!).first()
    assertNotNull("Stored ticket should not be null", storedTicket)
    assertEquals("Owner ID should match", userId, storedTicket?.ownerId)
    assertEquals("State should match", TicketState.ISSUED, storedTicket?.state)
  }

  @Test
  fun canRetrieveTicketById() = runTest {
    val testTicket = TicketTestData.createTestTicket(ownerId = userId)
    val createResult = ticketRepository.createTicket(testTicket)
    val ticketId = createResult.getOrNull()!!

    val retrievedTicket = ticketRepository.getTicketById(ticketId).first()
    assertNotNull("Ticket should be found", retrievedTicket)
    assertEquals("Ticket ID should match", ticketId, retrievedTicket?.ticketId)
    assertEquals("Owner ID should match", userId, retrievedTicket?.ownerId)
  }

  @Test
  fun canUpdateTicket() = runTest {
    val testTicket = TicketTestData.createTestTicket(ownerId = userId)
    val createResult = ticketRepository.createTicket(testTicket)
    val ticketId = createResult.getOrNull()!!

    // Retrieve and update
    val retrievedTicket = ticketRepository.getTicketById(ticketId).first()!!
    val updatedTicket =
        retrievedTicket.copy(
            state = TicketState.LISTED, transferLock = true, version = retrievedTicket.version + 1)

    val updateResult = ticketRepository.updateTicket(updatedTicket)
    assertTrue("Update should succeed", updateResult.isSuccess)

    val finalTicket = ticketRepository.getTicketById(ticketId).first()
    assertEquals("State should be updated", TicketState.LISTED, finalTicket?.state)
    assertEquals("Transfer lock should be updated", true, finalTicket?.transferLock)
    assertEquals("Version should be incremented", 2, finalTicket?.version)
  }

  @Test
  fun canDeleteTicket() = runTest {
    val testTicket = TicketTestData.createTestTicket(ownerId = userId)
    val createResult = ticketRepository.createTicket(testTicket)
    val ticketId = createResult.getOrNull()!!

    assertEquals("Should start with 1 ticket", 1, getTicketsCount())

    val deleteResult = ticketRepository.deleteTicket(ticketId)
    assertTrue("Delete should succeed", deleteResult.isSuccess)

    assertEquals("Should have 0 tickets remaining", 0, getTicketsCount())

    val deletedTicket = ticketRepository.getTicketById(ticketId).first()
    assertNull("Ticket should be deleted", deletedTicket)
  }

  @Test
  fun getTicketsByUserReturnsOnlyUsersTickets() = runTest {
    val tickets = TicketTestData.createTicketsForMultipleUsers()
    tickets.forEach { ticketRepository.createTicket(it) }

    val user1Tickets = ticketRepository.getTicketsByUser("user_1").first()
    assertEquals("Should have 2 tickets for user_1", 2, user1Tickets.size)
    assertTrue("All tickets should belong to user_1", user1Tickets.all { it.ownerId == "user_1" })

    val user2Tickets = ticketRepository.getTicketsByUser("user_2").first()
    assertEquals("Should have 2 tickets for user_2", 2, user2Tickets.size)
    assertTrue("All tickets should belong to user_2", user2Tickets.all { it.ownerId == "user_2" })
  }

  @Test
  fun getActiveTicketsReturnsOnlyActiveStates() = runTest {
    val tickets = TicketTestData.createTicketsWithDifferentStates(userId)
    tickets.forEach { ticketRepository.createTicket(it) }

    val activeTickets = ticketRepository.getActiveTickets(userId).first()

    val activeStates = setOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED)
    assertEquals("Should have 3 active tickets", 3, activeTickets.size)
    assertTrue("All should be active states", activeTickets.all { it.state in activeStates })
    assertTrue(
        "Should not include redeemed tickets",
        activeTickets.none { it.state == TicketState.REDEEMED })
    assertTrue(
        "Should not include revoked tickets",
        activeTickets.none { it.state == TicketState.REVOKED })
  }

  @Test
  fun getExpiredTicketsReturnsOnlyExpiredStates() = runTest {
    val tickets = TicketTestData.createTicketsWithDifferentStates(userId)
    tickets.forEach { ticketRepository.createTicket(it) }

    val expiredTickets = ticketRepository.getExpiredTickets(userId).first()

    val expiredStates = setOf(TicketState.REDEEMED, TicketState.REVOKED)
    assertEquals("Should have 2 expired tickets", 2, expiredTickets.size)
    assertTrue("All should be expired states", expiredTickets.all { it.state in expiredStates })
    assertTrue(
        "Should not include active tickets", expiredTickets.none { it.state == TicketState.ISSUED })
  }

  @Test
  fun ticketsAreSortedByIssuedAtDescending() = runTest {
    val tickets =
        listOf(
            TicketTestData.createTestTicket(
                ticketId = "ticket_old",
                ownerId = userId,
                issuedAt = TicketTestData.createPastTimestamp(daysAgo = 30)),
            TicketTestData.createTestTicket(
                ticketId = "ticket_recent",
                ownerId = userId,
                issuedAt = TicketTestData.createPastTimestamp(daysAgo = 1)),
            TicketTestData.createTestTicket(
                ticketId = "ticket_new", ownerId = userId, issuedAt = Timestamp.now()))

    // Add in random order
    tickets.shuffled().forEach { ticketRepository.createTicket(it) }

    val retrievedTickets = ticketRepository.getTicketsByUser(userId).first()

    // Verify they are in descending order by issuedAt
    for (i in 0 until retrievedTickets.size - 1) {
      val current = retrievedTickets[i].issuedAt
      val next = retrievedTickets[i + 1].issuedAt
      assertTrue(
          "Tickets should be sorted by issuedAt descending",
          current == null || next == null || current.seconds >= next.seconds)
    }
  }

  @Test
  fun ticketNotFoundReturnsNull() = runTest {
    val ticket = ticketRepository.getTicketById("non-existent-id").first()
    assertNull("Should return null for non-existent ticket", ticket)
  }

  @Test
  fun canHandleTicketsWithNullTimestamps() = runTest {
    val ticketWithNullTimestamps =
        TicketTestData.createTestTicket(
            ownerId = userId, issuedAt = null, expiresAt = null, deletedAt = null)

    val result = ticketRepository.createTicket(ticketWithNullTimestamps)
    assertTrue("Should create ticket with null timestamps", result.isSuccess)

    val ticketId = result.getOrNull()!!
    val retrievedTicket = ticketRepository.getTicketById(ticketId).first()

    assertNotNull("Ticket should be retrieved", retrievedTicket)
    assertNull("IssuedAt should be null", retrievedTicket?.issuedAt)
    assertNull("ExpiresAt should be null", retrievedTicket?.expiresAt)
    assertNull("DeletedAt should be null", retrievedTicket?.deletedAt)
  }

  @Test
  fun canCreateMultipleTicketsForSameEvent() = runTest {
    val eventId = "event_multi_ticket"
    val tickets =
        listOf(
            TicketTestData.createTestTicket(
                ticketId = "ticket_1",
                ownerId = userId,
                eventId = eventId,
                state = TicketState.ISSUED),
            TicketTestData.createTestTicket(
                ticketId = "ticket_2",
                ownerId = userId,
                eventId = eventId,
                state = TicketState.LISTED),
            TicketTestData.createTestTicket(
                ticketId = "ticket_3",
                ownerId = userId,
                eventId = eventId,
                state = TicketState.REDEEMED))

    tickets.forEach { ticketRepository.createTicket(it) }

    val userTickets = ticketRepository.getTicketsByUser(userId).first()
    assertEquals("Should have 3 tickets for same event", 3, userTickets.size)
    assertTrue("All tickets should be for same event", userTickets.all { it.eventId == eventId })
  }
}
