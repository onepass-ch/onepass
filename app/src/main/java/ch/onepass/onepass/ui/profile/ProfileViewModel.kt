package ch.onepass.onepass.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
)

sealed interface ProfileEffect {
    object NavigateToAccountSettings : ProfileEffect
    object NavigateToPaymentMethods : ProfileEffect
    object NavigateToHelp : ProfileEffect
    object NavigateToCreateEvent : ProfileEffect
    object NavigateToOrganizerOnboarding : ProfileEffect
    object SignOut : ProfileEffect
}

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ProfileEffect>()
    val effects: MutableSharedFlow<ProfileEffect> = _effects

    init {
        // TODO hook to repository. Mock for now.
        viewModelScope.launch {
            _state.value = ProfileUiState(
                displayName = "WILL SMITH",
                email = "willsmith@email.com",
                initials = "CJ", // derive from name/email if no avatar
                stats = ProfileStats(events = 12, upcoming = 4, saved = 8),
                isOrganizer = false,
                loading = false
            )
        }
    }

    fun onCreateEventClicked() = viewModelScope.launch {
        if (_state.value.isOrganizer) {
            _effects.emit(ProfileEffect.NavigateToCreateEvent)
        } else {
            _effects.emit(ProfileEffect.NavigateToOrganizerOnboarding)
        }
    }

    fun onAccountSettings() = viewModelScope.launch {
        _effects.emit(ProfileEffect.NavigateToAccountSettings)
    }

    fun onPaymentMethods() = viewModelScope.launch {
        _effects.emit(ProfileEffect.NavigateToPaymentMethods)
    }

    fun onHelp() = viewModelScope.launch {
        _effects.emit(ProfileEffect.NavigateToHelp)
    }

    fun onSignOut() = viewModelScope.launch {
        _effects.emit(ProfileEffect.SignOut)
    }
}
