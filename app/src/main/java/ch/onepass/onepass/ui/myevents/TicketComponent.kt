package ch.onepass.onepass.ui.myevents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.MarcFontFamily
import ch.onepass.onepass.ui.theme.OnePassTheme

/**
 * Component that displays a ticket with title, status, date/time, and location.
 *
 * @param title Title of the event
 * @param status Status of the ticket (e.g., CURRENTLY, UPCOMING, EXPIRED)
 * @param dateTime Date and time of the event
 * @param location Location of the event
 * @param modifier Modifier for styling
 */
@Composable
fun TicketComponent(
    title: String,
    status: TicketStatus,
    dateTime: String,
    location: String,
    modifier: Modifier = Modifier
) {
  var showDetails by remember { mutableStateOf(false) }

  if (showDetails) {
    Dialog(onDismissRequest = { showDetails = false }) {
      Card(
          shape = RoundedCornerShape(16.dp),
          colors =
              CardDefaults.cardColors(
                  containerColor = colorResource(id = R.color.surface_container)),
          modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  Text(
                      text = title,
                      modifier = Modifier.testTag(MyEventsTestTags.TICKET_DIALOG_TITLE),
                      style =
                          MaterialTheme.typography.bodyLarge.copy(
                              fontFamily = MarcFontFamily, fontWeight = FontWeight.Bold),
                      color = colorResource(id = R.color.on_background))

                  Row(
                      modifier = Modifier.testTag(MyEventsTestTags.TICKET_DIALOG_STATUS),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Start) {
                        Box(
                            modifier =
                                Modifier.size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colorResource(id = status.colorRes)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorResource(id = R.color.on_background))
                      }

                  Text(
                      text = dateTime,
                      modifier = Modifier.testTag(MyEventsTestTags.TICKET_DIALOG_DATE),
                      style = MaterialTheme.typography.bodyMedium,
                      color = colorResource(id = R.color.on_background).copy(alpha = 0.7f))

                  Text(
                      text = location,
                      modifier = Modifier.testTag(MyEventsTestTags.TICKET_DIALOG_LOCATION),
                      style = MaterialTheme.typography.bodyMedium,
                      color = colorResource(id = R.color.on_background).copy(alpha = 0.7f))
                }
          }
    }
  }

  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable { showDetails = true }
              .testTag(MyEventsTestTags.TICKET_CARD),
      shape = RoundedCornerShape(12.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = colorResource(id = R.color.surface_card_color))) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                  text = title,
                  style =
                      MaterialTheme.typography.bodyLarge.copy(
                          fontFamily = MarcFontFamily, fontWeight = FontWeight.Bold),
                  color = colorResource(id = R.color.on_background),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag(MyEventsTestTags.TICKET_TITLE))

              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Start,
                  modifier = Modifier.testTag(MyEventsTestTags.TICKET_STATUS)) {
                    Box(
                        modifier =
                            Modifier.size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colorResource(id = status.colorRes)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(id = R.color.on_background))
                  }

              Text(
                  text = dateTime,
                  style = MaterialTheme.typography.bodyMedium,
                  color = colorResource(id = R.color.on_background).copy(alpha = 0.7f),
                  modifier = Modifier.testTag(MyEventsTestTags.TICKET_DATE))

              Text(
                  text = location,
                  style = MaterialTheme.typography.bodyMedium,
                  color = colorResource(id = R.color.on_background).copy(alpha = 0.7f),
                  modifier = Modifier.testTag(MyEventsTestTags.TICKET_LOCATION))
            }
      }
}

@Preview(showBackground = true)
@Composable
fun TicketComponentPreview() {
  OnePassTheme {
    Column(modifier = Modifier.background(colorScheme.background).padding(16.dp)) {
      TicketComponent(
          title = "Lausanne Party",
          status = TicketStatus.CURRENTLY,
          dateTime = "Dec 15, 2024 • 9:00 PM",
          location = "Lausanne, Flon")
      Spacer(modifier = Modifier.height(16.dp))
      TicketComponent(
          title = "Morges Party",
          status = TicketStatus.UPCOMING,
          dateTime = "Dec 15, 2024 • 9:00 PM",
          location = "Morges")
    }
  }
}
