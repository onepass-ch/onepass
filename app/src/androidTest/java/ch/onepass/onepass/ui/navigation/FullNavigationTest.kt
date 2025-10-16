package ch.onepass.onepass.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.OnePassApp
import ch.onepass.onepass.ui.auth.SignInScreenTestTags
import ch.onepass.onepass.ui.feed.FeedScreenTestTags
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.myevents.MyEventsTestTags
import ch.onepass.onepass.ui.profile.ProfileTestTags
import org.junit.Rule
import org.junit.Test

class OnePassAppRoutesTest {

  @get:Rule val compose = createComposeRule()

  private fun setApp() {
    compose.setContent {
      OnePassApp(
          mapViewModel = viewModel<MapViewModel>(),
          isLocationPermissionGranted = true,
          testAuthButtonTag = SignInScreenTestTags.LOGIN_BUTTON // use AppNavHost bypass
          )
    }
  }

  private fun tapTestLogin() {
    compose.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed().performClick()
  }

  // 1) Auth → Events
  @Test
  fun auth_navigates_to_events() {
    setApp()
    tapTestLogin()
    compose.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
  }

  // 2) Events → Tickets (bottom bar)
  @Test
  fun bottomBar_navigates_to_tickets() {
    setApp()
    tapTestLogin()
    compose.onNodeWithTag("BOTTOM_TAB_TICKETS").assertIsDisplayed().performClick()
    compose.onNodeWithTag(MyEventsTestTags.TABS_ROW).assertIsDisplayed()
  }

  // 4) Map → Profile (bottom bar)
  @Test
  fun bottomBar_navigates_to_profile() {
    setApp()
    tapTestLogin()
    compose.onNodeWithTag("BOTTOM_TAB_PROFILE").assertIsDisplayed().performClick()
    compose.onNodeWithTag(ProfileTestTags.SCREEN).assertIsDisplayed()
  }

  // 5) Profile → Sign out → Auth
  @Test
  fun profile_sign_out_returns_to_auth() {
    setApp()
    tapTestLogin()
    compose.onNodeWithTag("BOTTOM_TAB_PROFILE").assertIsDisplayed().performClick()
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_SIGN_OUT).assertIsDisplayed().performClick()
    compose.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }
}
