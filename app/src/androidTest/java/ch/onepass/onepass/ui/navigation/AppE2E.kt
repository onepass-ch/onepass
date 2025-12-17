import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
import org.junit.Rule
import org.junit.Test

// @Ignore("Disabled during full test suite runs")
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
          testAuthButtonTag = SignInScreenTestTags.LOGIN_BUTTON)
    }
    compose.waitForIdle()
  }

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

    // Sign Out
    compose.onNodeWithTag(ProfileTestTags.SETTINGS_SIGN_OUT).performClick()
    compose.waitForIdle()

    // Back to Sign In Screen
    compose.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

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

    // Organization Form Screen
    compose.onNodeWithTag(OrganizationFormTestTags.SCROLL_COLUMN).assertIsDisplayed()

    // Fill out Create Organization Form
    compose.onNodeWithTag(OrganizationFormTestTags.NAME_FIELD).performTextInput("Test Organization")
    compose.waitForIdle()

    compose
        .onNodeWithTag(OrganizationFormTestTags.DESCRIPTION_FIELD)
        .performTextInput("This is a test organization for Compose E2E test.")
    compose.waitForIdle()

    compose
        .onNodeWithTag(OrganizationFormTestTags.EMAIL_FIELD)
        .performTextInput("test@organization.com")
    compose.waitForIdle()

    compose.onNodeWithTag(OrganizationFormTestTags.PHONE_FIELD).performTextInput("791234567")
    compose.waitForIdle()

    // Submit the form
    compose.onNodeWithTag(OrganizationFormTestTags.SUBMIT_BUTTON).performScrollTo().performClick()
    compose.waitForIdle()

    // Verify form submission success (back to profile or success screen)
    compose.onNodeWithTag(ProfileTestTags.SCREEN).assertIsDisplayed()
  }

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

  private fun swipeBack() {
    compose.onRoot().performTouchInput { swipeRight() }
    compose.waitForIdle()
  }

  private fun hasTestTagStartingWith(prefix: String) =
      SemanticsMatcher("Has testTag starting with '$prefix'") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
      }
}
