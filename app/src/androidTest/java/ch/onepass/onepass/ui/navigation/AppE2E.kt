import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeRight
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.BuildConfig
import ch.onepass.onepass.OnePassApp
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.auth.SignInScreenTestTags
import ch.onepass.onepass.ui.eventdetail.EventDetailTestTags
import ch.onepass.onepass.ui.eventfilters.EventFilterDialogTestTags
import ch.onepass.onepass.ui.feed.FeedScreenTestTags
import ch.onepass.onepass.ui.map.MapScreenTestTags
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.myevents.MyEventsTestTags
import ch.onepass.onepass.ui.navigation.NavigationDestinations
import ch.onepass.onepass.ui.organization.OrganizationDashboardTestTags
import ch.onepass.onepass.ui.organization.OrganizationFeedTestTags
import ch.onepass.onepass.ui.organizer.OrganizationFormTestTags
import ch.onepass.onepass.ui.profile.ProfileTestTags
import ch.onepass.onepass.utils.TimeProvider
import ch.onepass.onepass.utils.TimeProviderHolder
import com.google.firebase.Timestamp
import com.mapbox.common.MapboxOptions
import io.mockk.every
import io.mockk.mockkStatic
import java.util.Date
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore("Disabled during full test suite runs")
class AppE2E {

  @get:Rule val compose = createComposeRule()

  @Before
  fun setup() {
    // Initialize fake time provider
    TimeProviderHolder.initialize(
        object : TimeProvider {
          override fun now(): Timestamp = Timestamp(Date(System.currentTimeMillis()))

          override fun currentDate(): Date = now().toDate()

          override suspend fun syncWithServer() {}
        })

    // Set Mapbox token (fake if missing)
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN.ifEmpty { "pk.test.fake_token" }

    // Mock location permissions as granted
    mockkStatic(ContextCompat::class)
    every {
      ContextCompat.checkSelfPermission(any(), Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_GRANTED

    every {
      ContextCompat.checkSelfPermission(any(), Manifest.permission.ACCESS_COARSE_LOCATION)
    } returns PackageManager.PERMISSION_GRANTED
  }

  private fun setApp() {
    compose.setContent {
      OnePassApp(
          enableDeepLinking = false,
          mapViewModel = viewModel<MapViewModel>(),
          testAuthButtonTag = SignInScreenTestTags.LOGIN_BUTTON,
          allowEventExactTime = true)
    }
    compose.waitForIdle()
  }

  /**
   * Full end-to-end navigation test for basic user interactions.
   *
   * Flow:
   * 1. Launch app and perform login.
   * 2. Verify feed screen is displayed.
   * 3. Scroll through all events in the feed and toggle the like/unlike button.
   * 4. Navigate to Tickets tab.
   * 5. Navigate to Map tab and pan/recenter the map.
   * 6. Navigate to Profile tab.
   * 7. Verify profile screen is displayed and perform sign out.
   * 8. Ensure the app returns to the Sign In screen.
   */
  @Test
  fun fullEndToEndNavigationTestM1() {
    setApp()

    // Login
    compose.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed().performClick()
    compose.waitForIdle()

    // Feed Screen
    compose.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    val eventNodes = compose.onAllNodes(hasTestTagStartingWith("eventItem_"))
    val eventCount = eventNodes.fetchSemanticsNodes().size

    for (index in 0 until eventCount) {
      val itemNode = eventNodes[index]
      itemNode.performScrollTo()
      compose.waitForIdle()

      val parentTag =
          itemNode.fetchSemanticsNode().config.getOrNull(SemanticsProperties.TestTag) ?: continue

      // Click Like
      compose
          .onNode(
              hasTestTag(C.Tag.event_card_like_button) and hasParent(hasTestTag(parentTag)),
              useUnmergedTree = true)
          .performClick()
      compose.waitForIdle()

      // Click Unlike
      compose
          .onNode(
              hasTestTag(C.Tag.event_card_like_button) and hasParent(hasTestTag(parentTag)),
              useUnmergedTree = true)
          .performClick()
      compose.waitForIdle()
    }

    // Tickets Screen
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Tickets.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    // Map Screen
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Map.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    // Map Screen Displayed
    compose.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Pan Map
    compose.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).performTouchInput {
      swipe(
          start = center,
          end = center.copy(x = center.x + 100, y = center.y + 50),
          durationMillis = 500)
    }
    compose.waitForIdle()

    // Recenter Map
    compose.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).assertIsDisplayed().performClick()
    compose.waitForIdle()

    // Profile Screen
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Profile.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    // Profile Screen Displayed
    compose.onNodeWithTag(ProfileTestTags.SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Sign Out
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_SIGN_OUT).performClick()
    compose.waitForIdle()

    // Back to Sign In Screen
    compose.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  /**
   * End-to-end test covering feed interactions, filters, map, profile, and organization creation.
   *
   * Flow:
   * 1. Launch app and login.
   * 2. Verify feed screen and toggle like/unlike on all events.
   * 3. Open first event and navigate to Event Detail screen, then swipe back to feed.
   * 4. Open filters dialog, select region "Zurich", and apply filters.
   * 5. Navigate through Tickets and Map tabs, pan and recenter map.
   * 6. Navigate to Profile, open Invitations, return, and start Create Organization flow.
   * 7. Fill out organization form and submit.
   * 8. Logout and ensure the app returns to the Sign In screen.
   */
  @Test
  fun fullEndToEndNavigationTestM2() {
    setApp()

    // Login
    compose.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed().performClick()
    compose.waitForIdle()

    // Feed Screen
    compose.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    val eventNodes = compose.onAllNodes(hasTestTagStartingWith("eventItem_"))
    val eventCount = eventNodes.fetchSemanticsNodes().size

    for (index in 0 until eventCount) {
      val itemNode = eventNodes[index]
      itemNode.performScrollTo()
      compose.waitForIdle()

      val parentTag =
          itemNode.fetchSemanticsNode().config.getOrNull(SemanticsProperties.TestTag) ?: continue

      // Click Like
      compose
          .onNode(
              hasTestTag(C.Tag.event_card_like_button) and hasParent(hasTestTag(parentTag)),
              useUnmergedTree = true)
          .performClick()
      compose.waitForIdle()

      // Click Unlike
      compose
          .onNode(
              hasTestTag(C.Tag.event_card_like_button) and hasParent(hasTestTag(parentTag)),
              useUnmergedTree = true)
          .performClick()
      compose.waitForIdle()
    }

    // Click first event to go to Event Detail
    if (eventNodes.fetchSemanticsNodes().isNotEmpty()) {
      eventNodes[0].performClick()
      compose.waitForIdle()
    }

    // Event Detail Screen
    compose.onNodeWithTag(EventDetailTestTags.SCREEN).assertIsDisplayed()

    swipeBack()
    compose.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Open Filters Dialog
    compose.onNodeWithTag(FeedScreenTestTags.FILTER_BUTTON).assertIsDisplayed().performClick()
    compose.waitForIdle()

    // Ensure Filters Dialog is displayed
    compose
        .onNodeWithTag(EventFilterDialogTestTags.FILTER_DIALOG, useUnmergedTree = true)
        .assertIsDisplayed()

    // Select Region "Zurich"
    compose
        .onNodeWithTag(EventFilterDialogTestTags.REGION_DROPDOWN, useUnmergedTree = true)
        .performClick()
    compose.waitForIdle()
    compose.onNodeWithText("Zurich").assertIsDisplayed().performClick()
    compose.waitForIdle()

    // Apply Filters
    compose
        .onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON, useUnmergedTree = true)
        .performClick()
    compose.waitForIdle()

    // Ticket Screen
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Tickets.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    // Organization Feed Screen
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Map.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    // Map Screen Displayed
    compose.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Pan Map
    compose.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).performTouchInput {
      swipe(
          start = center,
          end = center.copy(x = center.x + 100, y = center.y + 50),
          durationMillis = 500)
    }
    compose.waitForIdle()

    // Recenter Map
    compose.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).assertIsDisplayed().performClick()
    compose.waitForIdle()

    // Profile Screen
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Profile.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    // Profile Screen Displayed
    compose.onNodeWithTag(ProfileTestTags.SCREEN).assertIsDisplayed()

    // Invitations Screen
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_INVITATIONS).assertIsDisplayed().performClick()
    compose.waitForIdle()

    // Back to Profile Screen
    compose
        .onNodeWithContentDescription("Back", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .performClick()
    compose.waitForIdle()

    // Create Organization Flow
    compose.onNodeWithTag(ProfileTestTags.SCREEN).assertIsDisplayed()

    val orgButton = compose.onNodeWithTag(ProfileTestTags.ORG_CTA, useUnmergedTree = true)
    orgButton.assertIsDisplayed()
    orgButton.performClick()
    compose.waitForIdle()

    // Fill out Create Organization Form
    compose.onNodeWithTag(OrganizationFormTestTags.NAME_FIELD).performTextInput("Test Organization")
    compose
        .onNodeWithTag(OrganizationFormTestTags.DESCRIPTION_FIELD)
        .performTextInput("This is a test organization for Compose E2E test.")
    compose.onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD).performTextInput("791234567")
    compose.waitForIdle()

    // Submit the form
    compose.onNodeWithTag(OrganizationFormTestTags.SUBMIT_BUTTON).performScrollTo().performClick()
  }

  /**
   * End-to-end test for searching events, navigating MyEvents (Tickets/Market), and signing out.
   *
   * Flow:
   * 1. Launch app and login.
   * 2. Search for events in the feed and optionally navigate to Event Detail if results exist.
   * 3. Clear search and navigate to Tickets (MyEvents) tab.
   * 4. Switch to Market tab and interact with hot events if available.
   * 5. Perform search in Market tab and click first search result if available.
   * 6. Return to feed screen and verify display.
   * 7. Navigate to Profile and perform sign out.
   *
   * This test uses try/catch blocks at various points because the workflow depends on the state of
   * the Firebase repositories.
   *
   * For example, whether tickets exist in the repository or not can change the behavior of the app.
   * To test all possible workflows, one could modify the Firebase data and rerun the test. This
   * approach is optimal because we cannot add tickets directly through the app:
   * - Stripe integration requires a logged-in user, which cannot be bypassed in the app.
   * - In these tests, login is bypassed to avoid using real Gmail accounts (our login system only
   *   supports Gmail).
   *
   * Therefore, try/catch ensures the test can proceed even if certain data (like tickets or hot
   * events) is absent, allowing us to cover all flows without failing due to Firebase state.
   */
  @Test
  fun fullEndToEndNavigationTestM3First() {
    setApp()

    // Login
    compose.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed().performClick()
    compose.waitForIdle()

    // Feed Screen
    compose.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Click on search bar in feed screen
    compose.onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD).assertIsDisplayed().performClick()
    compose.waitForIdle()

    // Type in search query
    compose.onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD).performTextInput("event")
    compose.waitForIdle()

    try {
      // Check if there are any search result event cards
      compose.waitUntil(timeoutMillis = 3000) {
        val searchEventCards = compose.onAllNodes(hasTestTagStartingWith("searchEvent_"))
        searchEventCards.fetchSemanticsNodes().isNotEmpty()
      }

      // If there are search results, click on the first one
      val searchEventCards = compose.onAllNodes(hasTestTagStartingWith("searchEvent_"))
      if (searchEventCards.fetchSemanticsNodes().isNotEmpty()) {
        println("Found search results, clicking first event")
        searchEventCards[0].performClick()
        compose.waitForIdle()

        // Verify we're on Event Detail Screen
        compose.onNodeWithTag(EventDetailTestTags.SCREEN).assertIsDisplayed()
        compose.waitForIdle()

        // Swipe back to return to feed
        swipeBack()
        compose.waitForIdle()

        // Verify we're back on feed screen
        compose.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
      } else {
        println("No search results found for 'event', continuing with test")
      }
    } catch (e: AssertionError) {
      // No search results found, clear search and continue
      println("Search timed out or no results, clearing search")
    }

    // Clear the search text
    try {
      compose.onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD).performTextClearance()
    } catch (e: Exception) {
      compose.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).performClick()
    }
    compose.waitForIdle()

    // Navigate to Tickets screen (MyEvents)
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Tickets.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    // Switch to Market tab
    compose.onNodeWithTag(MyEventsTestTags.MAIN_TAB_MARKET).assertIsDisplayed().performClick()
    compose.waitForIdle()

    // Click on a hot event if available
    try {
      compose.waitUntil(timeoutMillis = 3000) {
        compose
            .onAllNodesWithTag(MyEventsTestTags.HOT_EVENTS_LIST)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      val hotEventCards = compose.onAllNodesWithTag(MyEventsTestTags.HOT_EVENT_CARD)
      if (hotEventCards.fetchSemanticsNodes().isNotEmpty()) {
        hotEventCards[0].performClick()
        compose.waitForIdle()
      }
    } catch (e: AssertionError) {
      // No hot events available, continue with test
      println("No hot events available, continuing test")
    }

    // Interact with search bar
    val searchBar = compose.onNodeWithTag(MyEventsTestTags.MARKET_SEARCH_BAR)
    searchBar.assertIsDisplayed()

    searchBar.performClick()
    compose.waitForIdle()

    // Type in search bar
    searchBar.performTextInput("Super Mega Super Event")
    compose.waitForIdle()

    // Check if there are search results
    try {
      compose.waitUntil(timeoutMillis = 2000) {
        val dropdownExists =
            compose
                .onAllNodes(hasParent(hasTestTag(MyEventsTestTags.MARKET_SEARCH_BAR)))
                .fetchSemanticsNodes()
                .isNotEmpty()
        dropdownExists
      }

      val eventResults = compose.onAllNodesWithText("Event •", substring = true)
      val organizerResults = compose.onAllNodesWithText("Organizer •", substring = true)
      val anySearchResult =
          eventResults.fetchSemanticsNodes().isNotEmpty() ||
              organizerResults.fetchSemanticsNodes().isNotEmpty()

      if (anySearchResult) {
        // Click on first result (either event or organizer)
        compose
            .onAllNodes(hasParent(hasTestTag(MyEventsTestTags.MARKET_SEARCH_BAR)))[0]
            .performClick()
        compose.waitForIdle()
      } else {
        // No results, dismiss
      }
    } catch (e: Exception) {
      // No results, dismiss
    }

    // Navigate back to feed
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Events.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    // Verify we're back on feed screen
    compose.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Sign out
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Profile.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(ProfileTestTags.SETTINGS_SIGN_OUT).performClick()
    compose.waitForIdle()

    // Back to Sign In Screen
    compose.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  /**
   * End-to-end test for creating an organization and an event in the Organization Dashboard.
   *
   * Flow:
   * 1. Launch app and login.
   * 2. Navigate to Profile tab and start Create Organization flow.
   * 3. Fill organization creation form including name, description, email, phone, website, and
   *    social links.
   * 4. Submit organization form and wait for the organization feed to display.
   * 5. Navigate to the created organization's dashboard.
   * 6. Fill and submit a new event creation form including title, description, time, date,
   *    location, price, capacity, and category.
   * 7. Verify the event creation completes and return to Profile.
   * 8. Sign out and ensure return to Sign In screen.
   */
  @Test
  fun fullEndToEndNavigationTestM3Second() {
    setApp()

    compose.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed().performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Navigate to Profile
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Profile.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(ProfileTestTags.SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Click on Create Organization button
    compose
        .onNodeWithTag(ProfileTestTags.ORG_CTA, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    compose.waitForIdle()

    // Fill organization creation form
    compose
        .onNodeWithTag(OrganizationFormTestTags.NAME_FIELD)
        .performScrollTo()
        .performTextInput("Test Organization M3")
    compose.waitForIdle()

    compose
        .onNodeWithTag(OrganizationFormTestTags.DESCRIPTION_FIELD)
        .performScrollTo()
        .performTextInput("This is a test organization for M3 E2E test.")
    compose.waitForIdle()

    compose
        .onNodeWithTag(OrganizationFormTestTags.EMAIL_FIELD)
        .performScrollTo()
        .performTextInput("test@email.com")
    compose.waitForIdle()

    compose.onNodeWithTag(OrganizationFormTestTags.PHONE_PREFIX).performScrollTo().performClick()
    compose.onNode(hasText("American Samoa +1")).performScrollTo().performClick()

    compose
        .onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD)
        .performScrollTo()
        .performTextInput("123456789")
    compose.waitForIdle()

    compose
        .onNodeWithTag(OrganizationFormTestTags.WEBSITE_FIELD)
        .performScrollTo()
        .performTextInput("example.com")
    compose.waitForIdle()

    compose
        .onNodeWithTag(OrganizationFormTestTags.INSTAGRAM_FIELD)
        .performScrollTo()
        .performTextInput("test_insta")
    compose.waitForIdle()

    compose
        .onNodeWithTag(OrganizationFormTestTags.FACEBOOK_FIELD)
        .performScrollTo()
        .performTextInput("test_fb")
    compose.waitForIdle()

    compose
        .onNodeWithTag(OrganizationFormTestTags.TIKTOK_FIELD)
        .performScrollTo()
        .performTextInput("test_tiktok")
    compose.waitForIdle()

    compose
        .onNodeWithTag(OrganizationFormTestTags.ADDRESS_FIELD)
        .performScrollTo()
        .performTextInput("123 Test Street")
    compose.waitForIdle()

    // Submit the form
    compose.onNodeWithTag(OrganizationFormTestTags.SUBMIT_BUTTON).performScrollTo().performClick()
    compose.waitForIdle()

    // Wait for the organization feed to load
    compose.waitUntil(timeoutMillis = 5000) {
      compose
          .onAllNodesWithTag(OrganizationFeedTestTags.ORGANIZATION_FEED_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(OrganizationFeedTestTags.ORGANIZATION_FEED_SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Verify the title
    compose
        .onNodeWithTag(OrganizationFeedTestTags.ORGANIZATION_FEED_TITLE)
        .assertIsDisplayed()
        .assertTextEquals("MY ORGANIZATIONS")
    compose.waitForIdle()

    // Wait for our created organization to appear
    compose.waitUntil(timeoutMillis = 5000) {
      compose
          .onAllNodesWithTag(OrganizationFeedTestTags.ORGANIZATION_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Try to find the organization card by text content
    compose.waitUntil(timeoutMillis = 3000) {
      compose.onAllNodesWithText("Test Organization M3").fetchSemanticsNodes().isNotEmpty()
    }

    // Click on the first organization card that contains our text
    val orgCards = compose.onAllNodesWithText("Test Organization M3")
    if (orgCards.fetchSemanticsNodes().isNotEmpty()) {
      orgCards[0].performClick()
    } else {
      compose.onAllNodesWithText("Test Organization M3").onFirst().performClick()
    }
    compose.waitForIdle()

    // Now we are in Organization Dashboard
    compose.waitUntil(timeoutMillis = 5000) {
      compose
          .onAllNodesWithTag(OrganizationDashboardTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty() ||
          compose
              .onAllNodesWithTag(OrganizationDashboardTestTags.TITLE)
              .fetchSemanticsNodes()
              .isNotEmpty() ||
          compose
              .onAllNodesWithTag(OrganizationDashboardTestTags.BACK_BUTTON)
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    compose.onNodeWithTag(OrganizationDashboardTestTags.SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Verify organization name is displayed
    compose.waitUntil(timeoutMillis = 3000) {
      compose
          .onAllNodesWithText("Test Organization", substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onAllNodesWithText("Test Organization", substring = true).onFirst().assertExists()
    compose.waitForIdle()

    // Manage Events
    compose.onNodeWithTag(OrganizationDashboardTestTags.MANAGE_EVENTS_SECTION).assertIsDisplayed()
    compose.waitForIdle()
    compose
        .onNodeWithTag(OrganizationDashboardTestTags.CREATE_EVENT_BUTTON)
        .assertIsDisplayed()
        .performClick()
    compose.waitForIdle()

    // Create Event - Fill all required fields
    compose
        .onNodeWithTag("title_input_field")
        .assertIsDisplayed()
        .performTextInput("E2E Test Event")
    compose.waitForIdle()

    compose
        .onNodeWithTag("description_input_field")
        .assertIsDisplayed()
        .performTextInput("This is an event created during E2E testing for M3")
    compose.waitForIdle()

    compose.waitUntil(timeoutMillis = 2000) {
      compose.onAllNodesWithText("Start time").fetchSemanticsNodes().isNotEmpty() &&
          compose.onAllNodesWithText("End time").fetchSemanticsNodes().isNotEmpty()
    }

    val timePatternMatcher =
        SemanticsMatcher("Has time pattern") { node ->
          node.config.getOrNull(SemanticsProperties.Text)?.any { text ->
            text.text.contains(":") && text.text.matches(Regex("\\d{1,2}:\\d{2}"))
          } == true
        }

    compose.waitUntil(timeoutMillis = 2000) {
      compose.onAllNodes(timePatternMatcher).fetchSemanticsNodes().size >= 2
    }

    val timeNodes = compose.onAllNodes(timePatternMatcher)

    if (timeNodes.fetchSemanticsNodes().size >= 2) {
      timeNodes[0].performScrollTo()
      timeNodes[0].assertIsDisplayed()
      timeNodes[0].performClick()

      compose.waitUntil(timeoutMillis = 2000) {
        compose.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
      }
      compose.onAllNodesWithText("OK").onFirst().performClick()
      compose.waitForIdle()

      timeNodes[1].performScrollTo()
      timeNodes[1].assertIsDisplayed()
      timeNodes[1].performClick()
      compose.waitUntil(timeoutMillis = 3000) {
        compose.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
      }
      compose.onAllNodesWithText("OK").onFirst().performClick()
      compose.waitForIdle()
    }

    compose.onNodeWithText("Select date").performScrollTo().assertIsDisplayed().performClick()
    compose.waitForIdle()

    compose.waitUntil(timeoutMillis = 3000) {
      compose.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
    }

    compose.waitUntil(timeoutMillis = 3000) {
      compose.onAllNodesWithText("26", substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    compose.onNodeWithText("26", substring = true).performClick()

    compose.onNodeWithText("OK").performClick()
    compose.waitForIdle()

    compose.onNodeWithTag("location_input_field").assertIsDisplayed().performTextInput("Lausanne")
    compose.waitForIdle()

    compose.waitUntil(timeoutMillis = 5000) {
      val locationNodes = compose.onAllNodesWithText("Lausanne", substring = true)
      locationNodes.fetchSemanticsNodes().size > 1
    }

    val locationSuggestions = compose.onAllNodesWithText("Lausanne", substring = true)
    if (locationSuggestions.fetchSemanticsNodes().size > 1) {
      locationSuggestions[1].performClick()
    } else {
      compose.waitUntil(timeoutMillis = 3000) {
        compose
            .onAllNodes(hasParent(hasTestTag("location_search_field")))
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      val dropdownItems = compose.onAllNodes(hasParent(hasTestTag("location_search_field")))
      if (dropdownItems.fetchSemanticsNodes().isNotEmpty()) {
        dropdownItems[0].performClick()
      }
    }
    compose.waitForIdle()

    compose
        .onNodeWithTag("price_input_field")
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput("25.50")
    compose.waitForIdle()

    compose
        .onNodeWithTag("capacity_input_field")
        .performScrollTo()
        .assertIsDisplayed()
        .performTextInput("100")
    compose.waitForIdle()

    compose.onNodeWithText("Technology").performScrollTo().assertIsDisplayed().performClick()
    compose.waitForIdle()

    compose.onNodeWithText("Create event").performScrollTo().assertIsDisplayed().performClick()
    compose.waitForIdle()

    compose.waitUntil(timeoutMillis = 10000) {
      !compose.onAllNodesWithTag("title_input_field").fetchSemanticsNodes().isNotEmpty()
    }

    // Go back to Profile Screen
    swipeBack()
    swipeBack()

    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Profile.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(ProfileTestTags.SCREEN).assertIsDisplayed()
    compose.waitForIdle()

    // Sign out
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_SIGN_OUT).performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  private fun swipeBack() {
    compose.onRoot().performTouchInput { swipeRight() }
    compose.waitForIdle()
  }

  private fun hasTestTagStartingWith(prefix: String) =
      SemanticsMatcher("Has testTag starting with '$prefix'") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
      }
}
