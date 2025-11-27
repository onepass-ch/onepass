package ch.onepass.onepass.ui.myevents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.MarcFontFamily

/** Test tags for MyEvents screen components */
object MyEventsTestTags {
  const val TABS_ROW = "TabsRow"
  const val TAB_CURRENT = "TabCurrent"
  const val TAB_EXPIRED = "TabExpired"
  const val QR_CODE_ICON = "QrCodeIcon"
  const val QR_CODE_CARD = "QrCodeCard"
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
  MyEventsContent(userQrData = userQrData, viewModel = viewModel)
}

/**
 * Composable screen displaying user's events with tabs for current and expired tickets.
 *
 * @param viewModel ViewModel providing ticket data
 * @param userQrData String data to be encoded in the user's QR code
 */
@Composable
fun MyEventsContent(userQrData: String, viewModel: MyEventsViewModel = viewModel()) {
  // Collect UI state from ViewModel
  val uiState by viewModel.uiState.collectAsState()

  // Tickets to display based on selected tab
  val tickets =
      remember(uiState.selectedTab) {
        when (uiState.selectedTab) {
          TicketTab.CURRENT -> uiState.currentTickets
          TicketTab.EXPIRED -> uiState.expiredTickets
        }
      }

  // Define tabs with their titles
  val tabs = listOf(TicketTab.CURRENT to "Current", TicketTab.EXPIRED to "Expired")

  Surface(modifier = Modifier.fillMaxSize(), color = colorResource(id = R.color.background)) {
    Column(modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.background))) {
      // Screen title
      Text(
          text = "YOUR TICKETS",
          modifier = Modifier.padding(16.dp),
          style = MaterialTheme.typography.titleLarge.copy(fontFamily = MarcFontFamily),
          color = colorResource(id = R.color.on_background))

      // Tabs for switching between Current and Expired tickets
      TabRow(
          modifier = Modifier.testTag(MyEventsTestTags.TABS_ROW),
          selectedTabIndex = uiState.selectedTab.ordinal,
          containerColor = colorResource(id = R.color.surface),
          contentColor = colorResource(id = R.color.on_surface),
          indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                height = 4.dp,
                color = colorResource(id = R.color.tab_indicator))
          }) {
            tabs.forEach { (tab, title) ->
              val tabTestTag =
                  when (tab) {
                    TicketTab.CURRENT -> MyEventsTestTags.TAB_CURRENT
                    TicketTab.EXPIRED -> MyEventsTestTags.TAB_EXPIRED
                  }

              Tab(
                  text = {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                            if (uiState.selectedTab == tab) colorResource(id = R.color.tab_selected)
                            else colorResource(id = R.color.tab_unselected).copy(alpha = 0.6f))
                  },
                  selected = uiState.selectedTab == tab,
                  onClick = { viewModel.selectTab(tab) },
                  modifier = Modifier.testTag(tabTestTag))
            }
          }

      // QR Code component
      QrCodeComponent(
          qrData = userQrData,
          isExpanded = uiState.isQrExpanded,
          onToggleExpanded = { viewModel.toggleQrExpansion() },
          modifier = Modifier.fillMaxWidth().padding(16.dp))

      // List of tickets
      LazyColumn(
          modifier = Modifier.fillMaxWidth().weight(1f),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(items = tickets, key = { it.title + it.dateTime }) { ticket ->
              TicketComponent(
                  title = ticket.title,
                  status = ticket.status,
                  dateTime = ticket.dateTime,
                  location = ticket.location,
                  modifier = Modifier.testTag(MyEventsTestTags.TICKET_CARD))
            }
          }
    }
  }
}
