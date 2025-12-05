package ch.onepass.onepass.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.Background
import ch.onepass.onepass.ui.theme.OnBackground
import ch.onepass.onepass.ui.theme.Primary
import ch.onepass.onepass.ui.theme.Surface

@Composable
fun ComingSoonScreen(onBack: () -> Unit) {
  Surface(modifier = Modifier.fillMaxSize(), color = Background) {
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
              color = OnBackground)

          Spacer(modifier = Modifier.height(12.dp))

          // Subtitle text
          Text(
              text = "This feature isn’t ready yet.\nStay tuned for updates!",
              style = MaterialTheme.typography.bodyLarge,
              color = OnBackground.copy(alpha = 0.75f),
              lineHeight = 22.sp,
              modifier = Modifier.padding(horizontal = 16.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center)

          Spacer(modifier = Modifier.height(32.dp))

          // Back button (styled for dark mode)
          Button(
              onClick = onBack,
              colors =
                  ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Surface),
              modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)) {
                Text("Go Back", style = MaterialTheme.typography.bodyLarge)
              }
        }
  }
}
