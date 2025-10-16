package ch.onepass.onepass.ui.editevent

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditEventScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockRepository: EventRepository
  private lateinit var viewModel: EditEventViewModel

  private val testEvents =
      listOf(
          Event(eventId = "1", title = "Event 1", organizerName = "Organizer 1"),
          Event(eventId = "2", title = "Event 2", organizerName = "Organizer 2"))

  @Before
  fun setUp() {
    mockRepository = mockk(relaxed = true)
    viewModel = EditEventViewModel(mockRepository)
  }

  @Test
  fun editEventScreen_displaysTitle() {
    composeTestRule.setContent { EditEventScreen(userId = "user-id", viewModel = viewModel) }

    composeTestRule.onNodeWithText("Edit event").assertIsDisplayed()
  }

  @Test
  fun editEventScreen_displaysLoadingState() {
    coEvery { mockRepository.getEventsByOrganization(any()) } returns flowOf(emptyList())

    composeTestRule.setContent { EditEventScreen(userId = "user-id", viewModel = viewModel) }

    // Loading indicator should appear briefly
    composeTestRule.waitForIdle()
  }

  @Test
  fun editEventScreen_displaysEmptyState() {
    coEvery { mockRepository.getEventsByOrganization(any()) } returns flowOf(emptyList())

    composeTestRule.setContent { EditEventScreen(userId = "user-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("No Events Found").assertIsDisplayed()
  }

  @Test
  fun editEventScreen_displaysEventsList() {
    coEvery { mockRepository.getEventsByOrganization("user-id") } returns flowOf(testEvents)

    composeTestRule.setContent { EditEventScreen(userId = "user-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Verify both events are displayed
    composeTestRule.onNodeWithText("Event 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Event 2").assertIsDisplayed()
  }

  @Test
  fun editEventScreen_eventCard_isClickable() {
    coEvery { mockRepository.getEventsByOrganization("user-id") } returns flowOf(testEvents)

    var clickedEventId: String? = null

    composeTestRule.setContent {
      EditEventScreen(
          userId = "user-id",
          viewModel = viewModel,
          onNavigateToEditForm = { eventId -> clickedEventId = eventId })
    }

    composeTestRule.waitForIdle()

    // Click on first event card
    composeTestRule.onNodeWithText("Event 1").performClick()

    // Verify callback was triggered with correct eventId
    assert(clickedEventId == "1")
  }

  @Test
  fun editEventScreen_displaysErrorState() {
    coEvery { mockRepository.getEventsByOrganization(any()) } throws Exception("Network error")

    composeTestRule.setContent { EditEventScreen(userId = "user-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Oops!").assertIsDisplayed()
  }

  @Test
  fun editEventScreen_retryButton_reloadsData() {
    coEvery { mockRepository.getEventsByOrganization(any()) } throws
        Exception("Network error") andThen
        flowOf(testEvents)

    composeTestRule.setContent { EditEventScreen(userId = "user-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Click retry button
    composeTestRule.onNodeWithText("Try Again").performClick()

    composeTestRule.waitForIdle()

    // Events should now be displayed
    composeTestRule.onNodeWithText("Event 1").assertExists()
  }
}
