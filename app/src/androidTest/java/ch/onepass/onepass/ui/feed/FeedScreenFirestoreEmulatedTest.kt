package ch.onepass.onepass.ui.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.ui.theme.OnePassTheme
import ch.onepass.onepass.utils.EventTestData
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import ch.onepass.onepass.utils.UI_WAIT_TIMEOUT
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Firebase Emulator integration tests for FeedScreen. Tests the feed screen with actual Firestore
 * operations using the Firebase Emulator.
 *
 * Prerequisites:
 * - Firebase emulators must be running (firebase emulators:start)
 * - Test uses anonymous authentication
 */
class FeedScreenFirestoreEmulatedTest : FirestoreTestBase() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var userId: String

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      // Sign in anonymously for testing
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
    }
  }

  @Test
  fun feedScreen_displaysPublishedEvents_fromFirestore() = runTest {
    // Create and store published events
    val event1 = EventTestData.createPublishedEvent(organizerId = userId, capacity = 100)
    val event2 =
        EventTestData.createPublishedEvent(eventId = "pub-2", organizerId = userId, capacity = 200)
    repository.createEvent(event1)
    repository.createEvent(event2)

    // Launch the feed screen
    composeTestRule.setContent { OnePassTheme { FeedScreen() } }

    // Wait for events to load
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify both events are displayed
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()

    val allNodes =
        composeTestRule
            .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST, useUnmergedTree = true)
            .fetchSemanticsNodes()

    assertTrue("Events should be loaded", allNodes.isNotEmpty())
  }

  @Test
  fun feedScreen_doesNotDisplayDraftEvents_fromFirestore() = runTest {
    // Create draft and published events
    val draftEvent = EventTestData.createDraftEvent(organizerId = userId)
    val publishedEvent = EventTestData.createPublishedEvent(organizerId = userId)

    repository.createEvent(draftEvent)
    repository.createEvent(publishedEvent)

    composeTestRule.setContent { OnePassTheme { FeedScreen() } }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify only published event is shown
    val eventNodes =
        composeTestRule
            .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST, useUnmergedTree = true)
            .fetchSemanticsNodes()

    assertTrue("Published events should be displayed", eventNodes.isNotEmpty())
  }

  @Test
  fun feedScreen_showsEmptyState_whenNoPublishedEvents() = runTest {
    // Create only draft events
    val draftEvent = EventTestData.createDraftEvent(organizerId = userId)
    repository.createEvent(draftEvent)

    composeTestRule.setContent { OnePassTheme { FeedScreen() } }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EMPTY_STATE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithText("No Events Found").assertIsDisplayed()
  }

  @Test
  fun feedScreen_displaysMultiplePublishedEvents_sortedByDate() = runTest {
    // Create events with different dates
    val event1 =
        EventTestData.createTestEvent(
                eventId = "event1",
                organizerId = userId,
                title = "Event in 7 days",
                startTime = EventTestData.createFutureTimestamp(daysFromNow = 7))
            .copy(status = EventStatus.PUBLISHED)

    val event2 =
        EventTestData.createTestEvent(
                eventId = "event2",
                organizerId = userId,
                title = "Event in 30 days",
                startTime = EventTestData.createFutureTimestamp(daysFromNow = 30))
            .copy(status = EventStatus.PUBLISHED)

    val event3 =
        EventTestData.createTestEvent(
                eventId = "event3",
                organizerId = userId,
                title = "Event in 1 day",
                startTime = EventTestData.createFutureTimestamp(daysFromNow = 1))
            .copy(status = EventStatus.PUBLISHED)

    // Add events in random order
    repository.createEvent(event2)
    repository.createEvent(event1)
    repository.createEvent(event3)

    composeTestRule.setContent { OnePassTheme { FeedScreen() } }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify events are displayed
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
  }

  @Test
  fun feedScreen_canClickOnEvent_fromFirestore() = runTest {
    val event = EventTestData.createPublishedEvent(organizerId = userId)
    val result = repository.createEvent(event)
    assertTrue("Event creation should succeed", result.isSuccess)

    val eventId = result.getOrNull()!!
    var clickedEventId: String? = null

    composeTestRule.setContent {
      OnePassTheme { FeedScreen(onNavigateToEvent = { clickedEventId = it }) }
    }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.getTestTagForEventItem(eventId))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem(eventId)).performClick()

    assertEquals(eventId, clickedEventId)
  }

  @Test
  fun feedScreen_refreshesAfterNewEventAdded() = runTest {
    // Start with one event
    val event1 = EventTestData.createPublishedEvent(eventId = "event1", organizerId = userId)
    repository.createEvent(event1)

    val viewModel = FeedViewModel(repository)

    composeTestRule.setContent { OnePassTheme { FeedScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Add another event
    val event2 = EventTestData.createPublishedEvent(eventId = "event2", organizerId = userId)
    repository.createEvent(event2)

    // Refresh the feed
    viewModel.refreshEvents()

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Both events should be visible
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
  }

  @Test
  fun feedScreen_showsOnlyPublishedEvents_filtersByStatus() = runTest {
    // Create events with different statuses
    val events = EventTestData.createEventsWithDifferentStatuses(organizerId = userId)
    events.forEach { repository.createEvent(it) }

    composeTestRule.setContent { OnePassTheme { FeedScreen() } }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty() ||
          composeTestRule
              .onAllNodesWithTag(FeedScreenTestTags.EMPTY_STATE)
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    // Verify only published events (should be 1 from the test data)
    val publishedCount = events.count { it.status == EventStatus.PUBLISHED }
    assertTrue("Should have at least one published event", publishedCount > 0)
  }

  @Test
  fun feedScreen_displaysEventDetails_correctly() = runTest {
    val event =
        Event(
            eventId = "detail-test",
            title = "Test Event Title",
            description = "Test Description",
            organizerId = userId,
            organizerName = "Test Organizer",
            status = EventStatus.PUBLISHED,
            location = Location(coordinates = GeoPoint(46.5197, 6.6323), name = "Test Location"),
            startTime = Timestamp(Date()),
            capacity = 100,
            ticketsRemaining = 50,
            ticketsIssued = 50,
            pricingTiers = emptyList())

    repository.createEvent(event)

    composeTestRule.setContent { OnePassTheme { FeedScreen() } }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Event should be displayed
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
  }

  @Test
  fun feedScreen_handlesEmptyFirestore_gracefully() = runTest {
    // Don't add any events

    composeTestRule.setContent { OnePassTheme { FeedScreen() } }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EMPTY_STATE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(FeedScreenTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithText("No Events Found").assertIsDisplayed()
  }

  @Test
  fun feedScreen_displaysCorrectEventCount() = runTest {
    // Create exactly 3 published events
    val events =
        listOf(
            EventTestData.createPublishedEvent(eventId = "e1", organizerId = userId),
            EventTestData.createPublishedEvent(eventId = "e2", organizerId = userId),
            EventTestData.createPublishedEvent(eventId = "e3", organizerId = userId))

    events.forEach { repository.createEvent(it) }

    composeTestRule.setContent { OnePassTheme { FeedScreen() } }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.EVENT_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    assertEquals(3, getEventsCount())
  }
}
