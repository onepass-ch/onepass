package ch.onepass.onepass.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.R

/**
 * A simple "Coming Soon" screen to indicate that a feature is not yet available.
 *
 * @param onBack Lambda to be invoked when the back button is clicked.
 */
@Composable
fun ComingSoonScreen(onBack: () -> Unit) {
  Surface(
      modifier = Modifier.fillMaxSize(),
      color = colorScheme.background // dark background from theme
      ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              // App logo (same as Auth screen)
              Image(
                  painter = painterResource(id = R.drawable.logo_qr_map),
                  contentDescription = stringResource(id = R.string.app_name),
                  modifier = Modifier.size(240.dp).padding(bottom = 32.dp))
              Image(
                  painter = painterResource(id = R.drawable.logo_text),
                  contentDescription = stringResource(id = R.string.app_name),
                  modifier = Modifier.size(240.dp).padding(bottom = 32.dp))
              // Title text
              Text(
                  text = "Coming Soon",
                  style =
                      MaterialTheme.typography.headlineMedium.copy(
                          fontSize = 28.sp, fontWeight = FontWeight.Bold),
                  color = colorScheme.onBackground)

              Spacer(modifier = Modifier.height(12.dp))

              // Subtitle text
              Text(
                  text = "This feature isn’t ready yet.\nStay tuned for updates!",
                  style = MaterialTheme.typography.bodyLarge,
                  color = colorScheme.onBackground.copy(alpha = 0.75f),
                  lineHeight = 22.sp,
                  modifier = Modifier.padding(horizontal = 16.dp),
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center)

              Spacer(modifier = Modifier.height(32.dp))

              // Back button (styled for dark mode)
              Button(
                  onClick = onBack,
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = colorScheme.primary,
                          contentColor = colorScheme.onBackground),
                  modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)) {
                    Text("Go Back", style = MaterialTheme.typography.bodyLarge)
                  }
            }
      }
}
