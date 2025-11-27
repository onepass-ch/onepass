package ch.onepass.onepass.ui.myevents

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.ticket.TicketRepository
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
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

    composeTestRule.onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_STATUS).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_DATE)
        .assertTextEquals("December 16, 2024 • 12:40 AM")

    composeTestRule
        .onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_LOCATION)
        .assertTextEquals("Lausanne, Flon")
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

class FakeTicketRepository : TicketRepository {
  override fun getActiveTickets(
      userId: String
  ): Flow<List<ch.onepass.onepass.model.ticket.Ticket>> =
      flowOf(
          listOf(
              ch.onepass.onepass.model.ticket.Ticket(
                  ticketId = "t1",
                  eventId = "e1",
                  ownerId = userId,
                  state = ch.onepass.onepass.model.ticket.TicketState.ISSUED,
                  tierId = "tier1",
                  purchasePrice = 50.0,
                  issuedAt = Timestamp.now(),
                  expiresAt = null)))

  override fun getExpiredTickets(
      userId: String
  ): Flow<List<ch.onepass.onepass.model.ticket.Ticket>> =
      flowOf(
          listOf(
              ch.onepass.onepass.model.ticket.Ticket(
                  ticketId = "t2",
                  eventId = "e2",
                  ownerId = userId,
                  state = ch.onepass.onepass.model.ticket.TicketState.REDEEMED,
                  tierId = "tier2",
                  purchasePrice = 40.0,
                  issuedAt = Timestamp.now(),
                  expiresAt = Timestamp.now())))

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
}

private fun createFakeMyEventsViewModel(): MyEventsViewModel {
  return MyEventsViewModel(
      ticketRepo = FakeTicketRepository(), eventRepo = FakeEventRepository(), userId = "TEST_USER")
}

class FakeEventRepository : EventRepository {

  private val events =
      listOf(
          ch.onepass.onepass.model.event.Event(
              eventId = "e1",
              title = "Lausanne Party",
              location = Location(name = "Lausanne, Flon"),
              startTime = Timestamp(1734306000, 0)),
          ch.onepass.onepass.model.event.Event(
              eventId = "e2",
              title = "Morges Party",
              location = Location(name = "Morges"),
              startTime = Timestamp(1734306000 + 86400, 0)))

  override fun getEventById(eventId: String): Flow<ch.onepass.onepass.model.event.Event> =
      flowOf(events.find { it.eventId == eventId }!!)

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
}
