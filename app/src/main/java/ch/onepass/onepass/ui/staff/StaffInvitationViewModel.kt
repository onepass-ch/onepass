package ch.onepass.onepass.ui.staff

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.membership.MembershipRepository
import ch.onepass.onepass.model.membership.MembershipRepositoryFirebase
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import ch.onepass.onepass.model.user.UserSearchType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * UI state for the staff invitation screen.
 *
 * @property selectedTab The currently selected search tab (DISPLAY_NAME or EMAIL).
 * @property searchQuery The current search query input by the user.
 * @property searchResults The list of users matching the search query.
 * @property isLoading Whether a search is currently in progress.
 * @property isInviting Whether an invitation is currently being sent.
 * @property errorMessage Error message to display, null if no error.
 * @property invitedUserIds Set of user IDs that have been successfully invited in this session.
 * @property alreadyInvitedUserIds Set of user IDs that were already invited before this session.
 * @property currentUserRole The role of the current user in this organization.
 * @property showPermissionDeniedDialog Whether to show the permission denied dialog.
 * @property invitationResultMessage Message to display in the invitation result dialog.
 * @property invitationResultType Type of invitation result (success or error).
 */
data class StaffInvitationUiState(
    val selectedTab: UserSearchType = UserSearchType.DISPLAY_NAME,
    val searchQuery: String = "",
    val searchResults: List<StaffSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val isInviting: Boolean = false,
    val errorMessage: String? = null,
    val invitedUserIds: Set<String> = emptySet(),
    val alreadyInvitedUserIds: Set<String> = emptySet(),
    val selectedUserForInvite: StaffSearchResult? = null,
    val selectedRole: OrganizationRole = OrganizationRole.STAFF,
    val currentUserRole: OrganizationRole? = null,
    val showPermissionDeniedDialog: Boolean = false,
    val invitationResultMessage: String? = null,
    val invitationResultType: InvitationResultType? = null,
    val snackbarMessage: String? = null
)

/** Type of invitation result. */
enum class InvitationResultType {
  SUCCESS,
  ERROR
}

/**
 * Result of an invitation attempt.
 *
 * @property Success Whether the invitation was successfully created.
 * @property AlreadyInvited Whether the user was already invited (not an error, just informational).
 * @property Error Error message if the invitation failed.
 */
sealed class InvitationResult {
  data object Success : InvitationResult()

  data class AlreadyInvited(val message: String) : InvitationResult()

  data class Error(val message: String) : InvitationResult()
}

/**
 * ViewModel for managing staff invitation screen state and business logic.
 *
 * Responsibilities:
 * - Managing search tab selection (display name vs email)
 * - Handling user search with debouncing for lazy loading
 * - Checking if users are already invited
 * - Creating new invitations
 * - Managing UI state and error handling
 *
 * @param organizationId The ID of the organization to invite users to.
 * @param userRepository Repository for user-related operations.
 * @param organizationRepository Repository for organization-related operations.
 */
class StaffInvitationViewModel(
    private val organizationId: String,
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val membershipRepository: MembershipRepository = MembershipRepositoryFirebase()
) : ViewModel() {

  private val _uiState = MutableStateFlow(StaffInvitationUiState())
  val uiState: StateFlow<StaffInvitationUiState> = _uiState.asStateFlow()

  private var searchJob: Job? = null
  private var currentUserId: String? = null

  init {
    loadCurrentUserId()
    loadCurrentUserRole()
  }

  /** Loads the current user ID for creating invitations. */
  private fun loadCurrentUserId() {
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser() ?: userRepository.getOrCreateUser()
        currentUserId = user?.uid
        if (currentUserId == null) {
          _uiState.value =
              _uiState.value.copy(errorMessage = "User not found. Please log in and try again.")
        }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                errorMessage = "Failed to load user: ${e.message ?: "Unknown error"}")
      }
    }
  }

  /** Loads the current user's role in this organization. */
  private fun loadCurrentUserRole() {
    viewModelScope.launch {
      try {
        val userId = currentUserId ?: userRepository.getCurrentUser()?.uid ?: return@launch
        val memberships =
            membershipRepository.getUsersByOrganization(organizationId).getOrNull() ?: emptyList()
        val membership = memberships.find { it.userId == userId }
        _uiState.value = _uiState.value.copy(currentUserRole = membership?.role)
      } catch (e: Exception) {
        Log.e("StaffInvitationVM", "Failed to load user role", e)
      }
    }
  }

  /**
   * Updates the selected search tab.
   *
   * @param searchType The search type to select (DISPLAY_NAME or EMAIL).
   */
  fun selectTab(searchType: UserSearchType) {
    if (_uiState.value.selectedTab != searchType) {
      _uiState.value =
          _uiState.value.copy(
              selectedTab = searchType,
              searchQuery = "",
              searchResults = emptyList(),
              errorMessage = null)
    }
  }

  /**
   * Updates the search query and triggers a debounced search.
   *
   * @param query The search query string.
   */
  fun updateSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query, errorMessage = null)

    // Cancel previous search job
    searchJob?.cancel()

    // Clear results if query is empty
    if (query.isBlank()) {
      _uiState.value = _uiState.value.copy(searchResults = emptyList(), isLoading = false)
      return
    }

    // Start debounced search
    searchJob =
        viewModelScope.launch {
          delay(SEARCH_DEBOUNCE_MS)
          performSearch(query.trim())
        }
  }

  /**
   * Performs the actual search operation.
   *
   * @param query The search query (already trimmed).
   */
  private suspend fun performSearch(query: String) {
    if (query.isBlank()) return

    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

    try {
      val result =
          userRepository.searchUsers(
              query = query,
              searchType = _uiState.value.selectedTab,
              organizationId = organizationId)

      result.fold(
          onSuccess = { users ->
            _uiState.value =
                _uiState.value.copy(searchResults = users, isLoading = false, errorMessage = null)
          },
          onFailure = { error ->
            _uiState.value =
                _uiState.value.copy(
                    searchResults = emptyList(),
                    isLoading = false,
                    errorMessage = "Search failed: ${error.message ?: "Unknown error"}")
          })
    } catch (e: Exception) {
      _uiState.value =
          _uiState.value.copy(
              searchResults = emptyList(),
              isLoading = false,
              errorMessage = "Search error: ${e.message ?: "Unknown error"}")
    }
  }

  /**
   * Handles user selection and attempts to send an invitation.
   *
   * This method:
   * 1. Checks if the user is already invited to this organization
   * 2. If not invited, creates a new invitation
   * 3. Updates UI state accordingly
   *
   * @param user The user to invite.
   */
  fun onUserSelected(user: StaffSearchResult) {
    if (currentUserId == null) {
      _uiState.value = _uiState.value.copy(errorMessage = "User not authenticated. Please log in.")
      return
    }

    // Check if user is already invited in this session
    if (_uiState.value.invitedUserIds.contains(user.id)) {
      _uiState.value =
          _uiState.value.copy(snackbarMessage = "${user.displayName} has already been invited.")
      return
    }

    // Check if user has permission to invite
    val currentRole = _uiState.value.currentUserRole
    if (currentRole != OrganizationRole.OWNER && currentRole != OrganizationRole.ADMIN) {
      _uiState.value = _uiState.value.copy(showPermissionDeniedDialog = true)
      return
    }

    viewModelScope.launch {
      try {
        // Check if user is already invited or a member
        val existingInvitations = organizationRepository.getInvitationsByEmail(user.email).first()

        val hasPendingInvitation =
            existingInvitations.any {
              it.orgId == organizationId && it.status == InvitationStatus.PENDING
            }

        if (hasPendingInvitation) {
          _uiState.value =
              _uiState.value.copy(snackbarMessage = "${user.displayName} has already been invited.")
          return@launch
        }

        val hasAcceptedInvitation =
            existingInvitations.any {
              it.orgId == organizationId && it.status == InvitationStatus.ACCEPTED
            }

        if (hasAcceptedInvitation) {
          _uiState.value =
              _uiState.value.copy(snackbarMessage = "${user.displayName} is already a member.")
          return@launch
        }

        // Instead of inviting immediately, we set the user for confirmation
        _uiState.value =
            _uiState.value.copy(
                selectedUserForInvite = user,
                selectedRole = OrganizationRole.STAFF, // Default role
                errorMessage = null)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                errorMessage = "Failed to check invitation status: ${e.message ?: "Unknown error"}")
      }
    }
  }

  /**
   * Updates the selected role for the pending invitation.
   *
   * @param role The new role to select.
   */
  fun selectRole(role: OrganizationRole) {
    _uiState.value = _uiState.value.copy(selectedRole = role)
  }

  /** Cancels the pending invitation. */
  fun cancelInvitation() {
    _uiState.value = _uiState.value.copy(selectedUserForInvite = null, errorMessage = null)
  }

  /** Confirms and sends the invitation to the selected user with the selected role. */
  fun confirmInvitation() {
    val user = _uiState.value.selectedUserForInvite ?: return
    val role = _uiState.value.selectedRole

    if (_uiState.value.isInviting || _uiState.value.invitedUserIds.contains(user.id)) return

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isInviting = true, errorMessage = null)

      try {
        val invitationResult = checkAndCreateInvitation(user, role)

        when (invitationResult) {
          is InvitationResult.Success -> {
            _uiState.value =
                _uiState.value.copy(
                    isInviting = false,
                    invitedUserIds = _uiState.value.invitedUserIds + user.id,
                    selectedUserForInvite = null, // Close dialog on success
                    invitationResultMessage = user.displayName,
                    invitationResultType = InvitationResultType.SUCCESS)
          }
          is InvitationResult.AlreadyInvited -> {
            _uiState.value =
                _uiState.value.copy(
                    isInviting = false,
                    alreadyInvitedUserIds = _uiState.value.alreadyInvitedUserIds + user.id,
                    errorMessage = invitationResult.message,
                    selectedUserForInvite = null) // Close dialog on already invited
          }
          is InvitationResult.Error -> {
            _uiState.value =
                _uiState.value.copy(
                    isInviting = false,
                    selectedUserForInvite = null, // Close confirmation dialog
                    invitationResultMessage = invitationResult.message,
                    invitationResultType = InvitationResultType.ERROR)
          }
        }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isInviting = false,
                errorMessage = "Failed to send invitation: ${e.message ?: "Unknown error"}")
      }
    }
  }

  /**
   * Checks if a user is already invited and creates an invitation if not.
   *
   * @param user The user to check and invite.
   * @return The result of the invitation attempt.
   */
  private suspend fun checkAndCreateInvitation(
      user: StaffSearchResult,
      role: OrganizationRole
  ): InvitationResult {
    if (user.id == currentUserId) {
      return InvitationResult.Error("You cannot invite yourself.")
    }

    // Check if user is already invited to this organization
    val existingInvitations = organizationRepository.getInvitationsByEmail(user.email).first()

    val hasPendingInvitation =
        existingInvitations.any {
          it.orgId == organizationId && it.status == InvitationStatus.PENDING
        }

    if (hasPendingInvitation) {
      return InvitationResult.AlreadyInvited(
          "${user.displayName} (${user.email}) has already been invited to this organization.")
    }

    // Check if user has already accepted an invitation (is a member)
    val hasAcceptedInvitation =
        existingInvitations.any {
          it.orgId == organizationId && it.status == InvitationStatus.ACCEPTED
        }

    if (hasAcceptedInvitation) {
      return InvitationResult.AlreadyInvited(
          "${user.displayName} (${user.email}) is already a member of this organization.")
    }

    // Create new invitation
    val invitation =
        OrganizationInvitation(
            orgId = organizationId,
            inviteeEmail = user.email,
            role = role,
            invitedBy = currentUserId ?: "",
            status = InvitationStatus.PENDING)

    val result = organizationRepository.createInvitation(invitation)

    return result.fold(
        onSuccess = { InvitationResult.Success },
        onFailure = { error ->
          InvitationResult.Error("Failed to create invitation: ${error.message ?: "Unknown error"}")
        })
  }

  /** Clears the current error message. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  /** Dismisses the permission denied dialog. */
  fun dismissPermissionDeniedDialog() {
    _uiState.value = _uiState.value.copy(showPermissionDeniedDialog = false)
  }

  /** Dismisses the invitation result dialog. */
  fun dismissInvitationResultDialog() {
    _uiState.value =
        _uiState.value.copy(invitationResultMessage = null, invitationResultType = null)
  }

  /** Clears the snackbar message. */
  fun clearSnackbarMessage() {
    _uiState.value = _uiState.value.copy(snackbarMessage = null)
  }

  /** Returns the list of available roles for invitation (excludes OWNER). */
  fun getAvailableRoles(): List<OrganizationRole> {
    return OrganizationRole.entries.filter { it != OrganizationRole.OWNER }
  }

  companion object {
    /** Debounce delay for search queries in milliseconds. */
    private const val SEARCH_DEBOUNCE_MS = 500L
  }
}
