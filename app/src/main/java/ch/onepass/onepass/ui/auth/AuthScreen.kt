package ch.onepass.onepass.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.BlurCircleBottom
import ch.onepass.onepass.ui.theme.BlurCircleTop

object SignInScreenTestTags {
  const val AUTH_SCREEN = "authScreen"
  const val APP_LOGO = "appLogo"
  const val LOGIN_BUTTON = "loginButton"
  const val LOADING_INDICATOR = "loadingIndicator"
  const val HERO_TITLE = "heroTitleLogIn"
  const val BLUR_CIRCLE_TOP = "blurCircleTop"
  const val BLUR_CIRCLE_BOTTOM = "blurCircleBottom"
}

@Composable
fun AuthScreen(onSignedIn: () -> Unit = {}, authViewModel: AuthViewModel) {
  val context = LocalContext.current
  val credentialManager = remember { CredentialManager.create(context) }
  val uiState by authViewModel.uiState.collectAsState()
  val isLoading = uiState.isLoading

  LaunchedEffect(uiState.isSignedIn) { if (uiState.isSignedIn) onSignedIn() }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(SignInScreenTestTags.AUTH_SCREEN),
      containerColor = colorScheme.background) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).background(colorScheme.background),
            contentAlignment = Alignment.Center) {
              BlurCircle(
                  modifier =
                      Modifier.testTag(SignInScreenTestTags.BLUR_CIRCLE_TOP)
                          .offset(
                              x = AuthScreenDefaults.BlurCircleTopOffsetX,
                              y = AuthScreenDefaults.BlurCircleTopOffsetY),
                  size = AuthScreenDefaults.BlurCircleTopSize,
                  blurRadius = AuthScreenDefaults.BlurRadius,
                  color = BlurCircleTop)

              BlurCircle(
                  modifier =
                      Modifier.testTag(SignInScreenTestTags.BLUR_CIRCLE_BOTTOM)
                          .offset(
                              x = AuthScreenDefaults.BlurCircleBottomOffsetX,
                              y = AuthScreenDefaults.BlurCircleBottomOffsetY),
                  size = AuthScreenDefaults.BlurCircleBottomSize,
                  blurRadius = AuthScreenDefaults.BlurRadius,
                  color = BlurCircleBottom)

              Logo(
                  modifier =
                      Modifier.offset(
                              y = AuthScreenDefaults.LogoOffsetY,
                              x = AuthScreenDefaults.LogoOffsetX)
                          .testTag(SignInScreenTestTags.APP_LOGO),
                  iconSize = AuthScreenDefaults.LogoIconSize,
                  gap = AuthScreenDefaults.LogoGap,
                  fontSize = AuthScreenDefaults.LogoFontSize,
              )
              HeroTitle(
                  modifier =
                      Modifier.offset(
                              y = AuthScreenDefaults.HeroTitleOffsetY,
                              x = AuthScreenDefaults.HeroTitleOffsetX)
                          .testTag(SignInScreenTestTags.HERO_TITLE),
                  titleTop = androidx.compose.ui.res.stringResource(R.string.auth_hero_title_top),
                  titleBottom =
                      androidx.compose.ui.res.stringResource(R.string.auth_hero_title_bottom),
                  config =
                      HeroTitleConfig(
                          fontSize = AuthScreenDefaults.HeroTitleFontSize,
                          lineHeight = AuthScreenDefaults.HeroTitleLineHeight))

              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .offset(x = 0.dp, y = AuthScreenDefaults.SignInButtonOffsetY),
                  contentAlignment = Alignment.Center) {
                    if (isLoading) {
                      CircularProgressIndicator(
                          modifier =
                              Modifier.size(AuthScreenDefaults.LoadingIndicatorSize)
                                  .testTag(SignInScreenTestTags.LOADING_INDICATOR))
                    } else {
                      GoogleSignInButton(
                          onSignInClick = { authViewModel.signIn(context, credentialManager) })
                    }
                  }
            }
      }
}

@Composable
fun BlurCircle(modifier: Modifier = Modifier, size: Dp, color: Color, blurRadius: Dp) {
  Box(
      modifier =
          modifier
              .size(size)
              .blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
              .background(color, CircleShape))
}

@Composable
fun Logo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    gap: Dp = 12.dp,
    fontSize: Int = 36,
    textColor: Color = colorScheme.onBackground
) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Image(
        painter = painterResource(id = R.drawable.ticket_logo),
        contentDescription = androidx.compose.ui.res.stringResource(R.string.content_desc_app_logo),
        modifier = Modifier.size(iconSize).testTag("logo_icon"))
    Spacer(Modifier.width(gap))
    Column {
      Text(
          text = androidx.compose.ui.res.stringResource(R.string.app_name_part_one),
          style =
              MaterialTheme.typography.titleLarge.copy(
                  color = textColor, fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold))
      Text(
          text = androidx.compose.ui.res.stringResource(R.string.app_name_part_two),
          style =
              MaterialTheme.typography.titleLarge.copy(
                  color = textColor, fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold))
    }
  }
}

data class HeroTitleConfig(
    val fontSize: Int = 42,
    val lineHeight: Int = 44,
    val fontWeight: FontWeight = FontWeight.ExtraBold,
    val letterSpacing: Int = 0,
    val textAlign: TextAlign = TextAlign.Start,
    val textColor: Color? = null
)

@Composable
fun HeroTitle(
    modifier: Modifier = Modifier,
    titleTop: String,
    titleBottom: String,
    config: HeroTitleConfig = HeroTitleConfig()
) {
  val textColor = config.textColor ?: colorScheme.onBackground
  Text(
      modifier = modifier,
      text = "$titleTop\n$titleBottom",
      lineHeight = config.lineHeight.sp,
      letterSpacing = config.letterSpacing.sp,
      textAlign = config.textAlign,
      style =
          MaterialTheme.typography.titleLarge.copy(
              color = textColor, fontSize = config.fontSize.sp, fontWeight = config.fontWeight))
}

@Composable
fun GoogleSignInButton(onSignInClick: () -> Unit) {
  Button(
      onClick = onSignInClick,
      colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), // Button color
      shape = RoundedCornerShape(25), // Circular edges for the button
      border = BorderStroke(1.dp, colorScheme.onSurface),
      modifier =
          Modifier.padding(8.dp)
              .height(48.dp) // Adjust height as needed
              .fillMaxWidth(0.9f)
              .testTag(SignInScreenTestTags.LOGIN_BUTTON)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
              // Load the Google logo from resources
              Image(
                  painter =
                      painterResource(id = R.drawable.google_logo), // Ensure this drawable exists
                  contentDescription =
                      androidx.compose.ui.res.stringResource(R.string.content_desc_google_logo),
                  modifier =
                      Modifier.size(30.dp) // Size of the Google logo
                          .padding(end = 8.dp))

              // Text for the button
              Text(
                  text = androidx.compose.ui.res.stringResource(R.string.auth_sign_in_google),
                  color = colorScheme.onBackground, // Text color
                  fontSize = 16.sp, // Font size
                  fontWeight = FontWeight.Medium,
                  style = MaterialTheme.typography.bodyMedium)
            }
      }
}

private object AuthScreenDefaults {
  val BlurCircleTopOffsetX = (-190).dp
  val BlurCircleTopOffsetY = (-380).dp
  val BlurCircleTopSize = 481.dp
  val BlurRadius = 40.dp

  val BlurCircleBottomOffsetX = 200.dp
  val BlurCircleBottomOffsetY = 130.dp
  val BlurCircleBottomSize = 200.dp

  val LogoOffsetX = 54.dp
  val LogoOffsetY = (-125).dp
  val LogoIconSize = 100.dp
  val LogoGap = 12.dp
  const val LogoFontSize = 48

  val HeroTitleOffsetX = (-54).dp
  val HeroTitleOffsetY = 105.dp
  const val HeroTitleFontSize = 48
  const val HeroTitleLineHeight = 56

  val SignInButtonOffsetY = 290.dp
  val LoadingIndicatorSize = 48.dp
}
