package ch.onepass.onepass.ui.organization

import ch.onepass.onepass.model.membership.Membership
import ch.onepass.onepass.model.organization.OrganizationRole
import io.mockk.every
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationDashboardViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockAuth: com.google.firebase.auth.FirebaseAuth
  private lateinit var mockUser: com.google.firebase.auth.FirebaseUser

  // Test data
  private val testOrg = OrganizationDashboardTestData.createTestOrganization(members = emptyMap())
  private val testEvent1 = OrganizationDashboardTestData.createTestEvent()
  private val testEvent2 =
      OrganizationDashboardTestData.createTestEvent(
          eventId = "event-2",
          title = "Test Event 2",
          status = ch.onepass.onepass.model.event.EventStatus.DRAFT,
          capacity = 200,
          ticketsRemaining = 200)

  private val testMemberships =
      listOf(
          OrganizationDashboardTestData.createTestMembership(
              membershipId = "mem-1", userId = "owner-1", role = OrganizationRole.OWNER),
          OrganizationDashboardTestData.createTestMembership(
              membershipId = "mem-2", userId = "member-1", role = OrganizationRole.MEMBER),
          OrganizationDashboardTestData.createTestMembership(
              membershipId = "mem-3", userId = "staff-1", role = OrganizationRole.STAFF))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    val (auth, user) = OrganizationDashboardTestData.createMockAuth()
    mockAuth = auth
    mockUser = user
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(
      organization: ch.onepass.onepass.model.organization.Organization? = null,
      events: List<ch.onepass.onepass.model.event.Event> = emptyList(),
      memberships: List<Membership> = testMemberships,
      shouldThrowError: Boolean = false,
      removeResult: Result<Unit> = Result.success(Unit)
  ) =
      OrganizationDashboardViewModel(
          organizationRepository =
              MockOrganizationRepository(organization, shouldThrowError, removeResult),
          eventRepository = MockEventRepository(events),
          membershipRepository = MockMembershipRepository(memberships, removeResult),
          userRepository = MockUserRepository(),
          auth = mockAuth)

  private suspend fun loadAndAdvance(
      viewModel: OrganizationDashboardViewModel,
      orgId: String = "test-org-1"
  ) {
    viewModel.loadOrganization(orgId)
    testDispatcher.scheduler.advanceUntilIdle()
  }

  @Test
  fun viewModel_initialState_isEmpty() = runTest {
    val viewModel = createViewModel()
    val state = viewModel.uiState.value

    assertNull(state.organization)
    assertTrue(state.events.isEmpty())
    assertTrue(state.staffMembers.isEmpty())
    assertNull(state.currentUserRole)
    assertFalse(state.isLoading)
    assertNull(state.error)
  }

  @Test
  fun viewModel_loadOrganization_updatesStateWithOrganizationAndMembers() = runTest {
    val viewModel = createViewModel(organization = testOrg, events = listOf(testEvent1, testEvent2))
    loadAndAdvance(viewModel)

    val state = viewModel.uiState.value
    assertEquals(testOrg, state.organization)
    assertEquals(2, state.events.size)
    assertEquals(3, state.staffMembers.size)
    assertEquals(
        ch.onepass.onepass.model.organization.OrganizationRole.OWNER, state.currentUserRole)
    assertFalse(state.isLoading)
    assertNull(state.error)
  }

  @Test
  fun viewModel_loadOrganization_setsLoadingState() = runTest {
    val viewModel = createViewModel(organization = testOrg)
    viewModel.loadOrganization("test-org-1")

    assertTrue(viewModel.uiState.value.isLoading)

    testDispatcher.scheduler.advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun viewModel_loadOrganization_handlesError() = runTest {
    val viewModel = createViewModel(shouldThrowError = true)
    loadAndAdvance(viewModel)

    val state = viewModel.uiState.value
    assertNull(state.organization)
    assertFalse(state.isLoading)
    assertNotNull(state.error)
    assertTrue(state.error!!.contains("Test error"))
  }

  @Test
  fun viewModel_loadOrganization_handlesNullOrganization() = runTest {
    val viewModel = createViewModel(organization = null)
    loadAndAdvance(viewModel)

    val state = viewModel.uiState.value
    assertNull(state.organization)
    assertFalse(state.isLoading)
    assertEquals("Organization not found", state.error)
  }

  @Test
  fun viewModel_loadOrganization_setsCurrentUserRoleForOwner() = runTest {
    every { mockUser.uid } returns "owner-1"
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    assertEquals(
        ch.onepass.onepass.model.organization.OrganizationRole.OWNER,
        viewModel.uiState.value.currentUserRole)
  }

  @Test
  fun viewModel_loadOrganization_setsCurrentUserRoleForMember() = runTest {
    every { mockUser.uid } returns "member-1"
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    assertEquals(
        ch.onepass.onepass.model.organization.OrganizationRole.MEMBER,
        viewModel.uiState.value.currentUserRole)
  }

  @Test
  fun viewModel_loadOrganization_setsCurrentUserRoleForStaff() = runTest {
    every { mockUser.uid } returns "staff-1"
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    assertEquals(
        ch.onepass.onepass.model.organization.OrganizationRole.STAFF,
        viewModel.uiState.value.currentUserRole)
  }

  @Test
  fun viewModel_loadOrganization_setsNullRoleForNonMember() = runTest {
    every { mockUser.uid } returns "non-member"
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    assertNull(viewModel.uiState.value.currentUserRole)
  }

  @Test
  fun viewModel_loadOrganization_handlesNullCurrentUser() = runTest {
    every { mockAuth.currentUser } returns null
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    assertNull(viewModel.uiState.value.currentUserRole)
  }

  @Test
  fun viewModel_loadOrganization_doesNotReloadIfSameOrganization() = runTest {
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    val firstState = viewModel.uiState.value
    assertNotNull(firstState.organization)

    loadAndAdvance(viewModel) // Load again
    assertEquals(firstState.organization, viewModel.uiState.value.organization)
  }

  @Test
  fun viewModel_removeStaffMember_successfullyRemovesMember() = runTest {
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    viewModel.removeStaffMember("member-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.staffMembers.any { it.userId == "member-1" })
    assertEquals(2, state.staffMembers.size)
  }

  @Test
  fun viewModel_removeStaffMember_doesNothingIfNotOwner() = runTest {
    every { mockUser.uid } returns "member-1"
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    val beforeCount = viewModel.uiState.value.staffMembers.size
    viewModel.removeStaffMember("staff-1")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(beforeCount, viewModel.uiState.value.staffMembers.size)
  }

  @Test
  fun viewModel_removeStaffMember_doesNothingIfRemovingOwner() = runTest {
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    val beforeCount = viewModel.uiState.value.staffMembers.size
    viewModel.removeStaffMember("owner-1")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(beforeCount, viewModel.uiState.value.staffMembers.size)
    assertTrue(viewModel.uiState.value.staffMembers.any { it.userId == "owner-1" })
  }

  @Test
  fun viewModel_removeStaffMember_handlesError() = runTest {
    val viewModel =
        createViewModel(
            organization = testOrg, removeResult = Result.failure(Exception("Remove failed")))
    loadAndAdvance(viewModel)

    viewModel.removeStaffMember("member-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.error)
    assertTrue(state.error!!.contains("Remove failed"))
  }

  @Test
  fun viewModel_removeStaffMember_doesNothingIfNoOrganization() = runTest {
    val viewModel = createViewModel(organization = null)
    viewModel.removeStaffMember("member-1")
    testDispatcher.scheduler.advanceUntilIdle()

    assertNull(viewModel.uiState.value.organization)
  }

  @Test
  fun viewModel_refresh_reloadsOrganization() = runTest {
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.organization)
  }

  @Test
  fun viewModel_refresh_doesNothingIfNoOrganizationLoaded() = runTest {
    val viewModel = createViewModel()
    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    assertNull(viewModel.uiState.value.organization)
  }

  @Test
  fun viewModel_clearError_removesErrorMessage() = runTest {
    val viewModel = createViewModel(shouldThrowError = true)
    loadAndAdvance(viewModel)

    assertNotNull(viewModel.uiState.value.error)
    viewModel.clearError()
    assertNull(viewModel.uiState.value.error)
  }

  @Test
  fun viewModel_loadsEventsForOrganization() = runTest {
    val viewModel = createViewModel(organization = testOrg, events = listOf(testEvent1, testEvent2))
    loadAndAdvance(viewModel)

    val state = viewModel.uiState.value
    assertEquals(2, state.events.size)
    assertTrue(state.events.any { it.eventId == "event-1" })
    assertTrue(state.events.any { it.eventId == "event-2" })
  }

  @Test
  fun viewModel_loadsEmptyEventsList() = runTest {
    val viewModel = createViewModel(organization = testOrg, events = emptyList())
    loadAndAdvance(viewModel)

    assertEquals(0, viewModel.uiState.value.events.size)
  }

  @Test
  fun viewModel_loadsStaffMembers() = runTest {
    val viewModel = createViewModel(organization = testOrg)
    loadAndAdvance(viewModel)

    val state = viewModel.uiState.value
    assertEquals(3, state.staffMembers.size)
    assertTrue(state.staffMembers.any { it.userId == "owner-1" })
    assertTrue(state.staffMembers.any { it.userId == "member-1" })
    assertTrue(state.staffMembers.any { it.userId == "staff-1" })
  }
}
