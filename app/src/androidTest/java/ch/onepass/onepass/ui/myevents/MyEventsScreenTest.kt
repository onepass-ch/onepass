package ch.onepass.onepass.ui.myevents

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.pass.Pass
import ch.onepass.onepass.model.pass.PassRepository
import ch.onepass.onepass.model.ticket.TicketRepository
import ch.onepass.onepass.model.ticket.TicketState
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class MyEventsScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun setContent() {
    val fakeVm = createFakeMyEventsViewModel()
    composeTestRule.setContent {
      OnePassTheme { MyEventsContent(userQrData = "USER-QR-DATA", viewModel = fakeVm) }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun tabs_exist_and_switchingTabs_showsCorrectTickets() {
    setContent()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TABS_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_CURRENT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).assertIsDisplayed()
    composeTestRule.onNodeWithText("Lausanne Party").assertIsDisplayed()
    composeTestRule.onNodeWithText("Morges Party").assertDoesNotExist()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Morges Party").assertIsDisplayed()
    composeTestRule.onNodeWithText("Lausanne Party").assertDoesNotExist()
  }

  @Test
  fun clickingTicket_showsDetailsDialog() {
    setContent()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TICKET_CARD).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_TITLE)
        .assertTextEquals("Lausanne Party")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_STATUS).assertIsDisplayed()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_DATE)
        .assertTextEquals("December 16, 2024 • 12:40 AM")
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_LOCATION)
        .assertTextEquals("Lausanne, Flon")
    composeTestRule.waitForIdle()
  }

  @Test
  fun qrCodeCard_expandsOnClick() {
    setContent()
    val qrCard = composeTestRule.onNodeWithTag(MyEventsTestTags.QR_CODE_CARD)
    val initialHeight = qrCard.fetchSemanticsNode().boundsInRoot.height
    qrCard.performClick()
    composeTestRule.waitForIdle()
    val expandedHeight = qrCard.fetchSemanticsNode().boundsInRoot.height
    assert(expandedHeight > initialHeight) {
      "QR card did not expand (initial=$initialHeight, expanded=$expandedHeight)"
    }
  }
}

fun createTestTicket(ticketId: String, eventId: String, state: TicketState, userId: String) =
    ch.onepass.onepass.model.ticket.Ticket(
        ticketId = ticketId,
        eventId = eventId,
        ownerId = userId,
        state = state,
        tierId = "tier1",
        purchasePrice = 50.0,
        issuedAt = fixedTestTimestamp(),
        expiresAt = if (state == TicketState.REDEEMED) fixedTestTimestamp() else null)

fun createTestEvent(eventId: String, title: String, location: String, startTime: Timestamp) =
    ch.onepass.onepass.model.event.Event(
        eventId = eventId,
        title = title,
        location = Location(name = location),
        startTime = startTime)

fun fixedTestTimestamp(): Timestamp {
  val calendar = Calendar.getInstance()
  calendar.set(2024, Calendar.DECEMBER, 16, 0, 40, 0)
  calendar.set(Calendar.MILLISECOND, 0)
  return Timestamp(calendar.time)
}

class FakeTicketRepository : TicketRepository {
  override fun getActiveTickets(
      userId: String
  ): Flow<List<ch.onepass.onepass.model.ticket.Ticket>> =
      flowOf(listOf(createTestTicket("t1", "e1", TicketState.ISSUED, userId)))

  override fun getExpiredTickets(
      userId: String
  ): Flow<List<ch.onepass.onepass.model.ticket.Ticket>> =
      flowOf(listOf(createTestTicket("t2", "e2", TicketState.REDEEMED, userId)))

  override fun getTicketsByUser(
      userId: String
  ): Flow<List<ch.onepass.onepass.model.ticket.Ticket>> = flowOf(emptyList())

  override fun getTicketById(ticketId: String): Flow<ch.onepass.onepass.model.ticket.Ticket?> =
      flowOf(null)

  override suspend fun createTicket(ticket: ch.onepass.onepass.model.ticket.Ticket) =
      Result.success(ticket.ticketId)

  override suspend fun updateTicket(ticket: ch.onepass.onepass.model.ticket.Ticket) =
      Result.success(Unit)

  override suspend fun deleteTicket(ticketId: String) = Result.success(Unit)

  override suspend fun cancelTicketListing(ticketId: String) = Result.success(Unit)

  override fun getListedTickets(): Flow<List<ch.onepass.onepass.model.ticket.Ticket>> =
      flowOf(emptyList())

  override fun getListedTicketsByEvent(
      eventId: String
  ): Flow<List<ch.onepass.onepass.model.ticket.Ticket>> = flowOf(emptyList())

  override fun getListedTicketsByUser(
      userId: String
  ): Flow<List<ch.onepass.onepass.model.ticket.Ticket>> = flowOf(emptyList())

  override suspend fun listTicketForSale(ticketId: String, askingPrice: Double) =
      Result.success(Unit)

  override suspend fun purchaseListedTicket(ticketId: String, buyerId: String) =
      Result.success(Unit)
}

class FakeEventRepository : EventRepository {
  private val events =
      listOf(
          createTestEvent("e1", "Lausanne Party", "Lausanne, Flon", fixedTestTimestamp()),
          createTestEvent(
              "e2", "Morges Party", "Morges", Timestamp(fixedTestTimestamp().seconds + 86400, 0)))

  override fun getEventById(eventId: String): Flow<ch.onepass.onepass.model.event.Event> =
      flowOf(events.first { it.eventId == eventId })

  override fun getAllEvents(): Flow<List<ch.onepass.onepass.model.event.Event>> = flowOf(events)

  override fun searchEvents(query: String): Flow<List<ch.onepass.onepass.model.event.Event>> =
      flowOf(events.filter { it.title.contains(query, ignoreCase = true) })

  override fun getEventsByOrganization(
      orgId: String
  ): Flow<List<ch.onepass.onepass.model.event.Event>> = flowOf(events)

  override fun getEventsByLocation(
      center: Location,
      radiusKm: Double
  ): Flow<List<ch.onepass.onepass.model.event.Event>> = flowOf(events)

  override fun getEventsByTag(tag: String): Flow<List<ch.onepass.onepass.model.event.Event>> =
      flowOf(events)

  override fun getFeaturedEvents(): Flow<List<ch.onepass.onepass.model.event.Event>> =
      flowOf(events)

  override fun getEventsByStatus(
      status: ch.onepass.onepass.model.event.EventStatus
  ): Flow<List<ch.onepass.onepass.model.event.Event>> = flowOf(events)

  override suspend fun createEvent(event: ch.onepass.onepass.model.event.Event) =
      Result.success(event.eventId)

  override suspend fun updateEvent(event: ch.onepass.onepass.model.event.Event) =
      Result.success(Unit)

  override suspend fun deleteEvent(eventId: String) = Result.success(Unit)

  override suspend fun addEventImage(eventId: String, imageUrl: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun removeEventImage(eventId: String, imageUrl: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun updateEventImages(eventId: String, imageUrls: List<String>): Result<Unit> =
      Result.success(Unit)
}

private fun createFakeMyEventsViewModel(): MyEventsViewModel {
  // Mock DataStore
  val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
  // Mock PassRepository
  val mockPassRepository = mockk<PassRepository>(relaxed = true)
  val mockPass =
      Pass(
          uid = "TEST_USER",
          kid = "test-key",
          issuedAt = System.currentTimeMillis() / 1000,
          version = 1,
          signature = "test-signature")
  coEvery { mockPassRepository.getOrCreateSignedPass(any()) } returns Result.success(mockPass)

  return MyEventsViewModel(
      dataStore = mockDataStore,
      passRepository = mockPassRepository,
      ticketRepo = FakeTicketRepository(),
      eventRepo = FakeEventRepository(),
      userId = "TEST_USER")
}
