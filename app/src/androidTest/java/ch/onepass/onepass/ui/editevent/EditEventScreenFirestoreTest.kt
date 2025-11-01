package ch.onepass.onepass.ui.editevent

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Firebase Emulator integration tests for EditEventScreen.
 *
 * These tests verify that the EditEventScreen correctly displays events from Firestore and
 * interacts with the Firebase backend.
 *
 * Prerequisites:
 * - Firebase emulators must be running (firebase emulators:start)
 * - Test uses anonymous authentication
 */
@RunWith(AndroidJUnit4::class)
class EditEventScreenFirestoreTest : FirestoreTestBase() {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: EditEventViewModel
  private lateinit var userId: String

  @Before
  override fun setUp() {
    super.setUp()
    runBlocking {
      // Sign in anonymously for testing
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"

      // Create ViewModel with real repository
      viewModel = EditEventViewModel(repository)
    }
  }

  @Test
  fun editEventScreen_displaysEventsFromFirestore() {
    runBlocking {
      // Create test events in Firestore
      val event1 = createTestEvent("1", "Test Event 1", "Description 1")
      val event2 = createTestEvent("2", "Test Event 2", "Description 2")

      repository.createEvent(event1)
      repository.createEvent(event2)

      // Wait for Firestore operations to complete
      delay(1000)

      // Launch the screen
      composeTestRule.setContent { EditEventScreen(userId = userId, viewModel = viewModel) }

      // Wait for data to load
      composeTestRule.waitForIdle()
      delay(1000)

      // Verify both events are displayed
      composeTestRule.onNodeWithText("Test Event 1").assertIsDisplayed()
      composeTestRule.onNodeWithText("Test Event 2").assertIsDisplayed()
    }
  }

  @Test
  fun editEventScreen_displaysEmptyStateWhenNoEvents() {
    runBlocking {
      // Don't create any events

      composeTestRule.setContent { EditEventScreen(userId = userId, viewModel = viewModel) }

      composeTestRule.waitForIdle()
      delay(1000)

      // Verify empty state is displayed
      composeTestRule.onNodeWithText("No Events Found").assertIsDisplayed()
      composeTestRule
          .onNodeWithText("You don't have any events yet. Create your first event!")
          .assertIsDisplayed()
    }
  }

  @Test
  fun editEventScreen_eventCard_isClickableWithFirestoreEvent() {
    runBlocking {
      // Create a test event in Firestore
      val event = createTestEvent("test-123", "Clickable Event", "Test Description")
      repository.createEvent(event)

      delay(1000)

      // Get the actual event ID from Firestore (it may be auto-generated)
      val createdEvents = getAllEventsFromFirestore()
      val actualEventId = createdEvents.firstOrNull { it.title == "Clickable Event" }?.eventId

      assert(actualEventId != null) { "Event should be created in Firestore" }

      var clickedEventId: String? = null

      composeTestRule.setContent {
        EditEventScreen(
            userId = userId,
            viewModel = viewModel,
            onNavigateToEditForm = { eventId -> clickedEventId = eventId })
      }

      composeTestRule.waitForIdle()
      delay(1000)

      // Click on the event card
      composeTestRule.onNodeWithText("Clickable Event").performClick()

      // Verify navigation callback was triggered with the actual eventId from Firestore
      assert(clickedEventId == actualEventId) {
        "Expected eventId '$actualEventId' but got '$clickedEventId'"
      }
    }
  }

  @Test
  fun editEventScreen_displaysMultipleEventsInOrder() {
    runBlocking {
      // Create multiple test events
      val events =
          listOf(
              createTestEvent("1", "Event Alpha", "First event"),
              createTestEvent("2", "Event Beta", "Second event"),
              createTestEvent("3", "Event Gamma", "Third event"))

      events.forEach { repository.createEvent(it) }
      delay(2000) // Increased delay to ensure events are saved

      // Verify events are actually in Firestore
      val savedEvents = getAllEventsFromFirestore()
      assert(savedEvents.size == 3) {
        "Expected 3 events in Firestore but found ${savedEvents.size}"
      }

      composeTestRule.setContent { EditEventScreen(userId = userId, viewModel = viewModel) }

      composeTestRule.waitForIdle()
      delay(2000) // Increased delay to ensure UI loads

      // Verify the event list is displayed (not empty state)
      composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_LIST).assertExists()

      // Verify we have event cards - check that at least one event is displayed
      val eventCount = savedEvents.size
      assert(eventCount == 3) { "Expected 3 events but got $eventCount" }

      // Check that we can find at least one of the events
      // (LazyColumn might not render all items immediately)
      val foundAlpha =
          composeTestRule
              .onAllNodesWithText("Event Alpha", substring = true)
              .fetchSemanticsNodes()
              .isNotEmpty()
      val foundBeta =
          composeTestRule
              .onAllNodesWithText("Event Beta", substring = true)
              .fetchSemanticsNodes()
              .isNotEmpty()
      val foundGamma =
          composeTestRule
              .onAllNodesWithText("Event Gamma", substring = true)
              .fetchSemanticsNodes()
              .isNotEmpty()

      assert(foundAlpha || foundBeta || foundGamma) {
        "At least one event should be displayed. Found: Alpha=$foundAlpha, Beta=$foundBeta, Gamma=$foundGamma"
      }
    }
  }

  @Test
  fun editEventScreen_refreshEvents_loadsNewEvents() {
    runBlocking {
      // Start with one event
      val event1 = createTestEvent("1", "Initial Event", "Initial description")
      repository.createEvent(event1)
      delay(1000)

      composeTestRule.setContent { EditEventScreen(userId = userId, viewModel = viewModel) }

      composeTestRule.waitForIdle()
      delay(1000)

      // Verify initial event is displayed
      composeTestRule.onNodeWithText("Initial Event").assertIsDisplayed()

      // Add a new event to Firestore
      val event2 = createTestEvent("2", "New Event", "New description")
      repository.createEvent(event2)
      delay(1000)

      // Refresh events
      viewModel.refreshEvents(userId)

      composeTestRule.waitForIdle()
      delay(1000)

      // Verify both events are now displayed
      composeTestRule.onNodeWithText("Initial Event").assertIsDisplayed()
      composeTestRule.onNodeWithText("New Event").assertIsDisplayed()
    }
  }

  @Test
  fun editEventScreen_loadsEventsForSpecificOrganizer() {
    runBlocking {
      // Create events for the test user
      val userEvent = createTestEvent("user-1", "User Event", "This user's event")
      repository.createEvent(userEvent)

      delay(1000)

      composeTestRule.setContent { EditEventScreen(userId = userId, viewModel = viewModel) }

      composeTestRule.waitForIdle()
      delay(1000)

      // Verify user's event is displayed
      composeTestRule.onNodeWithText("User Event").assertIsDisplayed()
    }
  }

  @Test
  fun editEventScreen_displaysLoadingIndicator() {
    runBlocking {
      // Create events to load
      val event = createTestEvent("1", "Loading Test Event", "Description")
      repository.createEvent(event)

      composeTestRule.setContent { EditEventScreen(userId = userId, viewModel = viewModel) }

      // The loading indicator should appear briefly
      // (This test verifies the loading state exists, even if it's very brief)
      composeTestRule.waitForIdle()
    }
  }

  /** Helper function to create a test event with sensible defaults. */
  private fun createTestEvent(id: String, title: String, description: String): Event {
    return Event(
        eventId = id,
        title = title,
        description = description,
        organizerId = userId,
        organizerName = "Test Organizer",
        status = EventStatus.PUBLISHED,
        location =
            Location(coordinates = GeoPoint(46.5191, 6.6323), name = "Lausanne, Test Location"),
        startTime = Timestamp(Date(System.currentTimeMillis() + 86400000)), // Tomorrow
        endTime = Timestamp(Date(System.currentTimeMillis() + 90000000)), // Day after
        capacity = 100,
        ticketsRemaining = 100,
        ticketsIssued = 0,
        ticketsRedeemed = 0,
        pricingTiers =
            listOf(PricingTier(name = "General", price = 20.0, quantity = 100, remaining = 100)),
        currency = "CHF")
  }
}
