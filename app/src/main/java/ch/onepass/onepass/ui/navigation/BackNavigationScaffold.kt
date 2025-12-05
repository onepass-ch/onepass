package ch.onepass.onepass.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.DefaultBackground

/**
 * A Scaffold with a TopAppBar that includes a back navigation button.
 *
 * @param title The title to display in the TopAppBar.
 * @param onBack The callback to be invoked when the back button is pressed. If null, the composable
 *   will not be displayed.
 * @param modifier The Modifier to be applied to the Scaffold.
 * @param containerColor The background color of the Scaffold.
 * @param topBarTestTag Optional test tag for the TopAppBar.
 * @param backButtonTestTag Optional test tag for the back button.
 * @param titleTestTag Optional test tag for the title text.
 * @param subtitle Optional subtitle text to display below the title.
 * @param subtitleTestTag Optional test tag for the subtitle text.
 * @param actions Composable lambda for additional action icons in the TopAppBar.
 * @param content Composable lambda for the main content of the Scaffold, receiving PaddingValues.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackNavigationScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = DefaultBackground,
    topBarTestTag: String? = null,
    backButtonTestTag: String? = null,
    titleTestTag: String? = null,
    subtitle: String? = null,
    subtitleTestTag: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
  // Exit composable early if onBack is null
  if (onBack == null) {
    // Simple TopAppBar without back navigation
    TopAppBar(
        title = {
          Text(
              text = title,
              color = colorResource(id = R.color.white),
              style = MaterialTheme.typography.titleLarge,
              modifier = titleTestTag?.let { Modifier.testTag(it) } ?: Modifier)
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
        modifier = topBarTestTag?.let { Modifier.testTag(it) } ?: Modifier)
  } else {
    // Full Scaffold with TopAppBar and back navigation
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = containerColor,
        topBar = {
          TopAppBar(
              title = {
                Column {
                  Text(
                      text = title,
                      color = colorResource(id = R.color.white),
                      style = MaterialTheme.typography.titleLarge,
                      modifier = titleTestTag?.let { Modifier.testTag(it) } ?: Modifier)
                  subtitle?.let {
                    Text(
                        text = it,
                        color = colorResource(id = R.color.gray),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = subtitleTestTag?.let { Modifier.testTag(it) } ?: Modifier)
                  }
                }
              },
              navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = backButtonTestTag?.let { Modifier.testTag(it) } ?: Modifier) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                          contentDescription = "Back",
                          tint = colorResource(id = R.color.white))
                    }
              },
              actions = actions,
              colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
              modifier = topBarTestTag?.let { Modifier.testTag(it) } ?: Modifier)
        }) { padding ->
          content(padding)
        }
  }
}
