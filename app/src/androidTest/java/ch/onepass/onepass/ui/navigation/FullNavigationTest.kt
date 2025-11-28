package ch.onepass.onepass.ui.navigation

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.rule.GrantPermissionRule
import ch.onepass.onepass.BuildConfig
import ch.onepass.onepass.OnePassApp
import ch.onepass.onepass.model.auth.AuthRepositoryFirebase
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import ch.onepass.onepass.ui.auth.AuthViewModel
import ch.onepass.onepass.ui.feed.FeedScreenTestTags
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.profile.*
import com.mapbox.common.MapboxOptions
import io.mockk.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FullNavigationTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private lateinit var navController: TestNavHostController
  private lateinit var mockAuthRepo: AuthRepositoryFirebase
  private lateinit var mockUserRepo: UserRepositoryFirebase

  private val TEST_LOGIN_BUTTON = "TEST_LOGIN_BUTTON"
  private val TAB_TICKETS = "BOTTOM_TAB_TICKETS"
  private val TAB_MAP = "BOTTOM_TAB_MAP"
  private val TAB_PROFILE = "BOTTOM_TAB_PROFILE"

  private var injectedProfileVMFactory: ViewModelProvider.Factory? = null

  @SuppressLint("ViewModelConstructorInComposable")
  private fun setApp(signedIn: Boolean) {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

    mockAuthRepo = mockk(relaxed = true)
    mockUserRepo = mockk(relaxed = true)

    every { mockAuthRepo.isUserSignedIn() } returns signedIn
    // Note: isOrganizer() method has been removed from UserRepository
    // Organization membership is now checked via MembershipRepository

    val authVmFactory = viewModelFactory { initializer { AuthViewModel(mockAuthRepo) } }

    val activity = composeRule.activity
    activity.runOnUiThread {
      navController =
          TestNavHostController(activity).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setLifecycleOwner(activity)
            setViewModelStore(activity.viewModelStore)
            setOnBackPressedDispatcher(activity.onBackPressedDispatcher)
          }
    }

    composeRule.setContent {
      OnePassApp(
          navController = navController,
          mapViewModel = MapViewModel(),
          testAuthButtonTag = if (!signedIn) TEST_LOGIN_BUTTON else null,
          authViewModelFactory = authVmFactory,
          profileViewModelFactory = injectedProfileVMFactory)
    }

    composeRule.waitForIdle()
  }

  @Before
  fun beforeEach() {
    injectedProfileVMFactory = null
  }

  @Test
  fun start_at_auth_when_not_authenticated() {
    setApp(signedIn = false)
    composeRule.waitForIdle()
    assertEquals(NavigationDestinations.Screen.Auth.route, navController.currentDestination?.route)
    composeRule.onNodeWithTag(TEST_LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun login_button_navigates_to_events_and_shows_bottom_tabs() {
    setApp(signedIn = false)
    composeRule.onNodeWithTag(TEST_LOGIN_BUTTON).performClick()
    composeRule.waitForIdle()

    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
    composeRule.onNodeWithTag(TAB_TICKETS).assertIsDisplayed()
  }

  @Test
  fun bottom_tabs_navigate_between_top_level_destinations() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(TAB_TICKETS).assertIsDisplayed().performClick().assertIsSelected()
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.Tickets.route, navController.currentDestination?.route)

    composeRule.onNodeWithTag(TAB_MAP).assertIsDisplayed().performClick().assertIsSelected()
    composeRule.waitForIdle()
    assertEquals(NavigationDestinations.Screen.Map.route, navController.currentDestination?.route)

    composeRule.onNodeWithTag(TAB_PROFILE).assertIsDisplayed().performClick().assertIsSelected()
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.Profile.route, navController.currentDestination?.route)
  }

  @Test
  fun events_to_event_detail_to_map_and_back_flow() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.EventDetail.route("event_123"))
    }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.EventDetail.route, navController.currentDestination?.route)

    composeRule.runOnUiThread { navController.navigate(NavigationDestinations.Screen.Map.route) }
    composeRule.waitForIdle()
    assertEquals(NavigationDestinations.Screen.Map.route, navController.currentDestination?.route)

    composeRule.runOnUiThread { navController.popBackStack() }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.EventDetail.route, navController.currentDestination?.route)
  }

  @Test
  fun events_to_organizer_profile_and_edit_flow() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.OrganizationProfile.route("org_9"))
    }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.OrganizationProfile.route,
        navController.currentDestination?.route)

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.EditOrganization.route("org_9"))
    }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.EditOrganization.route,
        navController.currentDestination?.route)

    composeRule.runOnUiThread { navController.popBackStack() }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.OrganizationProfile.route,
        navController.currentDestination?.route)
  }

  @Test
  fun tickets_screen_shows_for_authenticated_user() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Tickets.route)
    }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.Tickets.route, navController.currentDestination?.route)
  }

  @Test
  fun map_navigate_to_event_detail_flow() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    composeRule.runOnUiThread { navController.navigate(NavigationDestinations.Screen.Map.route) }
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.EventDetail.route("ev_map_1"))
    }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.EventDetail.route, navController.currentDestination?.route)
  }

  @Test
  fun profile_navigates_to_become_organizer_and_organization_feed() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Profile.route)
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.BecomeOrganizer.route)
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.OrganizationFeed.route)
    }
    composeRule.waitForIdle()

    assertEquals(
        NavigationDestinations.Screen.OrganizationFeed.route,
        navController.currentDestination?.route)
  }

  @Test
  fun profile_navigates_to_my_invitations_and_coming_soon() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Profile.route)
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.MyInvitations.route)
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.ComingSoon.route)
    }
    composeRule.waitForIdle()

    assertEquals(
        NavigationDestinations.Screen.ComingSoon.route, navController.currentDestination?.route)
  }

  @Test
  fun organization_dashboard_full_flow() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    val orgId = "org_flow_77"
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.OrganizationDashboard.route(orgId))
    }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.OrganizationDashboard.route,
        navController.currentDestination?.route)

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.CreateEvent.route(orgId))
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.StaffInvitation.route(orgId))
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread { navController.popBackStack() }
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.EditOrganization.route(orgId))
    }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.EditOrganization.route,
        navController.currentDestination?.route)
  }

  @Test
  fun profile_as_organizer_navigates_to_orgFeed_via_myOrganizations() {

    val fakeVM = mockk<ProfileViewModel>(relaxed = true)

    val stateFlow =
        MutableStateFlow(
            ProfileUiState(
                displayName = "Org User",
                email = "org@example.com",
                isOrganizer = true,
                loading = false))
    every { fakeVM.state } returns stateFlow

    val effectsFlow = MutableSharedFlow<ProfileEffect>(extraBufferCapacity = 1)
    every { fakeVM.effects } returns effectsFlow

    val dummyJob = Job()
    every { fakeVM.onOrganizationButton() } answers
        {
          effectsFlow.tryEmit(ProfileEffect.NavigateToMyOrganizations)
          dummyJob
        }

    injectedProfileVMFactory = viewModelFactory { initializer { fakeVM } }

    setApp(signedIn = true)
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Profile.route)
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(ProfileTestTags.ORG_CTA).assertIsDisplayed()

    composeRule.onNodeWithTag(ProfileTestTags.ORG_CTA).performClick()
    composeRule.waitForIdle()

    assertEquals(
        NavigationDestinations.Screen.OrganizationFeed.route,
        navController.currentDestination?.route)
  }

  @Test
  fun parameterized_routes_handle_concrete_ids() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    val ids = listOf("a", "123", "with-dash_underscore")
    ids.forEach { id ->
      composeRule.runOnUiThread {
        navController.navigate(NavigationDestinations.Screen.EventDetail.route(id))
      }
      composeRule.waitForIdle()
      assertEquals(
          NavigationDestinations.Screen.EventDetail.route, navController.currentDestination?.route)

      composeRule.runOnUiThread { navController.popBackStack() }
      composeRule.waitForIdle()
    }
  }

  @Test
  fun real_signin_removes_auth_from_backstack() {
    setApp(signedIn = false)

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Events.route) {
        popUpTo(NavigationDestinations.Screen.Auth.route) { inclusive = true }
      }
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread { navController.popBackStack() }
    composeRule.waitForIdle()

    assertNotEquals(
        NavigationDestinations.Screen.Auth.route, navController.currentDestination?.route)
  }

  @Test
  fun eventDetail_navigates_to_organizer_profile() {
    setApp(signedIn = true)
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.EventDetail.route("ev-9"))
    }
    composeRule.waitForIdle()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.OrganizationProfile.route("org-22"))
    }
    composeRule.waitForIdle()

    assertEquals(
        NavigationDestinations.Screen.OrganizationProfile.route,
        navController.currentDestination?.route)
  }

  @Test
  fun organization_dashboard_to_edit_event_flow() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    val orgId = "org_flow_edit_test"
    val eventId = "event_to_edit_id"

    // 1. Navigate to Dashboard
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.OrganizationDashboard.route(orgId))
    }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.OrganizationDashboard.route,
        navController.currentDestination?.route)

    // 2. Navigate to Edit Event (Simulating the user action)
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.EditEvent.route(eventId))
    }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.EditEvent.route, navController.currentDestination?.route)

    // 3. Navigate Back
    composeRule.runOnUiThread { navController.popBackStack() }
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.OrganizationDashboard.route,
        navController.currentDestination?.route)
  }

  @Test
  fun edit_event_route_handles_ids() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    val ids = listOf("edit_1", "edit-event-id_complex")
    ids.forEach { id ->
      composeRule.runOnUiThread {
        navController.navigate(NavigationDestinations.Screen.EditEvent.route(id))
      }
      composeRule.waitForIdle()
      assertEquals(
          NavigationDestinations.Screen.EditEvent.route, navController.currentDestination?.route)

      composeRule.runOnUiThread { navController.popBackStack() }
      composeRule.waitForIdle()
    }
  }

  @Test
  fun organization_feed_fab_visible_for_organizer_and_navigates_to_become_organizer() {
    val fakeVM = mockk<ProfileViewModel>(relaxed = true)

    val stateFlow =
        MutableStateFlow(
            ProfileUiState(
                displayName = "Org User",
                email = "org@example.com",
                isOrganizer = true,
                loading = false))
    every { fakeVM.state } returns stateFlow

    val effectsFlow = MutableSharedFlow<ProfileEffect>(extraBufferCapacity = 1)
    every { fakeVM.effects } returns effectsFlow

    val dummyJob = Job()
    every { fakeVM.onOrganizationButton() } answers
        {
          effectsFlow.tryEmit(ProfileEffect.NavigateToMyOrganizations)
          dummyJob
        }

    injectedProfileVMFactory = viewModelFactory { initializer { fakeVM } }

    // Start app as signed in
    setApp(signedIn = true)
    composeRule.waitForIdle()
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Profile.route)
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(ProfileTestTags.ORG_CTA).assertIsDisplayed()
    composeRule.onNodeWithTag(ProfileTestTags.ORG_CTA).performClick()
    composeRule.waitForIdle()
    assertEquals(
        NavigationDestinations.Screen.OrganizationFeed.route,
        navController.currentDestination?.route)
    composeRule
        .onNodeWithTag(ch.onepass.onepass.ui.organization.OrganizationFeedTestTags.ADD_ORG_FAB)
        .assertIsDisplayed()
        .performClick()
    composeRule.waitForIdle()

    assertEquals(
        NavigationDestinations.Screen.BecomeOrganizer.route,
        navController.currentDestination?.route)
  }

  @Test
  fun feed_to_notification_flow() {
    setApp(signedIn = true)
    composeRule.waitForIdle()

    // On the Feed Screen by default
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)

    // Click the Notification button on the top bar
    composeRule
        .onNodeWithTag(FeedScreenTestTags.NOTIFICATION_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeRule.waitForIdle()

    // Check we navigated to Notification screen
    assertEquals(
        NavigationDestinations.Screen.Notification.route, navController.currentDestination?.route)

    // Click the Back button on the notification screen
    composeRule.onNodeWithTag("notification_back_button").assertIsDisplayed().performClick()

    composeRule.waitForIdle()

    // Check we navigated back to the Feed screen
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }
}
