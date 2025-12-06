package ch.onepass.onepass.model.organization

import ch.onepass.onepass.utils.OrganizationTestData
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

/** Unit tests for the Organization data class and related functionality. */
class OrganizationTest {

  @Test
  fun organizationHasCorrectDefaults() {
    val org = Organization()

    assertEquals("", org.id)
    assertEquals("", org.name)
    assertEquals("", org.description)
    assertEquals("", org.ownerId)
    assertEquals(OrganizationStatus.PENDING, org.status)
    assertFalse(org.verified)
    assertNull(org.profileImageUrl)
    assertNull(org.coverImageUrl)
    assertNull(org.website)
    assertNull(org.instagram)
    assertNull(org.tiktok)
    assertNull(org.facebook)
    assertNull(org.contactEmail)
    assertNull(org.contactPhone)
    assertNull(org.phonePrefix)
    assertNull(org.address)
    assertTrue(org.eventIds.isEmpty())
    assertEquals(0, org.followerCount)
    assertEquals(0f, org.averageRating)
    assertNull(org.createdAt)
    assertNull(org.updatedAt)
  }

  @Test
  fun organizationStatusEnumHasAllExpectedValues() {
    val statuses = OrganizationStatus.values()
    assertEquals(4, statuses.size)
    assertTrue(statuses.contains(OrganizationStatus.PENDING))
    assertTrue(statuses.contains(OrganizationStatus.ACTIVE))
    assertTrue(statuses.contains(OrganizationStatus.SUSPENDED))
    assertTrue(statuses.contains(OrganizationStatus.ARCHIVED))
  }

  @Test
  fun organizationRoleEnumHasAllExpectedValues() {
    val roles = OrganizationRole.values()
    assertEquals(3, roles.size)
    assertTrue(roles.contains(OrganizationRole.OWNER))
    assertTrue(roles.contains(OrganizationRole.MEMBER))
    assertTrue(roles.contains(OrganizationRole.STAFF))
  }

  @Test
  fun invitationStatusEnumHasAllExpectedValues() {
    val statuses = InvitationStatus.values()
    assertEquals(5, statuses.size)
    assertTrue(statuses.contains(InvitationStatus.PENDING))
    assertTrue(statuses.contains(InvitationStatus.ACCEPTED))
    assertTrue(statuses.contains(InvitationStatus.REJECTED))
    assertTrue(statuses.contains(InvitationStatus.EXPIRED))
    assertTrue(statuses.contains(InvitationStatus.REVOKED))
  }

  @Test
  fun nameLowerConvertsNameToLowercase() {
    val org = OrganizationTestData.createTestOrganization(name = "Test Organization")
    assertEquals("test organization", org.nameLower)
  }

  @Test
  fun nameLowerHandlesAlreadyLowercaseNames() {
    val org = OrganizationTestData.createTestOrganization(name = "already lower")
    assertEquals("already lower", org.nameLower)
  }

  @Test
  fun nameLowerHandlesMixedCaseNames() {
    val org = OrganizationTestData.createTestOrganization(name = "MixedCase AND UPPER")
    assertEquals("mixedcase and upper", org.nameLower)
  }

  @Test
  fun nameLowerHandlesEmptyNames() {
    val org = OrganizationTestData.createTestOrganization(name = "")
    assertEquals("", org.nameLower)
  }

  @Test
  fun isActiveReturnsTrueForActiveStatus() {
    val org = OrganizationTestData.createTestOrganization(status = OrganizationStatus.ACTIVE)
    assertTrue(org.isActive)
  }

  @Test
  fun isActiveReturnsFalseForNonActiveStatus() {
    val pendingOrg =
        OrganizationTestData.createTestOrganization(status = OrganizationStatus.PENDING)
    assertFalse(pendingOrg.isActive)

    val suspendedOrg =
        OrganizationTestData.createTestOrganization(status = OrganizationStatus.SUSPENDED)
    assertFalse(suspendedOrg.isActive)

    val archivedOrg =
        OrganizationTestData.createTestOrganization(status = OrganizationStatus.ARCHIVED)
    assertFalse(archivedOrg.isActive)
  }

  @Test
  fun eventCountReturnsCorrectCount() {
    val eventIds = listOf("event1", "event2", "event3", "event4")
    val org = OrganizationTestData.createTestOrganization(eventIds = eventIds)
    assertEquals(4, org.eventCount)
  }

  @Test
  fun eventCountReturnsZeroForNoEvents() {
    val org = OrganizationTestData.createTestOrganization(eventIds = emptyList())
    assertEquals(0, org.eventCount)
  }

  @Test
  fun organizationCopyWithNewValues() {
    val original =
        OrganizationTestData.createTestOrganization(
            name = "Original Name", status = OrganizationStatus.PENDING, verified = false)

    val updated =
        original.copy(name = "Updated Name", status = OrganizationStatus.ACTIVE, verified = true)

    assertEquals("ID should remain same", original.id, updated.id)
    assertEquals("Name should be updated", "Updated Name", updated.name)
    assertEquals("Status should be updated", OrganizationStatus.ACTIVE, updated.status)
    assertTrue("Verified should be updated", updated.verified)
  }

  @Test
  fun organizationEqualityBasedOnAllFields() {
    val fixedTimestamp = Timestamp(1234567890, 123000000)

    val org1 =
        OrganizationTestData.createTestOrganization(
            id = "same_id", name = "Org 1", createdAt = fixedTimestamp, updatedAt = fixedTimestamp)

    val org2 =
        OrganizationTestData.createTestOrganization(
            id = "same_id", name = "Org 1", createdAt = fixedTimestamp, updatedAt = fixedTimestamp)

    val org3 =
        OrganizationTestData.createTestOrganization(
            id = "different_id",
            name = "Org 1",
            createdAt = fixedTimestamp,
            updatedAt = fixedTimestamp)

    assertEquals("Organizations with same values should be equal", org1, org2)
    assertNotEquals("Organizations with different IDs should not be equal", org1, org3)
  }

  @Test
  fun organizationToStringIncludesImportantFields() {
    val org =
        OrganizationTestData.createTestOrganization(
            id = "test_123", name = "Test Org", status = OrganizationStatus.ACTIVE)

    val stringRepresentation = org.toString()

    assertTrue("Should include org ID", stringRepresentation.contains("test_123"))
    assertTrue("Should include name", stringRepresentation.contains("Test Org"))
    assertTrue("Should include status", stringRepresentation.contains("ACTIVE"))
  }

  @Test
  fun organizationInvitationHasCorrectDefaults() {
    val invitation = OrganizationInvitation()

    assertEquals("", invitation.id)
    assertEquals("", invitation.orgId)
    assertEquals("", invitation.inviteeEmail)
    assertEquals(OrganizationRole.MEMBER, invitation.role)
    assertEquals("", invitation.invitedBy)
    assertEquals(InvitationStatus.PENDING, invitation.status)
    assertNull(invitation.createdAt)
    assertNull(invitation.expiresAt)
  }

  @Test
  fun organizationInvitationCanBeCreatedWithValues() {
    val createdAt = Timestamp.now()
    val expiresAt = Timestamp.now()

    val invitation =
        OrganizationInvitation(
            id = "invite_123",
            orgId = "org_456",
            inviteeEmail = "user@example.com",
            role = OrganizationRole.STAFF,
            invitedBy = "owner_789",
            status = InvitationStatus.ACCEPTED,
            createdAt = createdAt,
            expiresAt = expiresAt)

    assertEquals("invite_123", invitation.id)
    assertEquals("org_456", invitation.orgId)
    assertEquals("user@example.com", invitation.inviteeEmail)
    assertEquals(OrganizationRole.STAFF, invitation.role)
    assertEquals("owner_789", invitation.invitedBy)
    assertEquals(InvitationStatus.ACCEPTED, invitation.status)
    assertEquals(createdAt, invitation.createdAt)
    assertEquals(expiresAt, invitation.expiresAt)
  }

  @Test
  fun organizationWithMultipleSocialMediaLinks() {
    val org =
        OrganizationTestData.createTestOrganization(
            website = "https://example.com",
            instagram = "@testorg",
            tiktok = "@testorg_tiktok",
            facebook = "facebook.com/testorg")

    assertEquals("https://example.com", org.website)
    assertEquals("@testorg", org.instagram)
    assertEquals("@testorg_tiktok", org.tiktok)
    assertEquals("facebook.com/testorg", org.facebook)
  }

  @Test
  fun organizationWithContactInformation() {
    val org =
        OrganizationTestData.createTestOrganization(
            contactEmail = "contact@example.com",
            contactPhone = "+41123456789",
            address = "Test Street 123, Lausanne")

    assertEquals("contact@example.com", org.contactEmail)
    assertEquals("+41123456789", org.contactPhone)
    assertEquals("Test Street 123, Lausanne", org.address)
  }

  @Test
  fun organizationWithHighFollowerCountAndRating() {
    val org =
        OrganizationTestData.createTestOrganization(followerCount = 10000, averageRating = 4.9f)

    assertEquals(10000, org.followerCount)
    assertEquals(4.9f, org.averageRating)
  }

  @Test
  fun verifiedOrganizationProperties() {
    val org =
        OrganizationTestData.createVerifiedOrganization(
            ownerId = "owner_123", followerCount = 500, averageRating = 4.5f)

    assertTrue("Organization should be verified", org.verified)
    assertEquals(OrganizationStatus.ACTIVE, org.status)
    assertEquals(500, org.followerCount)
    assertEquals(4.5f, org.averageRating)
  }
}
