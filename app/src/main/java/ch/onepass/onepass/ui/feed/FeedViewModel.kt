package ch.onepass.onepass.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import kotlin.collections.filter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for the feed screen.
 *
 * @property events List of events to display.
 * @property isLoading Whether data is currently being fetched.
 * @property error Error message if fetching failed, null otherwise.
 * @property location Current location name to display.
 */
data class FeedUIState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val location: String = "SWITZERLAND",
)

/**
 * ViewModel for the feed screen.
 *
 * @property repository The event repository for data operations.
 */
class FeedViewModel(
    private val repository: EventRepository = EventRepositoryFirebase(),
) : ViewModel() {

  companion object {
    /** Maximum number of loaded events to return */
    private const val LOADED_EVENTS_LIMIT = 20
  }

  private val _uiState = MutableStateFlow(FeedUIState())
  val uiState: StateFlow<FeedUIState> = _uiState.asStateFlow()
  private val _allEvents = MutableStateFlow<List<Event>>(emptyList())

  /** Loads events from the repository, filtering for PUBLISHED status. */
  fun loadEvents() {
    _uiState.update { it.copy(isLoading = true, error = null) }

    viewModelScope.launch {
      try {
        repository.getEventsByStatus(EventStatus.PUBLISHED).take(LOADED_EVENTS_LIMIT).collect {
            events ->
          _allEvents.value = events
          _uiState.update { it.copy(events = events, isLoading = false, error = null) }
        }
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load events") }
      }
    }
  }

  /** Apply current filters to the loaded events */
  fun applyFiltersToCurrentEvents(filters: EventFilters) {
    val filteredEvents = applyFiltersLocally(_allEvents.value, filters)
    _uiState.update { it.copy(events = filteredEvents) }
  }

  /** Apply filters locally to the given events */
  private fun applyFiltersLocally(
      events: List<Event>,
      filters: EventFilters,
  ): List<Event> {
    return events.filter { event ->
      val regionMatch =
          filters.region?.let { region ->
            event.location?.region.equals(region, ignoreCase = true) ||
                event.location?.name?.contains(region, ignoreCase = true) == true
          } ?: true
      val dateMatch =
          filters.dateRange?.let { range ->
            event.startTime?.toDate()?.time?.let { eventTime -> eventTime in range } ?: false
          } ?: true
      val availabilityMatch = !filters.hideSoldOut || !event.isSoldOut
      regionMatch && dateMatch && availabilityMatch
    }
  }

  /** Refreshes the events list. */
  fun refreshEvents() {
    loadEvents()
  }

  /** Clears any error state. */
  fun clearError() {
    _uiState.update { it.copy(error = null) }
  }

  /**
   * Updates the location filter.
   *
   * @param location The new location name.
   */
  fun setLocation(location: String) {
    _uiState.update { it.copy(location = location) }
  }
}
