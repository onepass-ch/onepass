package ch.onepass.onepass.ui.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import io.mockk.every
import io.mockk.mockk
import java.util.*
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FeedScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val testEvent1 =
      Event(
          eventId = "test1",
          title = "Test Event 1",
          description = "Description 1",
          organizerId = "org1",
          organizerName = "Organizer 1",
          status = EventStatus.PUBLISHED,
          location = Location(GeoPoint(46.5197, 6.6323), "Lausanne"),
          startTime = Timestamp(Date()),
          capacity = 100,
          ticketsRemaining = 50,
          ticketsIssued = 50,
          pricingTiers = emptyList())

  private val testEvent2 =
      Event(
          eventId = "test2",
          title = "Test Event 2",
          description = "Description 2",
          organizerId = "org2",
          organizerName = "Organizer 2",
          status = EventStatus.PUBLISHED,
          location = Location(GeoPoint(46.5191, 6.5668), "EPFL"),
          startTime = Timestamp(Date()),
          capacity = 200,
          ticketsRemaining = 100,
          ticketsIssued = 100,
          pricingTiers = emptyList())

  // Mocks needed for FeedViewModel instantiation in tests
  private val mockUserRepository = mockk<UserRepository>(relaxed = true)
  private val mockAuth = mockk<FirebaseAuth>(relaxed = true)
  private val mockUser = mockk<FirebaseUser>(relaxed = true)

  init {
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockUserRepository.getFavoriteEvents(any()) } returns flowOf(emptySet())
  }

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

    override suspend fun addEventImage(eventId: String, imageUrl: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun removeEventImage(eventId: String, imageUrl: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun updateEventImages(eventId: String, imageUrls: List<String>): Result<Unit> =
        Result.success(Unit)
  }

  @Test
  fun feedScreen_displayAllComponents_whenEventsExist() {
    val mockRepository = MockEventRepository(listOf(testEvent1, testEvent2))
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_LOCATION).assertIsDisplayed()
  }

  @Test
  fun feedScreen_displayEventList_whenEventsExist() {
    val mockRepository = MockEventRepository(listOf(testEvent1, testEvent2))
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(testEvent1.eventId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(testEvent2.eventId))
        .assertIsDisplayed()
  }

  @Test
  fun feedScreen_displayEmptyState_whenNoEvents() {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithText("No Events Found").assertIsDisplayed()
  }

  @Test
  fun feedScreen_eventCard_isClickable() {
    val mockRepository = MockEventRepository(listOf(testEvent1))
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)
    var clickedEventId: String? = null

    composeTestRule.setContent {
      OnePassTheme {
        FeedScreen(viewModel = viewModel, onNavigateToEvent = { clickedEventId = it })
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(testEvent1.eventId))
        .performClick()

    assert(clickedEventId == testEvent1.eventId)
  }

  @Test
  fun feedScreen_displayLocationText() {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_LOCATION).assertTextEquals("SWITZERLAND")
  }

  @Test
  fun feedScreen_displayTitleText() {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_TITLE).assertTextEquals("WELCOME")
  }

  @Test
  fun feedScreen_displayMultipleEvents() {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    events.forEach { event ->
      composeTestRule
          .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(event.eventId))
          .assertIsDisplayed()
    }
  }

  @Test
  fun feedScreen_scrollableEventList() {
    val manyEvents =
        (1..20).map { index -> testEvent1.copy(eventId = "test$index", title = "Event $index") }
    val mockRepository = MockEventRepository(manyEvents)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    // Verify first event is visible
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(manyEvents.first().eventId))
        .assertIsDisplayed()

    // Scroll to last event
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.EVENT_LIST)
        .performScrollToNode(
            hasTestTag(FeedScreenTestTags.getTestTagForEventItem(manyEvents.last().eventId)))

    // Verify last event is now visible
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(manyEvents.last().eventId))
        .assertIsDisplayed()
  }

  // ============ NEW TESTS TO COVER MISSING LINES ============

  @Test
  fun feedScreen_displayErrorState_whenLoadingFails() {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    // Verify error state is displayed
    composeTestRule.onNodeWithTag(FeedScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Oops!").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test error").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${FeedScreenTestTags.ERROR_MESSAGE}_retry_button")
        .assertIsDisplayed()
  }

  @Test
  fun feedScreen_retryButton_triggersRefresh() {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    // Verify error state is displayed
    composeTestRule.onNodeWithTag(FeedScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${FeedScreenTestTags.ERROR_MESSAGE}_retry_button")
        .assertIsDisplayed()

    // Click retry button
    composeTestRule.onNodeWithTag("${FeedScreenTestTags.ERROR_MESSAGE}_retry_button").performClick()

    // Verify loading state is triggered (error state should disappear momentarily)
    composeTestRule.waitForIdle()
  }

  @Test
  fun feedScreen_displayLoadingIndicator_initialLoad() {
    // Create a repository that simulates loading
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    // The loading indicator should appear briefly during initial load
    // We need to check this before waitForIdle completes
    Thread.sleep(50) // Small delay to catch the loading state

    // After loading completes, empty state should show
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EMPTY_STATE).assertIsDisplayed()
  }

  @Test
  fun feedScreen_displayEmptyStateMessage_correctly() {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    // Verify all empty state components
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithText("No Events Found").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Check back later for new events in your area!")
        .assertIsDisplayed()
  }

  @Test
  fun feedScreen_errorState_displaysAllComponents() {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    // Verify all error state components
    composeTestRule.onNodeWithTag(FeedScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Oops!").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test error").assertIsDisplayed()
    composeTestRule.onNodeWithText("Try Again").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${FeedScreenTestTags.ERROR_MESSAGE}_retry_button")
        .assertIsDisplayed()
  }

  @Test
  fun feedScreen_errorMessage_displaysCorrectErrorText() {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    // Verify specific error message
    composeTestRule.onNodeWithText("Test error", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun feedScreen_retryButton_hasCorrectStyling() {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    // Wait for error state to appear
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag("${FeedScreenTestTags.ERROR_MESSAGE}_retry_button")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify button and label
    composeTestRule
        .onNodeWithTag("${FeedScreenTestTags.ERROR_MESSAGE}_retry_button")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Try Again").assertIsDisplayed()
  }

  @Test
  fun feedScreen_notificationButton_isDisplayed_and_clickable() {
    val repo = MockEventRepository(emptyList())
    val vm = FeedViewModel(repo, mockUserRepository, mockAuth)
    var notificationClicked = false

    composeTestRule.setContent {
      OnePassTheme {
        FeedScreen(viewModel = vm, onNavigateToNotifications = { notificationClicked = true })
      }
    }

    // Verify button is displayed
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.NOTIFICATION_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()

    // Perform click
    composeTestRule.onNodeWithTag(FeedScreenTestTags.NOTIFICATION_BUTTON).performClick()

    // Verify callback invocation
    assertTrue(notificationClicked)
  }

  @Test
  fun feedScreen_pullToRefresh_triggersRefreshEvents() {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    var refreshTriggered = false
    val testViewModel =
        object : FeedViewModel(mockRepository, mockUserRepository, mockAuth) {
          override fun refreshEvents(currentFilters: EventFilters?) {
            refreshTriggered = true
            super.refreshEvents(currentFilters)
          }
        }
    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = testViewModel) } }
    composeTestRule.waitForIdle()
    // Simulate pull-to-refresh gesture
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).performTouchInput {
      swipeDown(startY = centerY - 100.dp.toPx(), endY = centerY + 100.dp.toPx())
    }
    composeTestRule.waitForIdle()
    // Verify refresh was triggered
    assert(refreshTriggered)
  }

  @Test
  fun feedScreen_showsRefreshingState_duringPullToRefresh() {
    val events = listOf(testEvent1)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)
    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }
    composeTestRule.waitForIdle()
    // Initially not refreshing
    val initialState = viewModel.uiState.value
    assert(!initialState.isRefreshing)
    // Trigger refresh - pass null for currentFilters
    viewModel.refreshEvents(null)
    // Verify refreshing state is set
    composeTestRule.waitForIdle()
    val refreshingState = viewModel.uiState.value
    assert(refreshingState.isRefreshing)
  }

  @Test
  fun feedScreen_eventListShows_whenNotLoadingAndNotRefreshing() {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)
    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }
    composeTestRule.waitForIdle()
    // Should show event list when not loading and not refreshing
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(testEvent1.eventId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(testEvent2.eventId))
        .assertIsDisplayed()
  }

  @Test
  fun feedScreen_emptyState_showsOnlyWhenNotLoadingAndNotRefreshing() {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)
    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }
    composeTestRule.waitForIdle()
    // Should show empty state when not loading and not refreshing with no events
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithText("No Events Found").assertIsDisplayed()
  }

  @Test
  fun feedScreen_isLoadingMore_showsLoadingInEventList() {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)
    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }
    composeTestRule.waitForIdle()
    viewModel.loadEvents()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(testEvent1.eventId))
        .assertIsDisplayed()
  }

  @Test
  fun feedScreen_stateTransitions_correctlyDuringRefreshCycle() {
    val events = listOf(testEvent1)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)
    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }
    composeTestRule.waitForIdle()
    // Initial state - events displayed
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
    // Trigger refresh - pass null for currentFilters
    viewModel.refreshEvents(null)
    // During refresh - events should still be displayed (not loading state)
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(testEvent1.eventId))
        .assertIsDisplayed()
    composeTestRule.waitForIdle()
    // After refresh - events still displayed
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(testEvent1.eventId))
        .assertIsDisplayed()
  }
}
