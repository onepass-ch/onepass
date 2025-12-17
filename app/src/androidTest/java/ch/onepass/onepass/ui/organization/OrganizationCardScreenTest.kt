package ch.onepass.onepass.ui.organization

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import ch.onepass.onepass.R
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import org.junit.Rule
import org.junit.Test

class OrganizationCardTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  @Test
  fun displaysOrganizationName_correctly() {
    val organization =
        Organization(
            id = "org-1",
            name = "Balelec",
            description = "Festival",
            verified = true,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule.onNodeWithText("Balelec").assertExists().assertIsDisplayed()
  }

  @Test
  fun showsVerifiedBadge_forVerifiedOrganization() {
    val organization =
        Organization(
            id = "org-1",
            name = "Verified Org",
            description = "Description",
            verified = true,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_VERIFIED_BADGE)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun hidesVerifiedBadge_forUnverifiedOrganization() {
    val organization =
        Organization(
            id = "org-1",
            name = "Unverified Org",
            description = "Description",
            verified = false,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_VERIFIED_BADGE)
        .assertDoesNotExist()
  }

  @Test
  fun displaysDescription_correctly() {
    val organization =
        Organization(
            id = "org-1",
            name = "Test Org",
            description = "This is a test description",
            verified = true,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule.onNodeWithText("This is a test description").assertExists().assertIsDisplayed()
  }

  @Test
  fun displaysProfileImage_withTestTag() {
    val organization =
        Organization(
            id = "org-1",
            name = "Test Org",
            description = "Description",
            profileImageUrl = "https://example.com/image.jpg",
            verified = true,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_IMAGE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun displaysFollowerCount_correctly() {
    val organization =
        Organization(
            id = "org-1",
            name = "Popular Org",
            description = "Description",
            verified = true,
            followerCount = 1500,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_FOLLOWER_COUNT)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("1.5K", substring = true).assertExists()
  }

  @Test
  fun formatsFollowerCount_inThousands() {
    val organization =
        Organization(
            id = "org-1",
            name = "Test Org",
            description = "Description",
            verified = true,
            followerCount = 5000,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule.onNodeWithText("5K", substring = true).assertExists()
  }

  @Test
  fun displaysRating_whenRatingGreaterThanZero() {
    val organization =
        Organization(
            id = "org-1",
            name = "Rated Org",
            description = "Description",
            verified = true,
            averageRating = 4.5f,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_RATING)
        .assertExists()
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("4.5", substring = true).assertExists()
  }

  @Test
  fun hidesRating_whenRatingIsZero() {
    val organization =
        Organization(
            id = "org-1",
            name = "Unrated Org",
            description = "Description",
            verified = true,
            averageRating = 0f,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule.onNodeWithTag(OrganizationCardTestTags.ORGANIZER_RATING).assertDoesNotExist()
  }

  @Test
  fun displaysCreatedDate_correctly() {
    val timestamp = Timestamp.now()
    val organization =
        Organization(
            id = "org-1",
            name = "Test Org",
            description = "Description",
            verified = true,
            createdAt = timestamp,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_CREATED_DATE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Use string resource for "since"
    composeTestRule
        .onNodeWithText(
            context.getString(R.string.organization_card_since_label, ""), substring = true)
        .assertExists()
  }

  @Test
  fun callsOnClick_whenCardClicked() {
    val organization =
        Organization(
            id = "org-1",
            name = "Clickable Org",
            description = "Description",
            verified = true,
            status = OrganizationStatus.ACTIVE)
    var clicked = false
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = { clicked = true }) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.getTestTagForOrganizationCard("org-1"))
        .performClick()
    assert(clicked) { "onClick callback was not invoked" }
  }

  @Test
  fun hasCorrectTestTag_basedOnOrganizationId() {
    val organization =
        Organization(
            id = "test-org-123",
            name = "Test Org",
            description = "Description",
            verified = true,
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule.onNodeWithTag("organizerCard_test-org-123").assertExists()
  }

  @Test
  fun displaysAllComponents_forCompleteOrganization() {
    val organization =
        Organization(
            id = "org-complete",
            name = "Complete Org",
            description = "A complete organization with all fields",
            profileImageUrl = "https://example.com/image.jpg",
            verified = true,
            followerCount = 10500,
            averageRating = 4.8f,
            createdAt = Timestamp.now(),
            status = OrganizationStatus.ACTIVE)
    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_IMAGE, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_NAME, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_VERIFIED_BADGE, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_DESCRIPTION, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_STATS_ROW, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_FOLLOWER_COUNT, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_RATING, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(OrganizationCardTestTags.ORGANIZER_CREATED_DATE, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun handlesNullData() {
    val organization =
        Organization(
            id = "org-null",
            name = "Maybe Null Org",
            description = "Test null fields",
            profileImageUrl = null, // null image
            createdAt = null, // null date
            averageRating = 0f, // rating hidden
            followerCount = 0, // should show "0"
            status = OrganizationStatus.ACTIVE,
            verified = false)

    composeTestRule.setContent {
      OnePassTheme { OrganizationCard(organization = organization, onClick = {}) }
    }

    // Verify it doesn't crash and renders key values
    composeTestRule.onNodeWithText("Maybe Null Org").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizationCardTestTags.ORGANIZER_RATING).assertDoesNotExist()
    composeTestRule.onNodeWithText("0").assertExists()
  }
}
