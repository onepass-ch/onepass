package ch.onepass.onepass.ui.staff

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.model.user.UserSearchType
import ch.onepass.onepass.ui.components.common.EmptyState
import ch.onepass.onepass.ui.components.common.LoadingState
import ch.onepass.onepass.ui.navigation.BackNavigationScaffold
import ch.onepass.onepass.ui.navigation.TopBarConfig
import kotlinx.coroutines.launch

object StaffInvitationTestTags {
  const val SCREEN = "staffInvitation_screen"
  const val TOP_BAR = "staffInvitation_topBar"
  const val BACK_BUTTON = "staffInvitation_backButton"
  const val TITLE = "staffInvitation_title"
  const val TAB_ROW = "staffInvitation_tabRow"
  const val TAB_DISPLAY_NAME = "staffInvitation_tab_displayName"
  const val TAB_EMAIL = "staffInvitation_tab_email"
  const val SEARCH_FIELD = "staffInvitation_searchField"
  const val RESULTS_LIST = "staffInvitation_resultsList"
  const val EMPTY_STATE = "staffInvitation_emptyState"
  const val LOADING_INDICATOR = "staffInvitation_loadingIndicator"
  const val ERROR_MESSAGE = "staffInvitation_errorMessage"
  const val CONFIRMATION_DIALOG = "staffInvitation_confirmationDialog"
  const val ROLE_DROPDOWN = "staffInvitation_roleDropdown"
  const val CONFIRM_BUTTON = "staffInvitation_confirmButton"
  const val CANCEL_BUTTON = "staffInvitation_cancelButton"
  const val PERMISSION_DENIED_DIALOG = "staffInvitation_permissionDeniedDialog"
  const val INVITATION_RESULT_DIALOG = "staffInvitation_invitationResultDialog"
}

/**
 * Screen for inviting staff members to an organization.
 *
 * @param viewModel ViewModel for managing screen state.
 * @param onNavigateBack Callback to navigate back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffInvitationScreen(
    viewModel: StaffInvitationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
  val uiState = viewModel.uiState.collectAsState().value
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { message ->
      scope.launch {
        snackbarHostState.showSnackbar(
            message = message, duration = SnackbarDuration.Short, withDismissAction = true)
      }
      viewModel.clearSnackbarMessage()
    }
  }

  BackNavigationScaffold(
      TopBarConfig(
          title = stringResource(R.string.staff_invitation_title),
          topBarTestTag = StaffInvitationTestTags.TOP_BAR,
          backButtonTestTag = StaffInvitationTestTags.BACK_BUTTON,
          titleTestTag = StaffInvitationTestTags.TITLE),
      onBack = onNavigateBack,
      modifier = modifier.testTag(StaffInvitationTestTags.SCREEN)) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
              StaffInvitationTabRow(
                  selectedTab = uiState.selectedTab, onTabSelected = viewModel::selectTab)

              StaffInvitationContent(
                  uiState = uiState,
                  onQueryChange = viewModel::updateSearchQuery,
                  onUserSelected = viewModel::onUserSelected)
            }
      }

  StaffInvitationDialogs(uiState = uiState, viewModel = viewModel)

  // Snackbar Host
  Box(modifier = Modifier.fillMaxSize()) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) { data ->
          androidx.compose.material3.Snackbar(
              snackbarData = data,
              containerColor = colorScheme.surface,
              contentColor = colorScheme.onSurface,
              actionColor = colorScheme.primary,
              dismissActionContentColor = colorScheme.onSurface)
        }
  }
}

@Composable
private fun StaffInvitationTabRow(
    selectedTab: UserSearchType,
    onTabSelected: (UserSearchType) -> Unit
) {
  val selectedTabIndex = if (selectedTab == UserSearchType.DISPLAY_NAME) 0 else 1

  TabRow(
      modifier = Modifier.fillMaxWidth().testTag(StaffInvitationTestTags.TAB_ROW),
      selectedTabIndex = selectedTabIndex,
      containerColor = colorScheme.surface,
      contentColor = colorScheme.onSurface,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
            height = 4.dp,
            color = colorScheme.primary)
      }) {
        Tab(
            text = {
              Text(
                  text = stringResource(R.string.staff_invitation_tab_username),
                  style = MaterialTheme.typography.titleMedium,
                  color =
                      if (selectedTab == UserSearchType.DISPLAY_NAME) colorScheme.primary
                      else colorScheme.outline)
            },
            selected = selectedTab == UserSearchType.DISPLAY_NAME,
            onClick = { onTabSelected(UserSearchType.DISPLAY_NAME) },
            modifier = Modifier.testTag(StaffInvitationTestTags.TAB_DISPLAY_NAME))
        Tab(
            text = {
              Text(
                  text = stringResource(R.string.staff_invitation_tab_email),
                  style = MaterialTheme.typography.titleMedium,
                  color =
                      if (selectedTab == UserSearchType.EMAIL) colorScheme.primary
                      else colorScheme.outline)
            },
            selected = selectedTab == UserSearchType.EMAIL,
            onClick = { onTabSelected(UserSearchType.EMAIL) },
            modifier = Modifier.testTag(StaffInvitationTestTags.TAB_EMAIL))
      }
}

@Composable
private fun StaffInvitationContent(
    uiState: StaffInvitationUiState,
    onQueryChange: (String) -> Unit,
    onUserSelected: (StaffSearchResult) -> Unit
) {
  // Search Input Field
  SearchInputField(
      value = uiState.searchQuery,
      onValueChange = onQueryChange,
      placeholder =
          if (uiState.selectedTab == UserSearchType.DISPLAY_NAME)
              stringResource(R.string.staff_invitation_search_by_name)
          else stringResource(R.string.staff_invitation_search_by_email),
      modifier = Modifier.fillMaxWidth().padding(top = 16.dp))

  // Error Message
  uiState.errorMessage?.let { error ->
    Text(
        text = error,
        color = colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier =
            Modifier.fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(StaffInvitationTestTags.ERROR_MESSAGE))
  }

  // Results List or Empty State
  Box(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
    when {
      uiState.isLoading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          LoadingState(testTag = StaffInvitationTestTags.LOADING_INDICATOR)
        }
      }
      uiState.searchQuery.isBlank() -> {
        EmptyState(
            title = stringResource(R.string.staff_invitation_search_users),
            message = stringResource(R.string.staff_invitation_enter_query),
            modifier = Modifier.fillMaxSize(),
            testTag = StaffInvitationTestTags.EMPTY_STATE)
      }
      uiState.searchResults.isEmpty() -> {
        EmptyState(
            title = stringResource(R.string.staff_invitation_no_users),
            message = stringResource(R.string.staff_invitation_try_different),
            modifier = Modifier.fillMaxSize(),
            testTag = StaffInvitationTestTags.EMPTY_STATE)
      }
      else -> {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag(StaffInvitationTestTags.RESULTS_LIST),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(items = uiState.searchResults, key = { it.id }) { user ->
                val isInvited = user.id in uiState.invitedUserIds
                val isAlreadyInvited = user.id in uiState.alreadyInvitedUserIds

                StaffListItem(
                    user = user,
                    onClick =
                        if (isInvited || isAlreadyInvited) null
                        else {
                          { onUserSelected(user) }
                        },
                    enabled = !(isInvited || isAlreadyInvited),
                    modifier = Modifier.fillMaxWidth())
              }
            }
      }
    }
  }
}

@Composable
private fun StaffInvitationDialogs(
    uiState: StaffInvitationUiState,
    viewModel: StaffInvitationViewModel
) {
  // Confirmation Dialog
  if (uiState.selectedUserForInvite != null) {
    StaffInvitationDialog(
        user = uiState.selectedUserForInvite,
        selectedRole = uiState.selectedRole,
        isInviting = uiState.isInviting,
        availableRoles = viewModel.getAvailableRoles(),
        onRoleSelected = viewModel::selectRole,
        onConfirm = viewModel::confirmInvitation,
        onCancel = viewModel::cancelInvitation)
  }

  // Permission Denied Dialog
  if (uiState.showPermissionDeniedDialog) {
    PermissionDeniedDialog(onDismiss = viewModel::dismissPermissionDeniedDialog)
  }

  // Invitation Result Dialog
  if (uiState.invitationResultMessage != null && uiState.invitationResultType != null) {
    InvitationResultDialog(
        userName = uiState.invitationResultMessage,
        resultType = uiState.invitationResultType,
        onDismiss = viewModel::dismissInvitationResultDialog)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffInvitationDialog(
    user: StaffSearchResult,
    selectedRole: OrganizationRole,
    isInviting: Boolean,
    availableRoles: List<OrganizationRole>,
    onRoleSelected: (OrganizationRole) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  AlertDialog(
      onDismissRequest = { if (!isInviting) onCancel() },
      title = { Text(text = stringResource(R.string.staff_invitation_dialog_title)) },
      text = {
        Column {
          Text(text = stringResource(R.string.staff_invitation_dialog_message, user.displayName))
          Spacer(modifier = Modifier.height(16.dp))

          Text(
              text = stringResource(R.string.staff_invitation_select_role),
              style = MaterialTheme.typography.labelMedium,
              color = colorScheme.onBackground)
          Spacer(modifier = Modifier.height(8.dp))

          ExposedDropdownMenuBox(
              expanded = expanded,
              onExpandedChange = { if (!isInviting) expanded = !expanded },
              modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = selectedRole.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                      ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier =
                        Modifier.menuAnchor(
                                androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                            .testTag(StaffInvitationTestTags.ROLE_DROPDOWN),
                    colors =
                        ExposedDropdownMenuDefaults.textFieldColors(
                            focusedIndicatorColor = colorScheme.primary,
                            focusedLabelColor = colorScheme.primary,
                            cursorColor = colorScheme.primary))

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                  availableRoles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.name) },
                        onClick = {
                          onRoleSelected(role)
                          expanded = false
                        })
                  }
                }
              }
        }
      },
      confirmButton = {
        Button(
            onClick = onConfirm,
            enabled = !isInviting,
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
            modifier = Modifier.testTag(StaffInvitationTestTags.CONFIRM_BUTTON)) {
              if (isInviting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colorScheme.onBackground,
                    strokeWidth = 2.dp)
              } else {
                Text(stringResource(R.string.staff_invitation_invite_button))
              }
            }
      },
      dismissButton = {
        TextButton(
            onClick = onCancel,
            enabled = !isInviting,
            colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.primary),
            modifier = Modifier.testTag(StaffInvitationTestTags.CANCEL_BUTTON)) {
              Text(stringResource(R.string.staff_invitation_cancel_button))
            }
      },
      modifier = Modifier.testTag(StaffInvitationTestTags.CONFIRMATION_DIALOG),
      shape = RoundedCornerShape(10.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
  TextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = {
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.outline))
      },
      modifier =
          modifier
              .fillMaxWidth()
              .border(1.dp, colorScheme.onSurface, RoundedCornerShape(10.dp))
              .heightIn(min = 50.dp)
              .testTag(StaffInvitationTestTags.SEARCH_FIELD),
      colors =
          TextFieldDefaults.colors(
              focusedContainerColor = colorScheme.surface,
              unfocusedContainerColor = colorScheme.surface,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              focusedTextColor = colorScheme.onBackground,
              unfocusedTextColor = colorScheme.onBackground,
          ),
      shape = RoundedCornerShape(10.dp),
      textStyle = MaterialTheme.typography.bodySmall,
      singleLine = true)
}

/** Dialog shown when user doesn't have permission to invite members. */
@Composable
fun PermissionDeniedDialog(onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(text = stringResource(R.string.staff_invitation_permission_denied_title)) },
      text = { Text(text = stringResource(R.string.staff_invitation_permission_denied_message)) },
      confirmButton = {
        Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
          Text(stringResource(R.string.staff_invitation_permission_denied_ok))
        }
      },
      modifier = Modifier.testTag(StaffInvitationTestTags.PERMISSION_DENIED_DIALOG),
      shape = RoundedCornerShape(10.dp))
}

/** Dialog shown after invitation attempt (success or error). */
@Composable
fun InvitationResultDialog(
    userName: String,
    resultType: InvitationResultType,
    onDismiss: () -> Unit
) {
  val (title, message, buttonColor) =
      when (resultType) {
        InvitationResultType.SUCCESS ->
            Triple(
                stringResource(R.string.staff_invitation_success_title),
                stringResource(R.string.staff_invitation_success_message, userName),
                colorScheme.primary)
        InvitationResultType.ERROR ->
            Triple(
                stringResource(R.string.staff_invitation_error_title),
                stringResource(R.string.staff_invitation_error_message, userName),
                colorScheme.error)
      }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(text = title) },
      text = { Text(text = message) },
      confirmButton = {
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(10.dp)) {
              Text(stringResource(R.string.staff_invitation_result_ok))
            }
      },
      modifier = Modifier.testTag(StaffInvitationTestTags.INVITATION_RESULT_DIALOG),
      shape = RoundedCornerShape(10.dp))
}
