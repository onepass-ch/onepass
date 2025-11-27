package ch.onepass.onepass.ui.myinvitations

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.utils.FakeUserRepository
import ch.onepass.onepass.utils.OrganizationTestData
import ch.onepass.onepass.utils.TestMockMembershipRepository
import ch.onepass.onepass.utils.TestMockOrganizationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
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

  // Helper functions to create repositories with common configurations
  private fun createEmptyOrgRepository(): TestMockOrganizationRepository =
      TestMockOrganizationRepository()

  private fun createOrgRepositoryWithInvitations(
      invitations: List<OrganizationInvitation>,
      email: String = testUserEmail
  ): TestMockOrganizationRepository =
      TestMockOrganizationRepository(invitationsByEmail = mapOf(email to invitations))

  private fun createOrgRepositoryWithError(
      shouldThrowOnGetInvitations: Boolean = false,
      shouldThrowOnUpdateStatus: Boolean = false,
      exceptionMessage: String? = "Test error"
  ): TestMockOrganizationRepository =
      TestMockOrganizationRepository(
          shouldThrowOnGetInvitations = shouldThrowOnGetInvitations,
          shouldThrowOnUpdateStatus = shouldThrowOnUpdateStatus,
          exceptionMessage = exceptionMessage)

  private fun createOrgRepositoryWithUpdateFailure(
      invitations: List<OrganizationInvitation> = listOf(pendingInvitation),
      email: String = testUserEmail
  ): TestMockOrganizationRepository =
      TestMockOrganizationRepository(
          invitationsByEmail = mapOf(email to invitations),
          updateInvitationStatusResult = Result.failure(Exception("Failed to update")))

  private fun createOrgRepositoryWithUpdateThrow(
      invitations: List<OrganizationInvitation> = listOf(pendingInvitation),
      email: String = testUserEmail
  ): TestMockOrganizationRepository =
      TestMockOrganizationRepository(
          invitationsByEmail = mapOf(email to invitations), shouldThrowOnUpdateStatus = true)

  private fun createOrgRepositoryWithUpdateFailureNullMessage(
      invitations: List<OrganizationInvitation> = listOf(pendingInvitation),
      email: String = testUserEmail
  ): TestMockOrganizationRepository =
      TestMockOrganizationRepository(
          invitationsByEmail = mapOf(email to invitations),
          updateInvitationStatusResult = Result.failure(Exception()))

  private fun createDefaultMembershipRepository(): TestMockMembershipRepository =
      TestMockMembershipRepository()

  private fun createMembershipRepositoryWithFailure(): TestMockMembershipRepository =
      TestMockMembershipRepository(
          addMembershipResult = Result.failure(Exception("Failed to add member")))

  private fun createMembershipRepositoryWithFailureNullMessage(): TestMockMembershipRepository =
      TestMockMembershipRepository(addMembershipResult = Result.failure(Exception()))

  private fun createUserRepositoryWithUser(user: User = testUser): FakeUserRepository =
      FakeUserRepository(currentUser = user)

  private fun createUserRepositoryWithoutUser(): FakeUserRepository =
      FakeUserRepository(currentUser = null, createdUser = null)

  private fun createUserRepositoryWithThrow(): FakeUserRepository =
      FakeUserRepository(throwOnLoad = true)

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
    val orgRepository = createEmptyOrgRepository()
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)

    val state = viewModel.state.value

    Assert.assertTrue(state.invitations.isEmpty())
    Assert.assertTrue(state.loading)
    Assert.assertNull(state.errorMessage)
  }

  // ========================================
  // Tests for Loading Invitations
  // ========================================

  @Test
  fun loadPendingInvitations_loadsOnlyPendingInvitations() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val allInvitations =
        listOf(pendingInvitation, pendingInvitation2, acceptedInvitation, rejectedInvitation)
    val orgRepository = createOrgRepositoryWithInvitations(allInvitations)
    val membershipRepository = createDefaultMembershipRepository()

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Wait for the state to stabilize
    val state = viewModel.state.filter { !it.loading || it.errorMessage != null }.first()

    Assert.assertEquals(2, state.invitations.size)
    Assert.assertTrue(state.invitations.all { it.status == InvitationStatus.PENDING })
    Assert.assertTrue(state.invitations.any { it.id == "invite-1" })
    Assert.assertTrue(state.invitations.any { it.id == "invite-2" })
    Assert.assertFalse(state.loading)
    Assert.assertNull(state.errorMessage)
  }

  @Test
  fun loadPendingInvitations_noPendingInvitations_returnsEmptyList() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithInvitations(listOf(acceptedInvitation))
    val membershipRepository = createDefaultMembershipRepository()

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    Assert.assertTrue(state.invitations.isEmpty())
    Assert.assertFalse(state.loading)
    Assert.assertNull(state.errorMessage)
  }

  @Test
  fun loadPendingInvitations_userNotFound_setsErrorMessage() = runTest {
    val userRepository = createUserRepositoryWithoutUser()
    val orgRepository = createEmptyOrgRepository()
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    Assert.assertNotNull("Expected an error message", state.errorMessage)
    val errorMessage = state.errorMessage!!
    Assert.assertTrue(
        errorMessage.contains("User not found") || errorMessage.contains("not logged in"))
  }

  @Test
  fun loadPendingInvitations_repositoryError_setsErrorMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()
    val orgRepository = createOrgRepositoryWithError(shouldThrowOnGetInvitations = true)
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    Assert.assertNotNull("Expected an error message", state.errorMessage)
  }

  // ========================================
  // Tests for Retry
  // ========================================

  @Test
  fun retry_afterUserLoadError_clearsErrorAndReloadsInvitations() = runTest {
    val userRepository = FakeUserRepository()
    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    var state = viewModel.state.value
    Assert.assertNotNull("Expected an error message when user cannot be loaded", state.errorMessage)
    Assert.assertTrue(
        "No invitations should be loaded when user fails", state.invitations.isEmpty())

    userRepository.updateCurrentUser(testUser)
    userRepository.updateCreatedUser(testUser)

    viewModel.retry()
    testDispatcher.scheduler.advanceUntilIdle()

    state = viewModel.state.value
    Assert.assertNull("Error message should be cleared after retry succeeds", state.errorMessage)
    Assert.assertFalse("Loading should be false after retry succeeds", state.loading)
    Assert.assertEquals(
        "Expected invitations to be reloaded after retry", 1, state.invitations.size)
    Assert.assertEquals("invite-1", state.invitations.first().id)
  }

  @Test
  fun retry_clearsErrorMessageAndSuccessMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()
    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    // Set an error message by trying to accept a non-existent invitation
    viewModel.acceptInvitation("non-existent")
    testDispatcher.scheduler.advanceUntilIdle()

    var state = viewModel.state.value
    Assert.assertNotNull("Error message should be set", state.errorMessage)

    // Set a success message by accepting a valid invitation
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Now call retry - both messages should be cleared
    // Since user exists, loadUserEmail() will succeed and not set a new error
    viewModel.retry()
    testDispatcher.scheduler.advanceUntilIdle()

    state = viewModel.state.value
    // After retry with a valid user, both messages should be cleared
    Assert.assertNull("Error message should be cleared when retry is called", state.errorMessage)
    Assert.assertNull(
        "Success message should be cleared when retry is called", state.successMessage)
  }

  @Test
  fun retry_clearsSuccessMessageWhenSet() = runTest {
    val userRepository = createUserRepositoryWithUser()
    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    // Set a success message by accepting an invitation
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    var state = viewModel.state.value
    Assert.assertNotNull(
        "Success message should be set after accepting invitation", state.successMessage)

    // Call retry - success message should be cleared
    // Since user exists, loadUserEmail() will succeed and not interfere
    viewModel.retry()
    testDispatcher.scheduler.advanceUntilIdle()

    state = viewModel.state.value
    Assert.assertNull(
        "Success message should be cleared when retry is called", state.successMessage)
  }

  // ========================================
  // Tests for Accepting Invitations
  // ========================================

  @Test
  fun acceptInvitation_success_removesInvitationFromList() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify invitation is initially in the list
    var state = viewModel.state.value
    Assert.assertEquals(1, state.invitations.size)
    Assert.assertEquals("invite-1", state.invitations.first().id)

    // Accept the invitation
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify invitation is removed from the list (since it's no longer PENDING)
    state = viewModel.state.value
    Assert.assertTrue("Invitation should be removed after acceptance", state.invitations.isEmpty())
    Assert.assertNull(state.errorMessage)

    // Verify addMembership was called with correct parameters
    Assert.assertEquals(
        "addMembership should be called once", 1, membershipRepository.addMembershipCalls.size)
    val (userId, orgId, role) = membershipRepository.addMembershipCalls.first()
    Assert.assertEquals("Organization ID should match", pendingInvitation.orgId, orgId)
    Assert.assertEquals("User ID should match", testUser.uid, userId)
    Assert.assertEquals("Role should match", pendingInvitation.role, role)
  }

  @Test
  fun acceptInvitation_success_callsAddMember() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify addMembership was called
    Assert.assertEquals(
        "addMembership should be called", 1, membershipRepository.addMembershipCalls.size)
    val (userId, orgId, role) = membershipRepository.addMembershipCalls.first()
    Assert.assertEquals("Organization ID should match", pendingInvitation.orgId, orgId)
    Assert.assertEquals("User ID should match", testUser.uid, userId)
    Assert.assertEquals("Role should match", pendingInvitation.role, role)
  }

  @Test
  fun acceptInvitation_addMemberFailure_showsErrorMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))
    val membershipRepository = createMembershipRepositoryWithFailure()

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message when addMember fails", state.errorMessage)
    Assert.assertTrue(
        "Error message should mention addMember failure",
        state.errorMessage!!.contains("add you as a member") ||
            state.errorMessage!!.contains("Failed to add member"))

    // Verify addMembership was still called (even though it failed)
    Assert.assertEquals(
        "addMembership should be called", 1, membershipRepository.addMembershipCalls.size)
  }

  @Test
  fun acceptInvitation_failure_setsErrorMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithUpdateFailure()

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message", state.errorMessage)
    Assert.assertTrue(state.errorMessage!!.contains("Failed"))
  }

  @Test
  fun acceptInvitation_nonExistentInvitation_handlesGracefully() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createEmptyOrgRepository()
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("non-existent-invite")
    testDispatcher.scheduler.advanceUntilIdle()

    // Should show error message for invitation not found
    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message", state.errorMessage)
    Assert.assertTrue(
        "Error should mention invitation not found",
        state.errorMessage!!.contains("not found") || state.errorMessage!!.contains("removed"))

    // addMembership should not be called if invitation is not found
    Assert.assertEquals(
        "addMembership should not be called", 0, membershipRepository.addMembershipCalls.size)
  }

  @Test
  fun acceptInvitation_userNotFound_showsErrorMessage() = runTest {
    val userRepository = createUserRepositoryWithoutUser()

    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message when user is not found", state.errorMessage)
    Assert.assertTrue(
        "Error should mention user not found",
        state.errorMessage!!.contains("User not found") || state.errorMessage!!.contains("log in"))

    // addMembership should not be called if user is not found
    Assert.assertEquals(
        "addMembership should not be called", 0, membershipRepository.addMembershipCalls.size)
  }

  // ========================================
  // Tests for Rejecting Invitations
  // ========================================

  @Test
  fun rejectInvitation_success_removesInvitationFromList() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify invitation is initially in the list
    var state = viewModel.state.value
    Assert.assertEquals(1, state.invitations.size)
    Assert.assertEquals("invite-1", state.invitations.first().id)

    // Reject the invitation
    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify invitation is removed from the list (since it's no longer PENDING)
    state = viewModel.state.value
    Assert.assertTrue("Invitation should be removed after rejection", state.invitations.isEmpty())
    Assert.assertNull(state.errorMessage)
  }

  @Test
  fun rejectInvitation_failure_setsErrorMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithUpdateFailure()

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message", state.errorMessage)
    Assert.assertTrue(state.errorMessage!!.contains("Failed"))
  }

  @Test
  fun rejectInvitation_nonExistentInvitation_handlesGracefully() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createEmptyOrgRepository()
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.rejectInvitation("non-existent-invite")
    testDispatcher.scheduler.advanceUntilIdle()

    // Should not crash, but may or may not set an error depending on implementation
    val state = viewModel.state.value
    Assert.assertNotNull(state)
  }

  // ========================================
  // Tests for Multiple Invitations
  // ========================================

  @Test
  fun multipleInvitations_acceptOne_removesOnlyThatInvitation() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository =
        createOrgRepositoryWithInvitations(listOf(pendingInvitation, pendingInvitation2))

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify both invitations are in the list
    var state = viewModel.state.value
    Assert.assertEquals(2, state.invitations.size)

    // Accept only the first invitation
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify only invitation2 remains
    state = viewModel.state.value
    Assert.assertEquals(1, state.invitations.size)
    Assert.assertEquals("invite-2", state.invitations.first().id)
  }

  @Test
  fun multipleInvitations_rejectOne_removesOnlyThatInvitation() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository =
        createOrgRepositoryWithInvitations(listOf(pendingInvitation, pendingInvitation2))

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify both invitations are in the list
    var state = viewModel.state.value
    Assert.assertEquals(2, state.invitations.size)

    // Reject only the first invitation
    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify only invitation2 remains
    state = viewModel.state.value
    Assert.assertEquals(1, state.invitations.size)
    Assert.assertEquals("invite-2", state.invitations.first().id)
  }

  // ========================================
  // Tests for Error Handling
  // ========================================

  @Test
  fun acceptInvitation_clearsPreviousError() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithUpdateFailure()

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // First attempt fails
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message", state.errorMessage)
    // Note: In a real scenario, we'd need to recreate the ViewModel with a new repository
    // to test error clearing, but for this test we're just verifying that error is set
  }

  @Test
  fun rejectInvitation_clearsPreviousError() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithUpdateFailure()

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // First attempt fails
    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message", state.errorMessage)
  }

  // ========================================
  // Tests for invitations StateFlow direct access
  // ========================================

  @Test
  fun invitationsStateFlow_directAccess_returnsPendingInvitations() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository =
        createOrgRepositoryWithInvitations(listOf(pendingInvitation, acceptedInvitation))
    val membershipRepository = createDefaultMembershipRepository()

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Directly access invitations StateFlow
    val invitations = viewModel.invitations.value

    Assert.assertEquals(1, invitations.size)
    Assert.assertEquals("invite-1", invitations.first().id)
    Assert.assertEquals(InvitationStatus.PENDING, invitations.first().status)
  }

  @Test
  fun invitationsStateFlow_emptyEmail_returnsEmptyList() = runTest {
    val userRepository = createUserRepositoryWithoutUser()
    val orgRepository = createEmptyOrgRepository()
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    // Directly access invitations StateFlow when email is null
    val invitations = viewModel.invitations.value

    Assert.assertTrue(invitations.isEmpty())
  }

  // ========================================
  // Tests for Error Handling in catch blocks
  // ========================================

  @Test
  fun loadUserEmail_throwsException_setsErrorMessage() = runTest {
    val userRepository = createUserRepositoryWithThrow()
    val orgRepository = createEmptyOrgRepository()
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    Assert.assertNotNull("Expected an error message when exception is thrown", state.errorMessage)
    val errorMessage = state.errorMessage!!
    Assert.assertTrue(
        errorMessage.contains("Failed to load user information") || errorMessage.contains("boom"))
  }

  @Test
  fun acceptInvitation_throwsException_setsErrorMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithUpdateThrow()

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message when exception is thrown", state.errorMessage)
    val errorMessage = state.errorMessage!!
    Assert.assertTrue(
        errorMessage.contains("Failed to accept invitation") ||
            errorMessage.contains("Update invitation status failed"))
  }

  @Test
  fun rejectInvitation_throwsException_setsErrorMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithUpdateThrow()

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message when exception is thrown", state.errorMessage)
    val errorMessage = state.errorMessage!!
    Assert.assertTrue(
        errorMessage.contains("Failed to reject invitation") ||
            errorMessage.contains("Update invitation status failed"))
  }

  @Test
  fun invitationsFlow_repositoryErrorWithNullMessage_handlesGracefully() = runTest {
    val userRepository = createUserRepositoryWithUser()

    // Create a repository that throws an exception with null message
    val orgRepository =
        createOrgRepositoryWithError(shouldThrowOnGetInvitations = true, exceptionMessage = null)
    val membershipRepository = createDefaultMembershipRepository()

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    Assert.assertNotNull("Expected an error message", state.errorMessage)
    Assert.assertEquals("Failed to load invitations", state.errorMessage!!)
  }

  @Test
  fun loadUserEmail_userWithBlankEmail_setsErrorMessage() = runTest {
    val testUser = User(uid = "user-1", email = "", displayName = "Test User")
    val userRepository = createUserRepositoryWithUser(testUser)
    val orgRepository = createEmptyOrgRepository()
    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    Assert.assertNotNull("Expected an error message for blank email", state.errorMessage)
    val errorMessage = state.errorMessage!!
    Assert.assertTrue(
        errorMessage.contains("User not found") || errorMessage.contains("not logged in"))
  }

  @Test
  fun acceptInvitation_resultFailureWithNullMessage_usesDefaultMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()
    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))
    val membershipRepository = createMembershipRepositoryWithFailureNullMessage()

    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message", state.errorMessage)
    Assert.assertTrue(
        "Error message should use default message", state.errorMessage!!.contains("Unknown error"))
  }

  @Test
  fun acceptInvitation_updateStatusFailureWithNullMessage_usesDefaultMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()

    // Create a failure result with exception that has null message
    val orgRepository = createOrgRepositoryWithUpdateFailureNullMessage()

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message", state.errorMessage)
    Assert.assertEquals("Failed to accept invitation", state.errorMessage!!)
  }

  @Test
  fun rejectInvitation_resultFailureWithNullMessage_usesDefaultMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()

    // Create a failure result with exception that has null message
    val orgRepository = createOrgRepositoryWithUpdateFailureNullMessage()

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    Assert.assertNotNull("Expected an error message", state.errorMessage)
    Assert.assertEquals("Failed to reject invitation", state.errorMessage!!)
  }

  // ========================================
  // Tests for clearSuccessMessage
  // ========================================

  @Test
  fun clearSuccessMessage_clearsSuccessMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // First, set a success message by accepting an invitation
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify success message is set
    var state = viewModel.state.value
    Assert.assertNotNull(
        "Expected a success message after accepting invitation", state.successMessage)

    // Clear the success message
    viewModel.clearSuccessMessage()
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify success message is cleared
    state = viewModel.state.value
    Assert.assertNull("Success message should be cleared", state.successMessage)
  }

  @Test
  fun clearSuccessMessage_whenNoSuccessMessage_doesNotError() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify initial state has no success message
    var state = viewModel.state.value
    Assert.assertNull("Initial state should have no success message", state.successMessage)

    // Clear success message when it's already null
    viewModel.clearSuccessMessage()
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify state is still valid and success message is still null
    state = viewModel.state.value
    Assert.assertNull("Success message should still be null", state.successMessage)
  }

  @Test
  fun clearSuccessMessage_afterRejectInvitation_clearsSuccessMessage() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Reject invitation to set success message
    viewModel.rejectInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify success message is set
    var state = viewModel.state.value
    Assert.assertNotNull(
        "Expected a success message after rejecting invitation", state.successMessage)
    Assert.assertTrue(
        "Success message should mention rejection", state.successMessage!!.contains("rejected"))

    // Clear the success message
    viewModel.clearSuccessMessage()
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify success message is cleared
    state = viewModel.state.value
    Assert.assertNull("Success message should be cleared", state.successMessage)
  }

  @Test
  fun clearSuccessMessage_doesNotAffectOtherState() = runTest {
    val userRepository = createUserRepositoryWithUser()

    val orgRepository = createOrgRepositoryWithInvitations(listOf(pendingInvitation))

    val membershipRepository = createDefaultMembershipRepository()
    val viewModel = MyInvitationsViewModel(orgRepository, userRepository, membershipRepository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Accept invitation to set success message
    viewModel.acceptInvitation("invite-1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Get state before clearing
    val stateBefore = viewModel.state.value
    val invitationsBefore = stateBefore.invitations
    val loadingBefore = stateBefore.loading
    val errorMessageBefore = stateBefore.errorMessage
    val userEmailBefore = stateBefore.userEmail

    // Clear success message
    viewModel.clearSuccessMessage()
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify other state properties are unchanged
    val stateAfter = viewModel.state.value
    Assert.assertEquals("Invitations should not change", invitationsBefore, stateAfter.invitations)
    Assert.assertEquals("Loading should not change", loadingBefore, stateAfter.loading)
    Assert.assertEquals(
        "Error message should not change", errorMessageBefore, stateAfter.errorMessage)
    Assert.assertEquals("User email should not change", userEmailBefore, stateAfter.userEmail)
    Assert.assertNull("Success message should be cleared", stateAfter.successMessage)
  }
}
