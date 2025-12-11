package ch.onepass.onepass.ui.eventform

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.EventTag
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.map.LocationRepository
import ch.onepass.onepass.model.storage.StorageRepository
import ch.onepass.onepass.ui.eventform.editform.EditEventFormViewModel
import ch.onepass.onepass.ui.eventform.editform.EditEventUiState
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class EditEventFormViewModelTest {
  private lateinit var mockLocationRepository: LocationRepository
  private lateinit var mockStorageRepository: StorageRepository

  // Set the main coroutines dispatcher for unit testing.
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var viewModel: EditEventFormViewModel
  private lateinit var mockRepository: EventRepository
  private lateinit var testEvent: Event

  // Helper to create Timestamps from "dd/MM/yyyy HH:mm"
  private fun parseTimestamp(dateTime: String): Timestamp {
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return Timestamp(format.parse(dateTime) ?: Date())
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher) // Set main dispatcher
    mockRepository = mockk<EventRepository>()
    mockLocationRepository = mockk(relaxed = true)
    mockStorageRepository = mockk(relaxed = true)

    testEvent =
        Event(
            eventId = "test-event-id",
            title = "Original Title",
            description = "Original Description",
            organizerId = "org-id",
            organizerName = "Test Organizer",
            status = EventStatus.DRAFT,
            location = Location(name = "Original Location"),
            startTime = parseTimestamp("25/12/2025 14:30"),
            endTime = parseTimestamp("25/12/2025 16:30"),
            capacity = 100,
            ticketsRemaining = 80,
            ticketsIssued = 20,
            pricingTiers = listOf(PricingTier("General", 25.0, 100, 80)))

    // Default setup for loading, tests can override this
    coEvery { mockRepository.getEventById(any()) } returns flowOf(testEvent)

    viewModel =
        EditEventFormViewModel(mockRepository, mockLocationRepository, mockStorageRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain() // Reset main dispatcher
  }

  @Test
  fun loadEventSuccessPopulatesFormAndSetsIdleState() = runTest {
    // Act
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutine completes

    // Assert
    Assert.assertEquals(EditEventUiState.Idle, viewModel.uiState.value)
    val form = viewModel.formState.value
    Assert.assertEquals("Original Title", form.title)
    Assert.assertEquals("Original Description", form.description)
    Assert.assertEquals("Original Location", form.location)
    Assert.assertEquals("25/12/2025", form.date)
    Assert.assertEquals("14:30", form.startTime)
    Assert.assertEquals("16:30", form.endTime)
    Assert.assertEquals("25", form.price)
    Assert.assertEquals("100", form.capacity)
  }

  @Test
  fun loadEventNotFoundSetsLoadErrorState() = runTest {
    // Arrange
    coEvery { mockRepository.getEventById("not-found") } returns flowOf(null)

    // Act
    viewModel.loadEvent("not-found")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    Assert.assertTrue(state is EditEventUiState.LoadError)
    Assert.assertEquals("Event not found", (state as EditEventUiState.LoadError).message)
  }

  @Test
  fun updateTitleUpdatesStateAndClearsError() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateTitle("") // Make invalid
    viewModel.updateEvent() // Trigger validation
    testDispatcher.scheduler.advanceUntilIdle()

    // Act
    viewModel.updateTitle("New Title")

    // Assert
    Assert.assertEquals("New Title", viewModel.formState.value.title)
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.TITLE.key))
  }

  @Test
  fun updateDescriptionUpdatesStateAndClearsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateDescription("") // Set invalid
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.DESCRIPTION.key))

    viewModel.updateDescription("New Desc")
    Assert.assertEquals("New Desc", viewModel.formState.value.description)
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.DESCRIPTION.key))
  }

  @Test
  fun updateStartTimeUpdatesStateAndClearsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateStartTime("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.START_TIME.key))

    viewModel.updateStartTime("10:00")
    Assert.assertEquals("10:00", viewModel.formState.value.startTime)
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.START_TIME.key))
  }

  @Test
  fun updateEndTimeUpdatesStateAndClearsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateEndTime("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.END_TIME.key))

    viewModel.updateEndTime("20:00")
    Assert.assertEquals("20:00", viewModel.formState.value.endTime)
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.END_TIME.key))
  }

  @Test
  fun updateDateUpdatesStateAndClearsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateDate("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.DATE.key))

    viewModel.updateDate("01/01/2026")
    Assert.assertEquals("01/01/2026", viewModel.formState.value.date)
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.DATE.key))
  }

  @Test
  fun updateLocationUpdatesStateAndClearsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateLocation("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.LOCATION.key))

    viewModel.updateLocation("New Location")
    Assert.assertEquals("New Location", viewModel.formState.value.location)
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.LOCATION.key))
  }

  @Test
  fun updatePriceUpdatesStateAndClearsAllPriceErrors() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updatePrice("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.PRICE_EMPTY.key))

    viewModel.updatePrice("50")
    Assert.assertEquals("50", viewModel.formState.value.price)
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.PRICE_EMPTY.key))
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(
            EventFormViewModel.ValidationError.PRICE_INVALID.key))
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(
            EventFormViewModel.ValidationError.PRICE_NEGATIVE.key))
  }

  @Test
  fun updateCapacityUpdatesStateAndClearsAllCapacityErrors() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateCapacity("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        viewModel.fieldErrors.value.containsKey(
            EventFormViewModel.ValidationError.CAPACITY_EMPTY.key))

    viewModel.updateCapacity("50")
    Assert.assertEquals("50", viewModel.formState.value.capacity)
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(
            EventFormViewModel.ValidationError.CAPACITY_EMPTY.key))
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(
            EventFormViewModel.ValidationError.CAPACITY_INVALID.key))
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(
            EventFormViewModel.ValidationError.CAPACITY_NEGATIVE.key))
  }

  @Test
  fun updateEventValidationFailsForEmptyTitle() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateTitle("")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val errors = viewModel.fieldErrors.value
    val state = viewModel.uiState.value
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.TITLE.key))
    Assert.assertEquals(
        EventFormViewModel.ValidationError.TITLE.message,
        errors[EventFormViewModel.ValidationError.TITLE.key])
    Assert.assertTrue(state is EditEventUiState.Error)
    Assert.assertEquals("Please fix validation errors", (state as EditEventUiState.Error).message)
  }

  @Test
  fun updateEventSuccessSetsSuccessStateAndCallsRepository() = runTest {
    // Arrange
    coEvery { mockRepository.updateEvent(any()) } coAnswers
        {
          delay(1)
          Result.success(Unit)
        }
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    val newTitle = "Updated Event Title"
    viewModel.updateTitle(newTitle)

    // Act
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    Assert.assertEquals(EditEventUiState.Success, viewModel.uiState.value)
    Assert.assertTrue(viewModel.fieldErrors.value.isEmpty())
    coVerify { mockRepository.updateEvent(match { it.title == newTitle }) }
  }

  @Test
  fun updateEventWithoutLoadingEventFirstSetsErrorState() = runTest {
    // Arrange
    val freshViewModel =
        EditEventFormViewModel(mockRepository, mockLocationRepository, mockStorageRepository)

    // Act
    freshViewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = freshViewModel.uiState.value
    Assert.assertTrue(state is EditEventUiState.Error)
    Assert.assertEquals("Original event not loaded", (state as EditEventUiState.Error).message)
  }

  @Test
  fun clearErrorWhenInErrorStateSetsIdleState() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateTitle("")
    viewModel.updateEvent() // Puts state in Error
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(viewModel.uiState.value is EditEventUiState.Error)

    // Act
    viewModel.clearError()

    // Assert
    Assert.assertEquals(EditEventUiState.Idle, viewModel.uiState.value)
  }

  @Test
  fun loadEventRepositoryThrowsExceptionSetsLoadErrorState() = runTest {
    // Arrange
    val exception = RuntimeException("Database error")
    coEvery { mockRepository.getEventById("error-id") } throws exception

    // Act
    viewModel.loadEvent("error-id")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    Assert.assertTrue(state is EditEventUiState.LoadError)
    Assert.assertEquals("Database error", (state as EditEventUiState.LoadError).message)
  }

  @Test
  fun loadEventHandlesNullLocation() = runTest {
    // Arrange
    val eventNoLocation = testEvent.copy(location = null)
    coEvery { mockRepository.getEventById("no-location") } returns flowOf(eventNoLocation)

    // Act
    viewModel.loadEvent("no-location")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    Assert.assertEquals("", viewModel.formState.value.location)
  }

  @Test
  fun loadEventHandlesZeroPrice() = runTest {
    // Arrange
    val eventZeroPrice = testEvent.copy(pricingTiers = listOf(PricingTier(price = 0.0)))
    coEvery { mockRepository.getEventById("zero-price") } returns flowOf(eventZeroPrice)

    // Act
    viewModel.loadEvent("zero-price")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    Assert.assertEquals("0", viewModel.formState.value.price)
  }

  @Test
  fun loadEventHandlesEmptyPricingTiers() = runTest {
    // Arrange
    val eventNoTiers = testEvent.copy(pricingTiers = emptyList())
    coEvery { mockRepository.getEventById("no-tiers") } returns flowOf(eventNoTiers)

    // Act
    viewModel.loadEvent("no-tiers")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    Assert.assertEquals("0", viewModel.formState.value.price)
  }

  @Test
  fun updateStartTimeClearsTimeError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateStartTime("20:00")
    viewModel.updateEndTime("10:00")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.TIME.key))

    viewModel.updateStartTime("09:00")
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.TIME.key))
  }

  @Test
  fun updateEndTimeClearsTimeError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateStartTime("20:00")
    viewModel.updateEndTime("10:00")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.TIME.key))

    viewModel.updateEndTime("21:00")
    Assert.assertFalse(
        viewModel.fieldErrors.value.containsKey(EventFormViewModel.ValidationError.TIME.key))
  }

  @Test
  fun updateEventValidationFailsForAllEmptyFields() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateTitle("")
    viewModel.updateDescription("")
    viewModel.updateDate("")
    viewModel.updateStartTime("")
    viewModel.updateEndTime("")
    viewModel.updateLocation("")
    viewModel.updatePrice("")
    viewModel.updateCapacity("")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    Assert.assertEquals(8, errors.size)
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.TITLE.key))
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.DESCRIPTION.key))
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.DATE.key))
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.START_TIME.key))
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.END_TIME.key))
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.LOCATION.key))
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.PRICE_EMPTY.key))
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.CAPACITY_EMPTY.key))
  }

  @Test
  fun updateEventValidationFailsForEndTimeBeforeStartTime() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateStartTime("14:00")
    viewModel.updateEndTime("12:00")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.TIME.key))
  }

  @Test
  fun updateEventValidationFailsForEndTimeEqualToStartTime() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateStartTime("14:00")
    viewModel.updateEndTime("14:00")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.TIME.key))
  }

  @Test
  fun updateEventValidationFailsForInvalidPrice() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updatePrice("abc")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.PRICE_INVALID.key))
  }

  @Test
  fun updateEventValidationFailsForInvalidCapacity() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateCapacity("abc")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.CAPACITY_INVALID.key))
  }

  @Test
  fun updateEventValidationFailsForZeroCapacity() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateCapacity("0")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    Assert.assertTrue(errors.containsKey(EventFormViewModel.ValidationError.CAPACITY_NEGATIVE.key))
  }

  @Test
  fun updateEventWithInvalidDateParseFailsSetsErrorState() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateDate("not a date") // Will cause parseDateAndTime to return null

    // Act
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    Assert.assertTrue(state is EditEventUiState.Error)
    Assert.assertEquals("Failed to process form data", (state as EditEventUiState.Error).message)
  }

  @Test
  fun updateEventWithInvalidTimeParseFailsSetsErrorState() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateEndTime("xx:xx")

    // Act
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    Assert.assertTrue(state is EditEventUiState.Error)
    Assert.assertEquals("Failed to process form data", (state as EditEventUiState.Error).message)
  }

  @Test
  fun updateEventRepositoryThrowsExceptionSetsErrorState() = runTest {
    // Arrange
    val exception = RuntimeException("DB write failed")
    coEvery { mockRepository.updateEvent(any()) } coAnswers
        {
          delay(1)
          Result.failure(exception)
        }
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateTitle("A valid new title") // Make form valid

    // Act
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    Assert.assertTrue(state is EditEventUiState.Error)
    Assert.assertEquals("DB write failed", (state as EditEventUiState.Error).message)
  }

  @Test
  fun updateEventSetsUpdatingStateBeforeCompletion() = runTest {
    // Arrange
    coEvery { mockRepository.updateEvent(any()) } coAnswers
        {
          delay(1)
          Result.success(Unit)
        }
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    // Act
    viewModel.updateEvent()
    testDispatcher.scheduler.runCurrent() // Run queued launch to set Updating and suspend at delay

    // Assert - state should be Updating
    Assert.assertEquals(EditEventUiState.Updating, viewModel.uiState.value)

    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertEquals(EditEventUiState.Success, viewModel.uiState.value)
  }

  @Test
  fun clearErrorWhenNotInErrorStateDoesNothing() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id") // Puts state in Loading, then Idle
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertEquals(EditEventUiState.Idle, viewModel.uiState.value)

    // Act
    viewModel.clearError()

    // Assert
    Assert.assertEquals(EditEventUiState.Idle, viewModel.uiState.value)
  }

  @Test
  fun loadEventHandlesNullStartTime() = runTest {
    // Arrange
    val eventNullTimes = testEvent.copy(startTime = null, endTime = null)
    coEvery { mockRepository.getEventById("null-times") } returns flowOf(eventNullTimes)

    // Act
    viewModel.loadEvent("null-times")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    Assert.assertEquals("", viewModel.formState.value.date)
    Assert.assertEquals("", viewModel.formState.value.startTime)
    Assert.assertEquals("", viewModel.formState.value.endTime)
  }

  // ===== NEW TESTS FOR IMAGE FUNCTIONALITY =====

  @Test
  fun selectImageAddsToExistingImages() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    val newImageUri = android.net.Uri.parse("content://media/image/new123")

    // Act
    viewModel.selectImage(newImageUri)

    // Assert
    val formState = viewModel.formState.value
    Assert.assertEquals(1, formState.selectedImageUris.size)
    Assert.assertTrue(formState.selectedImageUris.contains(newImageUri))
  }

  @Test
  fun selectMultipleImagesInEditForm() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    val imageUri1 = android.net.Uri.parse("content://media/image/123")
    val imageUri2 = android.net.Uri.parse("content://media/image/456")

    // Act
    viewModel.selectImage(imageUri1)
    viewModel.selectImage(imageUri2)

    // Assert
    val formState = viewModel.formState.value
    Assert.assertEquals(2, formState.selectedImageUris.size)
    Assert.assertTrue(formState.selectedImageUris.containsAll(listOf(imageUri1, imageUri2)))
  }

  @Test
  fun removeImageFromSelectedImages() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    val imageUri1 = android.net.Uri.parse("content://media/image/123")
    val imageUri2 = android.net.Uri.parse("content://media/image/456")
    val imageUri3 = android.net.Uri.parse("content://media/image/789")

    viewModel.selectImage(imageUri1)
    viewModel.selectImage(imageUri2)
    viewModel.selectImage(imageUri3)

    // Act
    viewModel.removeImage(imageUri2)

    // Assert
    val formState = viewModel.formState.value
    Assert.assertEquals(2, formState.selectedImageUris.size)
    Assert.assertFalse(formState.selectedImageUris.contains(imageUri2))
    Assert.assertTrue(formState.selectedImageUris.contains(imageUri1))
    Assert.assertTrue(formState.selectedImageUris.contains(imageUri3))
  }

  @Test
  fun loadEventDoesNotIncludeExistingImagesInFormState() = runTest {
    // Arrange - existing images are in Event.images, not in formState.selectedImageUris
    coEvery { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    // Act
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert - existing images are NOT in selectedImageUris (those are for NEW uploads only)
    val formState = viewModel.formState.value
    Assert.assertTrue(
        "Selected image URIs should be empty for existing event",
        formState.selectedImageUris.isEmpty())
  }

  @Test
  fun selectImageMaintainsOrder() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    val imageUri1 = android.net.Uri.parse("content://media/image/123")
    val imageUri2 = android.net.Uri.parse("content://media/image/456")
    val imageUri3 = android.net.Uri.parse("content://media/image/789")

    // Act
    viewModel.selectImage(imageUri1)
    viewModel.selectImage(imageUri2)
    viewModel.selectImage(imageUri3)

    // Assert
    val formState = viewModel.formState.value
    Assert.assertEquals(imageUri1, formState.selectedImageUris[0])
    Assert.assertEquals(imageUri2, formState.selectedImageUris[1])
    Assert.assertEquals(imageUri3, formState.selectedImageUris[2])
  }

  @Test
  fun removingNonExistentImageDoesNotFail() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    val imageUri1 = android.net.Uri.parse("content://media/image/123")
    val nonExistent = android.net.Uri.parse("content://media/image/999")

    viewModel.selectImage(imageUri1)

    // Act
    viewModel.removeImage(nonExistent)

    // Assert
    val formState = viewModel.formState.value
    Assert.assertEquals(1, formState.selectedImageUris.size)
    Assert.assertTrue(formState.selectedImageUris.contains(imageUri1))
  }

  @Test
  fun toggleTag_adds_and_removes_tag() {
    val eventRepository: EventRepository = mockk(relaxed = true)
    val locationRepository: LocationRepository = mockk(relaxed = true)
    val storageRepository: StorageRepository = mockk(relaxed = true)

    val viewModel =
        EditEventFormViewModel(
            eventRepository = eventRepository,
            locationRepository = locationRepository,
            storageRepository = storageRepository)

    val initialTags = viewModel.formState.value.selectedTags
    assertTrue(initialTags.isEmpty())

    viewModel.toggleTag(EventTag.TECH)
    assertEquals(1, viewModel.formState.value.selectedTags.size)
    assertTrue(viewModel.formState.value.selectedTags.contains(EventTag.TECH))

    viewModel.toggleTag(EventTag.TECH)
    assertEquals(0, viewModel.formState.value.selectedTags.size)
    assertFalse(viewModel.formState.value.selectedTags.contains(EventTag.TECH))
  }

  @Test
  fun toggleTag_enforces_maximum_limit_of_five() {
    val eventRepository: EventRepository = mockk(relaxed = true)
    val locationRepository: LocationRepository = mockk(relaxed = true)
    val storageRepository: StorageRepository = mockk(relaxed = true)

    val viewModel =
        EditEventFormViewModel(
            eventRepository = eventRepository,
            locationRepository = locationRepository,
            storageRepository = storageRepository)

    viewModel.toggleTag(EventTag.TECH)
    viewModel.toggleTag(EventTag.BUSINESS)
    viewModel.toggleTag(EventTag.ARTS)
    viewModel.toggleTag(EventTag.MUSIC)
    viewModel.toggleTag(EventTag.FOOD)

    assertEquals(5, viewModel.formState.value.selectedTags.size)

    viewModel.toggleTag(EventTag.SPORTS)

    assertEquals(5, viewModel.formState.value.selectedTags.size)
    assertFalse(viewModel.formState.value.selectedTags.contains(EventTag.SPORTS))
  }

  @Test
  fun updateTitle_withHtmlScript_setsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateTitle("<script>alert(1)</script>")
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.fieldErrors.value.containsKey("title"))
    assertEquals(
        EventFormViewModel.ValidationError.TITLE_DANGEROUS.message,
        viewModel.fieldErrors.value["title"])
  }

  @Test
  fun updateTitle_withSqlInjection_setsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateTitle("Event'; DROP TABLE users")
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.fieldErrors.value.containsKey("title"))
  }

  @Test
  fun updateTitle_withJavascriptProtocol_setsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateTitle("javascript:alert(1)")
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.fieldErrors.value.containsKey("title"))
  }

  @Test
  fun updateTitle_afterSanitizationError_clearsErrorOnValidInput() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateTitle("<img src=x>")
    Assert.assertTrue(viewModel.fieldErrors.value.containsKey("title"))

    viewModel.updateTitle("Valid Title")
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.fieldErrors.value.containsKey("title"))
  }

  @Test
  fun updateDescription_withHtmlScript_setsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateDescription("Nice event<script>alert(1)</script>")
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.fieldErrors.value.containsKey("description"))
    assertEquals(
        EventFormViewModel.ValidationError.DESCRIPTION_DANGEROUS.message,
        viewModel.fieldErrors.value["description"])
  }

  @Test
  fun updateDescription_withSqlInjection_setsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateDescription("Description; DROP TABLE users")
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.fieldErrors.value.containsKey("description"))
  }

  @Test
  fun updateDescription_afterSanitizationError_clearsErrorOnValidInput() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateDescription("Test<script>alert(1)</script>")
    assert(viewModel.fieldErrors.value.containsKey("description"))

    viewModel.updateDescription("Valid description")
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.fieldErrors.value.containsKey("description"))
  }

  @Test
  fun updatePrice_sanitizesSpecialCharactersAndLimitsDecimals() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    // Tests: special char removal + decimal limiting + multiple decimal points
    viewModel.updatePrice("$12.999.50")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("12.99", viewModel.formState.value.price)
  }

  @Test
  fun updatePrice_withMinusSign_removesIt() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updatePrice("-12.99")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("12.99", viewModel.formState.value.price)
  }

  @Test
  fun updateCapacity_sanitizesAllNonDigits() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    // Tests: leading zeros + special chars + minus sign removal
    viewModel.updateCapacity("-00#100@abc")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("100", viewModel.formState.value.capacity)
  }

  @Test
  fun updateDate_clearsDateInPastError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateDate("01/01/2020")
    viewModel.updateStartTime("10:00")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assert(
        viewModel.fieldErrors.value.containsKey(
            EventFormViewModel.ValidationError.DATE_IN_PAST.key))

    viewModel.updateDate("01/01/2030")
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(
        viewModel.fieldErrors.value.containsKey(
            EventFormViewModel.ValidationError.DATE_IN_PAST.key))
  }

  @Test
  fun updateStartTime_clearsDateInPastError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateDate("01/01/2020")
    viewModel.updateStartTime("10:00")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assert(
        viewModel.fieldErrors.value.containsKey(
            EventFormViewModel.ValidationError.DATE_IN_PAST.key))

    viewModel.updateDate("01/01/2030")
    viewModel.updateStartTime("15:00")
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(
        viewModel.fieldErrors.value.containsKey(
            EventFormViewModel.ValidationError.DATE_IN_PAST.key))
  }

  @Test
  fun `loadEvent populates tags from event`() = runTest {
    // Arrange: Event with tags
    val eventWithTags = testEvent.copy(tags = listOf("TECH", "BUSINESS"))
    coEvery { mockRepository.getEventById("event-with-tags") } returns flowOf(eventWithTags)

    // Act
    viewModel.loadEvent("event-with-tags")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val formState = viewModel.formState.value
    assertEquals(2, formState.selectedTags.size)
    assertTrue(formState.selectedTags.contains(EventTag.TECH))
    assertTrue(formState.selectedTags.contains(EventTag.BUSINESS))
  }

  @Test
  fun `loadEvent handles invalid tags gracefully`() = runTest {
    // Arrange: Event with invalid and valid tags
    val eventWithMixedTags = testEvent.copy(tags = listOf("TECH", "INVALID_TAG", "BUSINESS"))
    coEvery { mockRepository.getEventById("mixed-tags") } returns flowOf(eventWithMixedTags)

    // Act
    viewModel.loadEvent("mixed-tags")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert: Should only load valid tags
    val formState = viewModel.formState.value
    assertEquals(2, formState.selectedTags.size)
    assertTrue(formState.selectedTags.contains(EventTag.TECH))
    assertTrue(formState.selectedTags.contains(EventTag.BUSINESS))
    assertFalse(formState.selectedTags.contains(EventTag.SPORTS))
  }
}
