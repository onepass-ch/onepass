package ch.onepass.onepass.ui.navigation

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.rule.GrantPermissionRule
import ch.onepass.onepass.OnePassApp
import ch.onepass.onepass.model.auth.AuthRepositoryFirebase
import ch.onepass.onepass.ui.auth.AuthViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SwipeBackWrapperTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val cameraPermissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.CAMERA)

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.CAMERA,
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION)

  private lateinit var navController: TestNavHostController

  private fun setApp(signedIn: Boolean = true) {
    val activity = composeRule.activity

    activity.runOnUiThread {
      navController =
          TestNavHostController(activity).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setViewModelStore(activity.viewModelStore)
            setLifecycleOwner(activity)
            setOnBackPressedDispatcher(activity.onBackPressedDispatcher)
          }
    }

    val mockAuthRepo = mockk<AuthRepositoryFirebase>(relaxed = true)
    every { mockAuthRepo.isUserSignedIn() } returns signedIn

    val authVmFactory = viewModelFactory { initializer { AuthViewModel(mockAuthRepo) } }

    composeRule.setContent {
      OnePassApp(
          navController = navController,
          authViewModelFactory = authVmFactory,
          enableDeepLinking = false)
    }

    composeRule.waitForIdle()
  }

  private fun swipeBack() {
    composeRule.onRoot().performTouchInput { swipeRight() }
    composeRule.waitForIdle()
  }

  @Test
  fun notification_swipeBack() {
    setApp()
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Notification.route)
    }
    composeRule.waitForIdle()
    swipeBack()
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }

  @Test
  fun eventDetail_swipeBack() {
    setApp()
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.EventDetail.route("ev1"))
    }
    composeRule.waitForIdle()
    swipeBack()
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }

  @Test
  fun organizationFeed_swipeBack() {
    setApp()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Profile.route)
      navController.navigate(NavigationDestinations.Screen.OrganizationFeed.route)
    }

    composeRule.waitForIdle()
    swipeBack()

    assertEquals(
        NavigationDestinations.Screen.Profile.route, navController.currentDestination?.route)
  }

  @Test
  fun organizationDashboard_swipeBack() {
    setApp()
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.OrganizationDashboard.route("org1"))
    }
    composeRule.waitForIdle()
    swipeBack()
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }

  @Test
  fun organizationProfile_swipeBack() {
    setApp()
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.OrganizationProfile.route("org1"))
    }
    composeRule.waitForIdle()
    swipeBack()
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }

  @Test
  fun createEvent_swipeBack() {
    setApp()
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.CreateEvent.route("org1"))
    }
    composeRule.waitForIdle()
    swipeBack()
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }

  @Test
  fun editEvent_swipeBack() {
    setApp()
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.EditEvent.route("ev1"))
    }
    composeRule.waitForIdle()
    swipeBack()
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }

  @Test
  fun scan_swipeBack() {
    setApp()

    val orgId = "org1"
    val eventId = "ev1"

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.OrganizationDashboard.route(orgId))
      navController.navigate(NavigationDestinations.Screen.Scan.route(eventId))
    }

    composeRule.waitForIdle()
    swipeBack()

    assertEquals(
        NavigationDestinations.Screen.OrganizationDashboard.route,
        navController.currentDestination?.route)
  }

  @Test
  fun staffInvitation_swipeBack() {
    setApp()
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.StaffInvitation.route("org1"))
    }
    composeRule.waitForIdle()
    swipeBack()
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }

  @Test
  fun becomeOrganizer_swipeBack() {
    setApp()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Profile.route)
      navController.navigate(NavigationDestinations.Screen.BecomeOrganizer.route)
    }

    composeRule.waitForIdle()
    swipeBack()

    assertEquals(
        NavigationDestinations.Screen.Profile.route, navController.currentDestination?.route)
  }

  @Test
  fun myInvitations_swipeBack() {
    setApp()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Profile.route)
      navController.navigate(NavigationDestinations.Screen.MyInvitations.route)
    }

    composeRule.waitForIdle()
    swipeBack()

    assertEquals(
        NavigationDestinations.Screen.Profile.route, navController.currentDestination?.route)
  }

  @Test
  fun editOrganization_swipeBack() {
    setApp()
    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.EditOrganization.route("org1"))
    }
    composeRule.waitForIdle()
    swipeBack()
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }

  @Test
  fun comingSoon_swipeBack() {
    setApp()

    composeRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Profile.route)
      navController.navigate(NavigationDestinations.Screen.ComingSoon.route)
    }

    composeRule.waitForIdle()
    swipeBack()

    assertEquals(
        NavigationDestinations.Screen.Profile.route, navController.currentDestination?.route)
  }
}
