package ch.onepass.onepass.ui.feed

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private val testEvent1 =
      Event(
          eventId = "test1",
          title = "Test Event 1",
          description = "Description 1",
          organizerId = "org1",
          organizerName = "Organizer 1",
          status = EventStatus.PUBLISHED,
          location = Location(GeoPoint(46.5197, 6.6323), "Lausanne"),
          startTime = Timestamp(Date()),
          capacity = 100,
          ticketsRemaining = 50,
          ticketsIssued = 50,
          pricingTiers = emptyList(),
      )

  private val testEvent2 =
      Event(
          eventId = "test2",
          title = "Test Event 2",
          description = "Description 2",
          organizerId = "org2",
          organizerName = "Organizer 2",
          status = EventStatus.PUBLISHED,
          location = Location(GeoPoint(46.5191, 6.5668), "EPFL"),
          startTime = Timestamp(Date()),
          capacity = 200,
          ticketsRemaining = 100,
          ticketsIssued = 100,
          pricingTiers = emptyList(),
      )

  private class MockEventRepository(
      private val events: List<Event> = emptyList(),
      private val shouldThrowError: Boolean = false,
  ) : EventRepository {
    override fun getAllEvents(): Flow<List<Event>> = flowOf(events)

    override fun searchEvents(query: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventById(eventId: String): Flow<Event?> =
        flowOf(events.find { it.eventId == eventId })

    override fun getEventsByOrganization(orgId: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventsByLocation(center: Location, radiusKm: Double): Flow<List<Event>> =
        flowOf(emptyList())

    override fun getEventsByTag(tag: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getFeaturedEvents(): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> {
      if (shouldThrowError) {
        throw Exception("Test error")
      }
      return flowOf(if (status == EventStatus.PUBLISHED) events else emptyList())
    }

    override suspend fun createEvent(event: Event): Result<String> = Result.success("test-id")

    override suspend fun updateEvent(event: Event): Result<Unit> = Result.success(Unit)

    override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun feedViewModel_initialState_isEmpty() = runTest {
    val mockRepository = MockEventRepository()
    val viewModel = FeedViewModel(mockRepository)

    val state = viewModel.uiState.value

    assertEquals(emptyList<Event>(), state.events)
    assertFalse(state.isLoading)
    assertNull(state.error)
    assertEquals("SWITZERLAND", state.location)
  }

  @Test
  fun feedViewModel_loadEvents_updatesStateWithEvents() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    assertEquals(events, state.events)
    assertFalse(state.isLoading)
    assertNull(state.error)
  }

  @Test
  fun feedViewModel_loadEvents_setsLoadingState() = runTest {
    val mockRepository = MockEventRepository()
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()

    val stateWhileLoading = viewModel.uiState.value
    assertTrue(stateWhileLoading.isLoading)

    testDispatcher.scheduler.advanceUntilIdle()

    val finalState = viewModel.uiState.value
    assertFalse(finalState.isLoading)
  }

  @Test
  fun feedViewModel_loadEvents_handlesError() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    assertEquals(emptyList<Event>(), state.events)
    assertFalse(state.isLoading)
    assertEquals("Test error", state.error)
  }

  @Test
  fun feedViewModel_refreshEvents_reloadsEvents() = runTest {
    val events = listOf(testEvent1)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.refreshEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    assertEquals(events, state.events)
    assertFalse(state.isLoading)
    assertNull(state.error)
  }

  @Test
  fun feedViewModel_clearError_removesErrorMessage() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify error exists
    assertTrue(viewModel.uiState.value.error != null)

    viewModel.clearError()

    assertNull(viewModel.uiState.value.error)
  }

  @Test
  fun feedViewModel_setLocation_updatesLocation() = runTest {
    val mockRepository = MockEventRepository()
    val viewModel = FeedViewModel(mockRepository)

    val newLocation = "GENEVA"
    viewModel.setLocation(newLocation)

    assertEquals(newLocation, viewModel.uiState.value.location)
  }

  @Test
  fun feedViewModel_loadEvents_filtersPublishedEvents() = runTest {
    val publishedEvent = testEvent1.copy(status = EventStatus.PUBLISHED)
    val draftEvent = testEvent2.copy(status = EventStatus.DRAFT)
    val mockRepository = MockEventRepository(listOf(publishedEvent))
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    assertEquals(1, state.events.size)
    assertEquals(publishedEvent.eventId, state.events.first().eventId)
    assertEquals(EventStatus.PUBLISHED, state.events.first().status)
  }

  @Test
  fun feedViewModel_loadEventsMultipleTimes_updatesStateCorrectly() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    assertEquals(events, state.events)
    assertFalse(state.isLoading)
    assertNull(state.error)
  }

  @Test
  fun feedViewModel_emptyEventsList_handledCorrectly() = runTest {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    assertEquals(emptyList<Event>(), state.events)
    assertFalse(state.isLoading)
    assertNull(state.error)
  }

  @Test
  fun feedViewModel_applyFiltersToCurrentEvents_filtersByRegion() = runTest {
    val events =
        listOf(
            testEvent1.copy(location = Location(GeoPoint(46.5197, 6.6323), "Lausanne", "Vaud")),
            testEvent2.copy(location = Location(GeoPoint(46.2044, 6.1432), "Geneva", "Geneva")),
        )
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Apply region filter
    val filters = EventFilters(region = "Vaud")
    viewModel.applyFiltersToCurrentEvents(filters)
    testDispatcher.scheduler.advanceUntilIdle()

    val filteredState = viewModel.uiState.value
    assertEquals(1, filteredState.events.size)
    assertEquals("Lausanne", filteredState.events.first().location?.name)
  }

  @Test
  fun feedViewModel_applyFiltersToCurrentEvents_filtersByDateRange() = runTest {
    val calendar = Calendar.getInstance()
    val today = calendar.timeInMillis

    calendar.add(Calendar.DAY_OF_MONTH, 1)
    val tomorrow = calendar.timeInMillis

    calendar.add(Calendar.DAY_OF_MONTH, 6)
    val nextWeek = calendar.timeInMillis

    val events =
        listOf(
            testEvent1.copy(startTime = Timestamp(Date(today))),
            testEvent2.copy(startTime = Timestamp(Date(nextWeek))),
        )
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Apply date range filter (today only)
    val filters = EventFilters(dateRange = today..tomorrow)
    viewModel.applyFiltersToCurrentEvents(filters)
    testDispatcher.scheduler.advanceUntilIdle()

    val filteredState = viewModel.uiState.value
    assertEquals(1, filteredState.events.size)
    assertEquals(testEvent1.eventId, filteredState.events.first().eventId)
  }

  @Test
  fun feedViewModel_applyFiltersToCurrentEvents_filtersSoldOut() = runTest {
    val events =
        listOf(
            testEvent1.copy(ticketsRemaining = 0), // Sold out
            testEvent2.copy(ticketsRemaining = 50), // Available
        )
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Apply hide sold out filter
    val filters = EventFilters(hideSoldOut = true)
    viewModel.applyFiltersToCurrentEvents(filters)
    testDispatcher.scheduler.advanceUntilIdle()

    val filteredState = viewModel.uiState.value
    assertEquals(1, filteredState.events.size)
    assertEquals(testEvent2.eventId, filteredState.events.first().eventId)
  }

  @Test
  fun feedViewModel_applyFiltersToCurrentEvents_combinesMultipleFilters() = runTest {
    val events =
        listOf(
            testEvent1.copy(
                location = Location(GeoPoint(46.5197, 6.6323), "Lausanne", "Vaud"),
                ticketsRemaining = 0,
            ),
            testEvent2.copy(
                location = Location(GeoPoint(46.5197, 6.6323), "Lausanne", "Vaud"),
                ticketsRemaining = 50,
            ),
        )
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Apply combined filters
    val filters = EventFilters(region = "Vaud", hideSoldOut = true)
    viewModel.applyFiltersToCurrentEvents(filters)
    testDispatcher.scheduler.advanceUntilIdle()

    val filteredState = viewModel.uiState.value
    assertEquals(1, filteredState.events.size)
    assertEquals(testEvent2.eventId, filteredState.events.first().eventId)
  }

  @Test
  fun feedViewModel_applyFiltersToCurrentEvents_clearsFilters() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Apply filter then clear
    val filters = EventFilters(region = "Vaud")
    viewModel.applyFiltersToCurrentEvents(filters)
    testDispatcher.scheduler.advanceUntilIdle()

    val clearedFilters = EventFilters()
    viewModel.applyFiltersToCurrentEvents(clearedFilters)
    testDispatcher.scheduler.advanceUntilIdle()

    val finalState = viewModel.uiState.value
    assertEquals(2, finalState.events.size)
  }
}
