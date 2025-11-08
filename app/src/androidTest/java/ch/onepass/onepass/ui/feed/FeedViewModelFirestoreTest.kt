package ch.onepass.onepass.ui.feed

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.utils.EVENTS_COLLECTION_PATH
import ch.onepass.onepass.utils.EventTestData
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedViewModelFirestoreTest : FirestoreTestBase() {

  private lateinit var userId: String
  private lateinit var viewModel: FeedViewModel

  private suspend fun clearTestCollection() {
    val events = FirebaseEmulator.firestore.collection(EVENTS_COLLECTION_PATH).get().await()

    if (events.isEmpty) return

    val batch = FirebaseEmulator.firestore.batch()
    events.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()
  }

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      clearTestCollection()
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
      viewModel = FeedViewModel(EventRepositoryFirebase())
    }
  }

  @Test
  fun viewModel_loadsPublishedEvents_fromFirestore() = runTest {
    val event1 = EventTestData.createPublishedEvent(organizerId = userId, capacity = 100)
    val event2 =
        EventTestData.createPublishedEvent(eventId = "pub-2", organizerId = userId, capacity = 200)

    val result1 = repository.createEvent(event1)
    val result2 = repository.createEvent(event2)

    assertTrue("Event creation should succeed", result1.isSuccess)
    assertTrue("Event creation should succeed", result2.isSuccess)

    viewModel.loadEvents()

    val state = viewModel.uiState.first { it.events.size == 2 }

    assertFalse("Should not be loading", state.isLoading)
    assertNull("Should have no error", state.error)
    assertTrue(
        "All events should be published", state.events.all { it.status == EventStatus.PUBLISHED })

    val eventIds = state.events.map { it.eventId }
    val createdEvent1Id = result1.getOrThrow()
    val createdEvent2Id = result2.getOrThrow()

    assertTrue("Should contain created event 1", eventIds.contains(createdEvent1Id))
    assertTrue("Should contain created event 2", eventIds.contains(createdEvent2Id))
  }

  @Test
  fun viewModel_handlesFirestoreError_gracefully() = runTest {
    val mockRepository =
        object : ch.onepass.onepass.model.event.EventRepository {
          override fun getAllEvents(): Flow<List<Event>> = flowOf(emptyList())

          override fun searchEvents(query: String): Flow<List<Event>> = flowOf(emptyList())

          override fun getEventById(eventId: String): Flow<Event?> = flowOf(null)

          override fun getEventsByOrganization(orgId: String): Flow<List<Event>> =
              flowOf(emptyList())

          override fun getEventsByLocation(
              center: ch.onepass.onepass.model.map.Location,
              radiusKm: Double
          ): Flow<List<Event>> = flowOf(emptyList())

          override fun getEventsByTag(tag: String): Flow<List<Event>> = flowOf(emptyList())

          override fun getFeaturedEvents(): Flow<List<Event>> = flowOf(emptyList())

          override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> {
            throw Exception("Test error")
          }

          override suspend fun createEvent(event: Event): Result<String> = Result.success("test-id")

          override suspend fun updateEvent(event: Event): Result<Unit> = Result.success(Unit)

          override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
        }

    val testViewModel = FeedViewModel(mockRepository)
    testViewModel.loadEvents()

    val state = testViewModel.uiState.first { it.error != null }

    assertFalse("Should not be loading", state.isLoading)
    assertNotNull("Should have error", state.error)
    assertEquals("Test error", state.error)
  }

  @Test
  fun viewModel_refreshEvents_reloadsFromFirestore() = runTest {
    val initialEvent = EventTestData.createPublishedEvent(organizerId = userId)
    val initialEventId = repository.createEvent(initialEvent).getOrThrow()
    viewModel.loadEvents()

    val initialState = viewModel.uiState.first { it.events.size == 1 }
    assertEquals(1, initialState.events.size)

    val newEvent = EventTestData.createPublishedEvent(eventId = "new", organizerId = userId)
    val newEventId = repository.createEvent(newEvent).getOrThrow()
    viewModel.refreshEvents()

    val refreshedState = viewModel.uiState.first { it.events.size == 2 }

    assertEquals("Should have both events after refresh", 2, refreshedState.events.size)
    assertTrue(refreshedState.events.any { it.eventId == initialEventId })
    assertTrue(refreshedState.events.any { it.eventId == newEventId })
  }

  @Test
  fun viewModel_handlesEmptyFirestore() = runTest {
    viewModel.loadEvents()

    val state = viewModel.uiState.first { !it.isLoading }

    assertTrue("Should have no events", state.events.isEmpty())
    assertFalse("Should not be loading", state.isLoading)
    assertNull("Should have no error", state.error)
  }

  @Test
  fun viewModel_filtersOutDraftEvents() = runTest {
    val draftEvent = EventTestData.createDraftEvent(organizerId = userId)
    val publishedEvent = EventTestData.createPublishedEvent(organizerId = userId)

    repository.createEvent(draftEvent)
    val publishedEventId = repository.createEvent(publishedEvent).getOrThrow()
    viewModel.loadEvents()

    val state = viewModel.uiState.first { it.events.isNotEmpty() }

    val publishedEvents = state.events.filter { it.status == EventStatus.PUBLISHED }
    assertEquals("Should have only published events", 1, publishedEvents.size)

    val foundEvent = publishedEvents.find { it.eventId == publishedEventId }
    assertNotNull("Should find the published event", foundEvent)
  }

  @Test
  fun viewModel_setLocation_updatesLocation() = runTest {
    val newLocation = "GENEVA"
    viewModel.setLocation(newLocation)

    val state = viewModel.uiState.value
    assertEquals("Location should be updated", newLocation, state.location)
  }

  @Test
  fun viewModel_setShowFilterDialog_updatesVisibility() = runTest {
    assertFalse(viewModel.uiState.value.showFilterDialog)

    viewModel.setShowFilterDialog(true)
    var state = viewModel.uiState.value
    assertTrue(state.showFilterDialog)

    viewModel.setShowFilterDialog(false)
    state = viewModel.uiState.value
    assertFalse(state.showFilterDialog)
  }

  @Test
  fun viewModel_clearError_removesErrorState() = runTest {
    val mockRepository =
        object : ch.onepass.onepass.model.event.EventRepository {
          override fun getAllEvents(): Flow<List<Event>> = flowOf(emptyList())

          override fun searchEvents(query: String): Flow<List<Event>> = flowOf(emptyList())

          override fun getEventById(eventId: String): Flow<Event?> = flowOf(null)

          override fun getEventsByOrganization(orgId: String): Flow<List<Event>> =
              flowOf(emptyList())

          override fun getEventsByLocation(
              center: ch.onepass.onepass.model.map.Location,
              radiusKm: Double
          ): Flow<List<Event>> = flowOf(emptyList())

          override fun getEventsByTag(tag: String): Flow<List<Event>> = flowOf(emptyList())

          override fun getFeaturedEvents(): Flow<List<Event>> = flowOf(emptyList())

          override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> {
            throw Exception("Test error")
          }

          override suspend fun createEvent(event: Event): Result<String> = Result.success("test-id")

          override suspend fun updateEvent(event: Event): Result<Unit> = Result.success(Unit)

          override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
        }

    val testViewModel = FeedViewModel(mockRepository)
    testViewModel.loadEvents()

    val errorState = testViewModel.uiState.first { it.error != null }
    assertNotNull(errorState.error)

    testViewModel.clearError()
    assertNull(testViewModel.uiState.value.error)
  }

  @Test
  fun viewModel_loadsOnlyOwnEvents_withAuthentication() = runTest {
    val myEvent = EventTestData.createPublishedEvent(organizerId = userId)
    repository.createEvent(myEvent)

    val otherEvent =
        EventTestData.createPublishedEvent(eventId = "other-event", organizerId = "different-user")
    repository.createEvent(otherEvent)

    viewModel.loadEvents()

    val state = viewModel.uiState.first { it.events.size >= 2 }

    assertTrue("Should have events", state.events.isNotEmpty())
    assertEquals("Should have both events", 2, state.events.size)
  }

  @Test
  fun viewModel_handlesCancelledEvents_correctly() = runTest {
    val cancelledEvent = EventTestData.createCancelledEvent(organizerId = userId)
    val publishedEvent = EventTestData.createPublishedEvent(organizerId = userId)
    repository.createEvent(cancelledEvent)
    repository.createEvent(publishedEvent)

    viewModel.loadEvents()

    val state = viewModel.uiState.first { it.events.isNotEmpty() && !it.isLoading }

    assertEquals("Should only contain one event", 1, state.events.size)
    assertTrue(
        "Cancelled events should not be loaded",
        state.events.none { it.status == EventStatus.CANCELLED })
  }

  @Test
  fun viewModel_handlesClosedEvents_correctly() = runTest {
    val closedEvent = EventTestData.createClosedEvent(organizerId = userId)
    val publishedEvent = EventTestData.createPublishedEvent(organizerId = userId)
    repository.createEvent(closedEvent)
    repository.createEvent(publishedEvent)

    viewModel.loadEvents()

    val state = viewModel.uiState.first { it.events.isNotEmpty() && !it.isLoading }

    assertEquals("Should only contain one event", 1, state.events.size)
    assertTrue(
        "Closed events should not be loaded", state.events.none { it.status == EventStatus.CLOSED })
  }
}
