package ch.onepass.onepass.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SwipeWrapperTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
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
  }

  private fun setSwipeApp(startScreen: String) {
    composeRule.setContent {
      val swipeManager = SwipeNavigationManager(navController)
      val screens =
          listOf(
              NavigationDestinations.Screen.Events,
              NavigationDestinations.Screen.Tickets,
              NavigationDestinations.Screen.Map,
              NavigationDestinations.Screen.Profile)
      screens.forEach { swipeManager.register(it) }

      NavHost(navController, startDestination = startScreen) {
        screens.forEach { screen ->
          composable(screen.route) {
            val canSwipe = screen != NavigationDestinations.Screen.Map
            SwipeWrapper(swipeManager, screen, canSwipe) {
              Box(modifier = Modifier.fillMaxSize().semantics { testTag = "SWIPE_SCREEN" }) {
                Text(screen.route)
              }
            }
          }
        }
      }
    }
    composeRule.waitForIdle()
  }

  @Test
  fun map_is_not_swipeable() {
    setSwipeApp(NavigationDestinations.Screen.Map.route)

    // Perform right swipe → should not navigate
    composeRule.onNodeWithTag("SWIPE_SCREEN").performTouchInput {
      swipeLeft()
    } // swipe right (finger moves left)
    composeRule.waitForIdle()

    assertEquals(NavigationDestinations.Screen.Map.route, navController.currentDestination?.route)

    // Perform left swipe → should not navigate
    composeRule.onNodeWithTag("SWIPE_SCREEN").performTouchInput {
      swipeRight()
    } // swipe left (finger moves right)
    composeRule.waitForIdle()

    assertEquals(NavigationDestinations.Screen.Map.route, navController.currentDestination?.route)
  }

  @Test
  fun swipeRight_navigates_to_next_screen() {
    // Start at Events
    setSwipeApp(NavigationDestinations.Screen.Events.route)

    // Perform right swipe → goes to Tickets
    composeRule.onNodeWithTag("SWIPE_SCREEN").performTouchInput {
      swipeLeft()
    } // swipe right (finger moves left)
    composeRule.waitForIdle()

    assertEquals(
        NavigationDestinations.Screen.Tickets.route, navController.currentDestination?.route)
  }

  @Test
  fun swipeLeft_navigates_to_previous_screen() {
    // Start at Tickets
    setSwipeApp(NavigationDestinations.Screen.Tickets.route)

    // Perform left swipe → goes back to Events
    composeRule.onNodeWithTag("SWIPE_SCREEN").performTouchInput {
      swipeRight()
    } // swipe left (finger moves right)
    composeRule.waitForIdle()

    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }

  @Test
  fun swipeLeft_at_first_screen_does_not_navigate() {
    setSwipeApp(NavigationDestinations.Screen.Events.route)

    composeRule.onNodeWithTag("SWIPE_SCREEN").performTouchInput {
      swipeRight()
    } // swipe left (finger moves right)
    composeRule.waitForIdle()

    // Should remain at Events
    assertEquals(
        NavigationDestinations.Screen.Events.route, navController.currentDestination?.route)
  }

  @Test
  fun swipeRight_at_last_screen_does_not_navigate() {
    setSwipeApp(NavigationDestinations.Screen.Profile.route)

    composeRule.onNodeWithTag("SWIPE_SCREEN").performTouchInput {
      swipeLeft()
    } // swipe right (finger moves left)
    composeRule.waitForIdle()

    // Should remain at Profile
    assertEquals(
        NavigationDestinations.Screen.Profile.route, navController.currentDestination?.route)
  }
}
