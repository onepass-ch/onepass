package ch.onepass.onepass.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.membership.MembershipRepository
import ch.onepass.onepass.model.membership.MembershipRepositoryFirebase
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// --- UI Models ---
data class ProfileStats(
    val events: Int = 0,
    val upcoming: Int = 0,
    val saved: Int = 0,
)

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val initials: String = "",
    val stats: ProfileStats = ProfileStats(),
    val isOrganizer: Boolean = false,
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val pendingInvitations: Int = 0
)

sealed interface ProfileEffect {
  object NavigateToAccountSettings : ProfileEffect

  object NavigateToPaymentMethods : ProfileEffect

  object NavigateToHelp : ProfileEffect

  object NavigateToMyInvitations : ProfileEffect

  object NavigateToMyOrganizations : ProfileEffect

  object SignOut : ProfileEffect

  object NavigateToBecomeOrganizer : ProfileEffect
}

open class ProfileViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val membershipRepository: MembershipRepository = MembershipRepositoryFirebase(),
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase()
) : ViewModel() {

  private val _state = MutableStateFlow(ProfileUiState())
  open val state: StateFlow<ProfileUiState> = _state.asStateFlow()

  private val _effects = MutableSharedFlow<ProfileEffect>(replay = 0, extraBufferCapacity = 1)
  val effects = _effects.asSharedFlow()

  init {
    loadProfile()
    observePendingInvitations()
  }

  private fun observePendingInvitations() {
    viewModelScope.launch {
      try {
        val user = userRepository.getCurrentUser() ?: return@launch
        organizationRepository.getInvitationsByEmail(user.email).collect { invitations ->
          val pendingCount = invitations.count { it.status == InvitationStatus.PENDING }
          _state.value = _state.value.copy(pendingInvitations = pendingCount)
        }
      } catch (_: Exception) {}
    }
  }

  private suspend fun loadPendingInvitationsCount(): Int {
    return try {
      val user = userRepository.getCurrentUser() ?: return 0
      val invitations = organizationRepository.getInvitationsByEmail(user.email).first()
      invitations.count { it.status == InvitationStatus.PENDING }
    } catch (_: Exception) {
      0
    }
  }
  /** Fetches the current user profile from Firestore. */
  fun loadProfile() {
    viewModelScope.launch {
      try {
        _state.value = _state.value.copy(loading = true)
        val user = userRepository.getCurrentUser() ?: userRepository.getOrCreateUser()

        if (user != null) {
          // Check if user has any organization membership
          val memberships =
              membershipRepository.getOrganizationsByUser(user.uid).getOrNull() ?: emptyList()
          val isOrganizer = memberships.isNotEmpty()
          val pendingCount = loadPendingInvitationsCount()

          _state.value = user.toUiState(isOrganizer).copy(pendingInvitations = pendingCount)
        } else {
          _state.value =
              _state.value.copy(loading = false, errorMessage = "User not found or not logged in")
        }
      } catch (e: Exception) {
        _state.value =
            _state.value.copy(
                loading = false, errorMessage = e.message ?: "Failed to load user profile")
      }
    }
  }

  /**
   * Handles organization button click.
   * - If the user is already an organizer, navigates to My Organizations.
   * - Otherwise, navigates to Become an Organizer onboarding.
   */
  fun onOrganizationButton() =
      viewModelScope.launch {
        if (_state.value.isOrganizer) {
          _effects.emit(ProfileEffect.NavigateToMyOrganizations)
        } else {
          _effects.emit(ProfileEffect.NavigateToBecomeOrganizer)
        }
      }

  // --- Placeholder stubs to avoid navigating to non-existent screens ---

  fun onAccountSettings() =
      viewModelScope.launch {
        // TODO: enable when Account Settings screen exists
        // _effects.emit(ProfileEffect.NavigateToAccountSettings)
      }

  fun onInvitations() =
      viewModelScope.launch { _effects.tryEmit(ProfileEffect.NavigateToMyInvitations) }

  fun onPaymentMethods() =
      viewModelScope.launch {
        // TODO: enable when Payment Methods screen exists
        // _effects.emit(ProfileEffect.NavigateToPaymentMethods)
      }

  fun onHelp() =
      viewModelScope.launch {
        // TODO: enable when Help screen exists
        // _effects.emit(ProfileEffect.NavigateToHelp)
      }

  fun onSignOut() = viewModelScope.launch { _effects.tryEmit(ProfileEffect.SignOut) }
}

// --- Extension: Map User model to UI state ---
private fun User.toUiState(isOrganizer: Boolean): ProfileUiState {
  val initials =
      displayName
          .split(" ")
          .filter { it.isNotBlank() }
          .take(2)
          .joinToString("") { it.first().uppercase() }

  return ProfileUiState(
      displayName = displayName.ifBlank { email.substringBefore("@") },
      email = email,
      avatarUrl = avatarUrl,
      initials = initials,
      stats = ProfileStats(events = 0, upcoming = 0, saved = 0),
      isOrganizer = isOrganizer,
      loading = false)
}
