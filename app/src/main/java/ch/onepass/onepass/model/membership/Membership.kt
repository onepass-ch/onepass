package ch.onepass.onepass.model.membership

import ch.onepass.onepass.model.organization.OrganizationRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a membership relationship between a user and an organization in Cloud Firestore.
 *
 * This table defines the many-to-many relationship between users and organizations,
 * allowing users to belong to multiple organizations with different roles in each.
 *
 * @property membershipId Unique identifier for the membership record (Firestore document ID).
 * @property userId Identifier of the user who is a member of the organization.
 * @property orgId Identifier of the organization the user belongs to.
 * @property role Role assigned to the user within the organization (e.g., OWNER, MEMBER, STAFF).
 * @property createdAt Timestamp when the membership was created (server-set).
 * @property updatedAt Timestamp when the membership was last updated (server-set).
 */
data class Membership(
    val membershipId: String = "",
    val userId: String = "",
    val orgId: String = "",
    val role: OrganizationRole = OrganizationRole.MEMBER,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

