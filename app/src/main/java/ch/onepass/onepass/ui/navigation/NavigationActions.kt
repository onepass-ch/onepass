package ch.onepass.onepass.ui.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import ch.onepass.onepass.ui.navigation.NavigationDestinations.Screen

open class NavigationActions(
    private val navController: NavHostController,
) {
  /** Navigate to a [screen]. */
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

  private fun currentRoute(): String = navController.currentDestination?.route ?: ""

  private fun NavGraph.findStartDestination(): NavDestination {
    var current: NavDestination = this
    while (current is NavGraph) {
      current = current.findNode(current.startDestinationId)!!
    }
    return current
  }
}
