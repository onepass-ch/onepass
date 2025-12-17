package ch.onepass.onepass.ui.auth

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class AuthComponentsTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun blurCircle_withCustomProperties_rendersCorrectly() {
    val size = 200.dp
    val color = Color.Red

    composeRule.setContent {
      BlurCircle(
          modifier = Modifier.testTag("blur_circle"),
          size = size,
          color = color,
          blurRadius = 50.dp)
    }

    composeRule
        .onNodeWithTag("blur_circle")
        .assertIsDisplayed()
        .assertWidthIsEqualTo(size)
        .assertHeightIsEqualTo(size)
  }

  @Test
  fun logo_withCustomProperties_rendersCorrectly() {
    val iconSize = 64.dp
    val fontSize = 40
    val textColor = Color.Blue

    composeRule.setContent {
      Logo(
          modifier = Modifier.testTag("test_logo"),
          iconSize = iconSize,
          fontSize = fontSize,
          textColor = textColor)
    }

    composeRule.onNodeWithTag("test_logo").assertIsDisplayed()
    composeRule
        .onNode(hasTestTag("logo_icon"), useUnmergedTree = true)
        .assertWidthIsEqualTo(iconSize)
        .assertHeightIsEqualTo(iconSize)
  }

  @Test
  fun heroTitle_withCustomProperties_rendersCorrectly() {
    val titleTop = "Welcome to"
    val titleBottom = "OnePass"
    val textColor = Color.Magenta

    composeRule.setContent {
      HeroTitle(
          modifier = Modifier.testTag("hero_title"),
          titleTop = titleTop,
          titleBottom = titleBottom,
          config = HeroTitleConfig(textColor = textColor))
    }

    composeRule
        .onNodeWithTag("hero_title")
        .assertIsDisplayed()
        .assertTextEquals("$titleTop\n$titleBottom")
  }

  @Test
  fun authScreen_whenLoading_showsLoadingIndicator() {
    val fakeViewModel = FakeAuthViewModel()
    fakeViewModel.setLoading(true)

    composeRule.setContent { AuthScreen(authViewModel = fakeViewModel) }

    composeRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertIsDisplayed()
    composeRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertDoesNotExist()
  }

  @Test
  fun authScreen_whenNotLoading_showsSignInButton() {
    val fakeViewModel = FakeAuthViewModel()
    fakeViewModel.setLoading(false)

    composeRule.setContent { AuthScreen(authViewModel = fakeViewModel) }

    composeRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(SignInScreenTestTags.LOADING_INDICATOR).assertDoesNotExist()
  }
}
