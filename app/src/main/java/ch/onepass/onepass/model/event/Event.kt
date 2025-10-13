package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/** Event data model for Cloud Firestore. */
data class Event(
    val eventId: String = "",
    val title: String = "",
    val description: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val status: EventStatus = EventStatus.DRAFT,

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
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
) {
  val titleLower: String
    get() = title.lowercase()

  val displayLocation: String
    get() = location?.name ?: "Unknown Location"

  val displayDateTime: String
    get() {
      val startTimeDate = startTime?.toDate() ?: return "Date not set"
      val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
      val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
      return "${dateFormatter.format(startTimeDate)} • ${timeFormatter.format(startTimeDate)}"
    }

  val lowestPrice: UInt
    get() = pricingTiers.minOfOrNull { it.price.toUInt() } ?: 0u

  val imageUrl: String
    get() = images.firstOrNull() ?: ""

  val isSoldOut: Boolean
    get() = ticketsRemaining <= 0

  val isPublished: Boolean
    get() = status == EventStatus.PUBLISHED

  val canResell: Boolean
    get() = status == EventStatus.PUBLISHED && ticketsIssued > ticketsRedeemed
}

enum class EventStatus {
  DRAFT,
  PUBLISHED,
  CLOSED,
  CANCELLED
}

data class PricingTier(
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val remaining: Int = 0
)
