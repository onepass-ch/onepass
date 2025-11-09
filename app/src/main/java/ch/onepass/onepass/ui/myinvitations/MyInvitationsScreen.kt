package ch.onepass.onepass.ui.myinvitations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.ui.theme.OnePassTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/**
 * Test tags for MyInvitationsScreen components.
 *
 * These tags are used for UI testing to identify specific components on the screen.
 */
object MyInvitationsScreenTestTags {
  const val SCREEN = "my_invitations_screen"
  const val LOADING_INDICATOR = "my_invitations_loading"
  const val ERROR_MESSAGE = "my_invitations_error"
  const val RETRY_BUTTON = "my_invitations_retry"
  const val EMPTY_STATE = "my_invitations_empty"
  const val INVITATIONS_LIST = "my_invitations_list"
  const val INVITATION_CARD = "my_invitations_card"
  const val INVITATION_ORG_NAME = "my_invitations_org_name"
  const val INVITATION_ROLE = "my_invitations_role"
  const val ACCEPT_BUTTON = "my_invitations_accept"
  const val REJECT_BUTTON = "my_invitations_reject"

  /**
   * Generates a test tag for a specific invitation card.
   *
   * @param invitationId The unique identifier of the invitation.
   * @return A test tag string for the invitation card.
   */
  fun getInvitationCardTag(invitationId: String) = "invitation_card_$invitationId"

  /**
   * Generates a test tag for the accept button of a specific invitation.
   *
   * @param invitationId The unique identifier of the invitation.
   * @return A test tag string for the accept button.
   */
  fun getAcceptButtonTag(invitationId: String) = "accept_button_$invitationId"

  /**
   * Generates a test tag for the reject button of a specific invitation.
   *
   * @param invitationId The unique identifier of the invitation.
   * @return A test tag string for the reject button.
   */
  fun getRejectButtonTag(invitationId: String) = "reject_button_$invitationId"
}

/**
 * Main composable screen for displaying and managing organization invitations.
 *
 * This screen displays all pending invitations received by the current user. Each invitation shows
 * the organization name, role, and provides accept/reject actions.
 *
 * @param viewModel ViewModel instance for managing invitation state and operations. Defaults to a
 *   new instance created via viewModel().
 * @param organizationRepository Repository for fetching organization details. Defaults to
 *   OrganizationRepositoryFirebase().
 * @param onNavigateBack Callback invoked when the back button is clicked.
 * @param modifier Optional modifier for the screen layout.
 */
@Composable
fun MyInvitationsScreen(
    viewModel: MyInvitationsViewModel = viewModel(),
    organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
  val state by viewModel.state.collectAsState()

  MyInvitationsContent(
      state = state,
      onAcceptInvitation = viewModel::acceptInvitation,
      onRejectInvitation = viewModel::rejectInvitation,
      onNavigateBack = onNavigateBack,
      organizationRepository = organizationRepository,
      modifier = modifier)
}

/**
 * Content composable for the My Invitations screen.
 *
 * This composable handles the different UI states: loading, error, empty, and content.
 *
 * @param state Current UI state containing invitations, loading status, and error messages.
 * @param onAcceptInvitation Callback invoked when an invitation is accepted.
 * @param onRejectInvitation Callback invoked when an invitation is rejected.
 * @param onNavigateBack Callback invoked when the back button is clicked.
 * @param organizationRepository Repository for fetching organization details.
 * @param modifier Optional modifier for layout adjustments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyInvitationsContent(
    state: MyInvitationsUiState,
    onAcceptInvitation: (String) -> Unit,
    onRejectInvitation: (String) -> Unit,
    onNavigateBack: () -> Unit = {},
    organizationRepository: OrganizationRepository,
    modifier: Modifier = Modifier
) {
  Scaffold(
      modifier = modifier.fillMaxSize().testTag(MyInvitationsScreenTestTags.SCREEN),
      containerColor = Color(0xFF0A0A0A),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = "My Invitations",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.White)
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A)))
      }) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center) {
              when {
                state.loading -> {
                  LoadingState()
                }
                state.errorMessage != null && state.invitations.isEmpty() -> {
                  val errorMsg = state.errorMessage
                  ErrorState(
                      error = errorMsg, onRetry = { /* Retry logic can be added if needed */})
                }
                state.invitations.isEmpty() -> {
                  EmptyState()
                }
                else -> {
                  InvitationsList(
                      invitations = state.invitations,
                      onAcceptInvitation = onAcceptInvitation,
                      onRejectInvitation = onRejectInvitation,
                      organizationRepository = organizationRepository)
                }
              }
            }
      }
}

/** Displays a loading indicator while invitations are being fetched. */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  CircularProgressIndicator(
      modifier = modifier.testTag(MyInvitationsScreenTestTags.LOADING_INDICATOR),
      color = Color(0xFF9C6BFF))
}

/**
 * Displays an error state with a message and retry button.
 *
 * @param error The error message to display.
 * @param onRetry Callback invoked when the retry button is clicked.
 * @param modifier Optional modifier for layout adjustments.
 */
@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier =
          modifier.fillMaxWidth().padding(32.dp).testTag(MyInvitationsScreenTestTags.ERROR_MESSAGE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = "Oops!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag(MyInvitationsScreenTestTags.RETRY_BUTTON),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C6BFF), contentColor = Color.White)) {
              Text(text = "Try Again", fontWeight = FontWeight.Medium)
            }
      }
}

/**
 * Displays an empty state when no invitations are available.
 *
 * @param modifier Optional modifier for layout adjustments.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
  Column(
      modifier =
          modifier.fillMaxWidth().padding(32.dp).testTag(MyInvitationsScreenTestTags.EMPTY_STATE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = "No Invitations",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You don't have any pending invitations at the moment.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center)
      }
}

/**
 * Displays a list of invitations with organization details and action buttons.
 *
 * @param invitations List of pending invitations to display.
 * @param onAcceptInvitation Callback invoked when an invitation is accepted.
 * @param onRejectInvitation Callback invoked when an invitation is rejected.
 * @param organizationRepository Repository for fetching organization details.
 */
@Composable
private fun InvitationsList(
    invitations: List<OrganizationInvitation>,
    onAcceptInvitation: (String) -> Unit,
    onRejectInvitation: (String) -> Unit,
    organizationRepository: OrganizationRepository
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(MyInvitationsScreenTestTags.INVITATIONS_LIST),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items = invitations, key = { it.id }) { invitation ->
          InvitationCard(
              invitation = invitation,
              onAccept = { onAcceptInvitation(invitation.id) },
              onReject = { onRejectInvitation(invitation.id) },
              organizationRepository = organizationRepository)
        }
      }
}

/**
 * Displays a single invitation card with organization information and action buttons.
 *
 * @param invitation The invitation to display.
 * @param onAccept Callback invoked when the accept button is clicked.
 * @param onReject Callback invoked when the reject button is clicked.
 * @param organizationRepository Repository for fetching organization details.
 */
@Composable
private fun InvitationCard(
    invitation: OrganizationInvitation,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    organizationRepository: OrganizationRepository
) {
  // Fetch organization details for this invitation
  var organization by remember(invitation.orgId) { mutableStateOf<Organization?>(null) }
  var isLoadingOrg by remember(invitation.orgId) { mutableStateOf(true) }

  LaunchedEffect(invitation.orgId) {
    try {
      organization = organizationRepository.getOrganizationById(invitation.orgId).first()
      isLoadingOrg = false
    } catch (e: Exception) {
      isLoadingOrg = false
      // Organization not found or error - will show orgId as fallback
    }
  }

  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .testTag(MyInvitationsScreenTestTags.getInvitationCardTag(invitation.id)),
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF1B1B1B),
      tonalElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          // Organization name
          Text(
              text = organization?.name ?: invitation.orgId,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              modifier = Modifier.testTag(MyInvitationsScreenTestTags.INVITATION_ORG_NAME))

          Spacer(modifier = Modifier.height(8.dp))

          // Role information
          Text(
              text = "Role: ${invitation.role.name}",
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFF9CA3AF),
              modifier = Modifier.testTag(MyInvitationsScreenTestTags.INVITATION_ROLE))

          Spacer(modifier = Modifier.height(16.dp))

          // Action buttons
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Reject button
                Button(
                    onClick = onReject,
                    modifier =
                        Modifier.weight(1f)
                            .testTag(MyInvitationsScreenTestTags.getRejectButtonTag(invitation.id)),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A2A2A), contentColor = Color(0xFFD33A2C)),
                    shape = RoundedCornerShape(10.dp)) {
                      Text(text = "Reject", fontWeight = FontWeight.Medium)
                    }

                // Accept button
                Button(
                    onClick = onAccept,
                    modifier =
                        Modifier.weight(1f)
                            .testTag(MyInvitationsScreenTestTags.getAcceptButtonTag(invitation.id)),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C6BFF), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)) {
                      Text(text = "Accept", fontWeight = FontWeight.Medium)
                    }
              }
        }
      }
}

/**
 * Mock OrganizationRepository implementation for Preview purposes.
 *
 * This repository provides test data for previewing the My Invitations screen without
 * requiring actual Firebase connections.
 */
private class PreviewOrganizationRepository(
    private val organizations: Map<String, Organization> = emptyMap()
) : OrganizationRepository {
  override suspend fun createOrganization(organization: Organization): Result<String> =
      Result.success("org-id")

  override suspend fun updateOrganization(organization: Organization): Result<Unit> =
      Result.success(Unit)

  override suspend fun deleteOrganization(organizationId: String): Result<Unit> =
      Result.success(Unit)

  override fun getOrganizationById(organizationId: String): Flow<Organization?> =
      flowOf(organizations[organizationId])

  override fun getOrganizationsByOwner(ownerId: String): Flow<List<Organization>> =
      flowOf(emptyList())

  override fun getOrganizationsByMember(userId: String): Flow<List<Organization>> =
      flowOf(emptyList())

  override fun getOrganizationsByStatus(status: OrganizationStatus): Flow<List<Organization>> =
      flowOf(emptyList())

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

  override suspend fun deleteInvitation(invitationId: String): Result<Unit> =
      Result.success(Unit)
}

/**
 * Preview composable for My Invitations screen with sample data.
 *
 * This preview displays the screen with multiple invitations to test the UI layout,
 * organization name display, role information, and accept/reject buttons.
 */
@Preview(showBackground = true)
@Composable
fun MyInvitationsScreenPreview() {
  OnePassTheme {
    // Create test organizations
    val org1 =
        Organization(
            id = "org-1",
            name = "Tech Events Lausanne",
            description = "Organizing tech events in Lausanne",
            ownerId = "owner-1",
            status = OrganizationStatus.ACTIVE)

    val org2 =
        Organization(
            id = "org-2",
            name = "Music Festival Geneva",
            description = "Annual music festival in Geneva",
            ownerId = "owner-2",
            status = OrganizationStatus.ACTIVE)

    val org3 =
        Organization(
            id = "org-3",
            name = "Sports Club Zurich",
            description = "Community sports events",
            ownerId = "owner-3",
            status = OrganizationStatus.ACTIVE)

    // Create test invitations
    val invitation1 =
        OrganizationInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "user@example.com",
            role = OrganizationRole.MEMBER,
            invitedBy = "owner-1",
            status = InvitationStatus.PENDING)

    val invitation2 =
        OrganizationInvitation(
            id = "invite-2",
            orgId = "org-2",
            inviteeEmail = "user@example.com",
            role = OrganizationRole.STAFF,
            invitedBy = "owner-2",
            status = InvitationStatus.PENDING)

    val invitation3 =
        OrganizationInvitation(
            id = "invite-3",
            orgId = "org-3",
            inviteeEmail = "user@example.com",
            role = OrganizationRole.MEMBER,
            invitedBy = "owner-3",
            status = InvitationStatus.PENDING)

    // Create mock repository with organizations
    val orgRepository =
        PreviewOrganizationRepository(
            organizations = mapOf("org-1" to org1, "org-2" to org2, "org-3" to org3))

    // Create UI state with invitations
    val state =
        MyInvitationsUiState(
            invitations = listOf(invitation1, invitation2, invitation3),
            loading = false,
            errorMessage = null)

    MyInvitationsContent(
        state = state,
        onAcceptInvitation = { _ ->
          // Preview callback - can add logging or other actions here
        },
        onRejectInvitation = { _ ->
          // Preview callback - can add logging or other actions here
        },
        onNavigateBack = { /* Preview callback */ },
        organizationRepository = orgRepository)
  }
}

/**
 * Preview composable for empty state.
 *
 * This preview shows the screen when there are no pending invitations.
 */
@Preview(showBackground = true)
@Composable
fun MyInvitationsScreenEmptyPreview() {
  OnePassTheme {
    val state = MyInvitationsUiState(invitations = emptyList(), loading = false, errorMessage = null)

    MyInvitationsContent(
        state = state,
        onAcceptInvitation = {},
        onRejectInvitation = {},
        onNavigateBack = { /* Preview callback */ },
        organizationRepository = PreviewOrganizationRepository())
  }
}

/**
 * Preview composable for loading state.
 *
 * This preview shows the loading indicator while invitations are being fetched.
 */
@Preview(showBackground = true)
@Composable
fun MyInvitationsScreenLoadingPreview() {
  OnePassTheme {
    val state = MyInvitationsUiState(invitations = emptyList(), loading = true, errorMessage = null)

    MyInvitationsContent(
        state = state,
        onAcceptInvitation = {},
        onRejectInvitation = {},
        onNavigateBack = { /* Preview callback */ },
        organizationRepository = PreviewOrganizationRepository())
  }
}
