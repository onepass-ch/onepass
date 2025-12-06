package ch.onepass.onepass.ui.organizer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.ui.theme.OnePassTheme
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
          name = "",
          description = "",
          contactEmail = "",
          contactPhone = "",
          website = "",
          instagram = "",
          facebook = "",
          tiktok = "",
          address = "")

  private val mockUiState =
      MutableStateFlow(OrganizationEditorUiState(organization = mockOrganization))
  private val mockFormState = MutableStateFlow(OrganizationFormState())
  private val mockCountryList = MutableStateFlow(listOf("American Samoa" to 1))

  private val mockViewModel =
      mockk<OrganizationEditorViewModel>(relaxed = true) {
        every { uiState } returns mockUiState
        every { loadOrganizationById(any()) } returns Unit
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
            OrganizationTestTags.NAME_FIELD,
            OrganizationTestTags.DESCRIPTION_FIELD,
            OrganizationTestTags.EMAIL_FIELD,
            OrganizationTestTags.PHONE_FIELD,
            OrganizationTestTags.WEBSITE_FIELD,
            OrganizationTestTags.INSTAGRAM_FIELD,
            OrganizationTestTags.FACEBOOK_FIELD,
            OrganizationTestTags.TIKTOK_FIELD,
            OrganizationTestTags.ADDRESS_FIELD,
            OrganizationTestTags.SUBMIT_BUTTON,
            OrganizationTestTags.PREFIX_DROPDOWN)

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

    composeTestRule
        .onNode(hasText("+1 American Samoa", substring = true))
        .performScrollTo()
        .performClick()

    composeTestRule
        .onNodeWithTag(OrganizationTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("123456789")
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
        .onNodeWithTag(OrganizationTestTags.PREFIX_DROPDOWN)
        .performScrollTo()
        .performClick()

    composeTestRule
        .onNode(hasText("+1 American Samoa", substring = true))
        .assertIsDisplayed()
        .performClick()
  }
}
