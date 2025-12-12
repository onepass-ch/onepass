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
 * @property listingPrice Price at which the ticket is listed for sale (null if not listed).
 * @property listedAt Timestamp when the ticket was listed for sale.
 * @property currency Currency for the listing price (default: CHF).
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
    @ServerTimestamp val deletedAt: Timestamp? = null,

    // Market listing fields
    val listingPrice: Double? = null,
    val listedAt: Timestamp? = null,
    val currency: String = "CHF"
) {
  /** Returns true if the ticket is currently listed for sale on the market. */
  val isListed: Boolean
    get() = state == TicketState.LISTED && listingPrice != null

  /** Returns true if the ticket can be listed for sale (issued state and not transfer locked). */
  val canBeListed: Boolean
    get() = state == TicketState.ISSUED && !transferLock
}

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
      status = computeUiStatus(event), // dynamically compute status with event timing
      dateTime = event?.displayDateTime ?: issuedAt.formatAsDisplayDate(),
      location = event?.displayLocation ?: "Unknown Location")
}

/**
 * Computes the UI status of the ticket based on its state, event timing, and expiration.
 * - If the ticket is REDEEMED or REVOKED, it is considered EXPIRED.
 * - If the current time is past the expiresAt timestamp, it is EXPIRED.
 * - If the event has ended (past endTime), it is EXPIRED.
 * - If the event has started (past startTime), it is CURRENTLY valid.
 * - Otherwise, it is UPCOMING.
 *
 * @param event The associated event for timing information (nullable).
 * @param currentTime The current time to compare against (default is now).
 * @return The computed [TicketStatus] for UI display.
 */
fun Ticket.computeUiStatus(
    event: Event? = null,
    currentTime: Timestamp = Timestamp.now()
): TicketStatus {
  return when (state) {
    TicketState.REDEEMED,
    TicketState.REVOKED -> TicketStatus.EXPIRED
    else ->
        // Check if ticket has expired by its own expiration time
        when {
            expiresAt != null && currentTime.seconds > expiresAt.seconds -> {
                TicketStatus.EXPIRED
            }

            // Check if event has ended
            event?.endTime != null && currentTime.seconds > event.endTime.seconds -> {
                TicketStatus.EXPIRED
            }

            // Check if event has started (use event start time if available, otherwise fall back to
            // issuedAt)
            event?.startTime != null && currentTime.seconds >= event.startTime.seconds -> {
                TicketStatus.CURRENTLY
            }

            event?.startTime == null &&
                    issuedAt != null &&
                    currentTime.seconds >= issuedAt.seconds -> {
                // Fallback: if no event start time, use ticket issuedAt
                TicketStatus.CURRENTLY
            }

            else -> {
                TicketStatus.UPCOMING
            }
        }
  }
}
