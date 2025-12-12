package ch.onepass.onepass.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserSearchType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.apache.commons.text.similarity.LevenshteinDistance

/**
 * UI state for the global search screen.
 *
 * @property query Current search query.
 * @property users List of user search results.
 * @property events List of event search results.
 * @property organizations List of organization search results.
 * @property isLoading Whether a search is currently in progress.
 * @property error Error message if the search failed, null otherwise.
 */
data class GlobalSearchUiState(
    val query: String = "",
    val users: List<StaffSearchResult> = emptyList(),
    val events: List<Event> = emptyList(),
    val organizations: List<Organization> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the global search functionality across users, events, and organizations.
 *
 * @property userRepo Repository for user data operations.
 * @property eventRepo Repository for event data operations.
 * @property orgRepo Repository for organization data operations.
 */
class GlobalSearchViewModel(
    private val userRepo: UserRepository,
    private val eventRepo: EventRepository,
    private val orgRepo: OrganizationRepository,
) : ViewModel() {

  /** StateFlow holding the current UI state for global search. */
  private val _uiState = MutableStateFlow(GlobalSearchUiState())

  /** Publicly exposed StateFlow for observing the UI state. */
  val uiState: StateFlow<GlobalSearchUiState> = _uiState

  /** Variables for pagination and tracking the last fetched event. */
  private var lastEvent: Event? = null

  /** Current search query string. */
  private var currentQuery: String = ""

  /** Current page indices for events, users, and organizations. */
  private var currentEventPage = 0

  /** Current page index for users. */
  private var currentUserPage = 0

  /** Current page index for organizations. */
  private var currentOrgPage = 0

  /** Maximum number of results to fetch for each category. */
  private val MAX_EVENT_RESULTS = 3

  /** Maximum number of organization results to fetch. */
  private val MAX_ORG_RESULTS = 2

  /** Maximum number of user results to fetch. */
  private val MAX_USER_RESULTS = 3

  /**
   * Handles changes to the search query.
   *
   * @param query The new search query string.
   */
  fun onQueryChanged(query: String) {
    currentQuery = query
    lastEvent = null

    // Reset pagination
    currentEventPage = 0
    currentUserPage = 0
    currentOrgPage = 0

    // Clear previous results
    _uiState.value = GlobalSearchUiState(query = query, events = emptyList())
    fetchNextPage()
  }

  /**
   * Fetches the next page of search results based on the current query.
   *
   * This function performs fuzzy matching on event titles, user display names, and organization
   * names to find relevant results. It updates the UI state with the fetched results or any errors
   * encountered during the process.
   */
  fun fetchNextPage() {
    // Prevent multiple simultaneous loads
    if (_uiState.value.isLoading) return
    val query = _uiState.value.query.trim().lowercase()
    if (query.isBlank()) return

    // Indicate loading state
    _uiState.value = _uiState.value.copy(isLoading = true, error = null)

    // Launch a coroutine to perform the search
    viewModelScope.launch {
      try {
        val eventsDeferred: Deferred<List<Event>> = async {
          try {
            // Fetch published events and apply fuzzy matching
            val e =
                eventRepo
                    .getEventsByStatus(EventStatus.PUBLISHED)
                    .first()
                    .filter { isFuzzyMatch(it.title, query) }
                    .sortedBy { wordDistance(it.title, query) }
                    .take(MAX_EVENT_RESULTS)
            e
          } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            emptyList()
          }
        }

        val usersDeferred: Deferred<List<StaffSearchResult>> = async {
          try {
            // Fetch users and apply fuzzy matching
            val userSearch = userRepo.searchUsers(query, UserSearchType.DISPLAY_NAME, null)
            val users = userSearch.getOrElse { emptyList() }

            // Apply fuzzy matching and sorting
            users
                .filter { isFuzzyMatch(it.displayName, query) }
                .sortedBy { wordDistance(it.displayName, query) }
                .take(MAX_USER_RESULTS)
          } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            emptyList()
          }
        }

        val orgsDeferred: Deferred<List<Organization>> = async {
          try {
            // Fetch organizations and apply fuzzy matching
            orgRepo
                .searchOrganizations(query)
                .first()
                .filter { isFuzzyMatch(it.name, query) }
                .sortedBy { wordDistance(it.name, query) }
                .take(MAX_ORG_RESULTS)
          } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            emptyList()
          }
        }

        // Await all results concurrently
        val eventsResults = eventsDeferred.await()
        val usersResults = usersDeferred.await()
        val orgResults = orgsDeferred.await()

        // Update UI state with fetched results
        _uiState.value =
            _uiState.value.copy(
                events = eventsResults,
                users = usersResults,
                organizations = orgResults,
                isLoading = false)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
      }
    }
  }

  /**
   * Performs fuzzy matching between the given text and query using Levenshtein distance.
   *
   * @param text The text to match against.
   * @param query The search query.
   * @param maxDistance The maximum Levenshtein distance for a match.
   * @return True if a fuzzy match is found, false otherwise.
   */
  private fun isFuzzyMatch(text: String, query: String, maxDistance: Int = 2): Boolean {
    val normalizedText = text.lowercase().trim()
    val normalizedQuery = query.lowercase().trim()

    // Exact or prefix match
    if (normalizedText.contains(normalizedQuery) || normalizedText.startsWith(normalizedQuery))
        return true

    // Check each word separately
    val textWords = normalizedText.split("\\s+".toRegex())
    val queryWords = normalizedQuery.split("\\s+".toRegex())

    for (qWord in queryWords) {
      for (tWord in textWords) {
        // Prefix match for each word
        if (tWord.startsWith(qWord)) return true

        // Compute distance for fuzzy match
        if (wordDistance(tWord, qWord) <= maxDistance) return true
      }
    }

    return false
  }

  /**
   * Calculates the Levenshtein distance between two strings.
   *
   * @param s The first string.
   * @param t The second string.
   * @return The Levenshtein distance.
   */
  private fun wordDistance(s: String, t: String): Int {
    val d = LevenshteinDistance()
    return d.apply(s.lowercase().trim(), t.lowercase().trim())
  }

  /**
   * Factory for creating instances of [GlobalSearchViewModel].
   *
   * @param userRepo Repository for user data operations.
   * @param eventRepo Repository for event data operations.
   * @param orgRepo Repository for organization data operations.
   */
  class Factory(
      private val userRepo: UserRepository,
      private val eventRepo: EventRepository,
      private val orgRepo: OrganizationRepository,
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(GlobalSearchViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST") return GlobalSearchViewModel(userRepo, eventRepo, orgRepo) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }
}
