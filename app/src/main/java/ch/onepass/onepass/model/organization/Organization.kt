package ch.onepass.onepass.model.organization

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

/**
 * Represents an organization stored in Cloud Firestore.
 *
 * @property id Unique identifier of the organization (Firestore document ID).
 * @property name Name of the organization.
 * @property description Short description of the organization's purpose or activity.
 * @property ownerId User ID of the organization owner.
 * @property status Current lifecycle status of the organization.
 * @property verified Whether the organization has been verified by an admin.
 * @property profileImageUrl Optional URL to the organization's profile image.
 * @property coverImageUrl Optional URL to the organization's cover/banner image.
 * @property website Optional public website link.
 * @property instagram Optional Instagram profile link or handle.
 * @property tiktok Optional TikTok profile link or handle.
 * @property facebook Optional Facebook profile link or handle.
 * @property contactEmail Optional contact email address.
 * @property contactPhone Optional contact phone number.
 * @property address Optional address of the organization.
 * @property eventIds List of IDs for events associated with this organization.
 * @property followerCount Number of users following the organization.
 * @property averageRating Average rating across the organization's events.
 * @property createdAt Timestamp when the organization was created (server-set).
 * @property updatedAt Timestamp when the organization was last updated (server-set).
 */
data class Organization(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val status: OrganizationStatus = OrganizationStatus.PENDING,
    val verified: Boolean = false,
    val profileImageUrl: String? = null,
    val coverImageUrl: String? = null,
    val website: String? = null,
    val instagram: String? = null,
    val tiktok: String? = null,
    val facebook: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val phonePrefix: String? = null,
    val address: String? = null,
    val eventIds: List<String> = emptyList(),
    val followerCount: Int = 0,
    val averageRating: Float = 0f,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,

    // Stripe Connect integration for payment processing
    val stripeConnectedAccountId: String? = null, // Stripe Connect account ID
    val stripeAccountStatus: String? = null, // "incomplete", "complete", or "error"
    val stripeChargesEnabled: Boolean = false, // Can accept payments
    val stripePayoutsEnabled: Boolean = false, // Can receive payouts
    val stripeAccountError: String? = null // Error message if account creation failed
) {
  /** Returns organization name in lowercase for case-insensitive searching. */
  val nameLower: String
    get() = name.lowercase(Locale.ROOT)

  /** Indicates if the organization is currently active. */
  val isActive: Boolean
    get() = status == OrganizationStatus.ACTIVE

  /** Returns the number of events created by this organization. */
  val eventCount: Int
    get() = eventIds.size
}

/**
 * Represents an invitation to join an organization.
 *
 * @property id Unique identifier for the invitation.
 * @property orgId ID of the organization the invitation belongs to.
 * @property inviteeEmail Email of the user invited to join.
 * @property role Role to assign to the user upon acceptance.
 * @property invitedBy User ID of the inviter (must be an OWNER or ADMIN).
 * @property status Current status of the invitation (e.g., PENDING, ACCEPTED).
 * @property createdAt Timestamp when the invitation was created (server-set).
 * @property updatedAt Timestamp when the invitation was last updated (server-set).
 * @property expiresAt Timestamp when the invitation automatically expires.
 */
data class OrganizationInvitation(
    val id: String = "",
    val orgId: String = "",
    val inviteeEmail: String = "",
    val role: OrganizationRole = OrganizationRole.MEMBER,
    val invitedBy: String = "",
    val status: InvitationStatus = InvitationStatus.PENDING,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

/** Lifecycle states of an organization. */
enum class OrganizationStatus {
  /** The organization is created but not yet fully approved or active. */
  PENDING,
  /** The organization is active and operational. */
  ACTIVE,
  /** The organization has been temporarily suspended, possibly due to a violation. */
  SUSPENDED,
  /** The organization is inactive and stored for historical purposes. */
  ARCHIVED
}

/** Defines the hierarchy of roles within an organization. */
enum class OrganizationRole {
  /** **Full control** over the organization (manage members, events, and settings). */
  OWNER,
  /** **Administrator** role, can manage members (except Owner) and events. */
  ADMIN,
  /** **Standard member** role, typically allowed to create or manage assigned events. */
  MEMBER,
  /** **On-site staff** role, authorized to perform tasks like ticket scanning and validation. */
  STAFF
}

/** Represents the possible statuses of an organization invitation. */
enum class InvitationStatus {
  /** The invitation has been sent and is awaiting a response. */
  PENDING,
  /** The invited user has accepted the invitation. */
  ACCEPTED,
  /** The invited user has declined the invitation. */
  REJECTED,
  /** The invitation has passed its expiration time without being accepted. */
  EXPIRED,
  /** The invitation was manually canceled by the inviter/admin. */
  REVOKED
}
