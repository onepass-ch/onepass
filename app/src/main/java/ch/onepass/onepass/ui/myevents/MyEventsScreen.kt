package ch.onepass.onepass.ui.myevents

import androidx.annotation.ColorRes
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

data class Ticket(
    val title: String,
    val status: TicketStatus,
    val dateTime: String,
    val location: String,
)

enum class TicketStatus(@ColorRes val colorRes: Int) {
  CURRENTLY(R.color.status_currently),
  UPCOMING(R.color.status_upcoming),
  EXPIRED(R.color.status_expired)
}

@Composable
fun MyEventsScreen(userQrData: String, currentTickets: List<Ticket>, expiredTickets: List<Ticket>) {
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
              )
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
fun MyEventsScreenPreview() {
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

    MyEventsScreen("USER-QR-DATA", currentTickets = currentTickets, expiredTickets = expiredTickets)
  }
}
