package ch.onepass.onepass.ui.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import ch.onepass.onepass.ui.navigation.NavigationDestinations.Screen

/**
 * Class encapsulating navigation actions within the app.
 *
 * @param navController The NavHostController used for navigation.
 */
open class NavigationActions(
    private val navController: NavHostController,
) {
  /**
   * Navigate to the specified [screen].
   *
   * If the screen is a top-level destination and is already the current route, no action is taken.
   * For top-level destinations, the back stack is popped up to the start destination to avoid
   * building up a large back stack.
   *
   * @param screen The target screen to navigate to.
   */
  open fun navigateTo(screen: Screen) {
    if (screen.isTopLevelDestination && currentRoute() == screen.route) return
    navController.navigate(screen.route) {
      if (screen.isTopLevelDestination) {
        launchSingleTop = true
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        restoreState = true
      }
    }
  }

  /**
   * Get the current navigation route.
   *
   * @return The current route as a String, or an empty string if no destination is found.
   */
  private fun currentRoute(): String = navController.currentDestination?.route ?: ""

  /**
   * Find the start destination of a NavGraph, traversing nested graphs if necessary.
   *
   * @return The start NavDestination.
   */
  private fun NavGraph.findStartDestination(): NavDestination {
    var current: NavDestination = this
    while (current is NavGraph) {
      current = current.findNode(current.startDestinationId)!!
    }
    return current
  }
}
