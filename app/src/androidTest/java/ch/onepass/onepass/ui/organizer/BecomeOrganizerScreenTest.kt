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
            BecomeOrganizerTestTags.NAME_FIELD,
            BecomeOrganizerTestTags.DESCRIPTION_FIELD,
            BecomeOrganizerTestTags.EMAIL_FIELD,
            BecomeOrganizerTestTags.PHONE_FIELD,
            BecomeOrganizerTestTags.WEBSITE_FIELD,
            BecomeOrganizerTestTags.INSTAGRAM_FIELD,
            BecomeOrganizerTestTags.FACEBOOK_FIELD,
            BecomeOrganizerTestTags.TIKTOK_FIELD,
            BecomeOrganizerTestTags.ADDRESS_FIELD,
            BecomeOrganizerTestTags.SUBMIT_BUTTON)

    tags.forEach { tag -> composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed() }
  }

  @Test
  fun enteringTextUpdatesFields() {
    composeTestRule.setContent { OnePassTheme { BecomeOrganizerScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Test Org")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.DESCRIPTION_FIELD)
        .performScrollTo()
        .performTextInput("Cool description")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.EMAIL_FIELD)
        .performScrollTo()
        .performTextInput("test@email.com")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.WEBSITE_FIELD)
        .performScrollTo()
        .performTextInput("example.com")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.INSTAGRAM_FIELD)
        .performScrollTo()
        .performTextInput("test_insta")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.FACEBOOK_FIELD)
        .performScrollTo()
        .performTextInput("test_fb")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.TIKTOK_FIELD)
        .performScrollTo()
        .performTextInput("test_tiktok")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.ADDRESS_FIELD)
        .performScrollTo()
        .performTextInput("123 Test Street")

    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.PREFIX_DROPDOWN)
        .performScrollTo()
        .performClick()
    composeTestRule.onNode(hasText("+1 American Samoa")).performScrollTo().performClick()

    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("123456789")

    composeTestRule.onNodeWithTag(BecomeOrganizerTestTags.NAME_FIELD).assertTextContains("Test Org")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.DESCRIPTION_FIELD)
        .assertTextContains("Cool description")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.EMAIL_FIELD)
        .assertTextContains("test@email.com")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.PHONE_FIELD)
        .assertTextContains("123456789")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.WEBSITE_FIELD)
        .assertTextContains("example.com")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.INSTAGRAM_FIELD)
        .assertTextContains("test_insta")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.FACEBOOK_FIELD)
        .assertTextContains("test_fb")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.TIKTOK_FIELD)
        .assertTextContains("test_tiktok")
    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.ADDRESS_FIELD)
        .assertTextContains("123 Test Street")
  }

  @Test
  fun submitButtonShowsErrorOnEmptyRequiredFields() {
    composeTestRule.setContent { OnePassTheme { BecomeOrganizerScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.SUBMIT_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.onNodeWithText("Please fix errors").assertIsDisplayed()
  }

  @Test
  fun canSelectCountryPrefix() {
    composeTestRule.setContent { OnePassTheme { BecomeOrganizerScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(BecomeOrganizerTestTags.PREFIX_DROPDOWN)
        .performScrollTo()
        .performClick()

    composeTestRule
        .onNode(hasText("+1 American Samoa", substring = true))
        .assertIsDisplayed()
        .performClick()
  }
}
