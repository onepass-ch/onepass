package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.utils.DateTimeUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Event data model for Cloud Firestore.
 *
 * @property eventId Unique identifier for the event.
 * @property title Event title.
 * @property description Event description.
 * @property organizerId ID of the user or organization organizing the event.
 * @property organizerName Name of the organizer.
 * @property status Current status of the event (e.g., DRAFT, PUBLISHED).
 * @property location Optional location of the event.
 * @property startTime Start time of the event as a Firestore Timestamp.
 * @property endTime End time of the event as a Firestore Timestamp.
 * @property capacity Maximum number of attendees allowed.
 * @property ticketsRemaining Number of tickets still available.
 * @property ticketsIssued Total number of tickets issued.
 * @property ticketsRedeemed Number of tickets that have been redeemed.
 * @property currency Currency used for pricing (default: CHF).
 * @property pricingTiers List of available pricing tiers.
 * @property images List of image URLs associated with the event.
 * @property tags List of tags for discovery and categorization.
 * @property createdAt Timestamp when the event was created.
 * @property updatedAt Timestamp when the event was last updated.
 * @property deletedAt Timestamp when the event was soft-deleted (if applicable).
 */
data class Event(
    val eventId: String = "",
    val title: String = "",
    val description: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val status: EventStatus = EventStatus.PUBLISHED,

    // Location abstraction preserved
    val location: Location? = null,

    // Timing
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,

    // Capacity & Tickets
    val capacity: Int = 0,
    val ticketsRemaining: Int = 0,
    val ticketsIssued: Int = 0,
    val ticketsRedeemed: Int = 0,
    val currency: String = "CHF",
    val pricingTiers: List<PricingTier> = emptyList(),

    // Discovery & Media
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),

    // Metadata
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val deletedAt: Timestamp? = null
) {
  /** Returns the event title in lowercase for case-insensitive searching. */
  val titleLower: String
    get() = title.lowercase()

  /** Returns a human-readable location name or "Unknown Location" if location is null. */
  val displayLocation: String
    get() = location?.name ?: "Unknown Location"

  /**
   * Formats the event start time into a user-friendly string like "MMMM dd, yyyy • h:mm a".
   *
   * @return Formatted date-time string, or "Date not set" if [startTime] is null.
   */
  val displayDateTime: String
    get() = DateTimeUtils.formatDisplayDate(startTime)

  /**
   * Returns the lowest price among all pricing tiers as an unsigned integer (in cents or smallest
   * currency unit).
   *
   * @return Minimum price, or 0u if no pricing tiers exist.
   */
  val lowestPrice: UInt
    get() = pricingTiers.minOfOrNull { it.price.toUInt() } ?: 0u

  /** Returns the first image URL from the [images] list, or an empty string if none exist. */
  val imageUrl: String
    get() = images.firstOrNull() ?: ""

  /** Indicates whether the event is sold out (i.e., no tickets remaining). */
  val isSoldOut: Boolean
    get() = ticketsRemaining <= 0

  /** Indicates whether the event is published and visible to users. */
  val isPublished: Boolean
    get() = status == EventStatus.PUBLISHED

  /**
   * Indicates whether tickets can be resold (i.e., event is published and has unredeemed tickets).
   */
  val canResell: Boolean
    get() = status == EventStatus.PUBLISHED && ticketsIssued > ticketsRedeemed
}

/** Represents the lifecycle status of an event. */
enum class EventStatus {
  DRAFT,
  PUBLISHED,
  CLOSED,
  CANCELLED
}

/**
 * Represents a pricing tier for event tickets.
 *
 * @property name Name of the pricing tier (e.g., "General", "VIP").
 * @property price Price in the event's currency (e.g., 25.0 for CHF 25.00).
 * @property quantity Total number of tickets available in this tier.
 * @property remaining Number of tickets still available in this tier.
 */
data class PricingTier(
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val remaining: Int = 0
)

/**
 * Extension function to format a Firestore [Timestamp] into a user-friendly date string.
 *
 * @return Formatted date string like "MMMM dd, yyyy • h:mm a", or "Date not set" if null.
 * @receiver Timestamp? The Firestore timestamp to format (nullable).
 */
fun Timestamp?.formatAsDisplayDate(): String {
  return DateTimeUtils.formatDisplayDate(this)
}
