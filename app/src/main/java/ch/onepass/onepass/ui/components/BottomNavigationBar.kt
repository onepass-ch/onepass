package ch.onepass.onepass.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.ui.components.NavigationDestinations.Screen
import ch.onepass.onepass.ui.components.NavigationDestinations.tabs

/**
 * A composable implementation of the bottom navigation bar (Material 3).
 *
 * This component displays the app’s main navigation tabs at the bottom of the screen, allowing
 * users to switch between top-level destinations such as *Events*, *Tickets*, *Map*, and *Profile*.
 *
 * The design follows the app’s visual identity and Material 3 guidelines.
 *
 * @param currentRoute The current navigation route, usually obtained from
 *   [NavigationActions.currentRoute].
 * @param onNavigate A callback invoked when the user selects a tab. This should navigate to the
 *   associated [Screen].
 * @param modifier Optional [Modifier] to customize layout or styling.
 *
 * ### Behavior
 * - Highlights the active tab based on [currentRoute].
 * - Uses distinct colors for selected and unselected states.
 * - Restores state when reselecting a top-level destination.
 *
 * ### Example
 *
 * ```
 * BottomNavigationMenu(
 *     currentRoute = navActions.currentRoute(),
 *     onNavigate = { navActions.navigateTo(it) }
 * )
 * ```
 */
@Composable
fun BottomNavigationMenu(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
  NavigationBar(
      modifier = modifier.fillMaxWidth().height(85.dp),
      containerColor = Color(0xFF111118),
      tonalElevation = 0.dp,
      windowInsets = NavigationBarDefaults.windowInsets) {
        tabs.forEach { tab ->
          val selected = currentRoute == tab.destination.route
          NavigationBarItem(
              selected = selected,
              onClick = { onNavigate(tab.destination) },
              icon = {
                Icon(painter = painterResource(id = tab.iconRes), contentDescription = tab.name)
              },
              label = { Text(tab.name) },
              alwaysShowLabel = true,
              colors =
                  NavigationBarItemDefaults.colors(
                      selectedIconColor = Color(0xFF8B5CF6),
                      selectedTextColor = Color(0xFF8B5CF6),
                      indicatorColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                      unselectedIconColor = Color(0xFF9CA3AF),
                      unselectedTextColor = Color(0xFF9CA3AF)))
        }
      }
}

/**
 * A design-time preview of the [BottomNavigationMenu] component.
 *
 * This preview simulates a device width of 412dp and a height of 85dp, displaying the navigation
 * bar with the "Tickets" tab selected.
 */
@Preview(name = "BottomNav", showBackground = true, widthDp = 412, heightDp = 85)
@Composable
private fun BottomNavigationBarPreview() {
  MaterialTheme {
    BottomNavigationMenu(
        currentRoute = Screen.Tickets.route,
        onNavigate = { /* no-op for preview */},
        modifier = Modifier.width(412.dp).height(85.dp))
  }
}
