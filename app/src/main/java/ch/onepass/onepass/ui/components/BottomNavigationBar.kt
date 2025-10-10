package ch.onepass.onepass.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.android.sample.R

/**
 * Represents a bottom navigation tab.
 */
sealed class Tab(
    val name: String,
    @DrawableRes val iconRes: Int,
    val destination: Screen
) {
    object Events : Tab(
        name = "Events",
        iconRes = R.drawable.ic_fire,
        destination = Screen.Events
    )
    object Tickets : Tab(
        name = "Tickets",
        iconRes = R.drawable.ic_tickets,
        destination = Screen.Tickets
    )
    object Map : Tab(
        name = "Map",
        iconRes = R.drawable.ic_map,
        destination = Screen.Map
    )
    object Profile : Tab(
        name = "Profile",
        iconRes = R.drawable.ic_profile,
        destination = Screen.Profile
    )
}

private val tabs = listOf(
    Tab.Events,
    Tab.Tickets,
    Tab.Map,
    Tab.Profile
)

/**
 * Bottom navigation (Material 3).
 *
 * @param currentRoute The current route (e.g., navigationActions.currentRoute()).
 * @param onNavigate   Callback to navigate to a destination screen.
 * @param modifier     Optional modifier for the bar.
 */
@Composable
fun BottomNavigationMenu(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .height(85.dp),
        containerColor = Color(0xFF111118),
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
                    selectedIconColor = Color(0xFF8B5CF6),
                    selectedTextColor = Color(0xFF8B5CF6),
                    indicatorColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    unselectedIconColor = Color(0xFF9CA3AF),
                    unselectedTextColor = Color(0xFF9CA3AF)
                )
            )
        }
    }
}


@Preview(name = "BottomNav", showBackground = true, widthDp = 412, heightDp = 85)//figma container size
@Composable
private fun BottomNavigationBarPreview() {
    MaterialTheme {
        BottomNavigationMenu(
            currentRoute = Screen.Tickets.route,
            onNavigate = { /* no-op */ },
            modifier = Modifier
                .width(412.dp)      // force la largeur pour la preview Figma
                .height(85.dp)      // force la hauteur pour la preview Figma
        )
    }
}
