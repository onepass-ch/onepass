package ch.onepass.onepass.ui.eventform.createform

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.ui.eventform.EventFormViewModel.ValidationError
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
  fun createEventForm_displaysAllRequiredFields_and_titlePresent() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Check labels
    composeTestRule.onNodeWithText("Title*").assertIsDisplayed()
    composeTestRule.onNodeWithText("Description*").assertIsDisplayed()
    composeTestRule.onNodeWithText("Date & time*").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Location*").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Tickets*").performScrollTo().assertIsDisplayed()

    // Use stable test tags for inputs (less brittle than placeholder text)
    composeTestRule.onNodeWithTag("title_input_field").assertExists()
    composeTestRule.onNodeWithTag("description_input_field").assertExists()
    composeTestRule.onNodeWithTag("location_input_field").performScrollTo().assertExists()
    composeTestRule.onNodeWithTag("price_input_field").performScrollTo().assertExists()
    composeTestRule.onNodeWithTag("capacity_input_field").performScrollTo().assertExists()

    // Create button visible and clickable
    composeTestRule
        .onNodeWithText("Create event")
        .performScrollTo()
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun titleField_acceptsInput() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule.onNodeWithTag("title_input_field").performTextInput("My Test Event")
    composeTestRule.onNodeWithText("My Test Event").assertIsDisplayed()
  }

  @Test
  fun descriptionField_acceptsInput_usingTag() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule
        .onNodeWithTag("description_input_field")
        .performTextInput("This is a test event description")
    composeTestRule.onNodeWithText("This is a test event description").assertIsDisplayed()
  }

  @Test
  fun locationField_acceptsInput_usingTag() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule
        .onNodeWithTag("location_input_field")
        .performScrollTo()
        .performTextInput("EPFL, Lausanne")
    composeTestRule.onNodeWithText("EPFL, Lausanne").assertIsDisplayed()
  }

  @Test
  fun price_and_capacity_acceptsNumericInput_usingTags() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule.onNodeWithTag("price_input_field").performScrollTo().performTextInput("25.50")
    composeTestRule.onNodeWithTag("capacity_input_field").performScrollTo().performTextInput("100")

    composeTestRule.onNodeWithText("25.50").assertIsDisplayed()
    composeTestRule.onNodeWithText("100").assertIsDisplayed()
  }

  @Test
  fun formFields_updateViewModelState() = runTest {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    composeTestRule.onNodeWithTag("title_input_field").performTextInput("Test Event")
    composeTestRule.onNodeWithTag("description_input_field").performTextInput("Test Description")
    composeTestRule
        .onNodeWithTag("location_input_field")
        .performScrollTo()
        .performTextInput("Test Location")
    composeTestRule.onNodeWithTag("price_input_field").performScrollTo().performTextInput("20")
    composeTestRule.onNodeWithTag("capacity_input_field").performScrollTo().performTextInput("50")

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

  // Dialog, validation and other tests unchanged but use tags where possible.
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
  fun imageUploadButton_isVisible() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Upload image button should be visible
    composeTestRule
        .onNodeWithText("Event Image*", substring = true)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun imageSelectionIndicator_notShownInitially() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Image selection indicator should not be shown initially
    composeTestRule.onNodeWithText("image(s) selected", substring = true).assertDoesNotExist()
  }

  @Test
  fun imageSelectionIndicator_showsAfterSelectingImage() = runTest {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Simulate selecting an image by updating the ViewModel directly
    val testUri = android.net.Uri.parse("content://media/image/123")
    viewModel.selectImage(testUri)

    composeTestRule.waitForIdle()

    // Image selection indicator should now be shown
    composeTestRule.onNodeWithText("1 image(s) selected").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun imageSelectionIndicator_showsCorrectCountForMultipleImages() = runTest {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Simulate selecting multiple images
    val testUri1 = android.net.Uri.parse("content://media/image/123")
    val testUri2 = android.net.Uri.parse("content://media/image/456")
    val testUri3 = android.net.Uri.parse("content://media/image/789")
    viewModel.selectImage(testUri1)
    viewModel.selectImage(testUri2)
    viewModel.selectImage(testUri3)

    composeTestRule.waitForIdle()

    // Image selection indicator should show correct count
    composeTestRule.onNodeWithText("3 image(s) selected").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun imageSelectionIndicator_disappearsAfterRemovingAllImages() = runTest {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Simulate selecting and then removing images
    val testUri = android.net.Uri.parse("content://media/image/123")
    viewModel.selectImage(testUri)
    composeTestRule.waitForIdle()

    // Image indicator should be visible
    composeTestRule.onNodeWithText("1 image(s) selected").performScrollTo().assertIsDisplayed()

    // Remove the image
    viewModel.removeImage(testUri)
    composeTestRule.waitForIdle()

    // Image indicator should no longer be visible
    composeTestRule.onNodeWithText("image(s) selected", substring = true).assertDoesNotExist()
  }

  @Test
  fun tagsSection_isDisplayed_and_hasOptions() {
    composeTestRule.setContent { CreateEventForm(viewModel = viewModel) }

    // Scroll to bottom where tags are
    composeTestRule.onNodeWithText("Tags").performScrollTo().assertIsDisplayed()

    // Check for a specific category header
    composeTestRule.onNodeWithText("Theme").performScrollTo().assertIsDisplayed()

    // Check for a specific tag chip
    composeTestRule.onNodeWithText("Technology").performScrollTo().assertIsDisplayed()

    // Perform a click
    composeTestRule.onNodeWithText("Technology").performClick()
  }
}
