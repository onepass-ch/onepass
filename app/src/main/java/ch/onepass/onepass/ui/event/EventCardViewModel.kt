package ch.onepass.onepass.ui.event

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EventCardViewModel : ViewModel() {
  companion object {
    private val instance = EventCardViewModel()

    fun getInstance(): EventCardViewModel = instance
  }

  private val _likedEvents = MutableStateFlow<Set<String>>(emptySet())
  val likedEvents: StateFlow<Set<String>> = _likedEvents.asStateFlow()

  fun toggleLike(eventId: String) {
    _likedEvents.update { currentLiked ->
      currentLiked.toMutableSet().apply { if (contains(eventId)) remove(eventId) else add(eventId) }
    }
  }

  fun isEventLiked(eventId: String): Boolean {
    return _likedEvents.value.contains(eventId)
  }
}
