package ch.onepass.onepass.ui.editevent

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditEventViewModelTest {

  private lateinit var viewModel: EditEventViewModel
  private lateinit var mockRepository: EventRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk(relaxed = true)
    viewModel = EditEventViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial UI state is idle with empty events`() {
    val uiState = viewModel.uiState.value

    assertEquals(emptyList<Event>(), uiState.events)
    assertFalse(uiState.isLoading)
    assertNull(uiState.error)
  }

  @Test
  fun `loadUserEvents sets loading state`() = runTest {
    val testEvents = listOf(Event(eventId = "1", title = "Test Event"))
    coEvery { mockRepository.getEventsByOrganization("user-id") } returns flowOf(testEvents)

    viewModel.loadUserEvents("user-id")

    // Initially loading
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(testEvents, uiState.events)
    assertFalse(uiState.isLoading)
  }

  @Test
  fun `loadUserEvents handles repository success`() = runTest {
    val testEvents =
        listOf(Event(eventId = "1", title = "Event 1"), Event(eventId = "2", title = "Event 2"))
    coEvery { mockRepository.getEventsByOrganization("user-123") } returns flowOf(testEvents)

    viewModel.loadUserEvents("user-123")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(2, uiState.events.size)
    assertEquals("Event 1", uiState.events[0].title)
    assertFalse(uiState.isLoading)
    assertNull(uiState.error)
  }

  @Test
  fun `loadUserEvents handles repository error`() = runTest {
    coEvery { mockRepository.getEventsByOrganization("user-id") } throws Exception("Network error")

    viewModel.loadUserEvents("user-id")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(emptyList<Event>(), uiState.events)
    assertFalse(uiState.isLoading)
    assertEquals("Network error", uiState.error)
  }

  @Test
  fun `refreshEvents reloads data`() = runTest {
    val testEvents = listOf(Event(eventId = "1", title = "Refreshed Event"))
    coEvery { mockRepository.getEventsByOrganization("user-id") } returns flowOf(testEvents)

    viewModel.refreshEvents("user-id")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(1, uiState.events.size)
    assertEquals("Refreshed Event", uiState.events[0].title)
  }

  @Test
  fun `clearError removes error message`() = runTest {
    coEvery { mockRepository.getEventsByOrganization("user-id") } throws Exception("Error")

    viewModel.loadUserEvents("user-id")
    advanceUntilIdle()

    // Error should be present
    assertNotNull(viewModel.uiState.value.error)

    // Clear error
    viewModel.clearError()

    // Error should be cleared
    assertNull(viewModel.uiState.value.error)
  }
}
