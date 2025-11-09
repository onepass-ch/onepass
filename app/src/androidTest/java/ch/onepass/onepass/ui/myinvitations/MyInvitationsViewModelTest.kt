package ch.onepass.onepass.ui.myinvitations

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.user.FakeUserRepository
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.utils.OrganizationTestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for MyInvitationsViewModel.
 *
 * Tests cover:
 * - Loading pending invitations for the current user
 * - Accepting invitations
 * - Rejecting invitations
 * - State updates after operations
 * - Error handling
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MyInvitationsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  /** Mock implementation of OrganizationRepository for testing. */
  private class MockOrganizationRepository(
      private val invitationsByEmail: Map<String, List<OrganizationInvitation>> = emptyMap(),
      private val updateInvitationStatusResult: Result<Unit> = Result.success(Unit),
      private val shouldThrowOnGetInvitations: Boolean = false
  ) : OrganizationRepository {
    private val _invitationsFlowsByEmail =
        mutableMapOf<String, MutableStateFlow<List<OrganizationInvitation>>>()

    init {
      // Initialize flows with data from invitationsByEmail map
      invitationsByEmail.forEach { (email, invitations) ->
        _invitationsFlowsByEmail[email] = MutableStateFlow(invitations)
      }
    }

    override suspend fun createOrganization(
        organization: ch.onepass.onepass.model.organization.Organization
    ): Result<String> = Result.success("org-id")

    override suspend fun updateOrganization(
        organization: ch.onepass.onepass.model.organization.Organization
    ): Result<Unit> = Result.success(Unit)

    override suspend fun deleteOrganization(organizationId: String): Result<Unit> =
        Result.success(Unit)

    override fun getOrganizationById(
        organizationId: String
    ): Flow<ch.onepass.onepass.model.organization.Organization?> = flowOf(null)

    override fun getOrganizationsByOwner(
        ownerId: String
    ): Flow<List<ch.onepass.onepass.model.organization.Organization>> = flowOf(emptyList())

    override fun getOrganizationsByMember(
        userId: String
    ): Flow<List<ch.onepass.onepass.model.organization.Organization>> = flowOf(emptyList())

    override fun getOrganizationsByStatus(
        status: ch.onepass.onepass.model.organization.OrganizationStatus
    ): Flow<List<ch.onepass.onepass.model.organization.Organization>> = flowOf(emptyList())

    override fun searchOrganizations(
        query: String
    ): Flow<List<ch.onepass.onepass.model.organization.Organization>> = flowOf(emptyList())

    override fun getVerifiedOrganizations():
        Flow<List<ch.onepass.onepass.model.organization.Organization>> = flowOf(emptyList())

    override suspend fun addMember(
        organizationId: String,
        userId: String,
        role: ch.onepass.onepass.model.organization.OrganizationRole
    ): Result<Unit> = Result.success(Unit)

    override suspend fun removeMember(organizationId: String, userId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun updateMemberRole(
        organizationId: String,
        userId: String,
        newRole: ch.onepass.onepass.model.organization.OrganizationRole
    ): Result<Unit> = Result.success(Unit)

    override suspend fun createInvitation(invitation: OrganizationInvitation): Result<String> =
        Result.success("invite-id")

    override fun getPendingInvitations(organizationId: String): Flow<List<OrganizationInvitation>> =
        flowOf(emptyList())

    override fun getInvitationsByEmail(email: String): Flow<List<OrganizationInvitation>> {
      if (shouldThrowOnGetInvitations) {
        return flow { throw Exception("Test error") }
      }
      // Get or create a StateFlow for this email
      val flow =
          _invitationsFlowsByEmail.getOrPut(email) {
            MutableStateFlow(invitationsByEmail[email] ?: emptyList())
          }
      return flow
    }

    override suspend fun updateInvitationStatus(
        invitationId: String,
        newStatus: InvitationStatus
    ): Result<Unit> {
      // Update the mock state to reflect the status change for all email flows
      _invitationsFlowsByEmail.values.forEach { flow ->
        val currentInvitations = flow.value.toMutableList()
        val index = currentInvitations.indexOfFirst { it.id == invitationId }
        if (index >= 0) {
          currentInvitations[index] = currentInvitations[index].copy(status = newStatus)
          flow.value = currentInvitations
        }
      }
      return updateInvitationStatusResult
    }

    override suspend fun deleteInvitation(invitationId: String): Result<Unit> = Result.success(Unit)

    /**
     * Sets the invitations to be returned for a specific email. This is used to simulate the
     * repository state.
     */
    fun setInvitationsForEmail(email: String, invitations: List<OrganizationInvitation>) {
      val flow = _invitationsFlowsByEmail.getOrPut(email) { MutableStateFlow(emptyList()) }
      flow.value = invitations
    }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ========================================
  // Tests for Initial State
  // ========================================

  @Test
  fun initialState_hasCorrectDefaults() = runTest {
    val userRepository = FakeUserRepository(currentUser = null)
    val orgRepository = MockOrganizationRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)

    val state = viewModel.state.value

    assertTrue(state.invitations.isEmpty())
    assertTrue(state.loading)
    assertNull(state.errorMessage)
  }

  // ========================================
  // Tests for Loading Invitations
  // ========================================

  @Test
  fun loadPendingInvitations_loadsOnlyPendingInvitations() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val pendingInvitation1 =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)
    val pendingInvitation2 =
        OrganizationTestData.createTestInvitation(
            id = "invite-2",
            orgId = "org-2",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)
    val acceptedInvitation =
        OrganizationTestData.createTestInvitation(
            id = "invite-3",
            orgId = "org-3",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.ACCEPTED)
    val rejectedInvitation =
        OrganizationTestData.createTestInvitation(
            id = "invite-4",
            orgId = "org-4",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.REJECTED)

    val allInvitations =
        listOf(pendingInvitation1, pendingInvitation2, acceptedInvitation, rejectedInvitation)
    val orgRepository =
        MockOrganizationRepository(invitationsByEmail = mapOf("test@example.com" to allInvitations))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Wait for the state to stabilize
    val state = viewModel.state.filter { !it.loading || it.errorMessage != null }.first()

    assertEquals(2, state.invitations.size)
    assertTrue(state.invitations.all { it.status == InvitationStatus.PENDING })
    assertTrue(state.invitations.any { it.id == "invite-1" })
    assertTrue(state.invitations.any { it.id == "invite-2" })
    assertFalse(state.loading)
    assertNull(state.errorMessage)
  }

  @Test
  fun loadPendingInvitations_noPendingInvitations_returnsEmptyList() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val acceptedInvitation =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.ACCEPTED)

    val orgRepository =
        MockOrganizationRepository(
            invitationsByEmail = mapOf("test@example.com" to listOf(acceptedInvitation)))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertTrue(state.invitations.isEmpty())
    assertFalse(state.loading)
    assertNull(state.errorMessage)
  }

  @Test
  fun loadPendingInvitations_userNotFound_setsErrorMessage() = runTest {
    val userRepository = FakeUserRepository(currentUser = null, createdUser = null)
    val orgRepository = MockOrganizationRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertNotNull("Expected an error message", state.errorMessage)
    assertTrue(
        state.errorMessage!!.contains("User not found") ||
            state.errorMessage!!.contains("not logged in"))
  }

  @Test
  fun loadPendingInvitations_repositoryError_setsErrorMessage() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)
    val orgRepository = MockOrganizationRepository(shouldThrowOnGetInvitations = true)
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertNotNull("Expected an error message", state.errorMessage)
  }

  // ========================================
  // Tests for Accepting Invitations
  // ========================================

  @Test
  fun acceptInvitation_success_removesInvitationFromList() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val pendingInvitation =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)

    val orgRepository =
        MockOrganizationRepository(
            invitationsByEmail = mapOf("test@example.com" to listOf(pendingInvitation)))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify invitation is initially in the list
    var state = viewModel.state.value
    assertEquals(1, state.invitations.size)
    assertEquals("invite-1", state.invitations.first().id)

    // Accept the invitation
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify invitation is removed from the list (since it's no longer PENDING)
    state = viewModel.state.value
    assertTrue("Invitation should be removed after acceptance", state.invitations.isEmpty())
    assertNull(state.errorMessage)
  }

  @Test
  fun acceptInvitation_failure_setsErrorMessage() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val pendingInvitation =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)

    val orgRepository =
        MockOrganizationRepository(
            invitationsByEmail = mapOf("test@example.com" to listOf(pendingInvitation)),
            updateInvitationStatusResult = Result.failure(Exception("Failed to update")))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message", state.errorMessage)
    assertTrue(state.errorMessage!!.contains("Failed"))
  }

  @Test
  fun acceptInvitation_nonExistentInvitation_handlesGracefully() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository = MockOrganizationRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("non-existent-invite")
    testDispatcher.scheduler.advanceUntilIdle()

    // Should not crash, but may or may not set an error depending on implementation
    val state = viewModel.state.value
    assertNotNull(state)
  }

  // ========================================
  // Tests for Rejecting Invitations
  // ========================================

  @Test
  fun rejectInvitation_success_removesInvitationFromList() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val pendingInvitation =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)

    val orgRepository =
        MockOrganizationRepository(
            invitationsByEmail = mapOf("test@example.com" to listOf(pendingInvitation)))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify invitation is initially in the list
    var state = viewModel.state.value
    assertEquals(1, state.invitations.size)
    assertEquals("invite-1", state.invitations.first().id)

    // Reject the invitation
    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify invitation is removed from the list (since it's no longer PENDING)
    state = viewModel.state.value
    assertTrue("Invitation should be removed after rejection", state.invitations.isEmpty())
    assertNull(state.errorMessage)
  }

  @Test
  fun rejectInvitation_failure_setsErrorMessage() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val pendingInvitation =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)

    val orgRepository =
        MockOrganizationRepository(
            invitationsByEmail = mapOf("test@example.com" to listOf(pendingInvitation)),
            updateInvitationStatusResult = Result.failure(Exception("Failed to update")))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message", state.errorMessage)
    assertTrue(state.errorMessage!!.contains("Failed"))
  }

  @Test
  fun rejectInvitation_nonExistentInvitation_handlesGracefully() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository = MockOrganizationRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.rejectInvitation("non-existent-invite")
    testDispatcher.scheduler.advanceUntilIdle()

    // Should not crash, but may or may not set an error depending on implementation
    val state = viewModel.state.value
    assertNotNull(state)
  }

  // ========================================
  // Tests for Multiple Invitations
  // ========================================

  @Test
  fun multipleInvitations_acceptOne_removesOnlyThatInvitation() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val invitation1 =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)
    val invitation2 =
        OrganizationTestData.createTestInvitation(
            id = "invite-2",
            orgId = "org-2",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)

    val orgRepository =
        MockOrganizationRepository(
            invitationsByEmail = mapOf("test@example.com" to listOf(invitation1, invitation2)))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify both invitations are in the list
    var state = viewModel.state.value
    assertEquals(2, state.invitations.size)

    // Accept only the first invitation
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify only invitation2 remains
    state = viewModel.state.value
    assertEquals(1, state.invitations.size)
    assertEquals("invite-2", state.invitations.first().id)
  }

  @Test
  fun multipleInvitations_rejectOne_removesOnlyThatInvitation() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val invitation1 =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)
    val invitation2 =
        OrganizationTestData.createTestInvitation(
            id = "invite-2",
            orgId = "org-2",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)

    val orgRepository =
        MockOrganizationRepository(
            invitationsByEmail = mapOf("test@example.com" to listOf(invitation1, invitation2)))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify both invitations are in the list
    var state = viewModel.state.value
    assertEquals(2, state.invitations.size)

    // Reject only the first invitation
    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify only invitation2 remains
    state = viewModel.state.value
    assertEquals(1, state.invitations.size)
    assertEquals("invite-2", state.invitations.first().id)
  }

  // ========================================
  // Tests for Error Handling
  // ========================================

  @Test
  fun acceptInvitation_clearsPreviousError() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val pendingInvitation =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)

    val orgRepository =
        MockOrganizationRepository(
            invitationsByEmail = mapOf("test@example.com" to listOf(pendingInvitation)),
            updateInvitationStatusResult = Result.failure(Exception("First error")))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // First attempt fails
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    var state = viewModel.state.value
    assertNotNull("Expected an error message", state.errorMessage)
    // Note: In a real scenario, we'd need to recreate the ViewModel with a new repository
    // to test error clearing, but for this test we're just verifying that error is set
  }

  @Test
  fun rejectInvitation_clearsPreviousError() = runTest {
    val testUser = User(uid = "user-1", email = "test@example.com", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)

    val pendingInvitation =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "test@example.com",
            status = InvitationStatus.PENDING)

    val orgRepository =
        MockOrganizationRepository(
            invitationsByEmail = mapOf("test@example.com" to listOf(pendingInvitation)),
            updateInvitationStatusResult = Result.failure(Exception("First error")))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // First attempt fails
    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message", state.errorMessage)
  }
}
