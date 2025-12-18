package ch.onepass.onepass.ui.organization

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.ui.myevents.TicketComponent
import ch.onepass.onepass.ui.myevents.TicketStatus
import ch.onepass.onepass.ui.navigation.BackNavigationScaffold
import ch.onepass.onepass.ui.navigation.TopBarConfig
import ch.onepass.onepass.ui.theme.Typography
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collectLatest

object OrganizerProfileTestTags {
  const val SCREEN = "organizer_profile_screen"
  const val HEADER_SECTION = "organizer_header_section"
  const val BANNER_IMAGE = "organizer_banner_image"
  const val PROFILE_IMAGE = "organizer_profile_image"
  const val INFO_SECTION = "organizer_info_section"
  const val NAME_TEXT = "organizer_name_text"
  const val DESCRIPTION_TEXT = "organizer_description_text"
  const val SOCIAL_MEDIA_SECTION = "organizer_social_media_section"
  const val WEBSITE_LINK = "organizer_website_link"
  const val WEBSITE_TEXT = "organizer_website_text"
  const val INSTAGRAM_ICON = "organizer_instagram_icon"
  const val TIKTOK_ICON = "organizer_tiktok_icon"
  const val FACEBOOK_ICON = "organizer_facebook_icon"
  const val FOLLOW_SECTION = "organizer_follow_section"
  const val FOLLOW_BUTTON = "organizer_follow_button"
  const val FOLLOW_BUTTON_TEXT = "organizer_follow_button_text"
  const val FOLLOWERS_TEXT = "organizer_followers_text"
  const val EDIT_ORGANIZATION_BUTTON = "organizer_edit_organization_button"
  const val TAB_SECTION = "organizer_tab_section"
  const val TAB_POSTS = "organizer_tab_posts"
  const val TAB_UPCOMING = "organizer_tab_upcoming"
  const val TAB_PAST = "organizer_tab_past"
  const val EVENT_LIST = "organizer_event_list"
  const val BACK_ARROW = "organizer_back_arrow"
}

// Extracted Components for better testability and modularity

@Composable
fun OrganizerHeaderSection(
    bannerImageUrl: String?,
    profileImageUrl: String?,
    modifier: Modifier = Modifier
) {
  Column(
      verticalArrangement = Arrangement.spacedBy((-49).dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          modifier.width(392.dp).height(310.dp).testTag(OrganizerProfileTestTags.HEADER_SECTION)) {
        // Banner - Load from URL if available, otherwise use fallback
        AsyncImage(
            model = bannerImageUrl,
            contentDescription = stringResource(R.string.organizer_banner_description),
            contentScale = ContentScale.FillBounds,
            error = painterResource(id = R.drawable.image_fallback),
            placeholder = painterResource(id = R.drawable.image_fallback),
            modifier =
                Modifier.fillMaxWidth()
                    .height(261.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag(OrganizerProfileTestTags.BANNER_IMAGE))
        // Profile Picture - Load from URL if available, otherwise use fallback
        AsyncImage(
            model = profileImageUrl,
            contentDescription = stringResource(R.string.organizer_profile_picture_description),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.ic_launcher_foreground),
            placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
            modifier =
                Modifier.width(98.dp)
                    .height(98.dp)
                    .background(color = colorScheme.surface, shape = RoundedCornerShape(50.dp))
                    .clip(RoundedCornerShape(50.dp))
                    .testTag(OrganizerProfileTestTags.PROFILE_IMAGE))
      }
}

@Composable
fun OrganizerInfoSection(name: String, description: String, modifier: Modifier = Modifier) {
  Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = modifier.fillMaxWidth().testTag(OrganizerProfileTestTags.INFO_SECTION)) {
        // Name/Title
        Text(
            text = name,
            style = Typography.titleLarge,
            modifier = Modifier.testTag(OrganizerProfileTestTags.NAME_TEXT))
        // Description
        Text(
            text = description,
            style = Typography.bodySmall.copy(textAlign = TextAlign.Center),
            modifier =
                Modifier.padding(horizontal = 20.dp)
                    .testTag(OrganizerProfileTestTags.DESCRIPTION_TEXT))
      }
}

@Composable
fun SocialMediaIcon(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
  Image(
      painter = painterResource(id = iconRes),
      contentDescription = contentDescription,
      contentScale = ContentScale.None,
      modifier = modifier.width(30.dp).height(30.dp).clickable { onClick() })
}

@Composable
fun SocialMediaSection(
    websiteUrl: String?,
    instagramUrl: String?,
    tiktokUrl: String?,
    facebookUrl: String?,
    onWebsiteClick: () -> Unit = {},
    onInstagramClick: () -> Unit = {},
    onTiktokClick: () -> Unit = {},
    onFacebookClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
  Row(
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          modifier
              .fillMaxWidth()
              .wrapContentHeight()
              .padding(horizontal = 20.dp)
              .testTag(OrganizerProfileTestTags.SOCIAL_MEDIA_SECTION)) {
        // Website Link
        if (websiteUrl != null) {
          Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                  Modifier.testTag(OrganizerProfileTestTags.WEBSITE_LINK).clickable {
                    onWebsiteClick()
                  }) {
                Image(
                    painter = painterResource(id = R.drawable.internet_logo),
                    contentDescription =
                        stringResource(R.string.organizer_website_icon_description),
                    contentScale = ContentScale.None,
                    modifier = Modifier.width(20.dp).height(20.dp))
                Text(
                    text = stringResource(R.string.organizer_website_text),
                    style = Typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp),
                    modifier = Modifier.testTag(OrganizerProfileTestTags.WEBSITE_TEXT))
                Image(
                    painter = painterResource(id = R.drawable.link_icon),
                    contentDescription =
                        stringResource(R.string.organizer_external_link_description),
                    contentScale = ContentScale.None)
              }
        }

        // Social Media Icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              if (instagramUrl != null) {
                SocialMediaIcon(
                    iconRes = R.drawable.instagram_logo,
                    contentDescription = stringResource(R.string.organizer_instagram_description),
                    onClick = onInstagramClick,
                    modifier = Modifier.testTag(OrganizerProfileTestTags.INSTAGRAM_ICON))
              }
              if (tiktokUrl != null) {
                SocialMediaIcon(
                    iconRes = R.drawable.tiktok_logo,
                    contentDescription = stringResource(R.string.organizer_tiktok_description),
                    onClick = onTiktokClick,
                    modifier = Modifier.testTag(OrganizerProfileTestTags.TIKTOK_ICON))
              }
              if (facebookUrl != null) {
                SocialMediaIcon(
                    iconRes = R.drawable.facebook_logo,
                    contentDescription = stringResource(R.string.organizer_facebook_description),
                    onClick = onFacebookClick,
                    modifier = Modifier.testTag(OrganizerProfileTestTags.FACEBOOK_ICON))
              }
            }
      }
}

@Composable
fun FollowSection(
    modifier: Modifier = Modifier,
    followersCount: String,
    isFollowing: Boolean,
    isOwner: Boolean,
    onFollowClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          modifier
              .fillMaxWidth()
              .wrapContentHeight()
              .padding(horizontal = 20.dp)
              .testTag(OrganizerProfileTestTags.FOLLOW_SECTION)) {
        // Follow Button Row
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()) {
              // Follow Button
              Row(
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                      Modifier.width(182.dp)
                          .height(42.dp)
                          .background(
                              color =
                                  if (isFollowing) colorScheme.primary
                                  else colorScheme.outline.copy(alpha = 0.2f),
                              shape = RoundedCornerShape(size = 5.dp))
                          .clickable { onFollowClick() }
                          .testTag(OrganizerProfileTestTags.FOLLOW_BUTTON)) {
                    Text(
                        text =
                            if (isFollowing) stringResource(R.string.organizer_following_button)
                            else stringResource(R.string.organizer_follow_button),
                        style = Typography.titleMedium,
                        modifier = Modifier.testTag(OrganizerProfileTestTags.FOLLOW_BUTTON_TEXT),
                        color = colorScheme.onBackground)
                  }
              // Community Count
              Text(
                  text = stringResource(R.string.organizer_join_community, followersCount),
                  style = Typography.titleMedium.copy(fontSize = 12.sp, lineHeight = 20.sp),
                  modifier =
                      Modifier.fillMaxWidth()
                          .wrapContentWidth(Alignment.CenterHorizontally)
                          .testTag(OrganizerProfileTestTags.FOLLOWERS_TEXT))
            }

        // Edit Organization Button (only for owners)
        if (isOwner) {
          Button(
              onClick = onEditClick,
              modifier =
                  Modifier.fillMaxWidth()
                      .height(42.dp)
                      .testTag(OrganizerProfileTestTags.EDIT_ORGANIZATION_BUTTON),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = colorScheme.background,
                      contentColor = colorScheme.onBackground),
              shape = RoundedCornerShape(5.dp)) {
                Text(
                    text = stringResource(R.string.organizer_edit_button),
                    style = Typography.titleMedium)
              }
        }
      }
}

@Composable
fun Tab(
    text: String,
    tab: OrganizerProfileTab,
    selectedTab: OrganizerProfileTab,
    indicatorWidth: Int,
    onTabSelected: (OrganizerProfileTab) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
  val selectedColor = colorScheme.primary
  val unselectedColor = colorScheme.outline
  val isSelected = selectedTab == tab

  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
    Text(
        text = text,
        style =
            Typography.titleMedium.copy(color = if (isSelected) selectedColor else unselectedColor),
        modifier =
            Modifier.testTag(testTag).clickable { onTabSelected(tab) }.padding(bottom = 4.dp))
    if (isSelected) {
      Spacer(
          modifier =
              Modifier.width(indicatorWidth.dp).height(2.dp).background(color = selectedColor))
    }
  }
}

@Composable
fun TabSection(
    selectedTab: OrganizerProfileTab = OrganizerProfileTab.UPCOMING,
    onTabSelected: (OrganizerProfileTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
  Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          modifier
              .fillMaxWidth()
              .wrapContentHeight()
              .padding(horizontal = 20.dp)
              .testTag(OrganizerProfileTestTags.TAB_SECTION)) {
        Tab(
            text = stringResource(R.string.organizer_tab_posts),
            tab = OrganizerProfileTab.POSTS,
            selectedTab = selectedTab,
            indicatorWidth = 50,
            onTabSelected = onTabSelected,
            testTag = OrganizerProfileTestTags.TAB_POSTS)
        Tab(
            text = stringResource(R.string.organizer_tab_upcoming),
            tab = OrganizerProfileTab.UPCOMING,
            selectedTab = selectedTab,
            indicatorWidth = 80,
            onTabSelected = onTabSelected,
            testTag = OrganizerProfileTestTags.TAB_UPCOMING)
        Tab(
            text = stringResource(R.string.organizer_tab_past),
            tab = OrganizerProfileTab.PAST,
            selectedTab = selectedTab,
            indicatorWidth = 40,
            onTabSelected = onTabSelected,
            testTag = OrganizerProfileTestTags.TAB_PAST)
      }
}

/**
 * Main OrganizerProfileScreen that integrates with ViewModel. This is the entry point for the
 * organizer profile feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerProfileScreen(
    organizationId: String,
    viewModel: OrganizerProfileViewModel = viewModel(),
    onEffect: (OrganizerProfileEffect) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
  val state by viewModel.state.collectAsState()

  LaunchedEffect(organizationId) { viewModel.loadOrganizationProfile(organizationId) }

  LaunchedEffect(viewModel) { viewModel.effects.collectLatest { effect -> onEffect(effect) } }

  BackNavigationScaffold(
      TopBarConfig(
          title = stringResource(R.string.organizer_profile_title),
          backButtonTestTag = OrganizerProfileTestTags.BACK_ARROW),
      onBack = onNavigateBack,
  ) {
    OrganizerProfileContent(
        name = state.name,
        description = state.description,
        bannerImageUrl = state.coverImageUrl,
        profileImageUrl = state.profileImageUrl,
        websiteUrl = state.websiteUrl,
        instagramUrl = state.instagramUrl,
        tiktokUrl = state.tiktokUrl,
        facebookUrl = state.facebookUrl,
        followersCount = state.followersCountFormatted,
        isFollowing = state.isFollowing,
        isOwner = state.isOwner,
        selectedTab = state.selectedTab,
        upcomingEvents = state.upcomingEvents,
        pastEvents = state.pastEvents,
        onFollowClick = { viewModel.onFollowClicked() },
        onWebsiteClick = { viewModel.onWebsiteClicked() },
        onInstagramClick = { viewModel.onSocialMediaClicked("instagram") },
        onTiktokClick = { viewModel.onSocialMediaClicked("tiktok") },
        onFacebookClick = { viewModel.onSocialMediaClicked("facebook") },
        onEditOrganizationClick = { viewModel.onEditOrganizationClicked() },
        onTabSelected = { viewModel.onTabSelected(it) },
        onEventClick = { viewModel.onEventClicked(it) })
  }
}

/**
 * This is just the preview of the OrganizerProfileContent composable independant of the viewModel
 * for easier UI testing.
 */
@Preview
@Composable
fun OrganizerProfileContent(
    name: String = "No Title",
    description: String =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed vitae nisi nec magna consequat tincidunt. Curabitur suscipit sem vel.",
    bannerImageUrl: String? = null,
    profileImageUrl: String? = null,
    websiteUrl: String? = "https://example.com",
    instagramUrl: String? = "instagram",
    tiktokUrl: String? = "tiktok",
    facebookUrl: String? = "facebook",
    followersCount: String = "2.4K",
    isFollowing: Boolean = false,
    isOwner: Boolean = false,
    selectedTab: OrganizerProfileTab = OrganizerProfileTab.UPCOMING,
    upcomingEvents: List<Event> = emptyList(),
    pastEvents: List<Event> = emptyList(),
    onFollowClick: () -> Unit = {},
    onWebsiteClick: () -> Unit = {},
    onInstagramClick: () -> Unit = {},
    onTiktokClick: () -> Unit = {},
    onFacebookClick: () -> Unit = {},
    onEditOrganizationClick: () -> Unit = {},
    onTabSelected: (OrganizerProfileTab) -> Unit = {},
    onEventClick: (String) -> Unit = {}
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          Modifier.fillMaxSize()
              .background(color = colorScheme.background)
              .verticalScroll(rememberScrollState())
              .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 12.dp)
              .testTag(OrganizerProfileTestTags.SCREEN)) {
        // Header with banner and profile picture
        OrganizerHeaderSection(bannerImageUrl = bannerImageUrl, profileImageUrl = profileImageUrl)

        // Name and description
        OrganizerInfoSection(name = name, description = description)

        // Social media links
        SocialMediaSection(
            websiteUrl = websiteUrl,
            instagramUrl = instagramUrl,
            tiktokUrl = tiktokUrl,
            facebookUrl = facebookUrl,
            onWebsiteClick = onWebsiteClick,
            onInstagramClick = onInstagramClick,
            onTiktokClick = onTiktokClick,
            onFacebookClick = onFacebookClick)

        // Follow button and community count
        FollowSection(
            followersCount = followersCount,
            isFollowing = isFollowing,
            isOwner = isOwner,
            onFollowClick = onFollowClick,
            onEditClick = onEditOrganizationClick)

        // Section separator
        Spacer(modifier = Modifier.height(50.dp))

        // Tab section
        TabSection(selectedTab = selectedTab, onTabSelected = onTabSelected)

        // Tab content based on selection
        when (selectedTab) {
          OrganizerProfileTab.POSTS -> PostsTabContent()
          OrganizerProfileTab.UPCOMING ->
              UpcomingTabContent(events = upcomingEvents, onEventClick = onEventClick)
          OrganizerProfileTab.PAST ->
              PastTabContent(events = pastEvents, onEventClick = onEventClick)
        }
      }
}

/** Content for the Posts tab. Currently empty, to be implemented in the future. */
@Composable
fun PostsTabContent(modifier: Modifier = Modifier) {
  // TODO: Implement posts content
  Column(
      modifier =
          modifier.fillMaxWidth().padding(top = 8.dp).testTag(OrganizerProfileTestTags.EVENT_LIST),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.organizer_posts_empty),
            style = Typography.bodyMedium.copy(color = colorScheme.outline),
            modifier = Modifier.padding(32.dp))
      }
}

/**
 * Content for the Upcoming tab. Shows events that are currently happening or will happen in the
 * future.
 */
@Composable
fun UpcomingTabContent(
    events: List<Event>,
    onEventClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(top = 8.dp, bottom = 8.dp)
              .testTag(OrganizerProfileTestTags.EVENT_LIST),
      verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally) {
        if (events.isEmpty()) {
          Text(
              text = stringResource(R.string.organizer_upcoming_empty),
              style = Typography.bodyMedium.copy(color = colorScheme.outline),
              modifier = Modifier.padding(32.dp))
        } else {
          events
              .filter { event -> determineTicketStatus(event) != TicketStatus.EXPIRED }
              .forEach { event ->
                TicketComponent(
                    title = event.title,
                    status = determineTicketStatus(event),
                    dateTime = event.displayDateTime,
                    location = event.displayLocation,
                    onCardClick = { onEventClick(event.eventId) })
              }
        }
      }
}

/** Content for the Past tab. Shows events that have already ended. */
@Composable
fun PastTabContent(
    events: List<Event>,
    onEventClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(top = 8.dp, bottom = 8.dp)
              .testTag(OrganizerProfileTestTags.EVENT_LIST),
      verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally) {
        if (events.isEmpty()) {
          Text(
              text = stringResource(R.string.organizer_past_empty),
              style = Typography.bodyMedium.copy(color = colorScheme.outline),
              modifier = Modifier.padding(32.dp))
        } else {
          events
              .filter { event -> determineTicketStatus(event) == TicketStatus.EXPIRED }
              .forEach { event ->
                TicketComponent(
                    title = event.title,
                    status = TicketStatus.EXPIRED,
                    dateTime = event.displayDateTime,
                    location = event.displayLocation,
                    onCardClick = { onEventClick(event.eventId) })
              }
        }
      }
}

/**
 * Determines the ticket status for an event based on current time.
 * - CURRENTLY: Event is happening now (between start and end time)
 * - UPCOMING: Event hasn't started yet
 */
private fun determineTicketStatus(event: Event): TicketStatus {
  val now = Timestamp.now()
  val startTime = event.startTime
  val endTime = event.endTime

  return when {
    startTime != null && endTime != null -> {
      when {
        now.seconds < startTime.seconds -> TicketStatus.UPCOMING
        now.seconds in startTime.seconds..endTime.seconds -> TicketStatus.CURRENTLY
        else -> TicketStatus.EXPIRED // Fallback for edge cases
      }
    }
    else -> TicketStatus.UPCOMING
  }
}
