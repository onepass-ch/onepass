import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.BuildConfig
import ch.onepass.onepass.OnePassApp
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.auth.SignInScreenTestTags
import ch.onepass.onepass.ui.eventfilters.EventFilterDialogTestTags
import ch.onepass.onepass.ui.feed.FeedScreenTestTags
import ch.onepass.onepass.ui.map.MapScreenTestTags
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.myevents.MyEventsTestTags
import ch.onepass.onepass.ui.navigation.NavigationDestinations
import ch.onepass.onepass.ui.organizer.CreateOrganizationTestTags
import ch.onepass.onepass.ui.profile.ProfileTestTags
import com.mapbox.common.MapboxOptions
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore("Disabled during full test suite runs")
class AppE2E {

  @get:Rule val compose = createComposeRule()

  @Before
  fun setupMapboxToken() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
  }

  private fun setApp() {
    compose.setContent {
      OnePassApp(
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

    // Calendar
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Tickets.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    // Tickets Screen
    compose.onNodeWithTag(MyEventsTestTags.TABS_ROW).assertIsDisplayed()
    compose.onNodeWithTag(MyEventsTestTags.TAB_CURRENT).performClick()
    compose.waitForIdle()
    val currentTickets = compose.onAllNodesWithTag(MyEventsTestTags.TICKET_CARD)
    if (currentTickets.fetchSemanticsNodes().isNotEmpty()) {
      currentTickets[0].performClick()
      compose.waitForIdle()
    }

    // Expired Tickets
    compose.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).performClick()
    compose.waitForIdle()
    val expiredTickets = compose.onAllNodesWithTag(MyEventsTestTags.TICKET_CARD)
    if (expiredTickets.fetchSemanticsNodes().isNotEmpty()) {
      expiredTickets[0].performTouchInput { click(position = Offset(1f, 1f)) }
      compose.waitForIdle()
    }

    // QR Code
    val qrNodes = compose.onAllNodesWithTag(MyEventsTestTags.QR_CODE_ICON)
    if (qrNodes.fetchSemanticsNodes().isNotEmpty()) {
      qrNodes[0].performTouchInput { click(position = Offset(1f, 1f)) }
      compose.waitForIdle()
    }

    // Ensure still on Tickets Screen
    compose.onNodeWithTag(MyEventsTestTags.TABS_ROW).assertIsDisplayed()

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

    // Ensure Event Detail is displayed
    compose.onNodeWithText("Go Back", useUnmergedTree = true).assertIsDisplayed().performClick()
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

    // Toggle Hide Sold Out
    compose
        .onNodeWithTag(EventFilterDialogTestTags.HIDE_SOLD_OUT_CHECKBOX, useUnmergedTree = true)
        .performClick()
    compose.waitForIdle()

    // Apply Filters
    compose
        .onNodeWithTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON, useUnmergedTree = true)
        .performClick()
    compose.waitForIdle()

    // Calendar
    compose
        .onNodeWithTag("BOTTOM_TAB_${NavigationDestinations.Tab.Tickets.name.uppercase()}")
        .performClick()
    compose.waitForIdle()

    // Tickets Screen
    compose.onNodeWithTag(MyEventsTestTags.TABS_ROW).assertIsDisplayed()
    compose.onNodeWithTag(MyEventsTestTags.TAB_CURRENT).performClick()
    compose.waitForIdle()
    val currentTickets = compose.onAllNodesWithTag(MyEventsTestTags.TICKET_CARD)
    if (currentTickets.fetchSemanticsNodes().isNotEmpty()) {
      currentTickets[0].performClick()
      compose.waitForIdle()
    }

    // Expired Tickets
    compose.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).performClick()
    compose.waitForIdle()
    val expiredTickets = compose.onAllNodesWithTag(MyEventsTestTags.TICKET_CARD)
    if (expiredTickets.fetchSemanticsNodes().isNotEmpty()) {
      expiredTickets[0].performTouchInput { click(position = Offset(1f, 1f)) }
      compose.waitForIdle()
    }

    // QR Code
    val qrNodes = compose.onAllNodesWithTag(MyEventsTestTags.QR_CODE_ICON)
    if (qrNodes.fetchSemanticsNodes().isNotEmpty()) {
      qrNodes[0].performTouchInput { click(position = Offset(1f, 1f)) }
      compose.waitForIdle()
    }

    // Ensure still on Tickets Screen
    compose.onNodeWithTag(MyEventsTestTags.TABS_ROW).assertIsDisplayed()

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

    // Fill out Create Organization Form
    compose
        .onNodeWithTag(CreateOrganizationTestTags.NAME_FIELD)
        .performTextInput("Test Organization")
    compose
        .onNodeWithTag(CreateOrganizationTestTags.DESCRIPTION_FIELD)
        .performTextInput("This is a test organization for Compose E2E test.")
    compose.onNodeWithTag(CreateOrganizationTestTags.PHONE_FIELD).performTextInput("791234567")
    compose.waitForIdle()

    // Submit the form
    compose.onNodeWithTag(CreateOrganizationTestTags.SUBMIT_BUTTON).performScrollTo().performClick()
  }

  private fun hasTestTagStartingWith(prefix: String) =
      SemanticsMatcher("Has testTag starting with '$prefix'") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
      }
}
