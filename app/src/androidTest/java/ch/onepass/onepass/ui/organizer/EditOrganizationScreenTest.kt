package ch.onepass.onepass.ui.organizer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class EditOrganizationScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val mockOrganization =
      Organization(
          id = "org123",
          name = "Original Org",
          description = "Original Description",
          ownerId = "owner123",
          status = OrganizationStatus.ACTIVE,
          verified = false,
          profileImageUrl = null,
          coverImageUrl = null,
          contactEmail = "original@email.com",
          contactPhone = "123456789",
          website = "original.com",
          instagram = "original_insta",
          facebook = "original_fb",
          tiktok = "original_tiktok",
          address = "Original Address",
          eventIds = emptyList(),
          followerCount = 0,
          averageRating = 0.0f,
          createdAt = Timestamp.now(),
          updatedAt = null)

  private val mockUiState =
      MutableStateFlow(OrganizationEditorUiState(organization = mockOrganization))
  private val mockFormState = MutableStateFlow(OrganizationFormState())
  private val mockCountryList = MutableStateFlow(listOf("American Samoa" to 1))

  private val mockViewModel =
      mockk<OrganizationEditorViewModel>(relaxed = true) {
        every { uiState } returns mockUiState
        every { loadOrganizationById(any()) } returns Unit
        every { updateOrganization(any(), any()) } returns Unit
        every { clearSuccessFlag() } returns Unit
        every { clearError() } returns Unit
      }

  private val mockFormViewModel =
      mockk<OrganizationFormViewModel>(relaxed = true) {
        every { formState } returns mockFormState
        every { countryList } returns mockCountryList
        every { updateName(any()) } returns Unit
        every { updateDescription(any()) } returns Unit
        every { updateContactEmail(any()) } returns Unit
        every { updateContactPhone(any()) } returns Unit
        every { updateWebsite(any()) } returns Unit
        every { updateInstagram(any()) } returns Unit
        every { updateFacebook(any()) } returns Unit
        every { updateTiktok(any()) } returns Unit
        every { updateAddress(any()) } returns Unit
        every { updateCountryIndex(any()) } returns Unit
        every { validateForm() } returns true
      }

  @Test
  fun formFieldsAreVisible() {
    composeTestRule.setContent {
      OnePassTheme {
        EditOrganizationScreen(
            organizationId = "org123", viewModel = mockViewModel, formViewModel = mockFormViewModel)
      }
    }

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
            OrganizationFormTestTags.SUBMIT_BUTTON,
            OrganizationFormTestTags.PHONE_PREFIX)

    tags.forEach { tag -> composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed() }
  }

  @Test
  fun enteringTextUpdatesFields() {
    composeTestRule.setContent {
      OnePassTheme {
        EditOrganizationScreen(
            organizationId = "org123", viewModel = mockViewModel, formViewModel = mockFormViewModel)
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Updated Org")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.DESCRIPTION_FIELD)
        .performScrollTo()
        .performTextInput("Updated description")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.EMAIL_FIELD)
        .performScrollTo()
        .performTextInput("updated@email.com")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.WEBSITE_FIELD)
        .performScrollTo()
        .performTextInput("updated.com")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.INSTAGRAM_FIELD)
        .performScrollTo()
        .performTextInput("updated_insta")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.FACEBOOK_FIELD)
        .performScrollTo()
        .performTextInput("updated_fb")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.TIKTOK_FIELD)
        .performScrollTo()
        .performTextInput("updated_tiktok")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.ADDRESS_FIELD)
        .performScrollTo()
        .performTextInput("Updated Test Street")

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

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("987654321")
  }

  @Test
  fun canSelectCountryPrefix() {
    composeTestRule.setContent {
      OnePassTheme {
        EditOrganizationScreen(
            organizationId = "org123", viewModel = mockViewModel, formViewModel = mockFormViewModel)
      }
    }

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_PREFIX)
        .performScrollTo()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNode(hasText("American Samoa +1", substring = true))
        .assertIsDisplayed()
        .performClick()
  }

  @Test
  fun characterCountersDisplayForAllFields() {
    composeTestRule.setContent {
      OnePassTheme {
        EditOrganizationScreen(
            organizationId = "org123", viewModel = mockViewModel, formViewModel = mockFormViewModel)
      }
    }

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
    composeTestRule.setContent {
      OnePassTheme {
        EditOrganizationScreen(
            organizationId = "org123", viewModel = mockViewModel, formViewModel = mockFormViewModel)
      }
    }

    // Enter some text
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Test")

    // Character counter should show "X/50 characters" or similar
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.NAME_CHAR_COUNT)
        .assertTextContains("/", substring = true)
  }

  @Test
  fun submitButtonSubmitsTheForm() {
    composeTestRule.setContent {
      OnePassTheme {
        EditOrganizationScreen(
            organizationId = "org123", viewModel = mockViewModel, formViewModel = mockFormViewModel)
      }
    }
    composeTestRule.waitForIdle()

    // Fill in required fields
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Updated Org")

    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.DESCRIPTION_FIELD)
        .performScrollTo()
        .performTextInput("Updated description")

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
  fun phoneFieldAndPrefixWorkTogether() {
    composeTestRule.setContent {
      OnePassTheme {
        EditOrganizationScreen(
            organizationId = "org123", viewModel = mockViewModel, formViewModel = mockFormViewModel)
      }
    }

    // Select a country prefix
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

    // Enter phone number
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("123456789")

    // Verify phone field has the value
    composeTestRule
        .onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD)
        .assertTextContains("123456789")
  }

  @Test
  fun formScrollsToShowAllFields() {
    composeTestRule.setContent {
      OnePassTheme {
        EditOrganizationScreen(
            organizationId = "org123", viewModel = mockViewModel, formViewModel = mockFormViewModel)
      }
    }

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
}
