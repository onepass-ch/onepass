package ch.onepass.onepass.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.ui.navigation.NavigationDestinations.Screen
import ch.onepass.onepass.ui.navigation.NavigationDestinations.tabs

/**
 * Bottom navigation bar for the main screens of the app.
 *
 * @param currentRoute The current navigation route to determine the selected tab.
 * @param onNavigate Lambda function to handle navigation when a tab is selected.
 * @param modifier Modifier for styling.
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
  val itemColors =
      NavigationBarItemDefaults.colors(
          selectedIconColor = colorScheme.primary,
          selectedTextColor = colorScheme.primary,
          indicatorColor = colorScheme.primary.copy(alpha = 0.12f),
          unselectedIconColor = colorScheme.outline,
          unselectedTextColor = colorScheme.outline)

  NavigationBar(
      modifier = modifier.fillMaxWidth(),
      containerColor = colorScheme.secondaryContainer,
      tonalElevation = 0.dp,
      windowInsets = NavigationBarDefaults.windowInsets) {
        tabs.forEach { tab ->
          val selected = currentRoute == tab.destination.route
          val tabTag =
              when (tab.destination.route) {
                Screen.Events.route -> "BOTTOM_TAB_EVENTS"
                Screen.Tickets.route -> "BOTTOM_TAB_TICKETS"
                Screen.Map.route -> "BOTTOM_TAB_MAP"
                Screen.Profile.route -> "BOTTOM_TAB_PROFILE"
                else -> "BOTTOM_TAB_UNKNOWN"
              }

          NavigationBarItem(
              modifier = Modifier.testTag(tabTag),
              selected = selected,
              onClick = { onNavigate(tab.destination) },
              icon = {
                Icon(painter = painterResource(id = tab.iconRes), contentDescription = tab.name)
              },
              label = { Text(tab.name) },
              alwaysShowLabel = true,
              colors = itemColors)
        }
      }
}
