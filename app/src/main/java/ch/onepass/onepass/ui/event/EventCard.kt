package ch.onepass.onepass.ui.event

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.components.buttons.LikeButton
import ch.onepass.onepass.ui.theme.EventCardDimens
import ch.onepass.onepass.ui.theme.Shadow
import ch.onepass.onepass.utils.FormatUtils.formatPriceCompact
import coil.compose.AsyncImage

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
  val price = formatPriceCompact(event.lowestPrice.toDouble())
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
                  spotColor = Shadow,
                  ambientColor = Shadow,
              )
              .shadow(
                  elevation = EventCardDimens.shadowElevation2,
                  spotColor = Shadow,
                  ambientColor = Shadow,
              )
              .fillMaxWidth()
              .widthIn(max = EventCardDimens.maxWidth)
              .onSizeChanged { size -> cardWidth = with(density) { size.width.toDp() } }
              .then(calculatedHeight?.let { height -> Modifier.heightIn(min = height) } ?: Modifier)
              .background(
                  color = colorScheme.surface,
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
            contentDescription = stringResource(R.string.event_card_image_description, event.title),
            modifier = Modifier.fillMaxSize().testTag(C.Tag.event_card_image),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.image_fallback),
            error = painterResource(id = R.drawable.image_fallback),
        )
      } else {
        Image(
            modifier = Modifier.fillMaxSize().testTag(C.Tag.event_card_image),
            painter = painterResource(id = R.drawable.image_fallback),
            contentDescription = stringResource(R.string.event_card_default_image_description),
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
            color = colorScheme.onBackground,
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
            color = colorScheme.onSurface,
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
          color = colorScheme.onBackground,
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
            color = colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).testTag(C.Tag.event_card_location),
        )

        Spacer(modifier = Modifier.width(EventCardDimens.locationPriceSpacing))

        Text(
            text = price,
            style = MaterialTheme.typography.headlineSmall,
            color = colorScheme.onBackground,
            modifier = Modifier.testTag(C.Tag.event_card_price),
        )
      }
    }
  }
}
