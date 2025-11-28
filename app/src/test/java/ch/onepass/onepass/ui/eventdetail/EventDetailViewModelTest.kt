package ch.onepass.onepass.ui.eventdetail

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import io.mockk.every
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EventDetailViewModel.
 *
 * These tests verify the ViewModel's behavior for loading event and organization data, handling
 * errors, and managing loading states.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {

  private lateinit var mockEventRepository: EventRepository
  private lateinit var mockOrganizationRepository: OrganizationRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  private val testEvent =
      Event(
          eventId = "event-123",
          title = "Test Event",
          description = "Test Description",
          organizerId = "org-123",
          organizerName = "Test Organizer",
          status = EventStatus.PUBLISHED,
          location = Location(GeoPoint(46.5197, 6.6323), "Lausanne"),
          startTime = Timestamp(Date()),
          capacity = 100,
          ticketsRemaining = 50,
          ticketsIssued = 50,
          pricingTiers = emptyList(),
      )

  private val testOrganization =
      Organization(
          id = "org-123",
          name = "Test Organization",
          description = "Test Org Description",
          ownerId = "user-456",
          status = OrganizationStatus.ACTIVE,
          verified = true,
      )

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockEventRepository = mockk(relaxed = true)
    mockOrganizationRepository = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun eventDetailViewModel_initialState_isLoadingWithNullValues() {
    // Mock event repository to return a flow that doesn't emit immediately
    every { mockEventRepository.getEventById(any()) } returns
        flow {
          // Don't emit anything immediately
        }

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Check initial state before flow emits
    Assert.assertTrue(viewModel.isLoading.value)
    Assert.assertNull(viewModel.event.value)
    Assert.assertNull(viewModel.organization.value)
    Assert.assertNull(viewModel.error.value)
  }

  @Test
  fun eventDetailViewModel_loadEvent_loadsEventAndOrganizationSuccessfully() {
    // Mock successful event loading
    every { mockEventRepository.getEventById("event-123") } returns flowOf(testEvent)

    // Mock successful organization loading
    every { mockOrganizationRepository.getOrganizationById("org-123") } returns
        flowOf(testOrganization)

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify event is loaded
    Assert.assertEquals(testEvent, viewModel.event.value)

    // Verify organization is loaded
    Assert.assertEquals(testOrganization, viewModel.organization.value)

    // Verify loading is complete
    Assert.assertFalse(viewModel.isLoading.value)

    // Verify no errors
    Assert.assertNull(viewModel.error.value)
  }

  @Test
  fun eventDetailViewModel_loadEvent_handlesNullEvent() {
    // Mock event repository returning null (event not found)
    every { mockEventRepository.getEventById("event-123") } returns flowOf(null)

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify event is null
    Assert.assertNull(viewModel.event.value)

    // Verify organization is not loaded
    Assert.assertNull(viewModel.organization.value)

    // Verify loading is complete
    Assert.assertFalse(viewModel.isLoading.value)

    // Verify no error is set for missing event
    Assert.assertNull(viewModel.error.value)
  }

  @Test
  fun eventDetailViewModel_loadEvent_handlesRepositoryError() {
    // Mock event repository throwing an error
    every { mockEventRepository.getEventById("event-123") } returns
        flow { throw Exception("Network error") }

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify error is set
    Assert.assertEquals("Network error", viewModel.error.value)

    // Verify loading is complete
    Assert.assertFalse(viewModel.isLoading.value)

    // Verify event is null
    Assert.assertNull(viewModel.event.value)

    // Verify organization is null
    Assert.assertNull(viewModel.organization.value)
  }

  @Test
  fun eventDetailViewModel_loadEvent_handlesRepositoryErrorWithNullMessage() {
    // Mock event repository throwing an error with null message
    every { mockEventRepository.getEventById("event-123") } returns flow { throw Exception() }

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify default error message is set
    Assert.assertEquals("Failed to load event", viewModel.error.value)

    // Verify loading is complete
    Assert.assertFalse(viewModel.isLoading.value)
  }

  @Test
  fun eventDetailViewModel_loadOrganization_handlesErrorSilently() {
    // Mock successful event loading
    every { mockEventRepository.getEventById("event-123") } returns flowOf(testEvent)

    // Mock organization repository throwing an error
    every { mockOrganizationRepository.getOrganizationById("org-123") } returns
        flow { throw Exception("Organization error") }

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify event is loaded
    Assert.assertEquals(testEvent, viewModel.event.value)

    // Verify organization is null (error was caught)
    Assert.assertNull(viewModel.organization.value)

    // Verify loading is complete
    Assert.assertFalse(viewModel.isLoading.value)

    // Verify no error is set (organization errors are silent)
    Assert.assertNull(viewModel.error.value)
  }

  @Test
  fun eventDetailViewModel_loadOrganization_skipsLoadingWhenOrganizerIdIsBlank() {
    // Create event with blank organizerId
    val eventWithBlankOrganizer = testEvent.copy(organizerId = "")

    // Mock event repository
    every { mockEventRepository.getEventById("event-123") } returns flowOf(eventWithBlankOrganizer)

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify event is loaded
    Assert.assertEquals(eventWithBlankOrganizer, viewModel.event.value)

    // Verify organization is not loaded
    Assert.assertNull(viewModel.organization.value)

    // Verify loading is complete
    Assert.assertFalse(viewModel.isLoading.value)

    // Verify no error
    Assert.assertNull(viewModel.error.value)
  }

  @Test
  fun eventDetailViewModel_loadOrganization_skipsLoadingWhenOrganizerIdIsWhitespace() {
    // Create event with whitespace organizerId
    val eventWithWhitespaceOrganizer = testEvent.copy(organizerId = "   ")

    // Mock event repository
    every { mockEventRepository.getEventById("event-123") } returns
        flowOf(eventWithWhitespaceOrganizer)

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify event is loaded
    Assert.assertEquals(eventWithWhitespaceOrganizer, viewModel.event.value)

    // Verify organization is not loaded
    Assert.assertNull(viewModel.organization.value)

    // Verify loading is complete
    Assert.assertFalse(viewModel.isLoading.value)

    // Verify no error
    Assert.assertNull(viewModel.error.value)
  }

  @Test
  fun eventDetailViewModel_loadDifferentEventIds_loadsCorrectly() {
    val event1 = testEvent.copy(eventId = "event-1", title = "Event 1")
    val event2 = testEvent.copy(eventId = "event-2", title = "Event 2")

    // Mock for event-1
    every { mockEventRepository.getEventById("event-1") } returns flowOf(event1)
    every { mockOrganizationRepository.getOrganizationById(any()) } returns flowOf(testOrganization)

    val viewModel1 =
        EventDetailViewModel(
            eventId = "event-1",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    Assert.assertEquals("Event 1", viewModel1.event.value?.title)

    // Mock for event-2
    every { mockEventRepository.getEventById("event-2") } returns flowOf(event2)

    val viewModel2 =
        EventDetailViewModel(
            eventId = "event-2",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    Assert.assertEquals("Event 2", viewModel2.event.value?.title)
  }

  @Test
  fun eventDetailViewModel_loadOrganization_isTriggeredAfterEventLoads() {
    var organizationLoadCalled = false

    // Mock event repository
    every { mockEventRepository.getEventById("event-123") } returns flowOf(testEvent)

    // Mock organization repository to track when it's called
    every { mockOrganizationRepository.getOrganizationById("org-123") } returns
        flow {
          organizationLoadCalled = true
          emit(testOrganization)
        }

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify organization load was called after event loaded
    Assert.assertTrue(organizationLoadCalled)
    Assert.assertEquals(testOrganization, viewModel.organization.value)
  }

  @Test
  fun eventDetailViewModel_loadDifferentOrganizations_loadsCorrectly() {
    val organization2 =
        Organization(
            id = "org-456",
            name = "Second Organization",
            description = "Second Org Description",
            ownerId = "user-789",
            status = OrganizationStatus.ACTIVE,
        )

    val eventWithOrg2 = testEvent.copy(organizerId = "org-456")

    // Mock event and organization
    every { mockEventRepository.getEventById("event-123") } returns flowOf(eventWithOrg2)
    every { mockOrganizationRepository.getOrganizationById("org-456") } returns
        flowOf(organization2)

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify correct organization is loaded
    Assert.assertEquals(organization2, viewModel.organization.value)
    Assert.assertEquals("Second Organization", viewModel.organization.value?.name)
  }

  @Test
  fun eventDetailViewModel_loadingState_completesAfterDataLoads() {
    every { mockEventRepository.getEventById("event-123") } returns flowOf(testEvent)
    every { mockOrganizationRepository.getOrganizationById("org-123") } returns
        flowOf(testOrganization)

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // With UnconfinedTestDispatcher, coroutines execute immediately,
    // so loading should already be complete by the time we check

    // Loading should be complete
    Assert.assertFalse(viewModel.isLoading.value)
    Assert.assertEquals(testEvent, viewModel.event.value)
    Assert.assertEquals(testOrganization, viewModel.organization.value)
  }

  @Test
  fun eventDetailViewModel_loadFullEvent_loadsAllFieldsCorrectly() {
    val fullEvent =
        Event(
            eventId = "full-event",
            title = "Full Event",
            description = "Complete Description",
            organizerId = "org-123",
            organizerName = "Full Organizer",
            status = EventStatus.PUBLISHED,
            location = Location(GeoPoint(46.5197, 6.6323), "Lausanne", "Vaud"),
            startTime = Timestamp(Date()),
            endTime = Timestamp(Date(System.currentTimeMillis() + 3600000)),
            capacity = 200,
            ticketsRemaining = 100,
            ticketsIssued = 100,
            ticketsRedeemed = 50,
            currency = "CHF",
            pricingTiers = emptyList(),
            images = listOf("image1.jpg", "image2.jpg"),
            tags = listOf("music", "outdoor"),
        )

    every { mockEventRepository.getEventById("full-event") } returns flowOf(fullEvent)
    every { mockOrganizationRepository.getOrganizationById("org-123") } returns
        flowOf(testOrganization)

    val viewModel =
        EventDetailViewModel(
            eventId = "full-event",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    val loadedEvent = viewModel.event.value
    Assert.assertNotNull(loadedEvent)
    Assert.assertEquals("Full Event", loadedEvent?.title)
    Assert.assertEquals("Complete Description", loadedEvent?.description)
    Assert.assertEquals(200, loadedEvent?.capacity)
    Assert.assertEquals(2, loadedEvent?.images?.size)
    Assert.assertEquals(2, loadedEvent?.tags?.size)
  }

  @Test
  fun eventDetailViewModel_stateFlows_areProperlyInitialized() {
    every { mockEventRepository.getEventById("event-123") } returns flowOf(testEvent)
    every { mockOrganizationRepository.getOrganizationById("org-123") } returns
        flowOf(testOrganization)

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify StateFlows can be collected
    Assert.assertNotNull(viewModel.event)
    Assert.assertNotNull(viewModel.organization)
    Assert.assertNotNull(viewModel.isLoading)
    Assert.assertNotNull(viewModel.error)
  }

  @Test
  fun eventDetailViewModel_loadOrganization_handlesNullOrganizationCorrectly() {
    every { mockEventRepository.getEventById("event-123") } returns flowOf(testEvent)
    every { mockOrganizationRepository.getOrganizationById("org-123") } returns flowOf(null)

    val viewModel =
        EventDetailViewModel(
            eventId = "event-123",
            eventRepository = mockEventRepository,
            organizationRepository = mockOrganizationRepository)

    // Verify event is loaded
    Assert.assertEquals(testEvent, viewModel.event.value)

    // Verify organization is null
    Assert.assertNull(viewModel.organization.value)

    // Verify loading is complete
    Assert.assertFalse(viewModel.isLoading.value)

    // Verify no error
    Assert.assertNull(viewModel.error.value)
  }
}
