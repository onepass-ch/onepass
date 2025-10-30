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

/** Integration tests for OrganizationRepositoryFirebase using Firebase Emulator. */
class OrganizationRepositoryFirebaseTest : FirestoreTestBase() {
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
    if (orgs.isEmpty) return

    val batch = FirebaseEmulator.firestore.batch()
    orgs.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()

    val invites = FirebaseEmulator.firestore.collection("organization_invitations").get().await()
    if (!invites.isEmpty) {
      val inviteBatch = FirebaseEmulator.firestore.batch()
      invites.documents.forEach { inviteBatch.delete(it.reference) }
      inviteBatch.commit().await()
    }
  }

  private suspend fun getOrganizationsCount(): Int {
    return FirebaseEmulator.firestore.collection("organizations").get().await().size()
  }

  @Test
  fun canCreateOrganization() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)

    val result = orgRepository.createOrganization(testOrg)
    assertTrue("Create organization should succeed", result.isSuccess)

    val orgId = result.getOrNull()
    assertNotNull("Organization ID should not be null", orgId)
    assertEquals("Should have 1 organization in repository", 1, getOrganizationsCount())

    val storedOrg = orgRepository.getOrganizationById(orgId!!).first()
    assertNotNull("Stored organization should not be null", storedOrg)
    assertEquals("Owner ID should match", userId, storedOrg?.ownerId)
    assertEquals("Status should match", OrganizationStatus.ACTIVE, storedOrg?.status)
  }

  @Test
  fun canRetrieveOrganizationById() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val createResult = orgRepository.createOrganization(testOrg)
    val orgId = createResult.getOrNull()!!

    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()
    assertNotNull("Organization should be found", retrievedOrg)
    assertEquals("Organization ID should match", orgId, retrievedOrg?.id)
    assertEquals("Owner ID should match", userId, retrievedOrg?.ownerId)
  }

  @Test
  fun canUpdateOrganization() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val createResult = orgRepository.createOrganization(testOrg)
    val orgId = createResult.getOrNull()!!

    val retrievedOrg = orgRepository.getOrganizationById(orgId).first()!!
    val updatedOrg =
        retrievedOrg.copy(
            name = "Updated Organization Name",
            description = "Updated description",
            verified = true)

    val updateResult = orgRepository.updateOrganization(updatedOrg)
    assertTrue("Update should succeed", updateResult.isSuccess)

    val finalOrg = orgRepository.getOrganizationById(orgId).first()
    assertEquals("Name should be updated", "Updated Organization Name", finalOrg?.name)
    assertEquals("Description should be updated", "Updated description", finalOrg?.description)
    assertTrue("Verified should be updated", finalOrg?.verified ?: false)
  }

  @Test
  fun canDeleteOrganization() = runTest {
    val testOrg1 = OrganizationTestData.createTestOrganization(ownerId = userId)
    val testOrg2 = OrganizationTestData.createTestOrganization(ownerId = userId, name = "Org 2")

    orgRepository.createOrganization(testOrg1)
    val org2Id = orgRepository.createOrganization(testOrg2).getOrNull()!!

    assertEquals("Should start with 2 organizations", 2, getOrganizationsCount())

    val deleteResult = orgRepository.deleteOrganization(org2Id)
    assertTrue("Delete should succeed", deleteResult.isSuccess)

    assertEquals("Should have 1 organization remaining", 1, getOrganizationsCount())
  }

  @Test
  fun canGetOrganizationsByOwner() = runTest {
    val testOrg1 = OrganizationTestData.createTestOrganization(ownerId = userId, name = "Org 1")
    val testOrg2 = OrganizationTestData.createTestOrganization(ownerId = userId, name = "Org 2")
    val testOrg3 =
        OrganizationTestData.createTestOrganization(ownerId = "different-owner", name = "Org 3")

    orgRepository.createOrganization(testOrg1)
    orgRepository.createOrganization(testOrg2)
    orgRepository.createOrganization(testOrg3)

    val userOrgs = orgRepository.getOrganizationsByOwner(userId).first()
    assertEquals("Should get 2 organizations for user", 2, userOrgs.size)
    assertTrue("All should belong to user", userOrgs.all { it.ownerId == userId })
  }

  @Test
  fun canGetOrganizationsByStatus() = runTest {
    val orgs = OrganizationTestData.createOrganizationsWithDifferentStatuses(userId)
    orgs.forEach { orgRepository.createOrganization(it) }

    val activeOrgs = orgRepository.getOrganizationsByStatus(OrganizationStatus.ACTIVE).first()
    assertEquals("Should have 1 active organization", 1, activeOrgs.size)
    assertEquals("Should be active status", OrganizationStatus.ACTIVE, activeOrgs.first().status)

    val pendingOrgs = orgRepository.getOrganizationsByStatus(OrganizationStatus.PENDING).first()
    assertEquals("Should have 1 pending organization", 1, pendingOrgs.size)
  }

  @Test
  fun canSearchOrganizations() = runTest {
    val testOrg1 =
        OrganizationTestData.createTestOrganization(ownerId = userId, name = "Tech Startup")
    val testOrg2 =
        OrganizationTestData.createTestOrganization(ownerId = userId, name = "Tech Conference")
    val testOrg3 =
        OrganizationTestData.createTestOrganization(ownerId = userId, name = "Music Festival")

    orgRepository.createOrganization(testOrg1)
    orgRepository.createOrganization(testOrg2)
    orgRepository.createOrganization(testOrg3)

    val searchResults = orgRepository.searchOrganizations("Tech").first()
    assertEquals("Should find 2 tech organizations", 2, searchResults.size)
    assertTrue(
        "All should contain 'tech'",
        searchResults.all { it.name.contains("Tech", ignoreCase = true) })
  }

  @Test
  fun canGetVerifiedOrganizations() = runTest {
    val verifiedOrg1 = OrganizationTestData.createVerifiedOrganization(userId, followerCount = 100)
    val verifiedOrg2 = OrganizationTestData.createVerifiedOrganization(userId, followerCount = 200)
    val unverifiedOrg =
        OrganizationTestData.createTestOrganization(ownerId = userId, verified = false)

    orgRepository.createOrganization(verifiedOrg1)
    orgRepository.createOrganization(verifiedOrg2)
    orgRepository.createOrganization(unverifiedOrg)

    val verifiedOrgs = orgRepository.getVerifiedOrganizations().first()
    assertEquals("Should have 2 verified organizations", 2, verifiedOrgs.size)
    assertTrue("All should be verified", verifiedOrgs.all { it.verified })
  }

  @Test
  fun canAddMember() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val newMemberId = "new-member-123"
    val addResult = orgRepository.addMember(orgId, newMemberId, OrganizationRole.MEMBER)
    assertTrue("Add member should succeed", addResult.isSuccess)

    val updatedOrg = orgRepository.getOrganizationById(orgId).first()
    assertNotNull("Organization should have members", updatedOrg?.members)
    assertTrue("Member should be added", updatedOrg?.members?.containsKey(newMemberId) ?: false)
    assertEquals(
        "Member role should be correct",
        OrganizationRole.MEMBER,
        updatedOrg?.members?.get(newMemberId)?.role)
  }

  @Test
  fun canRemoveMember() = runTest {
    val memberId = "member-to-remove"
    val members = mapOf(memberId to OrganizationTestData.createTestMember())
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId, members = members)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val removeResult = orgRepository.removeMember(orgId, memberId)
    assertTrue("Remove member should succeed", removeResult.isSuccess)

    val updatedOrg = orgRepository.getOrganizationById(orgId).first()
    assertFalse("Member should be removed", updatedOrg?.members?.containsKey(memberId) ?: true)
  }

  @Test
  fun canUpdateMemberRole() = runTest {
    val memberId = "member-to-update"
    val members =
        mapOf(memberId to OrganizationTestData.createTestMember(role = OrganizationRole.MEMBER))
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId, members = members)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val updateResult = orgRepository.updateMemberRole(orgId, memberId, OrganizationRole.STAFF)
    assertTrue("Update member role should succeed", updateResult.isSuccess)

    val updatedOrg = orgRepository.getOrganizationById(orgId).first()
    assertEquals(
        "Member role should be updated",
        OrganizationRole.STAFF,
        updatedOrg?.members?.get(memberId)?.role)
  }

  @Test
  fun canCreateInvitation() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val invitation =
        OrganizationTestData.createTestInvitation(orgId = orgId, inviteeEmail = "test@example.com")
    val result = orgRepository.createInvitation(invitation)

    assertTrue("Create invitation should succeed", result.isSuccess)
    assertNotNull("Invitation ID should not be null", result.getOrNull())
  }

  @Test
  fun canGetPendingInvitations() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val invitations = OrganizationTestData.createInvitationsWithDifferentStatuses(orgId)
    invitations.forEach { orgRepository.createInvitation(it) }

    val pendingInvites = orgRepository.getPendingInvitations(orgId).first()
    assertEquals("Should have 1 pending invitation", 1, pendingInvites.size)
    assertEquals(
        "Should be pending status", InvitationStatus.PENDING, pendingInvites.first().status)
  }

  @Test
  fun canGetInvitationsByEmail() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val email = "specific@example.com"
    val invitation1 = OrganizationTestData.createTestInvitation(orgId = orgId, inviteeEmail = email)
    val invitation2 =
        OrganizationTestData.createTestInvitation(orgId = orgId, inviteeEmail = "other@example.com")

    orgRepository.createInvitation(invitation1)
    orgRepository.createInvitation(invitation2)

    val emailInvites = orgRepository.getInvitationsByEmail(email).first()
    assertEquals("Should find 1 invitation for email", 1, emailInvites.size)
    assertEquals("Email should match", email, emailInvites.first().inviteeEmail)
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

    kotlinx.coroutines.delay(100) // Wait a moment to ensure timestamp difference
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
  fun canDeleteInvitation() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(ownerId = userId)
    val orgId = orgRepository.createOrganization(testOrg).getOrNull()!!

    val invitation = OrganizationTestData.createTestInvitation(orgId = orgId)
    val inviteId = orgRepository.createInvitation(invitation).getOrNull()!!

    val deleteResult = orgRepository.deleteInvitation(inviteId)
    assertTrue("Delete invitation should succeed", deleteResult.isSuccess)
  }

  @Test
  fun organizationNotFoundReturnsNull() = runTest {
    val org = orgRepository.getOrganizationById("non-existent-id").first()
    assertNull("Should return null for non-existent organization", org)
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
}
