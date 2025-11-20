package ch.onepass.onepass.ui.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class FeedScreenTest {
  @get:Rule val composeTestRule = createComposeRule()
  private val baseEvent =
      Event(
          eventId = "test1",
          title = "Test Event",
          description = "Description",
          organizerId = "org1",
          organizerName = "Organizer",
          status = EventStatus.PUBLISHED,
          location = Location(GeoPoint(46.5197, 6.6323), "Lausanne"),
          startTime = Timestamp(Date()),
          capacity = 100,
          ticketsRemaining = 50,
          ticketsIssued = 50,
          pricingTiers = emptyList())

  private class MockEventRepository(
      private val events: List<Event> = emptyList(),
      private val shouldThrowError: Boolean = false
  ) : EventRepository {
    override fun getAllEvents(): Flow<List<Event>> = flowOf(events)

    override fun searchEvents(query: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventById(eventId: String): Flow<Event?> =
        flowOf(events.find { it.eventId == eventId })

    override fun getEventsByOrganization(orgId: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventsByLocation(center: Location, radiusKm: Double): Flow<List<Event>> =
        flowOf(emptyList())

    override fun getEventsByTag(tag: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getFeaturedEvents(): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> {
      if (shouldThrowError) {
        throw Exception("Test error")
      }
      return flowOf(if (status == EventStatus.PUBLISHED) events else emptyList())
    }

    override suspend fun createEvent(event: Event): Result<String> = Result.success("test-id")

    override suspend fun updateEvent(event: Event): Result<Unit> = Result.success(Unit)

    override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
  }

  @Test
  fun events_display_and_interactions_including_scroll() {
    val many = (1..20).map { idx -> baseEvent.copy(eventId = "e$idx", title = "Event $idx") }
    val repo = MockEventRepository(many)
    val vm = FeedViewModel(repo)
    var clickedEventId: String? = null

    composeTestRule.setContent {
      OnePassTheme { FeedScreen(viewModel = vm, onNavigateToEvent = { clickedEventId = it }) }
    }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_TITLE).assertTextEquals("WELCOME")
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_LOCATION).assertTextEquals("SWITZERLAND")

    val firstId = many.first().eventId
    composeTestRule.onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(firstId)).performClick()
    assert(clickedEventId == firstId)

    val lastId = many.last().eventId
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag(FeedScreenTestTags.getTestTagForEventItem(lastId)))
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(lastId))
        .assertIsDisplayed()
  }

  @Test
  fun empty_and_loading_state_messages() {
    val repo = MockEventRepository(emptyList())
    val vm = FeedViewModel(repo)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = vm) } }

    Thread.sleep(50)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithText("No Events Found").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Check back later for new events in your area!")
        .assertIsDisplayed()
  }

  @Test
  fun error_state_and_retry_button_behaviour_and_styling() {
    val repo = MockEventRepository(shouldThrowError = true)
    val vm = FeedViewModel(repo)
    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = vm) } }
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.ERROR_MESSAGE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(FeedScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Oops!").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test error").assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.RETRY_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithText("Try Again").assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.RETRY_BUTTON).performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun filterDialog_open_and_dismiss_via_viewmodel() {
    val repo = MockEventRepository(emptyList())
    val vm = FeedViewModel(repo)
    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = vm) } }
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FILTER_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) { vm.uiState.value.showFilterDialog }
    vm.setShowFilterDialog(false)
    composeTestRule.waitUntil(timeoutMillis = 5_000) { !vm.uiState.value.showFilterDialog }
  }
}
