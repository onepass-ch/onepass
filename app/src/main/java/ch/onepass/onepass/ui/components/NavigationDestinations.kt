package ch.onepass.onepass.ui.components

import androidx.annotation.DrawableRes
import ch.onepass.onepass.R

/**
 * Centralizes all navigation destinations and bottom navigation tabs used in the app.
 *
 * This object provides a single source of truth for defining:
 * - the app's navigation routes (screens),
 * - and the bottom navigation bar tabs linked to those routes.
 *
 * Keeping all navigation definitions in one place ensures consistency and makes it easier to update
 * or extend the navigation structure.
 */
object NavigationDestinations {

  /**
   * Represents a screen (or navigation route) in the app.
   *
   * @property route The unique string used by the navigation graph to identify this screen.
   * @property name A human-readable label for the screen.
   * @property isTopLevelDestination Indicates whether this screen appears as a top-level
   *   destination (i.e., directly accessible from the bottom navigation bar).
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
   * Represents a bottom navigation tab linked to a specific screen.
   *
   * @property name The title displayed below the tab icon.
   * @property iconRes The drawable resource representing the tab icon.
   * @property destination The corresponding [Screen] this tab navigates to.
   */
  sealed class Tab(val name: String, @DrawableRes val iconRes: Int, val destination: Screen) {
    object Events : Tab("Events", R.drawable.ic_fire, Screen.Events)

    object Tickets : Tab("Tickets", R.drawable.ic_tickets, Screen.Tickets)

    object Map : Tab("Map", R.drawable.ic_map, Screen.Map)

    object Profile : Tab("Profile", R.drawable.ic_profile, Screen.Profile)
  }

  /**
   * The list of tabs displayed in the bottom navigation bar.
   *
   * The order of this list determines the order of appearance in the UI.
   */
  val tabs = listOf(Tab.Events, Tab.Tickets, Tab.Map, Tab.Profile)
}
