package ch.onepass.onepass.ui.myinvitations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.model.organization.*
import ch.onepass.onepass.ui.components.common.EmptyState
import ch.onepass.onepass.ui.components.common.ErrorState
import ch.onepass.onepass.ui.components.common.LoadingState
import kotlinx.coroutines.flow.first

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
  const val SUCCESS_MESSAGE = "my_invitations_success"

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
      onRetry = viewModel::retry,
      onClearSuccessMessage = viewModel::clearSuccessMessage,
      onNavigateBack = onNavigateBack,
      organizationRepository = organizationRepository,
      modifier = modifier)
}

/**
 * Content composable for the My Invitations screen.
 *
 * This composable handles the different UI states: loading, error, empty, and content.
 *
 * @param state Current UI state containing invitations, loading status, error messages, and success
 *   messages.
 * @param onAcceptInvitation Callback invoked when an invitation is accepted.
 * @param onRejectInvitation Callback invoked when an invitation is rejected.
 * @param onRetry Callback invoked when retry button is clicked to reload data.
 * @param onClearSuccessMessage Callback invoked to clear the success message after displaying it.
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
    onRetry: () -> Unit = {},
    onClearSuccessMessage: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    organizationRepository: OrganizationRepository,
    modifier: Modifier = Modifier
) {
  val snackbarHostState = remember { SnackbarHostState() }

  // Show success message as snackbar and clear it after displaying
  // This ensures that subsequent operations with the same success message will trigger
  // the snackbar to show again (since LaunchedEffect only triggers when the key changes).
  LaunchedEffect(state.successMessage) {
    state.successMessage?.let { message ->
      snackbarHostState.showSnackbar(
          message = message, duration = SnackbarDuration.Short, withDismissAction = true)
      // Clear the success message after displaying to allow the same message to be shown again
      // if the same operation is performed multiple times.
      onClearSuccessMessage()
    }
  }

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(MyInvitationsScreenTestTags.SCREEN),
      containerColor = colorResource(id = R.color.screen_background),
      snackbarHost = {
        SnackbarHost(
            hostState = snackbarHostState,
            snackbar = { snackbarData ->
              Snackbar(
                  snackbarData = snackbarData,
                  modifier = Modifier.testTag(MyInvitationsScreenTestTags.SUCCESS_MESSAGE),
                  containerColor = colorResource(id = R.color.myinvitations_success_green),
                  contentColor = colorResource(id = R.color.white))
            })
      },
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = "My Invitations",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = colorResource(id = R.color.white))
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorResource(id = R.color.white))
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.screen_background)))
      }) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center) {
              when {
                state.loading -> {
                  LoadingState(testTag = MyInvitationsScreenTestTags.LOADING_INDICATOR)
                }
                state.errorMessage != null && state.invitations.isEmpty() -> {
                  ErrorState(
                      error = state.errorMessage,
                      onRetry = onRetry,
                      testTag = MyInvitationsScreenTestTags.ERROR_MESSAGE)
                }
                state.invitations.isEmpty() -> {
                  EmptyState(
                      title = "No Invitations",
                      message = "You don't have any pending invitations at the moment.",
                      testTag = MyInvitationsScreenTestTags.EMPTY_STATE)
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
      color = colorResource(id = R.color.myinvitations_accent_purple))
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
            color = colorResource(id = R.color.white))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.gray),
            textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag(MyInvitationsScreenTestTags.RETRY_BUTTON),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.myinvitations_accent_purple),
                    contentColor = colorResource(id = R.color.white))) {
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
            color = colorResource(id = R.color.white))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You don't have any pending invitations at the moment.",
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.gray),
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
      // Error is logged in OrganizationRepositoryFirebase
    }
  }

  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .testTag(MyInvitationsScreenTestTags.getInvitationCardTag(invitation.id)),
      shape = RoundedCornerShape(16.dp),
      color = colorResource(id = R.color.myinvitations_card_background),
      tonalElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          // Organization name
          Text(
              text = organization?.name ?: invitation.orgId,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = colorResource(id = R.color.white),
              modifier = Modifier.testTag(MyInvitationsScreenTestTags.INVITATION_ORG_NAME))

          Spacer(modifier = Modifier.height(8.dp))

          // Role information
          Text(
              text = "Role: ${invitation.role.name}",
              style = MaterialTheme.typography.bodyMedium,
              color = colorResource(id = R.color.gray),
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
                            containerColor = colorResource(id = R.color.myinvitations_reject_bg),
                            contentColor = colorResource(id = R.color.myinvitations_reject_red)),
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
                            containerColor =
                                colorResource(id = R.color.myinvitations_accent_purple),
                            contentColor = colorResource(id = R.color.white)),
                    shape = RoundedCornerShape(10.dp)) {
                      Text(text = "Accept", fontWeight = FontWeight.Medium)
                    }
              }
        }
      }
}
