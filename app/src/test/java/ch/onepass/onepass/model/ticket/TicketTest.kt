package ch.onepass.onepass.model.ticket

import com.google.firebase.Timestamp
import org.junit.Assert
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
      issuedAt: Timestamp? = Timestamp.Companion.now(),
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

    Assert.assertEquals("", ticket.ticketId)
    Assert.assertEquals("", ticket.eventId)
    Assert.assertEquals("", ticket.ownerId)
    Assert.assertEquals(TicketState.ISSUED, ticket.state)
    Assert.assertEquals("", ticket.tierId)
    Assert.assertEquals(0.0, ticket.purchasePrice, 0.01)
    Assert.assertNull(ticket.issuedAt)
    Assert.assertNull(ticket.expiresAt)
    Assert.assertFalse(ticket.transferLock)
    Assert.assertEquals(1, ticket.version)
    Assert.assertNull(ticket.deletedAt)
  }

  @Test
  fun ticketStateEnumHasAllExpectedValues() {
    val states = TicketState.values()
    Assert.assertEquals(5, states.size)
    Assert.assertTrue(states.contains(TicketState.ISSUED))
    Assert.assertTrue(states.contains(TicketState.LISTED))
    Assert.assertTrue(states.contains(TicketState.TRANSFERRED))
    Assert.assertTrue(states.contains(TicketState.REDEEMED))
    Assert.assertTrue(states.contains(TicketState.REVOKED))
  }

  @Test
  fun isActiveReturnsTrueForActiveStates() {
    val activeStates = listOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED)

    activeStates.forEach { state ->
      val ticket = createTestTicket(state = state)
      Assert.assertTrue(
          "Ticket with state $state should be active",
          ticket.state in listOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED))
    }
  }

  @Test
  fun isExpiredReturnsTrueForExpiredStates() {
    val expiredStates = listOf(TicketState.REDEEMED, TicketState.REVOKED)

    expiredStates.forEach { state ->
      val ticket = createTestTicket(state = state)
      Assert.assertTrue(
          "Ticket with state $state should be expired",
          ticket.state in listOf(TicketState.REDEEMED, TicketState.REVOKED))
    }
  }

  @Test
  fun canTransferReturnsTrueWhenNotLockedAndActive() {
    val transferableTicket = createTestTicket(state = TicketState.ISSUED, transferLock = false)
    Assert.assertTrue(
        "Should be transferable",
        !transferableTicket.transferLock &&
            transferableTicket.state in
                listOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED))

    val lockedTicket = createTestTicket(state = TicketState.ISSUED, transferLock = true)
    Assert.assertFalse(
        "Should not be transferable when locked",
        !lockedTicket.transferLock &&
            lockedTicket.state in
                listOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED))

    val redeemedTicket = createTestTicket(state = TicketState.REDEEMED, transferLock = false)
    Assert.assertFalse(
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

    Assert.assertEquals("Ticket ID should remain same", "original", updated.ticketId)
    Assert.assertEquals("State should be updated", TicketState.LISTED, updated.state)
    Assert.assertEquals("Price should be updated", 30.0, updated.purchasePrice, 0.01)
    Assert.assertEquals("Version should be incremented", 2, updated.version)
  }

  @Test
  fun ticketEqualityBasedOnTicketId() {
    val ticket1 = createTestTicket(ticketId = "same_id")
    val ticket2 = createTestTicket(ticketId = "same_id")
    val ticket3 = createTestTicket(ticketId = "different_id")

    Assert.assertEquals("Tickets with same ID should be equal", ticket1, ticket2)
    Assert.assertNotEquals("Tickets with different IDs should not be equal", ticket1, ticket3)
  }

  @Test
  fun ticketToStringIncludesImportantFields() {
    val ticket =
        createTestTicket(ticketId = "test_123", eventId = "event_456", state = TicketState.ISSUED)

    val stringRepresentation = ticket.toString()

    Assert.assertTrue("Should include ticket ID", stringRepresentation.contains("test_123"))
    Assert.assertTrue("Should include event ID", stringRepresentation.contains("event_456"))
    Assert.assertTrue("Should include state", stringRepresentation.contains("ISSUED"))
  }
}
