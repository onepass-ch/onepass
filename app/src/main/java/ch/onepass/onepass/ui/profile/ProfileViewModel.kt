package ch.onepass.onepass.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.membership.MembershipRepository
import ch.onepass.onepass.model.membership.MembershipRepositoryFirebase
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.ticket.TicketRepository
import ch.onepass.onepass.model.ticket.TicketRepositoryFirebase
import ch.onepass.onepass.model.ticket.computeUiStatus
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import ch.onepass.onepass.ui.myevents.TicketStatus
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
    val bio: String? = null,
    val country: String? = null,
    val initials: String = "",
    val stats: ProfileStats = ProfileStats(),
    val isOrganizer: Boolean = false,
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val pendingInvitations: Int = 0,
    val isEmailPublic: Boolean = false
)

sealed interface ProfileEffect {
  object NavigateToAccountSettings : ProfileEffect

  object NavigateToPaymentMethods : ProfileEffect

  object NavigateToHelp : ProfileEffect

  object NavigateToMyInvitations : ProfileEffect

  object NavigateToMyOrganizations : ProfileEffect

  object SignOut : ProfileEffect

  object NavigateToBecomeOrganizer : ProfileEffect

  object NavigateToEditProfile : ProfileEffect
}

open class ProfileViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val membershipRepository: MembershipRepository = MembershipRepositoryFirebase(),
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val ticketRepository: TicketRepository = TicketRepositoryFirebase()
) : ViewModel() {

  private val _state = MutableStateFlow(ProfileUiState())
  open val state: StateFlow<ProfileUiState> = _state.asStateFlow()

  private val _effects = MutableSharedFlow<ProfileEffect>(replay = 0, extraBufferCapacity = 1)
  val effects = _effects.asSharedFlow()

  init {
    loadProfile()
    observePendingInvitations()
    observeRealTimeStats()
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
  /** Watches "Saved" (favorites) and Tickets (upcoming/events) in real-time. */
  private fun observeRealTimeStats() {
    // MOVE EVERYTHING INSIDE viewModelScope.launch
    viewModelScope.launch {
      // NOW you can call suspend functions safely
      val userId = userRepository.getCurrentUser()?.uid ?: return@launch

      // 1. Observe Favorites for "Saved" count
      // We launch a NEW child coroutine so this flow runs in parallel
      launch {
        userRepository.getFavoriteEvents(userId).collect { favorites ->
          _state.value = _state.value.copy(stats = _state.value.stats.copy(saved = favorites.size))
        }
      }

      // 2. Observe Tickets for "Events" (Past) and "Upcoming"
      // Another parallel coroutine
      launch {
        ticketRepository.getTicketsByUser(userId).collect { tickets ->
          var upcomingCount = 0
          var pastCount = 0

          tickets.forEach { ticket ->
            // Simple check based on ticket state/expiration
            if (ticket.computeUiStatus() == TicketStatus.EXPIRED) {
              pastCount++
            } else {
              upcomingCount++
            }
          }

          _state.value =
              _state.value.copy(
                  stats = _state.value.stats.copy(upcoming = upcomingCount, events = pastCount))
        }
      }
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
          val existingStats = _state.value.stats
          val baseUiState = user.toUiState(isOrganizer)
          val mergedStats =
              existingStats.copy(
                  saved = user.favoriteEventIds.size // Always trust the user object for this
                  )
          _state.value =
              baseUiState.copy(
                  pendingInvitations = pendingCount,
                  stats = mergedStats // Set the merged stats, don't overwrite with 0!
                  )
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

  fun onAccountSettings() =
      viewModelScope.launch { _effects.emit(ProfileEffect.NavigateToAccountSettings) }

  fun onInvitations() =
      viewModelScope.launch { _effects.tryEmit(ProfileEffect.NavigateToMyInvitations) }

  fun onPaymentMethods() =
      viewModelScope.launch { _effects.emit(ProfileEffect.NavigateToPaymentMethods) }

  fun onHelp() =
      viewModelScope.launch {
        // TODO: enable when Help screen exists
        // _effects.emit(ProfileEffect.NavigateToHelp)
      }

  fun onEditProfile() {
    viewModelScope.launch { _effects.tryEmit(ProfileEffect.NavigateToEditProfile) }
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
      bio = bio,
      country = country,
      initials = initials,
      stats = ProfileStats(events = 0, upcoming = 0, saved = favoriteEventIds.size),
      isOrganizer = isOrganizer,
      loading = false,
  )
}
