package ch.onepass.onepass.ui.components

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.navigation.BottomNavigationBar
import ch.onepass.onepass.ui.navigation.NavigationDestinations
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the [ch.onepass.onepass.ui.navigation.BottomNavigationBar] component.
 *
 * These tests verify:
 * - All four tabs (Events, Tickets, Map, Profile) are visible.
 * - Clicking each tab updates the `currentRoute` via the `onNavigate` callback.
 * - The bottom bar remains usable even when the initial route is unknown.
 * - The functional order of tabs matches
 *   [ch.onepass.onepass.ui.navigation.NavigationDestinations.tabs].
 * - Re-clicking the currently selected tab is idempotent (does not change state).
 *
 * The test scene exposes the current route through a small [Text] node with a testTag, so
 * assertions can be performed without a full NavHost graph.
 */
class BottomNavigationBarTest {

  /** Compose test rule that drives the composition under test. */
  @get:Rule val composeTestRule = createComposeRule()

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  /**
   * Test tag used to expose the current route as a plain [Text] node, allowing stable assertions on
   * navigation outcomes.
   */
  private val CURRENT_ROUTE_TAG = "current_route"

  /**
   * Sets up a minimal scene containing [ch.onepass.onepass.ui.navigation.BottomNavigationBar] and a
   * text field exposing the current route through [CURRENT_ROUTE_TAG]. The [onNavigate] callback
   * updates the local state, mimicking how a NavController would change routes.
   *
   * @param initialRoute The starting route for the bottom navigation; defaults to Events.
   */
  private fun setBottomNavigation(
      initialRoute: String = NavigationDestinations.Screen.Events.route
  ) {
    composeTestRule.setContent {
      var currentRoute by remember { mutableStateOf(initialRoute) }

      BottomNavigationBar(
          currentRoute = currentRoute, onNavigate = { screen -> currentRoute = screen.route })

      // Expose the current route for assertions.
      Text(text = currentRoute, modifier = Modifier.testTag(CURRENT_ROUTE_TAG))
    }
  }

  /** Ensures that all four tabs are rendered and visible. */
  @Test
  fun allTabsAreVisible() {
    setBottomNavigation()
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_events_description))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_tickets_description))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_map_description))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_profile_description))
        .assertIsDisplayed()
  }

  /** Verifies that clicking the "Events" tab updates the current route accordingly. */
  @Test
  fun clickingEventsUpdatesRoute() {
    setBottomNavigation(initialRoute = NavigationDestinations.Screen.Tickets.route)
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_events_description))
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Events.route)
  }

  /** Verifies that clicking the "Tickets" tab updates the current route accordingly. */
  @Test
  fun clickingTicketsUpdatesRoute() {
    setBottomNavigation(initialRoute = NavigationDestinations.Screen.Events.route)
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_tickets_description))
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Tickets.route)
  }

  /** Verifies that clicking the "Map" tab updates the current route accordingly. */
  @Test
  fun clickingMapUpdatesRoute() {
    setBottomNavigation(initialRoute = NavigationDestinations.Screen.Events.route)
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_map_description))
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Map.route)
  }

  /** Verifies that clicking the "Profile" tab updates the current route accordingly. */
  @Test
  fun clickingProfileUpdatesRoute() {
    setBottomNavigation(initialRoute = NavigationDestinations.Screen.Events.route)
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_profile_description))
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Profile.route)
  }

  /** Exercises navigation through all tabs in sequence and asserts the route after each click. */
  @Test
  fun canNavigateBetweenAllTabs() {
    setBottomNavigation(initialRoute = NavigationDestinations.Screen.Events.route)

    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_tickets_description))
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Tickets.route)

    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_map_description))
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Map.route)

    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_profile_description))
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Profile.route)

    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_events_description))
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Events.route)
  }

  /**
   * Confirms that the functional order of tabs matches [NavigationDestinations.tabs]. For each
   * label in order, we click and verify that the exposed route matches the expected destination.
   */
  @Test
  fun tabsOrderMatchesNavigationDestinations() {
    setBottomNavigation()
    val expected =
        listOf(
            context.getString(R.string.bottom_nav_events_description),
            context.getString(R.string.bottom_nav_tickets_description),
            context.getString(R.string.bottom_nav_map_description),
            context.getString(R.string.bottom_nav_profile_description))

    expected.forEachIndexed { idx, label ->
      // Ensure the label is present in the tree.
      composeTestRule.onAllNodesWithText(label, substring = false)[0].assertIsDisplayed()
      // Click the tab and confirm the route matches the destination at the same index.
      composeTestRule.onNodeWithText(label).performClick()
      composeTestRule
          .onNodeWithTag(CURRENT_ROUTE_TAG)
          .assertTextContains(NavigationDestinations.tabs[idx].destination.route)
    }
  }

  /** Validates idempotency: clicking the currently selected tab should not change the route. */
  @Test
  fun clickingSelectedTabIsIdempotent() {
    setBottomNavigation(initialRoute = NavigationDestinations.Screen.Events.route)

    // First click: switch to Tickets.
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_tickets_description))
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Tickets.route)

    // Second click on the same "Tickets" tab: route remains unchanged.
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_tickets_description))
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Tickets.route)
  }

  /**
   * Ensures the bottom bar remains usable when provided an unknown initial route: tabs remain
   * visible and clicking an item sets a valid route.
   */
  @Test
  fun unknownCurrentRouteKeepsBarUsable() {
    setBottomNavigation(initialRoute = "unknown_route")
    composeTestRule
        .onNodeWithText(context.getString(R.string.bottom_nav_events_description))
        .assertIsDisplayed()
        .performClick()
    composeTestRule
        .onNodeWithTag(CURRENT_ROUTE_TAG)
        .assertTextContains(NavigationDestinations.Screen.Events.route)
  }
}
