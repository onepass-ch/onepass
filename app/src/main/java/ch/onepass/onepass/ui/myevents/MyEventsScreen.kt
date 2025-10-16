package ch.onepass.onepass.ui.myevents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.MarcFontFamily
import ch.onepass.onepass.ui.theme.OnePassTheme

/** Test tags for MyEvents screen components */
object MyEventsTestTags {
  const val TABS_ROW = "TabsRow"
  const val TAB_CURRENT = "TabCurrent"
  const val TAB_EXPIRED = "TabExpired"
  const val QR_CODE_ICON = "QrCodeIcon"
  const val QR_CODE_DIALOG = "QrCodeDialog"
  const val TICKET_CARD = "TicketCard"
  const val TICKET_TITLE = "TicketTitle"
  const val TICKET_STATUS = "TicketStatus"
  const val TICKET_DATE = "TicketDate"
  const val TICKET_LOCATION = "TicketLocation"
  const val TICKET_DIALOG_TITLE = "TicketDialogTitle"
  const val TICKET_DIALOG_STATUS = "TicketDialogStatus"
  const val TICKET_DIALOG_DATE = "TicketDialogDate"
  const val TICKET_DIALOG_LOCATION = "TicketDialogLocation"
}

/**
 * Composable screen displaying user's events with tabs for current and expired tickets.
 *
 * @param viewModel ViewModel providing ticket data
 * @param userQrData String data to be encoded in the user's QR code
 */
@Composable
fun MyEventsScreen(viewModel: MyEventsViewModel, userQrData: String) {
  val currentTickets by viewModel.currentTickets.collectAsState()
  val expiredTickets by viewModel.expiredTickets.collectAsState()

  MyEventsContent(
      userQrData = userQrData, currentTickets = currentTickets, expiredTickets = expiredTickets)
}

/**
 * Composable screen displaying user's events with tabs for current and expired tickets.
 *
 * @param userQrData String data to be encoded in the user's QR code
 * @param currentTickets List of current tickets to display
 * @param expiredTickets List of expired tickets to display
 */
@Composable
fun MyEventsContent(
    userQrData: String,
    currentTickets: List<Ticket>,
    expiredTickets: List<Ticket>
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Current", "Expired")
  val tickets = if (selectedTab == 0) currentTickets else expiredTickets

  Surface(modifier = Modifier.fillMaxSize(), color = colorResource(id = R.color.background)) {
    Column(modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.background))) {
      Text(
          text = "YOUR TICKETS",
          modifier = Modifier.padding(16.dp),
          style = MaterialTheme.typography.titleLarge.copy(fontFamily = MarcFontFamily),
          color = colorResource(id = R.color.on_background))

      TabRow(
          modifier = Modifier.testTag(MyEventsTestTags.TABS_ROW),
          selectedTabIndex = selectedTab,
          containerColor = colorResource(id = R.color.surface),
          contentColor = colorResource(id = R.color.on_surface),
          indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                height = 4.dp,
                color = colorResource(id = R.color.tab_indicator))
          }) {
            tabs.forEachIndexed { index, title ->
              val tabTestTag =
                  when (title) {
                    "Current" -> MyEventsTestTags.TAB_CURRENT
                    "Expired" -> MyEventsTestTags.TAB_EXPIRED
                    else -> null
                  }

              Tab(
                  text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                            if (selectedTab == index) colorResource(id = R.color.tab_selected)
                            else colorResource(id = R.color.tab_unselected).copy(alpha = 0.6f))
                  },
                  selected = selectedTab == index,
                  onClick = { selectedTab = index },
                  modifier = if (tabTestTag != null) Modifier.testTag(tabTestTag) else Modifier)
            }
          }
      QrCodeComponent(
          modifier = Modifier.fillMaxWidth().height(150.dp).padding(16.dp), qrData = userQrData)

      LazyColumn(
          modifier = Modifier.fillMaxWidth().weight(1f),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)) {
            tickets.forEach { ticket ->
              item {
                TicketComponent(
                    title = ticket.title,
                    status = ticket.status,
                    dateTime = ticket.dateTime,
                    location = ticket.location)
              }
            }
          }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MyEventsContentPreview() {
  OnePassTheme {
    val currentTickets =
        listOf(
            Ticket(
                "Lausanne Party",
                TicketStatus.CURRENTLY,
                "Dec 15, 2024 • 9:00 PM",
                "Lausanne, Flon"))

    val expiredTickets =
        listOf(Ticket("Morges Party", TicketStatus.EXPIRED, "Nov 10, 2024 • 8:00 PM", "Morges"))

    MyEventsContent(
        "USER-QR-DATA", currentTickets = currentTickets, expiredTickets = expiredTickets)
  }
}
