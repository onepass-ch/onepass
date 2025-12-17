package ch.onepass.onepass.ui.auth

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.R
import junit.framework.TestCase.assertTrue
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  @Test
  fun authScreen_initialState_displaysAllElements() {
    val vm = AuthViewModel()

    composeRule.setContent { AuthScreen(onSignedIn = {}, authViewModel = vm) }

    composeRule.onNodeWithTag(SignInScreenTestTags.AUTH_SCREEN).assertIsDisplayed()
    composeRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeRule.onNodeWithTag(SignInScreenTestTags.BLUR_CIRCLE_TOP).assertIsDisplayed()
    composeRule.onNodeWithTag(SignInScreenTestTags.BLUR_CIRCLE_BOTTOM).assertIsDisplayed()
  }

  @Test
  fun clickingLoginButton_triggersSignIn() {
    val vm = FakeAuthViewModel()

    composeRule.setContent { AuthScreen(onSignedIn = {}, authViewModel = vm) }

    composeRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    assertTrue("signIn method was not called", vm.signInCalled)
    composeRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun google_sign_in_is_configured() {
    val clientId = context.getString(R.string.default_web_client_id)

    // Skip test if resource is empty (useful for CI environments)
    Assume.assumeTrue("Google Sign-In not configured - skipping test", clientId.isNotEmpty())

    assertTrue(
        "Invalid Google client ID format: $clientId", clientId.endsWith(".googleusercontent.com"))
  }

  @Test
  fun loadingState_showsProgressBarAndHidesButton() {
    val vm = FakeAuthViewModel()
    vm.setLoading(true)

    composeRule.setContent { AuthScreen(onSignedIn = {}, authViewModel = vm) }

    composeRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertIsDisplayed()
    composeRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertDoesNotExist()
  }
}
