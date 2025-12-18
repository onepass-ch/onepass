package ch.onepass.onepass.ui.profile

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import ch.onepass.onepass.R
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

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

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
            avatarUrl = "https://example.com/avatar.jpg",
            stats = ProfileStats(events = 12, upcoming = 3, saved = 7),
            isOrganizer = false,
            loading = false)
    val fakeVm = FakeProfileViewModel(state)

    compose.setContent { ProfileScreen(viewModel = fakeVm, onEffect = {}) }

    // Main sections

    compose.onNodeWithTag(ProfileTestTags.SCREEN).assertExists()
    compose.onNodeWithTag(ProfileTestTags.HEADER).assertExists()
    compose.onNodeWithTag(ProfileTestTags.HEADER_NAME).assertExists()
    compose.onNodeWithTag(ProfileTestTags.HEADER_EMAIL).assertExists()
    // Verify avatar is shown and initials are hidden when avatarUrl is present
    compose.onNodeWithTag(ProfileTestTags.HEADER_AVATAR).assertIsDisplayed()
    compose.onNodeWithText("WS").assertDoesNotExist()

    compose.onNodeWithTag(ProfileTestTags.STATS_ROW).assertExists()
    compose.onNodeWithTag(ProfileTestTags.STAT_EVENTS).assertExists()
    compose.onNodeWithTag(ProfileTestTags.STAT_UPCOMING).assertExists()
    compose.onNodeWithTag(ProfileTestTags.STAT_SAVED).assertExists()

    compose.onNodeWithTag(ProfileTestTags.ORG_SECTION_TITLE).assertExists()
    compose.onNodeWithTag(ProfileTestTags.ORG_CARD).assertExists()
    compose.onNodeWithTag(ProfileTestTags.ORG_CTA).assertExists()

    compose.onNodeWithTag(ProfileTestTags.SETTINGS_INVITATIONS).assertExists()
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_ACCOUNT).assertExists()
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_PAYMENTS).assertExists()
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_HELP).assertExists()
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_SIGN_OUT).assertExists()

    // Sanity check for text content
    compose.onNodeWithText("WILL SMITH").assertExists()
    compose
        .onNodeWithText(context.getString(R.string.profile_organizer_section_title))
        .assertExists()
  }

  @Test
  fun content_showsInitialsWhenNoAvatar() {
    val state =
        ProfileUiState(
            displayName = "WILL SMITH",
            email = "willsmith@email.com",
            initials = "WS",
            avatarUrl = null,
            stats = ProfileStats(events = 12, upcoming = 3, saved = 7),
            isOrganizer = false,
            loading = false)
    val fakeVm = FakeProfileViewModel(state)

    compose.setContent { ProfileScreen(viewModel = fakeVm, onEffect = {}) }

    // Verify avatar is hidden and initials are shown
    compose.onNodeWithTag(ProfileTestTags.HEADER_AVATAR).assertDoesNotExist()
    compose.onNodeWithText("WS").assertIsDisplayed()
  }

  @Test
  fun invitationsBadge_notShownWhenCountIsZero() {
    val state =
        ProfileUiState(
            displayName = "Test User",
            email = "test@email.com",
            initials = "TU",
            pendingInvitations = 0, // Zero invitations
            loading = false)
    val fakeVm = FakeProfileViewModel(state)

    compose.setContent { ProfileScreen(viewModel = fakeVm, onEffect = {}) }

    // Badge should not exist when count is 0
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_INVITATIONS_BADGE).assertDoesNotExist()
  }

  @Test
  fun invitationsBadge_shownWhenCountIsGreaterThanZero() {
    val state =
        ProfileUiState(
            displayName = "Test User",
            email = "test@email.com",
            initials = "TU",
            pendingInvitations = 3, // 3 pending invitations
            loading = false)
    val fakeVm = FakeProfileViewModel(state)

    compose.setContent { ProfileScreen(viewModel = fakeVm, onEffect = {}) }

    // Badge should be displayed - use useUnmergedTree = true
    compose
        .onNodeWithTag(ProfileTestTags.SETTINGS_INVITATIONS_BADGE, useUnmergedTree = true)
        .assertIsDisplayed()
    // Badge should show the correct count
    compose.onNodeWithText("3", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun invitationsBadge_displaysCorrectCount() {
    val state =
        ProfileUiState(
            displayName = "Test User",
            email = "test@email.com",
            initials = "TU",
            pendingInvitations = 15, // 15 pending invitations
            loading = false)
    val fakeVm = FakeProfileViewModel(state)

    compose.setContent { ProfileScreen(viewModel = fakeVm, onEffect = {}) }

    compose
        .onNodeWithTag(ProfileTestTags.SETTINGS_INVITATIONS_BADGE, useUnmergedTree = true)
        .assertIsDisplayed()
    compose.onNodeWithText("15", useUnmergedTree = true).assertIsDisplayed()
  }
}
