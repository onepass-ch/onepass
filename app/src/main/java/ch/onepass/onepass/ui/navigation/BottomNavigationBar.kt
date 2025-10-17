package ch.onepass.onepass.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.ui.navigation.NavigationDestinations.Screen
import ch.onepass.onepass.ui.navigation.NavigationDestinations.tabs
import ch.onepass.onepass.ui.theme.BackgroundDark
import ch.onepass.onepass.ui.theme.GrayStroke
import ch.onepass.onepass.ui.theme.OnePassTheme
import ch.onepass.onepass.ui.theme.PurplePrimary

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
  val itemColors =
      NavigationBarItemDefaults.colors(
          selectedIconColor = PurplePrimary,
          selectedTextColor = PurplePrimary,
          indicatorColor = PurplePrimary.copy(alpha = 0.12f),
          unselectedIconColor = GrayStroke,
          unselectedTextColor = GrayStroke)

  NavigationBar(
      modifier = modifier.fillMaxWidth(),
      containerColor = BackgroundDark,
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

@Preview(name = "BottomNav (Dark)", showBackground = true, widthDp = 412, heightDp = 85)
@Composable
private fun BottomNavigationBarPreviewDark() {
  OnePassTheme(darkTheme = true) {
    BottomNavigationBar(
        currentRoute = Screen.Tickets.route,
        onNavigate = {},
        modifier = Modifier.width(412.dp).height(85.dp))
  }
}
