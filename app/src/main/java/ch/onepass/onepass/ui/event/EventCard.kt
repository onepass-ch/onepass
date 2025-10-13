package ch.onepass.onepass.ui.event

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ch.onepass.onepass.R
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.theme.CardBackground
import ch.onepass.onepass.ui.theme.CardShadow
import ch.onepass.onepass.ui.theme.EventCardDimens
import ch.onepass.onepass.ui.theme.EventDateColor
import ch.onepass.onepass.ui.theme.OnePassTheme
import ch.onepass.onepass.ui.theme.TextSecondary

/** Event card component that displays event information */
@Composable
fun EventCard(
    eventPrice: UInt,
    eventTitle: String,
    eventDate: String,
    eventLocation: String,
    eventOrganizer: String,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
  var isLiked by remember { mutableStateOf(false) }

  Column(
      modifier =
          modifier
              .testTag(C.Tag.event_card)
              .clickable(onClick = onCardClick)
              .shadow(
                  elevation = EventCardDimens.shadowElevation1,
                  spotColor = CardShadow,
                  ambientColor = CardShadow)
              .shadow(
                  elevation = EventCardDimens.shadowElevation2,
                  spotColor = CardShadow,
                  ambientColor = CardShadow)
              .fillMaxWidth()
              .padding(
                  start = EventCardDimens.eventCardPadding, end = EventCardDimens.eventCardPadding)
              .widthIn(max = EventCardDimens.maxWidth)
              .aspectRatio(392f / 417.93866f) // based on figma design
              .background(
                  color = CardBackground,
                  shape = RoundedCornerShape(size = EventCardDimens.cornerRadius))
              .padding(
                  start = EventCardDimens.horizontalPadding,
                  end = EventCardDimens.horizontalPadding),
      verticalArrangement = Arrangement.spacedBy(EventCardDimens.verticalSpacing, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Image container with clipping
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .aspectRatio(1f / EventCardDimens.imageHeightRatio)
                .clip(
                    RoundedCornerShape(
                        topStart = EventCardDimens.imageCornerRadius,
                        topEnd = EventCardDimens.imageCornerRadius)),
        contentAlignment = Alignment.Center) {
          Image(
              modifier = Modifier.fillMaxSize().testTag(C.Tag.event_card_image),
              painter = painterResource(id = R.drawable.image_fallback),
              contentDescription = "image description",
              contentScale = ContentScale.Crop)

          // Like button in top-right corner
          LikeButton(
              isLiked = isLiked,
              onLikeToggle = { isLiked = it },
              modifier =
                  Modifier.align(Alignment.TopEnd)
                      .padding(EventCardDimens.likeButtonPadding)
                      .testTag(C.Tag.event_card_like_button))
        }
    Column(
        modifier =
            Modifier.fillMaxWidth().padding(horizontal = EventCardDimens.contentHorizontalPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start) {
          // Title and Organizer section (Grid row 1)
          Column(
              modifier = Modifier.fillMaxWidth().height(EventCardDimens.titleSectionHeight),
              verticalArrangement = Arrangement.Top,
              horizontalAlignment = Alignment.Start) {
                Text(
                    text = eventTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier =
                        Modifier.padding(top = EventCardDimens.titleTopPadding)
                            .testTag(C.Tag.event_card_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(
                    text = eventOrganizer,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.testTag(C.Tag.event_card_organizer),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
              }

          // Separation space (Grid row 2-3 - empty rows with gap)
          Spacer(modifier = Modifier.height(EventCardDimens.sectionSpacing))

          // Date (Grid row 3/4)
          Text(
              text = eventDate,
              style = MaterialTheme.typography.bodyMedium,
              color = EventDateColor,
              modifier = Modifier.testTag(C.Tag.event_card_date),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)

          Spacer(modifier = Modifier.height(EventCardDimens.dateLocationSpacing))

          // Address and Price (Grid row 5)
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = eventLocation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).testTag(C.Tag.event_card_location))

                Spacer(modifier = Modifier.width(EventCardDimens.locationPriceSpacing))

                Text(
                    text = if (eventPrice == 0u) "FREE" else "CHF$eventPrice",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.testTag(C.Tag.event_card_price))
              }
        }
  }
}

@Preview(showBackground = true)
@Composable
fun PreviewEventCard() {
  OnePassTheme {
    Box(
        modifier = Modifier.fillMaxSize().background(color = Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center) {
          EventCard(
              eventDate = "December",
              eventPrice = 0u,
              eventTitle = "Lausanne Free Party",
              eventLocation = "Lausanne, flon",
              eventOrganizer = "Best Organizer")
        }
  }
}
