package ch.onepass.onepass.ui.organization

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Assert
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
      OnePassTheme {
        FollowSection(followersCount = followersCount, isFollowing = false, isOwner = false)
      }
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
      OnePassTheme {
        FollowSection(followersCount = followersCount, isFollowing = true, isOwner = false)
      }
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

  // ========================================
  // Tests for Edit Organization Button
  // ========================================

  @Test
  fun followSection_ownerUser_displaysEditOrganizationButton() {
    val followersCount = "2.4k"

    composeTestRule.setContent {
      OnePassTheme {
        FollowSection(
            followersCount = followersCount,
            isFollowing = false,
            isOwner = true,
            onFollowClick = {},
            onEditClick = {})
      }
    }

    // Verify follow section is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_SECTION).assertIsDisplayed()

    // Verify follow button is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON).assertIsDisplayed()

    // Verify edit organization button is displayed for owner
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.EDIT_ORGANIZATION_BUTTON)
        .assertIsDisplayed()
        .assertTextEquals("EDIT ORGANIZATION")
  }

  @Test
  fun followSection_nonOwnerUser_hidesEditOrganizationButton() {
    val followersCount = "1.5k"

    composeTestRule.setContent {
      OnePassTheme {
        FollowSection(
            followersCount = followersCount,
            isFollowing = false,
            isOwner = false,
            onFollowClick = {},
            onEditClick = {})
      }
    }

    // Verify follow section is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_SECTION).assertIsDisplayed()

    // Verify follow button is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON).assertIsDisplayed()

    // Verify edit organization button is NOT displayed for non-owner
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.EDIT_ORGANIZATION_BUTTON)
        .assertDoesNotExist()
  }

  @Test
  fun followSection_ownerAndFollowing_displaysBothButtons() {
    val followersCount = "5.7k"

    composeTestRule.setContent {
      OnePassTheme {
        FollowSection(
            followersCount = followersCount,
            isFollowing = true,
            isOwner = true,
            onFollowClick = {},
            onEditClick = {})
      }
    }

    // Verify both buttons are displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.EDIT_ORGANIZATION_BUTTON)
        .assertIsDisplayed()

    // Verify follow button shows "FOLLOWING"
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON_TEXT, useUnmergedTree = true)
        .assertTextEquals("FOLLOWING")
  }

  @Test
  fun followSection_ownerClickHandling_callsCorrectCallbacks() {
    var followClicked = false
    var editClicked = false

    composeTestRule.setContent {
      OnePassTheme {
        FollowSection(
            followersCount = "1k",
            isFollowing = false,
            isOwner = true,
            onFollowClick = { followClicked = true },
            onEditClick = { editClicked = true })
      }
    }

    // Click follow button
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_BUTTON).performClick()
    composeTestRule.waitForIdle()
    Assert.assertTrue("Follow callback should be called", followClicked)

    // Reset and click edit button
    followClicked = false
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.EDIT_ORGANIZATION_BUTTON).performClick()
    composeTestRule.waitForIdle()
    Assert.assertTrue("Edit callback should be called", editClicked)
    Assert.assertFalse("Follow callback should not be called", followClicked)
  }

  @Test
  fun organizerProfileContent_ownerUser_displaysEditButton() {
    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileContent(
            name = "Test Owner Organizer",
            description = "Test description",
            followersCount = "2.4k",
            isFollowing = false,
            isOwner = true,
            selectedTab = OrganizerProfileTab.UPCOMING)
      }
    }

    // Verify main screen is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.SCREEN).assertIsDisplayed()

    // Verify follow section is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_SECTION).assertIsDisplayed()

    // Verify edit organization button is displayed
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.EDIT_ORGANIZATION_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun organizerProfileContent_nonOwnerUser_hidesEditButton() {
    composeTestRule.setContent {
      OnePassTheme {
        OrganizerProfileContent(
            name = "Test Non-Owner Organizer",
            description = "Test description",
            followersCount = "1.2k",
            isFollowing = false,
            isOwner = false,
            selectedTab = OrganizerProfileTab.UPCOMING)
      }
    }

    // Verify main screen is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.SCREEN).assertIsDisplayed()

    // Verify follow section is displayed
    composeTestRule.onNodeWithTag(OrganizerProfileTestTags.FOLLOW_SECTION).assertIsDisplayed()

    // Verify edit organization button is NOT displayed
    composeTestRule
        .onNodeWithTag(OrganizerProfileTestTags.EDIT_ORGANIZATION_BUTTON)
        .assertDoesNotExist()
  }
}
