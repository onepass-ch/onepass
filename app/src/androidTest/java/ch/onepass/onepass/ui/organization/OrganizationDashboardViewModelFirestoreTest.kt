package ch.onepass.onepass.ui.organization

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.organization.*
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
  private lateinit var auth: FirebaseAuth

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
      orgRepository = OrganizationRepositoryFirebase()
      eventRepository = EventRepositoryFirebase()
      auth = FirebaseEmulator.auth
    }
  }

  private fun createViewModel() =
      OrganizationDashboardViewModel(
          organizationRepository = orgRepository, eventRepository = eventRepository, auth = auth)

  private fun createOrg(
      id: String,
      name: String = "Test Org",
      members: Map<String, OrganizationMember> =
          mapOf(
              userId to
                  OrganizationMember(role = OrganizationRole.OWNER, assignedEvents = emptyList())),
      followerCount: Int = 0,
      averageRating: Float = 0f
  ) =
      OrganizationTestData.createTestOrganization(
          id = id,
          name = name,
          ownerId = userId,
          status = OrganizationStatus.ACTIVE,
          members = members,
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
            members =
                mapOf(
                    userId to
                        OrganizationMember(
                            role = OrganizationRole.OWNER, assignedEvents = emptyList()),
                    "member-2" to
                        OrganizationMember(
                            role = OrganizationRole.MEMBER, assignedEvents = emptyList()),
                    "staff-2" to
                        OrganizationMember(
                            role = OrganizationRole.STAFF, assignedEvents = emptyList())),
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val viewModel = createViewModel()
    val state =
        loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.staffMembers.isNotEmpty() }
            .uiState
            .value

    assertEquals(3, state.staffMembers.size)
    assertEquals(OrganizationRole.OWNER, state.staffMembers[userId]?.role)
    assertEquals(OrganizationRole.MEMBER, state.staffMembers["member-2"]?.role)
    assertEquals(OrganizationRole.STAFF, state.staffMembers["staff-2"]?.role)
  }

  @Test
  fun viewModel_removesStaffMemberInFirestore() = runTest {
    val org =
        createOrg(
            "vm-org-4",
            "Remove Test Org",
            members =
                mapOf(
                    userId to
                        OrganizationMember(
                            role = OrganizationRole.OWNER, assignedEvents = emptyList()),
                    "removable-member" to
                        OrganizationMember(
                            role = OrganizationRole.MEMBER, assignedEvents = emptyList())),
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val viewModel = createViewModel()
    loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }

    viewModel.removeStaffMember("removable-member")
    val state = viewModel.uiState.first { !it.staffMembers.containsKey("removable-member") }

    assertFalse(state.staffMembers.containsKey("removable-member"))
    assertEquals(1, state.staffMembers.size)
    assertTrue(state.staffMembers.containsKey(userId))
  }

  @Test
  fun viewModel_doesNotRemoveOwnerFromFirestore() = runTest {
    val org =
        createOrg(
            "vm-org-5",
            "Owner Test Org",
            members =
                mapOf(
                    userId to
                        OrganizationMember(
                            role = OrganizationRole.OWNER, assignedEvents = emptyList()),
                    "safe-member" to
                        OrganizationMember(
                            role = OrganizationRole.MEMBER, assignedEvents = emptyList())),
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val viewModel = createViewModel()
    loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }

    val memberCountBefore = viewModel.uiState.value.staffMembers.size

    viewModel.removeStaffMember(userId)
    val afterRemove = viewModel.uiState.value

    assertEquals(memberCountBefore, afterRemove.staffMembers.size)
    assertTrue(afterRemove.staffMembers.containsKey(userId))
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
            members = emptyMap(),
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()

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
            members =
                mapOf(
                    userId to
                        OrganizationMember(
                            role = OrganizationRole.OWNER, assignedEvents = emptyList()),
                    "member-x" to
                        OrganizationMember(
                            role = OrganizationRole.MEMBER, assignedEvents = emptyList()),
                    "staff-x" to
                        OrganizationMember(
                            role = OrganizationRole.STAFF, assignedEvents = emptyList())),
        )
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val viewModel = createViewModel()
    loadOrgAndWait(viewModel, orgId) { !it.isLoading && it.organization != null }

    viewModel.removeStaffMember("member-x")
    val afterMemberRemove = viewModel.uiState.first { !it.staffMembers.containsKey("member-x") }
    assertFalse(afterMemberRemove.staffMembers.containsKey("member-x"))

    viewModel.removeStaffMember("staff-x")
    val afterStaffRemove = viewModel.uiState.first { !it.staffMembers.containsKey("staff-x") }
    assertFalse(afterStaffRemove.staffMembers.containsKey("staff-x"))

    assertTrue(afterStaffRemove.staffMembers.containsKey(userId))
    assertEquals(1, afterStaffRemove.staffMembers.size)
  }
}
