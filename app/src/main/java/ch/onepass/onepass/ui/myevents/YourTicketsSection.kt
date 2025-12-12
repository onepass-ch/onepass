package ch.onepass.onepass.ui.myevents

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.components.common.EmptyState

/**
 * Section displaying the user's tickets with modern pill-style sub-tabs.
 *
 * @param currentTickets List of current (active) tickets.
 * @param expiredTickets List of expired tickets.
 * @param selectedTab The currently selected sub-tab.
 * @param onTabSelected Callback when a sub-tab is selected.
 * @param userQrData QR code data for the user.
 * @param isQrExpanded Whether the QR code is expanded.
 * @param onToggleQrExpanded Callback to toggle QR code expansion.
 * @param modifier Modifier for styling.
 */
@Composable
fun YourTicketsSection(
    currentTickets: List<Ticket>,
    expiredTickets: List<Ticket>,
    selectedTab: TicketTab,
    onTabSelected: (TicketTab) -> Unit,
    userQrData: String,
    isQrExpanded: Boolean,
    onToggleQrExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
  val tickets =
      when (selectedTab) {
        TicketTab.CURRENT -> currentTickets
        TicketTab.EXPIRED -> expiredTickets
        TicketTab.LISTED -> emptyList() // Listed tickets handled separately
      }

  Column(modifier = modifier.fillMaxWidth()) {
    // Modern pill-style sub-tabs
    ModernPillTabs(
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier =
            Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag(MyEventsTestTags.TABS_ROW))

    // QR Code component (only for Current and Expired tabs)
    if (selectedTab != TicketTab.LISTED) {
      QrCodeComponent(
          qrData = userQrData,
          isExpanded = isQrExpanded,
          onToggleExpanded = onToggleQrExpanded,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
    }

    // List of tickets or empty state
    if (tickets.isEmpty()) {
      val (title, message) =
          when (selectedTab) {
            TicketTab.CURRENT ->
                "No Current Tickets" to
                    "You don't have any active tickets. Browse events to get started!"
            TicketTab.EXPIRED -> "No Expired Tickets" to "You don't have any expired tickets yet."
            TicketTab.LISTED -> "No Listed Tickets" to "You haven't listed any tickets for sale."
          }
      EmptyState(
          title = title,
          message = message,
          modifier = Modifier.padding(top = 32.dp),
          testTag = MyEventsTestTags.EMPTY_STATE)
    } else {
      LazyColumn(
          modifier = Modifier.fillMaxWidth().weight(1f),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items = tickets, key = { it.ticketId }) { ticket ->
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

/**
 * Modern pill-style tabs for Current/Expired/Listed selection.
 *
 * @param selectedTab Currently selected tab.
 * @param onTabSelected Callback when a tab is selected.
 * @param modifier Modifier for styling.
 */
@Composable
private fun ModernPillTabs(
    selectedTab: TicketTab,
    onTabSelected: (TicketTab) -> Unit,
    modifier: Modifier = Modifier
) {
  val tabs =
      listOf(
          TicketTab.CURRENT to "Current",
          TicketTab.EXPIRED to "Expired",
          TicketTab.LISTED to "Listed")

  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    tabs.forEach { (tab, title) ->
      val isSelected = selectedTab == tab

      val backgroundColor by
          animateColorAsState(
              targetValue =
                  if (isSelected) colorResource(id = R.color.accent_purple)
                  else colorResource(id = R.color.surface_container),
              label = "backgroundColor")

      val textColor by
          animateColorAsState(
              targetValue = if (isSelected) Color.White else colorResource(id = R.color.gray),
              label = "textColor")

      val tabTestTag =
          when (tab) {
            TicketTab.CURRENT -> MyEventsTestTags.TAB_CURRENT
            TicketTab.EXPIRED -> MyEventsTestTags.TAB_EXPIRED
            TicketTab.LISTED -> MyEventsTestTags.TAB_LISTED
          }

      Box(
          modifier =
              Modifier.clip(RoundedCornerShape(20.dp))
                  .background(backgroundColor)
                  .clickable { onTabSelected(tab) }
                  .padding(horizontal = 20.dp, vertical = 10.dp)
                  .testTag(tabTestTag),
          contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                color = textColor)
          }
    }
  }
}
