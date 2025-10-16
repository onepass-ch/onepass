package ch.onepass.onepass.ui.navigation

import androidx.annotation.DrawableRes
import ch.onepass.onepass.R

/** Centralizes all navigation destinations and bottom navigation tabs used in the app. */
object NavigationDestinations {

  /**
   * Represents a screen (navigation route).
   *
   * @property route Unique route string for NavController.
   * @property name Human-readable label.
   * @property isTopLevelDestination Appears on bottom bar when true.
   */
  sealed class Screen(
      val route: String,
      val name: String,
      val isTopLevelDestination: Boolean = false
  ) {
    // --- Top-level tabs ---
    object Events : Screen("events", "Events", true)

    object Tickets : Screen("tickets", "Tickets", true)

    object Map : Screen("map", "Map", true)

    object Profile : Screen("profile", "Profile", true)

    // --- Non top-level / flows ---
    object Auth : Screen("auth", "Authentication", false)

    object ComingSoon : Screen("coming_soon", "Coming Soon", false)

    // Parameterized routes
    object EventDetail : Screen("event/{eventId}", "Event Detail", false) {
      const val ARG_EVENT_ID = "eventId"

      fun route(eventId: String) = "event/$eventId"
    }

    // Not implemented yet → will redirect to ComingSoon for now
    object CreateEvent : Screen("create_event", "Create Event", false)
  }

  /** Bottom navigation tabs and their linked screens. */
  sealed class Tab(val name: String, @DrawableRes val iconRes: Int, val destination: Screen) {
    object Events : Tab("Events", R.drawable.ic_events, Screen.Events)

    object Tickets : Tab("Tickets", R.drawable.ic_tickets, Screen.Tickets)

    object Map : Tab("Map", R.drawable.ic_map, Screen.Map)

    object Profile : Tab("Profile", R.drawable.ic_profile, Screen.Profile)
  }
  /** Order determines appearance in the bottom bar. */
  val tabs = listOf(Tab.Events, Tab.Tickets, Tab.Map, Tab.Profile)
}
