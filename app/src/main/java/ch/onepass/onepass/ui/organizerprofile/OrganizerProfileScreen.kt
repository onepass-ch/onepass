package ch.onepass.onepass.ui.organizerprofile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.myevents.TicketComponent
import ch.onepass.onepass.ui.myevents.TicketStatus
import ch.onepass.onepass.ui.theme.Typography
import ch.onepass.onepass.ui.theme.White

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
  const val TAB_SECTION = "organizer_tab_section"
  const val TAB_POSTS = "organizer_tab_posts"
  const val TAB_UPCOMING = "organizer_tab_upcoming"
  const val TAB_PAST = "organizer_tab_past"
  const val EVENT_LIST = "organizer_event_list"
}

// Extracted Components for better testability and modularity

@Composable
fun OrganizerHeaderSection(
    bannerImageRes: Int,
    profileImageRes: Int,
    modifier: Modifier = Modifier
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(-49.dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          modifier.width(392.dp).height(310.dp).testTag(OrganizerProfileTestTags.HEADER_SECTION)) {
        // Banner
        Image(
            painter = painterResource(id = bannerImageRes),
            contentDescription = "Organizer Banner",
            contentScale = ContentScale.FillBounds,
            modifier =
                Modifier.fillMaxWidth()
                    .height(261.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag(OrganizerProfileTestTags.BANNER_IMAGE))
        // Profile Picture
        Image(
            painter = painterResource(id = profileImageRes),
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.width(98.dp)
                    .height(98.dp)
                    .background(color = Color(0xFF262626), shape = RoundedCornerShape(50.dp))
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
            style = Typography.titleLarge.copy(color = White),
            modifier = Modifier.testTag(OrganizerProfileTestTags.NAME_TEXT))
        // Description
        Text(
            text = description,
            style =
                Typography.bodySmall.copy(color = Color(0xFFC4C4C4), textAlign = TextAlign.Center),
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
      modifier = modifier.width(30.dp).height(30.dp))
}

@Composable
fun SocialMediaSection(
    websiteUrl: String?,
    instagramUrl: String?,
    tiktokUrl: String?,
    facebookUrl: String?,
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
              modifier = Modifier.testTag(OrganizerProfileTestTags.WEBSITE_LINK)) {
                Image(
                    painter = painterResource(id = R.drawable.internet_logo),
                    contentDescription = "Website icon",
                    contentScale = ContentScale.None,
                    modifier = Modifier.width(20.dp).height(20.dp))
                Text(
                    text = "Website",
                    style =
                        Typography.bodyMedium.copy(
                            color = Color(0xFFA3A3A3), fontSize = 15.sp, lineHeight = 20.sp),
                    modifier = Modifier.testTag(OrganizerProfileTestTags.WEBSITE_TEXT))
                Image(
                    painter = painterResource(id = R.drawable.link_icon),
                    contentDescription = "External link",
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
                    contentDescription = "Instagram",
                    modifier = Modifier.testTag(OrganizerProfileTestTags.INSTAGRAM_ICON))
              }
              if (tiktokUrl != null) {
                SocialMediaIcon(
                    iconRes = R.drawable.tiktok_logo,
                    contentDescription = "TikTok",
                    modifier = Modifier.testTag(OrganizerProfileTestTags.TIKTOK_ICON))
              }
              if (facebookUrl != null) {
                SocialMediaIcon(
                    iconRes = R.drawable.facebook_logo,
                    contentDescription = "Facebook",
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
    onFollowClick: () -> Unit = {},
) {
  Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          modifier
              .fillMaxWidth()
              .wrapContentHeight()
              .padding(horizontal = 20.dp)
              .testTag(OrganizerProfileTestTags.FOLLOW_SECTION)) {
        // Follow Button
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.width(182.dp)
                    .height(42.dp)
                    .background(color = Color(0xFF4A3857), shape = RoundedCornerShape(size = 5.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF242424),
                        shape = RoundedCornerShape(size = 5.dp))
                    .testTag(OrganizerProfileTestTags.FOLLOW_BUTTON)) {
              Text(
                  text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                  style = Typography.titleMedium.copy(color = White),
                  modifier = Modifier.testTag(OrganizerProfileTestTags.FOLLOW_BUTTON_TEXT))
            }
        // Community Count
        Text(
            text = "join $followersCount community",
            style =
                Typography.titleMedium.copy(color = White, fontSize = 12.sp, lineHeight = 20.sp),
            modifier = Modifier.testTag(OrganizerProfileTestTags.FOLLOWERS_TEXT))
      }
}

@Composable
fun TabSection(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
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
        Text(
            text = "Posts",
            style = Typography.titleMedium.copy(color = White),
            modifier = Modifier.testTag(OrganizerProfileTestTags.TAB_POSTS))
        Text(
            text = "UPCOMING",
            style = Typography.titleMedium.copy(color = White),
            modifier = Modifier.testTag(OrganizerProfileTestTags.TAB_UPCOMING))
        Text(
            text = "PAST",
            style = Typography.titleMedium.copy(color = White),
            modifier = Modifier.testTag(OrganizerProfileTestTags.TAB_PAST))
      }
}

@Preview
@Composable
fun OrganizerProfileScreen(
    name: String = "Lausanne - best organizer",
    description: String =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed vitae nisi nec magna consequat tincidunt. Curabitur suscipit sem vel.",
    bannerImageRes: Int = R.drawable.image_fallback,
    profileImageRes: Int = R.drawable.ic_launcher_foreground,
    websiteUrl: String? = "https://example.com",
    instagramUrl: String? = "instagram",
    tiktokUrl: String? = "tiktok",
    facebookUrl: String? = "facebook",
    followersCount: String = "2.4k",
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {}
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          Modifier.fillMaxSize()
              .background(color = Color(0xFF1A1A1A)) // TODO change to theme
              .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 12.dp)
              .testTag(OrganizerProfileTestTags.SCREEN)) {
        // Header with banner and profile picture
        OrganizerHeaderSection(bannerImageRes = bannerImageRes, profileImageRes = profileImageRes)

        // Name and description
        OrganizerInfoSection(name = name, description = description)

        // Social media links
        SocialMediaSection(
            websiteUrl = websiteUrl,
            instagramUrl = instagramUrl,
            tiktokUrl = tiktokUrl,
            facebookUrl = facebookUrl)

        // Follow button and community count
        FollowSection(
            followersCount = followersCount,
            isFollowing = isFollowing,
            onFollowClick = onFollowClick)

        // Section separator
        Spacer(modifier = Modifier.height(50.dp))

        // Tab section
        TabSection()

        // Event list
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier.padding(top = 8.dp, bottom = 8.dp)
                    .testTag(OrganizerProfileTestTags.EVENT_LIST)) {
              TicketComponent(
                  title = "Lausanne Party",
                  status = TicketStatus.CURRENTLY,
                  dateTime = "Dec 15, 2024 • 9:00 PM",
                  location = "Lausanne, Flon")
            }
      }
}
