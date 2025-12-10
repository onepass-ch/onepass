package ch.onepass.onepass.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.ui.event.EventCardViewModel
import ch.onepass.onepass.utils.EventFilteringUtils.applyFiltersLocally
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val location: String = "SWITZERLAND",
    val showFilterDialog: Boolean = false,
)

/**
 * ViewModel for the feed screen. Implements a content-based recommender system.
 *
 * @property repository The event repository for data operations.
 */
open class FeedViewModel(
    private val repository: EventRepository = EventRepositoryFirebase(),
    private val eventCardViewModel: EventCardViewModel? = null
) : ViewModel() {

  companion object {
    /** Maximum number of loaded events to return */
    const val LOADED_EVENTS_LIMIT = 20

    /** Recommendation Weights */
    private const val TAG_MATCH_WEIGHT = 2.0
    private const val RECENCY_BOOST = 5.0
    private const val USER_AFFINITY_BOOST = 10.0
  }

  private val _uiState = MutableStateFlow(FeedUIState())
  val uiState: StateFlow<FeedUIState> = _uiState.asStateFlow()

  // Holds the raw list of recommended events before local filters (region/date) are applied
  private val _allEvents = MutableStateFlow<List<Event>>(emptyList())

  /**
   * Loads events from the repository, combining them with user likes to generate a personalized
   * feed.
   */
  fun loadEvents() {
    _uiState.update { it.copy(isLoading = true, error = null) }

    viewModelScope.launch {
      try {
        repository.getEventsByStatus(EventStatus.PUBLISHED).collect { events ->
          // Fetch the current set of liked events value for recommendation
          val likedIds = eventCardViewModel?.likedEvents?.value ?: emptySet()
          val recommendedEvents = recommendEvents(events, likedIds)

          // Keep the full recommended list in memory
          _allEvents.value = recommendedEvents
          // Apply the view limit for the UI
          val limitedEvents = recommendedEvents.take(LOADED_EVENTS_LIMIT)

          _uiState.update { it.copy(events = limitedEvents, isLoading = false, error = null) }
        }
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load events") }
      }
    }
  }

  /**
   * Content-Based Recommender Algorithm. Sorts events based on tag relevance to the user's liked
   * history and recency.
   */
  fun recommendEvents(allEvents: List<Event>, userLikedEventIds: Set<String>): List<Event> {
    // If user has no likes, return default sort (usually chronological from repository)
    if (userLikedEventIds.isEmpty()) return allEvents

    // 1. Build User Profile (Tag Frequency Map)
    // Find all events the user has liked that are currently in the loaded list
    val likedEvents = allEvents.filter { it.eventId in userLikedEventIds }

    val userInterestProfile = mutableMapOf<String, Int>()
    likedEvents.forEach { event ->
      event.tags.forEach { tag ->
        val normalizedTag = tag.lowercase().trim()
        userInterestProfile[normalizedTag] = (userInterestProfile[normalizedTag] ?: 0) + 1
      }
    }

    // 2. Score Events
    val scoredEvents =
        allEvents.map { event ->
          var score = 0.0

          // A. Tag Matching Score
          // For every tag in the event, if the user likes it, add points based on frequency
          event.tags.forEach { tag ->
            val normalizedTag = tag.lowercase().trim()
            val interestWeight = userInterestProfile[normalizedTag] ?: 0
            score += interestWeight * TAG_MATCH_WEIGHT
          }

          // B. Recency Boost
          // Give a bonus to events created in the last 7 days to keep feed fresh
          val createdAtSeconds = event.createdAt?.seconds ?: 0L
          if (createdAtSeconds > 0) {
            val eventDate = Instant.ofEpochSecond(createdAtSeconds)
            val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)

            if (eventDate.isAfter(sevenDaysAgo)) {
              score += RECENCY_BOOST
            }
          }

          // C. User Affinity (Explicit Like)
          // Boost liked items to keep them visible.
          if (event.eventId in userLikedEventIds) {
            score += USER_AFFINITY_BOOST
          }

          event to score
        }

    // 3. Sort by Score Descending
    return scoredEvents.sortedByDescending { it.second }.map { it.first }
  }

  /** Apply current filters to the loaded events */
  fun applyFiltersToCurrentEvents(filters: EventFilters) {
    val filteredEvents = applyFiltersLocally(_allEvents.value, filters)
    _uiState.update { it.copy(events = filteredEvents) }
  }

  /**
   * Refreshes the events list manually.
   *
   * @param currentFilters Optional filter criteria to apply after refreshing. If null, empty
   *   filters are applied (showing all events).
   */
  open fun refreshEvents(currentFilters: EventFilters? = null) {
    _uiState.update { it.copy(isRefreshing = true, error = null) }

    viewModelScope.launch {
      try {
        withTimeout(10_000L) {
          coroutineScope {
            val dataDeferred = async { repository.getEventsByStatus(EventStatus.PUBLISHED).first() }
            val delayDeferred = async { delay(1000) }

            val events = dataDeferred.await()
            delayDeferred.await()

            // Re-run recommendation logic with current likes
            val likedIds = eventCardViewModel?.likedEvents?.value ?: emptySet()
            val recommended = recommendEvents(events, likedIds)

            _allEvents.value = recommended

            // Apply filters if provided, otherwise use empty filters
            val filtersToApply = currentFilters ?: EventFilters()
            val filteredEvents = applyFiltersLocally(recommended, filtersToApply)
            val limited = filteredEvents.take(LOADED_EVENTS_LIMIT)

            _uiState.update { it.copy(events = limited, isRefreshing = false, error = null) }
          }
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isRefreshing = false, error = e.message ?: "Failed to refresh events")
        }
      } finally {
        _uiState.update { it.copy(isRefreshing = false) }
      }
    }
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

  fun setShowFilterDialog(show: Boolean) {
    _uiState.update { it.copy(showFilterDialog = show) }
  }
}
