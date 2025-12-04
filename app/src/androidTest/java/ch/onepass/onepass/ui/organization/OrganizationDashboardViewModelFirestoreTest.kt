package ch.onepass.onepass.ui.organization

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.membership.MembershipRepositoryFirebase
import ch.onepass.onepass.model.organization.*
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import ch.onepass.onepass.utils.EventTestData
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import ch.onepass.onepass.utils.OrganizationTestData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizationDashboardViewModelFirestoreTest : FirestoreTestBase() {

  private lateinit var userId: String
  private lateinit var orgRepository: OrganizationRepositoryFirebase
  private lateinit var eventRepository: EventRepositoryFirebase
  private lateinit var membershipRepository: MembershipRepositoryFirebase
  private lateinit var userRepository: UserRepositoryFirebase
  private lateinit var auth: FirebaseAuth

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
      orgRepository = OrganizationRepositoryFirebase()
      eventRepository = EventRepositoryFirebase()
      membershipRepository = MembershipRepositoryFirebase()
      userRepository = UserRepositoryFirebase()
      auth = FirebaseEmulator.auth
    }
  }

  private fun createViewModel() =
      OrganizationDashboardViewModel(
          organizationRepository = orgRepository,
          eventRepository = eventRepository,
          membershipRepository = membershipRepository,
          userRepository = userRepository,
          auth = auth)

  private fun createOrg(
      id: String,
      name: String = "Test Org",
      followerCount: Int = 0,
      averageRating: Float = 0f
  ) =
      OrganizationTestData.createTestOrganization(
          id = id,
          name = name,
          ownerId = userId,
          status = OrganizationStatus.ACTIVE,
          members = emptyMap(), // Ensure decoupled from legacy members field
          followerCount = followerCount,
          averageRating = averageRating)

  private suspend fun loadOrgAndWait(
      viewModel: OrganizationDashboardViewModel,
      orgId: String,
      predicate: (OrganizationDashboardUiState) -> Boolean
  ) =
      viewModel.apply {
        loadOrganization(orgId)
        uiState.first(predicate)
      }

  @Test
  fun viewModel_loadsOrganizationFromFirestore() = runTest {
    val org = createOrg("vm-org-1", "VM Test Org", followerCount = 2000, averageRating = 4.8f)
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val members = mapOf(userId to OrganizationRole.OWNER)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)

    val viewModel = createViewModel()
    val state =
        loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }.uiState.value

    assertNotNull(state.organization)
    assertEquals("VM Test Org", state.organization?.name)
    assertEquals(2000, state.organization?.followerCount)
    assertEquals(4.8f, state.organization?.averageRating)
    assertEquals(OrganizationRole.OWNER, state.currentUserRole)
  }

  @Test
  fun viewModel_loadsEventsForOrganizationFromFirestore() = runTest {
    val org =
        createOrg(
            "vm-org-2",
            "Event Test Org",
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val members = mapOf(userId to OrganizationRole.OWNER)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)

    val event1 = EventTestData.createPublishedEvent("vm-evt-1", organizerId = orgId, capacity = 150)
    val event2 = EventTestData.createPublishedEvent("vm-evt-2", organizerId = orgId, capacity = 250)

    val event1Id = eventRepository.createEvent(event1).getOrNull()
    val event2Id = eventRepository.createEvent(event2).getOrNull()

    val viewModel = createViewModel()
    val state =
        loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.events.isNotEmpty() }.uiState.value

    assertEquals(2, state.events.size)
    assertTrue(state.events.any { it.eventId == event1Id })
    assertTrue(state.events.any { it.eventId == event2Id })
  }

  @Test
  fun viewModel_loadsStaffMembersFromFirestore() = runTest {
    val org =
        createOrg(
            "vm-org-3",
            "Staff Test Org",
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val members =
        mapOf(
            userId to OrganizationRole.OWNER,
            "member-2" to OrganizationRole.MEMBER,
            "staff-2" to OrganizationRole.STAFF)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)
    members.keys.forEach { uid -> FirestoreTestHelper.createFirestoreUser(uid) }

    val viewModel = createViewModel()
    val state =
        loadOrgAndWait(viewModel, orgId) { state ->
              !state.isLoading &&
                  state.staffMembers.isNotEmpty() &&
                  state.staffMembers.all { it.userProfile != null }
            }
            .uiState
            .value

    assertEquals(3, state.staffMembers.size)

    val owner = state.staffMembers.find { it.userId == userId }
    assertEquals(OrganizationRole.OWNER, owner?.role)
    // Verify profile loaded (assuming createFirestoreUser sets a default name)
    assertNotNull(owner?.userProfile)

    val member = state.staffMembers.find { it.userId == "member-2" }
    assertEquals(OrganizationRole.MEMBER, member?.role)
    assertNotNull(member?.userProfile)

    val staff = state.staffMembers.find { it.userId == "staff-2" }
    assertEquals(OrganizationRole.STAFF, staff?.role)
    assertNotNull(staff?.userProfile)
  }

  @Test
  fun viewModel_removesStaffMemberInFirestore() = runTest {
    val org =
        createOrg(
            "vm-org-4",
            "Remove Test Org",
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val members =
        mapOf(userId to OrganizationRole.OWNER, "removable-member" to OrganizationRole.MEMBER)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)
    members.keys.forEach { uid -> FirestoreTestHelper.createFirestoreUser(uid) }

    val viewModel = createViewModel()
    loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }

    viewModel.removeStaffMember("removable-member")
    val state =
        viewModel.uiState.first { state ->
          state.staffMembers.none { it.userId == "removable-member" }
        }

    assertFalse(state.staffMembers.any { it.userId == "removable-member" })
    assertEquals(1, state.staffMembers.size)
    assertTrue(state.staffMembers.any { it.userId == userId })
  }

  @Test
  fun viewModel_doesNotRemoveOwnerFromFirestore() = runTest {
    val org =
        createOrg(
            "vm-org-5",
            "Owner Test Org",
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val members = mapOf(userId to OrganizationRole.OWNER, "safe-member" to OrganizationRole.MEMBER)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)
    members.keys.forEach { uid -> FirestoreTestHelper.createFirestoreUser(uid) }

    val viewModel = createViewModel()
    loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }

    val memberCountBefore = viewModel.uiState.value.staffMembers.size

    viewModel.removeStaffMember(userId)
    val afterRemove = viewModel.uiState.value

    assertEquals(memberCountBefore, afterRemove.staffMembers.size)
    assertTrue(afterRemove.staffMembers.any { it.userId == userId })
  }

  @Test
  fun viewModel_handlesNonExistentOrganizationInFirestore() = runTest {
    val viewModel = createViewModel()
    viewModel.loadOrganization("non-existent-org-id")

    val state = viewModel.uiState.first { !it.isLoading }

    assertNull(state.organization)
    assertNotNull(state.error)
    assertEquals("Organization not found", state.error)
  }

  @Test
  fun viewModel_refreshReloadsDataFromFirestore() = runTest {
    val org =
        createOrg(
            "vm-org-6",
            "Refresh Test Org",
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()
    val members = mapOf(userId to OrganizationRole.OWNER)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)

    val viewModel = createViewModel()
    loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }

    viewModel.refresh()
    val state = viewModel.uiState.first { !it.isLoading && it.organization != null }

    assertNotNull(state.organization)
    assertEquals("Refresh Test Org", state.organization?.name)
  }

  @Test
  fun viewModel_clearErrorRemovesErrorState() = runTest {
    val viewModel = createViewModel()
    viewModel.loadOrganization("non-existent")

    val stateWithError = viewModel.uiState.first { it.error != null }
    assertNotNull(stateWithError.error)

    viewModel.clearError()
    assertNull(viewModel.uiState.value.error)
  }

  @Test
  fun viewModel_setsCorrectRoleForOwner() = runTest {
    val org =
        createOrg(
            "vm-org-7",
            "Owner Role Org",
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()
    val members = mapOf(userId to OrganizationRole.OWNER)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)

    val viewModel = createViewModel()
    val state =
        loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }.uiState.value

    assertEquals(OrganizationRole.OWNER, state.currentUserRole)
  }

  @Test
  fun viewModel_handlesEmptyEventsListFromFirestore() = runTest {
    val org =
        createOrg(
            "vm-org-8",
            "No Events Org",
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()
    val members = mapOf(userId to OrganizationRole.OWNER)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)

    val viewModel = createViewModel()
    val state =
        loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }.uiState.value

    assertEquals(0, state.events.size)
  }

  @Test
  fun viewModel_handlesEmptyStaffListFromFirestore() = runTest {
    val org =
        createOrg(
            "vm-org-9",
            "No Staff Org",
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()
    // Explicitly create NO memberships (empty map)
    FirestoreTestHelper.populateMemberships(orgId, emptyMap(), membershipRepository)

    val viewModel = createViewModel()
    val state =
        loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }.uiState.value

    assertEquals(0, state.staffMembers.size)
  }

  @Test
  fun viewModel_onlyRemovesNonOwnerMembers() = runTest {
    val org =
        createOrg(
            "vm-org-10",
            "Protection Test Org",
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val members =
        mapOf(
            userId to OrganizationRole.OWNER,
            "member-x" to OrganizationRole.MEMBER,
            "staff-x" to OrganizationRole.STAFF)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)
    members.keys.forEach { uid -> FirestoreTestHelper.createFirestoreUser(uid) }

    val viewModel = createViewModel()
    loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }

    viewModel.removeStaffMember("member-x")
    val afterMemberRemove =
        viewModel.uiState.first { state -> state.staffMembers.none { it.userId == "member-x" } }
    assertFalse(afterMemberRemove.staffMembers.any { it.userId == "member-x" })

    viewModel.removeStaffMember("staff-x")
    val afterStaffRemove =
        viewModel.uiState.first { state -> state.staffMembers.none { it.userId == "staff-x" } }
    assertFalse(afterStaffRemove.staffMembers.any { it.userId == "staff-x" })

    assertTrue(afterStaffRemove.staffMembers.any { it.userId == userId })
    assertEquals(1, afterStaffRemove.staffMembers.size)
  }

  @Test
  fun viewModel_fetchUserProfile_handlesErrorGracefully() = runTest {
    val org = createOrg("vm-org-error-1", "Profile Error Org")
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val members = mapOf(userId to OrganizationRole.OWNER, "missing-user" to OrganizationRole.MEMBER)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)
    FirestoreTestHelper.createFirestoreUser(userId) // Create owner only

    val viewModel = createViewModel()

    // Wait for initial load
    loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.staffMembers.size == 2 }

    // Wait for "missing-user" to stop loading.
    // Since the user doc doesn't exist, fetchUserProfile will complete (likely with null profile).
    // The ViewModel sets isLoading = false even on failure/null.
    val state =
        viewModel.uiState.first { state ->
          val missingMember = state.staffMembers.find { it.userId == "missing-user" }
          missingMember != null && !missingMember.isLoading
        }

    val missingMember = state.staffMembers.find { it.userId == "missing-user" }
    assertNotNull(missingMember)
    assertNull(missingMember?.userProfile) // Profile should be null
    assertFalse(missingMember!!.isLoading) // Loading should be false
  }

  @Test
  fun viewModel_loadOrganization_deduplicatesRequests() = runTest {
    val org = createOrg("vm-org-dedup", "Dedup Org")
    val orgId = orgRepository.createOrganization(org).getOrThrow()
    val members = mapOf(userId to OrganizationRole.OWNER)
    FirestoreTestHelper.populateMemberships(orgId, members, membershipRepository)
    FirestoreTestHelper.createFirestoreUser(userId)

    val viewModel = createViewModel()

    // First load
    loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }

    // Capture state
    val state1 = viewModel.uiState.value

    // Second load with SAME ID
    viewModel.loadOrganization(orgId)

    // State should be IDENTICAL (same instance) because it returns early
    assertTrue(viewModel.uiState.value === state1)

    // Third load with DIFFERENT ID (non-existent)
    viewModel.loadOrganization("other-id")

    // Should trigger loading
    val stateLoading = viewModel.uiState.first { it.isLoading }
    assertTrue(stateLoading.isLoading)
  }
}
