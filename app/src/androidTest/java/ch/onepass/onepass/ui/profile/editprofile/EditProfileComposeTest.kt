package ch.onepass.onepass.ui.profile.editprofile

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.ui.theme.OnePassTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileComposeTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockViewModel: EditProfileViewModel
  private lateinit var formStateFlow: MutableStateFlow<EditProfileFormState>
  private lateinit var uiStateFlow: MutableStateFlow<EditProfileUiState>
  private lateinit var countryListFlow: MutableStateFlow<List<Pair<String, String>>>
  private lateinit var selectedCountryCodeFlow: MutableStateFlow<String>

  @Before
  fun setup() {
    mockViewModel = mockk(relaxed = true)

    formStateFlow =
        MutableStateFlow(
            EditProfileFormState(
                displayName = "John Doe",
                phone = "123456789",
                country = "Switzerland",
                avatarUrl = null,
                avatarUri = null,
                initials = "JD"))

    uiStateFlow = MutableStateFlow(EditProfileUiState(isLoading = false, success = false))

    countryListFlow =
        MutableStateFlow(listOf("Switzerland" to "41", "United States" to "1", "France" to "33"))

    selectedCountryCodeFlow = MutableStateFlow("+41")

    every { mockViewModel.formState } returns formStateFlow
    every { mockViewModel.uiState } returns uiStateFlow
    every { mockViewModel.countryList } returns countryListFlow
    every { mockViewModel.selectedCountryCode } returns selectedCountryCodeFlow
  }

  @Test
  fun editProfileScreen_displaysAllElements() {
    composeTestRule.setContent { OnePassTheme { EditProfileScreen(viewModel = mockViewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditProfileTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(EditProfileTestTags.AVATAR).assertExists()
    composeTestRule.onNodeWithTag(EditProfileTestTags.NAME_FIELD).assertExists()
    composeTestRule.onNodeWithTag(EditProfileTestTags.PHONE_FIELD).assertExists()
    composeTestRule.onNodeWithTag(EditProfileTestTags.COUNTRY_FIELD).assertExists()
    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).assertExists()
  }

  @Test
  fun editProfileScreen_displaysExistingData() {
    composeTestRule.setContent { OnePassTheme { EditProfileScreen(viewModel = mockViewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditProfileTestTags.NAME_FIELD).assertTextContains("John Doe")
    composeTestRule.onNodeWithTag(EditProfileTestTags.PHONE_FIELD).assertTextContains("123456789")
    composeTestRule
        .onNodeWithTag(EditProfileTestTags.COUNTRY_FIELD)
        .assertTextContains("Switzerland")
  }

  @Test
  fun editProfileScreen_updateDisplayName() {
    var capturedName: String
    every { mockViewModel.updateDisplayName(any()) } answers
        {
          capturedName = firstArg()
          formStateFlow.value = formStateFlow.value.copy(displayName = capturedName)
        }

    composeTestRule.setContent { OnePassTheme { EditProfileScreen(viewModel = mockViewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditProfileTestTags.NAME_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(EditProfileTestTags.NAME_FIELD).performTextInput("Jane Smith")

    composeTestRule.waitForIdle()

    verify(atLeast = 1) { mockViewModel.updateDisplayName(any()) }
  }

  @Test
  fun editProfileScreen_updatePhoneNumber() {
    var capturedPhone: String
    every { mockViewModel.updatePhone(any()) } answers
        {
          capturedPhone = firstArg()
          formStateFlow.value = formStateFlow.value.copy(phone = capturedPhone)
        }

    composeTestRule.setContent { OnePassTheme { EditProfileScreen(viewModel = mockViewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditProfileTestTags.PHONE_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(EditProfileTestTags.PHONE_FIELD).performTextInput("987654321")

    composeTestRule.waitForIdle()

    verify(atLeast = 1) { mockViewModel.updatePhone(any()) }
  }

  @Test
  fun editProfileScreen_saveButtonCallsSaveProfile() {
    composeTestRule.setContent { OnePassTheme { EditProfileScreen(viewModel = mockViewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    verify { mockViewModel.saveProfile() }
  }

  @Test
  fun editProfileScreen_showsLoadingIndicator() {
    formStateFlow.value = EditProfileFormState()
    uiStateFlow.value = EditProfileUiState(isLoading = true, success = false)

    composeTestRule.setContent { OnePassTheme { EditProfileScreen(viewModel = mockViewModel) } }

    composeTestRule.waitForIdle()

    // The loading indicator is shown when isLoading=true and displayName is empty
    // Since the main content is hidden, we check for CircularProgressIndicator
    composeTestRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        .assertExists()
  }

  @Test
  fun editProfileScreen_disablesSaveButtonWhenLoading() {
    uiStateFlow.value = EditProfileUiState(isLoading = true, success = false)

    composeTestRule.setContent { OnePassTheme { EditProfileScreen(viewModel = mockViewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun editProfileScreen_avatarClickOpensOverlay() {
    composeTestRule.setContent { OnePassTheme { EditProfileScreen(viewModel = mockViewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditProfileTestTags.AVATAR).performClick()

    // Wait longer for overlay animation
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // The overlay shows a bottom sheet after clicking the edit button
    // First, we need to click the edit pencil icon
    composeTestRule.onNodeWithContentDescription("Edit", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()

    // Now check for overlay options
    composeTestRule
        .onNodeWithText("Choose from gallery", substring = true, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun editProfileScreen_countryDropdownFiltersResults() {
    composeTestRule.setContent { OnePassTheme { EditProfileScreen(viewModel = mockViewModel) } }

    composeTestRule.waitForIdle()

    // Clear the country field and type to filter
    composeTestRule.onNodeWithTag(EditProfileTestTags.COUNTRY_FIELD).performTextClearance()

    composeTestRule.onNodeWithTag(EditProfileTestTags.COUNTRY_FIELD).performTextInput("Switz")

    composeTestRule.waitForIdle()

    // Verify Switzerland appears in the filtered results
    composeTestRule
        .onAllNodesWithText("Switzerland", substring = true, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .size
        .let { count ->
          assert(count >= 1) { "Expected at least one 'Switzerland' node, found $count" }
        }
  }

  @Test
  fun editProfileScreen_navigatesBackOnBackButton() {
    var navigatedBack = false

    composeTestRule.setContent {
      OnePassTheme {
        EditProfileScreen(viewModel = mockViewModel, onNavigateBack = { navigatedBack = true })
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("Back").performClick()

    composeTestRule.waitForIdle()

    assert(navigatedBack)
  }

  @Test
  fun editProfileScreen_displaysInitialsWhenNoAvatar() {
    composeTestRule.setContent { OnePassTheme { EditProfileScreen(viewModel = mockViewModel) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("JD").assertExists()
  }

  @Test
  fun getFlagFromCountryName_returnsCorrectFlag() {
    val switzerlandFlag = getFlagFromCountryName("Switzerland")
    val usFlag = getFlagFromCountryName("United States")
    val unknownFlag = getFlagFromCountryName("InvalidCountry")

    assert(switzerlandFlag.isNotEmpty())
    assert(usFlag.isNotEmpty())
    assert(unknownFlag == "🌍")
  }
}
