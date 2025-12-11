package ch.onepass.onepass.ui.myevents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.components.common.EmptyState
import ch.onepass.onepass.ui.theme.MarcFontFamily

/**
 * List of tickets available for purchase in the marketplace.
 *
 * @param marketTickets List of market tickets to display.
 * @param onBuyTicket Callback when buy button is clicked for a ticket.
 * @param onSellTicket Callback when sell your ticket button is clicked.
 * @param currentUserId Current user's ID to determine if ticket is their own listing.
 * @param isLoading Whether the market is loading.
 * @param hasSellableTickets Whether the user has tickets they can sell.
 * @param modifier Modifier for styling.
 */
@Composable
fun MarketTicketList(
    marketTickets: List<MarketTicket>,
    onBuyTicket: (String) -> Unit,
    onSellTicket: () -> Unit,
    currentUserId: String?,
    isLoading: Boolean = false,
    hasSellableTickets: Boolean = false,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    // Section header with sell button
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "Available Tickets",
              style =
                  MaterialTheme.typography.titleMedium.copy(
                      fontFamily = MarcFontFamily, fontWeight = FontWeight.Bold),
              color = colorResource(id = R.color.on_background),
              modifier = Modifier.testTag(MyEventsTestTags.MARKET_TICKETS_TITLE))

          // Sell Your Ticket button
          if (hasSellableTickets) {
            Button(
                onClick = onSellTicket,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.accent_purple),
                        contentColor = colorResource(id = R.color.white)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag(MyEventsTestTags.SELL_TICKET_BUTTON)) {
                  Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = "Sell",
                      modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.size(4.dp))
                  Text(
                      text = "Sell Ticket",
                      style =
                          MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
          }
        }

    // Content area
    when {
      isLoading -> {
        // Loading state
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(200.dp)
                    .testTag(MyEventsTestTags.MARKET_LOADING),
            contentAlignment = Alignment.Center) {
              CircularProgressIndicator(
                  color = colorResource(id = R.color.primary), modifier = Modifier.size(40.dp))
            }
      }
      marketTickets.isEmpty() -> {
        // Empty state
        EmptyState(
            title = "No Tickets Available",
            message = "There are no tickets listed for sale at the moment. Check back later or list your own tickets!",
            modifier = Modifier.padding(top = 32.dp),
            testTag = MyEventsTestTags.MARKET_EMPTY_STATE)
      }
      else -> {
        // Ticket list
        LazyColumn(
            modifier = Modifier.fillMaxWidth().testTag(MyEventsTestTags.MARKET_TICKET_LIST),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              items(items = marketTickets, key = { it.ticketId }) { ticket ->
                MarketTicketCard(
                    marketTicket = ticket,
                    onBuyClick = { onBuyTicket(ticket.ticketId) },
                    isCurrentUserSeller = ticket.sellerId == currentUserId,
                    isLoading = isLoading,
                    modifier = Modifier.testTag(MyEventsTestTags.MARKET_TICKET_CARD))
              }
            }
      }
    }
  }
}
