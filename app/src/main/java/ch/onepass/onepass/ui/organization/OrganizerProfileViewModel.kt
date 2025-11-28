package ch.onepass.onepass.ui.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.utils.FormatUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- UI Models ---

/** Represents the selected tab in the organizer profile */
enum class OrganizerProfileTab {
  POSTS,
  UPCOMING,
  PAST
}

/** UI state for the organizer profile screen */
data class OrganizerProfileUiState(
    val organizationId: String = "",
    val name: String = "",
    val description: String = "",
    val profileImageUrl: String? = null,
    val coverImageUrl: String? = null,
    val websiteUrl: String? = null,
    val instagramUrl: String? = null,
    val tiktokUrl: String? = null,
    val facebookUrl: String? = null,
    val followersCount: Int = 0,
    val isFollowing: Boolean = false,
    val isOwner: Boolean = false,
    val isVerified: Boolean = false,
    val eventCount: Int = 0,
    val selectedTab: OrganizerProfileTab = OrganizerProfileTab.UPCOMING,
    val upcomingEvents: List<Event> = emptyList(),
    val pastEvents: List<Event> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null
) {
  /** Formatted followers count for display (e.g., "1K", "2.5M") */
  val followersCountFormatted: String
    get() = FormatUtils.formatCompactNumber(followersCount)
}

/** One-time effects/events for navigation and side effects */
sealed interface OrganizerProfileEffect {
  data class NavigateToEvent(val eventId: String) : OrganizerProfileEffect

  data class OpenWebsite(val url: String) : OrganizerProfileEffect

  data class OpenSocialMedia(val platform: String, val url: String) : OrganizerProfileEffect

  data class NavigateToEditOrganization(val organizationId: String) : OrganizerProfileEffect

  data class ShowError(val message: String) : OrganizerProfileEffect
}

// --- ViewModel ---

open class OrganizerProfileViewModel(
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val eventRepository: EventRepository = EventRepositoryFirebase(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _state = MutableStateFlow(OrganizerProfileUiState())
  open val state: StateFlow<OrganizerProfileUiState> = _state.asStateFlow()

  private val _effects =
      MutableSharedFlow<OrganizerProfileEffect>(replay = 0, extraBufferCapacity = 1)
  val effects = _effects.asSharedFlow()

  /**
   * Loads the organization profile for the given organization ID.
   *
   * @param organizationId The unique identifier of the organization to load.
   */
  fun loadOrganizationProfile(organizationId: String) {
    viewModelScope.launch {
      try {
        _state.value = _state.value.copy(loading = true, errorMessage = null)

        organizationRepository.getOrganizationById(organizationId).collect { organization ->
          if (organization != null) {
            val currentUserId = auth.currentUser?.uid
            val isOwner = currentUserId != null && organization.ownerId == currentUserId

            _state.value = organization.toUiState(isOwner = isOwner)
            // Load events for this organization
            loadOrganizationEvents(organizationId)
            // TODO: Check if current user is following this organization
            // This would require a user-organization relationship repository
          } else {
            _state.value =
                _state.value.copy(loading = false, errorMessage = "Organization not found")
          }
        }
      } catch (e: Exception) {
        _state.value =
            _state.value.copy(
                loading = false, errorMessage = e.message ?: "Failed to load organization profile")
        _effects.tryEmit(
            OrganizerProfileEffect.ShowError(e.message ?: "Failed to load organization profile"))
      }
    }
  }

  /**
   * Loads events for the organization and separates them into upcoming and past.
   *
   * @param organizationId The unique identifier of the organization.
   */
  private fun loadOrganizationEvents(organizationId: String) {
    viewModelScope.launch {
      try {
        eventRepository.getEventsByOrganization(organizationId).collect { events ->
          val now = Timestamp.now()
          val (upcoming, past) =
              events.partition { event -> event.endTime?.let { it.seconds >= now.seconds } ?: true }

          _state.value =
              _state.value.copy(
                  upcomingEvents = upcoming, pastEvents = past, eventCount = events.size)
        }
      } catch (e: Exception) {
        _effects.tryEmit(
            OrganizerProfileEffect.ShowError(e.message ?: "Failed to load organization events"))
      }
    }
  }

  /** Handles the follow/unfollow action */
  fun onFollowClicked() {
    viewModelScope.launch {
      try {
        // TODO: Implement follow/unfollow logic with user-organization relationship
        // this can't be done until  we have user list of organizations they follow
        // For now, just toggle the state locally
        val currentState = _state.value
        _state.value =
            currentState.copy(
                isFollowing = !currentState.isFollowing,
                followersCount =
                    if (currentState.isFollowing) maxOf(0, currentState.followersCount - 1)
                    else currentState.followersCount + 1)
      } catch (e: Exception) {
        _effects.tryEmit(
            OrganizerProfileEffect.ShowError(e.message ?: "Failed to update follow status"))
      }
    }
  }

  /** Handles tab selection changes */
  fun onTabSelected(tab: OrganizerProfileTab) {
    _state.value = _state.value.copy(selectedTab = tab)
  }

  /** Handles website link clicks */
  fun onWebsiteClicked() {
    val websiteUrl = _state.value.websiteUrl
    if (websiteUrl != null) {
      viewModelScope.launch { _effects.tryEmit(OrganizerProfileEffect.OpenWebsite(websiteUrl)) }
    }
  }

  /** Handles social media icon clicks */
  fun onSocialMediaClicked(platform: String) {
    val url =
        when (platform.lowercase()) {
          "instagram" -> _state.value.instagramUrl
          "tiktok" -> _state.value.tiktokUrl
          "facebook" -> _state.value.facebookUrl
          else -> null
        }

    if (url != null) {
      viewModelScope.launch {
        _effects.tryEmit(OrganizerProfileEffect.OpenSocialMedia(platform, url))
      }
    }
  }

  /** Handles event card clicks */
  fun onEventClicked(eventId: String) {
    viewModelScope.launch { _effects.tryEmit(OrganizerProfileEffect.NavigateToEvent(eventId)) }
  }

  /** Handles edit organization button click */
  fun onEditOrganizationClicked() {
    val organizationId = _state.value.organizationId
    if (organizationId.isNotEmpty()) {
      viewModelScope.launch {
        _effects.tryEmit(OrganizerProfileEffect.NavigateToEditOrganization(organizationId))
      }
    }
  }
}

// --- Extension: Map Organization model to UI state ---
private fun Organization.toUiState(isOwner: Boolean = false): OrganizerProfileUiState {
  return OrganizerProfileUiState(
      organizationId = id,
      name = name,
      description = description,
      profileImageUrl = profileImageUrl,
      coverImageUrl = coverImageUrl,
      websiteUrl = website,
      instagramUrl = instagram,
      tiktokUrl = tiktok,
      facebookUrl = facebook,
      followersCount = followerCount,
      isFollowing = false, // TODO: Determine from user-organization relationship, needs change !
      isOwner = isOwner,
      isVerified = verified,
      eventCount = eventIds.size,
      loading = false)
}
