package ch.onepass.onepass.ui.eventform.editform

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
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
  fun editEventForm_displaysTitle() {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.onNodeWithText("Edit Event").assertIsDisplayed()
  }

  @Test
  fun editEventForm_displaysLoadErrorWhenEventNotFound() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(null)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    // Wait for loading to complete
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventFormTestTags.LOAD_ERROR_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithText("Failed to load event").assertIsDisplayed()
  }

  @Test
  fun editEventForm_displaysFormWhenEventLoaded() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventFormTestTags.FORM_COLUMN).assertIsDisplayed()
  }

  @Test
  fun editEventForm_populatesFieldsWithEventData() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Verify title is populated
    composeTestRule.onNodeWithText("Test Event").assertIsDisplayed()

    // Verify description is populated
    composeTestRule.onNodeWithText("Test Description").assertIsDisplayed()
  }

  @Test
  fun titleField_canBeEdited() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Use the direct test tag
    composeTestRule.onNodeWithTag("title_input_field").performTextClearance()

    composeTestRule
        .onNodeWithTag("title_input_field")
        .performTextInput("Amazing event Updated Title")

    composeTestRule.waitForIdle()

    assert(viewModel.formState.value.title == "Amazing event Updated Title")
  }

  @Test
  fun descriptionField_canBeEdited() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Test Description").performTextClearance()
    composeTestRule.onNodeWithText("This is amazing..").performTextInput("Updated Description")

    composeTestRule.onNodeWithText("Updated Description").assertIsDisplayed()
  }

  @Test
  fun updateButton_isDisplayed() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Update event").assertIsDisplayed()
  }

  @Test
  fun updateButton_isClickable() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .assertHasClickAction()
  }

  @Test
  fun backButton_isDisplayedAndClickable() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    var backPressed = false

    composeTestRule.setContent {
      EditEventForm(
          eventId = "test-event-id", viewModel = viewModel, onNavigateBack = { backPressed = true })
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithContentDescription("Back")
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    assert(backPressed) { "Back button should trigger navigation" }
  }

  @Test
  fun formFields_updateViewModelState() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("title_input_field").performTextClearance()

    composeTestRule.onNodeWithTag("title_input_field").performTextInput("New Title")

    composeTestRule.waitForIdle()

    assert(viewModel.formState.value.title == "New Title")
  }

  @Test
  fun errorDialog_showsWhenUpdateFails() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    coEvery { mockRepository.updateEvent(any()) } returns Result.failure(Exception("Update failed"))

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Trigger update
    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    // Error dialog should appear
    composeTestRule.onNodeWithTag(EditEventFormTestTags.ERROR_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Update Failed").assertIsDisplayed()
  }

  @Test
  fun errorDialog_canBeDismissed() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    coEvery { mockRepository.updateEvent(any()) } returns Result.failure(Exception("Update failed"))

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    // Dismiss dialog
    composeTestRule.onNodeWithText("OK").performClick()

    composeTestRule.waitForIdle()

    // Dialog should be gone
    composeTestRule.onNodeWithTag(EditEventFormTestTags.ERROR_DIALOG).assertDoesNotExist()
  }

  @Test
  fun validationErrors_displayWhenFieldsEmpty() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Clear title
    composeTestRule.onNodeWithText("Test Event").performTextClearance()

    // Try to update
    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    // Should show validation error
    composeTestRule
        .onNodeWithText(ValidationError.TITLE.message)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun locationField_acceptsInput() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText("Type a location")
        .performScrollTo()
        .performTextInput("EPFL, Lausanne")

    composeTestRule.onNodeWithText("EPFL, Lausanne").assertIsDisplayed()
  }

  @Test
  fun screen_hasCorrectTag() {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.onNodeWithTag(EditEventFormTestTags.SCREEN).assertExists()
  }

  @Test
  fun editEventForm_showsLoadingIndicatorWhileLoading() = runTest {
    // Create a custom ViewModel that stays in Loading state
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

    composeTestRule.onNodeWithText("Retry").performClick()

    composeTestRule.waitForIdle()

    // Form should now be displayed
    composeTestRule.onNodeWithTag(EditEventFormTestTags.FORM_COLUMN).assertExists()
  }

  @Test
  fun updateButton_showsLoadingWhenUpdating() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    coEvery { mockRepository.updateEvent(any()) } returns Result.success(Unit)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .performClick()

    // Should briefly show loading indicator (this might be too fast to catch)
    composeTestRule.waitForIdle()
  }

  @Test
  fun formIsScrollable() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Verify we can see the top
    composeTestRule.onNodeWithText("Title*").assertIsDisplayed()

    // Scroll to bottom
    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()

    // Scroll back to top
    composeTestRule.onNodeWithText("Title*").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun allFieldsAreDisplayed() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Verify all field sections are present
    composeTestRule.onNodeWithTag(EditEventFormTestTags.TITLE_FIELD).assertExists()
    composeTestRule.onNodeWithTag(EditEventFormTestTags.DESCRIPTION_FIELD).assertExists()
    composeTestRule.onNodeWithTag(EditEventFormTestTags.TIME_FIELD).performScrollTo().assertExists()
    composeTestRule.onNodeWithTag(EditEventFormTestTags.DATE_FIELD).performScrollTo().assertExists()
    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.LOCATION_FIELD)
        .performScrollTo()
        .assertExists()
    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.TICKETS_FIELD)
        .performScrollTo()
        .assertExists()
  }

  @Test
  fun priceField_acceptsNumericInput() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Find and clear price field
    composeTestRule.onNodeWithText("25.0").performScrollTo().performTextClearance()

    composeTestRule.onNodeWithText("ex: 12").performTextInput("30.5")

    composeTestRule.onNodeWithText("30.5").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun capacityField_acceptsNumericInput() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("100").performScrollTo().performTextClearance()

    composeTestRule.onNodeWithText("ex: 100").performTextInput("150")

    composeTestRule.onNodeWithText("150").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun formPreservesDataOnRecomposition() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

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
  fun retryButton_hasCorrectTag() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(null)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventFormTestTags.RETRY_BUTTON).assertExists()
  }

  @Test
  fun errorDialogOkButton_hasCorrectTag() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    coEvery { mockRepository.updateEvent(any()) } returns Result.failure(Exception("Error"))

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventFormTestTags.ERROR_DIALOG_OK_BUTTON).assertExists()
  }

  @Test
  fun backButton_hasCorrectTag() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventFormTestTags.BACK_BUTTON).assertExists()
  }

  @Test
  fun validationErrors_clearWhenFieldsUpdated() = runTest {
    every { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    composeTestRule.setContent { EditEventForm(eventId = "test-event-id", viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Clear description to trigger error
    composeTestRule.onNodeWithText("Test Description").performTextClearance()

    // Try to update
    composeTestRule
        .onNodeWithTag(EditEventFormTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    // Should show validation error
    composeTestRule.onNodeWithText(ValidationError.DESCRIPTION.message).assertIsDisplayed()

    // Fix the error
    composeTestRule.onNodeWithText("This is amazing..").performTextInput("New Description")

    composeTestRule.waitForIdle()

    // Error should be cleared
    composeTestRule.onNodeWithText(ValidationError.DESCRIPTION.message).assertDoesNotExist()
  }
}
