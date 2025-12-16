package ch.onepass.onepass.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import ch.onepass.onepass.utils.EventFilteringUtils.applyFiltersLocally
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * UI state for the feed screen.
 *
 * @property events List of events to display.
 * @property isLoading Whether data is currently being fetched.
 * @property error Error message if fetching failed, null otherwise.
 * @property location Current location name to display.
 * @property isShowingFavorites Whether the feed is currently showing only liked events.
 */
data class FeedUIState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val location: String = "SWITZERLAND",
    val showFilterDialog: Boolean = false,
    val isShowingFavorites: Boolean = false
)

/**
 * ViewModel for the feed screen. Implements a content-based recommender system.
 *
 * @property repository The event repository for data operations.
 * @property userRepository The user repository for accessing liked events.
 * @property auth Firebase auth for getting current user.
 * @property timeProvider TimeProvider for getting the current time, used for time-sensitive
 *   features.
 */
open class FeedViewModel(
    private val repository: EventRepository = EventRepositoryFirebase(),
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val timeProvider: TimeProvider = FirebaseTimeProvider()
) : ViewModel() {

  companion object {
    /** Maximum number of loaded events to return */
    const val LOADED_EVENTS_LIMIT = 20

    /** Recommendation Weights */
    private const val TAG_MATCH_WEIGHT = 2.0
    private const val RECENCY_BOOST = 5.0
    private const val USER_AFFINITY_BOOST = 10.0
    private const val URGENCY_BOOST = 8.0
    private const val EXPIRATION_PENALTY = 50.0
  }

  private val _uiState = MutableStateFlow(FeedUIState())
  val uiState: StateFlow<FeedUIState> = _uiState.asStateFlow()

  // Holds the raw list of all events from the repository
  private val _allEvents = MutableStateFlow<List<Event>>(emptyList())

  // Store the current filters being applied
  private var _currentFilters: EventFilters = EventFilters()

  // Store current liked events for recommendations
  private val _currentLikedEvents = MutableStateFlow<Set<String>>(emptySet())

  init {
    // Observe liked events changes from UserRepository (but don't trigger reordering)
    val uid = auth.currentUser?.uid
    if (uid != null) {
      viewModelScope.launch {
        userRepository.getFavoriteEvents(uid).collect { likedIds ->
          _currentLikedEvents.value = likedIds
          // If we are in favorites mode, we must update the list immediately when likes change
          if (_uiState.value.isShowingFavorites) {
            recalculateRecommendations()
          }
        }
      }
    }
  }

  /**
   * Loads events from the repository, combining them with user likes to generate a personalized
   * feed.
   */
  fun loadEvents() {
    _uiState.update { it.copy(isLoading = true, error = null) }

    viewModelScope.launch {
      try {
        repository.getEventsByStatus(EventStatus.PUBLISHED).collect { events ->
          // Filter out past and sold out events before storing
          val activeEvents = filterActiveEvents(events)
          _allEvents.value = activeEvents

          // Calculate recommendations and apply filters
          recalculateRecommendations()
        }
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load events") }
      }
    }
  }

  /**
   * Filters events to only show those that are:
   * - Not yet ended (based on Firebase server timestamp)
   * - Not sold out
   */
  private fun filterActiveEvents(events: List<Event>): List<Event> {
    // Use Firebase server timestamp for consistency across all devices
    val nowSeconds = timeProvider.currentTimestamp().seconds

    return events.filter { event ->
      // Check if event has ended
      val endTimeSeconds = event.endTime?.seconds ?: Long.MAX_VALUE
      val hasNotEnded = endTimeSeconds > nowSeconds

      // Check if event is not sold out
      val isNotSoldOut = !event.isSoldOut

      hasNotEnded && isNotSoldOut
    }
  }

  /** Toggles between the main recommendation feed and the favorites feed. */
  fun toggleFavoritesMode() {
    _uiState.update { it.copy(isShowingFavorites = !it.isShowingFavorites) }
    recalculateRecommendations()
  }

  /**
   * Recalculates recommendations based on current events and liked events set. Used when likes
   * change or events are updated.
   */
  private fun recalculateRecommendations() {
    val currentEvents = _allEvents.value
    if (currentEvents.isEmpty()) {
      _uiState.update { it.copy(isLoading = false, events = emptyList()) }
      return
    }

    val likedIds = _currentLikedEvents.value
    val isShowingFavorites = _uiState.value.isShowingFavorites

    val eventsToShow: List<Event> =
        if (isShowingFavorites) {
          // In Favorites Mode: Show only liked events, apply filters, sorted by start time
          val likedEvents = currentEvents.filter { it.eventId in likedIds }
          applyFiltersLocally(likedEvents, _currentFilters).sortedBy {
            it.startTime?.seconds ?: Long.MAX_VALUE
          }
        } else {
          // In Main Feed: Exclude liked events, apply recommendations
          val unlikedEvents = currentEvents.filter { it.eventId !in likedIds }
          val recommendedEvents = recommendEvents(unlikedEvents, likedIds)
          val filteredEvents = applyFiltersLocally(recommendedEvents, _currentFilters)
          filteredEvents.take(LOADED_EVENTS_LIMIT)
        }

    _uiState.update { it.copy(events = eventsToShow, isLoading = false, error = null) }
  }

  /**
   * Content-Based Recommender Algorithm. Sorts events based on tag relevance to the user's liked
   * history, recency, and urgency.
   *
   * @param candidateEvents Subset of events to score and rank (e.g., unliked events in main feed,
   *   or all events when doing background recommendations). Does not necessarily equal all loaded
   *   events.
   * @param userLikedEventIds Set of event IDs the user has previously liked.
   *
   * Uses Firebase server timestamp to avoid client-side time manipulation.
   *
   * Logic:
   * - Boosts events starting in < 24h (Urgency)
   * - Penalizes events ending in < 2h (Expiration Closing)
   */
  fun recommendEvents(candidateEvents: List<Event>, userLikedEventIds: Set<String>): List<Event> {
    val userInterestProfile = buildUserInterestProfile(userLikedEventIds)
    val now = timeProvider.currentInstant()

    val scoredEvents =
        candidateEvents.map { event ->
          event to calculateEventScore(event, userInterestProfile, now)
        }

    return scoredEvents.sortedByDescending { it.second }.map { it.first }
  }

  private fun buildUserInterestProfile(userLikedEventIds: Set<String>): Map<String, Int> {
    val allLoadedEvents = _allEvents.value
    val likedEvents = allLoadedEvents.filter { it.eventId in userLikedEventIds }

    val profile = mutableMapOf<String, Int>()
    likedEvents.forEach { event ->
      event.tags.forEach { tag ->
        val normalizedTag = tag.lowercase().trim()
        profile[normalizedTag] = (profile[normalizedTag] ?: 0) + 1
      }
    }
    return profile
  }

  private fun calculateEventScore(
      event: Event,
      userInterestProfile: Map<String, Int>,
      now: Instant
  ): Double {
    var score = 0.0

    // Tag matching
    score += calculateTagScore(event, userInterestProfile)
    // Recency boost
    score += calculateRecencyScore(event, now)
    // Urgency boost
    score += calculateUrgencyScore(event, now)
    // Expiration penalty
    score -= calculateExpirationPenalty(event, now)

    return score
  }

  private fun calculateTagScore(event: Event, userInterestProfile: Map<String, Int>): Double {
    return event.tags.sumOf { tag ->
      val normalizedTag = tag.lowercase().trim()
      val interestWeight = userInterestProfile[normalizedTag] ?: 0
      interestWeight * TAG_MATCH_WEIGHT
    }
  }

  private fun calculateRecencyScore(event: Event, now: Instant): Double {
    val createdAtSeconds = event.createdAt?.seconds ?: 0L
    if (createdAtSeconds > 0) {
      val eventDate = Instant.ofEpochSecond(createdAtSeconds)
      val sevenDaysAgo = now.minus(7, ChronoUnit.DAYS)
      if (eventDate.isAfter(sevenDaysAgo)) {
        return RECENCY_BOOST
      }
    }
    return 0.0
  }

  private fun calculateUrgencyScore(event: Event, now: Instant): Double {
    val startTimeSeconds = event.startTime?.seconds ?: 0L
    if (startTimeSeconds > 0) {
      val startDate = Instant.ofEpochSecond(startTimeSeconds)
      val tomorrow = now.plus(24, ChronoUnit.HOURS)
      if (startDate.isAfter(now) && startDate.isBefore(tomorrow)) {
        return URGENCY_BOOST
      }
    }
    return 0.0
  }

  private fun calculateExpirationPenalty(event: Event, now: Instant): Double {
    val endTimeSeconds = event.endTime?.seconds ?: 0L
    if (endTimeSeconds > 0) {
      val endDate = Instant.ofEpochSecond(endTimeSeconds)
      val twoHoursFromNow = now.plus(2, ChronoUnit.HOURS)
      if (endDate.isAfter(now) && endDate.isBefore(twoHoursFromNow)) {
        return EXPIRATION_PENALTY
      }
    }
    return 0.0
  }

  /** Apply current filters to the loaded events */
  fun applyFiltersToCurrentEvents(filters: EventFilters) {
    _currentFilters = filters
    recalculateRecommendations()
  }

  /**
   * Refreshes the events list manually.
   *
   * @param currentFilters Optional filter criteria to apply after refreshing. If null, empty
   *   filters are applied (showing all events).
   */
  open fun refreshEvents(currentFilters: EventFilters? = null) {
    _uiState.update { it.copy(isRefreshing = true, error = null) }

    if (currentFilters != null) {
      _currentFilters = currentFilters
    }

    viewModelScope.launch {
      try {
        withTimeout(10_000L) {
          coroutineScope {
            val dataDeferred = async { repository.getEventsByStatus(EventStatus.PUBLISHED).first() }
            val delayDeferred = async { delay(1000) }

            val events = dataDeferred.await()
            delayDeferred.await()

            // Filter out past and sold out events before storing
            val activeEvents = filterActiveEvents(events)
            _allEvents.value = activeEvents

            // Recalculate recommendations with current likes
            recalculateRecommendations()

            _uiState.update { it.copy(isRefreshing = false, error = null) }
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
