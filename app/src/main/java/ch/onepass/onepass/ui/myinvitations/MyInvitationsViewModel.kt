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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the My Invitations screen.
 *
 * @property invitations List of pending invitations for the current user.
 * @property loading Whether the invitations are currently being loaded.
 * @property errorMessage Error message to display if an operation fails, null if no error.
 */
data class MyInvitationsUiState(
    val invitations: List<OrganizationInvitation> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null
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

  private val _userEmail = MutableStateFlow<String?>(null)
  private val _errorMessage = MutableStateFlow<String?>(null)

  /**
   * StateFlow of pending invitations for the current user.
   *
   * This Flow automatically updates when invitations change in the repository. It filters to only
   * include invitations with PENDING status.
   */
  val invitations: StateFlow<List<OrganizationInvitation>> =
      _userEmail
          .flatMapLatest { email ->
            if (email.isNullOrBlank()) {
              flowOf(emptyList<OrganizationInvitation>())
            } else {
              organizationRepository
                  .getInvitationsByEmail(email)
                  .map { allInvitations ->
                    // Filter to only include PENDING invitations
                    allInvitations.filter { it.status == InvitationStatus.PENDING }
                  }
                  .catch { e ->
                    // If there's an error loading invitations, update error message
                    _errorMessage.value = e.message ?: "Failed to load invitations"
                    emit(emptyList())
                  }
            }
          }
          .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  /** Combined UI state that includes invitations, loading state, and error messages. */
  val state: StateFlow<MyInvitationsUiState> =
      combine(invitations, _userEmail, _errorMessage) { invs, email, error ->
            MyInvitationsUiState(invitations = invs, loading = email == null, errorMessage = error)
          }
          .stateIn(viewModelScope, SharingStarted.Eagerly, MyInvitationsUiState(loading = true))

  init {
    loadUserEmail()
  }

  /**
   * Loads the current user's email address.
   *
   * This method retrieves the current user and extracts their email, which is then used to fetch
   * their invitations.
   */
  private fun loadUserEmail() {
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser() ?: userRepository.getOrCreateUser()
        if (user == null || user.email.isBlank()) {
          _errorMessage.value = "User not found or not logged in"
          _userEmail.value = null
        } else {
          _userEmail.value = user.email
          _errorMessage.value = null
        }
      } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to load user information"
        _userEmail.value = null
      }
    }
  }

  /**
   * Accepts an invitation by updating its status to ACCEPTED.
   *
   * After successfully updating the invitation status, the UI state will automatically update
   * because the Flow from getInvitationsByEmail will emit the updated list, and the invitation will
   * be filtered out since it's no longer PENDING.
   *
   * @param invitationId The ID of the invitation to accept.
   */
  fun acceptInvitation(invitationId: String) {
    viewModelScope.launch {
      try {
        _errorMessage.value = null

        val result =
            organizationRepository.updateInvitationStatus(
                invitationId = invitationId, newStatus = InvitationStatus.ACCEPTED)

        result.onFailure { error ->
          _errorMessage.value = error.message ?: "Failed to accept invitation"
        }
        // On success, the Flow will automatically update the state
      } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to accept invitation"
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
        _errorMessage.value = null

        val result =
            organizationRepository.updateInvitationStatus(
                invitationId = invitationId, newStatus = InvitationStatus.REJECTED)

        result.onFailure { error ->
          _errorMessage.value = error.message ?: "Failed to reject invitation"
        }
        // On success, the Flow will automatically update the state
      } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to reject invitation"
      }
    }
  }
}
