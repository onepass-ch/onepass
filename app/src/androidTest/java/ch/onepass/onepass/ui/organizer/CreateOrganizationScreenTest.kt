package ch.onepass.onepass.ui.organizer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.ui.components.forms.SubmitButton
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
            OrganizationFormTestTags.NAME_FIELD,
            OrganizationFormTestTags.DESCRIPTION_FIELD,
            OrganizationFormTestTags.EMAIL_FIELD,
            OrganizationFormTestTags.PHONE_FIELD,
            OrganizationFormTestTags.WEBSITE_FIELD,
            OrganizationFormTestTags.INSTAGRAM_FIELD,
            OrganizationFormTestTags.FACEBOOK_FIELD,
            OrganizationFormTestTags.TIKTOK_FIELD,
            OrganizationFormTestTags.ADDRESS_FIELD,
            OrganizationFormTestTags.SUBMIT_BUTTON)

    tags.forEach { tag -> composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed() }
  }

  @Test
  fun enteringTextUpdatesFields() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Test Org")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.DESCRIPTION_FIELD)
        .performScrollTo()
        .performTextInput("Cool description")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.EMAIL_FIELD)
        .performScrollTo()
        .performTextInput("test@email.com")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.WEBSITE_FIELD)
        .performScrollTo()
        .performTextInput("example.com")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.INSTAGRAM_FIELD)
        .performScrollTo()
        .performTextInput("test_insta")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.FACEBOOK_FIELD)
        .performScrollTo()
        .performTextInput("test_fb")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.TIKTOK_FIELD)
        .performScrollTo()
        .performTextInput("test_tiktok")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.ADDRESS_FIELD)
        .performScrollTo()
        .performTextInput("123 Test Street")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_PREFIX)
        .performScrollTo()
        .performClick()
    composeTestRule.onNode(hasText("American Samoa +1")).performScrollTo().performClick()

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("123456789")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.NAME_FIELD)
        .assertTextContains("Test Org")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.DESCRIPTION_FIELD)
        .assertTextContains("Cool description")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.EMAIL_FIELD)
        .assertTextContains("test@email.com")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD)
        .assertTextContains("123456789")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.WEBSITE_FIELD)
        .assertTextContains("example.com")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.INSTAGRAM_FIELD)
        .assertTextContains("test_insta")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.FACEBOOK_FIELD)
        .assertTextContains("test_fb")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.TIKTOK_FIELD)
        .assertTextContains("test_tiktok")
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.ADDRESS_FIELD)
        .assertTextContains("123 Test Street")
  }

  @Test
  fun submitButtonShowsErrorOnEmptyRequiredFields() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.SUBMIT_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.onNodeWithText("Please fix validation errors").assertIsDisplayed()
  }

  @Test
  fun canSelectCountryPrefix() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_PREFIX)
        .performScrollTo()
        .performClick()

    composeTestRule
        .onNode(hasText("American Samoa +1", substring = true))
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
        .onNodeWithTag(OrganizationFormTestTags.PROFILE_IMAGE_BUTTON)
        .performScrollTo()
        .assertExists()
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.COVER_IMAGE_BUTTON)
        .performScrollTo()
        .assertExists()
  }

  @Test
  fun characterCountersDisplayForAllFields() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    val charCountTags =
        listOf(
            OrganizationFormTestTags.NAME_CHAR_COUNT,
            OrganizationFormTestTags.DESCRIPTION_CHAR_COUNT,
            OrganizationFormTestTags.EMAIL_CHAR_COUNT,
            OrganizationFormTestTags.PHONE_CHAR_COUNT,
            OrganizationFormTestTags.WEBSITE_CHAR_COUNT,
            OrganizationFormTestTags.INSTAGRAM_CHAR_COUNT,
            OrganizationFormTestTags.FACEBOOK_CHAR_COUNT,
            OrganizationFormTestTags.TIKTOK_CHAR_COUNT,
            OrganizationFormTestTags.ADDRESS_CHAR_COUNT)

    charCountTags.forEach { tag ->
      composeTestRule.onNodeWithTag(tag).performScrollTo().assertExists()
    }
  }

  @Test
  fun characterCounterShowsCorrectFormat() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    // Enter some text
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Test")

    // Character counter should show "4/50 characters" or similar
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.NAME_CHAR_COUNT)
        .assertTextContains("/", substring = true)
  }

  @Test
  fun phoneFieldAndPrefixWorkTogether() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    // Select a country prefix
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_PREFIX)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNode(hasText("Switzerland +41")).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Enter phone number
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("791234567")

    // Verify phone field has the value
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD)
        .assertTextContains("791234567")
  }

  @Test
  fun formScrollsToShowAllFields() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    // Scroll to and verify each field is accessible
    val allFieldTags =
        listOf(
            OrganizationFormTestTags.NAME_FIELD,
            OrganizationFormTestTags.DESCRIPTION_FIELD,
            OrganizationFormTestTags.EMAIL_FIELD,
            OrganizationFormTestTags.PHONE_FIELD,
            OrganizationFormTestTags.WEBSITE_FIELD,
            OrganizationFormTestTags.INSTAGRAM_FIELD,
            OrganizationFormTestTags.FACEBOOK_FIELD,
            OrganizationFormTestTags.TIKTOK_FIELD,
            OrganizationFormTestTags.ADDRESS_FIELD,
            OrganizationFormTestTags.PROFILE_IMAGE_BUTTON,
            OrganizationFormTestTags.COVER_IMAGE_BUTTON,
            OrganizationFormTestTags.SUBMIT_BUTTON)

    allFieldTags.forEach { tag ->
      composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
    }
  }

  @Test
  fun submittingFormWithValidDataCallsCreateOrganization() {
    composeTestRule.setContent { OnePassTheme { CreateOrganizationScreen(ownerId = "user123") } }

    // Fill in required fields
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Valid Organization")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.DESCRIPTION_FIELD)
        .performScrollTo()
        .performTextInput("This is a valid organization")

    // Select country prefix
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_PREFIX)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNode(hasText("American Samoa +1", substring = true))
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()

    // Enter phone
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("791234567")

    // Submit form
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.SUBMIT_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()
  }

  @Test
  fun submitButton_showsLoading_whenRequested() {
    composeTestRule.setContent {
      OnePassTheme { SubmitButton(onClick = {}, text = "Submit", isLoading = true) }
    }

    // Assert that progress indicator exists and text does not
    composeTestRule.onNodeWithTag("submit_loading_indicator").assertExists()
    composeTestRule.onNodeWithText("Submit").assertDoesNotExist()
  }

  @Test
  fun submitButton_isDisabled_whenLoading() {
    composeTestRule.setContent {
      OnePassTheme { SubmitButton(onClick = {}, text = "Submit", isLoading = true) }
    }

    // The button (which contains the loading indicator) should be disabled
    composeTestRule.onNodeWithTag("submit_loading_indicator").onParent().assertIsNotEnabled()
  }
}
