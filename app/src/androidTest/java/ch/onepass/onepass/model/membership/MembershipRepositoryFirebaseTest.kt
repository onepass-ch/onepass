package ch.onepass.onepass.model.membership

import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import junit.framework.TestCase.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for MembershipRepositoryFirebase using Firebase Emulator.
 *
 * These tests verify CRUD operations and queries against a real Firestore instance running in the
 * emulator. This ensures the repository implementation works correctly with actual Firebase APIs.
 *
 * Prerequisites:
 * - Firebase emulators must be running (firebase emulators:start)
 * - Test uses anonymous authentication
 *
 * @see FirestoreTestBase for setup/teardown logic
 */
class MembershipRepositoryFirebaseTest : FirestoreTestBase() {
  private lateinit var membershipRepository: MembershipRepositoryFirebase
  private lateinit var userId: String

  // Test constants
  private val testOrgId = "test_org_1"
  private val testOrgId2 = "test_org_2"
  private val testOrgId3 = "test_org_3"
  private val testUserId1 = "test_user_1"
  private val testUserId2 = "test_user_2"
  private val testUserId3 = "test_user_3"
  private val testRoleMember = OrganizationRole.MEMBER
  private val testRoleOwner = OrganizationRole.OWNER
  private val testRoleStaff = OrganizationRole.STAFF

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      membershipRepository = MembershipRepositoryFirebase()

      // Sign in anonymously for testing
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"

      // Clear any existing test data
      clearTestCollection()
    }
  }

  /** Clears all documents in the "memberships" collection to ensure a clean state. */
  private suspend fun clearTestCollection() {
    val memberships = FirebaseEmulator.firestore.collection("memberships").get().await()
    if (memberships.isEmpty) return

    val batch = FirebaseEmulator.firestore.batch()
    memberships.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()
  }

  /** Helper to get the current count of memberships in the test collection. */
  private suspend fun getMembershipsCount(): Int {
    return FirebaseEmulator.firestore.collection("memberships").get().await().size()
  }

  @Test
  fun canAddMembership() = runTest {
    val result = membershipRepository.addMembership(userId, testOrgId, testRoleMember)

    assertTrue("Add membership should succeed", result.isSuccess)
    val membershipId = result.getOrNull()
    assertNotNull("Membership ID should not be null", membershipId)
    assertEquals("Should have 1 membership in repository", 1, getMembershipsCount())

    // Verify membership exists
    val exists = membershipRepository.hasMembership(userId, testOrgId)
    assertTrue("Membership should exist", exists)
  }

  @Test
  fun addMembershipFailsWhenAlreadyExists() = runTest {
    // Create first membership
    val firstResult = membershipRepository.addMembership(userId, testOrgId, testRoleMember)
    assertTrue("First add should succeed", firstResult.isSuccess)

    // Try to add again
    val secondResult = membershipRepository.addMembership(userId, testOrgId, testRoleMember)
    assertTrue("Second add should fail", secondResult.isFailure)
    assertTrue(
        "Should throw IllegalStateException",
        secondResult.exceptionOrNull() is IllegalStateException)
    assertEquals("Should still have only 1 membership", 1, getMembershipsCount())
  }

  @Test
  fun canAddMembershipWithDifferentRoles() = runTest {
    val result1 = membershipRepository.addMembership(userId, testOrgId, testRoleOwner)
    val result2 = membershipRepository.addMembership(userId, testOrgId2, testRoleMember)
    val result3 = membershipRepository.addMembership(userId, testOrgId3, testRoleStaff)

    assertTrue("Add OWNER should succeed", result1.isSuccess)
    assertTrue("Add MEMBER should succeed", result2.isSuccess)
    assertTrue("Add STAFF should succeed", result3.isSuccess)
    assertEquals("Should have 3 memberships", 3, getMembershipsCount())
  }

  @Test
  fun canRemoveMembership() = runTest {
    // Create membership first
    membershipRepository.addMembership(userId, testOrgId, testRoleMember)
    assertEquals("Should have 1 membership", 1, getMembershipsCount())

    // Remove membership
    val result = membershipRepository.removeMembership(userId, testOrgId)
    assertTrue("Remove membership should succeed", result.isSuccess)
    assertEquals("Should have 0 memberships", 0, getMembershipsCount())

    // Verify membership no longer exists
    val exists = membershipRepository.hasMembership(userId, testOrgId)
    assertFalse("Membership should not exist", exists)
  }

  @Test
  fun removeMembershipFailsWhenNotExists() = runTest {
    val result = membershipRepository.removeMembership(userId, testOrgId)
    assertTrue("Remove should fail", result.isFailure)
    assertTrue(
        "Should throw IllegalStateException", result.exceptionOrNull() is IllegalStateException)
  }

  @Test
  fun canUpdateMembership() = runTest {
    // Create membership
    membershipRepository.addMembership(userId, testOrgId, testRoleMember)

    // Update membership
    val result = membershipRepository.updateMembership(userId, testOrgId, testRoleOwner)
    assertTrue("Update membership should succeed", result.isSuccess)

    // Verify role was updated
    val memberships = membershipRepository.getOrganizationsByUser(userId)
    val membership = memberships.find { it.orgId == testOrgId }
    assertNotNull("Membership should exist", membership)
    assertEquals("Role should be updated", testRoleOwner, membership?.role)
  }

  @Test
  fun updateMembershipFailsWhenNotExists() = runTest {
    val result = membershipRepository.updateMembership(userId, testOrgId, testRoleOwner)
    assertTrue("Update should fail", result.isFailure)
    assertTrue(
        "Should throw IllegalStateException", result.exceptionOrNull() is IllegalStateException)
  }

  @Test
  fun canUpdateMembershipToDifferentRoles() = runTest {
    // Create with MEMBER
    membershipRepository.addMembership(userId, testOrgId, testRoleMember)

    // Update to OWNER
    var result = membershipRepository.updateMembership(userId, testOrgId, testRoleOwner)
    assertTrue("Update to OWNER should succeed", result.isSuccess)

    // Update to STAFF
    result = membershipRepository.updateMembership(userId, testOrgId, testRoleStaff)
    assertTrue("Update to STAFF should succeed", result.isSuccess)

    // Verify final role
    val memberships = membershipRepository.getOrganizationsByUser(userId)
    assertEquals("Role should be STAFF", testRoleStaff, memberships.first().role)
  }

  @Test
  fun canGetUsersByOrganization() = runTest {
    // Create memberships for different users in same org
    membershipRepository.addMembership(testUserId1, testOrgId, testRoleOwner)
    membershipRepository.addMembership(testUserId2, testOrgId, testRoleMember)
    membershipRepository.addMembership(testUserId3, testOrgId, testRoleStaff)

    val memberships = membershipRepository.getUsersByOrganization(testOrgId)

    assertEquals("Should have 3 memberships", 3, memberships.size)
    assertTrue("All should belong to same org", memberships.all { it.orgId == testOrgId })
    assertEquals(
        "Should include user1", testUserId1, memberships.find { it.userId == testUserId1 }?.userId)
    assertEquals(
        "Should include user2", testUserId2, memberships.find { it.userId == testUserId2 }?.userId)
    assertEquals(
        "Should include user3", testUserId3, memberships.find { it.userId == testUserId3 }?.userId)
  }

  @Test
  fun getUsersByOrganizationReturnsEmptyListWhenNoMembers() = runTest {
    val memberships = membershipRepository.getUsersByOrganization(testOrgId)

    assertTrue("Should return empty list", memberships.isEmpty())
  }

  @Test
  fun getUsersByOrganizationFiltersByOrganization() = runTest {
    membershipRepository.addMembership(testUserId1, testOrgId, testRoleMember)
    membershipRepository.addMembership(testUserId2, testOrgId2, testRoleMember)

    val org1Members = membershipRepository.getUsersByOrganization(testOrgId)
    val org2Members = membershipRepository.getUsersByOrganization(testOrgId2)

    assertEquals("Org1 should have 1 member", 1, org1Members.size)
    assertEquals("Org2 should have 1 member", 1, org2Members.size)
    assertTrue("Org1 members should belong to org1", org1Members.all { it.orgId == testOrgId })
    assertTrue("Org2 members should belong to org2", org2Members.all { it.orgId == testOrgId2 })
  }

  @Test
  fun canGetOrganizationsByUser() = runTest {
    // Create memberships for same user in different orgs
    membershipRepository.addMembership(userId, testOrgId, testRoleOwner)
    membershipRepository.addMembership(userId, testOrgId2, testRoleMember)
    membershipRepository.addMembership(userId, testOrgId3, testRoleStaff)

    val memberships = membershipRepository.getOrganizationsByUser(userId)

    assertEquals("Should have 3 memberships", 3, memberships.size)
    assertTrue("All should belong to same user", memberships.all { it.userId == userId })
    assertTrue("Should include org1", memberships.any { it.orgId == testOrgId })
    assertTrue("Should include org2", memberships.any { it.orgId == testOrgId2 })
    assertTrue("Should include org3", memberships.any { it.orgId == testOrgId3 })
  }

  @Test
  fun getOrganizationsByUserReturnsEmptyListWhenNoMemberships() = runTest {
    val memberships = membershipRepository.getOrganizationsByUser(userId)

    assertTrue("Should return empty list", memberships.isEmpty())
  }

  @Test
  fun getOrganizationsByUserFiltersByUser() = runTest {
    membershipRepository.addMembership(testUserId1, testOrgId, testRoleMember)
    membershipRepository.addMembership(testUserId2, testOrgId, testRoleMember)

    val user1Orgs = membershipRepository.getOrganizationsByUser(testUserId1)
    val user2Orgs = membershipRepository.getOrganizationsByUser(testUserId2)

    assertEquals("User1 should have 1 org", 1, user1Orgs.size)
    assertEquals("User2 should have 1 org", 1, user2Orgs.size)
    assertTrue("User1 orgs should belong to user1", user1Orgs.all { it.userId == testUserId1 })
    assertTrue("User2 orgs should belong to user2", user2Orgs.all { it.userId == testUserId2 })
  }

  @Test
  fun hasMembershipReturnsTrueWhenExists() = runTest {
    membershipRepository.addMembership(userId, testOrgId, testRoleMember)

    val exists = membershipRepository.hasMembership(userId, testOrgId)

    assertTrue("Membership should exist", exists)
  }

  @Test
  fun hasMembershipReturnsFalseWhenNotExists() = runTest {
    val exists = membershipRepository.hasMembership(userId, testOrgId)

    assertFalse("Membership should not exist", exists)
  }

  @Test
  fun hasMembershipWithSpecificRoleReturnsTrueWhenRoleMatches() = runTest {
    membershipRepository.addMembership(userId, testOrgId, testRoleOwner)

    val exists = membershipRepository.hasMembership(userId, testOrgId, listOf(testRoleOwner))

    assertTrue("Membership with OWNER role should exist", exists)
  }

  @Test
  fun hasMembershipWithSpecificRoleReturnsFalseWhenRoleDoesNotMatch() = runTest {
    membershipRepository.addMembership(userId, testOrgId, testRoleMember)

    val exists = membershipRepository.hasMembership(userId, testOrgId, listOf(testRoleOwner))

    assertFalse("Membership with OWNER role should not exist", exists)
  }

  @Test
  fun hasMembershipWithMultipleRolesReturnsTrueWhenAnyRoleMatches() = runTest {
    membershipRepository.addMembership(userId, testOrgId, testRoleMember)

    val exists =
        membershipRepository.hasMembership(userId, testOrgId, listOf(testRoleOwner, testRoleMember))

    assertTrue("Membership with MEMBER role should exist", exists)
  }

  @Test
  fun hasMembershipUsesDefaultRolesWhenNotSpecified() = runTest {
    membershipRepository.addMembership(userId, testOrgId, testRoleStaff)

    // Call without roles parameter (should use default - all roles)
    val exists = membershipRepository.hasMembership(userId, testOrgId)

    assertTrue("Membership should exist with default roles check", exists)
  }

  @Test
  fun canHandleMultipleMembershipsForSameUser() = runTest {
    membershipRepository.addMembership(userId, testOrgId, testRoleOwner)
    membershipRepository.addMembership(userId, testOrgId2, testRoleMember)
    membershipRepository.addMembership(userId, testOrgId3, testRoleStaff)

    val memberships = membershipRepository.getOrganizationsByUser(userId)

    assertEquals("Should have 3 memberships", 3, memberships.size)
    assertTrue("All should belong to same user", memberships.all { it.userId == userId })
  }

  @Test
  fun canHandleMultipleUsersInSameOrganization() = runTest {
    membershipRepository.addMembership(testUserId1, testOrgId, testRoleOwner)
    membershipRepository.addMembership(testUserId2, testOrgId, testRoleMember)
    membershipRepository.addMembership(testUserId3, testOrgId, testRoleStaff)

    val memberships = membershipRepository.getUsersByOrganization(testOrgId)

    assertEquals("Should have 3 memberships", 3, memberships.size)
    assertTrue("All should belong to same org", memberships.all { it.orgId == testOrgId })
  }

  @Test
  fun getUsersByOrganizationReturnsSortedByCreatedAtDescending() = runTest {
    // Add with delay to ensure different timestamps
    membershipRepository.addMembership(testUserId1, testOrgId, testRoleMember)
    kotlinx.coroutines.delay(100)
    membershipRepository.addMembership(testUserId2, testOrgId, testRoleMember)
    kotlinx.coroutines.delay(100)
    membershipRepository.addMembership(testUserId3, testOrgId, testRoleMember)

    val memberships = membershipRepository.getUsersByOrganization(testOrgId)

    assertEquals("Should have 3 memberships", 3, memberships.size)
    // Most recent should be first (user3)
    assertEquals("First should be most recent", testUserId3, memberships[0].userId)
    assertEquals("Last should be oldest", testUserId1, memberships[2].userId)
  }

  @Test
  fun getOrganizationsByUserReturnsSortedByCreatedAtDescending() = runTest {
    membershipRepository.addMembership(userId, testOrgId, testRoleMember)
    kotlinx.coroutines.delay(100)
    membershipRepository.addMembership(userId, testOrgId2, testRoleMember)
    kotlinx.coroutines.delay(100)
    membershipRepository.addMembership(userId, testOrgId3, testRoleMember)

    val memberships = membershipRepository.getOrganizationsByUser(userId)

    assertEquals("Should have 3 memberships", 3, memberships.size)
    // Most recent should be first (org3)
    assertEquals("First should be most recent", testOrgId3, memberships[0].orgId)
    assertEquals("Last should be oldest", testOrgId, memberships[2].orgId)
  }

  @Test
  fun hasMembershipWithAllRolesReturnsTrueForAnyRole() = runTest {
    membershipRepository.addMembership(userId, testOrgId, testRoleStaff)

    // Check with all roles (default)
    val exists = membershipRepository.hasMembership(userId, testOrgId)

    assertTrue("Should find membership with any role", exists)
  }

  @Test
  fun addMembershipCreatesMembershipWithCorrectFields() = runTest {
    val result = membershipRepository.addMembership(userId, testOrgId, testRoleOwner)
    assertTrue("Should succeed", result.isSuccess)

    val memberships = membershipRepository.getOrganizationsByUser(userId)
    val membership = memberships.find { it.orgId == testOrgId }

    assertNotNull("Membership should exist", membership)
    assertEquals("User ID should match", userId, membership?.userId)
    assertEquals("Org ID should match", testOrgId, membership?.orgId)
    assertEquals("Role should match", testRoleOwner, membership?.role)
    assertNotNull("Membership ID should be set", membership?.membershipId)
  }

  @Test
  fun updateMembershipPreservesOtherFields() = runTest {
    // Create membership
    val createResult = membershipRepository.addMembership(userId, testOrgId, testRoleMember)
    assertTrue("Create should succeed", createResult.isSuccess)

    // Get original membership
    val originalMemberships = membershipRepository.getOrganizationsByUser(userId)
    val originalMembership = originalMemberships.find { it.orgId == testOrgId }!!

    // Update role
    kotlinx.coroutines.delay(100) // Ensure timestamp difference
    val updateResult = membershipRepository.updateMembership(userId, testOrgId, testRoleOwner)
    assertTrue("Update should succeed", updateResult.isSuccess)

    // Verify updated membership
    val updatedMemberships = membershipRepository.getOrganizationsByUser(userId)
    val updatedMembership = updatedMemberships.find { it.orgId == testOrgId }!!

    assertEquals("User ID should be preserved", originalMembership.userId, updatedMembership.userId)
    assertEquals("Org ID should be preserved", originalMembership.orgId, updatedMembership.orgId)
    assertEquals(
        "Membership ID should be preserved",
        originalMembership.membershipId,
        updatedMembership.membershipId)
    assertEquals("Role should be updated", testRoleOwner, updatedMembership.role)
    assertNotEquals(
        "UpdatedAt should be different", originalMembership.updatedAt, updatedMembership.updatedAt)
  }

  @Test
  fun getUsersByOrganizationHandlesLargeLists() = runTest {
    val userCount = 10

    // Create multiple memberships
    repeat(userCount) { index ->
      membershipRepository.addMembership("user_$index", testOrgId, testRoleMember)
    }

    val memberships = membershipRepository.getUsersByOrganization(testOrgId)

    assertEquals("Should have all memberships", userCount, memberships.size)
    assertTrue("All should belong to same org", memberships.all { it.orgId == testOrgId })
  }

  @Test
  fun getOrganizationsByUserHandlesLargeLists() = runTest {
    val orgCount = 10

    // Create multiple memberships
    repeat(orgCount) { index ->
      membershipRepository.addMembership(userId, "org_$index", testRoleMember)
    }

    val memberships = membershipRepository.getOrganizationsByUser(userId)

    assertEquals("Should have all memberships", orgCount, memberships.size)
    assertTrue("All should belong to same user", memberships.all { it.userId == userId })
  }

  @Test
  fun hasMembershipWithEmptyRoleListStillWorks() = runTest {
    membershipRepository.addMembership(userId, testOrgId, testRoleMember)

    // Empty list should still work (though not practical)
    val exists = membershipRepository.hasMembership(userId, testOrgId, emptyList())

    // Empty list means no roles match, so should return false
    assertFalse("Empty role list should not match", exists)
  }

  @Test
  fun canRemoveAndReaddMembership() = runTest {
    // Create
    val createResult1 = membershipRepository.addMembership(userId, testOrgId, testRoleMember)
    assertTrue("First create should succeed", createResult1.isSuccess)

    // Remove
    val removeResult = membershipRepository.removeMembership(userId, testOrgId)
    assertTrue("Remove should succeed", removeResult.isSuccess)

    // Re-add
    val createResult2 = membershipRepository.addMembership(userId, testOrgId, testRoleMember)
    assertTrue("Second create should succeed", createResult2.isSuccess)

    val exists = membershipRepository.hasMembership(userId, testOrgId)
    assertTrue("Membership should exist after re-add", exists)
  }

  @Test
  fun membershipOperationsAreIsolatedByUserAndOrg() = runTest {
    // Create memberships
    membershipRepository.addMembership(testUserId1, testOrgId, testRoleOwner)
    membershipRepository.addMembership(testUserId1, testOrgId2, testRoleMember)
    membershipRepository.addMembership(testUserId2, testOrgId, testRoleMember)
    membershipRepository.addMembership(testUserId2, testOrgId2, testRoleStaff)

    // Verify isolation
    val user1Orgs = membershipRepository.getOrganizationsByUser(testUserId1)
    val user2Orgs = membershipRepository.getOrganizationsByUser(testUserId2)
    val org1Users = membershipRepository.getUsersByOrganization(testOrgId)
    val org2Users = membershipRepository.getUsersByOrganization(testOrgId2)

    assertEquals("User1 should have 2 orgs", 2, user1Orgs.size)
    assertEquals("User2 should have 2 orgs", 2, user2Orgs.size)
    assertEquals("Org1 should have 2 users", 2, org1Users.size)
    assertEquals("Org2 should have 2 users", 2, org2Users.size)

    // Verify hasMembership works correctly
    assertTrue(
        "User1 in org1 should exist", membershipRepository.hasMembership(testUserId1, testOrgId))
    assertTrue(
        "User1 in org2 should exist", membershipRepository.hasMembership(testUserId1, testOrgId2))
    assertTrue(
        "User2 in org1 should exist", membershipRepository.hasMembership(testUserId2, testOrgId))
    assertTrue(
        "User2 in org2 should exist", membershipRepository.hasMembership(testUserId2, testOrgId2))
    assertFalse(
        "User1 in non-existent org should not exist",
        membershipRepository.hasMembership(testUserId1, "org_999"))
  }

  @Test
  fun getUsersByOrganizationFlowEmitsUpdates() = runTest {
    // Start collecting flow
    val flow = membershipRepository.getUsersByOrganizationFlow(testOrgId)
    val initialMemberships = flow.first()
    assertTrue("Initial flow should be empty", initialMemberships.isEmpty())

    // Add a member
    membershipRepository.addMembership(testUserId1, testOrgId, testRoleMember)

    // Verify flow emits update
    val updatedMemberships = flow.first()
    assertEquals("Should have 1 membership", 1, updatedMemberships.size)
    assertEquals("Should be user1", testUserId1, updatedMemberships[0].userId)
  }

  @Test
  fun getOrganizationsByUserFlowEmitsUpdates() = runTest {
    // Start collecting flow
    val flow = membershipRepository.getOrganizationsByUserFlow(userId)
    val initialMemberships = flow.first()
    assertTrue("Initial flow should be empty", initialMemberships.isEmpty())

    // Add a membership
    membershipRepository.addMembership(userId, testOrgId, testRoleMember)

    // Verify flow emits update
    val updatedMemberships = flow.first()
    assertEquals("Should have 1 membership", 1, updatedMemberships.size)
    assertEquals("Should be org1", testOrgId, updatedMemberships[0].orgId)
  }
}
