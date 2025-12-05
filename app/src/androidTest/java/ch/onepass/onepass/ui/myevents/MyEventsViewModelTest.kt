package ch.onepass.onepass.ui.myevents

import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.ticket.TicketRepository
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
class MyEventsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var ticketRepository: TicketRepository
  private lateinit var eventRepository: EventRepository

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    ticketRepository = mockk(relaxed = true)
    eventRepository = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(userId: String? = "test-user"): MyEventsViewModel {
    return MyEventsViewModel(
        ticketRepo = ticketRepository, eventRepo = eventRepository, userId = userId ?: "")
  }

  @Test
  fun init_loadsNothing_whenNoCacheForUser() = runTest(testDispatcher) {
    // Setup: no tickets for user
    coEvery { ticketRepository.getActiveTickets(any()) } returns flowOf(emptyList())
    coEvery { ticketRepository.getExpiredTickets(any()) } returns flowOf(emptyList())

    val viewModel = createViewModel("test-user")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue("Current tickets should be empty", state.currentTickets.isEmpty())
    assertTrue("Expired tickets should be empty", state.expiredTickets.isEmpty())
  }

  @Test
  fun clearCache_withNullUser_doesNothing() = runTest(testDispatcher) {
    // Setup: no tickets
    coEvery { ticketRepository.getActiveTickets(any()) } returns flowOf(emptyList())
    coEvery { ticketRepository.getExpiredTickets(any()) } returns flowOf(emptyList())

    val viewModel = createViewModel(null)
    testDispatcher.scheduler.advanceUntilIdle()

    // When user is null, ViewModel should still initialize without errors
    val state = viewModel.uiState.value
    assertTrue("Current tickets should be empty", state.currentTickets.isEmpty())
    assertTrue("Expired tickets should be empty", state.expiredTickets.isEmpty())
  }
}
