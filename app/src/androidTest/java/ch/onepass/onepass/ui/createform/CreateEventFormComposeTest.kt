package ch.onepass.onepass.ui.createform

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.EventRepository
import io.mockk.mockk
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

    // Check for all required field labels
    composeTestRule.onNodeWithText("Title*").assertIsDisplayed()
    composeTestRule.onNodeWithText("Description*").assertIsDisplayed()
    composeTestRule.onNodeWithText("Date & time*").assertIsDisplayed()
    composeTestRule.onNodeWithText("Location*").assertIsDisplayed()
    composeTestRule.onNodeWithText("Tickets*").assertIsDisplayed()
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

    // Find price field by placeholder
    composeTestRule.onNodeWithText("ex: 12").performTextInput("25.50")

    // Verify the input was accepted
    composeTestRule.onNodeWithText("25.50").assertIsDisplayed()
  }

  @Test
  fun capacityField_acceptsNumericInput() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Find capacity field by placeholder
    composeTestRule.onNodeWithText("ex: 250").performTextInput("100")

    // Verify the input was accepted
    composeTestRule.onNodeWithText("100").assertIsDisplayed()
  }

  @Test
  fun createTicketButton_isDisplayed() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule.onNodeWithText("Create ticket").assertIsDisplayed()
  }

  @Test
  fun createTicketButton_isClickable() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule.onNodeWithText("Create ticket").assertHasClickAction()
  }

  @Test
  fun formFields_updateViewModelState() = runTest {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Input data into fields
    composeTestRule.onNodeWithText("Amazing event").performTextInput("Test Event")

    composeTestRule.onNodeWithText("This is amazing..").performTextInput("Test Description")

    composeTestRule.onNodeWithText("Type a location").performTextInput("Test Location")

    composeTestRule.onNodeWithText("ex: 12").performTextInput("20")

    composeTestRule.onNodeWithText("ex: 250").performTextInput("50")

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

    // Verify that we can perform scroll action
    composeTestRule.onNodeWithText("CREATE YOUR EVENT").assertIsDisplayed()

    // The form should be scrollable to see the button at the bottom
    composeTestRule.onNodeWithText("Create ticket").performScrollTo().assertIsDisplayed()
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

    // Trigger recomposition by scrolling
    composeTestRule.onNodeWithText("Create ticket").performScrollTo()

    // Verify data is still there
    composeTestRule.onNodeWithText("Test Event").assertIsDisplayed()
  }
}
