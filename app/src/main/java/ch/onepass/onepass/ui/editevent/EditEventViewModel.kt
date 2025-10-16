package ch.onepass.onepass.ui.editevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for the edit event screen.
 *
 * @property events List of events associated with the user/organization.
 * @property isLoading Whether data is currently being fetched.
 * @property error Error message if fetching failed, null otherwise.
 */
data class EditEventUIState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the edit event screen.
 *
 * @property repository The event repository for data operations.
 */
class EditEventViewModel(private val repository: EventRepository = EventRepositoryFirebase()) :
    ViewModel() {

  private val _uiState = MutableStateFlow(EditEventUIState())
  val uiState: StateFlow<EditEventUIState> = _uiState.asStateFlow()

  /**
   * Loads events for a specific organization/user.
   *
   * @param userId The user/organization ID to fetch events for.
   */
  fun loadUserEvents(userId: String) {
    _uiState.update { it.copy(isLoading = true, error = null) }

    viewModelScope.launch {
      try {
        repository.getEventsByOrganization(userId).collect { events ->
          _uiState.update { it.copy(events = events, isLoading = false, error = null) }
        }
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load events") }
      }
    }
  }

  /** Refreshes the events list. */
  fun refreshEvents(userId: String) {
    loadUserEvents(userId)
  }

  /** Clears any error state. */
  fun clearError() {
    _uiState.update { it.copy(error = null) }
  }
}
