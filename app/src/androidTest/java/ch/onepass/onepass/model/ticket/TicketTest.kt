package ch.onepass.onepass.model.ticket

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the Ticket data class and its related functionality. */
class TicketTest {

  /** Helper function to create a Ticket with specified or default parameters. */
  private fun createTestTicket(
      ticketId: String = "test_ticket_1",
      eventId: String = "test_event_1",
      ownerId: String = "test_user_1",
      state: TicketState = TicketState.ISSUED,
      tierId: String = "general",
      purchasePrice: Double = 25.0,
      issuedAt: Timestamp? = Timestamp.now(),
      expiresAt: Timestamp? = null,
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

  @Test
  fun ticketHasCorrectDefaults() {
    val ticket = Ticket()

    assertEquals("", ticket.ticketId)
    assertEquals("", ticket.eventId)
    assertEquals("", ticket.ownerId)
    assertEquals(TicketState.ISSUED, ticket.state)
    assertEquals("", ticket.tierId)
    assertEquals(0.0, ticket.purchasePrice, 0.01)
    assertNull(ticket.issuedAt)
    assertNull(ticket.expiresAt)
    assertFalse(ticket.transferLock)
    assertEquals(1, ticket.version)
    assertNull(ticket.deletedAt)
  }

  @Test
  fun ticketStateEnumHasAllExpectedValues() {
    val states = TicketState.values()
    assertEquals(5, states.size)
    assertTrue(states.contains(TicketState.ISSUED))
    assertTrue(states.contains(TicketState.LISTED))
    assertTrue(states.contains(TicketState.TRANSFERRED))
    assertTrue(states.contains(TicketState.REDEEMED))
    assertTrue(states.contains(TicketState.REVOKED))
  }

  @Test
  fun isActiveReturnsTrueForActiveStates() {
    val activeStates = listOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED)

    activeStates.forEach { state ->
      val ticket = createTestTicket(state = state)
      assertTrue(
          "Ticket with state $state should be active",
          ticket.state in listOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED))
    }
  }

  @Test
  fun isExpiredReturnsTrueForExpiredStates() {
    val expiredStates = listOf(TicketState.REDEEMED, TicketState.REVOKED)

    expiredStates.forEach { state ->
      val ticket = createTestTicket(state = state)
      assertTrue(
          "Ticket with state $state should be expired",
          ticket.state in listOf(TicketState.REDEEMED, TicketState.REVOKED))
    }
  }

  @Test
  fun canTransferReturnsTrueWhenNotLockedAndActive() {
    val transferableTicket = createTestTicket(state = TicketState.ISSUED, transferLock = false)
    assertTrue(
        "Should be transferable",
        !transferableTicket.transferLock &&
            transferableTicket.state in
                listOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED))

    val lockedTicket = createTestTicket(state = TicketState.ISSUED, transferLock = true)
    assertFalse(
        "Should not be transferable when locked",
        !lockedTicket.transferLock &&
            lockedTicket.state in
                listOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED))

    val redeemedTicket = createTestTicket(state = TicketState.REDEEMED, transferLock = false)
    assertFalse(
        "Should not be transferable when redeemed",
        !redeemedTicket.transferLock &&
            redeemedTicket.state in
                listOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED))
  }

  @Test
  fun ticketCopyWithNewValues() {
    val original =
        createTestTicket(ticketId = "original", state = TicketState.ISSUED, purchasePrice = 25.0)

    val updated =
        original.copy(
            state = TicketState.LISTED, purchasePrice = 30.0, version = original.version + 1)

    assertEquals("Ticket ID should remain same", "original", updated.ticketId)
    assertEquals("State should be updated", TicketState.LISTED, updated.state)
    assertEquals("Price should be updated", 30.0, updated.purchasePrice, 0.01)
    assertEquals("Version should be incremented", 2, updated.version)
  }

  @Test
  fun ticketEqualityBasedOnTicketId() {
    val ticket1 = createTestTicket(ticketId = "same_id")
    val ticket2 = createTestTicket(ticketId = "same_id")
    val ticket3 = createTestTicket(ticketId = "different_id")

    assertEquals("Tickets with same ID should be equal", ticket1, ticket2)
    assertNotEquals("Tickets with different IDs should not be equal", ticket1, ticket3)
  }

  @Test
  fun ticketToStringIncludesImportantFields() {
    val ticket =
        createTestTicket(ticketId = "test_123", eventId = "event_456", state = TicketState.ISSUED)

    val stringRepresentation = ticket.toString()

    assertTrue("Should include ticket ID", stringRepresentation.contains("test_123"))
    assertTrue("Should include event ID", stringRepresentation.contains("event_456"))
    assertTrue("Should include state", stringRepresentation.contains("ISSUED"))
  }
}
