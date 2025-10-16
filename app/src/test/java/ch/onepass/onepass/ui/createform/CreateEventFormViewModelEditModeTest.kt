package ch.onepass.onepass.ui.createform

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEventFormViewModelEditModeTest {

  private lateinit var viewModel: CreateEventFormViewModel
  private lateinit var mockRepository: EventRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  private val testEvent =
      Event(
          eventId = "test-event-id",
          title = "Test Event",
          description = "Test Description",
          organizerId = "org-id",
          organizerName = "Test Organizer",
          status = EventStatus.DRAFT,
          startTime = Timestamp(Date(2025, 10, 25, 14, 30)),
          endTime = Timestamp(Date(2025, 10, 25, 16, 30)),
          capacity = 100,
          ticketsRemaining = 100,
          pricingTiers =
              listOf(PricingTier(name = "General", price = 25.0, quantity = 100, remaining = 100)))

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk(relaxed = true)
    viewModel = CreateEventFormViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `isEditMode returns false initially`() {
    assertFalse(viewModel.isEditMode())
  }

  @Test
  fun `loadEvent sets edit mode to true`() = runTest {
    coEvery { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    viewModel.loadEvent("test-event-id")
    advanceUntilIdle()

    assertTrue(viewModel.isEditMode())
  }

  @Test
  fun `loadEvent populates form fields from event`() = runTest {
    coEvery { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    viewModel.loadEvent("test-event-id")
    advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("Test Event", formState.title)
    assertEquals("Test Description", formState.description)
    assertEquals("25.0", formState.price)
    assertEquals("100", formState.capacity)
  }

  @Test
  fun `loadEvent handles event not found`() = runTest {
    coEvery { mockRepository.getEventById("non-existent") } returns flowOf(null)

    viewModel.loadEvent("non-existent")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertTrue(uiState is CreateEventUiState.Error)
    assertEquals("Event not found", (uiState as CreateEventUiState.Error).message)
  }

  @Test
  fun `saveEvent calls updateEvent when in edit mode`() = runTest {
    coEvery { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    coEvery { mockRepository.updateEvent(any()) } returns Result.success(Unit)

    // Load event to enter edit mode
    viewModel.loadEvent("test-event-id")
    advanceUntilIdle()

    // Modify a field
    viewModel.updateTitle("Updated Title")

    // Save event
    viewModel.saveEvent("org-id", "Test Organizer")
    advanceUntilIdle()

    // Verify updateEvent was called, not createEvent
    coVerify(exactly = 1) { mockRepository.updateEvent(any()) }
    coVerify(exactly = 0) { mockRepository.createEvent(any()) }
  }

  @Test
  fun `saveEvent calls createEvent when not in edit mode`() = runTest {
    coEvery { mockRepository.createEvent(any()) } returns Result.success("new-event-id")

    // Fill form without loading an event
    viewModel.updateTitle("New Event")
    viewModel.updateDescription("New Description")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("EPFL")
    viewModel.updatePrice("20")
    viewModel.updateCapacity("50")

    viewModel.saveEvent("org-id", "Test Organizer")
    advanceUntilIdle()

    // Verify createEvent was called, not updateEvent
    coVerify(exactly = 1) { mockRepository.createEvent(any()) }
    coVerify(exactly = 0) { mockRepository.updateEvent(any()) }
  }

  @Test
  fun `resetForm clears edit mode`() = runTest {
    coEvery { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)

    viewModel.loadEvent("test-event-id")
    advanceUntilIdle()

    assertTrue(viewModel.isEditMode())

    viewModel.resetForm()

    assertFalse(viewModel.isEditMode())
  }

  @Test
  fun `saveEvent shows success state after update`() = runTest {
    coEvery { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    coEvery { mockRepository.updateEvent(any()) } returns Result.success(Unit)

    viewModel.loadEvent("test-event-id")
    advanceUntilIdle()

    viewModel.saveEvent("org-id", "Test Organizer")
    advanceUntilIdle()

    // After successful update, form is reset
    val formState = viewModel.formState.value
    assertEquals("", formState.title)
  }

  @Test
  fun `saveEvent handles update failure`() = runTest {
    coEvery { mockRepository.getEventById("test-event-id") } returns flowOf(testEvent)
    coEvery { mockRepository.updateEvent(any()) } returns Result.failure(Exception("Update failed"))

    viewModel.loadEvent("test-event-id")
    advanceUntilIdle()

    viewModel.saveEvent("org-id", "Test Organizer")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertTrue(uiState is CreateEventUiState.Error)
    assertTrue((uiState as CreateEventUiState.Error).message.contains("Update failed"))
  }
}
