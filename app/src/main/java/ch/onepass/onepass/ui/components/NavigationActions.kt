package ch.onepass.onepass.ui.components

import androidx.navigation.NavHostController

/**
 * Represents a screen (destination) in the app with a unique route.
 */
sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false
) {
    object Events : Screen("events", "Events", true)
    object Tickets : Screen("tickets", "Tickets", true)
    object Map : Screen("map", "Map", true)
    object Profile : Screen("profile", "Profile", true)
}

/**
 * Handles navigation between different screens in the app.
 */
open class NavigationActions(
    private val navController: NavHostController,
) {

    /**
     * Navigates to the given [screen].
     *
     * @param screen The screen to navigate to.
     */
    open fun navigateTo(screen: Screen) {
        // Do nothing if we're already on this top-level screen
        if (screen.isTopLevelDestination && currentRoute() == screen.route) return

        navController.navigate(screen.route) {
            if (screen.isTopLevelDestination) {
                launchSingleTop = true
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            }
            restoreState = true
        }
    }

    /**
     * Navigates back to the previous screen.
     *
     * @return True if navigation was successful, false otherwise.
     */
    open fun goBack(): Boolean = navController.popBackStack()

    /**
     * Returns the current route.
     *
     * @return The current route as a string.
     */
    open fun currentRoute(): String = navController.currentDestination?.route ?: ""
}
