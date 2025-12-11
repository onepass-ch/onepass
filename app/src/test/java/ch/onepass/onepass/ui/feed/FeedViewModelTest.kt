package ch.onepass.onepass.ui.feed

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.ui.feed.FeedViewModel.Companion.LOADED_EVENTS_LIMIT
import ch.onepass.onepass.utils.EventTestData
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
          location = Location(GeoPoint(46.5197, 6.6323), "Lausanne", "Vaud"),
          startTime = Timestamp(Date()),
          capacity = 100,
          ticketsRemaining = 50,
          ticketsIssued = 50,
          pricingTiers = emptyList(),
          tags = emptyList(),
          createdAt = Timestamp(1000, 0))

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
          tags = emptyList(),
          createdAt = Timestamp(2000, 0))

  // Mocks for new dependencies
  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser

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
      return if (shouldThrowError) {
        throw RuntimeException("Test error")
      } else {
        flowOf(events.filter { it.status == status })
      }
    }

    override suspend fun createEvent(event: Event): Result<String> = Result.success("test-id")

    override suspend fun updateEvent(event: Event): Result<Unit> = Result.success(Unit)

    override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun addEventImage(eventId: String, imageUrl: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun removeEventImage(eventId: String, imageUrl: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun updateEventImages(eventId: String, imageUrls: List<String>): Result<Unit> =
        Result.success(Unit)
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockUserRepository = mockk(relaxed = true)
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)

    // Default mock behavior
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-uid"
    every { mockUserRepository.getFavoriteEvents("test-uid") } returns flowOf(emptySet())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun feedViewModel_initialState_isEmpty() = runTest {
    val mockRepository = MockEventRepository()
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

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
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    Assert.assertEquals(2, state.events.size)
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
  }

  @Test
  fun feedViewModel_loadEvents_setsLoadingState() = runTest {
    val mockRepository = MockEventRepository()
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

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
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

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
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.refreshEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    Assert.assertEquals(1, state.events.size)
    Assert.assertFalse(state.isRefreshing)
    Assert.assertNull(state.error)
  }

  @Test
  fun feedViewModel_clearError_removesErrorMessage() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

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
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    val newLocation = "GENEVA"
    viewModel.setLocation(newLocation)

    Assert.assertEquals(newLocation, viewModel.uiState.value.location)
  }

  @Test
  fun feedViewModel_loadEvents_filtersPublishedEvents() = runTest {
    val publishedEvent = testEvent1.copy(status = EventStatus.PUBLISHED)
    val draftEvent = testEvent2.copy(status = EventStatus.DRAFT)
    val mockRepository = MockEventRepository(listOf(publishedEvent, draftEvent))
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

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
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    Assert.assertEquals(2, state.events.size)
    Assert.assertFalse(state.isLoading)
    Assert.assertNull(state.error)
  }

  @Test
  fun feedViewModel_emptyEventsList_handledCorrectly() = runTest {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

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
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Apply region filter
    val filters = EventFilters(region = "Vaud")
    viewModel.applyFiltersToCurrentEvents(filters)

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
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Apply date range filter (today only)
    val filters = EventFilters(dateRange = today..tomorrow)
    viewModel.applyFiltersToCurrentEvents(filters)

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
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Apply hide sold out filter
    val filters = EventFilters(hideSoldOut = true)
    viewModel.applyFiltersToCurrentEvents(filters)

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
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Apply combined filters
    val filters = EventFilters(region = "Vaud", hideSoldOut = true)
    viewModel.applyFiltersToCurrentEvents(filters)

    val filteredState = viewModel.uiState.value
    Assert.assertEquals(1, filteredState.events.size)
    Assert.assertEquals(testEvent2.eventId, filteredState.events.first().eventId)
  }

  @Test
  fun feedViewModel_applyFiltersToCurrentEvents_clearsFilters() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Apply filter then clear
    val filters = EventFilters(region = "Vaud")
    viewModel.applyFiltersToCurrentEvents(filters)

    val clearedFilters = EventFilters()
    viewModel.applyFiltersToCurrentEvents(clearedFilters)

    val finalState = viewModel.uiState.value
    Assert.assertEquals(2, finalState.events.size)
  }

  @Test
  fun feedViewModel_refreshEvents_setsRefreshingState() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.refreshEvents()

    // Check that isRefreshing is set to true immediately
    val stateWhileRefreshing = viewModel.uiState.value
    Assert.assertTrue(stateWhileRefreshing.isRefreshing)
    Assert.assertFalse(stateWhileRefreshing.isLoading)

    testDispatcher.scheduler.advanceUntilIdle()

    // Check that isRefreshing is set to false after completion
    val finalState = viewModel.uiState.value
    Assert.assertFalse(finalState.isRefreshing)
    Assert.assertFalse(finalState.isLoading)
  }

  @Test
  fun feedViewModel_refreshEvents_handlesError() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.refreshEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    Assert.assertEquals(emptyList<Event>(), state.events)
    Assert.assertFalse(state.isRefreshing)
    Assert.assertEquals("Test error", state.error)
  }

  @Test
  fun feedViewModel_refreshEvents_clearsRefreshingStateOnError() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.refreshEvents()

    // Verify refreshing state is set initially
    Assert.assertTrue(viewModel.uiState.value.isRefreshing)

    testDispatcher.scheduler.advanceUntilIdle()

    // Verify refreshing state is cleared even on error
    Assert.assertFalse(viewModel.uiState.value.isRefreshing)
  }

  @Test
  fun feedViewModel_refreshEvents_resetsErrorState() = runTest {
    val mockRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    // First load with error
    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertTrue(viewModel.uiState.value.error != null)

    // Refresh should clear error initially
    viewModel.refreshEvents()
    Assert.assertNull(viewModel.uiState.value.error)
    Assert.assertTrue(viewModel.uiState.value.isRefreshing)

    testDispatcher.scheduler.advanceUntilIdle()

    // Error should be set again if refresh fails
    Assert.assertTrue(viewModel.uiState.value.error != null)
  }

  @Test
  fun feedViewModel_refreshEvents_timeoutHandling() = runTest {
    // This test verifies that the timeout in refreshEvents doesn't cause issues
    // in normal operation (events load faster than timeout)
    val events = listOf(testEvent1)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.refreshEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    Assert.assertEquals(1, state.events.size)
    Assert.assertFalse(state.isRefreshing)
    Assert.assertNull(state.error)
  }

  @Test
  fun feedViewModel_refreshEvents_respectsEventLimit() = runTest {
    // Create more than LOADED_EVENTS_LIMIT events
    val manyEvents =
        List(LOADED_EVENTS_LIMIT + 5) { index -> testEvent1.copy(eventId = "event$index") }
    val mockRepository = MockEventRepository(manyEvents)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.refreshEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should only load up to LOADED_EVENTS_LIMIT events
    Assert.assertEquals(LOADED_EVENTS_LIMIT, state.events.size)
    Assert.assertFalse(state.isRefreshing)
    Assert.assertNull(state.error)
  }

  @Test
  fun feedViewModel_concurrentLoadAndRefresh_handledCorrectly() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    // Start both operations
    viewModel.loadEvents()
    viewModel.refreshEvents()

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should end up with valid state regardless of order
    Assert.assertEquals(2, state.events.size)
    Assert.assertFalse(state.isLoading)
    Assert.assertFalse(state.isRefreshing)
    Assert.assertNull(state.error)
  }

  @Test
  fun feedViewModel_applyFiltersToCurrentEvents_updatesState() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    // Initial load
    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()
    Assert.assertEquals(2, viewModel.uiState.value.events.size)

    // Apply filter that matches only testEvent1
    val filters = EventFilters(region = "Vaud") // testEvent1 is Lausanne/Vaud
    viewModel.applyFiltersToCurrentEvents(filters)

    val filteredState = viewModel.uiState.value
    Assert.assertEquals(1, filteredState.events.size)
    Assert.assertEquals(testEvent1.eventId, filteredState.events.first().eventId)
  }

  @Test
  fun feedViewModel_refreshEvents_triggersRecalculateWithFilters() = runTest {
    val events = listOf(testEvent1, testEvent2)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Set a filter
    viewModel.applyFiltersToCurrentEvents(EventFilters(region = "Vaud"))
    Assert.assertEquals(1, viewModel.uiState.value.events.size)

    // Refresh events
    viewModel.refreshEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Filter should persist after refresh
    Assert.assertEquals(1, viewModel.uiState.value.events.size)
    Assert.assertEquals("test1", viewModel.uiState.value.events[0].eventId)
  }

  @Test
  fun feedViewModel_recalculateRecommendations_emptyEvents_updatesState() = runTest {
    val mockRepository = MockEventRepository(emptyList())
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertTrue(state.events.isEmpty())
    Assert.assertFalse(state.isLoading)
  }

  @Test
  fun recommendEvents_sorts_by_tag_affinity_recency_and_like_status() {
    val viewModel = FeedViewModel(MockEventRepository(), mockUserRepository, mockAuth)

    val currentTimestamp = Timestamp.now().seconds

    val likedTechEvent1 = EventTestData.createTestEvent(eventId = "L1", tags = listOf("TECH"))
    val likedTechEvent2 =
        EventTestData.createTestEvent(eventId = "L2", tags = listOf("TECH", "COMMUNITY"))
    val likedFoodEvent = EventTestData.createTestEvent(eventId = "L3", tags = listOf("FOOD"))

    val targetEventA =
        EventTestData.createTestEvent(
            eventId = "A", tags = listOf("TECH"), createdAt = Timestamp.now())

    val targetEventB =
        EventTestData.createTestEvent(
            eventId = "B",
            tags = listOf("TECH", "BUSINESS"),
            createdAt = Timestamp(Date((currentTimestamp - 86400 * 10) * 1000L)))

    val targetEventC =
        EventTestData.createTestEvent(
            eventId = "C", tags = listOf("SPORTS"), createdAt = Timestamp.now())

    val targetEventD =
        EventTestData.createTestEvent(
            eventId = "D",
            tags = listOf("MEETUP"),
            createdAt = Timestamp(Date((currentTimestamp - 86400 * 10) * 1000L)))

    val allEvents = listOf(targetEventA, targetEventB, targetEventC, targetEventD)
    val likedIds = setOf("A", "L1", "L2", "L3")

    val recommended = viewModel.recommendEvents(allEvents, likedIds)

    assertEquals("A", recommended[0].eventId)
    assertEquals("C", recommended[1].eventId)
    assertEquals("B", recommended[2].eventId)
    assertEquals("D", recommended[3].eventId)
  }

  @Test
  fun recommendEvents_penalizes_expiration_soon() = runTest {
    val viewModel = FeedViewModel(MockEventRepository(), mockUserRepository, mockAuth)

    val now = Instant.now()

    // Ends in 30 minutes (should be penalized)
    val endingSoonEvent =
        EventTestData.createTestEvent(
            eventId = "ending-soon",
            endTime = Timestamp(Date.from(now.plus(30, ChronoUnit.MINUTES))),
            tags = listOf("TECH"))

    // Ends tomorrow (should be normal)
    val normalEvent =
        EventTestData.createTestEvent(
            eventId = "normal",
            endTime = Timestamp(Date.from(now.plus(24, ChronoUnit.HOURS))),
            tags = listOf("TECH"))

    val events = listOf(endingSoonEvent, normalEvent)

    // Both match user interest "TECH" equally, but endingSoon should be penalized
    val recommended = viewModel.recommendEvents(events, emptySet())

    assertEquals("normal", recommended[0].eventId)
    assertEquals("ending-soon", recommended[1].eventId)
  }

  @Test
  fun recommendEvents_boosts_urgency_starts_soon() = runTest {
    val viewModel = FeedViewModel(MockEventRepository(), mockUserRepository, mockAuth)

    val now = Instant.now()

    // Starts in 3 hours (should be boosted)
    val startingSoonEvent =
        EventTestData.createTestEvent(
            eventId = "starting-soon",
            startTime = Timestamp(Date.from(now.plus(3, ChronoUnit.HOURS))),
            endTime = Timestamp(Date.from(now.plus(5, ChronoUnit.HOURS))),
            createdAt =
                Timestamp(
                    Date.from(
                        now.minus(10, ChronoUnit.DAYS))) // Old creation to avoid recency boost
            )

    // Starts next week
    val futureEvent =
        EventTestData.createTestEvent(
            eventId = "future",
            startTime = Timestamp(Date.from(now.plus(7, ChronoUnit.DAYS))),
            endTime = Timestamp(Date.from(now.plus(8, ChronoUnit.DAYS))),
            createdAt = Timestamp(Date.from(now.minus(10, ChronoUnit.DAYS))))

    val events = listOf(startingSoonEvent, futureEvent)
    val recommended = viewModel.recommendEvents(events, emptySet())

    assertEquals("starting-soon", recommended[0].eventId)
    assertEquals("future", recommended[1].eventId)
  }

  @Test
  fun toggleFavoritesMode_filtersLikedEvents() = runTest {
    val likedEvent = testEvent1 // ID: test1
    val unlikedEvent = testEvent2 // ID: test2
    val allEvents = listOf(likedEvent, unlikedEvent)

    val mockRepository = MockEventRepository(allEvents)
    // Set up user repo to return 'test1' as liked
    every { mockUserRepository.getFavoriteEvents("test-uid") } returns flowOf(setOf("test1"))

    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)
    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    // Initial state: Main feed should exclude liked event (test1) and show unliked (test2)
    Assert.assertFalse(viewModel.uiState.value.isShowingFavorites)
    Assert.assertEquals(1, viewModel.uiState.value.events.size)
    Assert.assertEquals("test2", viewModel.uiState.value.events[0].eventId)

    // Toggle to favorites mode
    viewModel.toggleFavoritesMode()

    // Favorites feed: Should ONLY show liked event (test1)
    Assert.assertTrue(viewModel.uiState.value.isShowingFavorites)
    Assert.assertEquals(1, viewModel.uiState.value.events.size)
    Assert.assertEquals("test1", viewModel.uiState.value.events[0].eventId)

    // Toggle back
    viewModel.toggleFavoritesMode()
    Assert.assertFalse(viewModel.uiState.value.isShowingFavorites)
    Assert.assertEquals("test2", viewModel.uiState.value.events[0].eventId)
  }

  @Test
  fun feedViewModel_loadEvents_filtersPastEvents() = runTest {
    // Create a fixed time point
    val fixedTime = Timestamp(System.currentTimeMillis() / 1000, 0)
    val mockTimeProvider = MockTimeProvider(fixedTime)

    val now = fixedTime.seconds * 1000
    val pastEvent =
        testEvent1.copy(
            eventId = "past", endTime = Timestamp(Date(now - 86400000)) // Ended yesterday
            )
    val futureEvent =
        testEvent2.copy(
            eventId = "future", endTime = Timestamp(Date(now + 86400000)) // Ends tomorrow
            )

    val events = listOf(pastEvent, futureEvent)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth, mockTimeProvider)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should only show future event, not past event
    Assert.assertEquals(1, state.events.size)
    Assert.assertEquals("future", state.events.first().eventId)
  }

  @Test
  fun feedViewModel_loadEvents_filtersSoldOutEvents() = runTest {
    val availableEvent = testEvent1.copy(eventId = "available", ticketsRemaining = 10)
    val soldOutEvent = testEvent2.copy(eventId = "soldout", ticketsRemaining = 0)

    val events = listOf(availableEvent, soldOutEvent)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should only show available event, not sold out
    Assert.assertEquals(1, state.events.size)
    Assert.assertEquals("available", state.events.first().eventId)
  }

  @Test
  fun feedViewModel_loadEvents_filtersPastAndSoldOutEvents() = runTest {
    val now = System.currentTimeMillis()

    val activeEvent =
        testEvent1.copy(
            eventId = "active", ticketsRemaining = 10, endTime = Timestamp(Date(now + 86400000)))
    val pastEvent =
        testEvent1.copy(
            eventId = "past", ticketsRemaining = 10, endTime = Timestamp(Date(now - 86400000)))
    val soldOutEvent =
        testEvent1.copy(
            eventId = "soldout", ticketsRemaining = 0, endTime = Timestamp(Date(now + 86400000)))
    val pastAndSoldOut =
        testEvent1.copy(
            eventId = "past-soldout",
            ticketsRemaining = 0,
            endTime = Timestamp(Date(now - 86400000)))

    val events = listOf(activeEvent, pastEvent, soldOutEvent, pastAndSoldOut)
    val mockRepository = MockEventRepository(events)
    val viewModel = FeedViewModel(mockRepository, mockUserRepository, mockAuth)

    viewModel.loadEvents()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should only show active event
    Assert.assertEquals(1, state.events.size)
    Assert.assertEquals("active", state.events.first().eventId)
  }
}
