package ch.onepass.onepass.ui.organization

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.ui.myevents.MyEventsTestTags
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import java.util.Date
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive tests for the OrganizerProfileScreen and its components. Tests verify that all UI
 * elements are displayed correctly.
 */
@RunWith(AndroidJUnit4::class)
class OrganizerProfileScreenComposeTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun organizerProfileContent_displaysCorrectly() {
    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileContent(
            name = "Test Organizer",
            description = "Test description for organizer",
            bannerImageRes = R.drawable.image_fallback,
            profileImageRes = R.drawable.ic_launcher_foreground,
            websiteUrl = "https://example.com",
            instagramUrl = "instagram",
            tiktokUrl = "tiktok",
            facebookUrl = "facebook",
            followersCount = "2.4k",
            isFollowing = false)
      }
    }

    // Verify main screen is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.SCREEN).assertIsDisplayed()
  }

  @Test
  fun headerSection_displaysImages() {
    composeTestRule.setContent {
      OnePassTheme {
        OrganizerHeaderSection(
            bannerImageRes = R.drawable.image_fallback,
            profileImageRes = R.drawable.ic_launcher_foreground)
      }
    }

    // Verify header section and images are displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.HEADER_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.BANNER_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.PROFILE_IMAGE).assertIsDisplayed()
  }

  @Test
  fun infoSection_displaysNameAndDescription() {
    val testName = "Test Organizer Name"
    val testDescription = "This is a test description for the organizer"

    composeTestRule.setContent {
      OnePassTheme { OrganizerInfoSection(name = testName, description = testDescription) }
    }

    // Verify info section is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INFO_SECTION).assertIsDisplayed()

    // Verify name is displayed with correct text
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.NAME_TEXT)
        .assertIsDisplayed()
        .assertTextEquals(testName)

    // Verify description is displayed with correct text
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.DESCRIPTION_TEXT)
        .assertIsDisplayed()
        .assertTextEquals(testDescription)
  }

  @Test
  fun socialMediaSection_displaysAllSocialMediaIcons() {
    composeTestRule.setContent {
      OnePassTheme {
        SocialMediaSection(
            websiteUrl = "https://example.com",
            instagramUrl = "instagram",
            tiktokUrl = "tiktok",
            facebookUrl = "facebook")
      }
    }

    // Wait for layout to complete
    composeTestRule.waitForIdle()

    // Verify social media section is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.SOCIAL_MEDIA_SECTION).assertIsDisplayed()

    // Verify website link is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).assertExists()
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.WEBSITE_TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("Website")

    // Verify all social media icons are displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).assertExists()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TIKTOK_ICON).assertExists()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FACEBOOK_ICON).assertExists()
  }

  @Test
  fun socialMediaSection_hidesNullSocialMedia() {
    composeTestRule.setContent {
      OnePassTheme {
        SocialMediaSection(
            websiteUrl = null, instagramUrl = null, tiktokUrl = "tiktok", facebookUrl = null)
      }
    }

    composeTestRule.waitForIdle()

    // Verify social media section is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.SOCIAL_MEDIA_SECTION).assertIsDisplayed()

    // Verify website link is NOT displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).assertDoesNotExist()

    // Verify only TikTok icon is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TIKTOK_ICON).assertExists()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FACEBOOK_ICON).assertDoesNotExist()
  }

  @Test
  fun socialMediaSection_allNullSocialMedia_displaysSection() {
    composeTestRule.setContent {
      OnePassTheme {
        SocialMediaSection(
            websiteUrl = null, instagramUrl = null, tiktokUrl = null, facebookUrl = null)
      }
    }

    // Verify social media section STILL EXISTS even when all are null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.SOCIAL_MEDIA_SECTION).assertExists()

    // Verify no social media icons are displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TIKTOK_ICON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FACEBOOK_ICON).assertDoesNotExist()
  }

  @Test
  fun socialMediaSection_websiteOnly_displaysWebsite() {
    composeTestRule.setContent {
      OnePassTheme {
        SocialMediaSection(
            websiteUrl = "https://example.com",
            instagramUrl = null,
            tiktokUrl = null,
            facebookUrl = null)
      }
    }

    composeTestRule.waitForIdle()

    // Verify website link is displayed when websiteUrl is non-null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).assertExists()
    // Verify social media icons are not displayed when URLs are null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TIKTOK_ICON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FACEBOOK_ICON).assertDoesNotExist()
  }

  @Test
  fun socialMediaSection_instagramOnly_displaysInstagram() {
    composeTestRule.setContent {
      OnePassTheme {
        SocialMediaSection(
            websiteUrl = null, instagramUrl = "instagram", tiktokUrl = null, facebookUrl = null)
      }
    }

    composeTestRule.waitForIdle()

    // Verify instagram icon is displayed when instagramUrl is non-null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).assertExists()
    // Verify other icons are not displayed when URLs are null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TIKTOK_ICON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FACEBOOK_ICON).assertDoesNotExist()
  }

  @Test
  fun socialMediaSection_tiktokOnly_displaysTiktok() {
    composeTestRule.setContent {
      OnePassTheme {
        SocialMediaSection(
            websiteUrl = null, instagramUrl = null, tiktokUrl = "tiktok", facebookUrl = null)
      }
    }

    composeTestRule.waitForIdle()

    // Verify tiktok icon is displayed when tiktokUrl is non-null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TIKTOK_ICON).assertExists()
    // Verify other icons are not displayed when URLs are null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FACEBOOK_ICON).assertDoesNotExist()
  }

  @Test
  fun socialMediaSection_facebookOnly_displaysFacebook() {
    composeTestRule.setContent {
      OnePassTheme {
        SocialMediaSection(
            websiteUrl = null, instagramUrl = null, tiktokUrl = null, facebookUrl = "facebook")
      }
    }

    composeTestRule.waitForIdle()

    // Verify facebook icon is displayed when facebookUrl is non-null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FACEBOOK_ICON).assertExists()
    // Verify other icons are not displayed when URLs are null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TIKTOK_ICON).assertDoesNotExist()
  }

  @Test
  fun socialMediaSection_websiteNull_othersNonNull_websiteHidden() {
    composeTestRule.setContent {
      OnePassTheme {
        SocialMediaSection(
            websiteUrl = null,
            instagramUrl = "instagram",
            tiktokUrl = "tiktok",
            facebookUrl = "facebook")
      }
    }

    composeTestRule.waitForIdle()

    // Verify website link is NOT displayed when websiteUrl is null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).assertDoesNotExist()
    // Verify social media icons are displayed when URLs are non-null
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).assertExists()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TIKTOK_ICON).assertExists()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FACEBOOK_ICON).assertExists()
  }

  @Test
  fun followSection_displaysFollowButton_notFollowing() {
    val followersCount = "1.5k"

    composeTestRule.setContent {
      OnePassTheme { FollowSection(followersCount = followersCount, isFollowing = false) }
    }

    // Verify follow section is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_SECTION).assertIsDisplayed()

    // Verify follow button is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON).assertIsDisplayed()

    // Verify follow button text shows "FOLLOW"
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON_TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("FOLLOW")

    // Verify followers count is displayed
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.FOLLOWERS_TEXT)
        .assertIsDisplayed()
        .assertTextEquals("join $followersCount community")
  }

  @Test
  fun followSection_displaysFollowingButton_isFollowing() {
    val followersCount = "3.2k"

    composeTestRule.setContent {
      OnePassTheme { FollowSection(followersCount = followersCount, isFollowing = true) }
    }

    // Verify follow button text shows "FOLLOWING"
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON_TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("FOLLOWING")

    // Verify followers count is displayed
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.FOLLOWERS_TEXT)
        .assertIsDisplayed()
        .assertTextEquals("join $followersCount community")
  }

  @Test
  fun tabSection_displaysAllTabs() {
    composeTestRule.setContent {
      OnePassTheme { TabSection(selectedTab = OrganizerProfileTab.UPCOMING) }
    }

    // Verify tab section is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TAB_SECTION).assertIsDisplayed()

    // Verify all tabs are displayed
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.TAB_POSTS)
        .assertIsDisplayed()
        .assertTextEquals("Posts")

    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.TAB_UPCOMING)
        .assertIsDisplayed()
        .assertTextEquals("UPCOMING")

    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.TAB_PAST)
        .assertIsDisplayed()
        .assertTextEquals("PAST")
  }

  @Test
  fun tabSection_postsTabSelected_displaysCorrectly() {
    composeTestRule.setContent {
      OnePassTheme { TabSection(selectedTab = OrganizerProfileTab.POSTS) }
    }

    // Verify Posts tab is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TAB_POSTS).assertIsDisplayed()
  }

  @Test
  fun tabSection_pastTabSelected_displaysCorrectly() {
    composeTestRule.setContent {
      OnePassTheme { TabSection(selectedTab = OrganizerProfileTab.PAST) }
    }

    // Verify Past tab is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TAB_PAST).assertIsDisplayed()
  }

  @Test
  fun organizerProfileContent_displaysAllSections() {
    val testName = "Full Test Organizer"
    val testDescription = "Complete organizer description"
    val followersCount = "5.7k"

    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileContent(
            name = testName,
            description = testDescription,
            bannerImageRes = R.drawable.image_fallback,
            profileImageRes = R.drawable.ic_launcher_foreground,
            websiteUrl = "https://example.com",
            instagramUrl = "instagram",
            tiktokUrl = "tiktok",
            facebookUrl = "facebook",
            followersCount = followersCount,
            isFollowing = false,
            selectedTab = OrganizerProfileTab.UPCOMING)
      }
    }

    // Verify main screen
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.SCREEN).assertIsDisplayed()

    // Verify header section
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.HEADER_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.BANNER_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.PROFILE_IMAGE).assertIsDisplayed()

    // Verify info section
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INFO_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.NAME_TEXT).assertTextEquals(testName)
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.DESCRIPTION_TEXT)
        .assertTextEquals(testDescription)

    // Verify social media section
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.SOCIAL_MEDIA_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TIKTOK_ICON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FACEBOOK_ICON).assertIsDisplayed()

    // Verify follow section
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON_TEXT, useUnmergedTree = true)
        .assertTextEquals("FOLLOW")
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.FOLLOWERS_TEXT)
        .assertTextEquals("join $followersCount community")

    // Verify tab section
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TAB_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TAB_POSTS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TAB_UPCOMING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TAB_PAST).assertIsDisplayed()

    // Verify event list section
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.EVENT_LIST).assertIsDisplayed()
  }

  @Test
  fun organizerProfileContent_displaysCorrectTextContent() {
    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileContent(
            name = "Lausanne - best organizer",
            description = "Test organizer description",
            followersCount = "2.4k",
            isFollowing = false,
            selectedTab = OrganizerProfileTab.UPCOMING)
      }
    }

    // Verify text content is displayed
    composeTestRule.onNodeWithText("Lausanne - best organizer").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test organizer description").assertIsDisplayed()
    composeTestRule.onNodeWithText("Website").assertIsDisplayed()
    composeTestRule.onNodeWithText("FOLLOW").assertIsDisplayed()
    composeTestRule.onNodeWithText("join 2.4k community").assertIsDisplayed()
    composeTestRule.onNodeWithText("Posts").assertIsDisplayed()
    composeTestRule.onNodeWithText("UPCOMING").assertIsDisplayed()
    composeTestRule.onNodeWithText("PAST").assertIsDisplayed()
  }

  @Test
  fun organizerProfileContent_minimalData_displaysCorrectly() {
    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileContent(
            name = "Minimal Organizer",
            description = "Short desc",
            websiteUrl = null,
            instagramUrl = null,
            tiktokUrl = null,
            facebookUrl = null,
            followersCount = "0",
            isFollowing = false,
            selectedTab = OrganizerProfileTab.UPCOMING)
      }
    }

    // Verify main screen is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.SCREEN).assertIsDisplayed()

    // Verify basic info is displayed
    composeTestRule.onNodeWithText("Minimal Organizer").assertIsDisplayed()
    composeTestRule.onNodeWithText("Short desc").assertIsDisplayed()

    // Verify follow button shows correct text
    composeTestRule.onNodeWithText("FOLLOW").assertIsDisplayed()
    composeTestRule.onNodeWithText("join 0 community").assertIsDisplayed()

    // Verify no social media icons are displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TIKTOK_ICON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FACEBOOK_ICON).assertDoesNotExist()
  }

  @Test
  fun postsTabContent_displaysEmptyState() {
    composeTestRule.setContent { OnePassTheme { PostsTabContent() } }

    // Verify event list is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.EVENT_LIST).assertIsDisplayed()

    // Verify empty state message is displayed
    composeTestRule.onNodeWithText("No posts yet").assertIsDisplayed()
  }

  @Test
  fun upcomingTabContent_displaysEmptyState() {
    composeTestRule.setContent { OnePassTheme { UpcomingTabContent(events = emptyList()) } }

    // Verify event list is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.EVENT_LIST).assertIsDisplayed()

    // Verify empty state message is displayed
    composeTestRule.onNodeWithText("No upcoming events").assertIsDisplayed()
  }

  @Test
  fun pastTabContent_displaysEmptyState() {
    composeTestRule.setContent { OnePassTheme { PastTabContent(events = emptyList()) } }

    // Verify event list is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.EVENT_LIST).assertIsDisplayed()

    // Verify empty state message is displayed
    composeTestRule.onNodeWithText("No past events").assertIsDisplayed()
  }

  @Test
  fun organizerProfileScreen_loadsData_displaysUpcomingEvents() {
    val organizationId = "org-profile"
    val organization =
        createTestOrganization(
            id = organizationId,
            name = "Compose Organizer",
            description = "Compose description",
            followerCount = 1530,
            website = "https://compose.example",
            instagram = "https://instagram.com/compose",
            tiktok = "https://tiktok.com/@compose",
            facebook = "https://facebook.com/compose")
    val events =
        listOf(
            createEvent(
                id = "future-event",
                title = "Future Event",
                organizerId = organizationId,
                startHoursFromNow = 4,
                endHoursFromNow = 6),
            createEvent(
                id = "current-event",
                title = "Current Event",
                organizerId = organizationId,
                startHoursFromNow = -1,
                endHoursFromNow = 1),
            createEvent(
                id = "past-event",
                title = "Past Event",
                organizerId = organizationId,
                startHoursFromNow = -5,
                endHoursFromNow = -3))
    val viewModel = MockOrganizerProfileViewModel(organization = organization, events = events)

    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileScreen(organizationId = organizationId, viewModel = viewModel)
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) { !viewModel.state.value.loading }

    assertTrue(viewModel.requestedOrganizationIds.contains(organizationId))
    assertTrue(viewModel.requestedEventOrganizationIds.contains(organizationId))

    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.NAME_TEXT)
        .assertIsDisplayed()
        .assertTextEquals(organization.name)
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.DESCRIPTION_TEXT)
        .assertIsDisplayed()
        .assertTextEquals(organization.description)

    composeTestRule.onNodeWithText("Future Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Current Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Past Event").assertDoesNotExist()
    composeTestRule.onAllNodesWithTag(MyEventsTestTags.TICKET_CARD).assertCountEquals(2)
  }

  @Test
  fun organizerProfileScreen_followButton_togglesFollowState() {
    val organizationId = "org-follow"
    val organization =
        createTestOrganization(
            id = organizationId,
            name = "Follow Org",
            description = "Follow description",
            followerCount = 9)
    val viewModel = MockOrganizerProfileViewModel(organization = organization, events = emptyList())

    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileScreen(organizationId = organizationId, viewModel = viewModel)
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) { !viewModel.state.value.loading }

    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON_TEXT, useUnmergedTree = true)
        .assertTextEquals("FOLLOW")

    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON).performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) { viewModel.state.value.isFollowing }

    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON_TEXT, useUnmergedTree = true)
        .assertTextEquals("FOLLOWING")
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.FOLLOWERS_TEXT)
        .assertTextEquals("join 10 community")

    assertTrue(viewModel.state.value.isFollowing)
  }

  @Test
  fun organizerProfileScreen_switchToPastTab_showsPastEventsOnly() {
    val organizationId = "org-tabs"
    val organization =
        createTestOrganization(
            id = organizationId,
            name = "Tabs Org",
            description = "Tabs description",
            followerCount = 42)
    val events =
        listOf(
            createEvent(
                id = "future-event",
                title = "Future Event",
                organizerId = organizationId,
                startHoursFromNow = 2,
                endHoursFromNow = 3),
            createEvent(
                id = "past-event",
                title = "Past Event",
                organizerId = organizationId,
                startHoursFromNow = -6,
                endHoursFromNow = -5))
    val viewModel = MockOrganizerProfileViewModel(organization = organization, events = events)

    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileScreen(organizationId = organizationId, viewModel = viewModel)
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) { !viewModel.state.value.loading }

    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.TAB_PAST).performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      viewModel.state.value.selectedTab == OrganizerProfileTab.PAST
    }

    composeTestRule.onNodeWithText("Past Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Future Event").assertDoesNotExist()
    composeTestRule.onAllNodesWithTag(OrganizerProfileTestTags.EVENT_LIST).assertCountEquals(1)
  }

  @Test
  fun organizerProfileScreen_clickWebsite_emitsOpenWebsiteEffect() {
    val organizationId = "org-website"
    val organization =
        createTestOrganization(
            id = organizationId,
            name = "Website Org",
            description = "Website description",
            followerCount = 50,
            website = "https://website.example")
    val viewModel = MockOrganizerProfileViewModel(organization = organization, events = emptyList())

    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileScreen(organizationId = organizationId, viewModel = viewModel)
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) { !viewModel.state.value.loading }

    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.WEBSITE_LINK).performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      viewModel.recordedEffects.any { it is OrganizerProfileEffect.OpenWebsite }
    }

    val effect = viewModel.recordedEffects.last() as OrganizerProfileEffect.OpenWebsite
    assertEquals(organization.website, effect.url)
  }

  @Test
  fun organizerProfileScreen_clickSocialMedia_emitsOpenSocialMediaEffect() {
    val organizationId = "org-social"
    val organization =
        createTestOrganization(
            id = organizationId,
            name = "Social Org",
            description = "Social description",
            followerCount = 77,
            instagram = "https://instagram.com/social")
    val viewModel = MockOrganizerProfileViewModel(organization = organization, events = emptyList())

    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileScreen(organizationId = organizationId, viewModel = viewModel)
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) { !viewModel.state.value.loading }

    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.INSTAGRAM_ICON).performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      viewModel.recordedEffects.any {
        it is OrganizerProfileEffect.OpenSocialMedia && it.platform == "instagram"
      }
    }

    val effect = viewModel.recordedEffects.last() as OrganizerProfileEffect.OpenSocialMedia
    assertEquals("instagram", effect.platform)
    assertEquals(organization.instagram, effect.url)
  }

  @Test
  fun organizerProfileScreen_clickEvent_emitsNavigateEffect() {
    val organizationId = "org-events"
    val organization =
        createTestOrganization(
            id = organizationId,
            name = "Events Org",
            description = "Events description",
            followerCount = 15)
    val events =
        listOf(
            createEvent(
                id = "event-navigate",
                title = "Navigate Event",
                organizerId = organizationId,
                startHoursFromNow = 1,
                endHoursFromNow = 2))
    val viewModel = MockOrganizerProfileViewModel(organization = organization, events = events)

    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileScreen(organizationId = organizationId, viewModel = viewModel)
      }
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) { !viewModel.state.value.loading }

    composeTestRule.onNodeWithTag(MyEventsTestTags.TICKET_CARD).performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      viewModel.recordedEffects.any { it is OrganizerProfileEffect.NavigateToEvent }
    }

    val effect = viewModel.recordedEffects.last() as OrganizerProfileEffect.NavigateToEvent
    assertEquals("event-navigate", effect.eventId)
  }

  private fun createTestOrganization(
      id: String,
      name: String,
      description: String,
      followerCount: Int,
      website: String? = "https://example.com",
      instagram: String? = "https://instagram.com/example",
      tiktok: String? = "https://tiktok.com/@example",
      facebook: String? = "https://facebook.com/example"
  ): Organization =
      Organization(
          id = id,
          name = name,
          description = description,
          ownerId = "owner-$id",
          status = OrganizationStatus.ACTIVE,
          verified = true,
          website = website,
          instagram = instagram,
          tiktok = tiktok,
          facebook = facebook,
          followerCount = followerCount,
          eventIds = listOf("future-event", "current-event", "past-event"))

  private fun createEvent(
      id: String,
      title: String,
      organizerId: String,
      startHoursFromNow: Long,
      endHoursFromNow: Long
  ): Event {
    val now = System.currentTimeMillis()
    val startMillis = now + TimeUnit.HOURS.toMillis(startHoursFromNow)
    val endMillis = now + TimeUnit.HOURS.toMillis(endHoursFromNow)
    return Event(
        eventId = id,
        title = title,
        description = "$title description",
        organizerId = organizerId,
        organizerName = "Organizer $organizerId",
        status = EventStatus.PUBLISHED,
        startTime = Timestamp(Date(startMillis)),
        endTime = Timestamp(Date(endMillis)))
  }
}
