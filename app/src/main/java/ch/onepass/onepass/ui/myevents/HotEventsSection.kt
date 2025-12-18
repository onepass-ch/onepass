package ch.onepass.onepass.ui.myevents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.ui.theme.MarcFontFamily
import coil.compose.AsyncImage

/**
 * Section displaying featured/popular events with available tickets.
 *
 * @param events List of hot events to display.
 * @param onEventClick Callback when an event is clicked.
 * @param isLoading Whether the events are loading.
 * @param modifier Modifier for styling.
 */
@Composable
fun HotEventsSection(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    // Section title
    Text(
        text = stringResource(R.string.hot_events_title),
        modifier =
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(MyEventsTestTags.HOT_EVENTS_TITLE),
        style =
            MaterialTheme.typography.titleMedium.copy(
                fontFamily = MarcFontFamily, fontWeight = FontWeight.Bold),
        color = colorScheme.onBackground)

    when {
      isLoading -> {
        // Loading state
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentAlignment = Alignment.Center) {
              CircularProgressIndicator(
                  color = colorScheme.primary, modifier = Modifier.size(32.dp))
            }
      }
      events.isEmpty() -> {
        // Empty state
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(100.dp)
                    .padding(horizontal = 16.dp)
                    .testTag(MyEventsTestTags.HOT_EVENTS_EMPTY),
            contentAlignment = Alignment.Center) {
              Text(
                  text = stringResource(R.string.hot_events_empty),
                  style = MaterialTheme.typography.bodyMedium,
                  color = colorScheme.onSurface)
            }
      }
      else -> {
        // Horizontal scrollable list of events
        LazyRow(
            modifier = Modifier.fillMaxWidth().testTag(MyEventsTestTags.HOT_EVENTS_LIST),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              items(items = events, key = { it.eventId }) { event ->
                HotEventCard(
                    event = event,
                    onClick = { onEventClick(event) },
                    modifier = Modifier.testTag(MyEventsTestTags.HOT_EVENT_CARD))
              }
            }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

/**
 * Card displaying a hot event with Spotify-inspired album design. Features a rounded image box with
 * event title and date below.
 *
 * @param event The event to display.
 * @param onClick Callback when the card is clicked.
 * @param modifier Modifier for styling.
 */
@Composable
private fun HotEventCard(event: Event, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.width(140.dp).clickable(onClick = onClick),
      horizontalAlignment = Alignment.Start) {
        // Rounded image box (Spotify-style album cover)
        Box(
            modifier =
                Modifier.size(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surface)) {
              // Event image
              if (event.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription =
                        stringResource(R.string.hot_events_image_description, event.title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.image_fallback),
                    error = painterResource(id = R.drawable.image_fallback))
              } else {
                Image(
                    painter = painterResource(id = R.drawable.image_fallback),
                    contentDescription =
                        stringResource(R.string.hot_events_default_image_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop)
              }
            }

        Spacer(modifier = Modifier.height(8.dp))

        // Event title with ellipsis
        Text(
            text = event.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(4.dp))

        // Event date with ellipsis
        Text(
            text = event.displayDateTime,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth())
      }
}
