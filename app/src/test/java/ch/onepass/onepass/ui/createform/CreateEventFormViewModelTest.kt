package ch.onepass.onepass.ui.createform

import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.ui.eventform.createform.CreateEventFormViewModel
import ch.onepass.onepass.ui.eventform.createform.CreateEventFormViewModel.ValidationError
import ch.onepass.onepass.ui.eventform.createform.CreateEventUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
  private lateinit var mockEventRepository: EventRepository
  private lateinit var mockOrganizationRepository: OrganizationRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  private val testOrganization =
      Organization(
          id = "test-org-id",
          name = "Test Organization",
          description = "Test Description",
          ownerId = "test-owner-id",
          status = OrganizationStatus.ACTIVE)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockEventRepository = mockk(relaxed = true)
    mockOrganizationRepository = mockk(relaxed = true)

    // Mock the organization repository to return a test organization
    coEvery { mockOrganizationRepository.getOrganizationById(any()) } returns
        flowOf(testOrganization)

    viewModel = CreateEventFormViewModel(mockEventRepository, mockOrganizationRepository)

    // Set the organization ID for the viewModel
    viewModel.setOrganizationId("test-org-id")
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

        viewModel.createEvent()

        val fieldErrors = viewModel.fieldErrors.value
        assertTrue(fieldErrors.containsKey(ValidationError.TITLE.key))
        assertEquals(ValidationError.TITLE.message, fieldErrors[ValidationError.TITLE.key])
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

        viewModel.createEvent()

        val fieldErrors = viewModel.fieldErrors.value
        assertTrue(fieldErrors.containsKey(ValidationError.DESCRIPTION.key))
        assertEquals(
            ValidationError.DESCRIPTION.message, fieldErrors[ValidationError.DESCRIPTION.key])
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

        viewModel.createEvent()

        val fieldErrors = viewModel.fieldErrors.value
        assertTrue(fieldErrors.containsKey(ValidationError.DATE.key))
        assertEquals(ValidationError.DATE.message, fieldErrors[ValidationError.DATE.key])
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

        viewModel.createEvent()

        val fieldErrors = viewModel.fieldErrors.value
        assertTrue(fieldErrors.containsKey(ValidationError.START_TIME.key))
        assertEquals(
            ValidationError.START_TIME.message, fieldErrors[ValidationError.START_TIME.key])
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

        viewModel.createEvent()

        val fieldErrors = viewModel.fieldErrors.value
        assertTrue(fieldErrors.containsKey(ValidationError.END_TIME.key))
        assertEquals(ValidationError.END_TIME.message, fieldErrors[ValidationError.END_TIME.key])
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

        viewModel.createEvent()

        val fieldErrors = viewModel.fieldErrors.value
        assertTrue(fieldErrors.containsKey(ValidationError.LOCATION.key))
        assertEquals(ValidationError.LOCATION.message, fieldErrors[ValidationError.LOCATION.key])
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

        viewModel.createEvent()

        val fieldErrors = viewModel.fieldErrors.value
        assertTrue(fieldErrors.containsKey(ValidationError.PRICE_INVALID.key))
        assertEquals(
            ValidationError.PRICE_INVALID.message, fieldErrors[ValidationError.PRICE_INVALID.key])
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

        viewModel.createEvent()

        val fieldErrors = viewModel.fieldErrors.value
        assertTrue(fieldErrors.containsKey(ValidationError.CAPACITY_INVALID.key))
        assertEquals(
            ValidationError.CAPACITY_INVALID.message,
            fieldErrors[ValidationError.CAPACITY_INVALID.key])
      }

  @Test
  fun `createEvent succeeds with valid data`() =
      runTest(testDispatcher) {
        // Mock successful event creation
        coEvery { mockEventRepository.createEvent(any()) } returns Result.success("new-event-id")

        // Fill in valid form data
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Test Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25.50")
        viewModel.updateCapacity("100")

        viewModel.createEvent()

        // Verify repository was called
        coVerify(timeout = 2000) { mockEventRepository.createEvent(any()) }

        // Verify UI state is Success (form is reset after success)
        val uiState = viewModel.uiState.value
        assertTrue(uiState is CreateEventUiState.Success)
        assertEquals("new-event-id", (uiState as CreateEventUiState.Success).eventId)
      }

  @Test
  fun `createEvent handles repository failure`() =
      runTest(testDispatcher) {
        // Mock repository failure
        coEvery { mockEventRepository.createEvent(any()) } returns
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

        viewModel.createEvent()

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
    viewModel.createEvent()
    advanceUntilIdle()

    // Clear error
    viewModel.clearError()

    val uiState = viewModel.uiState.value
    assertTrue(uiState is CreateEventUiState.Idle)
  }

  @Test
  fun `createEvent calls repository with valid data`() =
      runTest(testDispatcher) {
        coEvery { mockEventRepository.createEvent(any()) } returns Result.success("event-id")

        // Fill in valid form data
        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Test Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("100")

        viewModel.createEvent()

        // Verify repository was called
        coVerify(timeout = 2000) { mockEventRepository.createEvent(any()) }
      }

  @Test
  fun `createEvent validates all required fields before creating`() =
      runTest(testDispatcher) {
        // Test with all valid fields - should succeed
        coEvery { mockEventRepository.createEvent(any()) } returns Result.success("event-id")

        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Test Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25.50")
        viewModel.updateCapacity("100")

        viewModel.createEvent()

        // Verify repository was called (indicating validation passed)
        coVerify(timeout = 2000) { mockEventRepository.createEvent(any()) }
      }

  @Test
  fun `resetForm after successful creation`() =
      runTest(testDispatcher) {
        coEvery { mockEventRepository.createEvent(any()) } returns Result.success("event-id")

        viewModel.updateTitle("Test Event")
        viewModel.updateDescription("Test Description")
        viewModel.updateDate("25/12/2025")
        viewModel.updateStartTime("14:30")
        viewModel.updateEndTime("16:30")
        viewModel.updateLocation("EPFL")
        viewModel.updatePrice("25")
        viewModel.updateCapacity("150")

        viewModel.createEvent()

        // Wait for async completion
        advanceUntilIdle()

        // After successful creation, form should be reset
        viewModel.resetForm()

        val formState = viewModel.formState.value
        assertEquals("", formState.title)
        assertEquals("", formState.description)

        val fieldErrors = viewModel.fieldErrors.value
        assertTrue(fieldErrors.isEmpty())
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
