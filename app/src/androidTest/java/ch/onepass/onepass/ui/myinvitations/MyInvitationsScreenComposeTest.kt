package ch.onepass.onepass.ui.myinvitations

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.ui.theme.OnePassTheme
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
   * Mock implementation of OrganizationRepository for testing.
   *
   * This mock allows us to control the organization data returned for testing purposes.
   */
  private class MockOrganizationRepository(
      private val organizations: Map<String, Organization> = emptyMap()
  ) : OrganizationRepository {
    override suspend fun createOrganization(organization: Organization): Result<String> =
        Result.success("org-id")

    override suspend fun updateOrganization(organization: Organization): Result<Unit> =
        Result.success(Unit)

    override suspend fun deleteOrganization(organizationId: String): Result<Unit> =
        Result.success(Unit)

    override fun getOrganizationById(organizationId: String): Flow<Organization?> {
      return flowOf(organizations[organizationId])
    }

    override fun getOrganizationsByOwner(ownerId: String): Flow<List<Organization>> =
        flowOf(emptyList())

    override fun getOrganizationsByMember(userId: String): Flow<List<Organization>> =
        flowOf(emptyList())

    override fun getOrganizationsByStatus(
        status: ch.onepass.onepass.model.organization.OrganizationStatus
    ): Flow<List<Organization>> = flowOf(emptyList())

    override fun searchOrganizations(query: String): Flow<List<Organization>> = flowOf(emptyList())

    override fun getVerifiedOrganizations(): Flow<List<Organization>> = flowOf(emptyList())

    override suspend fun addMember(
        organizationId: String,
        userId: String,
        role: OrganizationRole
    ): Result<Unit> = Result.success(Unit)

    override suspend fun removeMember(organizationId: String, userId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun updateMemberRole(
        organizationId: String,
        userId: String,
        newRole: OrganizationRole
    ): Result<Unit> = Result.success(Unit)

    override suspend fun createInvitation(invitation: OrganizationInvitation): Result<String> =
        Result.success("invite-id")

    override fun getPendingInvitations(organizationId: String): Flow<List<OrganizationInvitation>> =
        flowOf(emptyList())

    override fun getInvitationsByEmail(email: String): Flow<List<OrganizationInvitation>> =
        flowOf(emptyList())

    override suspend fun updateInvitationStatus(
        invitationId: String,
        newStatus: InvitationStatus
    ): Result<Unit> = Result.success(Unit)

    override suspend fun deleteInvitation(invitationId: String): Result<Unit> = Result.success(Unit)
  }

  /**
   * Creates a test invitation with the specified parameters.
   *
   * @param id Unique identifier for the invitation.
   * @param orgId Organization ID associated with the invitation.
   * @param inviteeEmail Email of the invited user.
   * @param role Role to be assigned upon acceptance.
   * @return A test OrganizationInvitation instance.
   */
  private fun createTestInvitation(
      id: String,
      orgId: String,
      inviteeEmail: String = "test@example.com",
      role: OrganizationRole = OrganizationRole.MEMBER
  ): OrganizationInvitation {
    return OrganizationInvitation(
        id = id,
        orgId = orgId,
        inviteeEmail = inviteeEmail,
        role = role,
        invitedBy = "owner-1",
        status = InvitationStatus.PENDING)
  }

  /**
   * Creates a test organization with the specified parameters.
   *
   * @param id Unique identifier for the organization.
   * @param name Name of the organization.
   * @return A test Organization instance.
   */
  private fun createTestOrganization(id: String, name: String): Organization {
    return Organization(
        id = id,
        name = name,
        description = "Test organization",
        ownerId = "owner-1",
        status = ch.onepass.onepass.model.organization.OrganizationStatus.ACTIVE)
  }

  /**
   * Sets up the compose content with the given state and callbacks.
   *
   * @param state UI state to display.
   * @param onAcceptInvitation Callback for accepting invitations.
   * @param onRejectInvitation Callback for rejecting invitations.
   * @param organizationRepository Repository for fetching organization details.
   */
  private fun setContent(
      state: MyInvitationsUiState,
      onAcceptInvitation: (String) -> Unit = {},
      onRejectInvitation: (String) -> Unit = {},
      organizationRepository: OrganizationRepository = MockOrganizationRepository()
  ) {
    composeTestRule.setContent {
      OnePassTheme {
        MyInvitationsContent(
            state = state,
            onAcceptInvitation = onAcceptInvitation,
            onRejectInvitation = onRejectInvitation,
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
        organizationRepository = MockOrganizationRepository())

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Oops!").assertIsDisplayed()
    composeTestRule.onNodeWithText("Failed to load invitations").assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.RETRY_BUTTON).assertIsDisplayed()
  }

  @Test
  fun errorState_retryButton_isClickable() {
    val state =
        MyInvitationsUiState(
            loading = false, errorMessage = "Test error", invitations = emptyList())

    setContent(state = state)

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.RETRY_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.RETRY_BUTTON).performClick()
    composeTestRule.waitForIdle()
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
    val invitation1 = createTestInvitation(id = "invite-1", orgId = "org-1")
    val invitation2 = createTestInvitation(id = "invite-2", orgId = "org-2")
    val state =
        MyInvitationsUiState(
            loading = false, errorMessage = null, invitations = listOf(invitation1, invitation2))

    val org1 = createTestOrganization(id = "org-1", name = "Organization One")
    val org2 = createTestOrganization(id = "org-2", name = "Organization Two")
    val orgRepository =
        MockOrganizationRepository(organizations = mapOf("org-1" to org1, "org-2" to org2))

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
    val invitation = createTestInvitation(id = "invite-1", orgId = "org-1")
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val organization = createTestOrganization(id = "org-1", name = "Test Organization")
    val orgRepository = MockOrganizationRepository(organizations = mapOf("org-1" to organization))

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
        createTestInvitation(id = "invite-1", orgId = "org-1", role = OrganizationRole.STAFF)
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val organization = createTestOrganization(id = "org-1", name = "Test Organization")
    val orgRepository = MockOrganizationRepository(organizations = mapOf("org-1" to organization))

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.INVITATION_ROLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Role: STAFF").assertIsDisplayed()
  }

  @Test
  fun invitationCard_displaysAcceptAndRejectButtons() {
    val invitation = createTestInvitation(id = "invite-1", orgId = "org-1")
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val organization = createTestOrganization(id = "org-1", name = "Test Organization")
    val orgRepository = MockOrganizationRepository(organizations = mapOf("org-1" to organization))

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
    val invitation = createTestInvitation(id = "invite-1", orgId = "org-1")
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val organization = createTestOrganization(id = "org-1", name = "Test Organization")
    val orgRepository = MockOrganizationRepository(organizations = mapOf("org-1" to organization))

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
    val invitation = createTestInvitation(id = "invite-1", orgId = "org-1")
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val organization = createTestOrganization(id = "org-1", name = "Test Organization")
    val orgRepository = MockOrganizationRepository(organizations = mapOf("org-1" to organization))

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
    val invitation = createTestInvitation(id = "invite-1", orgId = "org-unknown")
    val state =
        MyInvitationsUiState(loading = false, errorMessage = null, invitations = listOf(invitation))

    val orgRepository = MockOrganizationRepository(organizations = emptyMap())

    setContent(state = state, organizationRepository = orgRepository)

    composeTestRule.waitForIdle()

    // When organization is not found, should display orgId as fallback
    composeTestRule.onNodeWithText("org-unknown").assertIsDisplayed()
  }

  @Test
  fun multipleInvitations_eachHasUniqueButtons() {
    val invitation1 = createTestInvitation(id = "invite-1", orgId = "org-1")
    val invitation2 = createTestInvitation(id = "invite-2", orgId = "org-2")
    val state =
        MyInvitationsUiState(
            loading = false, errorMessage = null, invitations = listOf(invitation1, invitation2))

    val org1 = createTestOrganization(id = "org-1", name = "Organization One")
    val org2 = createTestOrganization(id = "org-2", name = "Organization Two")
    val orgRepository =
        MockOrganizationRepository(organizations = mapOf("org-1" to org1, "org-2" to org2))

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
}
