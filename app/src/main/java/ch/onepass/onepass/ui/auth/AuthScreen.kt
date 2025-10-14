package ch.onepass.onepass.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import ch.onepass.onepass.R

object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_BUTTON = "loginButton"
}

@Composable
@Preview
fun AuthScreen(onSignedIn: () -> Unit = {}, authViewModel: AuthViewModel = AuthViewModel()) {
  val context = LocalContext.current
  val credentialManager = remember { CredentialManager.create(context) }
  val uiState by authViewModel.uiState.collectAsState()
  val isLoading = uiState.isLoading

  // The main container for the screen
  Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color(0xFF0F0F0F),
      content = { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
          Logo()

          Spacer(modifier = Modifier.height(100.dp))

          // Authenticate With Google Button
          if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
          } else {
            GoogleSignInButton(onSignInClick = { authViewModel.signIn(context, credentialManager) })
            onSignedIn()
          }
        }
      })
}

@Composable
fun Logo() {
  val logoImageSize = 200.dp
  val logoTextSize = 150.dp
  val logoSpacer = 16.dp
  val maxLogoHeight = logoImageSize + logoSpacer + logoTextSize

  var isTicketLogo by remember { mutableStateOf(false) }

  val ticketAlpha by
      animateFloatAsState(
          targetValue = if (isTicketLogo) 1f else 0f,
          animationSpec = tween(durationMillis = 300),
          label = "ticketAlpha")
  val comboAlpha by
      animateFloatAsState(
          targetValue = if (isTicketLogo) 0f else 1f,
          animationSpec = tween(durationMillis = 300),
          label = "comboAlpha")

  Box(
      modifier =
          Modifier.height(maxLogoHeight)
              .fillMaxWidth()
              .clickable { isTicketLogo = !isTicketLogo }
              .testTag(SignInScreenTestTags.APP_LOGO),
      contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center).alpha(comboAlpha)) {
              Image(
                  painter = painterResource(id = R.drawable.logo_qr_map),
                  contentDescription = "App Logo",
                  modifier = Modifier.size(logoImageSize))
              Spacer(modifier = Modifier.height(logoSpacer))
              Image(
                  painter = painterResource(id = R.drawable.logo_text),
                  contentDescription = "App Logo Text",
                  modifier = Modifier.size(logoTextSize))
            }

        Image(
            painter = painterResource(id = R.drawable.logo_ticket),
            contentDescription = "Ticket Logo",
            modifier = Modifier.align(Alignment.Center).size(300.dp).alpha(ticketAlpha))
      }
}

@Composable
fun GoogleSignInButton(onSignInClick: () -> Unit) {
  Button(
      onClick = onSignInClick,
      colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), // Button color
      shape = RoundedCornerShape(25), // Circular edges for the button
      border = BorderStroke(1.dp, Color.LightGray),
      modifier =
          Modifier.padding(8.dp)
              .height(48.dp) // Adjust height as needed
              .fillMaxWidth(0.8f)
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
                  color = Color.White, // Text color
                  fontSize = 16.sp, // Font size
                  fontWeight = FontWeight.Medium)
            }
      }
}
