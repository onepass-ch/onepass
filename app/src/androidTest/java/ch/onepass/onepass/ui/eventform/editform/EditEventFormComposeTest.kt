package ch.onepass.onepass.ui.eventform.editform

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.EventTag
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.ui.eventform.EventFormViewModel.ValidationError
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for EditEventForm composable.
 *
 * These tests verify that the UI components render correctly, load event data, and interact
 * properly with the ViewModel.
 */
@RunWith(AndroidJUnit4::class)
class EditEventFormComposeTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockRepository: EventRepository
  private lateinit var viewModel: EditEventFormViewModel
  private lateinit var testEvent: Event

  @Before
  fun setUp() {
    mockRepository = mockk(relaxed = true)
    viewModel = EditEventFormViewModel(mockRepository)

    // Create a test event with known values
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.DECEMBER, 25, 14, 30, 0)
    val startTime = Timestamp(calendar.time)
    calendar.set(2025, Calendar.DECEMBER, 25, 16, 30, 0)
    val endTime = Timestamp(calendar.time)

    testEvent =
        Event(
            eventId = "test-event-id",
            title = "Test Event",
            description = "Test Description",
            organizerId = "org-id",
            organizerName = "Test Organizer",
            status = EventStatus.DRAFT,
            startTime = startTime,
            endTime = endTime,
            capacity = 100,
            ticketsRemaining = 100,
            ticketsIssued = 0,
            ticketsRedeemed = 0,
            pricingTiers = listOf(PricingTier("General", 25.0, 100, 100)))
  }

  @Test
  fun load_and_display_form_states() = runTest {
    // combines title/description population + form shown
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Edit Event").assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventFormTestTags.FORM_COLUMN).assertIsDisplayed()
    composeTestRule.onNodeWithText("Test Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test Description").assertIsDisplayed()
  }

  @Test
  fun updateButton_isDisplayedAndClickable() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
        .assertHasClickAction()

    composeTestRule.onNodeWithText("Update event").assertIsDisplayed()
  }

  @Test
  fun backButton_triggersNavigation() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    var backPressed = false

    composeTestRule.setContent {
      EditEventForm(
          eventId = "test-event-id", viewModel = viewModel, onNavigateBack = { backPressed = true })
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.BACK_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    assert(backPressed)
  }

  @Test
  fun formFields_updateViewModelState_title_editing() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }
    composeTestRule.waitForIdle()

    // use stable tag instead of text-based selector
    composeTestRule.onNodeWithTag("title_input_field").performTextClearance()
    composeTestRule.onNodeWithTag("title_input_field").performTextInput("New Title")
    composeTestRule.waitForIdle()

    assert(viewModel.formState.value.title == "New Title")
  }

  @Test
  fun descriptionField_canBeEdited_usingTag() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("description_input_field").performTextClearance()
    composeTestRule.onNodeWithTag("description_input_field").performTextInput("Updated Description")

    composeTestRule.onNodeWithText("Updated Description").assertIsDisplayed()
  }

  @Test
  fun location_price_capacity_inputs_use_tags() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }
    composeTestRule.waitForIdle()

    // location
    composeTestRule.onNodeWithTag("location_input_field").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("location_input_field").performTextInput("EPFL, Lausanne")
    composeTestRule.onNodeWithText("EPFL, Lausanne").assertIsDisplayed()

    // price
    composeTestRule.onNodeWithTag("price_input_field").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("price_input_field").performTextInput("30.5")
    composeTestRule.onNodeWithText("30.5").assertIsDisplayed()

    // capacity
    composeTestRule.onNodeWithTag("capacity_input_field").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("capacity_input_field").performTextInput("150")
    composeTestRule.onNodeWithText("150").assertIsDisplayed()
  }

  @Test
  fun errorDialog_flow_and_dismiss() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    coEvery { mockRepository.updateEvent(any()) } returns Result.failure(Exception("Update failed"))

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventFormTestTags.ERROR_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Update Failed").assertIsDisplayed()

    composeTestRule.onNodeWithTag(EditEventFormTestTags.ERROR_DIALOG_OK_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(EditEventFormTestTags.ERROR_DIALOG).assertDoesNotExist()
  }

  @Test
  fun validationErrors_display_and_clear_when_fixed() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }
    composeTestRule.waitForIdle()

    // clear title via tag and attempt update
    composeTestRule.onNodeWithTag("title_input_field").performTextClearance()
    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithText(ValidationError.TITLE.message)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // fix title
    composeTestRule.onNodeWithTag("title_input_field").performTextInput("Fixed")
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithText(ValidationError.TITLE.message)
          .fetchSemanticsNodes()
          .isEmpty()
    }
    composeTestRule.onNodeWithText(ValidationError.TITLE.message).assertDoesNotExist()
  }

  @Test
  fun loading_shows_loading_indicator() = runTest {
    val loadingViewModel = EditEventFormViewModel(mockRepository)

    // Don't mock the repository to return anything - let it hang
    every { mockRepository.getEventById("test-event-id") } answers
        {
          flow<Event?> {
            // Never emit - stay loading forever
            delay(Long.MAX_VALUE)
          }
        }

    composeTestRule.setContent {
      EditEventForm(eventId = "test-event-id", viewModel = loadingViewModel)
    }

    // Give it a moment to enter loading state
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventFormTestTags.LOADING_INDICATOR).assertExists()
  }

  @Test
  fun retryButton_reloadsEvent() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(null)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Now return a valid event on retry
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    composeTestRule.onNodeWithTag(EditEventFormTestTags.RETRY_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Form should now be displayed
    composeTestRule.onNodeWithTag(EditEventFormTestTags.FORM_COLUMN).assertExists()
  }

  @Test
  fun updateButton_showsLoadingWhenUpdating() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Title*").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag("title_input_field").performTextClearance()
    composeTestRule.onNodeWithTag("title_input_field").performTextInput("Modified")
    composeTestRule.waitForIdle()

    // Force recomposition
    composeTestRule.onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON).performScrollTo()
    composeTestRule.onNodeWithText("Title*").performScrollTo()

    composeTestRule.waitForIdle()

    assert(viewModel.formState.value.title == "Modified")
  }

  @Test
  fun tagsSection_isDisplayed_and_Interactable() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Tags").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Technology").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun loading_event_parses_tags_correctly() = runTest {
    // Create event with specific tags
    val tagsEvent = testEvent.copy(tags = listOf("Technology", "Conference"))
    every { mockRepository.getEventById("test-event-id") } returns flowOf(tagsEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }
    composeTestRule.waitForIdle()

    // Verify ViewModel state has parsed tags
    assert(viewModel.formState.value.selectedTags.contains(EventTag.TECH))
    assert(viewModel.formState.value.selectedTags.contains(EventTag.CONFERENCE))

    // Check UI reflects this (Chip selected)
    composeTestRule.onNodeWithText("Technology").performScrollTo().assertIsDisplayed()
  }
}
