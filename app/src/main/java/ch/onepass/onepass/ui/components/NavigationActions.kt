package ch.onepass.onepass.ui.components

import androidx.navigation.NavHostController
import ch.onepass.onepass.ui.components.NavigationDestinations.Screen

/**
 * Provides centralized navigation logic for the application.
 *
 * This class wraps around the [NavHostController] to handle all navigation between the app’s
 * different [Screen] destinations in a consistent and predictable way.
 *
 * It is designed to:
 * - Prevent duplicate navigations to the same top-level destination.
 * - Manage back stack behavior and state restoration.
 * - Offer reusable, extendable navigation methods for ViewModels or UI components.
 *
 * Subclass this class if you need to extend or customize navigation behavior.
 *
 * Example usage:
 * ```
 * val navActions = NavigationActions(navController)
 * navActions.navigateTo(NavigationDestinations.Screen.Profile)
 * ```
 */
open class NavigationActions(
    private val navController: NavHostController,
) {

  /**
   * Navigates to the specified [screen].
   *
   * If the screen is a top-level destination and the user is already there, this method will return
   * early to avoid unnecessary recompositions.
   *
   * When navigating to a top-level destination, this method also:
   * - Launches the destination in single-top mode (avoids duplicate destinations).
   * - Pops up to the root of the navigation graph while preserving state.
   * - Restores any previously saved state for that destination.
   *
   * @param screen The [Screen] to navigate to.
   */
  open fun navigateTo(screen: Screen) {
    if (screen.isTopLevelDestination && currentRoute() == screen.route) return

    navController.navigate(screen.route) {
      if (screen.isTopLevelDestination) {
        launchSingleTop = true
        popUpTo(navController.graph.startDestinationId) { saveState = true }
      }
      restoreState = true
    }
  }

  /**
   * Navigates back to the previous screen in the back stack.
   *
   * @return `true` if the navigation action was successful, `false` if there is no screen to go
   *   back to.
   */
  open fun goBack(): Boolean = navController.popBackStack()

  /**
   * Returns the current navigation route.
   *
   * @return The current route as a [String], or an empty string if none is found.
   */
  open fun currentRoute(): String = navController.currentDestination?.route ?: ""
}
