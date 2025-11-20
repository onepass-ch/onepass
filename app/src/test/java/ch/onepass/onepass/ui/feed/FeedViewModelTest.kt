package ch.onepass.onepass.ui.feed

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.ui.feed.FeedViewModel.Companion.LOADED_EVENTS_LIMIT
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
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

    Assert.assertEquals(emptyList<Event>(), state.events)
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
    Assert.assertEquals("SWITZERLAND", state.location)
  }

  @Test
  fun feedViewModel_loadEvents_updatesStateWithEvents() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    Assert.assertEquals(events, state.events)
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
  }

  @Test
  fun feedViewModel_loadEvents_setsLoadingState() = runTest {
    val mockRepository = MockEventRepository()
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()

    val stateWhileLoading = viewModel.uiState.value
    Assert.assertTrue(stateWhileLoading.isLoading)

    testDispatcher.scheduler.advanceUntilIdle()

    val finalState = viewModel.uiState.value
    Assert.assertFalse(finalState.isLoading)
  }

  @Test
  fun feedViewModel_loadEvents_handlesError() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    Assert.assertEquals(emptyList<Event>(), state.events)
    Assert.assertFalse(state.isLoading)
    Assert.assertEquals("Test error", state.error)
  }

  @Test
  fun feedViewModel_refreshEvents_reloadsEvents() = runTest {
    val events = listOf(testEvent1)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.refreshEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    Assert.assertEquals(events, state.events)
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
  }

  @Test
  fun feedViewModel_clearError_removesErrorMessage() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify error exists
    Assert.assertTrue(viewModel.uiState.value.error != null)

    viewModel.clearError()

    Assert.assertNull(viewModel.uiState.value.error)
  }

  @Test
  fun feedViewModel_setLocation_updatesLocation() = runTest {
    val mockRepository = MockEventRepository()
    val viewModel = FeedViewModel(mockRepository)

    val newLocation = "GENEVA"
    viewModel.setLocation(newLocation)

    Assert.assertEquals(newLocation, viewModel.uiState.value.location)
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

    Assert.assertEquals(1, state.events.size)
    Assert.assertEquals(publishedEvent.eventId, state.events.first().eventId)
    Assert.assertEquals(EventStatus.PUBLISHED, state.events.first().status)
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

    Assert.assertEquals(events, state.events)
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
  }

  @Test
  fun feedViewModel_emptyEventsList_handledCorrectly() = runTest {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    Assert.assertEquals(emptyList<Event>(), state.events)
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
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
    Assert.assertEquals(1, filteredState.events.size)
    Assert.assertEquals("Lausanne", filteredState.events.first().location?.name)
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
    Assert.assertEquals(1, filteredState.events.size)
    Assert.assertEquals(testEvent1.eventId, filteredState.events.first().eventId)
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
    Assert.assertEquals(1, filteredState.events.size)
    Assert.assertEquals(testEvent2.eventId, filteredState.events.first().eventId)
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
    Assert.assertEquals(1, filteredState.events.size)
    Assert.assertEquals(testEvent2.eventId, filteredState.events.first().eventId)
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
    Assert.assertEquals(2, finalState.events.size)
  }

  @Test
  fun feedViewModel_refreshEvents_setsRefreshingState() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.refreshEvents()

    // Check that isRefreshing is set to true immediately
    val stateWhileRefreshing = viewModel.uiState.value
    assertTrue(stateWhileRefreshing.isRefreshing)
    assertFalse(stateWhileRefreshing.isLoading)

    testDispatcher.scheduler.advanceUntilIdle()

    // Check that isRefreshing is set to false after completion
    val finalState = viewModel.uiState.value
    assertFalse(finalState.isRefreshing)
    assertFalse(finalState.isLoading)
  }

  @Test
  fun feedViewModel_refreshEvents_handlesError() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.refreshEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    assertEquals(emptyList<Event>(), state.events)
    assertFalse(state.isRefreshing)
    assertEquals("Test error", state.error)
  }

  @Test
  fun feedViewModel_refreshEvents_clearsRefreshingStateOnError() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.refreshEvents()

    // Verify refreshing state is set initially
    assertTrue(viewModel.uiState.value.isRefreshing)

    testDispatcher.scheduler.advanceUntilIdle()

    // Verify refreshing state is cleared even on error
    assertFalse(viewModel.uiState.value.isRefreshing)
  }

  @Test
  fun feedViewModel_refreshEvents_resetsErrorState() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository)

    // First load with error
    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.uiState.value.error != null)

    // Refresh should clear error initially
    viewModel.refreshEvents()
    assertNull(viewModel.uiState.value.error)
    assertTrue(viewModel.uiState.value.isRefreshing)

    testDispatcher.scheduler.advanceUntilIdle()

    // Error should be set again if refresh fails
    assertTrue(viewModel.uiState.value.error != null)
  }

  @Test
  fun feedViewModel_refreshEvents_timeoutHandling() = runTest {
    // This test verifies that the timeout in refreshEvents doesn't cause issues
    // in normal operation (events load faster than timeout)
    val events = listOf(testEvent1)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.refreshEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    assertEquals(events, state.events)
    assertFalse(state.isRefreshing)
    assertNull(state.error)
  }

  @Test
  fun feedViewModel_refreshEvents_respectsEventLimit() = runTest {
    // Create more than LOADED_EVENTS_LIMIT events
    val manyEvents =
        List(LOADED_EVENTS_LIMIT + 5) { index -> testEvent1.copy(eventId = "event$index") }
    val mockRepository = MockEventRepository(manyEvents)
    val viewModel = FeedViewModel(mockRepository)

    viewModel.refreshEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should only load up to LOADED_EVENTS_LIMIT events
    assertEquals(LOADED_EVENTS_LIMIT, state.events.size)
    assertFalse(state.isRefreshing)
    assertNull(state.error)
  }

  @Test
  fun feedViewModel_concurrentLoadAndRefresh_handledCorrectly() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository)

    // Start both operations
    viewModel.loadEvents()
    viewModel.refreshEvents()

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should end up with valid state regardless of order
    assertEquals(events, state.events)
    assertFalse(state.isLoading)
    assertFalse(state.isRefreshing)
    assertNull(state.error)
  }
}
