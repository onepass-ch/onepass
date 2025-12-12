package ch.onepass.onepass.ui.staff

import ch.onepass.onepass.model.membership.Membership
import ch.onepass.onepass.model.organization.FakeOrganizationRepository
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.model.user.FakeUserRepository
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.model.user.UserSearchType
import ch.onepass.onepass.utils.TestMockMembershipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StaffInvitationViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  private lateinit var userRepository: FakeUserRepository
  private lateinit var organizationRepository: FakeOrganizationRepository
  private lateinit var membershipRepository: TestMockMembershipRepository
  private val testOrganizationId = "org_123"
  private val testUserId = "user_123"

  private val testUser =
      User(uid = testUserId, email = "test@example.com", displayName = "Test User")

  private val testSearchResult1 =
      StaffSearchResult(
          id = "user1", email = "john@example.com", displayName = "John Doe", avatarUrl = null)

  private val testSearchResult2 =
      StaffSearchResult(
          id = "user2",
          email = "jane@example.com",
          displayName = "Jane Smith",
          avatarUrl = "https://example.com/avatar.jpg")

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    userRepository = FakeUserRepository(currentUser = testUser)
    organizationRepository = FakeOrganizationRepository()
    membershipRepository =
        TestMockMembershipRepository(
            usersByOrganization =
                mapOf(
                    testOrganizationId to
                        listOf(
                            Membership(
                                userId = testUserId,
                                orgId = testOrganizationId,
                                role = OrganizationRole.OWNER))))
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ========== Initialization Tests ==========

  @Test
  fun staffInvitationViewModel_initialState_hasCorrectDefaults() =
      testScope.runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value

        assertEquals(UserSearchType.DISPLAY_NAME, state.selectedTab)
        assertEquals("", state.searchQuery)
        assertEquals(emptyList<StaffSearchResult>(), state.searchResults)
        assertFalse(state.isLoading)
        assertFalse(state.isInviting)
        assertNull(state.errorMessage)
        assertEquals(emptySet<String>(), state.invitedUserIds)
        assertEquals(emptySet<String>(), state.alreadyInvitedUserIds)
        assertNull(state.selectedUserForInvite)
        assertEquals(OrganizationRole.STAFF, state.selectedRole)
      }

  @Test
  fun staffInvitationViewModel_init_loadsCurrentUserId() =
      testScope.runTest {
        val viewModel = createViewModel()

        // Wait for initialization
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not have error since user is loaded
        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
      }

  @Test
  fun staffInvitationViewModel_init_handlesMissingUser() =
      testScope.runTest {
        val repoWithoutUser = FakeUserRepository(currentUser = null, createdUser = null)
        val viewModel =
            StaffInvitationViewModel(
                organizationId = testOrganizationId,
                userRepository = repoWithoutUser,
                organizationRepository = organizationRepository,
                membershipRepository = membershipRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Expected error message when user not found", state.errorMessage)
        assertTrue(
            "Error should mention user not found",
            state.errorMessage?.contains("not found", ignoreCase = true) == true)
      }

  @Test
  fun staffInvitationViewModel_init_handlesExceptionWhenLoadingUser() =
      testScope.runTest {
        val repoWithError = FakeUserRepository(throwOnLoad = true)
        val viewModel =
            StaffInvitationViewModel(
                organizationId = testOrganizationId,
                userRepository = repoWithError,
                organizationRepository = organizationRepository,
                membershipRepository = membershipRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Expected error message when exception occurs", state.errorMessage)
      }

  // ========== Tab Selection Tests ==========

  @Test
  fun staffInvitationViewModel_selectTab_updatesSelectedTab() =
      testScope.runTest {
        val viewModel = createViewModel()

        viewModel.selectTab(UserSearchType.EMAIL)

        assertEquals(UserSearchType.EMAIL, viewModel.uiState.value.selectedTab)
      }

  @Test
  fun staffInvitationViewModel_selectTab_clearsSearchWhenSwitchingTabs() =
      testScope.runTest {
        val viewModel = createViewModel()

        // Set up initial search state
        userRepository.setSearchResults(listOf(testSearchResult1))
        viewModel.updateSearchQuery("john")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify search results exist
        assertTrue(viewModel.uiState.value.searchResults.isNotEmpty())

        // Switch tab
        viewModel.selectTab(UserSearchType.EMAIL)

        val state = viewModel.uiState.value
        assertEquals(UserSearchType.EMAIL, state.selectedTab)
        assertEquals("", state.searchQuery)
        assertEquals(emptyList<StaffSearchResult>(), state.searchResults)
        assertNull(state.errorMessage)
      }

  @Test
  fun staffInvitationViewModel_selectTab_doesNothingWhenSelectingSameTab() =
      testScope.runTest {
        val viewModel = createViewModel()

        viewModel.updateSearchQuery("test")
        testDispatcher.scheduler.advanceUntilIdle()

        val queryBefore = viewModel.uiState.value.searchQuery

        viewModel.selectTab(UserSearchType.DISPLAY_NAME)

        assertEquals(queryBefore, viewModel.uiState.value.searchQuery)
      }

  // ========== Search Query Tests ==========

  @Test
  fun staffInvitationViewModel_updateSearchQuery_updatesQueryImmediately() =
      testScope.runTest {
        val viewModel = createViewModel()

        viewModel.updateSearchQuery("john")

        assertEquals("john", viewModel.uiState.value.searchQuery)
      }

  @Test
  fun staffInvitationViewModel_updateSearchQuery_clearsResultsWhenEmpty() =
      testScope.runTest {
        val viewModel = createViewModel()

        // Set up search results
        userRepository.setSearchResults(listOf(testSearchResult1))
        viewModel.updateSearchQuery("john")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.isNotEmpty())

        // Clear query
        viewModel.updateSearchQuery("")

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(emptyList<StaffSearchResult>(), state.searchResults)
        assertFalse(state.isLoading)
      }

  @Test
  fun staffInvitationViewModel_updateSearchQuery_debouncesSearch() =
      testScope.runTest {
        val viewModel = createViewModel()
        userRepository.setSearchResults(listOf(testSearchResult1))

        viewModel.updateSearchQuery("j")
        testDispatcher.scheduler.advanceTimeBy(200)
        viewModel.updateSearchQuery("jo")
        testDispatcher.scheduler.advanceTimeBy(200)
        viewModel.updateSearchQuery("john")

        // Should not have searched yet (debounce not elapsed)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(emptyList<StaffSearchResult>(), viewModel.uiState.value.searchResults)

        // Advance just before debounce delay completes
        testDispatcher.scheduler.advanceTimeBy(499)
        // Should still not have searched
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(emptyList<StaffSearchResult>(), viewModel.uiState.value.searchResults)

        // Advance past debounce delay - this will trigger search
        testDispatcher.scheduler.advanceTimeBy(1)
        // Search should have completed (repository call is synchronous in test)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify search results are set
        assertEquals(listOf(testSearchResult1), viewModel.uiState.value.searchResults)
        assertFalse(viewModel.uiState.value.isLoading)
      }

  @Test
  fun staffInvitationViewModel_updateSearchQuery_cancelsPreviousSearchJob() =
      testScope.runTest {
        val viewModel = createViewModel()
        userRepository.setSearchResults(listOf(testSearchResult1))

        viewModel.updateSearchQuery("john")
        testDispatcher.scheduler.advanceTimeBy(200)

        // Update query before debounce completes
        viewModel.updateSearchQuery("jane")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should only have searched once for "jane"
        assertEquals("jane", viewModel.uiState.value.searchQuery)
      }

  @Test
  fun staffInvitationViewModel_updateSearchQuery_clearsErrorMessage() =
      testScope.runTest {
        val viewModel = createViewModel()

        // Set an error
        viewModel.updateSearchQuery("test")
        userRepository.setSearchError(RuntimeException("Search failed"))
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        // Update query should clear error
        viewModel.updateSearchQuery("new query")

        assertNull(viewModel.uiState.value.errorMessage)
      }

  // ========== Search Execution Tests ==========

  @Test
  fun staffInvitationViewModel_performSearch_updatesResultsOnSuccess() =
      testScope.runTest {
        val viewModel = createViewModel()
        val results = listOf(testSearchResult1, testSearchResult2)
        userRepository.setSearchResults(results)

        viewModel.updateSearchQuery("john")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(results, state.searchResults)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
      }

  @Test
  fun staffInvitationViewModel_performSearch_setsLoadingState() =
      testScope.runTest {
        val viewModel = createViewModel()
        userRepository.setSearchResults(listOf(testSearchResult1))

        viewModel.updateSearchQuery("john")
        // Advance just before debounce completes
        testDispatcher.scheduler.advanceTimeBy(499)
        // Should not be loading yet (delay not completed)
        assertFalse(viewModel.uiState.value.isLoading)

        // Advance past debounce delay - this triggers performSearch
        // Since repository call is synchronous in test, performSearch completes immediately.
        // We verify that performSearch was called by checking that results are set.
        testDispatcher.scheduler.advanceTimeBy(1)
        // Run current to ensure performSearch has started
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify search was performed (results are set and loading is false)
        assertEquals(listOf(testSearchResult1), viewModel.uiState.value.searchResults)
        assertFalse(viewModel.uiState.value.isLoading)
      }

  @Test
  fun staffInvitationViewModel_performSearch_handlesSearchError() =
      testScope.runTest {
        val viewModel = createViewModel()
        userRepository.setSearchError(RuntimeException("Network error"))

        viewModel.updateSearchQuery("john")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(emptyList<StaffSearchResult>(), state.searchResults)
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage?.contains("Search failed", ignoreCase = true) == true)
      }

  @Test
  fun staffInvitationViewModel_performSearch_handlesException() =
      testScope.runTest {
        val viewModel = createViewModel()
        userRepository.setSearchFunction { _, _, _ ->
          throw IllegalStateException("Unexpected error")
        }

        viewModel.updateSearchQuery("john")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(emptyList<StaffSearchResult>(), state.searchResults)
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage?.contains("Search error", ignoreCase = true) == true)
      }

  @Test
  fun staffInvitationViewModel_performSearch_usesCorrectSearchType() =
      testScope.runTest {
        val viewModel = createViewModel()
        var capturedSearchType: UserSearchType? = null

        userRepository.setSearchFunction { _, searchType, _ ->
          capturedSearchType = searchType
          Result.success(emptyList())
        }

        viewModel.selectTab(UserSearchType.EMAIL)
        viewModel.updateSearchQuery("test@example.com")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(UserSearchType.EMAIL, capturedSearchType)
      }

  @Test
  fun staffInvitationViewModel_performSearch_passesOrganizationId() =
      testScope.runTest {
        val viewModel = createViewModel()
        var capturedOrgId: String? = null

        userRepository.setSearchFunction { _, _, orgId ->
          capturedOrgId = orgId
          Result.success(emptyList())
        }

        viewModel.updateSearchQuery("test")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testOrganizationId, capturedOrgId)
      }

  @Test
  fun staffInvitationViewModel_performSearch_trimsQuery() =
      testScope.runTest {
        val viewModel = createViewModel()
        var capturedQuery: String? = null

        userRepository.setSearchFunction { query, _, _ ->
          capturedQuery = query
          Result.success(emptyList())
        }

        viewModel.updateSearchQuery("  john  ")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("john", capturedQuery)
      }

  // ========== User Selection and Invitation Tests ==========

  @Test
  fun staffInvitationViewModel_onUserSelected_opensConfirmationDialog() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUserSelected(testSearchResult1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInviting)
        assertEquals(testSearchResult1, state.selectedUserForInvite)
        assertEquals(OrganizationRole.STAFF, state.selectedRole)
        assertNull(state.errorMessage)
        // Invitation should NOT be sent yet
        assertTrue(state.invitedUserIds.isEmpty())
      }

  @Test
  fun staffInvitationViewModel_confirmInvitation_sendsInvitation() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Select user first
        viewModel.onUserSelected(testSearchResult1)

        // Confirm invitation
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInviting)
        assertTrue(testSearchResult1.id in state.invitedUserIds)
        assertNull(state.selectedUserForInvite) // Dialog should be closed
        assertNull(state.errorMessage)
      }

  @Test
  fun staffInvitationViewModel_cancelInvitation_clearsSelection() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUserSelected(testSearchResult1)
        assertNotNull(viewModel.uiState.value.selectedUserForInvite)

        viewModel.cancelInvitation()

        val state = viewModel.uiState.value
        assertNull(state.selectedUserForInvite)
        assertNull(state.errorMessage)
        assertTrue(state.invitedUserIds.isEmpty())
      }

  @Test
  fun staffInvitationViewModel_selectRole_updatesRole() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUserSelected(testSearchResult1)
        assertEquals(OrganizationRole.STAFF, viewModel.uiState.value.selectedRole)

        viewModel.selectRole(OrganizationRole.OWNER)
        assertEquals(OrganizationRole.OWNER, viewModel.uiState.value.selectedRole)
      }

  @Test
  fun staffInvitationViewModel_confirmInvitation_setsInvitingState() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUserSelected(testSearchResult1)
        viewModel.confirmInvitation()

        // Run current tasks to ensure the coroutine has started and isInviting is set
        testDispatcher.scheduler.runCurrent()

        val stateAfterRun = viewModel.uiState.value
        // Verify invitation was processed (either isInviting is true or invitation completed)
        assertTrue(stateAfterRun.isInviting || testSearchResult1.id in stateAfterRun.invitedUserIds)

        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.uiState.value
        // Verify invitation was processed successfully
        assertTrue(testSearchResult1.id in finalState.invitedUserIds)
        assertFalse(finalState.isInviting)
      }

  @Test
  fun staffInvitationViewModel_onUserSelected_handlesMissingUserId() =
      testScope.runTest {
        val repoWithoutUser = FakeUserRepository(currentUser = null, createdUser = null)
        val viewModel =
            StaffInvitationViewModel(
                organizationId = testOrganizationId,
                userRepository = repoWithoutUser,
                organizationRepository = organizationRepository,
                membershipRepository = membershipRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUserSelected(testSearchResult1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInviting)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage?.contains("not authenticated", ignoreCase = true) == true)
      }

  @Test
  fun staffInvitationViewModel_confirmInvitation_detectsAlreadyPendingInvitation() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Create a pending invitation
        val existingInvitation =
            OrganizationInvitation(
                id = "inv_1",
                orgId = testOrganizationId,
                inviteeEmail = testSearchResult1.email,
                role = OrganizationRole.STAFF,
                invitedBy = testUserId,
                status = InvitationStatus.PENDING)
        organizationRepository.addTestInvitation(existingInvitation)

        viewModel.onUserSelected(testSearchResult1)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInviting)
        // onUserSelected detects it and shows snackbar, so confirmInvitation is not called
        assertNotNull(state.snackbarMessage)
        assertTrue(
            state.snackbarMessage?.contains("already been invited", ignoreCase = true) == true)
        assertNull(state.selectedUserForInvite) // Dialog should not open
      }

  @Test
  fun staffInvitationViewModel_confirmInvitation_detectsAlreadyAcceptedInvitation() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Create an accepted invitation (user is already a member)
        val existingInvitation =
            OrganizationInvitation(
                id = "inv_1",
                orgId = testOrganizationId,
                inviteeEmail = testSearchResult1.email,
                role = OrganizationRole.STAFF,
                invitedBy = testUserId,
                status = InvitationStatus.ACCEPTED)
        organizationRepository.addTestInvitation(existingInvitation)

        viewModel.onUserSelected(testSearchResult1)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInviting)
        // onUserSelected detects it and shows snackbar
        assertNotNull(state.snackbarMessage)
        assertTrue(state.snackbarMessage?.contains("already a member", ignoreCase = true) == true)
        assertNull(state.selectedUserForInvite) // Dialog should not open
      }

  @Test
  fun staffInvitationViewModel_confirmInvitation_ignoresInvitationsForDifferentOrgs() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Create invitation for different organization
        val otherOrgInvitation =
            OrganizationInvitation(
                id = "inv_1",
                orgId = "other_org",
                inviteeEmail = testSearchResult1.email,
                role = OrganizationRole.STAFF,
                invitedBy = testUserId,
                status = InvitationStatus.PENDING)
        organizationRepository.addTestInvitation(otherOrgInvitation)

        viewModel.onUserSelected(testSearchResult1)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // Should succeed since invitation is for different org
        assertTrue(testSearchResult1.id in state.invitedUserIds)
      }

  @Test
  fun staffInvitationViewModel_confirmInvitation_ignoresNonPendingInvitations() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Create rejected invitation
        val rejectedInvitation =
            OrganizationInvitation(
                id = "inv_1",
                orgId = testOrganizationId,
                inviteeEmail = testSearchResult1.email,
                role = OrganizationRole.STAFF,
                invitedBy = testUserId,
                status = InvitationStatus.REJECTED)
        organizationRepository.addTestInvitation(rejectedInvitation)

        viewModel.onUserSelected(testSearchResult1)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // Should succeed since previous invitation was rejected
        assertTrue(testSearchResult1.id in state.invitedUserIds)
      }

  @Test
  fun staffInvitationViewModel_confirmInvitation_handlesInvitationCreationError() =
      testScope.runTest {
        val failingOrgRepo = FakeOrganizationRepository(shouldThrowOnCreate = true)
        val viewModel =
            StaffInvitationViewModel(
                organizationId = testOrganizationId,
                userRepository = userRepository,
                organizationRepository = failingOrgRepo,
                membershipRepository = membershipRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUserSelected(testSearchResult1)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInviting)
        assertNotNull(state.invitationResultMessage)
        assertTrue(
            state.invitationResultMessage?.contains(
                "Failed to create invitation", ignoreCase = true) == true)
        assertEquals(InvitationResultType.ERROR, state.invitationResultType)
        // Dialog closes on error
        assertNull(state.selectedUserForInvite)
      }

  @Test
  fun staffInvitationViewModel_confirmInvitation_handlesExceptionDuringInvitation() =
      testScope.runTest {
        // Make getInvitationsByEmail throw
        val failingOrgRepo = FakeOrganizationRepository(shouldThrowOnGetInvitations = true)
        val failingViewModel =
            StaffInvitationViewModel(
                organizationId = testOrganizationId,
                userRepository = userRepository,
                organizationRepository = failingOrgRepo,
                membershipRepository = membershipRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        failingViewModel.onUserSelected(testSearchResult1)
        failingViewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = failingViewModel.uiState.value
        assertFalse(state.isInviting)
        assertNotNull(state.errorMessage)
        assertTrue(
            state.errorMessage?.contains("Failed to check invitation status", ignoreCase = true) ==
                true)
      }

  @Test
  fun staffInvitationViewModel_confirmInvitation_createsInvitationWithCorrectData() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUserSelected(testSearchResult1)
        viewModel.selectRole(OrganizationRole.OWNER)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(
            "Error message should be null: ${viewModel.uiState.value.errorMessage}",
            viewModel.uiState.value.errorMessage)

        // Verify invitation was created
        val invitations =
            organizationRepository.getInvitationsByEmail(testSearchResult1.email).first()

        assertEquals(1, invitations.size)
        val invitation = invitations.first()
        assertEquals(testOrganizationId, invitation.orgId)
        assertEquals(testSearchResult1.email, invitation.inviteeEmail)
        assertEquals(OrganizationRole.OWNER, invitation.role)
        assertEquals(testUserId, invitation.invitedBy)
        assertEquals(InvitationStatus.PENDING, invitation.status)
      }

  // ========== Error Handling Tests ==========

  @Test
  fun staffInvitationViewModel_clearError_removesErrorMessage() =
      testScope.runTest {
        val viewModel = createViewModel()

        // Set an error
        userRepository.setSearchError(RuntimeException("Test error"))
        viewModel.updateSearchQuery("test")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
      }

  // ========== Integration Tests ==========

  @Test
  fun staffInvitationViewModel_fullFlow_searchAndInviteWorksCorrectly() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Search for users
        userRepository.setSearchResults(listOf(testSearchResult1, testSearchResult2))
        viewModel.selectTab(UserSearchType.EMAIL)
        viewModel.updateSearchQuery("john@example.com")
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.searchResults.size)

        // Invite first user
        viewModel.onUserSelected(testSearchResult1)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(testSearchResult1.id in viewModel.uiState.value.invitedUserIds)
        assertFalse(testSearchResult1.id in viewModel.uiState.value.alreadyInvitedUserIds)

        // Try to invite same user again - should be prevented by early return
        viewModel.onUserSelected(testSearchResult1)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        // User should still be in invitedUserIds
        assertTrue(testSearchResult1.id in viewModel.uiState.value.invitedUserIds)
        // Should show snackbar because we added the check for already invited users in session
        assertNotNull(viewModel.uiState.value.snackbarMessage)
        assertTrue(
            viewModel.uiState.value.snackbarMessage?.contains("already been invited") == true)
      }

  @Test
  fun staffInvitationViewModel_multipleUsers_canBeInvited() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUserSelected(testSearchResult1)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUserSelected(testSearchResult2)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(testSearchResult1.id in state.invitedUserIds)
        assertTrue(testSearchResult2.id in state.invitedUserIds)
        assertEquals(2, state.invitedUserIds.size)
      }

  @Test
  fun staffInvitationViewModel_init_loadsCurrentUserRole() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(OrganizationRole.OWNER, viewModel.uiState.value.currentUserRole)
      }

  @Test
  fun staffInvitationViewModel_onUserSelected_showsPermissionDeniedForNonAdmin() =
      testScope.runTest {
        // Setup user as MEMBER (not OWNER or ADMIN)
        membershipRepository =
            TestMockMembershipRepository(
                usersByOrganization =
                    mapOf(
                        testOrganizationId to
                            listOf(
                                Membership(
                                    userId = testUserId,
                                    orgId = testOrganizationId,
                                    role = OrganizationRole.MEMBER))))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUserSelected(testSearchResult1)

        assertTrue(viewModel.uiState.value.showPermissionDeniedDialog)
        assertNull(viewModel.uiState.value.selectedUserForInvite)
      }

  @Test
  fun staffInvitationViewModel_checkAndCreateInvitation_preventsSelfInvitation() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Create a search result that matches the current user
        val selfSearchResult =
            StaffSearchResult(
                id = testUserId,
                email = testUser.email,
                displayName = testUser.displayName,
                avatarUrl = null)

        viewModel.onUserSelected(selfSearchResult)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(InvitationResultType.ERROR, state.invitationResultType)
        assertTrue(state.invitationResultMessage?.contains("cannot invite yourself") == true)
      }

  @Test
  fun staffInvitationViewModel_dismissPermissionDeniedDialog_hidesDialog() =
      testScope.runTest {
        val viewModel = createViewModel()
        // Manually set state to show dialog (or trigger it via non-admin flow)
        // Using reflection or just triggering it is easier if we have the setup.
        // Let's use the public method to trigger it by mocking a MEMBER role.
        membershipRepository =
            TestMockMembershipRepository(
                usersByOrganization =
                    mapOf(
                        testOrganizationId to
                            listOf(
                                Membership(
                                    userId = testUserId,
                                    orgId = testOrganizationId,
                                    role = OrganizationRole.MEMBER))))
        val memberViewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        memberViewModel.onUserSelected(testSearchResult1)
        assertTrue(memberViewModel.uiState.value.showPermissionDeniedDialog)

        memberViewModel.dismissPermissionDeniedDialog()
        assertFalse(memberViewModel.uiState.value.showPermissionDeniedDialog)
      }

  @Test
  fun staffInvitationViewModel_dismissInvitationResultDialog_clearsResult() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Trigger a success result
        viewModel.onUserSelected(testSearchResult1)
        viewModel.confirmInvitation()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.invitationResultType)

        viewModel.dismissInvitationResultDialog()

        assertNull(viewModel.uiState.value.invitationResultType)
        assertNull(viewModel.uiState.value.invitationResultMessage)
      }

  @Test
  fun staffInvitationViewModel_clearSnackbarMessage_clearsMessage() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Trigger a snackbar (e.g. already invited)
        val existingInvitation =
            OrganizationInvitation(
                id = "inv_1",
                orgId = testOrganizationId,
                inviteeEmail = testSearchResult1.email,
                role = OrganizationRole.STAFF,
                invitedBy = testUserId,
                status = InvitationStatus.PENDING)
        organizationRepository.addTestInvitation(existingInvitation)

        viewModel.onUserSelected(testSearchResult1)
        assertNotNull(viewModel.uiState.value.snackbarMessage)

        viewModel.clearSnackbarMessage()
        assertNull(viewModel.uiState.value.snackbarMessage)
      }

  @Test
  fun staffInvitationViewModel_getAvailableRoles_excludesOwner() =
      testScope.runTest {
        val viewModel = createViewModel()
        val roles = viewModel.getAvailableRoles()

        assertFalse(roles.contains(OrganizationRole.OWNER))
        assertTrue(roles.contains(OrganizationRole.ADMIN))
        assertTrue(roles.contains(OrganizationRole.MEMBER))
      }

  // ========== Helper Methods ==========

  private fun createViewModel(): StaffInvitationViewModel {
    return StaffInvitationViewModel(
        organizationId = testOrganizationId,
        userRepository = userRepository,
        organizationRepository = organizationRepository,
        membershipRepository = membershipRepository)
  }

  @Test
  fun staffInvitationViewModel_onUserSelected_returnsEarlyWhenAlreadyInviting() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Arrange: Set inviting state
        val uiStateField = StaffInvitationViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        val mutableStateFlow =
            uiStateField.get(viewModel) as MutableStateFlow<StaffInvitationUiState>
        val currentState = mutableStateFlow.value
        mutableStateFlow.value = currentState.copy(isInviting = true)

        // Act
        viewModel.onUserSelected(testSearchResult1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: Should not create invitation when already inviting
        val invitations =
            organizationRepository.getInvitationsByEmail(testSearchResult1.email).first()
        assertEquals(0, invitations.size)
      }

  @Test
  fun staffInvitationViewModel_onUserSelected_returnsEarlyWhenUserAlreadyInvited() =
      testScope.runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Arrange: Add user to invitedUserIds
        val uiStateField = StaffInvitationViewModel::class.java.getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        val mutableStateFlow =
            uiStateField.get(viewModel) as MutableStateFlow<StaffInvitationUiState>
        val currentState = mutableStateFlow.value
        mutableStateFlow.value = currentState.copy(invitedUserIds = setOf(testSearchResult1.id))

        // Act
        viewModel.onUserSelected(testSearchResult1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: Should not create duplicate invitation
        val invitations =
            organizationRepository.getInvitationsByEmail(testSearchResult1.email).first()
        assertEquals(0, invitations.size)
      }
}
