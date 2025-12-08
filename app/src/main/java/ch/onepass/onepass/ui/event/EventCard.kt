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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.components.buttons.LikeButton
import ch.onepass.onepass.ui.theme.CardBackground
import ch.onepass.onepass.ui.theme.CardShadow
import ch.onepass.onepass.ui.theme.EventCardDimens
import ch.onepass.onepass.ui.theme.EventDateColor
import ch.onepass.onepass.ui.theme.OnePassTheme
import ch.onepass.onepass.ui.theme.TextSecondary
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar

/**
 * A card component that displays information about an event.
 *
 * The card shows:
 * - Event image with a fallback placeholder
 * - Close button at the top-left corner
 * - Like button at the top-right corner
 * - Title and organizer
 * - Event date
 * - Event location and price
 *
 * The card layout and spacing are based on Figma designs.
 *
 * @param event The [Event] whose details are displayed.
 * @param modifier Optional [Modifier] for styling, layout, or gesture handling. Defaults to
 *   [Modifier].
 * @param onCardClick Lambda called when the card is clicked.
 */
@Composable
fun EventCard(
    event: Event,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    onLikeToggle: (String) -> Unit = {},
    onCardClick: () -> Unit = {},
) {
  val title = event.title
  val date = event.displayDateTime
  val location = event.displayLocation
  val price = event.lowestPrice
  val organizer = event.organizerName

  // Responsive aspect ratio: maintain proportions while adapting to screen size
  val aspectRatio = 392f / 417.93866f // Original Figma ratio
  val density = LocalDensity.current
  var cardWidth by remember { mutableStateOf(0.dp) }

  // Calculate responsive height based on measured width
  val calculatedHeight =
      remember(cardWidth) {
        if (cardWidth > 0.dp) {
          cardWidth / aspectRatio
        } else {
          null
        }
      }

  Column(
      modifier =
          modifier
              .testTag(C.Tag.event_card)
              .clickable(onClick = onCardClick)
              .shadow(
                  elevation = EventCardDimens.shadowElevation1,
                  spotColor = CardShadow,
                  ambientColor = CardShadow,
              )
              .shadow(
                  elevation = EventCardDimens.shadowElevation2,
                  spotColor = CardShadow,
                  ambientColor = CardShadow,
              )
              .fillMaxWidth()
              .widthIn(max = EventCardDimens.maxWidth)
              .onSizeChanged { size -> cardWidth = with(density) { size.width.toDp() } }
              .then(calculatedHeight?.let { height -> Modifier.heightIn(min = height) } ?: Modifier)
              .background(
                  color = CardBackground,
                  shape = RoundedCornerShape(size = EventCardDimens.cornerRadius),
              ),
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
                        topEnd = EventCardDimens.imageCornerRadius,
                    )),
        contentAlignment = Alignment.Center,
    ) {
      // Use AsyncImage to load event images from URLs, or fallback to default image
      if (event.imageUrl.isNotEmpty()) {
        AsyncImage(
            model = event.imageUrl,
            contentDescription = "Event image for ${event.title}",
            modifier = Modifier.fillMaxSize().testTag(C.Tag.event_card_image),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.image_fallback),
            error = painterResource(id = R.drawable.image_fallback),
        )
      } else {
        Image(
            modifier = Modifier.fillMaxSize().testTag(C.Tag.event_card_image),
            painter = painterResource(id = R.drawable.image_fallback),
            contentDescription = "Default event image",
            contentScale = ContentScale.Crop,
        )
      }
      // Like button in top-right corner
      LikeButton(
          isLiked = isLiked,
          onLikeToggle = { onLikeToggle(event.eventId) },
          modifier =
              Modifier.align(Alignment.TopEnd)
                  .padding(EventCardDimens.likeButtonPadding)
                  .testTag(C.Tag.event_card_like_button))
    }
    Column(
        modifier =
            Modifier.fillMaxWidth().padding(horizontal = EventCardDimens.contentHorizontalPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
      // Title and Organizer section (Grid row 1)
      Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.Top,
          horizontalAlignment = Alignment.Start,
      ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
            color = Color.White,
            modifier =
                Modifier.padding(top = EventCardDimens.titleTopPadding)
                    .testTag(C.Tag.event_card_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = organizer,
            style =
                MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 18.sp, fontWeight = FontWeight.Normal),
            color = TextSecondary,
            modifier = Modifier.testTag(C.Tag.event_card_organizer),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }

      // Separation space (Grid row 2-3 - empty rows with gap)
      Spacer(modifier = Modifier.height(EventCardDimens.sectionSpacing))

      // Date (Grid row 3/4)
      Text(
          text = date,
          style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
          color = EventDateColor,
          modifier = Modifier.testTag(C.Tag.event_card_date),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(EventCardDimens.dateLocationSpacing))

      // Address and Price (Grid row 5)
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = location,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).testTag(C.Tag.event_card_location),
        )

        Spacer(modifier = Modifier.width(EventCardDimens.locationPriceSpacing))

        Text(
            text = if (price == 0u) "FREE" else "CHF$price",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.testTag(C.Tag.event_card_price),
        )
      }
    }
  }
}

/** Preview function for EventCard component. */
@Preview(showBackground = false, name = "Event Card")
@Composable
private fun EventCardPreview() {
  OnePassTheme {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      // Sample event with image
      EventCard(
          event =
              createSampleEvent(
                  eventId = "1",
                  title = "Summer Music Festival",
                  organizerName = "Music Events Inc.",
                  location =
                      Location(coordinates = GeoPoint(46.5197, 6.6323), name = "Lausanne, Flon"),
                  price = 50.0,
                  imageUrl = "https://picsum.photos/400/300"),
          isLiked = false,
          onLikeToggle = {},
          onCardClick = {})

      // Sample event without image (fallback)
      EventCard(
          event =
              createSampleEvent(
                  eventId = "2",
                  title = "Tech Conference 2024",
                  organizerName = "Tech Hub",
                  location = Location(coordinates = GeoPoint(47.3769, 8.5417), name = "Zurich, HB"),
                  price = 0.0,
                  imageUrl = ""),
          isLiked = true,
          onLikeToggle = {},
          onCardClick = {})

      // Free event
      EventCard(
          event =
              createSampleEvent(
                  eventId = "3",
                  title = "Community Meetup",
                  organizerName = "Local Community",
                  location = Location(coordinates = GeoPoint(46.2044, 6.1432), name = "Geneva"),
                  price = 0.0,
                  imageUrl = "https://picsum.photos/400/300?random=3"),
          isLiked = false,
          onLikeToggle = {},
          onCardClick = {})
    }
  }
}

/** Helper function to create sample Event data for previews. */
private fun createSampleEvent(
    eventId: String,
    title: String,
    organizerName: String,
    location: Location,
    price: Double,
    imageUrl: String
): Event {
  val calendar = Calendar.getInstance()
  calendar.add(Calendar.DAY_OF_MONTH, 30)
  val startTime = Timestamp(calendar.time)
  calendar.add(Calendar.HOUR_OF_DAY, 3)
  val endTime = Timestamp(calendar.time)

  return Event(
      eventId = eventId,
      title = title,
      description = "Sample event description",
      organizerId = "org-1",
      organizerName = organizerName,
      status = EventStatus.PUBLISHED,
      location = location,
      startTime = startTime,
      endTime = endTime,
      capacity = 100,
      ticketsRemaining = 75,
      ticketsIssued = 25,
      ticketsRedeemed = 0,
      currency = "CHF",
      pricingTiers =
          if (price > 0) {
            listOf(PricingTier("General", price, 100, 75))
          } else {
            emptyList()
          },
      images = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList(),
      tags = listOf("sample", "preview"),
      createdAt = Timestamp.now(),
      updatedAt = Timestamp.now())
}
