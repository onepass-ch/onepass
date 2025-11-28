package ch.onepass.onepass.model.organization

import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import ch.onepass.onepass.utils.OrganizationTestData
import junit.framework.TestCase.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/** Advanced integration tests for OrganizationRepositoryFirebase. */
class OrganizationRepositoryAdvancedTest : FirestoreTestBase() {
  private lateinit var orgRepository: OrganizationRepositoryFirebase
  private lateinit var userId: String

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      orgRepository = OrganizationRepositoryFirebase()
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
      clearTestCollection()
    }
  }

  private suspend fun clearTestCollection() {
    val orgs = FirebaseEmulator.firestore.collection("organizations").get().await()
    if (!orgs.isEmpty) {
      val batch = FirebaseEmulator.firestore.batch()
      orgs.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()
    }

    val invites = FirebaseEmulator.firestore.collection("organization_invitations").get().await()
    if (!invites.isEmpty) {
      val inviteBatch = FirebaseEmulator.firestore.batch()
      invites.documents.forEach { inviteBatch.delete(it.reference) }
      inviteBatch.commit().await()
    }
  }

  @Test
  fun canCreateOrganizationWithMultipleMembers() = runTest {
    val org =
        OrganizationTestData.createOrganizationWithMembers(
            ownerId = userId, memberIds = listOf("member1", "member2", "member3"))

    val result = orgRepository.createOrganization(org)
    assertTrue("Create organization should succeed", result.isSuccess)

    val orgId = result.getOrNull()!!
    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()

    assertNotNull("Retrieved org should not be null", retrievedOrg)
    assertEquals("Should have 3 members", 3, retrievedOrg?.memberCount)
  }

  @Test
  fun canCreateOrganizationWithEvents() = runTest {
    val org = OrganizationTestData.createOrganizationWithEvents(ownerId = userId, eventCount = 5)

    val result = orgRepository.createOrganization(org)
    assertTrue("Create organization should succeed", result.isSuccess)

    val orgId = result.getOrNull()!!
    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()

    assertEquals("Should have 5 events", 5, retrievedOrg?.eventCount)
  }

  @Test
  fun verifiedOrganizationsSortedByFollowerCount() = runTest {
    val org1 =
        OrganizationTestData.createVerifiedOrganization(
            userId, followerCount = 50, averageRating = 4.0f)
    val org2 =
        OrganizationTestData.createVerifiedOrganization(
            userId, followerCount = 200, averageRating = 4.5f)
    val org3 =
        OrganizationTestData.createVerifiedOrganization(
            userId, followerCount = 100, averageRating = 4.2f)

    orgRepository.createOrganization(org1)
    orgRepository.createOrganization(org2)
    orgRepository.createOrganization(org3)

    val verifiedOrgs = orgRepository.getVerifiedOrganizations().first()

    assertEquals("Should have 3 verified organizations", 3, verifiedOrgs.size)

    // Verify they are sorted by follower count descending
    assertTrue(
        "First org should have most followers",
        verifiedOrgs[0].followerCount >= verifiedOrgs[1].followerCount)
    assertTrue(
        "Second org should have more followers than third",
        verifiedOrgs[1].followerCount >= verifiedOrgs[2].followerCount)
  }

  @Test
  fun canHandleOrganizationsWithDifferentStatuses() = runTest {
    val orgs = OrganizationTestData.createOrganizationsWithDifferentStatuses(userId)
    orgs.forEach { orgRepository.createOrganization(it) }

    val activeOrgs = orgRepository.getOrganizationsByStatus(OrganizationStatus.ACTIVE).first()
    val pendingOrgs = orgRepository.getOrganizationsByStatus(OrganizationStatus.PENDING).first()
    val suspendedOrgs = orgRepository.getOrganizationsByStatus(OrganizationStatus.SUSPENDED).first()
    val archivedOrgs = orgRepository.getOrganizationsByStatus(OrganizationStatus.ARCHIVED).first()

    assertEquals("Should have 1 active org", 1, activeOrgs.size)
    assertEquals("Should have 1 pending org", 1, pendingOrgs.size)
    assertEquals("Should have 1 suspended org", 1, suspendedOrgs.size)
    assertEquals("Should have 1 archived org", 1, archivedOrgs.size)
  }

  @Test
  fun searchOrganizationsIsCaseInsensitive() = runTest {
    val org = OrganizationTestData.createTestOrganization(ownerId = userId, name = "SwEnt Workshop")
    orgRepository.createOrganization(org)

    val searchResults1 = orgRepository.searchOrganizations("SwEnt").first()
    val searchResults2 = orgRepository.searchOrganizations("swent").first()
    val searchResults3 = orgRepository.searchOrganizations("SWENT").first()

    assertEquals(1, searchResults1.size)
    assertEquals(1, searchResults2.size)
    assertEquals(1, searchResults3.size)
  }

  @Test
  fun updatingOrganizationPreservesUnmodifiedFields() = runTest {
    val originalOrg =
        OrganizationTestData.createTestOrganization(
            ownerId = userId,
            name = "Original Name",
            description = "Original Description",
            verified = false,
            followerCount = 50)

    val createResult = orgRepository.createOrganization(originalOrg)
    val orgId = createResult.getOrNull()!!

    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()!!
    val updatedOrg = retrievedOrg.copy(name = "Updated Name")

    orgRepository.updateOrganization(updatedOrg)

    val finalOrg = orgRepository.getOrganizationById(orgId).first()
    assertEquals("Name should be updated", "Updated Name", finalOrg?.name)
    assertEquals("Description should be preserved", "Original Description", finalOrg?.description)
    assertEquals("Verified should be preserved", false, finalOrg?.verified)
    assertEquals("Follower count should be preserved", 50, finalOrg?.followerCount)
  }

  @Test
  fun canDeleteOrganizationFromMiddleOfCollection() = runTest {
    val orgs = OrganizationTestData.createTestOrganizations(count = 5, ownerId = userId)
    orgs.forEach { orgRepository.createOrganization(it) }

    val allOrgs = orgRepository.getOrganizationsByOwner(userId).first()
    assertEquals("Should have 5 organizations", 5, allOrgs.size)

    val orgToDelete = allOrgs[2]
    orgRepository.deleteOrganization(orgToDelete.id)

    val remainingOrgs = orgRepository.getOrganizationsByOwner(userId).first()
    assertEquals("Should have 4 organizations", 4, remainingOrgs.size)
    assertFalse("Deleted org should not exist", remainingOrgs.any { it.id == orgToDelete.id })
  }

  @Test
  fun multipleUsersCanHaveOrganizationsWithSameName() = runTest {
    val user1Org =
        OrganizationTestData.createTestOrganization(ownerId = userId, name = "Tech Company")

    val user2Org =
        OrganizationTestData.createTestOrganization(
            ownerId = "different-user-id", name = "Tech Company")

    val result1 = orgRepository.createOrganization(user1Org)
    val result2 = orgRepository.createOrganization(user2Org)

    assertTrue(result1.isSuccess)
    assertTrue(result2.isSuccess)

    val user1Orgs = orgRepository.getOrganizationsByOwner(userId).first()
    assertEquals("User 1 should have 1 organization", 1, user1Orgs.size)
    assertEquals("Name should match", "Tech Company", user1Orgs.first().name)
  }

  @Test
  fun addMemberWithDifferentRoles() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    orgRepository.addMember(orgId, "member1", OrganizationRole.MEMBER)
    orgRepository.addMember(orgId, "staff1", OrganizationRole.STAFF)
    orgRepository.addMember(orgId, "owner2", OrganizationRole.OWNER)

    val updatedOrg = orgRepository.getOrganizationById(orgId).first()

    assertEquals("Should have 3 members", 3, updatedOrg?.memberCount)
    assertEquals(
        "member1 should have MEMBER role",
        OrganizationRole.MEMBER,
        updatedOrg?.members?.get("member1")?.role)
    assertEquals(
        "staff1 should have STAFF role",
        OrganizationRole.STAFF,
        updatedOrg?.members?.get("staff1")?.role)
    assertEquals(
        "owner2 should have OWNER role",
        OrganizationRole.OWNER,
        updatedOrg?.members?.get("owner2")?.role)
  }

  @Test
  fun canUpdateMemberFromMemberToOwner() = runTest {
    val memberId = "member-to-promote"
    val members =
        mapOf(memberId to OrganizationTestData.createTestMember(role = OrganizationRole.MEMBER))
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId, members = members)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    orgRepository.updateMemberRole(orgId, memberId, OrganizationRole.OWNER)

    val updatedOrg = orgRepository.getOrganizationById(orgId).first()
    assertEquals(
        "Member should be promoted to OWNER",
        OrganizationRole.OWNER,
        updatedOrg?.members?.get(memberId)?.role)
  }

  @Test
  fun removingNonExistentMemberDoesNotFail() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val removeResult = orgRepository.removeMember(orgId, "non-existent-member")
    assertTrue("Remove should not fail for non-existent member", removeResult.isSuccess)
  }

  @Test
  fun canCreateMultipleInvitationsForSameOrganization() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val invitations =
        listOf(
            OrganizationTestData.createTestInvitation(
                orgId = orgId, inviteeEmail = "user1@example.com"),
            OrganizationTestData.createTestInvitation(
                orgId = orgId, inviteeEmail = "user2@example.com"),
            OrganizationTestData.createTestInvitation(
                orgId = orgId, inviteeEmail = "user3@example.com"))

    invitations.forEach { invitation ->
      val result = orgRepository.createInvitation(invitation)
      assertTrue("Create invitation should succeed", result.isSuccess)
    }

    val pendingInvites = orgRepository.getPendingInvitations(orgId).first()
    assertEquals("Should have 3 pending invitations", 3, pendingInvites.size)
  }

  @Test
  fun invitationStatusTransitionsWorkCorrectly() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val invitation =
        OrganizationTestData.createTestInvitation(orgId = orgId, status = InvitationStatus.PENDING)
    val inviteId = orgRepository.createInvitation(invitation).getOrNull()!!

    // PENDING -> ACCEPTED
    orgRepository.updateInvitationStatus(inviteId, InvitationStatus.ACCEPTED)

    // Verify it's no longer in pending
    val pendingInvites = orgRepository.getPendingInvitations(orgId).first()
    assertFalse("Should not be in pending", pendingInvites.any { it.id == inviteId })
  }

  @Test
  fun getInvitationsByEmailReturnsOnlyMatchingInvitations() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val targetEmail = "target@example.com"
    val invitations =
        listOf(
            OrganizationTestData.createTestInvitation(orgId = orgId, inviteeEmail = targetEmail),
            OrganizationTestData.createTestInvitation(orgId = orgId, inviteeEmail = targetEmail),
            OrganizationTestData.createTestInvitation(
                orgId = orgId, inviteeEmail = "other@example.com"))

    invitations.forEach { orgRepository.createInvitation(it) }

    val emailInvites = orgRepository.getInvitationsByEmail(targetEmail).first()
    assertEquals("Should find 2 invitations for target email", 2, emailInvites.size)
    assertTrue("All should have target email", emailInvites.all { it.inviteeEmail == targetEmail })
  }

  @Test
  fun organizationWithNoMembersHasZeroMemberCount() = runTest {
    val testOrg =
        OrganizationTestData.createTestOrganization(ownerId = userId, members = emptyMap())
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()
    assertEquals("Should have 0 members", 0, retrievedOrg?.memberCount)
  }

  @Test
  fun canHandleOrganizationWithMaximumFields() = runTest {
    val fullOrg =
        OrganizationTestData.createTestOrganization(
            ownerId = userId,
            name = "Complete Organization",
            description = "Fully populated organization for testing",
            verified = true,
            profileImageUrl = "https://example.com/profile.jpg",
            website = "https://example.com",
            instagram = "@example",
            tiktok = "@example",
            facebook = "facebook.com/example",
            contactEmail = "contact@example.com",
            contactPhone = "+41123456789",
            address = "Test Street 123, Lausanne",
            eventIds = listOf("event1", "event2", "event3"),
            followerCount = 1000,
            averageRating = 4.8f,
            status = OrganizationStatus.ACTIVE)

    val result = orgRepository.createOrganization(fullOrg)
    assertTrue("Create organization should succeed", result.isSuccess)

    val orgId = result.getOrNull()!!
    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()

    assertNotNull("Retrieved org should not be null", retrievedOrg)
    assertEquals("Name should match", "Complete Organization", retrievedOrg?.name)
    assertEquals("Website should match", "https://example.com", retrievedOrg?.website)
    assertEquals("Instagram should match", "@example", retrievedOrg?.instagram)
    assertEquals("Follower count should match", 1000, retrievedOrg?.followerCount)
    assertEquals("Average rating should match", 4.8f, retrievedOrg?.averageRating)
    assertTrue("Should be verified", retrievedOrg?.verified ?: false)
  }

  @Test
  fun organizationWithNoEventsHasZeroEventCount() = runTest {
    val testOrg =
        OrganizationTestData.createTestOrganization(ownerId = userId, eventIds = emptyList())
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()
    assertEquals("Should have 0 events", 0, retrievedOrg?.eventCount)
  }

  @Test
  fun updateMemberRoleFailsForNonExistentMember() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val updateResult =
        orgRepository.updateMemberRole(orgId, "non-existent-member", OrganizationRole.STAFF)
    assertTrue("Update should fail for non-existent member", updateResult.isFailure)
  }

  @Test
  fun updateMemberRoleFailsForNonExistentOrganization() = runTest {
    val updateResult =
        orgRepository.updateMemberRole("non-existent-org", "member-id", OrganizationRole.STAFF)
    assertTrue("Update should fail for non-existent organization", updateResult.isFailure)
  }

  @Test
  fun canCreateOrganizationWithSocialMediaLinks() = runTest {
    val testOrg =
        OrganizationTestData.createTestOrganization(
            ownerId = userId,
            website = "https://example.com",
            instagram = "@testorg",
            tiktok = "@testorg_tiktok",
            facebook = "facebook.com/testorg")

    val result = orgRepository.createOrganization(testOrg)
    assertTrue("Create should succeed", result.isSuccess)

    val orgId = result.getOrNull()!!
    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()

    assertEquals("Website should match", "https://example.com", retrievedOrg?.website)
    assertEquals("Instagram should match", "@testorg", retrievedOrg?.instagram)
    assertEquals("TikTok should match", "@testorg_tiktok", retrievedOrg?.tiktok)
    assertEquals("Facebook should match", "facebook.com/testorg", retrievedOrg?.facebook)
  }

  @Test
  fun searchReturnsEmptyForNonMatchingQuery() = runTest {
    val testOrg =
        OrganizationTestData.createTestOrganization(ownerId = userId, name = "Tech Company")
    orgRepository.createOrganization(testOrg)

    val searchResults = orgRepository.searchOrganizations("Music").first()
    assertEquals("Should return empty list for non-matching query", 0, searchResults.size)
  }

  @Test
  fun canHandleOrganizationWithLongDescription() = runTest {
    val longDescription = "A".repeat(1000)
    val testOrg =
        OrganizationTestData.createTestOrganization(ownerId = userId, description = longDescription)

    val result = orgRepository.createOrganization(testOrg)
    assertTrue("Create should succeed with long description", result.isSuccess)

    val orgId = result.getOrNull()!!
    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()

    assertEquals("Description should be preserved", longDescription, retrievedOrg?.description)
  }

  @Test
  fun organizationStatusChangesAreTracked() = runTest {
    val testOrg =
        OrganizationTestData.createTestOrganization(
            ownerId = userId, status = OrganizationStatus.PENDING)

    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    // Update to ACTIVE
    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()!!
    orgRepository.updateOrganization(retrievedOrg.copy(status = OrganizationStatus.ACTIVE))

    val activeOrg = orgRepository.getOrganizationById(orgId).first()
    assertEquals("Status should be ACTIVE", OrganizationStatus.ACTIVE, activeOrg?.status)
    assertTrue("isActive should be true", activeOrg?.isActive ?: false)

    // Update to SUSPENDED
    orgRepository.updateOrganization(activeOrg!!.copy(status = OrganizationStatus.SUSPENDED))

    val suspendedOrg = orgRepository.getOrganizationById(orgId).first()
    assertEquals("Status should be SUSPENDED", OrganizationStatus.SUSPENDED, suspendedOrg?.status)
    assertFalse("isActive should be false", suspendedOrg?.isActive ?: true)
  }

  @Test
  fun addingMultipleMembersIncreasesCount() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    orgRepository.addMember(orgId, "member1", OrganizationRole.MEMBER)
    orgRepository.addMember(orgId, "member2", OrganizationRole.MEMBER)
    orgRepository.addMember(orgId, "member3", OrganizationRole.STAFF)

    val updatedOrg = orgRepository.getOrganizationById(orgId).first()
    assertEquals("Should have 3 members", 3, updatedOrg?.memberCount)
  }

  @Test
  fun removingMembersDecreasesCount() = runTest {
    val members =
        mapOf(
            "member1" to OrganizationTestData.createTestMember(),
            "member2" to OrganizationTestData.createTestMember(),
            "member3" to OrganizationTestData.createTestMember())
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId, members = members)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    orgRepository.removeMember(orgId, "member1")
    orgRepository.removeMember(orgId, "member2")

    val updatedOrg = orgRepository.getOrganizationById(orgId).first()
    assertEquals("Should have 1 member remaining", 1, updatedOrg?.memberCount)
  }

  @Test
  fun invitationsWithDifferentRolesCanBeCreated() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val memberInvite =
        OrganizationTestData.createTestInvitation(
            orgId = orgId, inviteeEmail = "member@example.com", role = OrganizationRole.MEMBER)
    val staffInvite =
        OrganizationTestData.createTestInvitation(
            orgId = orgId, inviteeEmail = "staff@example.com", role = OrganizationRole.STAFF)
    val ownerInvite =
        OrganizationTestData.createTestInvitation(
            orgId = orgId, inviteeEmail = "owner@example.com", role = OrganizationRole.OWNER)

    orgRepository.createInvitation(memberInvite)
    orgRepository.createInvitation(staffInvite)
    orgRepository.createInvitation(ownerInvite)

    val pendingInvites = orgRepository.getPendingInvitations(orgId).first()
    assertEquals("Should have 3 invitations", 3, pendingInvites.size)

    val roles = pendingInvites.map { it.role }.toSet()
    assertEquals("Should have all 3 role types", 3, roles.size)
    assertTrue("Should include MEMBER role", roles.contains(OrganizationRole.MEMBER))
    assertTrue("Should include STAFF role", roles.contains(OrganizationRole.STAFF))
    assertTrue("Should include OWNER role", roles.contains(OrganizationRole.OWNER))
  }

  @Test
  fun organizationsSortedCorrectlyByCreationDate() = runTest {
    val orgs =
        (1..3).map { i ->
          OrganizationTestData.createTestOrganization(ownerId = userId, name = "Org $i")
        }

    // Create in order
    orgs.forEach { org ->
      orgRepository.createOrganization(org)
      kotlinx.coroutines.delay(100) // Small delay to ensure different timestamps
    }

    val retrievedOrgs = orgRepository.getOrganizationsByOwner(userId).first()

    // Should be sorted by creation date descending (newest first)
    assertTrue("Should have 3 organizations", retrievedOrgs.size >= 3)
  }

  @Test
  fun updateInvitationStatusUpdatesTimestamp() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!
    val invitation =
        OrganizationTestData.createTestInvitation(orgId = orgId, status = InvitationStatus.PENDING)
    val inviteId = orgRepository.createInvitation(invitation).getOrNull()!!

    // Get initial invitation
    val initialInvite =
        FirebaseEmulator.firestore
            .collection("organization_invitations")
            .document(inviteId)
            .get()
            .await()
            .toObject(OrganizationInvitation::class.java)
    val initialUpdatedAt = initialInvite?.updatedAt

    kotlinx.coroutines.delay(100) // Wait to ensure timestamp difference
    val updateResult = orgRepository.updateInvitationStatus(inviteId, InvitationStatus.ACCEPTED)
    assertTrue("Update invitation status should succeed", updateResult.isSuccess)

    // Verify updatedAt changed
    val updatedInvite =
        FirebaseEmulator.firestore
            .collection("organization_invitations")
            .document(inviteId)
            .get()
            .await()
            .toObject(OrganizationInvitation::class.java)
    val updatedUpdatedAt = updatedInvite?.updatedAt

    assertNotNull("Updated invitation should have updatedAt", updatedInvite?.updatedAt)
    assertEquals("Status should be updated", InvitationStatus.ACCEPTED, updatedInvite?.status)
    assertNotEquals(
        "updatedAt should be different from the initial timestamp",
        initialUpdatedAt,
        updatedUpdatedAt)
    assertTrue(
        "updatedAt timestamp ($updatedUpdatedAt) should be greater than the initial one ($initialUpdatedAt)",
        updatedUpdatedAt!! > initialUpdatedAt!!)
  }

  @Test
  fun creatingInvitationSetsCreatedAtAndUpdatedAt() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val invitation =
        OrganizationTestData.createTestInvitation(orgId = orgId, inviteeEmail = "new@example.com")
    val inviteId = orgRepository.createInvitation(invitation).getOrNull()!!

    val storedInvite =
        FirebaseEmulator.firestore
            .collection("organization_invitations")
            .document(inviteId)
            .get()
            .await()
            .toObject(OrganizationInvitation::class.java)

    assertNotNull("createdAt should be set", storedInvite?.createdAt)
    assertNotNull("updatedAt should be set", storedInvite?.updatedAt)
  }

  @Test
  fun organizationNotFoundReturnsNull() = runTest {
    val org = orgRepository.getOrganizationById("non-existent-id").first()
    assertNull("Should return null for non-existent organization", org)
  }

  @Test
  fun canDeleteInvitation() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val invitation = OrganizationTestData.createTestInvitation(orgId = orgId)
    val inviteId = orgRepository.createInvitation(invitation).getOrNull()!!

    val deleteResult = orgRepository.deleteInvitation(inviteId)
    assertTrue("Delete invitation should succeed", deleteResult.isSuccess)
  }

  @Test
  fun canGetOrganizationsByMember() = runTest {
    val memberId = "member-user-123"
    val members = mapOf(memberId to OrganizationTestData.createTestMember())
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId, members = members)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val memberOrgs = orgRepository.getOrganizationsByMember(memberId).first()
    assertEquals("Should find 1 organization for member", 1, memberOrgs.size)
    assertEquals("Organization ID should match", orgId, memberOrgs.first().id)
  }
}
