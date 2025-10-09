package com.android.sample.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.resources.C

// Marc Font Family
val MarcFontFamily =
    FontFamily(
        Font(R.font.marc_light, FontWeight.Light),
        Font(R.font.marc_light_italic, FontWeight.Light, FontStyle.Italic),
        Font(R.font.marc_regular, FontWeight.Normal),
        Font(R.font.marc_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.marc_bold, FontWeight.Bold),
        Font(R.font.marc_bold_italic, FontWeight.Bold, FontStyle.Italic))

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
              .shadow(
                  elevation = 4.dp, spotColor = Color(0x1A000000), ambientColor = Color(0x1A000000))
              .shadow(
                  elevation = 6.dp, spotColor = Color(0x1A000000), ambientColor = Color(0x1A000000))
              .width(392.dp)
              .height(417.93866.dp)
              .background(color = Color(0xFF262626), shape = RoundedCornerShape(size = 10.dp))
              .padding(start = 1.dp, end = 1.dp),
      verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Image container with clipping
    Box(
        modifier =
            Modifier.width(390.dp)
                .height(255.98468.dp)
                .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp)),
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
                      .padding(12.dp)
                      .testTag(C.Tag.event_card_like_button))
        }
    Column(
        modifier = Modifier.width(390.dp).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start) {
          // Title and Organizer section (Grid row 1)
          Column(
              modifier = Modifier.fillMaxWidth().height(55.17.dp),
              verticalArrangement = Arrangement.Top,
              horizontalAlignment = Alignment.Start) {
                Text(
                    text = eventTitle,
                    fontFamily = MarcFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 27.6.sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = 9.dp).testTag(C.Tag.event_card_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(
                    text = eventOrganizer,
                    fontFamily = MarcFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 20.8.sp,
                    color = Color(0xFF9CA3AF), // gray-400
                    modifier = Modifier.testTag(C.Tag.event_card_organizer),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
              }

          // Separation space (Grid row 2-3 - empty rows with gap)
          Spacer(modifier = Modifier.height(20.dp))

          // Date (Grid row 3/4)
          Text(
              text = eventDate,
              fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 16.sp,
              lineHeight = 20.8.sp,
              color = Color(0xFF841DA4), // Purple/magenta color
              modifier = Modifier.testTag(C.Tag.event_card_date),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)

          Spacer(modifier = Modifier.height(4.dp))

          // Address and Price (Grid row 5)
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = eventLocation,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 20.8.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).testTag(C.Tag.event_card_location))

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (eventPrice == 0u) "FREE" else "CHF$eventPrice",
                    fontFamily = MarcFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    color = Color.White,
                    modifier = Modifier.testTag(C.Tag.event_card_price))
              }
        }
  }
}
