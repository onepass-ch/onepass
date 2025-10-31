package ch.onepass.onepass.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
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
fun AuthScreen(onSignedIn: () -> Unit = {}, authViewModel: AuthViewModel = AuthViewModel()) {
  val context = LocalContext.current
  val credentialManager = remember { CredentialManager.create(context) }
  val uiState by authViewModel.uiState.collectAsState()
  val isLoading = uiState.isLoading

  LaunchedEffect(uiState.isSignedIn) { if (uiState.isSignedIn) onSignedIn() }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(SignInScreenTestTags.AUTH_SCREEN),
      containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center) {
              BlurCircle(
                  modifier =
                      Modifier.testTag(SignInScreenTestTags.BLUR_CIRCLE_TOP)
                          .offset(x = (-190).dp, y = (-380).dp),
                  size = 481.dp,
                  blurRadius = 40.dp,
                  color = Color(0xFF683F88))

              BlurCircle(
                  modifier =
                      Modifier.testTag(SignInScreenTestTags.BLUR_CIRCLE_BOTTOM)
                          .offset(x = (200).dp, y = (130).dp),
                  size = 200.dp,
                  blurRadius = 40.dp,
                  color = Color(0xFF4B210A),
              )

              Logo(
                  modifier =
                      Modifier.offset(y = (-125).dp, x = 54.dp)
                          .testTag(SignInScreenTestTags.APP_LOGO),
                  iconSize = 100.dp,
                  gap = 12.dp,
                  fontSize = 48,
              )
              HeroTitle(
                  modifier =
                      Modifier.offset(y = 105.dp, x = (-75).dp)
                          .testTag(SignInScreenTestTags.HERO_TITLE),
                  titleTop = "BUY, SELL",
                  titleBottom = "DISCOVER",
                  fontSize = 48,
                  lineHeight = 56)

              Box(
                  modifier = Modifier.fillMaxWidth().offset(x = 0.dp, y = 290.dp),
                  contentAlignment = Alignment.Center) {
                    if (isLoading) {
                      CircularProgressIndicator(
                          modifier =
                              Modifier.size(48.dp).testTag(SignInScreenTestTags.LOADING_INDICATOR))
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
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Image(
        painter = painterResource(id = R.drawable.ticket_logo),
        contentDescription = "App Logo Icon",
        modifier = Modifier.size(iconSize).testTag("logo_icon"))
    Spacer(Modifier.width(gap))
    Column {
      Text(
          text = "ONE",
          style =
              MaterialTheme.typography.titleLarge.copy(
                  color = textColor, fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold))
      Text(
          text = "PASS.",
          style =
              MaterialTheme.typography.titleLarge.copy(
                  color = textColor, fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold))
    }
  }
}

@Composable
fun HeroTitle(
    modifier: Modifier = Modifier,
    titleTop: String,
    titleBottom: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: Int = 42,
    lineHeight: Int = 44,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    letterSpacing: Int = 0,
    textAlign: TextAlign = TextAlign.Start
) {
  Text(
      modifier = modifier,
      text = "$titleTop\n$titleBottom",
      lineHeight = lineHeight.sp,
      letterSpacing = letterSpacing.sp,
      textAlign = textAlign,
      style =
          MaterialTheme.typography.titleLarge.copy(
              color = textColor, fontSize = fontSize.sp, fontWeight = fontWeight))
}

@Composable
fun GoogleSignInButton(onSignInClick: () -> Unit) {
  Button(
      onClick = onSignInClick,
      colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), // Button color
      shape = RoundedCornerShape(25), // Circular edges for the button
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                  contentDescription = "Google Logo",
                  modifier =
                      Modifier.size(30.dp) // Size of the Google logo
                          .padding(end = 8.dp))

              // Text for the button
              Text(
                  text = "Sign in with Google",
                  color = MaterialTheme.colorScheme.onBackground, // Text color
                  fontSize = 16.sp, // Font size
                  fontWeight = FontWeight.Medium,
                  style = MaterialTheme.typography.bodyMedium)
            }
      }
}
