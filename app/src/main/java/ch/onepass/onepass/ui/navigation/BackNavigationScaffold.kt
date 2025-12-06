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

/** Configuration data class for BackNavigationScaffold */
data class TopBarConfig(
    val title: String,
    val subtitle: String? = null,
    val topBarTestTag: String? = null,
    val titleTestTag: String? = null,
    val subtitleTestTag: String? = null,
    val backButtonTestTag: String? = null,
    val actions: @Composable RowScope.() -> Unit = {}
)

/**
 * A Scaffold with a TopAppBar that includes a back navigation button.
 *
 * @param topBarConfig Configuration for the top bar including title, subtitle, and actions.
 * @param onBack Optional lambda function to handle back navigation. If null, no back button is
 *   displayed.
 * @param modifier Modifier for styling the Scaffold.
 * @param containerColor Background color of the Scaffold.
 * @param content Composable content to be displayed within the Scaffold.
 * @see TopBarConfig
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackNavigationScaffold(
    topBarConfig: TopBarConfig,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = DefaultBackground,
    content: @Composable (PaddingValues) -> Unit
) {
  // Exit composable early if onBack is null
  if (onBack == null) {
    // Simple TopAppBar without back navigation
    TopAppBar(
        title = {
          Text(
              text = topBarConfig.title,
              color = colorResource(id = R.color.white),
              style = MaterialTheme.typography.titleLarge,
              modifier = topBarConfig.titleTestTag?.let { Modifier.testTag(it) } ?: Modifier)
        },
        actions = topBarConfig.actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
        modifier = topBarConfig.topBarTestTag?.let { Modifier.testTag(it) } ?: Modifier)
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
                      text = topBarConfig.title,
                      color = colorResource(id = R.color.white),
                      style = MaterialTheme.typography.titleLarge,
                      modifier =
                          topBarConfig.titleTestTag?.let { Modifier.testTag(it) } ?: Modifier)
                  topBarConfig.subtitle?.let {
                    Text(
                        text = it,
                        color = colorResource(id = R.color.gray),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier =
                            topBarConfig.subtitleTestTag?.let { Modifier.testTag(it) } ?: Modifier)
                  }
                }
              },
              navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier =
                        topBarConfig.backButtonTestTag?.let { Modifier.testTag(it) } ?: Modifier) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                          contentDescription = "Back",
                          tint = colorResource(id = R.color.white))
                    }
              },
              actions = topBarConfig.actions,
              colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
              modifier = topBarConfig.topBarTestTag?.let { Modifier.testTag(it) } ?: Modifier)
        }) { padding ->
          content(padding)
        }
  }
}
