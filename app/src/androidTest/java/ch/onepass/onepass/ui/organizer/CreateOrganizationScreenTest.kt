package ch.onepass.onepass.ui.organizer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Rule
import org.junit.Test

class CreateOrganizationScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun formFieldsAreVisible() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    val tags =
        listOf(
            CreateOrganizationTestTags.NAME_FIELD,
            CreateOrganizationTestTags.DESCRIPTION_FIELD,
            CreateOrganizationTestTags.EMAIL_FIELD,
            CreateOrganizationTestTags.PHONE_FIELD,
            CreateOrganizationTestTags.WEBSITE_FIELD,
            CreateOrganizationTestTags.INSTAGRAM_FIELD,
            CreateOrganizationTestTags.FACEBOOK_FIELD,
            CreateOrganizationTestTags.TIKTOK_FIELD,
            CreateOrganizationTestTags.ADDRESS_FIELD,
            CreateOrganizationTestTags.SUBMIT_BUTTON)

    tags.forEach { tag -> composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed() }
  }

  @Test
  fun enteringTextUpdatesFields() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Test Org")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.DESCRIPTION_FIELD)
        .performScrollTo()
        .performTextInput("Cool description")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.EMAIL_FIELD)
        .performScrollTo()
        .performTextInput("test@email.com")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.WEBSITE_FIELD)
        .performScrollTo()
        .performTextInput("example.com")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.INSTAGRAM_FIELD)
        .performScrollTo()
        .performTextInput("test_insta")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.FACEBOOK_FIELD)
        .performScrollTo()
        .performTextInput("test_fb")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.TIKTOK_FIELD)
        .performScrollTo()
        .performTextInput("test_tiktok")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.ADDRESS_FIELD)
        .performScrollTo()
        .performTextInput("123 Test Street")

    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.PREFIX_DROPDOWN)
        .performScrollTo()
        .performClick()
    composeTestRule.onNode(hasText("+1 American Samoa")).performScrollTo().performClick()

    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("123456789")

    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.NAME_FIELD)
        .assertTextContains("Test Org")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.DESCRIPTION_FIELD)
        .assertTextContains("Cool description")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.EMAIL_FIELD)
        .assertTextContains("test@email.com")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.PHONE_FIELD)
        .assertTextContains("123456789")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.WEBSITE_FIELD)
        .assertTextContains("example.com")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.INSTAGRAM_FIELD)
        .assertTextContains("test_insta")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.FACEBOOK_FIELD)
        .assertTextContains("test_fb")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.TIKTOK_FIELD)
        .assertTextContains("test_tiktok")
    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.ADDRESS_FIELD)
        .assertTextContains("123 Test Street")
  }

  @Test
  fun submitButtonShowsErrorOnEmptyRequiredFields() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.SUBMIT_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.onNodeWithText("Please fix errors").assertIsDisplayed()
  }

  @Test
  fun canSelectCountryPrefix() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(CreateOrganizationTestTags.PREFIX_DROPDOWN)
        .performScrollTo()
        .performClick()

    composeTestRule
        .onNode(hasText("+1 American Samoa", substring = true))
        .assertIsDisplayed()
        .performClick()
  }
}
