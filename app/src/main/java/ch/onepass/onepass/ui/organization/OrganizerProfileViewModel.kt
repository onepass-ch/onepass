package ch.onepass.onepass.ui.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.organization.Post
import ch.onepass.onepass.model.organization.PostRepository
import ch.onepass.onepass.model.organization.PostRepositoryFirebase
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
    val posts: List<Post> = emptyList(),
    val postsLoading: Boolean = false,
    val showCreatePostDialog: Boolean = false,
    val isSubmittingPost: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val postToDelete: Post? = null,
    val isDeletingPost: Boolean = false,
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
    private val postRepository: PostRepository = PostRepositoryFirebase(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _state = MutableStateFlow(OrganizerProfileUiState())
  open val state: StateFlow<OrganizerProfileUiState> = _state.asStateFlow()

  private val _effects =
      MutableSharedFlow<OrganizerProfileEffect>(replay = 0, extraBufferCapacity = 1)
  val effects = _effects.asSharedFlow()

  private var currentOrganizationId: String? = null

  /**
   * Loads the organization profile for the given organization ID.
   *
   * @param organizationId The unique identifier of the organization to load.
   */
  fun loadOrganizationProfile(organizationId: String) {
    // Avoid reloading if already loaded
    if (currentOrganizationId == organizationId && _state.value.organizationId.isNotEmpty()) {
      return
    }
    currentOrganizationId = organizationId

    viewModelScope.launch {
      try {
        _state.value = _state.value.copy(loading = true, errorMessage = null)

        organizationRepository.getOrganizationById(organizationId).collect { organization ->
          if (organization != null) {
            val currentUserId = auth.currentUser?.uid
            val isOwner = currentUserId != null && organization.ownerId == currentUserId

            // Preserve existing posts when updating organization info
            val currentPosts = _state.value.posts
            val currentPostsLoading = _state.value.postsLoading
            _state.value = organization.toUiState(isOwner = isOwner).copy(
                posts = currentPosts,
                postsLoading = currentPostsLoading
            )
            
            // Load events and posts only on first load
            if (currentPosts.isEmpty() && !currentPostsLoading) {
              loadOrganizationEvents(organizationId)
              loadOrganizationPosts(organizationId)
            }
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

  /**
   * Loads posts for the organization.
   *
   * @param organizationId The unique identifier of the organization.
   */
  private fun loadOrganizationPosts(organizationId: String) {
    viewModelScope.launch {
      try {
        _state.value = _state.value.copy(postsLoading = true)
        postRepository.getPostsByOrganization(organizationId).collect { posts ->
          android.util.Log.d("OrganizerProfileVM", "Loaded ${posts.size} posts for org: $organizationId")
          _state.value = _state.value.copy(posts = posts, postsLoading = false)
        }
      } catch (e: Exception) {
        android.util.Log.e("OrganizerProfileVM", "Failed to load posts: ${e.message}", e)
        _state.value = _state.value.copy(posts = emptyList(), postsLoading = false)
        _effects.tryEmit(OrganizerProfileEffect.ShowError("Failed to load posts: ${e.message}"))
      }
    }
  }

  // ===== Post Management =====

  /** Opens the create post dialog. */
  fun openCreatePostDialog() {
    _state.value = _state.value.copy(showCreatePostDialog = true)
  }

  /** Closes the create post dialog. */
  fun closeCreatePostDialog() {
    _state.value = _state.value.copy(showCreatePostDialog = false, isSubmittingPost = false)
  }

  /**
   * Creates a new post for the organization.
   *
   * @param content The sanitized content of the post.
   */
  fun createPost(content: String) {
    val organizationId = _state.value.organizationId
    val userId = auth.currentUser?.uid ?: return
    val userName = auth.currentUser?.displayName ?: "Unknown"

    _state.value = _state.value.copy(isSubmittingPost = true)

    viewModelScope.launch {
      val post = Post(
          organizationId = organizationId,
          authorId = userId,
          authorName = userName,
          content = content
      )

      postRepository.createPost(post)
          .onSuccess {
            _state.value = _state.value.copy(
                showCreatePostDialog = false,
                isSubmittingPost = false
            )
          }
          .onFailure { error ->
            _state.value = _state.value.copy(isSubmittingPost = false)
            _effects.tryEmit(
                OrganizerProfileEffect.ShowError(error.message ?: "Failed to create post"))
          }
    }
  }

  /**
   * Opens the delete confirmation dialog for a post.
   *
   * @param post The post to delete.
   */
  fun openDeleteConfirmation(post: Post) {
    _state.value = _state.value.copy(
        showDeleteConfirmation = true,
        postToDelete = post
    )
  }

  /** Closes the delete confirmation dialog. */
  fun closeDeleteConfirmation() {
    _state.value = _state.value.copy(
        showDeleteConfirmation = false,
        postToDelete = null,
        isDeletingPost = false
    )
  }

  /** Confirms and executes the deletion of the selected post. */
  fun confirmDeletePost() {
    val post = _state.value.postToDelete ?: return

    _state.value = _state.value.copy(isDeletingPost = true)

    viewModelScope.launch {
      postRepository.deletePost(post.id)
          .onSuccess {
            _state.value = _state.value.copy(
                showDeleteConfirmation = false,
                postToDelete = null,
                isDeletingPost = false,
                posts = _state.value.posts.filter { it.id != post.id }
            )
          }
          .onFailure { error ->
            _state.value = _state.value.copy(isDeletingPost = false)
            _effects.tryEmit(
                OrganizerProfileEffect.ShowError(error.message ?: "Failed to delete post"))
          }
    }
  }

  /**
   * Toggles the like status of a post for the current user.
   *
   * @param postId The ID of the post to like/unlike.
   */
  fun toggleLike(postId: String) {
    val userId = auth.currentUser?.uid ?: return
    val post = _state.value.posts.find { it.id == postId } ?: return
    val isCurrentlyLiked = post.isLikedBy(userId)

    viewModelScope.launch {
      if (isCurrentlyLiked) {
        postRepository.unlikePost(postId, userId)
      } else {
        postRepository.likePost(postId, userId)
      }
    }
  }

  /**
   * Returns the current user's ID, if logged in.
   */
  fun getCurrentUserId(): String? = auth.currentUser?.uid

  /** Handles the follow/unfollow action */
  fun onFollowClicked() {
    viewModelScope.launch {
      try {
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
