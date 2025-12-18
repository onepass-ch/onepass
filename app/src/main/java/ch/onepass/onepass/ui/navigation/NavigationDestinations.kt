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

    object MyInvitations : Screen("my_invitations", "My Invitations", false)

    object Notification : Screen("notifications", "Notifications", false)

    object AccountSettings : Screen("account_settings", "Account Settings", false)

    object EditProfile : Screen("edit_profile", "Edit Profile", false)

    // Parameterized routes
    /*
     * ----------- PARAMETERIZED ROUTES -----------
     *
     * NOTE: These retain the same public API:
     * - ARG_<paramName> constant for the argument name
     * - route(...) function to build the route with parameters
     */
    object EventDetail : Screen("event/{eventId}", "Event Detail", false) {
      const val ARG_EVENT_ID = "eventId"

      fun route(eventId: String) = "event/$eventId"
    }

    object CreateEvent : Screen("create_event/{organizationId}", "Create Event", false) {
      private const val BASE_ROUTE = "create_event"
      const val ARG_ORGANIZATION_ID = "organizationId"

      fun route(organizationId: String) = "$BASE_ROUTE/$organizationId"
    }

    object EditEvent : Screen("edit_event/{eventId}", "Edit Event", false) {
      const val ARG_EVENT_ID = "eventId"

      fun route(eventId: String) = "edit_event/$eventId"
    }

    object Scan : Screen("scan/{eventId}", "Scan Tickets", false) {
      const val ARG_EVENT_ID = "eventId"

      fun route(eventId: String) = "scan/$eventId"
    }

    object BecomeOrganizer : Screen("become_organizer", "Become Organizer", false)

    object OrganizationFeed : Screen("organization_feed", "My Organizations", false)

    object OrganizationDashboard :
        Screen("organization/{organizationId}", "Organization Dashboard", false) {
      const val ARG_ORGANIZATION_ID = "organizationId"

      fun route(organizationId: String) = "organization/$organizationId"
    }

    object StaffInvitation :
        Screen("staff_invitation/{organizationId}", "Staff Invitation", false) {
      const val ARG_ORGANIZATION_ID = "organizationId"

      fun route(organizationId: String) = "staff_invitation/$organizationId"
    }

    object OrganizationProfile :
        Screen("organization_profile/{organizationId}", "Organization Profile", false) {
      const val ARG_ORGANIZATION_ID = "organizationId"

      fun route(organizationId: String) = "organization_profile/$organizationId"
    }

    object EditOrganization :
        Screen("edit_organization/{organizationId}", "Edit Organization", false) {
      private const val BASE_ROUTE = "edit_organization"
      const val ARG_ORGANIZATION_ID = "organizationId"

      fun route(organizationId: String) = "$BASE_ROUTE/$organizationId"
    }
  }

  /**
   * Represents a top-level tab in the bottom navigation bar.
   *
   * @property name Human-readable label.
   * @property iconRes Drawable resource ID for the tab icon.
   * @property destination Corresponding Screen destination.
   */
  sealed class Tab(val name: String, @DrawableRes val iconRes: Int, val destination: Screen) {
    object Events : Tab("Events", R.drawable.ic_events, Screen.Events)

    object Tickets : Tab("Tickets", R.drawable.ic_tickets, Screen.Tickets)

    object Map : Tab("Map", R.drawable.ic_map, Screen.Map)

    object Profile : Tab("Profile", R.drawable.ic_profile, Screen.Profile)
  }

  /** List of all top-level tabs in the app. Order determines appearance in the bottom bar. */
  val tabs = listOf(Tab.Events, Tab.Tickets, Tab.Map, Tab.Profile)
}
