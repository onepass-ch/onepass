package ch.onepass.onepass.ui.createform

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.ui.createform.CreateEventFormViewModel.ValidationError
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for CreateEventForm composable.
 *
 * These tests verify that the UI components render correctly and interact with the ViewModel.
 */
@RunWith(AndroidJUnit4::class)
class CreateEventFormComposeTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockRepository: EventRepository
  private lateinit var viewModel: CreateEventFormViewModel

  @Before
  fun setUp() {
    mockRepository = mockk(relaxed = true)
    viewModel = CreateEventFormViewModel(mockRepository)
  }

  @Test
  fun createEventForm_displaysTitle() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule.onNodeWithText("CREATE YOUR EVENT").assertIsDisplayed()
  }

  @Test
  fun createEventForm_displaysAllRequiredFields() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Check for all required field labels (scroll to each to ensure visibility)
    composeTestRule.onNodeWithText("Title*").assertIsDisplayed()
    composeTestRule.onNodeWithText("Description*").assertIsDisplayed()
    composeTestRule.onNodeWithText("Date & time*").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Location*").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Tickets*").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun titleField_acceptsInput() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Find the title text field by its placeholder text
    composeTestRule.onNodeWithText("Amazing event").performTextInput("My Test Event")

    // Verify the input was accepted
    composeTestRule.onNodeWithText("My Test Event").assertIsDisplayed()
  }

  @Test
  fun descriptionField_acceptsInput() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Find the description field by its placeholder
    composeTestRule
        .onNodeWithText("This is amazing..")
        .performTextInput("This is a test event description")

    // Verify the input was accepted
    composeTestRule.onNodeWithText("This is a test event description").assertIsDisplayed()
  }

  @Test
  fun locationField_acceptsInput() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Find location field by placeholder
    composeTestRule.onNodeWithText("Type a location").performTextInput("EPFL, Lausanne")

    // Verify the input was accepted
    composeTestRule.onNodeWithText("EPFL, Lausanne").assertIsDisplayed()
  }

  @Test
  fun priceField_acceptsNumericInput() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Find price field by placeholder (scroll to it first)
    composeTestRule.onNodeWithText("ex: 12").performScrollTo().performTextInput("25.50")

    // Verify the input was accepted
    composeTestRule.onNodeWithText("25.50").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun capacityField_acceptsNumericInput() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Find capacity field by placeholder (scroll to it first)
    composeTestRule.onNodeWithText("ex: 250").performScrollTo().performTextInput("100")

    // Verify the input was accepted
    composeTestRule.onNodeWithText("100").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun createEventButton_isDisplayed() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Button is at the bottom, so scroll to it first
    composeTestRule.onNodeWithText("Create Event").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun createEventButton_isClickable() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Button is at the bottom, so scroll to it first
    composeTestRule.onNodeWithText("Create Event").performScrollTo().assertHasClickAction()
  }

  @Test
  fun formFields_updateViewModelState() = runTest {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Input data into fields (scroll to each field first)
    composeTestRule.onNodeWithText("Amazing event").performTextInput("Test Event")

    composeTestRule.onNodeWithText("This is amazing..").performTextInput("Test Description")

    composeTestRule
        .onNodeWithText("Type a location")
        .performScrollTo()
        .performTextInput("Test Location")

    composeTestRule.onNodeWithText("ex: 12").performScrollTo().performTextInput("20")

    composeTestRule.onNodeWithText("ex: 250").performScrollTo().performTextInput("50")

    // Wait for state to update
    composeTestRule.waitForIdle()

    // Verify ViewModel state was updated
    val formState = viewModel.formState.value
    assert(formState.title == "Test Event")
    assert(formState.description == "Test Description")
    assert(formState.location == "Test Location")
    assert(formState.price == "20")
    assert(formState.capacity == "50")
  }

  @Test
  fun datePickerField_isClickable() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Find date picker by text "Select date"
    composeTestRule.onNodeWithText("Select date").assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun startTimeField_isDisplayed() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule.onNodeWithText("Start time").assertIsDisplayed()
  }

  @Test
  fun endTimeField_isDisplayed() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule.onNodeWithText("End time").assertIsDisplayed()
  }

  @Test
  fun formIsScrollable() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Verify that we can see the top of the form
    composeTestRule.onNodeWithText("Title*").assertIsDisplayed()

    // The form should be scrollable to see the button at the bottom
    composeTestRule.onNodeWithText("Create Event").performScrollTo().assertIsDisplayed()

    // Verify we can scroll back to top
    composeTestRule.onNodeWithText("Title*").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun loadingIndicator_showsWhenLoading() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Set loading state
    viewModel.createEvent("test-org-id", "Test Organizer")

    // Wait for loading state
    composeTestRule.waitForIdle()

    // Note: Loading indicator might be difficult to test directly
    // This is a placeholder for loading state verification
  }

  @Test
  fun formPreservesStateOnRecomposition() = runTest {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Input data
    composeTestRule.onNodeWithText("Amazing event").performTextInput("Test Event")

    composeTestRule.waitForIdle()

    // Trigger recomposition by scrolling to bottom
    composeTestRule.onNodeWithText("Create Event").performScrollTo()

    // Scroll back to top to verify data is still there
    composeTestRule.onNodeWithText("Title*").performScrollTo()
    composeTestRule.onNodeWithText("Test Event").assertIsDisplayed()
  }

  @Test
  fun backButton_isDisplayedAndClickable() {
    var backPressed = false
    composeTestRule.setContent {
      CreateEventForm(viewModel = viewModel, onNavigateBack = { backPressed = true })
    }

    composeTestRule
        .onNodeWithContentDescription("Back")
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    assert(backPressed) { "Back button should trigger navigation" }
  }

  @Test
  fun timePickerDialog_opensAndSelectsTime() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Scroll to and click start time field
    composeTestRule.onNodeWithText("Start time").performScrollTo()

    // Click the time picker box (it's a clickable Box)
    composeTestRule
        .onAllNodesWithText("${Calendar.HOUR_OF_DAY}:${Calendar.MINUTE}")
        .onFirst()
        .performClick()

    // Verify dialog opens
    composeTestRule.onNodeWithText("Select Time").assertIsDisplayed()

    // Click OK
    composeTestRule.onNodeWithText("OK").performClick()

    // Verify time was set
    composeTestRule.waitForIdle()
    assert(viewModel.formState.value.startTime.isNotEmpty())
  }

  @Test
  fun timePickerDialog_canBeCancelled() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule.onNodeWithText("Start time").performScrollTo()

    composeTestRule
        .onAllNodesWithText("${Calendar.HOUR_OF_DAY}:${Calendar.MINUTE}")
        .onFirst()
        .performClick()

    // Click Cancel
    composeTestRule.onNodeWithText("Cancel").performClick()

    // Dialog should close, time should remain empty
    composeTestRule.waitForIdle()
    assert(viewModel.formState.value.startTime.isEmpty())
  }

  @Test
  fun datePickerDialog_canBeCancelled() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule.onNodeWithText("Select date").performScrollTo().performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.waitForIdle()
    assert(viewModel.formState.value.date.isEmpty())
  }

  @Test
  fun validationErrors_displayedWhenFieldsEmpty() = runTest {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Click create button without filling fields
    composeTestRule.onNodeWithText("Create Event").performScrollTo().performClick()

    composeTestRule.waitForIdle()

    // Error message is shown in a Snackbar at the bottom
    // The message will contain the TITLE error among others
    composeTestRule
        .onNodeWithText(ValidationError.TITLE.message, substring = true)
        .assertIsDisplayed()
  }

  @Test
  fun invalidPrice_showsError() = runTest {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Fill all fields
    composeTestRule.fillAllFields(viewModel)

    // Set invalid price - clear first then input
    composeTestRule
        .onNodeWithText("10") // The value we just set
        .performScrollTo()
        .performTextClearance()

    composeTestRule.onNodeWithText("ex: 12").performScrollTo().performTextInput("abc")

    // Try to create
    composeTestRule.onNodeWithText("Create Event").performScrollTo().performClick()

    composeTestRule.waitForIdle()

    // Error message is shown in a Snackbar, which doesn't need scrolling
    composeTestRule.onNodeWithText(ValidationError.PRICE_INVALID.message).assertIsDisplayed()
  }

  @Test
  fun editMode_displaysEditTitle() = runTest {
    // Create a test event
    val testEvent =
        Event(
            eventId = "test-event-id",
            title = "Existing Event",
            description = "Existing Description",
            organizerId = "org-123",
            organizerName = "Test Organizer",
            status = EventStatus.PUBLISHED,
            location = Location(name = "EPFL"),
            startTime = Timestamp(Date(2025, 11, 15, 14, 30)),
            endTime = Timestamp(Date(2025, 11, 15, 16, 30)),
            capacity = 100,
            ticketsRemaining = 80,
            pricingTiers =
                listOf(PricingTier(name = "General", price = 25.0, quantity = 100, remaining = 80)))

    // Mock the repository to return the test event
    coEvery { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    // Create ViewModel with mocked repository
    val testViewModel = CreateEventFormViewModel(mockRepository)

    composeTestRule.setContent {
      CreateEventForm(eventId = "test-event-id", viewModel = testViewModel)
    }

    // Wait for the event to load
    composeTestRule.waitForIdle()

    // Verify the title shows "EDIT YOUR EVENT"
    composeTestRule.onNodeWithText("EDIT YOUR EVENT").assertIsDisplayed()
  }

  @Test
  fun editMode_displaysUpdateEventButton() = runTest {
    // Create a test event
    val testEvent =
        Event(
            eventId = "test-event-id",
            title = "Existing Event",
            description = "Existing Description",
            organizerId = "org-123",
            organizerName = "Test Organizer",
            status = EventStatus.PUBLISHED,
            location = Location(name = "EPFL"),
            startTime = Timestamp(Date(2025, 11, 15, 14, 30)),
            endTime = Timestamp(Date(2025, 11, 15, 16, 30)),
            capacity = 100,
            ticketsRemaining = 80,
            pricingTiers =
                listOf(PricingTier(name = "General", price = 25.0, quantity = 100, remaining = 80)))

    // Mock the repository to return the test event
    coEvery { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    // Create ViewModel with mocked repository
    val testViewModel = CreateEventFormViewModel(mockRepository)

    composeTestRule.setContent {
      CreateEventForm(eventId = "test-event-id", viewModel = testViewModel)
    }

    // Wait for the event to load
    composeTestRule.waitForIdle()

    // Scroll to button and verify it shows "Update Event"
    composeTestRule.onNodeWithText("Update Event").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun editMode_populatesFormFields() = runTest {
    // Create a test event
    val testEvent =
        Event(
            eventId = "test-event-id",
            title = "Existing Event",
            description = "Existing Description",
            organizerId = "org-123",
            organizerName = "Test Organizer",
            status = EventStatus.PUBLISHED,
            location = Location(name = "EPFL"),
            startTime = Timestamp(Date(2025, 11, 15, 14, 30)),
            endTime = Timestamp(Date(2025, 11, 15, 16, 30)),
            capacity = 100,
            ticketsRemaining = 80,
            pricingTiers =
                listOf(PricingTier(name = "General", price = 25.0, quantity = 100, remaining = 80)))

    // Mock the repository to return the test event
    coEvery { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    // Create ViewModel with mocked repository
    val testViewModel = CreateEventFormViewModel(mockRepository)

    composeTestRule.setContent {
      CreateEventForm(eventId = "test-event-id", viewModel = testViewModel)
    }

    // Wait for the event to load
    composeTestRule.waitForIdle()

    // Verify form fields are populated
    composeTestRule.onNodeWithText("Existing Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Existing Description").assertIsDisplayed()
    composeTestRule.onNodeWithText("EPFL").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("25.0").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("100").performScrollTo().assertIsDisplayed()
  }
}
// Fixed Helper function - moved outside test class
private fun AndroidComposeTestRule<*, *>.fillAllFields(viewModel: CreateEventFormViewModel) {
  onNodeWithText("Amazing event").performTextInput("Test Event")
  onNodeWithText("This is amazing..").performTextInput("Description")
  onNodeWithText("Type a location").performScrollTo().performTextInput("Location")
  onNodeWithText("ex: 12").performScrollTo().performTextInput("10")
  onNodeWithText("ex: 250").performScrollTo().performTextInput("50")

  // Set date
  onNodeWithText("Select date").performScrollTo().performClick()
  waitForIdle()
  onNodeWithText("OK").performClick()
  waitForIdle()

  // Set times
  viewModel.updateStartTime("10:00")
  viewModel.updateEndTime("12:00")
}
