package ch.onepass.onepass.ui.myinvitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the My Invitations screen.
 *
 * @property invitations List of pending invitations for the current user.
 * @property loading Whether the invitations are currently being loaded.
 * @property errorMessage Error message to display if an operation fails, null if no error.
 * @property successMessage Success message to display after a successful operation, null if no
 *   success message.
 * @property userEmail Email address of the current user, null if not loaded or not logged in.
 */
data class MyInvitationsUiState(
    val invitations: List<OrganizationInvitation> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val userEmail: String? = null
)

/**
 * ViewModel for managing organization invitations for the current user.
 *
 * This ViewModel handles:
 * - Loading pending invitations for the current user
 * - Accepting invitations
 * - Rejecting invitations
 * - Updating the UI state after operations
 *
 * @param organizationRepository Repository for organization-related operations.
 * @param userRepository Repository for user-related operations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyInvitationsViewModel(
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val userRepository: UserRepository = UserRepositoryFirebase()
) : ViewModel() {

  private val _uiState = MutableStateFlow(MyInvitationsUiState())

  /** Combined UI state that includes invitations, loading state, error messages, and user email. */
  val uiState: StateFlow<MyInvitationsUiState> = _uiState.asStateFlow()

  /**
   * StateFlow of pending invitations for the current user.
   *
   * This Flow automatically updates when invitations change in the repository. It filters to only
   * include invitations with PENDING status.
   *
   * When userEmail is null (user not logged in or still loading), this flow does not emit values,
   * preserving the initial empty list. This allows proper distinction between:
   * - Not logged in: invitations remains at initial emptyList() (never updated)
   * - Logged in but no invitations: invitations is updated to emptyList() after query
   *
   * The `state` property combines this with `loading` and `userEmail` for complete state
   * management.
   */
  val invitations: StateFlow<List<OrganizationInvitation>> =
      _uiState
          .map { it.userEmail }
          .flatMapLatest { email ->
            if (email.isNullOrBlank()) {
              // When email is null, don't emit any values. This preserves the initial state
              // and allows distinction between "not loaded" and "loaded but empty".
              // The StateFlow will maintain its initial value (emptyList()) until email is
              // available.
              emptyFlow()
            } else {
              // When email is available, fetch invitations from repository
              organizationRepository
                  .getInvitationsByEmail(email)
                  .map { allInvitations ->
                    // Filter to only include PENDING invitations
                    allInvitations.filter { it.status == InvitationStatus.PENDING }
                  }
                  .catch { e ->
                    // If there's an error loading invitations, update error message
                    _uiState.value =
                        _uiState.value.copy(
                            errorMessage = e.message ?: "Failed to load invitations")
                    emit(emptyList())
                  }
            }
          }
          .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  /**
   * Combined UI state that includes invitations, loading state, error messages, and user email.
   *
   * This is the primary state that UI should observe. It properly distinguishes between:
   * 1. User not logged in or still loading:
   *     - `loading = true`
   *     - `userEmail = null`
   *     - `errorMessage = null`
   *     - `invitations = emptyList()` (initial state, never updated from repository)
   * 2. User logged in but has no pending invitations:
   *     - `loading = false`
   *     - `userEmail != null` (e.g., "user@example.com")
   *     - `errorMessage = null`
   *     - `invitations = emptyList()` (updated from repository after query)
   * 3. User logged in with pending invitations:
   *     - `loading = false`
   *     - `userEmail != null`
   *     - `errorMessage = null`
   *     - `invitations = [list of pending invitations]`
   * 4. Error state (loading failed):
   *     - `loading = false`
   *     - `userEmail = null` (or may be set if error occurred after login)
   *     - `errorMessage != null`
   *     - `invitations = emptyList()`
   *
   * The key distinction: In case 1, invitations never gets updated (stays at initial emptyList()).
   * In case 2, invitations is explicitly updated to emptyList() after a successful repository
   * query.
   */
  val state: StateFlow<MyInvitationsUiState> =
      combine(invitations, _uiState) { invs, currentState ->
            // loading is true only when userEmail is null AND there's no error
            // If there's an error, we should stop loading and show the error message
            currentState.copy(
                invitations = invs,
                loading = currentState.userEmail == null && currentState.errorMessage == null)
          }
          .stateIn(viewModelScope, SharingStarted.Eagerly, MyInvitationsUiState(loading = true))

  init {
    loadUserEmail()
  }

  /**
   * Retries loading user information and invitations.
   *
   * This method can be called when an error occurs to retry loading the user's email and
   * subsequently their invitations.
   */
  fun retry() {
    loadUserEmail()
  }

  /**
   * Loads the current user's email address.
   *
   * This method retrieves the current user and extracts their email, which is then used to fetch
   * their invitations.
   *
   * Note: When an error occurs, we set both errorMessage and userEmail to null. The loading state
   * is calculated as `userEmail == null && errorMessage == null`, so when there's an error, loading
   * will be false (not true), preventing the loading spinner from showing alongside the error
   * message.
   */
  private fun loadUserEmail() {
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser() ?: userRepository.getOrCreateUser()
        if (user == null || user.email.isBlank()) {
          _uiState.value =
              _uiState.value.copy(
                  errorMessage = "User not found or not logged in",
                  userEmail = null,
                  successMessage = null)
        } else {
          _uiState.value =
              _uiState.value.copy(
                  userEmail = user.email, errorMessage = null, successMessage = null)
        }
      } catch (e: Exception) {
        // When error occurs, set errorMessage and userEmail to null.
        // This ensures loading = false (since errorMessage != null),
        // so users won't see loading spinner and error message simultaneously.
        _uiState.value =
            _uiState.value.copy(
                errorMessage = e.message ?: "Failed to load user information",
                userEmail = null,
                successMessage = null)
      }
    }
  }

  /**
   * Accepts an invitation by updating its status to ACCEPTED and adding the user as a member to the
   * organization.
   *
   * After successfully updating the invitation status and adding the member, the UI state will
   * automatically update because the Flow from getInvitationsByEmail will emit the updated list,
   * and the invitation will be filtered out since it's no longer PENDING.
   *
   * @param invitationId The ID of the invitation to accept.
   */
  fun acceptInvitation(invitationId: String) {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)

        // Get current user first - we need userId to add as member
        val user = userRepository.getCurrentUser() ?: userRepository.getOrCreateUser()
        if (user == null) {
          _uiState.value =
              _uiState.value.copy(errorMessage = "User not found. Please log in and try again.")
          return@launch
        }

        // Find the invitation from the current list to get orgId and role
        val invitation =
            invitations.value.find { it.id == invitationId }
                ?: run {
                  _uiState.value =
                      _uiState.value.copy(
                          errorMessage = "Invitation not found. It may have been removed.")
                  return@launch
                }

        // Step 1: Update invitation status to ACCEPTED
        val updateStatusResult =
            organizationRepository.updateInvitationStatus(
                invitationId = invitationId, newStatus = InvitationStatus.ACCEPTED)

        updateStatusResult
            .onSuccess {
              // Step 2: Add user as a member to the organization
              val addMemberResult =
                  organizationRepository.addMember(
                      organizationId = invitation.orgId, userId = user.uid, role = invitation.role)

              addMemberResult
                  .onSuccess {
                    // Both operations succeeded - provide success feedback
                    _uiState.value =
                        _uiState.value.copy(
                            successMessage =
                                "Invitation accepted successfully. You are now a member.")
                    // The invitation will automatically disappear from the list via the Flow update
                  }
                  .onFailure { error ->
                    // Invitation status was updated but adding member failed
                    // This is a critical error - the invitation is accepted but user is not a
                    // member
                    _uiState.value =
                        _uiState.value.copy(
                            errorMessage =
                                "Invitation accepted but failed to add you as a member: ${error.message
                                    ?: "Unknown error"}. Please contact support.")
                  }
            }
            .onFailure { error ->
              _uiState.value =
                  _uiState.value.copy(errorMessage = error.message ?: "Failed to accept invitation")
            }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(errorMessage = e.message ?: "Failed to accept invitation")
      }
    }
  }

  /**
   * Rejects an invitation by updating its status to REJECTED.
   *
   * After successfully updating the invitation status, the UI state will automatically update
   * because the Flow from getInvitationsByEmail will emit the updated list, and the invitation will
   * be filtered out since it's no longer PENDING.
   *
   * @param invitationId The ID of the invitation to reject.
   */
  fun rejectInvitation(invitationId: String) {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)

        val result =
            organizationRepository.updateInvitationStatus(
                invitationId = invitationId, newStatus = InvitationStatus.REJECTED)

        result
            .onSuccess {
              // Provide success feedback to the user
              _uiState.value =
                  _uiState.value.copy(successMessage = "Invitation rejected successfully")
              // Clear success message after a delay (UI can handle this with a timeout)
              // The invitation will automatically disappear from the list via the Flow update
            }
            .onFailure { error ->
              _uiState.value =
                  _uiState.value.copy(errorMessage = error.message ?: "Failed to reject invitation")
            }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(errorMessage = e.message ?: "Failed to reject invitation")
      }
    }
  }

  /**
   * Clears the success message from the UI state.
   *
   * This method should be called after displaying the success message (e.g., in a snackbar) to
   * ensure that subsequent operations with the same success message will trigger the UI to show the
   * snackbar again.
   */
  fun clearSuccessMessage() {
    _uiState.value = _uiState.value.copy(successMessage = null)
  }
}
