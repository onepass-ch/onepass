package ch.onepass.onepass.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.ui.event.EventCard
import ch.onepass.onepass.ui.event.EventCardViewModel
import ch.onepass.onepass.ui.eventfilters.ActiveFiltersBar
import ch.onepass.onepass.ui.eventfilters.EventFilterViewModel
import ch.onepass.onepass.ui.eventfilters.FilterDialog

/**
 * Feed screen showing all published events. Displays a list of events with loading, error, and
 * empty states.
 */
object FeedScreenTestTags {
  const val FEED_SCREEN = "feedScreen"
  const val FEED_TOP_BAR = "feedTopBar"
  const val FEED_TITLE = "feedTitle"
  const val FEED_LOCATION = "feedLocation"
  const val FILTER_BUTTON = "filterButton"
  const val CALENDAR_BUTTON = "calendarButton"
  const val EVENT_LIST = "eventList"
  const val LOADING_INDICATOR = "loadingIndicator"
  const val ERROR_MESSAGE = "errorMessage"
  const val RETRY_BUTTON = "retryButton"
  const val EMPTY_STATE = "emptyState"
  const val ACTIVE_FILTERS_BAR = "activeFiltersBar"

  fun getTestTagForEventItem(eventId: String) = "eventItem_$eventId"
}

/**
 * Main Feed Screen composable.
 *
 * @param onNavigateToEvent Callback when an event card is clicked, receives eventId.
 * @param onNavigateToCalendar Callback when calendar button is clicked.
 * @param viewModel FeedViewModel instance, can be overridden for testing.
 * @param modifier Optional modifier for the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    onNavigateToEvent: (String) -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    viewModel: FeedViewModel = viewModel(),
    filterViewModel: EventFilterViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val currentFilters by filterViewModel.currentFilters.collectAsState()

  // Load events when screen is first displayed
  LaunchedEffect(Unit) { viewModel.loadEvents() }
  // Apply filters when they change OR when events are loaded
  LaunchedEffect(currentFilters, uiState.isLoading) {
    if (!uiState.isLoading) { // Only apply filters after events are loaded
      viewModel.applyFiltersToCurrentEvents(currentFilters)
    }
  }

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(FeedScreenTestTags.FEED_SCREEN),
      topBar = {
        Column {
          FeedTopBar(
              currentLocation = uiState.location,
              currentDateRange = "WELCOME",
              onCalendarClick = onNavigateToCalendar,
              onFilterClick = { viewModel.setShowFilterDialog(true) },
          )
          if (currentFilters.hasActiveFilters) {
            ActiveFiltersBar(
                filters = currentFilters,
                onClearFilters = { filterViewModel.clearFilters() },
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag(FeedScreenTestTags.ACTIVE_FILTERS_BAR),
            )
          }
        }
      },
      containerColor = Color(0xFF0A0A0A),
  ) { paddingValues ->
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
      when {
        uiState.isLoading && uiState.events.isEmpty() -> {
          LoadingState()
        }
        uiState.error != null && uiState.events.isEmpty() -> {
          ErrorState(error = uiState.error!!, onRetry = { viewModel.refreshEvents() })
        }
        !uiState.isLoading && uiState.events.isEmpty() -> {
          EmptyFeedState()
        }
        else -> {
          EventListContent(
              events = uiState.events,
              isLoadingMore = uiState.isLoading,
              onEventClick = onNavigateToEvent,
          )
        }
      }
      // Filter Dialog
      if (uiState.showFilterDialog) {
        // Sync localFilters to current global filters on dialog open
        LaunchedEffect(Unit) { filterViewModel.updateLocalFilters(currentFilters) }

        FilterDialog(
            viewModel = filterViewModel,
            onApply = { newFilters ->
              filterViewModel.applyFilters(newFilters)
              viewModel.setShowFilterDialog(false)
            },
            onDismiss = { viewModel.setShowFilterDialog(false) },
        )
      }
    }
  }
}

/** Top bar with title, location, and action buttons. */
@Composable
private fun FeedTopBar(
    currentLocation: String,
    currentDateRange: String,
    onCalendarClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Surface(
      modifier = modifier.fillMaxWidth().testTag(FeedScreenTestTags.FEED_TOP_BAR),
      color = Color(0xFF0A0A0A),
      tonalElevation = 0.dp,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
              text = currentDateRange,
              style = MaterialTheme.typography.headlineLarge,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              letterSpacing = 2.sp,
              modifier = Modifier.testTag(FeedScreenTestTags.FEED_TITLE),
          )
          Text(
              text = currentLocation.uppercase(),
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFF9CA3AF),
              modifier = Modifier.padding(top = 4.dp).testTag(FeedScreenTestTags.FEED_LOCATION),
          )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          // Filter Button
          IconButton(
              onClick = onFilterClick,
              modifier = Modifier.size(48.dp).testTag(FeedScreenTestTags.FILTER_BUTTON),
          ) {
            Icon(
                painter = painterResource(id = R.drawable.filter_icon),
                contentDescription = "Filter events",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
          }
          IconButton(
              onClick = onCalendarClick,
              modifier = Modifier.size(48.dp).testTag(FeedScreenTestTags.CALENDAR_BUTTON),
          ) {
            Icon(
                painter = painterResource(id = R.drawable.calendar),
                contentDescription = "Calendar view",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
          }
        }
      }
    }
  }
}

/** Event list content with scrollable cards. */
@Composable
private fun EventListContent(
    events: List<Event>,
    isLoadingMore: Boolean,
    onEventClick: (String) -> Unit,
) {
  val eventCardViewModel = EventCardViewModel.getInstance()
  val likedEvents by eventCardViewModel.likedEvents.collectAsState()

  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.EVENT_LIST),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)) {
        items(items = events, key = { it.eventId }) { event ->
          EventCard(
              event = event,
              modifier = Modifier.testTag(FeedScreenTestTags.getTestTagForEventItem(event.eventId)),
              isLiked = likedEvents.contains(event.eventId),
              onLikeToggle = { eventId -> eventCardViewModel.toggleLike(eventId) },
              onCardClick = { onEventClick(event.eventId) })
        }
        if (isLoadingMore && events.isNotEmpty()) {
          item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center) {
                  LoadingState()
                }
          }
        }
      }
}

/** Loading state indicator. */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  CircularProgressIndicator(
      modifier = modifier.testTag(FeedScreenTestTags.LOADING_INDICATOR),
      color = Color(0xFF841DA4),
  )
}

/** Error state with retry button. */
@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth().padding(32.dp).testTag(FeedScreenTestTags.ERROR_MESSAGE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = "Oops!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = error,
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF9CA3AF),
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onRetry,
        modifier = Modifier.testTag(FeedScreenTestTags.RETRY_BUTTON),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color(0xFF841DA4),
                contentColor = Color.White,
            ),
    ) {
      Text(text = "Try Again", fontWeight = FontWeight.Medium)
    }
  }
}

/** Empty state when no events are available. */
@Composable
private fun EmptyFeedState(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth().padding(32.dp).testTag(FeedScreenTestTags.EMPTY_STATE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = "No Events Found",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Check back later for new events in your area!",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF9CA3AF),
        textAlign = TextAlign.Center,
    )
  }
}
