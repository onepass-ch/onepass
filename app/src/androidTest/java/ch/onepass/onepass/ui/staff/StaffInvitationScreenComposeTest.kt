package ch.onepass.onepass.ui.staff

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.model.user.UserSearchType
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Rule
import org.junit.Test

class StaffInvitationScreenComposeTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun setContent(viewModel: StaffInvitationViewModel) {
    composeRule.setContent {
      OnePassTheme { StaffInvitationScreen(viewModel, onNavigateBack = {}) }
    }
  }

  private fun setUiState(viewModel: StaffInvitationViewModel, state: StaffInvitationUiState) {
    val field = StaffInvitationViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<*>
    @Suppress("UNCHECKED_CAST")
    (stateFlow as kotlinx.coroutines.flow.MutableStateFlow<StaffInvitationUiState>).value = state
    composeRule.waitForIdle()
  }

  @Test
  fun topBar_tabs_and_searchField_areDisplayed() {
    val vm = StaffInvitationViewModel(organizationId = "org_test")
    setContent(vm)

    composeRule.onNodeWithTag(StaffInvitationTestTags.TOP_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(StaffInvitationTestTags.BACK_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(StaffInvitationTestTags.TITLE).assertIsDisplayed()

    composeRule.onNodeWithTag(StaffInvitationTestTags.TAB_ROW).assertIsDisplayed()
    composeRule.onNodeWithTag(StaffInvitationTestTags.TAB_DISPLAY_NAME).assertIsDisplayed()
    composeRule.onNodeWithTag(StaffInvitationTestTags.TAB_EMAIL).assertIsDisplayed()

    // Initial selected tab is DISPLAY_NAME
    composeRule.onNodeWithTag(StaffInvitationTestTags.TAB_DISPLAY_NAME).assertIsSelected()

    composeRule.onNodeWithTag(StaffInvitationTestTags.SEARCH_FIELD).assertIsDisplayed()
  }

  @Test
  fun showsLoadingIndicator_whenIsLoadingTrue() {
    val vm = StaffInvitationViewModel(organizationId = "org_test")
    setContent(vm)

    setUiState(vm, StaffInvitationUiState(isLoading = true))

    composeRule.onNodeWithTag(StaffInvitationTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun showsEmptyState_whenQueryBlank() {
    val vm = StaffInvitationViewModel(organizationId = "org_test")
    setContent(vm)

    // Default state: blank query, no results
    setUiState(
        vm,
        StaffInvitationUiState(
            selectedTab = UserSearchType.DISPLAY_NAME,
            searchQuery = "",
            searchResults = emptyList()))

    composeRule.onNodeWithTag(StaffInvitationTestTags.EMPTY_STATE).assertIsDisplayed()
  }

  @Test
  fun showsEmptyState_whenNoResultsForNonBlankQuery() {
    val vm = StaffInvitationViewModel(organizationId = "org_test")
    setContent(vm)

    setUiState(vm, StaffInvitationUiState(searchQuery = "alice", searchResults = emptyList()))

    composeRule.onNodeWithTag(StaffInvitationTestTags.EMPTY_STATE).assertIsDisplayed()
  }

  @Test
  fun showsResultsList_whenResultsPresent() {
    val vm = StaffInvitationViewModel(organizationId = "org_test")
    setContent(vm)

    val results =
        listOf(
            StaffSearchResult("1", "alice@onepass.ch", "Alice Keller", null),
            StaffSearchResult("2", "bob@onepass.ch", "Bob Smith", "https://example.com/a.png"))

    setUiState(vm, StaffInvitationUiState(searchQuery = "a", searchResults = results))

    composeRule.onNodeWithTag(StaffInvitationTestTags.RESULTS_LIST).assertIsDisplayed()
    // Check list item texts appear
    composeRule.onNodeWithText("Alice Keller").assertIsDisplayed()
    composeRule.onNodeWithText("Bob Smith").assertIsDisplayed()
  }

  @Test
  fun showsErrorMessage_whenErrorPresent() {
    val vm = StaffInvitationViewModel(organizationId = "org_test")
    setContent(vm)

    setUiState(vm, StaffInvitationUiState(searchQuery = "a", errorMessage = "Boom!"))

    composeRule.onNodeWithTag(StaffInvitationTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeRule.onNodeWithText("Boom!").assertIsDisplayed()

    // With error message, list should not be shown if there are no results
    composeRule.onNodeWithTag(StaffInvitationTestTags.RESULTS_LIST).assertIsNotDisplayed()
  }
}
