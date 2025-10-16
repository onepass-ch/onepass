package ch.onepass.onepass.ui.createform

import ch.onepass.onepass.model.event.EventRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CreateEventFormViewModel.
 *
 * These tests verify the ViewModel's business logic, validation, and state management using a
 * mocked repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateEventFormViewModelTest {

  private lateinit var viewModel: CreateEventFormViewModel
  private lateinit var mockRepository: EventRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk(relaxed = true)

    // Setup default behavior for createEvent
    coEvery { mockRepository.createEvent(any()) } returns Result.success("test-event-id")

    viewModel = CreateEventFormViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial form state is empty`() {
    val formState = viewModel.formState.value

    assertEquals("", formState.title)
    assertEquals("", formState.description)
    assertEquals("", formState.startTime)
    assertEquals("", formState.endTime)
    assertEquals("", formState.date)
    assertEquals("", formState.location)
    assertEquals("", formState.price)
    assertEquals("", formState.capacity)
  }

  @Test
  fun `initial UI state is Idle`() {
    val uiState = viewModel.uiState.value
    assertTrue(uiState is CreateEventUiState.Idle)
  }

  @Test
  fun `updateTitle updates form state`() {
    viewModel.updateTitle("New Event Title")

    assertEquals("New Event Title", viewModel.formState.value.title)
  }

  @Test
  fun `updateDescription updates form state`() {
    viewModel.updateDescription("New Description")

    assertEquals("New Description", viewModel.formState.value.description)
  }

  @Test
  fun `updateStartTime updates form state`() {
    viewModel.updateStartTime("14:30")

    assertEquals("14:30", viewModel.formState.value.startTime)
  }

  @Test
  fun `updateEndTime updates form state`() {
    viewModel.updateEndTime("16:30")

    assertEquals("16:30", viewModel.formState.value.endTime)
  }

  @Test
  fun `updateDate updates form state`() {
    viewModel.updateDate("25/12/2025")

    assertEquals("25/12/2025", viewModel.formState.value.date)
  }

  @Test
  fun `updateLocation updates form state`() {
    viewModel.updateLocation("EPFL, Lausanne")

    assertEquals("EPFL, Lausanne", viewModel.formState.value.location)
  }

  @Test
  fun `updatePrice updates form state`() {
    viewModel.updatePrice("25.50")

    assertEquals("25.50", viewModel.formState.value.price)
  }

  @Test
  fun `updateCapacity updates form state`() {
    viewModel.updateCapacity("100")

    assertEquals("100", viewModel.formState.value.capacity)
  }

  @Test
  fun `createEvent fails validation when title is blank`() =
      runTest(testDispatcher) {
        // Leave title blank, fill other fields
        viewModel.updateDescription("Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-id", "Organizer")

        val uiState = viewModel.uiState.value
        assertTrue(uiState is CreateEventUiState.Error)
        assertTrue((uiState as CreateEventUiState.Error).message.contains("Title is required"))
      }

  @Test
  fun `createEvent fails validation when description is blank`() =
      runTest(testDispatcher) {
        viewModel.updateTitle("Test Event")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-id", "Organizer")

        val uiState = viewModel.uiState.value
        assertTrue(uiState is CreateEventUiState.Error)
        assertTrue(
            (uiState as CreateEventUiState.Error).message.contains("Description is required"))
      }

  @Test
  fun `createEvent fails validation when date is blank`() =
      runTest(testDispatcher) {
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Description")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-id", "Organizer")

        val uiState = viewModel.uiState.value
        assertTrue(uiState is CreateEventUiState.Error)
        assertTrue((uiState as CreateEventUiState.Error).message.contains("Date is required"))
      }

  @Test
  fun `createEvent fails validation when startTime is blank`() =
      runTest(testDispatcher) {
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-id", "Organizer")

        val uiState = viewModel.uiState.value
        assertTrue(uiState is CreateEventUiState.Error)
        assertTrue((uiState as CreateEventUiState.Error).message.contains("Start time is required"))
      }

  @Test
  fun `createEvent fails validation when endTime is blank`() =
      runTest(testDispatcher) {
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-id", "Organizer")

        val uiState = viewModel.uiState.value
        assertTrue(uiState is CreateEventUiState.Error)
        assertTrue((uiState as CreateEventUiState.Error).message.contains("End time is required"))
      }

  @Test
  fun `createEvent fails validation when location is blank`() =
      runTest(testDispatcher) {
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-id", "Organizer")

        val uiState = viewModel.uiState.value
        assertTrue(uiState is CreateEventUiState.Error)
        assertTrue((uiState as CreateEventUiState.Error).message.contains("Location is required"))
      }

  @Test
  fun `createEvent fails validation when price is invalid`() =
      runTest(testDispatcher) {
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("invalid")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-id", "Organizer")

        val uiState = viewModel.uiState.value
        assertTrue(uiState is CreateEventUiState.Error)
        assertTrue(
            (uiState as CreateEventUiState.Error).message.contains("Price must be a valid number"))
      }

  @Test
  fun `createEvent fails validation when capacity is invalid`() =
      runTest(testDispatcher) {
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("invalid")

        viewModel.createEvent("org-id", "Organizer")

        val uiState = viewModel.uiState.value
        assertTrue(uiState is CreateEventUiState.Error)
        assertTrue(
            (uiState as CreateEventUiState.Error)
                .message
                .contains("Capacity must be a valid number"))
      }

  @Test
  fun `createEvent succeeds with valid data`() =
      runTest(testDispatcher) {
        // Mock successful event creation
        coEvery { mockRepository.createEvent(any()) } returns Result.success("new-event-id")

        // Fill in valid form data
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Test Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25.50")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-id", "Organizer Name")

        // Verify repository was called
        coVerify(timeout = 2000) { mockRepository.createEvent(any()) }

        // Verify UI state is Success (form is reset after success)
        val formState = viewModel.formState.value
        assertEquals("", formState.title)
      }

  @Test
  fun `createEvent handles repository failure`() =
      runTest(testDispatcher) {
        // Mock repository failure
        coEvery { mockRepository.createEvent(any()) } returns
            Result.failure(Exception("Network error"))

        // Fill in valid form data
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Test Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-id", "Organizer")

        advanceUntilIdle()

        // Verify UI state is Error
        val uiState = viewModel.uiState.value
        assertTrue(uiState is CreateEventUiState.Error)
        assertTrue((uiState as CreateEventUiState.Error).message.contains("Network error"))
      }

  @Test
  fun `resetForm clears all form fields`() {
    // Fill in form data
    viewModel.updateTitle("Test Event")
    viewModel.updateDescription("Test Description")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("EPFL")
    viewModel.updatePrice("25")
    viewModel.updateCapacity("100")

    // Reset form
    viewModel.resetForm()

    // Verify all fields are cleared
    val formState = viewModel.formState.value
    assertEquals("", formState.title)
    assertEquals("", formState.description)
    assertEquals("", formState.date)
    assertEquals("", formState.startTime)
    assertEquals("", formState.endTime)
    assertEquals("", formState.location)
    assertEquals("", formState.price)
    assertEquals("", formState.capacity)
  }

  @Test
  fun `resetForm clears UI state`() {
    viewModel.resetForm()

    val uiState = viewModel.uiState.value
    assertTrue(uiState is CreateEventUiState.Idle)
  }

  @Test
  fun `clearError sets UI state to Idle`() = runTest {
    // Trigger an error
    viewModel.createEvent("org-id", "Organizer")
    advanceUntilIdle()

    // Clear error
    viewModel.clearError()

    val uiState = viewModel.uiState.value
    assertTrue(uiState is CreateEventUiState.Idle)
  }

  @Test
  fun `createEvent calls repository with valid data`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createEvent(any()) } returns Result.success("event-id")

        // Fill in valid form data
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Test Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-123", "Test Organizer")

        // Verify repository was called
        coVerify(timeout = 2000) { mockRepository.createEvent(any()) }
      }

  @Test
  fun `createEvent validates all required fields before creating`() =
      runTest(testDispatcher) {
        // Test with all valid fields - should succeed
        coEvery { mockRepository.createEvent(any()) } returns Result.success("event-id")

        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Test Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25.50")
        viewModel.updateCapacity("100")

        viewModel.createEvent("org-id", "Organizer")

        // Verify repository was called (indicating validation passed)
        coVerify(timeout = 2000) { mockRepository.createEvent(any()) }
      }

  @Test
  fun `resetForm after successful creation`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createEvent(any()) } returns Result.success("event-id")

        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Test Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("150")

        viewModel.createEvent("org-id", "Organizer")

        // Wait for async completion
        advanceUntilIdle()

        // After successful creation, form should be reset
        val formState = viewModel.formState.value
        assertEquals("", formState.title)
        assertEquals("", formState.description)
      }

  @Test
  fun `multiple field updates preserve previous values`() {
    viewModel.updateTitle("Title 1")
    viewModel.updateDescription("Description 1")
    viewModel.updatePrice("10")

    assertEquals("Title 1", viewModel.formState.value.title)
    assertEquals("Description 1", viewModel.formState.value.description)
    assertEquals("10", viewModel.formState.value.price)

    // Update only title
    viewModel.updateTitle("Title 2")

    // Other fields should remain unchanged
    assertEquals("Title 2", viewModel.formState.value.title)
    assertEquals("Description 1", viewModel.formState.value.description)
    assertEquals("10", viewModel.formState.value.price)
  }
}
