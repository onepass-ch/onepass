package ch.onepass.onepass.ui.map

import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.FakeEventRepository
import ch.onepass.onepass.utils.EventTestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MapPinsVisibilityTest {

  private lateinit var fakeRepo: FakeEventRepository
  private lateinit var mapViewModel: MapViewModel
  private val userId = "test-user"

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher())

    fakeRepo = FakeEventRepository()
    mapViewModel = MapViewModel(eventRepository = fakeRepo)

    testScheduler.advanceUntilIdle()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    mapViewModel.onCleared()
    Dispatchers.resetMain()
  }

  @Test
  fun onlyPublishedEventsWithLocationShowPins() = runTest {
    val publishedWithLoc =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation())
    val draftWithLoc =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.DRAFT,
            location = EventTestData.createTestLocation())
    val publishedNoLoc =
        EventTestData.createTestEvent(
            organizerId = userId, status = EventStatus.PUBLISHED, location = null)
    val cancelledWithLoc =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.CANCELLED,
            location = EventTestData.createTestLocation())

    fakeRepo.setEvents(listOf(publishedWithLoc, draftWithLoc, publishedNoLoc, cancelledWithLoc))
    mapViewModel.refreshEvents()
    testScheduler.advanceUntilIdle()

    val uiState = mapViewModel.uiState.first()
    assertEquals(1, uiState.events.size)
    val visible = uiState.events.first()
    assertEquals(EventStatus.PUBLISHED, visible.status)
    assertNotNull(visible.location?.coordinates)
    assertEquals(publishedWithLoc.eventId, visible.eventId)
  }

  @Test
  fun eventsWithoutCoordinatesAreFilteredOut() = runTest {
    val withCoords =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(latitude = 46.5191, longitude = 6.5668))
    val nullCoords =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation().copy(coordinates = null))
    val nullLocation =
        EventTestData.createTestEvent(
            organizerId = userId, status = EventStatus.PUBLISHED, location = null)

    fakeRepo.setEvents(listOf(withCoords, nullCoords, nullLocation))
    mapViewModel.refreshEvents()
    testScheduler.advanceUntilIdle()

    val uiState = mapViewModel.uiState.first()
    assertEquals(1, uiState.events.size)
    assertEquals(withCoords.eventId, uiState.events.first().eventId)
  }

  @Test
  fun eventsRemovedWhenDeleted() = runTest {
    val event =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation())

    fakeRepo.addEvent(event)
    mapViewModel.refreshEvents()
    testScheduler.advanceUntilIdle()

    var uiState = mapViewModel.uiState.first { it.events.isNotEmpty() }
    assertEquals(1, uiState.events.size)
    assertEquals(event.eventId, uiState.events.first().eventId)

    fakeRepo.deleteEvent(event.eventId)
    testScheduler.advanceUntilIdle()

    uiState = mapViewModel.uiState.first { it.events.isEmpty() }
    assertTrue(uiState.events.isEmpty())
  }
}
