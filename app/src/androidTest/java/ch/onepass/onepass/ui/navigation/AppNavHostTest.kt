package ch.onepass.onepass.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.auth.AuthRepositoryFirebase
import ch.onepass.onepass.ui.auth.AuthViewModel
import ch.onepass.onepass.ui.auth.SignInScreenTestTags
import ch.onepass.onepass.ui.feed.FeedScreenTestTags
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.myinvitations.MyInvitationsScreenTestTags
import ch.onepass.onepass.ui.profile.ProfileTestTags
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavHostTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun appNavHost_startsAtAuthScreen_whenUserIsNotSignedIn() {
    // Given: a mock auth repository that returns false for isUserSignedIn
    val mockAuthRepository = mockk<AuthRepositoryFirebase>(relaxed = true)
    every { mockAuthRepository.isUserSignedIn() } returns false

    val authViewModelFactory = viewModelFactory {
      initializer { AuthViewModel(mockAuthRepository) }
    }

    // When: we set the AppNavHost
    composeTestRule.setContent {
      AppNavHost(
          navController = rememberNavController(),
          mapViewModel = MapViewModel(),
          isLocationPermissionGranted = true,
          testAuthButtonTag = SignInScreenTestTags.LOGIN_BUTTON,
          authViewModelFactory = authViewModelFactory)
    }

    // Then: the auth screen should be displayed
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun appNavHost_startsAtEventsScreen_whenUserIsSignedIn() {
    // Given: a mock auth repository that returns true for isUserSignedIn
    val mockAuthRepository = mockk<AuthRepositoryFirebase>(relaxed = true)
    every { mockAuthRepository.isUserSignedIn() } returns true

    val authViewModelFactory = viewModelFactory {
      initializer { AuthViewModel(mockAuthRepository) }
    }

    // When: we set the AppNavHost
    composeTestRule.setContent {
      AppNavHost(
          navController = rememberNavController(),
          mapViewModel = MapViewModel(),
          isLocationPermissionGranted = true,
          authViewModelFactory = authViewModelFactory)
    }

    // Then: the events (feed) screen should be displayed
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
  }

  @Test
  fun appNavHost_navigatesToEvents_afterTestLogin() {
    // Given: a mock auth repository that returns false initially
    val mockAuthRepository = mockk<AuthRepositoryFirebase>(relaxed = true)
    every { mockAuthRepository.isUserSignedIn() } returns false

    val authViewModelFactory = viewModelFactory {
      initializer { AuthViewModel(mockAuthRepository) }
    }

    // When: we set the AppNavHost and click the test login button
    composeTestRule.setContent {
      AppNavHost(
          navController = rememberNavController(),
          mapViewModel = MapViewModel(),
          isLocationPermissionGranted = true,
          testAuthButtonTag = SignInScreenTestTags.LOGIN_BUTTON,
          authViewModelFactory = authViewModelFactory)
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    // Then: we should navigate to the events screen
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
  }

  @Test
  fun appNavHost_usesSharedAuthViewModel_acrossComposables() {
    // Given: a mock auth repository
    val mockAuthRepository = mockk<AuthRepositoryFirebase>(relaxed = true)
    every { mockAuthRepository.isUserSignedIn() } returns false
    every { mockAuthRepository.signOut() } returns Result.success(Unit)

    val authViewModelFactory = viewModelFactory {
      initializer { AuthViewModel(mockAuthRepository) }
    }

    // Create navController outside of setContent so we can use it to navigate
    lateinit var navController: androidx.navigation.NavHostController

    // When: we set the AppNavHost
    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(
          navController = navController,
          mapViewModel = MapViewModel(),
          isLocationPermissionGranted = true,
          testAuthButtonTag = SignInScreenTestTags.LOGIN_BUTTON,
          authViewModelFactory = authViewModelFactory)
    }

    // Navigate to events
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Navigate to profile screen programmatically (no bottom bar in AppNavHost)
    composeTestRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Profile.route)
    }
    composeTestRule.waitForIdle()
    Thread.sleep(500)
    composeTestRule.waitForIdle()
    // Verify we navigated to the profile screen
    composeTestRule.onNodeWithTag(ProfileTestTags.SCREEN).assertIsDisplayed()

    // Then: the same auth repository instance should be used
    // (This is verified by the fact that we only created one instance via the factory)
    verify(exactly = 1) { mockAuthRepository.isUserSignedIn() }
  }

  @Test
  fun appNavHost_withRealFirebaseAuth_startsCorrectly_whenNoUser() {
    // Given: real Firebase auth with no user
    val mockFirebaseAuth = mockk<FirebaseAuth>()
    every { mockFirebaseAuth.currentUser } returns null

    val authRepository = AuthRepositoryFirebase(auth = mockFirebaseAuth)
    val authViewModelFactory = viewModelFactory { initializer { AuthViewModel(authRepository) } }

    // When: we set the AppNavHost
    composeTestRule.setContent {
      AppNavHost(
          navController = rememberNavController(),
          mapViewModel = MapViewModel(),
          isLocationPermissionGranted = true,
          testAuthButtonTag = SignInScreenTestTags.LOGIN_BUTTON,
          authViewModelFactory = authViewModelFactory)
    }

    // Then: should start at auth screen
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun appNavHost_withRealFirebaseAuth_startsCorrectly_whenUserExists() {
    // Given: real Firebase auth with a user
    val mockFirebaseAuth = mockk<FirebaseAuth>()
    val mockFirebaseUser = mockk<FirebaseUser>()
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

    val authRepository = AuthRepositoryFirebase(auth = mockFirebaseAuth)
    val authViewModelFactory = viewModelFactory { initializer { AuthViewModel(authRepository) } }

    // When: we set the AppNavHost
    composeTestRule.setContent {
      AppNavHost(
          navController = rememberNavController(),
          mapViewModel = MapViewModel(),
          isLocationPermissionGranted = true,
          authViewModelFactory = authViewModelFactory)
    }

    // Then: should start at events screen
    composeTestRule.onNodeWithTag(FeedScreenTestTags.FEED_SCREEN).assertIsDisplayed()
  }

  @Test
  fun appNavHost_navigatesToMyInvitations_fromProfile() {
    // Given: a mock auth repository that returns true for isUserSignedIn
    val mockAuthRepository = mockk<AuthRepositoryFirebase>(relaxed = true)
    every { mockAuthRepository.isUserSignedIn() } returns true

    val authViewModelFactory = viewModelFactory {
      initializer { AuthViewModel(mockAuthRepository) }
    }

    // Create navController outside of setContent so we can use it to navigate
    lateinit var navController: androidx.navigation.NavHostController

    // When: we set the AppNavHost
    composeTestRule.setContent {
      navController = rememberNavController()
      AppNavHost(
          navController = navController,
          mapViewModel = MapViewModel(),
          isLocationPermissionGranted = true,
          authViewModelFactory = authViewModelFactory)
    }

    // Navigate to profile screen
    composeTestRule.runOnUiThread {
      navController.navigate(NavigationDestinations.Screen.Profile.route)
    }
    composeTestRule.waitForIdle()
    Thread.sleep(500)
    composeTestRule.waitForIdle()

    // Verify we navigated to the profile screen
    composeTestRule.onNodeWithTag(ProfileTestTags.SCREEN).assertIsDisplayed()

    // Click on invitations button
    composeTestRule.onNodeWithTag(ProfileTestTags.SETTINGS_INVITATIONS).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500)
    composeTestRule.waitForIdle()

    // Then: we should navigate to the My Invitations screen
    composeTestRule.onNodeWithTag(MyInvitationsScreenTestTags.SCREEN).assertIsDisplayed()
  }
}
