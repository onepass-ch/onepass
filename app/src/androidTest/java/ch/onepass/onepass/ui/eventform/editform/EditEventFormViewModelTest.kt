package ch.onepass.onepass.ui.eventform.editform

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.ui.eventform.EventFormViewModel.ValidationError
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class EditEventFormViewModelTest {

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

    viewModel = EditEventFormViewModel(mockRepository)
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
    assertEquals(EditEventUiState.Idle, viewModel.uiState.value)
    val form = viewModel.formState.value
    assertEquals("Original Title", form.title)
    assertEquals("Original Description", form.description)
    assertEquals("Original Location", form.location)
    assertEquals("25/12/2025", form.date)
    assertEquals("14:30", form.startTime)
    assertEquals("16:30", form.endTime)
    assertEquals("25", form.price)
    assertEquals("100", form.capacity)
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
    assertTrue(state is EditEventUiState.LoadError)
    assertEquals("Event not found", (state as EditEventUiState.LoadError).message)
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
    assertEquals("New Title", viewModel.formState.value.title)
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.TITLE.key))
  }

  @Test
  fun updateDescriptionUpdatesStateAndClearsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateDescription("") // Set invalid
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.fieldErrors.value.containsKey(ValidationError.DESCRIPTION.key))

    viewModel.updateDescription("New Desc")
    assertEquals("New Desc", viewModel.formState.value.description)
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.DESCRIPTION.key))
  }

  @Test
  fun updateStartTimeUpdatesStateAndClearsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateStartTime("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.fieldErrors.value.containsKey(ValidationError.START_TIME.key))

    viewModel.updateStartTime("10:00")
    assertEquals("10:00", viewModel.formState.value.startTime)
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.START_TIME.key))
  }

  @Test
  fun updateEndTimeUpdatesStateAndClearsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateEndTime("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.fieldErrors.value.containsKey(ValidationError.END_TIME.key))

    viewModel.updateEndTime("20:00")
    assertEquals("20:00", viewModel.formState.value.endTime)
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.END_TIME.key))
  }

  @Test
  fun updateDateUpdatesStateAndClearsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateDate("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.fieldErrors.value.containsKey(ValidationError.DATE.key))

    viewModel.updateDate("01/01/2026")
    assertEquals("01/01/2026", viewModel.formState.value.date)
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.DATE.key))
  }

  @Test
  fun updateLocationUpdatesStateAndClearsError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateLocation("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.fieldErrors.value.containsKey(ValidationError.LOCATION.key))

    viewModel.updateLocation("New Location")
    assertEquals("New Location", viewModel.formState.value.location)
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.LOCATION.key))
  }

  @Test
  fun updatePriceUpdatesStateAndClearsAllPriceErrors() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updatePrice("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.fieldErrors.value.containsKey(ValidationError.PRICE_EMPTY.key))

    viewModel.updatePrice("50")
    assertEquals("50", viewModel.formState.value.price)
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.PRICE_EMPTY.key))
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.PRICE_INVALID.key))
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.PRICE_NEGATIVE.key))
  }

  @Test
  fun updateCapacityUpdatesStateAndClearsAllCapacityErrors() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateCapacity("")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.fieldErrors.value.containsKey(ValidationError.CAPACITY_EMPTY.key))

    viewModel.updateCapacity("50")
    assertEquals("50", viewModel.formState.value.capacity)
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.CAPACITY_EMPTY.key))
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.CAPACITY_INVALID.key))
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.CAPACITY_NEGATIVE.key))
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
    assertTrue(errors.containsKey(ValidationError.TITLE.key))
    assertEquals(ValidationError.TITLE.message, errors[ValidationError.TITLE.key])
    assertTrue(state is EditEventUiState.Error)
    assertEquals("Please fix validation errors", (state as EditEventUiState.Error).message)
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
    assertEquals(EditEventUiState.Success, viewModel.uiState.value)
    assertTrue(viewModel.fieldErrors.value.isEmpty())
    coVerify { mockRepository.updateEvent(match { it.title == newTitle }) }
  }

  @Test
  fun updateEventWithoutLoadingEventFirstSetsErrorState() = runTest {
    // Arrange
    val freshViewModel = EditEventFormViewModel(mockRepository) // originalEvent is null

    // Act
    freshViewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = freshViewModel.uiState.value
    assertTrue(state is EditEventUiState.Error)
    assertEquals("Original event not loaded", (state as EditEventUiState.Error).message)
  }

  @Test
  fun clearErrorWhenInErrorStateSetsIdleState() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateTitle("")
    viewModel.updateEvent() // Puts state in Error
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.uiState.value is EditEventUiState.Error)

    // Act
    viewModel.clearError()

    // Assert
    assertEquals(EditEventUiState.Idle, viewModel.uiState.value)
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
    assertTrue(state is EditEventUiState.LoadError)
    assertEquals("Database error", (state as EditEventUiState.LoadError).message)
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
    assertEquals("", viewModel.formState.value.location)
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
    assertEquals("0", viewModel.formState.value.price)
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
    assertEquals("0", viewModel.formState.value.price)
  }

  @Test
  fun updateStartTimeClearsTimeError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateStartTime("20:00")
    viewModel.updateEndTime("10:00")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.fieldErrors.value.containsKey(ValidationError.TIME.key))

    viewModel.updateStartTime("09:00")
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.TIME.key))
  }

  @Test
  fun updateEndTimeClearsTimeError() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateStartTime("20:00")
    viewModel.updateEndTime("10:00")
    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.fieldErrors.value.containsKey(ValidationError.TIME.key))

    viewModel.updateEndTime("21:00")
    assertFalse(viewModel.fieldErrors.value.containsKey(ValidationError.TIME.key))
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
    assertEquals(8, errors.size)
    assertTrue(errors.containsKey(ValidationError.TITLE.key))
    assertTrue(errors.containsKey(ValidationError.DESCRIPTION.key))
    assertTrue(errors.containsKey(ValidationError.DATE.key))
    assertTrue(errors.containsKey(ValidationError.START_TIME.key))
    assertTrue(errors.containsKey(ValidationError.END_TIME.key))
    assertTrue(errors.containsKey(ValidationError.LOCATION.key))
    assertTrue(errors.containsKey(ValidationError.PRICE_EMPTY.key))
    assertTrue(errors.containsKey(ValidationError.CAPACITY_EMPTY.key))
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
    assertTrue(errors.containsKey(ValidationError.TIME.key))
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
    assertTrue(errors.containsKey(ValidationError.TIME.key))
  }

  @Test
  fun updateEventValidationFailsForInvalidPrice() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updatePrice("abc")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    assertTrue(errors.containsKey(ValidationError.PRICE_INVALID.key))
  }

  @Test
  fun updateEventValidationFailsForNegativePrice() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updatePrice("-10")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    assertTrue(errors.containsKey(ValidationError.PRICE_NEGATIVE.key))
  }

  @Test
  fun updateEventValidationFailsForInvalidCapacity() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateCapacity("abc")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    assertTrue(errors.containsKey(ValidationError.CAPACITY_INVALID.key))
  }

  @Test
  fun updateEventValidationFailsForZeroCapacity() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateCapacity("0")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    assertTrue(errors.containsKey(ValidationError.CAPACITY_NEGATIVE.key))
  }

  @Test
  fun updateEventValidationFailsForNegativeCapacity() = runTest {
    viewModel.loadEvent("test-event-id")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.updateCapacity("-5")

    viewModel.updateEvent()
    testDispatcher.scheduler.advanceUntilIdle()

    val errors = viewModel.fieldErrors.value
    assertTrue(errors.containsKey(ValidationError.CAPACITY_NEGATIVE.key))
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
    assertTrue(state is EditEventUiState.Error)
    assertEquals("Failed to process form data", (state as EditEventUiState.Error).message)
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
    assertTrue(state is EditEventUiState.Error)
    assertEquals("Failed to process form data", (state as EditEventUiState.Error).message)
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
    assertTrue(state is EditEventUiState.Error)
    assertEquals("DB write failed", (state as EditEventUiState.Error).message)
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
    assertEquals(EditEventUiState.Updating, viewModel.uiState.value)

    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(EditEventUiState.Success, viewModel.uiState.value)
  }

  @Test
  fun clearErrorWhenNotInErrorStateDoesNothing() = runTest {
    // Arrange
    viewModel.loadEvent("test-event-id") // Puts state in Loading, then Idle
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(EditEventUiState.Idle, viewModel.uiState.value)

    // Act
    viewModel.clearError()

    // Assert
    assertEquals(EditEventUiState.Idle, viewModel.uiState.value)
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
    assertEquals("", viewModel.formState.value.date)
    assertEquals("", viewModel.formState.value.startTime)
    assertEquals("", viewModel.formState.value.endTime)
  }
}
