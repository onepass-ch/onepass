package ch.onepass.onepass.ui.eventdetail

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.ui.event.EventCardViewModel
import ch.onepass.onepass.ui.event.LikeButton
import ch.onepass.onepass.ui.organization.OrganizationCard
import ch.onepass.onepass.ui.theme.DefaultBackground
import coil.compose.AsyncImage

object EventDetailTestTags {
  const val SCREEN = "eventDetailScreen"
  const val TITLE = "eventDetailTitle"
  const val EVENT_IMAGE = "eventDetailImage"
  const val LIKE_BUTTON = "eventDetailLikeButton"
  const val EVENT_TITLE = "eventDetailEventTitle"
  const val ORGANIZER_SECTION = "eventDetailOrganizerSection"
  const val ORGANIZER_NAME = "eventDetailOrganizerName"
  const val ORGANIZER_FOLLOWERS = "eventDetailOrganizerFollowers"
  const val ORGANIZER_RATING = "eventDetailOrganizerRating"
  const val ABOUT_EVENT = "eventDetailAboutEvent"
  const val EVENT_DATE = "eventDetailDate"
  const val EVENT_LOCATION = "eventDetailLocation"
  const val MAP_BUTTON = "eventDetailMapButton"
  const val BUY_TICKET_BUTTON = "eventDetailBuyTicketButton"
  const val LOADING = "eventDetailLoading"
  const val ERROR = "eventDetailError"
}

/** Event detail screen displaying full event information. */
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onNavigateToMap: (String) -> Unit = {},
    onBuyTicket: (String) -> Unit = {},
    onNavigateToOrganizerProfile: (String) -> Unit,
    viewModel: EventDetailViewModel =
        viewModel(
            factory = viewModelFactory { initializer { EventDetailViewModel(eventId = eventId) } })
) {
  val event by viewModel.event.collectAsState()
  val organization by viewModel.organization.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val error by viewModel.error.collectAsState()

  val eventCardViewModel = EventCardViewModel.getInstance()
  val likedEvents by eventCardViewModel.likedEvents.collectAsState()
  val isLiked = likedEvents.contains(eventId)

  EventDetailScreenContent(
      uiState =
          EventDetailUiState(
              event = event,
              organization = organization,
              isLoading = isLoading,
              errorMessage = error,
              isLiked = isLiked),
      onBack = onBack,
      onLikeToggle = { eventCardViewModel.toggleLike(eventId) },
      onNavigateToMap = { onNavigateToMap(eventId) },
      onNavigateToOrganizerProfile = onNavigateToOrganizerProfile,
      onBuyTicket = { onBuyTicket(eventId) })
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal data class EventDetailUiState(
    val event: Event? = null,
    val organization: Organization? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLiked: Boolean = false
)

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun EventDetailScreenContent(
    uiState: EventDetailUiState,
    onBack: () -> Unit,
    onLikeToggle: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToOrganizerProfile: (String) -> Unit = {},
    onBuyTicket: () -> Unit
) {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(DefaultBackground)
              .testTag(EventDetailTestTags.SCREEN)) {
        when {
          uiState.isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).testTag(EventDetailTestTags.LOADING),
                color = MaterialTheme.colorScheme.primary)
          }
          uiState.errorMessage != null -> {
            Column(
                modifier =
                    Modifier.align(Alignment.Center)
                        .padding(16.dp)
                        .testTag(EventDetailTestTags.ERROR),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                  Text(text = uiState.errorMessage, color = Color.White)
                  Button(onClick = onBack) { Text("Go Back") }
                }
          }
          uiState.event != null -> {
            EventDetailContent(
                event = uiState.event,
                organization = uiState.organization,
                isLiked = uiState.isLiked,
                onLikeToggle = onLikeToggle,
                onNavigateToMap = onNavigateToMap,
                onBuyTicket = onBuyTicket,
                onNavigateToOrganizerProfile = onNavigateToOrganizerProfile,
                onBack = onBack)

            BuyButton(
                onBuyTicket = onBuyTicket,
                priceText = formatPrice(uiState.event),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth())
          }
        }
      }
}

@Composable
private fun BuyButton(
    modifier: Modifier = Modifier,
    onBuyTicket: () -> Unit = {},
    priceText: String = "text"
) {
  // Buy ticket button - fixed at bottom with padding
  Surface(modifier = modifier, shadowElevation = 8.dp, color = DefaultBackground) {
    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp)) {
      Button(
          onClick = onBuyTicket,
          modifier = Modifier.fillMaxWidth().testTag(EventDetailTestTags.BUY_TICKET_BUTTON),
          shape = RoundedCornerShape(5.dp),
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF413857) // Purple from design
                  )) {
            Text(
                text = priceText,
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 63.dp, vertical = 14.dp))
          }
    }
  }
}

@Composable
private fun BackSection(onBack: () -> Unit) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .background(Color(0xFF1A1A1A)) // Semi-transparent black
              .height(79.dp)
              .padding(start = 22.dp, top = 55.dp, end = 22.dp, bottom = 6.dp)
              .testTag(EventDetailTestTags.TITLE),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.Start)) {
        IconButton(onClick = onBack) {
          Icon(
              painter = painterResource(R.drawable.go_back_vector),
              contentDescription = "Back",
              tint = Color.White)
        }
      }
}

@Composable
private fun EventDetailContent(
    event: Event,
    organization: Organization?,
    isLiked: Boolean,
    onLikeToggle: () -> Unit,
    onNavigateToMap: () -> Unit,
    onBuyTicket: () -> Unit,
    onNavigateToOrganizerProfile: (String) -> Unit,
    onBack: () -> Unit = {}
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("eventDetailScrollableContent")
                .padding(
                    bottom =
                        80.dp), // Add bottom padding so content isn't hidden behind fixed button
        verticalArrangement = Arrangement.spacedBy(30.dp)) {
          // Title placeholder (79px height as per Figma)
          Spacer(modifier = Modifier.height(79.dp))

          // Image with heart button
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(261.dp)
                      .padding(horizontal = 10.dp)
                      .clip(RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.TopEnd) {
                AsyncImage(
                    model =
                        event.imageUrl.ifEmpty {
                          null
                        }, // TODO this will be changed once we have storage
                    contentDescription = "Event image",
                    placeholder = painterResource(R.drawable.image_fallback),
                    error = painterResource(id = R.drawable.image_fallback),
                    modifier = Modifier.fillMaxSize().testTag(EventDetailTestTags.EVENT_IMAGE),
                    contentScale = ContentScale.Crop)

                // Heart button overlay
                LikeButton(
                    isLiked = isLiked,
                    onLikeToggle = { onLikeToggle() },
                    modifier =
                        Modifier.padding(top = 11.dp, end = 11.dp)
                            .testTag(EventDetailTestTags.LIKE_BUTTON))
              }

          // Event title
          Text(
              text = event.title.ifEmpty { "Event Title" },
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
              textAlign = TextAlign.Center,
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(horizontal = 10.dp)
                      .testTag(EventDetailTestTags.EVENT_TITLE))

          // Organizer section
          OrganizerSection(
              organization = organization,
              organizerName = event.organizerName,
              onNavigateToOrganizerProfile = onNavigateToOrganizerProfile,
              modifier = Modifier.padding(horizontal = 10.dp))

          // About Event section
          AboutEventSection(
              description = event.description, modifier = Modifier.padding(horizontal = 10.dp))

          // Event details (date, location, map button, buy ticket button)
          EventDetailsSection(
              event = event,
              onNavigateToMap = onNavigateToMap,
              onBuyTicket = onBuyTicket,
              modifier = Modifier.padding(horizontal = 10.dp))

          // Extra spacing at the end
          Spacer(modifier = Modifier.height(60.dp))
        }

    // BackSection overlay at the top
    BackSection(onBack = onBack)
  }
}

@Composable
private fun OrganizerSection(
    organization: Organization?,
    organizerName: String,
    onNavigateToOrganizerProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier.testTag(EventDetailTestTags.ORGANIZER_SECTION),
      verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = "ORGANIZER",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(vertical = 10.dp))

        if (organization != null) {
          OrganizationCard(
              organization = organization,
              onClick = { onNavigateToOrganizerProfile(organization.id) })
        } else {
          Text(
              text = organizerName,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              modifier = Modifier.padding(vertical = 8.dp))
        }
      }
}

@SuppressLint("ResourceAsColor")
@Composable
private fun AboutEventSection(description: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(
        text = "ABOUT EVENT",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        modifier = Modifier.padding(vertical = 10.dp))

    Text(
        text = description.ifEmpty { "No description available." },
        style = MaterialTheme.typography.bodyMedium,
        color = Color(R.color.icon_color_detailScreen),
        modifier = Modifier.testTag(EventDetailTestTags.ABOUT_EVENT))
  }
}

@Composable
private fun EventDetailsSection(
    event: Event,
    onNavigateToMap: () -> Unit, // Information for navigation: This onNavigateToMap takes the event
    // geolocation and navigate to the map at this location for him to see it.
    onBuyTicket: () -> Unit, // TODO: implement buy ticket action in M3
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(15.dp)) {
    // Date and Location
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
      // Date row
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(20.dp),
          verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = Color(0xFFA3A3A3))
            Text(
                text = event.displayDateTime,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
                modifier = Modifier.testTag(EventDetailTestTags.EVENT_DATE))
          }

      // Location row
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(20.dp),
          verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationCity,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = Color(0xFFA3A3A3))
            Text(
                text = event.displayLocation,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
                modifier = Modifier.testTag(EventDetailTestTags.EVENT_LOCATION))
          }
    }

    // See event on map button
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .border(1.dp, Color(0xFF242424), RoundedCornerShape(0.dp))
                .clickable(onClick = onNavigateToMap)
                .padding(vertical = 14.dp, horizontal = 16.dp)
                .testTag(EventDetailTestTags.MAP_BUTTON),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "See event on map",
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              modifier = Modifier.padding(end = 30.dp))

          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = null,
              modifier = Modifier.rotate(180f),
              tint = Color.White)
        }
  }
}

internal fun formatPrice(event: Event): String {
  val lowestPrice = event.lowestPrice
  return if (lowestPrice == 0u) {
    "FREE"
  } else {
    "Buy ticket for ${lowestPrice}${event.currency.lowercase()}"
  }
}
