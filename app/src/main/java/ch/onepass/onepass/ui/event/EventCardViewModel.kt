package ch.onepass.onepass.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the "Like" state of event cards.
 *
 * It observes the current user's favorite events from the repository and exposes them as a
 * [StateFlow]. It also provides a method to toggle the like status of an event, which updates the
 * backend and optimistically updates the local state.
 *
 * @param userRepository The repository for user data operations.
 * @param auth The Firebase authentication instance to get the current user.
 */
class EventCardViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _likedEvents = MutableStateFlow<Set<String>>(emptySet())
  /** A set of event IDs that the current user has liked. */
  val likedEvents: StateFlow<Set<String>> = _likedEvents.asStateFlow()

  init {
    observeLikes()
  }

  /**
   * Starts observing the user's favorite events from the repository. If no user is logged in, no
   * observation occurs.
   */
  private fun observeLikes() {
    val uid = auth.currentUser?.uid
    if (uid != null) {
      viewModelScope.launch {
        userRepository.getFavoriteEvents(uid).collect { favorites ->
          _likedEvents.value = favorites
        }
      }
    }
  }

  /**
   * Toggles the like status of an event.
   *
   * This method performs an optimistic update on the local state and then initiates the network
   * request. If the network request fails, the local state is reverted.
   *
   * @param eventId The ID of the event to toggle.
   */
  fun toggleLike(eventId: String) {
    val uid = auth.currentUser?.uid ?: return
    val currentLikes = _likedEvents.value
    val isLiked = currentLikes.contains(eventId)

    // Optimistic update
    _likedEvents.update { if (isLiked) it - eventId else it + eventId }

    viewModelScope.launch {
      val result =
          if (isLiked) {
            userRepository.removeFavoriteEvent(uid, eventId)
          } else {
            userRepository.addFavoriteEvent(uid, eventId)
          }

      // Revert if failed
      if (result.isFailure) {
        android.util.Log.e(
            "EventCardViewModel",
            "Failed to update like status for event $eventId (isLiked: $isLiked)",
            result.exceptionOrNull())
        _likedEvents.update { if (isLiked) it + eventId else it - eventId }
      }
    }
  }

  /**
   * Checks if a specific event is currently liked.
   *
   * @param eventId The ID of the event.
   * @return True if the event is liked, false otherwise.
   */
  fun isEventLiked(eventId: String): Boolean {
    return _likedEvents.value.contains(eventId)
  }
}
