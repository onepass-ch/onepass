package ch.onepass.onepass.ui.eventform.createform

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.ui.eventform.EventFormViewModel.ValidationError
import io.mockk.mockk
import java.util.Calendar
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

    composeTestRule.onNodeWithText("Create your Event").assertIsDisplayed()
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
    composeTestRule.onNodeWithText("ex: 100").performScrollTo().performTextInput("100")

    // Verify the input was accepted
    composeTestRule.onNodeWithText("100").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun createTicketButton_isDisplayed() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Button is at the bottom, so scroll to it first
    composeTestRule.onNodeWithText("Create event").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun createTicketButton_isClickable() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Button is at the bottom, so scroll to it first
    composeTestRule.onNodeWithText("Create event").performScrollTo().assertHasClickAction()
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

    composeTestRule.onNodeWithText("ex: 100").performScrollTo().performTextInput("50")

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
    composeTestRule.onNodeWithText("Create event").performScrollTo().assertIsDisplayed()

    // Verify we can scroll back to top
    composeTestRule.onNodeWithText("Title*").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun loadingIndicator_showsWhenLoading() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Set loading state
    viewModel.createEvent()

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
    composeTestRule.onNodeWithText("Create event").performScrollTo()

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
    composeTestRule.onNodeWithText("Create event").performScrollTo().performClick()

    composeTestRule.waitForIdle()

    // Scroll to top to see error messages
    composeTestRule.onNodeWithText("Title*").performScrollTo()

    composeTestRule.onNodeWithText(ValidationError.TITLE.message).assertIsDisplayed()
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
    composeTestRule.onNodeWithText("Create event").performScrollTo().performClick()

    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithText(ValidationError.PRICE_INVALID.message)
        .performScrollTo()
        .assertIsDisplayed()
  }
}
// Fixed Helper function - moved outside test class
private fun AndroidComposeTestRule<*, *>.fillAllFields(viewModel: CreateEventFormViewModel) {
  onNodeWithText("Amazing event").performTextInput("Test Event")
  onNodeWithText("This is amazing..").performTextInput("Description")
  onNodeWithText("Type a location").performScrollTo().performTextInput("Location")
  onNodeWithText("ex: 12").performScrollTo().performTextInput("10")
  onNodeWithText("ex: 100").performScrollTo().performTextInput("50")

  // Set date
  onNodeWithText("Select date").performScrollTo().performClick()
  waitForIdle()
  onNodeWithText("OK").performClick()
  waitForIdle()

  // Set times
  viewModel.updateStartTime("10:00")
  viewModel.updateEndTime("12:00")
}
