package ch.onepass.onepass.ui.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.ui.components.NavigationDestinations.Screen
import ch.onepass.onepass.ui.components.NavigationDestinations.tabs
import ch.onepass.onepass.ui.theme.OnePassTheme
import ch.onepass.onepass.ui.theme.PurplePrimary
import ch.onepass.onepass.ui.theme.BackgroundDark
import ch.onepass.onepass.ui.theme.GrayStroke

@Composable
fun BottomNavigationMenu(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = BackgroundDark, // ta couleur de fond dark
        tonalElevation = 0.dp,
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(tab.destination) },
                icon = {
                    Icon(
                        painter = painterResource(id = tab.iconRes),
                        contentDescription = tab.name
                    )
                },
                label = { Text(tab.name) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PurplePrimary,
                    selectedTextColor = PurplePrimary,
                    indicatorColor = PurplePrimary.copy(alpha = 0.12f),
                    unselectedIconColor = GrayStroke,
                    unselectedTextColor = GrayStroke
                )
            )
        }
    }
}

@Preview(name = "BottomNav (Dark)", showBackground = true, widthDp = 412, heightDp = 85)
@Composable
private fun BottomNavigationBarPreviewDark() {
    OnePassTheme(darkTheme = true) {
        BottomNavigationMenu(
            currentRoute = Screen.Tickets.route,
            onNavigate = { /* no-op for preview */ },
            modifier = Modifier.width(412.dp).height(85.dp)
        )
    }
}

