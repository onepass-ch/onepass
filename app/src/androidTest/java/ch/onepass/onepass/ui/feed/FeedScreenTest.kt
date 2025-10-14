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

  private class MockEventRepository(private val events: List<Event> = emptyList()) :
      EventRepository {
    override fun getAllEvents(): Flow<List<Event>> = flowOf(events)

    override fun searchEvents(query: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventById(eventId: String): Flow<Event?> =
        flowOf(events.find { it.eventId == eventId })

    override fun getEventsByOrganization(orgId: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventsByLocation(center: Location, radiusKm: Double): Flow<List<Event>> =
        flowOf(emptyList())

    override fun getEventsByTag(tag: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getFeaturedEvents(): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> =
        flowOf(if (status == EventStatus.PUBLISHED) events else emptyList())

    override suspend fun createEvent(event: Event): Result<String> = Result.success("test-id")

    override suspend fun updateEvent(event: Event): Result<Unit> = Result.success(Unit)

    override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
  }

  @Test
  fun feedScreen_displayAllComponents_whenEventsExist() {
    val mockRepository = MockEventRepository(listOf(testEvent1, testEvent2))
    val viewModel = FeedViewModel(mockRepository)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_LOCATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.CALENDAR_BUTTON).assertIsDisplayed()
  }

  @Test
  fun feedScreen_displayEventList_whenEventsExist() {
    val mockRepository = MockEventRepository(listOf(testEvent1, testEvent2))
    val viewModel = FeedViewModel(mockRepository)

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
    val viewModel = FeedViewModel(mockRepository)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithText("No Events Found").assertIsDisplayed()
  }

  @Test
  fun feedScreen_calendarButton_isClickable() {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository)
    var calendarClicked = false

    composeTestRule.setContent {
      OnePassTheme {
        FeedScreen(viewModel = viewModel, onNavigateToCalendar = { calendarClicked = true })
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.CALENDAR_BUTTON).performClick()

    assert(calendarClicked)
  }

  @Test
  fun feedScreen_eventCard_isClickable() {
    val mockRepository = MockEventRepository(listOf(testEvent1))
    val viewModel = FeedViewModel(mockRepository)
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
    val viewModel = FeedViewModel(mockRepository)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_LOCATION).assertTextEquals("LAUSANNE")
  }

  @Test
  fun feedScreen_displayTitleText() {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_TITLE).assertTextEquals("TODAY")
  }

  @Test
  fun feedScreen_displayMultipleEvents() {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

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
    val viewModel = FeedViewModel(mockRepository)

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
}
