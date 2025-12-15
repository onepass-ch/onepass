package ch.onepass.onepass.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Generic loading state indicator. */
@Composable
fun LoadingState(modifier: Modifier = Modifier, testTag: String = "loading_indicator") {
  CircularProgressIndicator(
      modifier = modifier.testTag(testTag),
      color = colorScheme.primary,
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
        color = colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = error,
        style = MaterialTheme.typography.bodyMedium,
        color = colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onRetry,
        modifier = Modifier.testTag("${testTag}_retry_button"),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary, contentColor = colorScheme.onBackground),
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
        color = colorScheme.onBackground)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
  }
}
