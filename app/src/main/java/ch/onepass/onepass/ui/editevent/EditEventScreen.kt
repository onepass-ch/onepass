package ch.onepass.onepass.ui.editevent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.ui.event.EventCard

/**
 * Test tags for the edit event screen.
 */
object EditEventScreenTestTags {
  const val EDIT_EVENT_SCREEN = "editEventScreen"
  const val EDIT_EVENT_TITLE = "editEventTitle"
  const val EVENT_LIST = "eventList"
  const val LOADING_INDICATOR = "loadingIndicator"
  const val ERROR_MESSAGE = "errorMessage"
  const val RETRY_BUTTON = "retryButton"
  const val EMPTY_STATE = "emptyState"

  fun getTestTagForEventItem(eventId: String) = "eventItem_$eventId"
}

/**
 * Edit Event Screen composable.
 *
 * Displays a list of events associated with the current user/organization.
 * Users can select an event to edit it.
 *
 * @param userId The ID of the user/organization whose events to display.
 * @param onNavigateToEditForm Callback when an event card is clicked, receives eventId.
 * @param viewModel EditEventViewModel instance, can be overridden for testing.
 * @param modifier Optional modifier for the screen.
 */
@Composable
fun EditEventScreen(
    userId: String,
    modifier: Modifier = Modifier,
    onNavigateToEditForm: (String) -> Unit = {},
    viewModel: EditEventViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()

  // Load events when screen is first displayed
  LaunchedEffect(userId) { viewModel.loadUserEvents(userId) }

  Box(
      modifier =
          modifier
              .fillMaxSize()
              .background(Color(0xFF0A0A0A))
              .testTag(EditEventScreenTestTags.EDIT_EVENT_SCREEN),
      contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize()) {
          // Title
          Text(
              text = "Edit event",
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(horizontal = 20.dp, vertical = 24.dp)
                      .testTag(EditEventScreenTestTags.EDIT_EVENT_TITLE),
              style = MaterialTheme.typography.headlineLarge,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              letterSpacing = 2.sp,
              textAlign = TextAlign.Start)

          // Content
          when {
            uiState.isLoading && uiState.events.isEmpty() -> {
              LoadingState()
            }
            uiState.error != null && uiState.events.isEmpty() -> {
              ErrorState(
                  error = uiState.error!!,
                  onRetry = { viewModel.refreshEvents(userId) })
            }
            uiState.events.isEmpty() && !uiState.isLoading -> {
              EmptyState()
            }
            else -> {
              EventListContent(events = uiState.events, onEventClick = onNavigateToEditForm)
            }
          }
        }
      }
}

/** Event list content with scrollable cards. */
@Composable
private fun EventListContent(events: List<Event>, onEventClick: (String) -> Unit) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(EditEventScreenTestTags.EVENT_LIST),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)) {
        items(items = events, key = { it.eventId }) { event ->
          EventCard(
              event = event,
              modifier =
                  Modifier.testTag(
                      EditEventScreenTestTags.getTestTagForEventItem(event.eventId)),
              onCardClick = { onEventClick(event.eventId) })
        }
      }
}

/** Loading state indicator. */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(
        modifier = Modifier.testTag(EditEventScreenTestTags.LOADING_INDICATOR),
        color = Color(0xFF841DA4))
  }
}

/** Error state with retry button. */
@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(32.dp)
              .testTag(EditEventScreenTestTags.ERROR_MESSAGE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = "Oops!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag(EditEventScreenTestTags.RETRY_BUTTON),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF841DA4), contentColor = Color.White)) {
              Text(text = "Try Again", fontWeight = FontWeight.Medium)
            }
      }
}

/** Empty state when no events are available. */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
  Column(
      modifier =
          modifier.fillMaxSize().padding(32.dp).testTag(EditEventScreenTestTags.EMPTY_STATE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = "No Events Found",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You don't have any events yet. Create your first event!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center)
      }
}

