package ch.onepass.onepass.ui.organizer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Rule
import org.junit.Test

class BecomeOrganizerScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun formFieldsAreVisible() {
    composeTestRule.setContent { OnePassTheme { BecomeOrganizerScreen(ownerId = "user123") } }

    val tags =
        listOf(
            OrganizerTestTags.NAME_FIELD,
            OrganizerTestTags.DESCRIPTION_FIELD,
            OrganizerTestTags.EMAIL_FIELD,
            OrganizerTestTags.PHONE_FIELD,
            OrganizerTestTags.WEBSITE_FIELD,
            OrganizerTestTags.INSTAGRAM_FIELD,
            OrganizerTestTags.FACEBOOK_FIELD,
            OrganizerTestTags.TIKTOK_FIELD,
            OrganizerTestTags.ADDRESS_FIELD,
            OrganizerTestTags.SUBMIT_BUTTON)

    tags.forEach { tag -> composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed() }
  }

  @Test
  fun enteringTextUpdatesFields() {
    composeTestRule.setContent { OnePassTheme { BecomeOrganizerScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(OrganizerTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Test Org")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.DESCRIPTION_FIELD)
        .performScrollTo()
        .performTextInput("Cool description")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.EMAIL_FIELD)
        .performScrollTo()
        .performTextInput("test@email.com")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.WEBSITE_FIELD)
        .performScrollTo()
        .performTextInput("example.com")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.INSTAGRAM_FIELD)
        .performScrollTo()
        .performTextInput("test_insta")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.FACEBOOK_FIELD)
        .performScrollTo()
        .performTextInput("test_fb")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.TIKTOK_FIELD)
        .performScrollTo()
        .performTextInput("test_tiktok")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.ADDRESS_FIELD)
        .performScrollTo()
        .performTextInput("123 Test Street")

    composeTestRule
        .onNodeWithTag(OrganizerTestTags.PREFIX_DROPDOWN)
        .performScrollTo()
        .performClick()
    composeTestRule.onNode(hasText("+1 American Samoa")).performScrollTo().performClick()

    composeTestRule
        .onNodeWithTag(OrganizerTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("123456789")

    composeTestRule.onNodeWithTag(OrganizerTestTags.NAME_FIELD).assertTextContains("Test Org")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.DESCRIPTION_FIELD)
        .assertTextContains("Cool description")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.EMAIL_FIELD)
        .assertTextContains("test@email.com")
    composeTestRule.onNodeWithTag(OrganizerTestTags.PHONE_FIELD).assertTextContains("123456789")
    composeTestRule.onNodeWithTag(OrganizerTestTags.WEBSITE_FIELD).assertTextContains("example.com")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.INSTAGRAM_FIELD)
        .assertTextContains("test_insta")
    composeTestRule.onNodeWithTag(OrganizerTestTags.FACEBOOK_FIELD).assertTextContains("test_fb")
    composeTestRule.onNodeWithTag(OrganizerTestTags.TIKTOK_FIELD).assertTextContains("test_tiktok")
    composeTestRule
        .onNodeWithTag(OrganizerTestTags.ADDRESS_FIELD)
        .assertTextContains("123 Test Street")
  }

  @Test
  fun submitButtonShowsErrorOnEmptyRequiredFields() {
    composeTestRule.setContent { OnePassTheme { BecomeOrganizerScreen(ownerId = "user123") } }

    composeTestRule.onNodeWithTag(OrganizerTestTags.SUBMIT_BUTTON).performScrollTo().performClick()

    composeTestRule.onNodeWithText("Please fix errors").assertIsDisplayed()
  }

  @Test
  fun canSelectCountryPrefix() {
    composeTestRule.setContent { OnePassTheme { BecomeOrganizerScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(OrganizerTestTags.PREFIX_DROPDOWN)
        .performScrollTo()
        .performClick()

    composeTestRule
        .onNode(hasText("+1 American Samoa", substring = true))
        .assertIsDisplayed()
        .performClick()
  }
}
