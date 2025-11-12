package ch.onepass.onepass.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Tests rendering of ProfileScreen() via the public composable. Uses a fake ViewModel to inject
 * deterministic state.
 */
class ProfileScreenComposeTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  /** Simple fake ViewModel that overrides only the state. */
  private class FakeProfileViewModel(state: ProfileUiState) : ProfileViewModel() {
    private val fakeState = MutableStateFlow(state)
    override val state: StateFlow<ProfileUiState>
      get() = fakeState
  }

  @Test
  fun loading_showsTaggedProgress() {
    val fakeVm = FakeProfileViewModel(ProfileUiState(loading = true))

    compose.setContent { ProfileScreen(viewModel = fakeVm, onEffect = {}) }

    compose.onNodeWithTag(ProfileTestTags.LOADING).assertIsDisplayed()
  }

  @Test
  fun content_showsTaggedSections() {
    val state =
        ProfileUiState(
            displayName = "WILL SMITH",
            email = "willsmith@email.com",
            initials = "WS",
            stats = ProfileStats(events = 12, upcoming = 3, saved = 7),
            isOrganizer = false,
            loading = false)
    val fakeVm = FakeProfileViewModel(state)

    compose.setContent { ProfileScreen(viewModel = fakeVm, onEffect = {}) }

    // Main sections
    compose.onNodeWithTag(ProfileTestTags.SCREEN).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.HEADER).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.HEADER_NAME).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.HEADER_EMAIL).assertIsDisplayed()

    compose.onNodeWithTag(ProfileTestTags.STATS_ROW).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.STAT_EVENTS).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.STAT_UPCOMING).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.STAT_SAVED).assertIsDisplayed()

    compose.onNodeWithTag(ProfileTestTags.ORG_SECTION_TITLE).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.ORG_CARD).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.ORG_CTA).assertIsDisplayed()

    //    compose.onNodeWithTag(ProfileTestTags.SETTINGS_INVITATIONS).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_ACCOUNT).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_PAYMENTS).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_HELP).assertIsDisplayed()
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_SIGN_OUT).assertIsDisplayed()

    // Sanity check for text content
    compose.onNodeWithText("WILL SMITH").assertIsDisplayed()
    compose.onNodeWithText("ORGANIZER SETTINGS").assertIsDisplayed()
  }
}
