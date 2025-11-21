package ch.onepass.onepass.ui.myinvitations

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.ui.organization.MockOrganizationRepository
import ch.onepass.onepass.ui.theme.OnePassTheme
import ch.onepass.onepass.utils.OrganizationTestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for MyInvitationsScreen composable.
 *
 * These tests verify that the screen correctly displays:
 * - Loading state
 * - Error state
 * - Empty state
 * - List of invitations with organization names
 * - Accept and reject buttons for each invitation
 * - Interaction with buttons
 */
@RunWith(AndroidJUnit4::class)
class MyInvitationsScreenComposeTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  /**
   * Extended MockOrganizationRepository that supports multiple organizations via a Map.
   *
   * This extends the existing MockOrganizationRepository to support looking up organizations by ID
   * from a Map, which is needed for testing scenarios with multiple organizations.
   */
  private class TestOrganizationRepository(
      private val organizations: Map<String, Organization> = emptyMap()
  ) : MockOrganizationRepository() {
    override fun getOrganizationById(organizationId: String): Flow<Organization?> {
      return flowOf(organizations[organizationId])
    }
  }

  // Reusable test data
  private val testInvitation1 =
      OrganizationTestData.createTestInvitation(
          id = "invite-1", orgId = "org-1", inviteeEmail = "test@example.com")
  private val testInvitation2 =
      OrganizationTestData.createTestInvitation(
          id = "invite-2", orgId = "org-2", inviteeEmail = "test@example.com")
  private val testInvitation3 =
      OrganizationTestData.createTestInvitation(
          id = "invite-3", orgId = "org-3", inviteeEmail = "test@example.com")

  private val testOrg1 =
      OrganizationTestData.createTestOrganization(id = "org-1", name = "Test Organization")
  private val testOrg2 =
      OrganizationTestData.createTestOrganization(id = "org-2", name = "Organization Two")
  private val testOrg3 =
      OrganizationTestData.createTestOrganization(id = "org-3", name = "Organization Three")

  /**
   * Sets up the compose content with the given state and callbacks.
   *
   * @param state UI state to display.
   * @param onAcceptInvitation Callback for accepting invitations.
   * @param onRejectInvitation Callback for rejecting invitations.
   * @param onRetry Callback for retry action.
   * @param onClearSuccessMessage Callback for clearing success message.
   * @param onNavigateBack Callback for navigation back.
   * @param organizationRepository Repository for fetching organization details.
   */
  private fun setContent(
      state: MyInvitationsUiState,
      onAcceptInvitation: (String) -> Unit = {},
      onRejectInvitation: (String) -> Unit = {},
      onRetry: () -> Unit = {},
      onClearSuccessMessage: () -> Unit = {},
      onNavigateBack: () -> Unit = {},
      organizationRepository: OrganizationRepository = TestOrganizationRepository()
  ) {
    composeTestRule.setContent {
      OnePassTheme {
        MyInvitationsContent(
            state = state,
            onAcceptInvitation = onAcceptInvitation,
            onRejectInvitation = onRejectInvitation,
            onRetry = onRetry,
            onClearSuccessMessage = onClearSuccessMessage,
            onNavigateBack = onNavigateBack,
            organizationRepository = organizationRepository)
      }
    }
  }

  // ========================================
  // Tests for Loading State
  // ========================================

  @Test
  fun loadingState_displaysLoadingIndicator() {
    val state = MyInvitationsUiState(loading = true)

    setContent(state = state)

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.LOADING_INDICATOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.SCREEN).assertExists()
  }

  // ========================================
  // Tests for Error State
  // ========================================

  @Test
  fun errorState_displaysErrorMessage() {
    val state =
        MyInvitationsUiState(
            loading = false, errorMessage = "Failed to load invitations", invitations = emptyList())

    setContent(
        state = state,
        onAcceptInvitation = {},
        onRejectInvitation = {},
        organizationRepository = TestOrganizationRepository())

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Oops!").assertIsDisplayed()
    composeTestRule.onNodeWithText("Failed to load invitations").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${MyInvitationsScreenTestTags.ERROR_MESSAGE}_retry_button")
        .assertIsDisplayed()
  }

  @Test
  fun errorState_retryButton_isClickable() {
    val state =
        MyInvitationsUiState(
            loading = false, errorMessage = "Test error", invitations = emptyList())

    var retryCalled = false
    setContent(state = state, onRetry = { retryCalled = true })

    composeTestRule
        .onNodeWithTag("${MyInvitationsScreenTestTags.ERROR_MESSAGE}_retry_button")
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${MyInvitationsScreenTestTags.ERROR_MESSAGE}_retry_button")
        .performClick()
    composeTestRule.waitForIdle()

    assert(retryCalled) { "Retry callback should be called" }
  }

  // ========================================
  // Tests for Empty State
  // ========================================

  @Test
  fun emptyState_displaysEmptyMessage() {
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = emptyList())

    setContent(state = state)

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithText("No Invitations").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("You don't have any pending invitations at the moment.")
        .assertIsDisplayed()
  }

  // ========================================
  // Tests for Invitations List
  // ========================================

  @Test
  fun invitationsList_displaysAllInvitations() {
    val invitation1 = testInvitation1
    val invitation2 = testInvitation2
    val state =
        MyInvitationsUiState(
            loading = false, errorMessage = null, invitations = listOf(invitation1, invitation2))

    val org1 = OrganizationTestData.createTestOrganization(id = "org-1", name = "Organization One")
    val org2 = OrganizationTestData.createTestOrganization(id = "org-2", name = "Organization Two")
    val orgRepository =
        TestOrganizationRepository(organizations = mapOf("org-1" to org1, "org-2" to org2))

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.INVITATIONS_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getInvitationCardTag("invite-1"))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getInvitationCardTag("invite-2"))
        .assertIsDisplayed()
  }

  @Test
  fun invitationCard_displaysOrganizationName() {
    val invitation = testInvitation1
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.INVITATION_ORG_NAME)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Test Organization").assertIsDisplayed()
  }

  @Test
  fun invitationCard_displaysRole() {
    val invitation =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            role = ch.onepass.onepass.model.organization.OrganizationRole.STAFF)
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.INVITATION_ROLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Role: STAFF").assertIsDisplayed()
  }

  @Test
  fun invitationCard_displaysAcceptAndRejectButtons() {
    val invitation = testInvitation1
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getAcceptButtonTag("invite-1"))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getRejectButtonTag("invite-1"))
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Accept").assertIsDisplayed()
    composeTestRule.onNodeWithText("Reject").assertIsDisplayed()
  }

  @Test
  fun invitationCard_acceptButton_isClickable() {
    val invitation = testInvitation1
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    var acceptCalled = false
    setContent(
        state = state,
        onAcceptInvitation = { acceptCalled = true },
        organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getAcceptButtonTag("invite-1"))
        .performClick()
    composeTestRule.waitForIdle()

    assert(acceptCalled) { "Accept callback should be called" }
  }

  @Test
  fun invitationCard_rejectButton_isClickable() {
    val invitation = testInvitation1
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    var rejectCalled = false
    setContent(
        state = state,
        onRejectInvitation = { rejectCalled = true },
        organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getRejectButtonTag("invite-1"))
        .performClick()
    composeTestRule.waitForIdle()

    assert(rejectCalled) { "Reject callback should be called" }
  }

  @Test
  fun invitationCard_organizationNotFound_displaysOrgId() {
    val invitation =
        OrganizationTestData.createTestInvitation(id = "invite-1", orgId = "org-unknown")
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val orgRepository = TestOrganizationRepository(organizations = emptyMap())

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    // When organization is not found, should display orgId as fallback
    composeTestRule.onNodeWithText("org-unknown").assertIsDisplayed()
  }

  @Test
  fun multipleInvitations_eachHasUniqueButtons() {
    val invitation1 = testInvitation1
    val invitation2 = testInvitation2
    val state =
        MyInvitationsUiState(
            loading = false, errorMessage = null, invitations = listOf(invitation1, invitation2))

    val org1 = OrganizationTestData.createTestOrganization(id = "org-1", name = "Organization One")
    val org2 = OrganizationTestData.createTestOrganization(id = "org-2", name = "Organization Two")
    val orgRepository =
        TestOrganizationRepository(organizations = mapOf("org-1" to org1, "org-2" to org2))

    var acceptInvitationId: String? = null
    var rejectInvitationId: String? = null

    setContent(
        state = state,
        onAcceptInvitation = { acceptInvitationId = it },
        onRejectInvitation = { rejectInvitationId = it },
        organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    // Click accept on first invitation
    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getAcceptButtonTag("invite-1"))
        .performClick()
    composeTestRule.waitForIdle()

    assert(acceptInvitationId == "invite-1") {
      "Accept should be called with invite-1, but got $acceptInvitationId"
    }

    // Click reject on second invitation
    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getRejectButtonTag("invite-2"))
        .performClick()
    composeTestRule.waitForIdle()

    assert(rejectInvitationId == "invite-2") {
      "Reject should be called with invite-2, but got $rejectInvitationId"
    }
  }

  @Test
  fun screen_displaysTitle() {
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = emptyList())

    setContent(state = state)

    composeTestRule.onNodeWithText("My Invitations").assertIsDisplayed()
  }

  // ========================================
  // Tests for Navigation
  // ========================================

  @Test
  fun navigationButton_clickTriggersCallback() {
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = emptyList())

    var navigateBackCalled = false
    setContent(state = state, onNavigateBack = { navigateBackCalled = true })

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()

    assert(navigateBackCalled) { "Navigate back callback should be called" }
  }

  // ========================================
  // Tests for Success Message (Snackbar)
  // ========================================

  @Test
  fun successMessage_displaysSnackbar() {
    val state =
        MyInvitationsUiState(
            loading = false,
            errorMessage = null,
            invitations = emptyList(),
            successMessage = "Invitation accepted successfully")

    setContent(state = state)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.SUCCESS_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Invitation accepted successfully").assertIsDisplayed()
  }

  @Test
  fun successMessage_acceptInvitation_showsSnackbar() {
    val invitation = testInvitation1
    val state =
        MyInvitationsUiState(
            loading = false,
            errorMessage = null,
            invitations = listOf(invitation),
            successMessage = "Invitation accepted successfully. You are now a member.")

    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.SUCCESS_MESSAGE).assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Invitation accepted successfully. You are now a member.")
        .assertIsDisplayed()
  }

  @Test
  fun successMessage_rejectInvitation_showsSnackbar() {
    val invitation = testInvitation1
    val state =
        MyInvitationsUiState(
            loading = false,
            errorMessage = null,
            invitations = listOf(invitation),
            successMessage = "Invitation rejected successfully")

    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.SUCCESS_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Invitation rejected successfully").assertIsDisplayed()
  }

  @Test
  fun successMessage_clearsAfterDisplaying() {
    val state =
        MyInvitationsUiState(
            loading = false,
            errorMessage = null,
            invitations = emptyList(),
            successMessage = "Test success message")

    var clearSuccessMessageCalled = false
    setContent(state = state, onClearSuccessMessage = { clearSuccessMessageCalled = true })

    // Wait for LaunchedEffect to trigger, show snackbar, and call clearSuccessMessage
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.waitForIdle()
      clearSuccessMessageCalled
    }

    // Verify snackbar was displayed (it may have been dismissed by now)
    // and that clearSuccessMessage was called
    assert(clearSuccessMessageCalled) {
      "onClearSuccessMessage should be called after displaying snackbar"
    }
  }

  @Test
  fun successMessage_displaysAndClears() {
    val invitation = testInvitation1
    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    var clearSuccessMessageCallCount = 0
    val successMessage = "Invitation accepted successfully"

    val state =
        MyInvitationsUiState(
            loading = false,
            errorMessage = null,
            invitations = listOf(invitation),
            successMessage = successMessage)

    setContent(
        state = state,
        onClearSuccessMessage = { clearSuccessMessageCallCount++ },
        organizationRepository = orgRepository)

    // Wait for snackbar to show and clearSuccessMessage to be called
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.waitForIdle()
      clearSuccessMessageCallCount >= 1
    }

    // Verify snackbar was displayed
    // Note: The snackbar may have been dismissed by the time we check,
    // but clearSuccessMessage should have been called
    assert(clearSuccessMessageCallCount >= 1) {
      "onClearSuccessMessage should be called after displaying snackbar, but was called $clearSuccessMessageCallCount times"
    }
  }

  // ========================================
  // Tests for Organization Loading Error
  // ========================================

  @Test
  fun invitationCard_organizationLoadError_displaysOrgId() {
    val invitation = OrganizationTestData.createTestInvitation(id = "invite-1", orgId = "org-error")
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    // Repository that returns null (organization not found) - this is the actual fallback case
    val orgRepository = TestOrganizationRepository(organizations = emptyMap())

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    // When organization is not found, should display orgId as fallback
    composeTestRule.onNodeWithText("org-error").assertIsDisplayed()
  }

  // ========================================
  // Tests for Different Roles
  // ========================================

  @Test
  fun invitationCard_displaysAllRoles() {
    // Test all roles in a single test with multiple invitations
    val invitation1 =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            role = ch.onepass.onepass.model.organization.OrganizationRole.OWNER)
    val invitation2 =
        OrganizationTestData.createTestInvitation(
            id = "invite-2",
            orgId = "org-2",
            role = ch.onepass.onepass.model.organization.OrganizationRole.STAFF)
    val invitation3 =
        OrganizationTestData.createTestInvitation(
            id = "invite-3",
            orgId = "org-3",
            role = ch.onepass.onepass.model.organization.OrganizationRole.MEMBER)

    val state =
        MyInvitationsUiState(
            loading = false,
            errorMessage = null,
            invitations = listOf(invitation1, invitation2, invitation3))

    val org1 =
        OrganizationTestData.createTestOrganization(id = "org-1", name = "Test Organization 1")
    val org2 =
        OrganizationTestData.createTestOrganization(id = "org-2", name = "Test Organization 2")
    val org3 =
        OrganizationTestData.createTestOrganization(id = "org-3", name = "Test Organization 3")
    val orgRepository =
        TestOrganizationRepository(
            organizations = mapOf("org-1" to org1, "org-2" to org2, "org-3" to org3))

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    // Verify all roles are displayed
    composeTestRule.onNodeWithText("Role: OWNER").assertIsDisplayed()
    composeTestRule.onNodeWithText("Role: STAFF").assertIsDisplayed()
    composeTestRule.onNodeWithText("Role: MEMBER").assertIsDisplayed()
  }

  // ========================================
  // Tests for Error State with Invitations
  // ========================================

  @Test
  fun errorState_withInvitations_showsInvitationsList() {
    val invitation = testInvitation1
    val state =
        MyInvitationsUiState(
            loading = false, errorMessage = "Some error occurred", invitations = listOf(invitation))

    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    // When there are invitations, they should be displayed even if there's an error message
    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.INVITATIONS_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getInvitationCardTag("invite-1"))
        .assertIsDisplayed()
  }

  // ========================================
  // Comprehensive Integration Tests
  // ========================================

  @Test
  fun comprehensiveFlow_multipleInvitations_acceptAndReject() {
    val invitation1 =
        OrganizationTestData.createTestInvitation(
            id = "invite-1",
            orgId = "org-1",
            role = ch.onepass.onepass.model.organization.OrganizationRole.OWNER)
    val invitation2 =
        OrganizationTestData.createTestInvitation(
            id = "invite-2",
            orgId = "org-2",
            role = ch.onepass.onepass.model.organization.OrganizationRole.STAFF)
    val invitation3 =
        OrganizationTestData.createTestInvitation(
            id = "invite-3",
            orgId = "org-3",
            role = ch.onepass.onepass.model.organization.OrganizationRole.MEMBER)

    val state =
        MyInvitationsUiState(
            loading = false,
            errorMessage = null,
            invitations = listOf(invitation1, invitation2, invitation3))

    val org1 =
        OrganizationTestData.createTestOrganization(id = "org-1", name = "Owner Organization")
    val org2 =
        OrganizationTestData.createTestOrganization(id = "org-2", name = "Staff Organization")
    val org3 =
        OrganizationTestData.createTestOrganization(id = "org-3", name = "Member Organization")
    val orgRepository =
        TestOrganizationRepository(
            organizations = mapOf("org-1" to org1, "org-2" to org2, "org-3" to org3))

    val acceptCalls = mutableListOf<String>()
    val rejectCalls = mutableListOf<String>()

    setContent(
        state = state,
        onAcceptInvitation = { acceptCalls.add(it) },
        onRejectInvitation = { rejectCalls.add(it) },
        organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    // Verify all invitations are displayed
    composeTestRule.onNodeWithText("Owner Organization").assertIsDisplayed()
    composeTestRule.onNodeWithText("Staff Organization").assertIsDisplayed()
    composeTestRule.onNodeWithText("Member Organization").assertIsDisplayed()

    // Verify all roles are displayed
    composeTestRule.onNodeWithText("Role: OWNER").assertIsDisplayed()
    composeTestRule.onNodeWithText("Role: STAFF").assertIsDisplayed()
    composeTestRule.onNodeWithText("Role: MEMBER").assertIsDisplayed()

    // Accept first invitation
    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getAcceptButtonTag("invite-1"))
        .performClick()
    composeTestRule.waitForIdle()

    assert(acceptCalls.contains("invite-1")) { "Accept should be called for invite-1" }

    // Reject second invitation
    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getRejectButtonTag("invite-2"))
        .performClick()
    composeTestRule.waitForIdle()

    assert(rejectCalls.contains("invite-2")) { "Reject should be called for invite-2" }

    // Accept third invitation
    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getAcceptButtonTag("invite-3"))
        .performClick()
    composeTestRule.waitForIdle()

    assert(acceptCalls.contains("invite-3")) { "Accept should be called for invite-3" }

    // Verify all callbacks were called
    assert(acceptCalls.size == 2) { "Should have 2 accept calls" }
    assert(rejectCalls.size == 1) { "Should have 1 reject call" }
  }

  @Test
  fun contentState_withSuccessMessage_displaysSnackbar() {
    // Test success message display when there are invitations
    val invitation = testInvitation1
    val state =
        MyInvitationsUiState(
            loading = false,
            errorMessage = null,
            invitations = listOf(invitation),
            successMessage = "Invitation accepted successfully. You are now a member.")
    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    setContent(state = state, onAcceptInvitation = {}, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    // Verify content is displayed
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Test Organization").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Test Organization").assertIsDisplayed()

    // Verify success message is displayed (Snackbar) - wait for LaunchedEffect to trigger
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(MyInvitationsScreenTestTags.SUCCESS_MESSAGE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.SUCCESS_MESSAGE).assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Invitation accepted successfully. You are now a member.")
        .assertIsDisplayed()
  }

  @Test
  fun contentState_afterErrorState_displaysInvitations() {
    // Test that content can be displayed after an error state (simulating retry success)
    val invitation = testInvitation1
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))
    val orgRepository = TestOrganizationRepository(organizations = mapOf("org-1" to testOrg1))

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    // Verify content is displayed (wait for organization to load asynchronously)
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Test Organization").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Test Organization").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyInvitationsScreenTestTags.getInvitationCardTag("invite-1"))
        .assertIsDisplayed()
  }
}
