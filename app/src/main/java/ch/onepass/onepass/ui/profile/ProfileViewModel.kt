package ch.onepass.onepass.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

sealed interface ProfileEffect {
  // TODO: Re-enable once destinations exist
  object NavigateToAccountSettings : ProfileEffect

  object NavigateToPaymentMethods : ProfileEffect

  object NavigateToHelp : ProfileEffect

  // Keep this one active (only supported flow for now)
  object NavigateToOrganizerOnboarding : ProfileEffect

  // TODO: Wire this when auth flow is implemented
  object SignOut : ProfileEffect

  // TODO: Enable when Create Event screen exists
  object NavigateToCreateEvent : ProfileEffect
}

// --- ViewModel ---

open class ProfileViewModel(private val userRepository: UserRepository = UserRepositoryFirebase()) :
    ViewModel() {

  private val _state = MutableStateFlow(ProfileUiState())
  open val state: StateFlow<ProfileUiState> = _state.asStateFlow()

  private val _effects = MutableSharedFlow<ProfileEffect>(replay = 0, extraBufferCapacity = 1)
  val effects = _effects.asSharedFlow()

  init {
    loadProfile()
  }

  /** Fetches the current user profile from Firestore. */
  private fun loadProfile() {
    viewModelScope.launch {
      try {
        _state.value = _state.value.copy(loading = true)
        val user = userRepository.getCurrentUser() ?: userRepository.getOrCreateUser()

        if (user != null) {
          _state.value = user.toUiState()
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
   * Organizer action: For now, always navigate to Organizer Onboarding to avoid dead routes/black
   * screens.
   *
   * TODO: If user is already an organizer, route to Create Event when that screen exists.
   */
  fun onCreateEventClicked() =
      viewModelScope.launch {
        _effects.tryEmit(ProfileEffect.NavigateToOrganizerOnboarding)
        // TODO: when Create Event screen is implemented:
        // if (_state.value.isOrganizer) _effects.emit(ProfileEffect.NavigateToCreateEvent)
        // else _effects.emit(ProfileEffect.NavigateToOrganizerOnboarding)
      }

  // --- Placeholder stubs to avoid navigating to non-existent screens ---

  fun onAccountSettings() =
      viewModelScope.launch {
        // TODO: enable when Account Settings screen exists
        // _effects.emit(ProfileEffect.NavigateToAccountSettings)
      }

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
private fun User.toUiState(): ProfileUiState {
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
      isOrganizer = false, // TODO: bind to organization membership when available
      loading = false)
}
