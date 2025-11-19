package ch.onepass.onepass.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import ch.onepass.onepass.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.ui.theme.EventDateColor

/** Generic loading state indicator. */
@Composable
fun LoadingState(modifier: Modifier = Modifier, testTag: String = "loading_indicator") {
  CircularProgressIndicator(
      modifier = modifier.testTag(testTag),
      color = EventDateColor,
  )
}

/** Generic error state with retry button. */
@Composable
fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "error_message"
) {
  Column(
      modifier = modifier.fillMaxWidth().padding(32.dp).testTag(testTag),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = "Oops!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = colorResource(R.color.white),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = error,
        style = MaterialTheme.typography.bodyMedium,
        color = colorResource(R.color.state_display_text),
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onRetry,
        modifier = Modifier.testTag("${testTag}_retry_button"),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = EventDateColor,
                contentColor = colorResource(R.color.white),
            ),
    ) {
      Text(text = "Try Again", fontWeight = FontWeight.Medium)
    }
  }
}

/** Generic empty state with a message. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    testTag: String = "empty_state"
) {
  Column(
      modifier = modifier.fillMaxWidth().padding(32.dp).testTag(testTag),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = colorResource(R.color.white),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = colorResource(R.color.state_display_text),
        textAlign = TextAlign.Center,
    )
  }
}
