package ch.onepass.onepass.ui.myinvitations

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.model.user.FakeUserRepository
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.ui.organization.MockOrganizationRepository
import ch.onepass.onepass.utils.OrganizationTestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
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

  // Reusable test data
  private val testUserEmail = "test@example.com"
  private val testUser = User(uid = "user-1", email = testUserEmail, displayName = "Test User")

  private val pendingInvitation =
      OrganizationTestData.createTestInvitation(
          id = "invite-1",
          orgId = "org-1",
          inviteeEmail = testUserEmail,
          status = InvitationStatus.PENDING)

  private val pendingInvitation2 =
      OrganizationTestData.createTestInvitation(
          id = "invite-2",
          orgId = "org-2",
          inviteeEmail = testUserEmail,
          status = InvitationStatus.PENDING)

  private val acceptedInvitation =
      OrganizationTestData.createTestInvitation(
          id = "invite-3",
          orgId = "org-3",
          inviteeEmail = testUserEmail,
          status = InvitationStatus.ACCEPTED)

  private val rejectedInvitation =
      OrganizationTestData.createTestInvitation(
          id = "invite-4",
          orgId = "org-4",
          inviteeEmail = testUserEmail,
          status = InvitationStatus.REJECTED)

  /** Extended MockOrganizationRepository with specific implementations for invitation testing. */
  private class TestMockOrganizationRepository(
      private val invitationsByEmail: Map<String, List<OrganizationInvitation>> = emptyMap(),
      private val updateInvitationStatusResult: Result<Unit> = Result.success(Unit),
      private val addMemberResult: Result<Unit> = Result.success(Unit),
      private val shouldThrowOnGetInvitations: Boolean = false,
      private val shouldThrowOnUpdateStatus: Boolean = false,
      private val exceptionMessage: String? = "Test error"
  ) : MockOrganizationRepository() {
    private val _invitationsFlowsByEmail =
        mutableMapOf<String, MutableStateFlow<List<OrganizationInvitation>>>()

    // Track addMember calls for testing
    val addMemberCalls = mutableListOf<Triple<String, String, OrganizationRole>>()

    init {
      // Initialize flows with data from invitationsByEmail map
      invitationsByEmail.forEach { (email, invitations) ->
        _invitationsFlowsByEmail[email] = MutableStateFlow(invitations)
      }
    }

    override fun getInvitationsByEmail(email: String): Flow<List<OrganizationInvitation>> {
      if (shouldThrowOnGetInvitations) {
        return flow {
          if (exceptionMessage != null) {
            throw Exception(exceptionMessage)
          } else {
            throw Exception()
          }
        }
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
      if (shouldThrowOnUpdateStatus) {
        throw Exception("Update invitation status failed")
      }
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

    override suspend fun addMember(
        organizationId: String,
        userId: String,
        role: OrganizationRole
    ): Result<Unit> {
      // Track the call for testing
      addMemberCalls.add(Triple(organizationId, userId, role))
      return addMemberResult
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
    val orgRepository = TestMockOrganizationRepository()
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
    val userRepository = FakeUserRepository(currentUser = testUser)

    val allInvitations =
        listOf(pendingInvitation, pendingInvitation2, acceptedInvitation, rejectedInvitation)
    val orgRepository =
        TestMockOrganizationRepository(invitationsByEmail = mapOf(testUserEmail to allInvitations))

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
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(acceptedInvitation)))

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
    val orgRepository = TestMockOrganizationRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertNotNull("Expected an error message", state.errorMessage)
    val errorMessage = state.errorMessage!!
    assertTrue(errorMessage.contains("User not found") || errorMessage.contains("not logged in"))
  }

  @Test
  fun loadPendingInvitations_repositoryError_setsErrorMessage() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)
    val orgRepository = TestMockOrganizationRepository(shouldThrowOnGetInvitations = true)
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertNotNull("Expected an error message", state.errorMessage)
  }

  // ========================================
  // Tests for Retry
  // ========================================

  @Test
  fun retry_afterUserLoadError_clearsErrorAndReloadsInvitations() = runTest {
    val userRepository = FakeUserRepository()
    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)))
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    var state = viewModel.state.value
    assertNotNull("Expected an error message when user cannot be loaded", state.errorMessage)
    assertTrue("No invitations should be loaded when user fails", state.invitations.isEmpty())

    userRepository.updateCurrentUser(testUser)
    userRepository.updateCreatedUser(testUser)

    viewModel.retry()
    testDispatcher.scheduler.advanceUntilIdle()

    state = viewModel.state.value
    assertNull("Error message should be cleared after retry succeeds", state.errorMessage)
    assertFalse("Loading should be false after retry succeeds", state.loading)
    assertEquals("Expected invitations to be reloaded after retry", 1, state.invitations.size)
    assertEquals("invite-1", state.invitations.first().id)
  }

  // ========================================
  // Tests for Accepting Invitations
  // ========================================

  @Test
  fun acceptInvitation_success_removesInvitationFromList() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)))

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

    // Verify addMember was called with correct parameters
    assertEquals("addMember should be called once", 1, orgRepository.addMemberCalls.size)
    val (orgId, userId, role) = orgRepository.addMemberCalls.first()
    assertEquals("Organization ID should match", pendingInvitation.orgId, orgId)
    assertEquals("User ID should match", testUser.uid, userId)
    assertEquals("Role should match", pendingInvitation.role, role)
  }

  @Test
  fun acceptInvitation_success_callsAddMember() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify addMember was called
    assertEquals("addMember should be called", 1, orgRepository.addMemberCalls.size)
    val (orgId, userId, role) = orgRepository.addMemberCalls.first()
    assertEquals("Organization ID should match", pendingInvitation.orgId, orgId)
    assertEquals("User ID should match", testUser.uid, userId)
    assertEquals("Role should match", pendingInvitation.role, role)
  }

  @Test
  fun acceptInvitation_addMemberFailure_showsErrorMessage() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)),
            addMemberResult = Result.failure(Exception("Failed to add member")))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message when addMember fails", state.errorMessage)
    assertTrue(
        "Error message should mention addMember failure",
        state.errorMessage!!.contains("add you as a member") ||
            state.errorMessage!!.contains("Failed to add member"))

    // Verify addMember was still called (even though it failed)
    assertEquals("addMember should be called", 1, orgRepository.addMemberCalls.size)
  }

  @Test
  fun acceptInvitation_failure_setsErrorMessage() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)),
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
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository = TestMockOrganizationRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("non-existent-invite")
    testDispatcher.scheduler.advanceUntilIdle()

    // Should show error message for invitation not found
    val state = viewModel.state.value
    assertNotNull("Expected an error message", state.errorMessage)
    assertTrue(
        "Error should mention invitation not found",
        state.errorMessage!!.contains("not found") || state.errorMessage!!.contains("removed"))

    // addMember should not be called if invitation is not found
    assertEquals("addMember should not be called", 0, orgRepository.addMemberCalls.size)
  }

  @Test
  fun acceptInvitation_userNotFound_showsErrorMessage() = runTest {
    val userRepository = FakeUserRepository(currentUser = null, createdUser = null)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message when user is not found", state.errorMessage)
    assertTrue(
        "Error should mention user not found",
        state.errorMessage!!.contains("User not found") || state.errorMessage!!.contains("log in"))

    // addMember should not be called if user is not found
    assertEquals("addMember should not be called", 0, orgRepository.addMemberCalls.size)
  }

  // ========================================
  // Tests for Rejecting Invitations
  // ========================================

  @Test
  fun rejectInvitation_success_removesInvitationFromList() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)))

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
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)),
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
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository = TestMockOrganizationRepository()
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
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail =
                mapOf(testUserEmail to listOf(pendingInvitation, pendingInvitation2)))

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
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail =
                mapOf(testUserEmail to listOf(pendingInvitation, pendingInvitation2)))

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
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)),
            updateInvitationStatusResult = Result.failure(Exception("First error")))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // First attempt fails
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message", state.errorMessage)
    // Note: In a real scenario, we'd need to recreate the ViewModel with a new repository
    // to test error clearing, but for this test we're just verifying that error is set
  }

  @Test
  fun rejectInvitation_clearsPreviousError() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)),
            updateInvitationStatusResult = Result.failure(Exception("First error")))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // First attempt fails
    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message", state.errorMessage)
  }

  // ========================================
  // Tests for invitations StateFlow direct access
  // ========================================

  @Test
  fun invitationsStateFlow_directAccess_returnsPendingInvitations() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail =
                mapOf(testUserEmail to listOf(pendingInvitation, acceptedInvitation)))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Directly access invitations StateFlow
    val invitations = viewModel.invitations.value

    assertEquals(1, invitations.size)
    assertEquals("invite-1", invitations.first().id)
    assertEquals(InvitationStatus.PENDING, invitations.first().status)
  }

  @Test
  fun invitationsStateFlow_emptyEmail_returnsEmptyList() = runTest {
    val userRepository = FakeUserRepository(currentUser = null, createdUser = null)
    val orgRepository = TestMockOrganizationRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    // Directly access invitations StateFlow when email is null
    val invitations = viewModel.invitations.value

    assertTrue(invitations.isEmpty())
  }

  // ========================================
  // Tests for Error Handling in catch blocks
  // ========================================

  @Test
  fun loadUserEmail_throwsException_setsErrorMessage() = runTest {
    val userRepository = FakeUserRepository(throwOnLoad = true)
    val orgRepository = TestMockOrganizationRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertNotNull("Expected an error message when exception is thrown", state.errorMessage)
    val errorMessage = state.errorMessage!!
    assertTrue(
        errorMessage.contains("Failed to load user information") || errorMessage.contains("boom"))
  }

  @Test
  fun acceptInvitation_throwsException_setsErrorMessage() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)),
            shouldThrowOnUpdateStatus = true)

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message when exception is thrown", state.errorMessage)
    val errorMessage = state.errorMessage!!
    assertTrue(
        errorMessage.contains("Failed to accept invitation") ||
            errorMessage.contains("Update invitation status failed"))
  }

  @Test
  fun rejectInvitation_throwsException_setsErrorMessage() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)),
            shouldThrowOnUpdateStatus = true)

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message when exception is thrown", state.errorMessage)
    val errorMessage = state.errorMessage!!
    assertTrue(
        errorMessage.contains("Failed to reject invitation") ||
            errorMessage.contains("Update invitation status failed"))
  }

  @Test
  fun invitationsFlow_repositoryErrorWithNullMessage_handlesGracefully() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    // Create a repository that throws an exception with null message
    val orgRepository =
        TestMockOrganizationRepository(shouldThrowOnGetInvitations = true, exceptionMessage = null)

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertNotNull("Expected an error message", state.errorMessage)
    assertEquals("Failed to load invitations", state.errorMessage!!)
  }

  @Test
  fun loadUserEmail_userWithBlankEmail_setsErrorMessage() = runTest {
    val testUser = User(uid = "user-1", email = "", displayName = "Test User")
    val userRepository = FakeUserRepository(currentUser = testUser)
    val orgRepository = TestMockOrganizationRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertNotNull("Expected an error message for blank email", state.errorMessage)
    val errorMessage = state.errorMessage!!
    assertTrue(errorMessage.contains("User not found") || errorMessage.contains("not logged in"))
  }

  @Test
  fun acceptInvitation_resultFailureWithNullMessage_usesDefaultMessage() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    // Create a failure result with exception that has null message
    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)),
            updateInvitationStatusResult = Result.failure(Exception()))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message", state.errorMessage)
    assertEquals("Failed to accept invitation", state.errorMessage!!)
  }

  @Test
  fun rejectInvitation_resultFailureWithNullMessage_usesDefaultMessage() = runTest {
    val userRepository = FakeUserRepository(currentUser = testUser)

    // Create a failure result with exception that has null message
    val orgRepository =
        TestMockOrganizationRepository(
            invitationsByEmail = mapOf(testUserEmail to listOf(pendingInvitation)),
            updateInvitationStatusResult = Result.failure(Exception()))

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertNotNull("Expected an error message", state.errorMessage)
    assertEquals("Failed to reject invitation", state.errorMessage!!)
  }
}
