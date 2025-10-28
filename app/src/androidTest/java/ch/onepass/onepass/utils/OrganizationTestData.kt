package ch.onepass.onepass.utils

import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationMember
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.organization.OrganizationStatus
import com.google.firebase.Timestamp
import java.util.Calendar

/** Helper object to create test data for Organization-related unit tests. */
object OrganizationTestData {
  /** Creates an Organization instance with specified or default parameters. */
  fun createTestOrganization(
      id: String = "org_${System.currentTimeMillis()}",
      name: String = "Test Organization",
      description: String = "Test Description",
      ownerId: String = "owner_test_1",
      status: OrganizationStatus = OrganizationStatus.ACTIVE,
      members: Map<String, OrganizationMember> = emptyMap(),
      verified: Boolean = false,
      profileImageUrl: String? = null,
      website: String? = null,
      instagram: String? = null,
      tiktok: String? = null,
      facebook: String? = null,
      contactEmail: String? = "contact@test.org",
      contactPhone: String? = "+41123456789",
      address: String? = "Test Address",
      eventIds: List<String> = emptyList(),
      followerCount: Int = 0,
      averageRating: Float = 0f,
      createdAt: Timestamp? = Timestamp.Companion.now(),
      updatedAt: Timestamp? = Timestamp.Companion.now()
  ): Organization {
    return Organization(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        status = status,
        members = members,
        verified = verified,
        profileImageUrl = profileImageUrl,
        website = website,
        instagram = instagram,
        tiktok = tiktok,
        facebook = facebook,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
        address = address,
        eventIds = eventIds,
        followerCount = followerCount,
        averageRating = averageRating,
        createdAt = createdAt,
        updatedAt = updatedAt)
  }

  /** Creates an OrganizationMember with specified parameters. */
  fun createTestMember(
      role: OrganizationRole = OrganizationRole.MEMBER,
      joinedAt: Timestamp? = Timestamp.Companion.now(),
      assignedEvents: List<String> = emptyList()
  ): OrganizationMember {
    return OrganizationMember(role = role, joinedAt = joinedAt, assignedEvents = assignedEvents)
  }

  /** Creates an OrganizationInvitation with specified parameters. */
  fun createTestInvitation(
      id: String = "invite_${System.currentTimeMillis()}",
      orgId: String = "org_test_1",
      inviteeEmail: String = "invitee@test.com",
      role: OrganizationRole = OrganizationRole.MEMBER,
      invitedBy: String = "owner_test_1",
      status: InvitationStatus = InvitationStatus.PENDING,
      createdAt: Timestamp? = Timestamp.Companion.now(),
      expiresAt: Timestamp? = createFutureTimestamp(daysFromNow = 7)
  ): OrganizationInvitation {
    return OrganizationInvitation(
        id = id,
        orgId = orgId,
        inviteeEmail = inviteeEmail,
        role = role,
        invitedBy = invitedBy,
        status = status,
        createdAt = createdAt,
        expiresAt = expiresAt)
  }

  /** Creates organizations with different statuses. */
  fun createOrganizationsWithDifferentStatuses(ownerId: String): List<Organization> {
    return listOf(
        createTestOrganization(
            id = "org_pending",
            name = "Pending Organization",
            ownerId = ownerId,
            status = OrganizationStatus.PENDING),
        createTestOrganization(
            id = "org_active",
            name = "Active Organization",
            ownerId = ownerId,
            status = OrganizationStatus.ACTIVE),
        createTestOrganization(
            id = "org_suspended",
            name = "Suspended Organization",
            ownerId = ownerId,
            status = OrganizationStatus.SUSPENDED),
        createTestOrganization(
            id = "org_archived",
            name = "Archived Organization",
            ownerId = ownerId,
            status = OrganizationStatus.ARCHIVED))
  }

  /** Creates an organization with members. */
  fun createOrganizationWithMembers(
      ownerId: String,
      memberIds: List<String> = listOf("member1", "member2", "member3")
  ): Organization {
    val members =
        memberIds.associateWith { memberId ->
          createTestMember(
              role =
                  if (memberId == memberIds.first()) OrganizationRole.OWNER
                  else OrganizationRole.MEMBER)
        }
    return createTestOrganization(
        name = "Organization with Members", ownerId = ownerId, members = members)
  }

  /** Creates a verified organization. */
  fun createVerifiedOrganization(
      ownerId: String,
      followerCount: Int = 100,
      averageRating: Float = 4.5f
  ): Organization {
    return createTestOrganization(
        name = "Verified Organization",
        ownerId = ownerId,
        verified = true,
        followerCount = followerCount,
        averageRating = averageRating,
        status = OrganizationStatus.ACTIVE)
  }

  /** Creates an organization with events. */
  fun createOrganizationWithEvents(ownerId: String, eventCount: Int = 5): Organization {
    val eventIds = (1..eventCount).map { "event_$it" }
    return createTestOrganization(
        name = "Organization with Events",
        ownerId = ownerId,
        eventIds = eventIds,
        status = OrganizationStatus.ACTIVE)
  }

  /** Creates multiple test organizations. */
  fun createTestOrganizations(
      count: Int = 10,
      ownerId: String = "test-owner",
      baseNamePrefix: String = "Test Organization"
  ): List<Organization> {
    return (1..count).map { index ->
      createTestOrganization(
          id = "org_test_$index",
          name = "$baseNamePrefix $index",
          ownerId = ownerId,
          status = if (index % 2 == 0) OrganizationStatus.ACTIVE else OrganizationStatus.PENDING)
    }
  }

  /** Creates invitations with different statuses. */
  fun createInvitationsWithDifferentStatuses(orgId: String): List<OrganizationInvitation> {
    return listOf(
        createTestInvitation(
            id = "invite_pending", orgId = orgId, status = InvitationStatus.PENDING),
        createTestInvitation(
            id = "invite_accepted", orgId = orgId, status = InvitationStatus.ACCEPTED),
        createTestInvitation(
            id = "invite_rejected", orgId = orgId, status = InvitationStatus.REJECTED),
        createTestInvitation(
            id = "invite_expired", orgId = orgId, status = InvitationStatus.EXPIRED),
        createTestInvitation(
            id = "invite_revoked", orgId = orgId, status = InvitationStatus.REVOKED))
  }

  /** Creates a future timestamp by adding specified days to the current date. */
  fun createFutureTimestamp(daysFromNow: Int = 30): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, daysFromNow)
    return Timestamp(calendar.time)
  }

  /** Creates a past timestamp by subtracting specified days from the current date. */
  fun createPastTimestamp(daysAgo: Int = 7): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return Timestamp(calendar.time)
  }

  /** Creates an expired invitation. */
  fun createExpiredInvitation(orgId: String): OrganizationInvitation {
    return createTestInvitation(
        orgId = orgId,
        status = InvitationStatus.PENDING,
        expiresAt = createPastTimestamp(daysAgo = 1))
  }
}
