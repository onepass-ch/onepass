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
            OrganizationTestTags.NAME_FIELD,
            OrganizationTestTags.DESCRIPTION_FIELD,
            OrganizationTestTags.EMAIL_FIELD,
            OrganizationTestTags.PHONE_FIELD,
            OrganizationTestTags.WEBSITE_FIELD,
            OrganizationTestTags.INSTAGRAM_FIELD,
            OrganizationTestTags.FACEBOOK_FIELD,
            OrganizationTestTags.TIKTOK_FIELD,
            OrganizationTestTags.ADDRESS_FIELD,
            OrganizationTestTags.SUBMIT_BUTTON)

    tags.forEach { tag -> composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed() }
  }

  @Test
  fun enteringTextUpdatesFields() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(OrganizationTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Test Org")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.DESCRIPTION_FIELD)
        .performScrollTo()
        .performTextInput("Cool description")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.EMAIL_FIELD)
        .performScrollTo()
        .performTextInput("test@email.com")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.WEBSITE_FIELD)
        .performScrollTo()
        .performTextInput("example.com")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.INSTAGRAM_FIELD)
        .performScrollTo()
        .performTextInput("test_insta")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.FACEBOOK_FIELD)
        .performScrollTo()
        .performTextInput("test_fb")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.TIKTOK_FIELD)
        .performScrollTo()
        .performTextInput("test_tiktok")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.ADDRESS_FIELD)
        .performScrollTo()
        .performTextInput("123 Test Street")

    composeTestRule
        .onNodeWithTag(OrganizationTestTags.PREFIX_DROPDOWN)
        .performScrollTo()
        .performClick()
    composeTestRule.onNode(hasText("+1 American Samoa")).performScrollTo().performClick()

    composeTestRule
        .onNodeWithTag(OrganizationTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("123456789")

    composeTestRule.onNodeWithTag(OrganizationTestTags.NAME_FIELD).assertTextContains("Test Org")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.DESCRIPTION_FIELD)
        .assertTextContains("Cool description")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.EMAIL_FIELD)
        .assertTextContains("test@email.com")
    composeTestRule.onNodeWithTag(OrganizationTestTags.PHONE_FIELD).assertTextContains("123456789")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.WEBSITE_FIELD)
        .assertTextContains("example.com")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.INSTAGRAM_FIELD)
        .assertTextContains("test_insta")
    composeTestRule.onNodeWithTag(OrganizationTestTags.FACEBOOK_FIELD).assertTextContains("test_fb")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.TIKTOK_FIELD)
        .assertTextContains("test_tiktok")
    composeTestRule
        .onNodeWithTag(OrganizationTestTags.ADDRESS_FIELD)
        .assertTextContains("123 Test Street")
  }

  @Test
  fun submitButtonShowsErrorOnEmptyRequiredFields() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(OrganizationTestTags.SUBMIT_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.onNodeWithText("Please fix errors").assertIsDisplayed()
  }

  @Test
  fun canSelectCountryPrefix() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(OrganizationTestTags.PREFIX_DROPDOWN)
        .performScrollTo()
        .performClick()

    composeTestRule
        .onNode(hasText("+1 American Samoa", substring = true))
        .assertIsDisplayed()
        .performClick()
  }

  @Test
  fun profileImageSelectionIndicatorNotShownInitially() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    // Profile image selection indicator should not be shown initially
    composeTestRule.onNodeWithText("✓ Profile image selected").assertDoesNotExist()
  }

  @Test
  fun coverImageSelectionIndicatorNotShownInitially() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    // Banner image selection indicator should not be shown initially
    composeTestRule.onNodeWithText("✓ Banner image selected").assertDoesNotExist()
  }

  @Test
  fun uploadImageButtonsAreVisible() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    // Should have upload buttons (they contain text about uploading images)
    composeTestRule
        .onNodeWithText("Profile image", substring = true)
        .performScrollTo()
        .assertExists()
    composeTestRule
        .onNodeWithText("Banner image", substring = true)
        .performScrollTo()
        .assertExists()
  }
}
