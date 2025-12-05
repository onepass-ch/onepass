package ch.onepass.onepass.model.ticket

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.formatAsDisplayDate
import ch.onepass.onepass.ui.myevents.TicketStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Ticket data model for Firestore.
 *
 * @property ticketId Unique identifier for the ticket.
 * @property eventId Identifier of the associated event.
 * @property ownerId Identifier of the ticket owner (user).
 * @property state Current state of the ticket (e.g., ISSUED, REDEEMED).
 * @property tierId Identifier of the pricing tier for the ticket.
 * @property purchasePrice Price paid for the ticket.
 * @property issuedAt Timestamp when the ticket was issued.
 * @property expiresAt Optional timestamp when the ticket expires.
 * @property transferLock Indicates if the ticket is locked from transfers.
 * @property version Version number for optimistic concurrency control.
 * @property deletedAt Optional timestamp when the ticket was soft-deleted.
 */
data class Ticket(
    val ticketId: String = "",
    val eventId: String = "",
    val ownerId: String = "",
    val state: TicketState = TicketState.ISSUED,
    val tierId: String = "",
    val purchasePrice: Double = 0.0,

    // Automatic server timestamp when the ticket is issued
    @ServerTimestamp val issuedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val transferLock: Boolean = false,
    val version: Int = 1,

    // Automatic server timestamp when the document is deleted (soft delete)
    @ServerTimestamp val deletedAt: Timestamp? = null
)

/** Enum representing the various states a ticket can be in. */
enum class TicketState {
  ISSUED,
  LISTED,
  TRANSFERRED,
  REDEEMED,
  REVOKED
}

/**
 * Maps a [Ticket] and its associated [Event] to a UI-friendly ticket representation.
 *
 * If the event is null, default placeholder values are used.
 *
 * @param event The associated event for the ticket, or null if not available.
 * @return A UI-friendly ticket object for display purposes.
 */
fun Ticket.toUiTicket(event: Event?): ch.onepass.onepass.ui.myevents.Ticket {
  return ch.onepass.onepass.ui.myevents.Ticket(
      ticketId = ticketId,
      title = event?.title ?: "Unknown Event",
      status = computeUiStatus(), // dynamically compute status
      dateTime = event?.displayDateTime ?: issuedAt.formatAsDisplayDate(),
      location = event?.displayLocation ?: "Unknown Location")
}

/**
 * Computes the UI status of the ticket based on its state and timing.
 * - If the ticket is REDEEMED or REVOKED, it is considered EXPIRED.
 * - If the current time is past the expiresAt timestamp, it is EXPIRED.
 * - If the current time is past the issuedAt timestamp, it is CURRENTLY valid.
 * - Otherwise, it is UPCOMING.
 *
 * @param currentTime The current time to compare against (default is now).
 * @return The computed [TicketStatus] for UI display.
 */
fun Ticket.computeUiStatus(currentTime: Timestamp = Timestamp.now()): TicketStatus {
  return when (state) {
    TicketState.REDEEMED,
    TicketState.REVOKED -> TicketStatus.EXPIRED
    else -> {
      if (expiresAt != null && currentTime.seconds > expiresAt.seconds) {
        TicketStatus.EXPIRED
      } else if (issuedAt != null && currentTime.seconds > issuedAt.seconds) {
        TicketStatus.CURRENTLY
      } else {
        TicketStatus.UPCOMING
      }
    }
  }
}
