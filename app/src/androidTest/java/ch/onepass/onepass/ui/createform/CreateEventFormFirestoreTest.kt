package ch.onepass.onepass.ui.createform

import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import junit.framework.TestCase.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test

/**
 * Firestore integration tests for CreateEventForm functionality.
 *
 * These tests verify that events created through the CreateEventFormViewModel are correctly stored
 * in Firestore and can be retrieved.
 *
 * Prerequisites:
 * - Firebase emulators must be running (firebase emulators:start)
 * - Test uses anonymous authentication
 */
class CreateEventFormFirestoreTest : FirestoreTestBase() {

  private lateinit var viewModel: CreateEventFormViewModel
  private lateinit var userId: String

  @Before
  override fun setUp() {
    super.setUp()
    runBlocking {
      // Sign in anonymously for testing
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"

      // Create ViewModel with real repository
      viewModel = CreateEventFormViewModel(repository)
    }
  }

  @Test
  fun createEvent_successfullyStoresInFirestore() = runBlocking {
    // Fill in form with valid data
    viewModel.updateTitle("Integration Test Event")
    viewModel.updateDescription("This is a test event created through integration test")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("EPFL Campus")
    viewModel.updatePrice("30.00")
    viewModel.updateCapacity("100")

    // Create event
    viewModel.createEvent(userId, "Test Organizer")

    // Wait for async operation
    delay(2000)

    // Verify event was stored in Firestore
    val events = getAllEventsFromFirestore()
    assertEquals("Should have 1 event in Firestore", 1, events.size)

    val storedEvent = events.first()
    assertEquals("Integration Test Event", storedEvent.title)
    assertEquals("This is a test event created through integration test", storedEvent.description)
    assertEquals(userId, storedEvent.organizerId)
    assertEquals("Test Organizer", storedEvent.organizerName)
    assertEquals(100, storedEvent.capacity)
    assertEquals(100, storedEvent.ticketsRemaining)
    assertEquals(EventStatus.DRAFT, storedEvent.status)
  }

  @Test
  fun createEvent_generatesUniqueEventId() = runBlocking {
    // Create first event
    viewModel.updateTitle("Event 1")
    viewModel.updateDescription("Description 1")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("Location 1")
    viewModel.updatePrice("20")
    viewModel.updateCapacity("50")

    viewModel.createEvent(userId, "Organizer")
    delay(2000)

    // Reset and create second event
    viewModel.resetForm()

    viewModel.updateTitle("Event 2")
    viewModel.updateDescription("Description 2")
    viewModel.updateDate("26/12/2025")
    viewModel.updateStartTime("15:30")
    viewModel.updateEndTime("17:30")
    viewModel.updateLocation("Location 2")
    viewModel.updatePrice("25")
    viewModel.updateCapacity("75")

    viewModel.createEvent(userId, "Organizer")
    delay(2000)

    // Verify both events exist with different IDs
    val events = getAllEventsFromFirestore()
    assertEquals("Should have 2 events", 2, events.size)

    val eventIds = events.map { it.eventId }.toSet()
    assertEquals("Event IDs should be unique", 2, eventIds.size)

    val titles = events.map { it.title }.toSet()
    assertTrue("Should contain Event 1", titles.contains("Event 1"))
    assertTrue("Should contain Event 2", titles.contains("Event 2"))
  }

  @Test
  fun createEvent_storesPricingTierCorrectly() = runBlocking {
    viewModel.updateTitle("Pricing Test Event")
    viewModel.updateDescription("Testing pricing tier storage")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("Test Location")
    viewModel.updatePrice("45.75")
    viewModel.updateCapacity("200")

    viewModel.createEvent(userId, "Test Organizer")
    delay(2000)

    val events = getAllEventsFromFirestore()
    val storedEvent = events.first()

    // Verify pricing tier
    assertNotNull("Pricing tiers should not be null", storedEvent.pricingTiers)
    assertEquals("Should have 1 pricing tier", 1, storedEvent.pricingTiers.size)

    val pricingTier = storedEvent.pricingTiers.first()
    assertEquals("General", pricingTier.name)
    assertEquals(45.75, pricingTier.price, 0.01)
    assertEquals(200, pricingTier.quantity)
    assertEquals(200, pricingTier.remaining)
  }

  @Test
  fun createEvent_storesTimestampsCorrectly() = runBlocking {
    viewModel.updateTitle("Timestamp Test Event")
    viewModel.updateDescription("Testing timestamp storage")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("Test Location")
    viewModel.updatePrice("20")
    viewModel.updateCapacity("100")

    viewModel.createEvent(userId, "Test Organizer")

    // Wait for event creation
    delay(2000)

    val events = getAllEventsFromFirestore()
    val storedEvent = events.first()

    // Verify timestamps exist
    assertNotNull("Start time should not be null", storedEvent.startTime)
    assertNotNull("End time should not be null", storedEvent.endTime)

    // Verify end time is after start time
    assertTrue(
        "End time should be after start time",
        storedEvent.endTime!!.seconds > storedEvent.startTime!!.seconds)
  }

  @Test
  fun createEvent_canBeRetrievedById() = runBlocking {
    viewModel.updateTitle("Retrieval Test Event")
    viewModel.updateDescription("Testing event retrieval by ID")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("Test Location")
    viewModel.updatePrice("30")
    viewModel.updateCapacity("150")

    viewModel.createEvent(userId, "Test Organizer")

    // Wait for event creation
    delay(2000)

    // Get all events and verify one was created
    val events = getAllEventsFromFirestore()
    assertEquals("Should have 1 event", 1, events.size)

    val storedEvent = events.first()
    val eventId = storedEvent.eventId

    // Retrieve event by ID from repository
    val retrievedEvent = repository.getEventById(eventId).first()

    assertNotNull("Event should be retrievable by ID", retrievedEvent)
    assertEquals("Retrieval Test Event", retrievedEvent?.title)
    assertEquals("Testing event retrieval by ID", retrievedEvent?.description)
    assertEquals(eventId, retrievedEvent?.eventId)
  }

  @Test
  fun createEvent_setsDefaultCurrency() = runBlocking {
    viewModel.updateTitle("Currency Test Event")
    viewModel.updateDescription("Testing default currency")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("Test Location")
    viewModel.updatePrice("25")
    viewModel.updateCapacity("100")

    viewModel.createEvent(userId, "Test Organizer")

    // Wait for event creation
    delay(2000)

    val events = getAllEventsFromFirestore()
    val storedEvent = events.first()

    assertEquals("Currency should be CHF", "CHF", storedEvent.currency)
  }

  @Test
  fun createEvent_initializesTicketCounters() = runBlocking {
    viewModel.updateTitle("Ticket Counter Test Event")
    viewModel.updateDescription("Testing ticket counter initialization")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("Test Location")
    viewModel.updatePrice("20")
    viewModel.updateCapacity("120")

    viewModel.createEvent(userId, "Test Organizer")

    // Wait for event creation
    delay(2000)

    val events = getAllEventsFromFirestore()
    val storedEvent = events.first()

    assertEquals("Capacity should be 120", 120, storedEvent.capacity)
    assertEquals("Tickets remaining should equal capacity", 120, storedEvent.ticketsRemaining)
    assertEquals("Tickets issued should be 0", 0, storedEvent.ticketsIssued)
    assertEquals("Tickets redeemed should be 0", 0, storedEvent.ticketsRedeemed)
  }

  @Test
  fun createMultipleEvents_allStoredCorrectly() = runBlocking {
    // Create first event
    viewModel.updateTitle("Multi Event 1")
    viewModel.updateDescription("First event")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:00")
    viewModel.updateEndTime("16:00")
    viewModel.updateLocation("Location 1")
    viewModel.updatePrice("20")
    viewModel.updateCapacity("50")

    viewModel.createEvent(userId, "Organizer 1")
    delay(2000)

    viewModel.resetForm()

    // Create second event
    viewModel.updateTitle("Multi Event 2")
    viewModel.updateDescription("Second event")
    viewModel.updateDate("26/12/2025")
    viewModel.updateStartTime("18:00")
    viewModel.updateEndTime("20:00")
    viewModel.updateLocation("Location 2")
    viewModel.updatePrice("30")
    viewModel.updateCapacity("75")

    viewModel.createEvent(userId, "Organizer 2")
    delay(2000)

    viewModel.resetForm()

    // Create third event
    viewModel.updateTitle("Multi Event 3")
    viewModel.updateDescription("Third event")
    viewModel.updateDate("27/12/2025")
    viewModel.updateStartTime("10:00")
    viewModel.updateEndTime("12:00")
    viewModel.updateLocation("Location 3")
    viewModel.updatePrice("15")
    viewModel.updateCapacity("100")

    viewModel.createEvent(userId, "Organizer 3")
    delay(2000)

    // Verify all events are stored
    val events = getAllEventsFromFirestore()
    assertEquals("Should have 3 events", 3, events.size)

    val titles = events.map { it.title }.sorted()
    assertEquals(listOf("Multi Event 1", "Multi Event 2", "Multi Event 3"), titles)
  }

  @Test
  fun createEvent_withFreePrice_storesZeroPrice() = runBlocking {
    viewModel.updateTitle("Free Event")
    viewModel.updateDescription("This is a free event")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("Community Center")
    viewModel.updatePrice("0")
    viewModel.updateCapacity("200")

    viewModel.createEvent(userId, "Community Organizer")

    // Wait for event creation
    delay(2000)

    val events = getAllEventsFromFirestore()
    val storedEvent = events.first()

    assertEquals("Free Event", storedEvent.title)
    assertEquals(0.0, storedEvent.pricingTiers.first().price, 0.01)
  }

  @Test
  fun createEvent_resetsFormAfterSuccess() = runBlocking {
    // Fill and submit form
    viewModel.updateTitle("Reset Test Event")
    viewModel.updateDescription("Testing form reset")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("Test Location")
    viewModel.updatePrice("25")
    viewModel.updateCapacity("100")

    viewModel.createEvent(userId, "Test Organizer")

    // Wait for event creation (Success state is transient, form gets reset immediately)
    delay(2000)

    // Give a little more time for the state to settle after reset
    delay(100)

    // Verify form was reset
    val formState = viewModel.formState.value
    assertEquals("Title should be empty", "", formState.title)
    assertEquals("Description should be empty", "", formState.description)
    assertEquals("Date should be empty", "", formState.date)
    assertEquals("Start time should be empty", "", formState.startTime)
    assertEquals("End time should be empty", "", formState.endTime)
    assertEquals("Location should be empty", "", formState.location)
    assertEquals("Price should be empty", "", formState.price)
    assertEquals("Capacity should be empty", "", formState.capacity)
  }

  @Test
  fun createEvent_queryByOrganizerId() = runBlocking {
    viewModel.updateTitle("Query Test Event")
    viewModel.updateDescription("Testing query by organizer")
    viewModel.updateDate("25/12/2025")
    viewModel.updateStartTime("14:30")
    viewModel.updateEndTime("16:30")
    viewModel.updateLocation("Test Location")
    viewModel.updatePrice("20")
    viewModel.updateCapacity("100")

    viewModel.createEvent(userId, "Test Organizer")

    // Wait for event creation
    delay(2000)

    // Query events by organizer ID
    val events = repository.getEventsByOrganization(userId).first()

    assertEquals("Should find 1 event for this organizer", 1, events.size)
    assertEquals("Query Test Event", events.first().title)
    assertEquals(userId, events.first().organizerId)
  }
}
