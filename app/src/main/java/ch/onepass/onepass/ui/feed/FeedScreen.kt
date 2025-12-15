package ch.onepass.onepass.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.ui.components.common.EmptyState
import ch.onepass.onepass.ui.components.common.ErrorState
import ch.onepass.onepass.ui.components.common.LoadingState
import ch.onepass.onepass.ui.event.EventCard
import ch.onepass.onepass.ui.event.EventCardViewModel
import ch.onepass.onepass.ui.eventfilters.ActiveFiltersBar
import ch.onepass.onepass.ui.eventfilters.EventFilterViewModel
import ch.onepass.onepass.ui.eventfilters.FilterDialog
import ch.onepass.onepass.ui.feed.FeedScreenTestTags.getTestTagForEventItem
import ch.onepass.onepass.ui.feed.FeedScreenTestTags.getTestTagForSearchEvent
import ch.onepass.onepass.ui.feed.FeedScreenTestTags.getTestTagForSearchOrg
import ch.onepass.onepass.ui.feed.FeedScreenTestTags.getTestTagForSearchUser
import ch.onepass.onepass.ui.organization.OrganizationCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Feed screen showing all published events. Displays a list of events with loading, error, and
 * empty states.
 */
object FeedScreenTestTags {
  const val FEED_SCREEN = "feedScreen"
  const val FEED_TOP_BAR = "feedTopBar"
  const val SEARCH_TEXT_FIELD = "feedSearchTextField"
  const val FILTER_BUTTON = "filterButton"
  const val NOTIFICATION_BUTTON = "notificationButton"
  const val FAVORITES_BUTTON = "favoritesButton"
  const val EVENT_LIST = "eventList"
  const val LOADING_INDICATOR = "loadingIndicator"
  const val ERROR_MESSAGE = "errorMessage"
  const val RETRY_BUTTON = "retryButton"
  const val EMPTY_STATE = "emptyState"
  const val ACTIVE_FILTERS_BAR = "activeFiltersBar"

  /** Returns a unique test tag for a user search result with the given [userId]. */
  fun getTestTagForSearchUser(userId: String) = "searchUser_$userId"

  /** Returns a unique test tag for an event search result with the given [eventId]. */
  fun getTestTagForSearchEvent(eventId: String) = "searchEvent_$eventId"

  /** Returns a unique test tag for an organization search result with the given [orgId]. */
  fun getTestTagForSearchOrg(orgId: String) = "searchOrg_$orgId"

  /** Returns a unique test tag for an event item in the feed with the given [eventId]. */
  fun getTestTagForEventItem(eventId: String) = "eventItem_$eventId"
}

/**
 * Represents a click action on a global search result item.
 *
 * This sealed class defines the different types of clickable items that may appear in a global
 * search list, allowing callers to handle each item type explicitly using `when` expressions.
 */
sealed class GlobalSearchItemClick {
  data class UserClick(val userId: String) : GlobalSearchItemClick()

  data class EventClick(val eventId: String) : GlobalSearchItemClick()

  data class OrganizationClick(val organizationId: String) : GlobalSearchItemClick()
}

/**
 * Listener for click events on global search result items.
 *
 * Implementations of this functional interface receive a [GlobalSearchItemClick] instance
 * describing which item was clicked.
 */
fun interface GlobalSearchItemClickListener {
  /**
   * Called when a global search item is clicked.
   *
   * @param click The click event describing the clicked item.
   */
  fun onItemClick(click: GlobalSearchItemClick)
}

/**
 * Main Feed Screen composable.
 *
 * @param modifier Optional modifier for the screen.
 * @param onNavigateToEvent Callback when an event card is clicked, receives eventId.
 * @param onNavigateToNotifications Callback when the notification button is clicked.
 * @param viewModel FeedViewModel instance, can be overridden for testing.
 * @param filterViewModel EventFilterViewModel instance, providing filter logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    onNavigateToEvent: (String) -> Unit = {},
    globalSearchItemClickListener: GlobalSearchItemClickListener? = null,
    onNavigateToNotifications: () -> Unit = {},
    viewModel: FeedViewModel = viewModel(),
    filterViewModel: EventFilterViewModel = viewModel(),
    eventCardViewModel: EventCardViewModel = viewModel(),
    globalSearchViewModel: GlobalSearchViewModel? = null
) {
  // 1. State Collection
  val uiState by viewModel.uiState.collectAsState()
  val currentFilters by filterViewModel.currentFilters.collectAsState()
  val likedEvents by eventCardViewModel.likedEvents.collectAsState(emptySet())
  val searchState = globalSearchViewModel?.uiState?.collectAsState()?.value
  var searchQuery by remember { mutableStateOf("") }

  // LazyList state for controlling scroll position
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val pullState = rememberPullToRefreshState()

  // 2. Side effects
  RememberFeedScreenSideEffects(
      uiState = uiState,
      currentFilters = currentFilters,
      viewModel = viewModel,
      listState = listState,
      coroutineScope = coroutineScope)

  // 3. UI Structure
  Scaffold(
      modifier = modifier.fillMaxSize().testTag(FeedScreenTestTags.FEED_SCREEN),
      topBar = {
        FeedScreenTopBar(
            uiState = uiState,
            currentFilters = currentFilters,
            searchQuery = searchQuery,
            onSearchChanged = { newQuery ->
              searchQuery = newQuery
              globalSearchViewModel?.onQueryChanged(newQuery)
            },
            globalSearchViewModel = globalSearchViewModel,
            onFilterClick = { viewModel.setShowFilterDialog(true) },
            onNotificationClick = onNavigateToNotifications,
            onFavoritesClick = { viewModel.toggleFavoritesMode() },
            onClearFilters = { filterViewModel.clearFilters() })
      },
      containerColor = colorScheme.background,
  ) { paddingValues ->
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refreshEvents,
        state = pullState,
        modifier = Modifier.fillMaxSize().padding(paddingValues),
    ) {
      FeedContentStateSwitcher(
          uiState = uiState,
          onNavigateToEvent = onNavigateToEvent,
          onRetry = { viewModel.refreshEvents() },
          eventCardViewModel = eventCardViewModel,
          listState = listState,
          searchQuery = searchQuery,
          searchState = searchState,
          globalSearchViewModel = globalSearchViewModel,
          globalSearchItemClickListener = globalSearchItemClickListener,
          likedEvents = likedEvents)
    }

    // 4. Dialog Display
    FeedFilterDialog(
        showDialog = uiState.showFilterDialog,
        currentFilters = currentFilters,
        filterViewModel = filterViewModel,
        onApply = { newFilters ->
          filterViewModel.applyFilters(newFilters)
          viewModel.setShowFilterDialog(false)
        },
        onDismiss = { viewModel.setShowFilterDialog(false) })
  }
}

/**
 * Encapsulates LaunchedEffect side effects for FeedScreen. Performs:
 * - initial load
 * - apply filters when ready
 * - scroll-to-top when a refresh completes
 */
@Composable
private fun RememberFeedScreenSideEffects(
    uiState: FeedUIState,
    currentFilters: EventFilters,
    viewModel: FeedViewModel,
    listState: LazyListState,
    coroutineScope: CoroutineScope
) {
  // Track previous refresh state to detect when refresh completes
  var wasRefreshing by remember { mutableStateOf(false) }

  // Load events when screen is first displayed
  LaunchedEffect(Unit) { viewModel.loadEvents() }

  // Apply filters when they change OR when events are loaded
  LaunchedEffect(currentFilters, uiState.isLoading) {
    if (!uiState.isLoading) { // Only apply filters after events are loaded
      viewModel.applyFiltersToCurrentEvents(currentFilters)
    }
  }

  // Scroll to top when refresh completes
  LaunchedEffect(uiState.isRefreshing) {
    if (wasRefreshing && !uiState.isRefreshing) {
      // Refresh just completed, scroll to top smoothly
      coroutineScope.launch { listState.animateScrollToItem(0) }
    }
    wasRefreshing = uiState.isRefreshing
  }
}

/** Top bar content including the title, location, action buttons, and active filters bar. */
@Composable
private fun FeedScreenTopBar(
    uiState: FeedUIState,
    currentFilters: EventFilters,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    globalSearchViewModel: GlobalSearchViewModel?,
    onFilterClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onClearFilters: () -> Unit
) {
  Column {
    FeedTopBar(
        searchQuery = searchQuery,
        onSearchChanged = { newQuery ->
          onSearchChanged(newQuery)
          globalSearchViewModel?.onQueryChanged(newQuery)
        },
        isShowingFavorites = uiState.isShowingFavorites,
        onFilterClick = onFilterClick,
        onNotificationClick = onNotificationClick,
        onFavoritesClick = onFavoritesClick,
    )

    if (currentFilters.hasActiveFilters) {
      ActiveFiltersBar(
          filters = currentFilters,
          onClearFilters = onClearFilters,
          modifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 8.dp)
                  .testTag(FeedScreenTestTags.ACTIVE_FILTERS_BAR))
    }
  }
}

/** Handles the conditional display of Loading, Error, Empty, and Content states. */
@Composable
private fun FeedContentStateSwitcher(
    uiState: FeedUIState,
    onNavigateToEvent: (String) -> Unit,
    onRetry: () -> Unit,
    eventCardViewModel: EventCardViewModel,
    listState: LazyListState,
    searchQuery: String,
    searchState: GlobalSearchUiState?,
    globalSearchViewModel: GlobalSearchViewModel?,
    globalSearchItemClickListener: GlobalSearchItemClickListener?,
    likedEvents: Set<String>
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.EVENT_LIST),
      state = listState,
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Branch: Search mode is active (non-blank query with search viewmodel)
        if (globalSearchViewModel != null && searchQuery.isNotBlank()) {
          SearchContent(
              searchState = searchState,
              globalSearchItemClickListener = globalSearchItemClickListener,
              eventCardViewModel = eventCardViewModel,
              likedEvents = likedEvents,
              onNavigateToEvent = onNavigateToEvent)
        } else {
          // Branch: Normal feed mode
          FeedContent(
              uiState = uiState,
              onRetry = onRetry,
              eventCardViewModel = eventCardViewModel,
              likedEvents = likedEvents,
              onNavigateToEvent = onNavigateToEvent)
        }
      }
}

/** Displays search results including users, events, and organizations. */
private fun LazyListScope.SearchContent(
    searchState: GlobalSearchUiState?,
    globalSearchItemClickListener: GlobalSearchItemClickListener?,
    eventCardViewModel: EventCardViewModel,
    likedEvents: Set<String>,
    onNavigateToEvent: (String) -> Unit
) {
  when {
    searchState?.isLoading == true -> {
      item { CenteredLoadingState(testTag = FeedScreenTestTags.LOADING_INDICATOR) }
    }
    searchState?.error != null -> {
      item {
        Text(
            text = "Error: ${searchState.error}",
            color = colorScheme.error,
            modifier = Modifier.padding(16.dp).testTag(FeedScreenTestTags.ERROR_MESSAGE))
      }
    }
    else -> {
      SearchResultsContent(
          searchState = searchState,
          globalSearchItemClickListener = globalSearchItemClickListener,
          eventCardViewModel = eventCardViewModel,
          likedEvents = likedEvents,
          onNavigateToEvent = onNavigateToEvent)
    }
  }
}

/** Displays the actual search results (users, events, organizations). */
private fun LazyListScope.SearchResultsContent(
    searchState: GlobalSearchUiState?,
    globalSearchItemClickListener: GlobalSearchItemClickListener?,
    eventCardViewModel: EventCardViewModel,
    likedEvents: Set<String>,
    onNavigateToEvent: (String) -> Unit
) {
  // User search results
  searchState?.users?.let { users ->
    if (users.isNotEmpty()) {
      items(users) { user ->
        UserSearchItem(
            user,
            globalSearchItemClickListener,
            modifier = Modifier.testTag(getTestTagForSearchUser(user.id)))
      }
    }
  }

  // Event search results
  searchState?.events?.let { events ->
    if (events.isNotEmpty()) {
      items(events) { event ->
        EventCard(
            event = event,
            isLiked = likedEvents.contains(event.eventId),
            onLikeToggle = { eventCardViewModel.toggleLike(event.eventId) },
            onCardClick = { onNavigateToEvent(event.eventId) },
            modifier = Modifier.testTag(getTestTagForSearchEvent(event.eventId)))
      }
    }
  }

  // Organisation search results
  searchState?.organizations?.let { orgs ->
    if (orgs.isNotEmpty()) {
      items(orgs) { org ->
        OrganizationCard(
            organization = org,
            onClick = {
              globalSearchItemClickListener?.onItemClick(
                  GlobalSearchItemClick.OrganizationClick(org.id))
            },
            modifier = Modifier.testTag(getTestTagForSearchOrg(org.id)))
      }
    }
  }

  // Text when no result is found
  if (searchState?.users.isNullOrEmpty() &&
      searchState?.events.isNullOrEmpty() &&
      searchState?.organizations.isNullOrEmpty()) {
    item { Text("No results found", modifier = Modifier.padding(16.dp)) }
  }
}

/** Displays normal feed content with loading, error, empty, and event list states. */
private fun LazyListScope.FeedContent(
    uiState: FeedUIState,
    onRetry: () -> Unit,
    eventCardViewModel: EventCardViewModel,
    likedEvents: Set<String>,
    onNavigateToEvent: (String) -> Unit
) {
  when {
    // Initial loading state (only show when not refreshing to avoid duplicate indicators)
    uiState.isLoading && uiState.events.isEmpty() && !uiState.isRefreshing -> {
      item { CenteredLoadingState(testTag = FeedScreenTestTags.LOADING_INDICATOR) }
    }
    // Error state (only show when we have no events to display)
    uiState.error != null && uiState.events.isEmpty() -> {
      item {
        ErrorState(
            error = uiState.error, onRetry = onRetry, testTag = FeedScreenTestTags.ERROR_MESSAGE)
      }
    }
    // Empty state (only when not loading/refreshing and truly empty)
    !uiState.isLoading && !uiState.isRefreshing && uiState.events.isEmpty() -> {
      item {
        EmptyState(
            title = if (uiState.isShowingFavorites) "No Favorites" else "No Events Found",
            message =
                if (uiState.isShowingFavorites) "You haven't liked any events yet."
                else "Check back later for new events in your area!",
            testTag = FeedScreenTestTags.EMPTY_STATE)
      }
    }
    // Normal content display (handles both initial load and refresh scenarios)
    uiState.events.isNotEmpty() -> {
      EventListContent(
          events = uiState.events,
          isLoadingMore = uiState.isLoading && !uiState.isRefreshing,
          eventCardViewModel = eventCardViewModel,
          likedEvents = likedEvents,
          onNavigateToEvent = onNavigateToEvent)
    }
  }
}

/** Displays the list of events with optional loading indicator at the bottom. */
private fun LazyListScope.EventListContent(
    events: List<Event>,
    isLoadingMore: Boolean,
    eventCardViewModel: EventCardViewModel,
    likedEvents: Set<String>,
    onNavigateToEvent: (String) -> Unit
) {
  items(events) { event ->
    EventCard(
        event = event,
        isLiked = likedEvents.contains(event.eventId),
        onLikeToggle = { eventCardViewModel.toggleLike(event.eventId) },
        onCardClick = { onNavigateToEvent(event.eventId) },
        modifier = Modifier.testTag(getTestTagForEventItem(event.eventId)))
  }

  // Show loading indicator at bottom when loading more (not during refresh)
  if (isLoadingMore) {
    item {
      Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        LoadingState()
      }
    }
  }
}

/** Helper composable to show a centered loading indicator. */
@Composable
private fun CenteredLoadingState(testTag: String) {
  Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
    LoadingState(modifier = Modifier.testTag(testTag))
  }
}

/** Logic and display for the filter dialog. */
@Composable
private fun FeedFilterDialog(
    showDialog: Boolean,
    currentFilters: EventFilters,
    filterViewModel: EventFilterViewModel,
    onApply: (EventFilters) -> Unit,
    onDismiss: () -> Unit,
) {
  if (showDialog) {
    // Sync localFilters to current global filters on dialog open
    LaunchedEffect(Unit) { filterViewModel.updateLocalFilters(currentFilters) }

    FilterDialog(
        viewModel = filterViewModel,
        onApply = onApply,
        onDismiss = onDismiss,
    )
  }
}

/**
 * Displays a single user item in the global search result list.
 *
 * @param user The user data to display.
 * @param globalSearchItemClickListener Callback invoked when the user item is clicked.
 * @param modifier Modifier for customizing layout or semantics.
 */
@Composable
private fun UserSearchItem(
    user: StaffSearchResult,
    globalSearchItemClickListener: GlobalSearchItemClickListener?,
    modifier: Modifier = Modifier
) {
  Text(
      text = user.displayName,
      modifier =
          modifier.fillMaxWidth().padding(8.dp).clickable {
            globalSearchItemClickListener?.onItemClick(GlobalSearchItemClick.UserClick(user.id))
          })
}

/**
 * Top bar with title, location, and action buttons.
 *
 * @param searchQuery Current text shown in the search field.
 * @param onSearchChanged Callback invoked when the search text changes.
 * @param isShowingFavorites Boolean indicating if favorites mode is active.
 * @param onFilterClick Callback invoked when the filter button is clicked.
 * @param onNotificationClick Callback invoked when the notification button is clicked.
 * @param onFavoritesClick Callback invoked when the favorites button is clicked.
 * @param modifier Optional modifier for the top bar.
 */
@Composable
private fun FeedTopBar(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    isShowingFavorites: Boolean,
    onFilterClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Surface(
      modifier = modifier.fillMaxWidth().testTag(FeedScreenTestTags.FEED_TOP_BAR),
      color = colorScheme.background,
      tonalElevation = 0.dp,
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
              value = searchQuery,
              onValueChange = onSearchChanged,
              placeholder = { Text("Search...") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag(FeedScreenTestTags.SEARCH_TEXT_FIELD),
              shape = RoundedCornerShape(10.dp),
              keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
              colors =
                  OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = colorScheme.primary,
                      unfocusedBorderColor = colorScheme.onBackground,
                      cursorColor = colorScheme.onBackground,
                      disabledBorderColor = colorScheme.primary),
          )

          Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onFavoritesClick,
                    modifier = Modifier.size(48.dp).testTag(FeedScreenTestTags.FAVORITES_BUTTON)) {
                      Icon(
                          imageVector =
                              if (isShowingFavorites) Icons.Filled.Favorite
                              else Icons.Outlined.FavoriteBorder,
                          contentDescription = "Favorites",
                          tint = colorScheme.onBackground,
                          modifier = Modifier.size(24.dp))
                    }

                IconButton(
                    onClick = onNotificationClick,
                    modifier =
                        Modifier.size(48.dp).testTag(FeedScreenTestTags.NOTIFICATION_BUTTON)) {
                      Icon(
                          imageVector = Icons.Default.Notifications,
                          contentDescription = "Notifications",
                          tint = colorScheme.onBackground,
                          modifier = Modifier.size(24.dp))
                    }

                IconButton(
                    onClick = onFilterClick,
                    modifier = Modifier.size(48.dp).testTag(FeedScreenTestTags.FILTER_BUTTON)) {
                      Icon(
                          painter = painterResource(id = R.drawable.filter_icon),
                          contentDescription = "Filter events",
                          tint = colorScheme.onBackground,
                          modifier = Modifier.size(24.dp))
                    }
              }
        }
  }
}
