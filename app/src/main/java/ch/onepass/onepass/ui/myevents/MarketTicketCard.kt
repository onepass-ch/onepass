package ch.onepass.onepass.ui.myevents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.MarcFontFamily
import ch.onepass.onepass.utils.FormatUtils.formatPriceCompact
import coil.compose.AsyncImage

/**
 * Card displaying a ticket available for purchase in the marketplace.
 *
 * Shows event details, seller price vs original price, and a buy button.
 *
 * @param marketTicket The market ticket to display.
 * @param onBuyClick Callback when the buy button is clicked.
 * @param isCurrentUserSeller Whether the current user is the seller.
 * @param isLoading Whether a purchase is in progress.
 * @param modifier Modifier for styling.
 */
@Composable
fun MarketTicketCard(
    marketTicket: MarketTicket,
    onBuyClick: () -> Unit,
    isCurrentUserSeller: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
  Card(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.cardColors(containerColor = colorResource(id = R.color.surface_card_color)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              // Event image thumbnail
              Box(
                  modifier =
                      Modifier.size(100.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .background(colorResource(id = R.color.surface_container))) {
                    if (marketTicket.eventImageUrl.isNotEmpty()) {
                      AsyncImage(
                          model = marketTicket.eventImageUrl,
                          contentDescription = "Event image",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop,
                          placeholder = painterResource(id = R.drawable.image_fallback),
                          error = painterResource(id = R.drawable.image_fallback))
                    } else {
                      Image(
                          painter = painterResource(id = R.drawable.image_fallback),
                          contentDescription = "Default event image",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop)
                    }
                  }

              // Ticket info
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Event title
                Text(
                    text = marketTicket.eventTitle,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = MarcFontFamily, fontWeight = FontWeight.Bold),
                    color = colorResource(id = R.color.on_background),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(MyEventsTestTags.MARKET_TICKET_TITLE))

                // Event date
                Text(
                    text = marketTicket.eventDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorResource(id = R.color.gray),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(MyEventsTestTags.MARKET_TICKET_DATE))

                // Event location
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = "Location",
                      tint = colorResource(id = R.color.gray),
                      modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                      text = marketTicket.eventLocation,
                      style = MaterialTheme.typography.bodySmall,
                      color = colorResource(id = R.color.gray),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.testTag(MyEventsTestTags.MARKET_TICKET_LOCATION))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Price section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      // Price comparison
                      Column {
                        // Seller price (highlighted)
                        Text(
                            text = "${marketTicket.currency} ${formatPriceCompact(marketTicket.sellerPrice)}",
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold),
                            color = colorResource(id = R.color.tab_selected),
                            modifier = Modifier.testTag(MyEventsTestTags.MARKET_TICKET_SELLER_PRICE))

                        // Original price (strikethrough if different)
                        if (marketTicket.originalPrice != marketTicket.sellerPrice) {
                          Text(
                              text =
                                  "Original: ${marketTicket.currency} ${formatPriceCompact(marketTicket.originalPrice)}",
                              style =
                                  MaterialTheme.typography.bodySmall.copy(
                                      textDecoration = TextDecoration.LineThrough),
                              color = colorResource(id = R.color.gray),
                              modifier =
                                  Modifier.testTag(MyEventsTestTags.MARKET_TICKET_ORIGINAL_PRICE))
                        }
                      }

                      // Buy button
                      if (!isCurrentUserSeller) {
                        Button(
                            onClick = onBuyClick,
                            enabled = !isLoading,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = colorResource(id = R.color.event_buy_button_bg),
                                    contentColor = colorResource(id = R.color.white)),
                            shape = RoundedCornerShape(8.dp),
                            modifier =
                                Modifier.height(36.dp)
                                    .testTag(MyEventsTestTags.MARKET_TICKET_BUY_BUTTON)) {
                              Text(
                                  text = if (isLoading) "..." else "Buy",
                                  style =
                                      MaterialTheme.typography.bodyMedium.copy(
                                          fontWeight = FontWeight.SemiBold))
                            }
                      } else {
                        // Show "Your listing" label for seller's own tickets
                        Box(
                            modifier =
                                Modifier.background(
                                        colorResource(id = R.color.accent_purple).copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                              Text(
                                  text = "Your listing",
                                  style = MaterialTheme.typography.bodySmall,
                                  color = colorResource(id = R.color.accent_purple))
                            }
                      }
                    }
              }
            }
      }
}
