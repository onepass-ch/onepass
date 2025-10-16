package ch.onepass.onepass.ui.map

import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.utils.EventTestData
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests to verify that map pins are correctly displayed for published events with location data.
 */
class MapPinsVisibilityTest : FirestoreTestBase() {

  private lateinit var mapViewModel: MapViewModel
  private lateinit var userId: String

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
      mapViewModel = MapViewModel(repository)
    }
  }

  @Test
  fun onlyPublishedEventsWithLocationShowPins() = runTest {
    // Arrange: Create events with different statuses and location configurations
    val publishedEventWithLocation =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    val draftEventWithLocation =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.DRAFT,
            location = EventTestData.createTestLocation(),
        )

    val publishedEventWithoutLocation =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = null,
        )

    val cancelledEventWithLocation =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.CANCELLED,
            location = EventTestData.createTestLocation(),
        )

    // Act: Add all events to repository and get their actual IDs
    val publishedWithLocId = repository.createEvent(publishedEventWithLocation).getOrNull()!!
    repository.createEvent(draftEventWithLocation)
    repository.createEvent(publishedEventWithoutLocation)
    repository.createEvent(cancelledEventWithLocation)

    // Wait for ViewModel to process events
    mapViewModel.refreshEvents()

    // Assert: Only published events with location should be in the UI state
    val uiState = mapViewModel.uiState.first()
    assertEquals("Should only show published events with location", 1, uiState.events.size)

    val visibleEvent = uiState.events.first()
    assertEquals(EventStatus.PUBLISHED, visibleEvent.status)
    assertTrue("Event should have location", visibleEvent.location?.coordinates != null)

    // Use the actual ID from creation
    assertEquals(publishedWithLocId, visibleEvent.eventId)
  }

  @Test
  fun eventsWithoutCoordinatesAreFilteredOut() = runTest {
    // Arrange: Create published events with and without coordinates
    val eventWithCoordinates =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(latitude = 46.5191, longitude = 6.5668),
        )

    val eventWithNullCoordinates =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation().copy(coordinates = null),
        )

    val eventWithNullLocation =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = null,
        )

    // Act: Add events and refresh
    val withCoordsId = repository.createEvent(eventWithCoordinates).getOrNull()!!
    repository.createEvent(eventWithNullCoordinates)
    repository.createEvent(eventWithNullLocation)

    mapViewModel.refreshEvents()

    // Assert: Only event with valid coordinates should be visible
    val uiState = mapViewModel.uiState.first()
    assertEquals("Should only show events with valid coordinates", 1, uiState.events.size)

    // Use the actual ID from creation
    assertEquals(withCoordsId, uiState.events.first().eventId)
  }

  @Test
  fun eventsRemovedWhenDeleted() = runTest {
    // Arrange: Add a published event with location
    val event =
        EventTestData.createTestEvent(
            organizerId = userId,
            status = EventStatus.PUBLISHED,
            location = EventTestData.createTestLocation(),
        )

    val eventId = repository.createEvent(event).getOrNull()!!

    // Wait for the event to be processed by the ViewModel
    var uiState = mapViewModel.uiState.first { it.events.isNotEmpty() }

    // Verify event is visible initially
    assertEquals("Event should be visible initially", 1, uiState.events.size)
    assertEquals(eventId, uiState.events.first().eventId)

    // Act: Delete the event
    repository.deleteEvent(eventId)

    // Wait for the ViewModel to reflect the deletion
    uiState = mapViewModel.uiState.first { it.events.isEmpty() }

    // Assert: Event should no longer be visible
    assertTrue("Event should be removed after deletion", uiState.events.isEmpty())
  }
}
