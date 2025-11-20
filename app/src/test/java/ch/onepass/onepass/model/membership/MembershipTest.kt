package ch.onepass.onepass.model.membership

import ch.onepass.onepass.model.organization.OrganizationRole
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

/** Unit tests for the Membership data class. */
class MembershipTest {

  @Test
  fun membershipHasCorrectDefaults() {
    val membership = Membership()

    assertNull(membership.membershipId)
    assertEquals("", membership.userId)
    assertEquals("", membership.orgId)
    assertEquals(OrganizationRole.MEMBER, membership.role)
    assertNull(membership.createdAt)
    assertNull(membership.updatedAt)
  }

  @Test
  fun membershipCanBeCreatedWithAllValues() {
    val createdAt = Timestamp(1234567890, 123000000)
    val updatedAt = Timestamp(1234567900, 124000000)

    val membership =
        Membership(
            membershipId = "membership_123",
            userId = "user_456",
            orgId = "org_789",
            role = OrganizationRole.OWNER,
            createdAt = createdAt,
            updatedAt = updatedAt)

    assertEquals("membership_123", membership.membershipId)
    assertEquals("user_456", membership.userId)
    assertEquals("org_789", membership.orgId)
    assertEquals(OrganizationRole.OWNER, membership.role)
    assertEquals(createdAt, membership.createdAt)
    assertEquals(updatedAt, membership.updatedAt)
  }

  @Test
  fun membershipWithDifferentRoles() {
    val ownerMembership =
        Membership(userId = "user1", orgId = "org1", role = OrganizationRole.OWNER)
    assertEquals(OrganizationRole.OWNER, ownerMembership.role)

    val memberMembership =
        Membership(userId = "user2", orgId = "org2", role = OrganizationRole.MEMBER)
    assertEquals(OrganizationRole.MEMBER, memberMembership.role)

    val staffMembership =
        Membership(userId = "user3", orgId = "org3", role = OrganizationRole.STAFF)
    assertEquals(OrganizationRole.STAFF, staffMembership.role)
  }

  @Test
  fun membershipCopyWithNewValues() {
    val original =
        Membership(
            membershipId = "membership_123",
            userId = "user_456",
            orgId = "org_789",
            role = OrganizationRole.MEMBER)

    val updated = original.copy(role = OrganizationRole.OWNER, userId = "user_999")

    assertEquals("Membership ID should remain same", "membership_123", updated.membershipId)
    assertEquals("Org ID should remain same", "org_789", updated.orgId)
    assertEquals("Role should be updated", OrganizationRole.OWNER, updated.role)
    assertEquals("User ID should be updated", "user_999", updated.userId)
  }

  @Test
  fun membershipEqualityBasedOnAllFields() {
    val fixedTimestamp = Timestamp(1234567890, 123000000)

    val membership1 =
        Membership(
            membershipId = "same_id",
            userId = "user_1",
            orgId = "org_1",
            role = OrganizationRole.MEMBER,
            createdAt = fixedTimestamp,
            updatedAt = fixedTimestamp)

    val membership2 =
        Membership(
            membershipId = "same_id",
            userId = "user_1",
            orgId = "org_1",
            role = OrganizationRole.MEMBER,
            createdAt = fixedTimestamp,
            updatedAt = fixedTimestamp)

    val membership3 =
        Membership(
            membershipId = "different_id",
            userId = "user_1",
            orgId = "org_1",
            role = OrganizationRole.MEMBER,
            createdAt = fixedTimestamp,
            updatedAt = fixedTimestamp)

    assertEquals("Memberships with same values should be equal", membership1, membership2)
    assertNotEquals("Memberships with different IDs should not be equal", membership1, membership3)
  }

  @Test
  fun membershipEqualityWithDifferentRoles() {
    val baseMembership =
        Membership(
            membershipId = "membership_123",
            userId = "user_456",
            orgId = "org_789",
            role = OrganizationRole.MEMBER)

    val ownerMembership = baseMembership.copy(role = OrganizationRole.OWNER)
    val staffMembership = baseMembership.copy(role = OrganizationRole.STAFF)

    assertNotEquals(
        "Different roles should make memberships unequal", baseMembership, ownerMembership)
    assertNotEquals(
        "Different roles should make memberships unequal", baseMembership, staffMembership)
    assertNotEquals(
        "Different roles should make memberships unequal", ownerMembership, staffMembership)
  }

  @Test
  fun membershipEqualityWithDifferentUserIds() {
    val membership1 = Membership(membershipId = "id1", userId = "user1", orgId = "org1")
    val membership2 = Membership(membershipId = "id1", userId = "user2", orgId = "org1")

    assertNotEquals("Different user IDs should make memberships unequal", membership1, membership2)
  }

  @Test
  fun membershipEqualityWithDifferentOrgIds() {
    val membership1 = Membership(membershipId = "id1", userId = "user1", orgId = "org1")
    val membership2 = Membership(membershipId = "id1", userId = "user1", orgId = "org2")

    assertNotEquals("Different org IDs should make memberships unequal", membership1, membership2)
  }

  @Test
  fun membershipHashCodeConsistency() {
    val membership1 =
        Membership(
            membershipId = "membership_123",
            userId = "user_456",
            orgId = "org_789",
            role = OrganizationRole.MEMBER)

    val membership2 =
        Membership(
            membershipId = "membership_123",
            userId = "user_456",
            orgId = "org_789",
            role = OrganizationRole.MEMBER)

    assertEquals(
        "Equal memberships should have same hashCode",
        membership1.hashCode(),
        membership2.hashCode())
  }

  @Test
  fun membershipToStringIncludesImportantFields() {
    val membership =
        Membership(
            membershipId = "membership_123",
            userId = "user_456",
            orgId = "org_789",
            role = OrganizationRole.OWNER)

    val stringRepresentation = membership.toString()

    assertTrue("Should include membership ID", stringRepresentation.contains("membership_123"))
    assertTrue("Should include user ID", stringRepresentation.contains("user_456"))
    assertTrue("Should include org ID", stringRepresentation.contains("org_789"))
    assertTrue("Should include role", stringRepresentation.contains("OWNER"))
  }

  @Test
  fun membershipWithTimestamps() {
    val createdAt = Timestamp.now()
    val updatedAt = Timestamp.now()

    val membership =
        Membership(
            membershipId = "membership_123",
            userId = "user_456",
            orgId = "org_789",
            role = OrganizationRole.MEMBER,
            createdAt = createdAt,
            updatedAt = updatedAt)

    assertNotNull("CreatedAt should be set", membership.createdAt)
    assertNotNull("UpdatedAt should be set", membership.updatedAt)
    assertEquals(createdAt, membership.createdAt)
    assertEquals(updatedAt, membership.updatedAt)
  }

  @Test
  fun membershipWithoutTimestamps() {
    val membership =
        Membership(
            membershipId = "membership_123",
            userId = "user_456",
            orgId = "org_789",
            role = OrganizationRole.MEMBER)

    assertNull("CreatedAt should be null when not set", membership.createdAt)
    assertNull("UpdatedAt should be null when not set", membership.updatedAt)
  }

  @Test
  fun membershipCanRepresentMultipleUsersInSameOrg() {
    val orgId = "org_123"

    val membership1 =
        Membership(
            membershipId = "m1", userId = "user1", orgId = orgId, role = OrganizationRole.OWNER)
    val membership2 =
        Membership(
            membershipId = "m2", userId = "user2", orgId = orgId, role = OrganizationRole.MEMBER)
    val membership3 =
        Membership(
            membershipId = "m3", userId = "user3", orgId = orgId, role = OrganizationRole.STAFF)

    assertEquals(orgId, membership1.orgId)
    assertEquals(orgId, membership2.orgId)
    assertEquals(orgId, membership3.orgId)
    assertNotEquals(membership1.userId, membership2.userId)
    assertNotEquals(membership2.userId, membership3.userId)
  }

  @Test
  fun membershipCanRepresentSameUserInMultipleOrgs() {
    val userId = "user_123"

    val membership1 =
        Membership(
            membershipId = "m1", userId = userId, orgId = "org1", role = OrganizationRole.OWNER)
    val membership2 =
        Membership(
            membershipId = "m2", userId = userId, orgId = "org2", role = OrganizationRole.MEMBER)
    val membership3 =
        Membership(
            membershipId = "m3", userId = userId, orgId = "org3", role = OrganizationRole.STAFF)

    assertEquals(userId, membership1.userId)
    assertEquals(userId, membership2.userId)
    assertEquals(userId, membership3.userId)
    assertNotEquals(membership1.orgId, membership2.orgId)
    assertNotEquals(membership2.orgId, membership3.orgId)
  }
}
