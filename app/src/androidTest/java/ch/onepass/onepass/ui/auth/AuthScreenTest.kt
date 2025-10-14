package ch.onepass.onepass.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun authScreen_displaysLogoAndLoginButton() {
    val vm = FakeAuthViewModel()

    composeRule.setContent { AuthScreen(onSignedIn = {}, authViewModel = vm) }

    composeRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun clickingLogo_togglesBetweenStates() {
    val vm = FakeAuthViewModel()

    composeRule.setContent { AuthScreen(onSignedIn = {}, authViewModel = vm) }

    val logoNode = composeRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO)
    logoNode.assertIsDisplayed()

    logoNode.performClick()
    logoNode.assertIsDisplayed()

    logoNode.performClick()
    logoNode.assertIsDisplayed()
  }

  @Test
  fun clickingLoginButton_triggersSignIn() {
    val vm = FakeAuthViewModel()

    composeRule.setContent { AuthScreen(onSignedIn = {}, authViewModel = vm) }

    composeRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    assertTrue("signIn() should be called", vm.signInCalled)
  }
}
