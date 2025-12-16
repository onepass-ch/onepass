package ch.onepass.onepass.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- Core color schemes ---
private val DarkColorScheme =
    darkColorScheme(
        primary = Primary,
        secondary = Primary,
        background = Background,
        primaryContainer = Primary,
        secondaryContainer = BackgroundSecondary,
        surface = Surface,
        onBackground = OnBackground,
        onSurface = OnSurface,
        outline = UnSelected,
        error = Error)

private val LightColorScheme =
    lightColorScheme(
        primary = Primary,
        secondary = Primary,
        background = Background,
        primaryContainer = Primary,
        secondaryContainer = BackgroundSecondary,
        surface = Surface,
        onBackground = OnBackground,
        onSurface = OnSurface,
        outline = UnSelected,
        error = Error)

/**
 * Dynamic theme wrapper supporting:
 * - Dark/Light mode
 * - Dynamic system colors (Android 12+)
 * - Status bar color and contrast handling
 */
@Composable
fun OnePassTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    primaryOverride: Color? = null,
    secondaryOverride: Color? = null,
    content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }.let { baseScheme ->
        // Apply optional overrides dynamically
        baseScheme.copy(
            primary = primaryOverride ?: baseScheme.primary,
            secondary = secondaryOverride ?: baseScheme.secondary)
      }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = Background.toArgb()

      // Adjust status bar content to ensure contrast
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
