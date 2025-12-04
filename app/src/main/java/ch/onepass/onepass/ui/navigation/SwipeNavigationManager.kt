package ch.onepass.onepass.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.navigation.NavHostController

/**
 * Manages the list of screens that support swipe navigation.
 *
 * @param navController The NavHostController used for navigation.
 */
class SwipeNavigationManager(val navController: NavHostController) {
  /** List of registered screens that support swipe navigation. */
  private val _screens = mutableStateListOf<NavigationDestinations.Screen>()
  /** Public read-only access to the list of registered screens. */
  val screens: List<NavigationDestinations.Screen>
    get() = _screens

  /**
   * Registers a screen to support swipe navigation if not already registered.
   *
   * @param screen The screen to register.
   */
  fun register(screen: NavigationDestinations.Screen) {
    if (screen !in _screens) _screens.add(screen)
  }
}
