package ch.onepass.onepass.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ch.onepass.onepass.ui.auth.AuthScreen
import ch.onepass.onepass.ui.auth.AuthViewModel
import ch.onepass.onepass.ui.feed.FeedScreen
import ch.onepass.onepass.ui.map.MapScreen
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.myevents.MyEventsScreen
import ch.onepass.onepass.ui.myevents.MyEventsViewModel
import ch.onepass.onepass.ui.myinvitations.MyInvitationsScreen
import ch.onepass.onepass.ui.navigation.NavigationDestinations.Screen
import ch.onepass.onepass.ui.profile.ProfileEffect
import ch.onepass.onepass.ui.profile.ProfileScreen
import ch.onepass.onepass.ui.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel,
    isLocationPermissionGranted: Boolean,
    testAuthButtonTag: String? = null
) {
  NavHost(
      navController = navController,
      startDestination = Screen.Auth.route, // Auth comes first
      modifier = modifier) {

        // ------------------ Auth ------------------
        composable(Screen.Auth.route) {
          if (testAuthButtonTag != null) {
            // test shortcut: tagged button that immediately navigates to Events
            Box(modifier = Modifier.fillMaxSize()) {
              Button(
                  onClick = {
                    navController.navigate(Screen.Events.route) { launchSingleTop = true }
                  },
                  modifier = Modifier.semantics { testTag = testAuthButtonTag }) {
                    Text("Login")
                  }
            }
          } else {
            val authVm: AuthViewModel = viewModel()
            AuthScreen(
                onSignedIn = {
                  navController.navigate(Screen.Events.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                    launchSingleTop = true
                  }
                },
                authViewModel = authVm)
          }
        }

        // ------------------ Events (Feed) ------------------
        composable(Screen.Events.route) {
          FeedScreen(
              // TODO: replace ComingSoon with EventDetail when implemented
              onNavigateToEvent = { /* eventId -> */
                navController.navigate(Screen.ComingSoon.route)
              },
              // TODO: replace ComingSoon with Calendar when implemented
              onNavigateToCalendar = { navController.navigate(Screen.ComingSoon.route) })
        }

        // ------------------ Tickets (My Events) ------------------
        composable(Screen.Tickets.route) {
          val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "LOCAL_TEST_UID"

          val myEventsVm: MyEventsViewModel =
              viewModel(
                  factory = viewModelFactory { initializer { MyEventsViewModel(userId = uid) } })
          MyEventsScreen(viewModel = myEventsVm, userQrData = "USER-QR-DEMO")
        }

        // ------------------ Map ------------------
        composable(Screen.Map.route) {
          MapScreen(
              mapViewModel = mapViewModel,
              isLocationPermissionGranted = isLocationPermissionGranted)
        }

        // ------------------ Profile ------------------
        composable(Screen.Profile.route) {
          val profileVm: ProfileViewModel = viewModel()
          val authVm: AuthViewModel = viewModel()
          ProfileScreen(
              viewModel = profileVm,
              onEffect = { effect ->
                when (effect) {
                  // TODO: wire to real destinations when implemented
                  ProfileEffect.NavigateToOrganizerOnboarding ->
                      navController.navigate(Screen.ComingSoon.route)
                  ProfileEffect.NavigateToCreateEvent ->
                      navController.navigate(Screen.CreateEvent.route)
                  ProfileEffect.NavigateToAccountSettings,
                  ProfileEffect.NavigateToPaymentMethods,
                  ProfileEffect.NavigateToHelp -> navController.navigate(Screen.ComingSoon.route)
                  ProfileEffect.NavigateToMyInvitations ->
                      navController.navigate(Screen.MyInvitations.route)
                  ProfileEffect.SignOut -> {
                    authVm.signOut()
                    navController.navigate(Screen.Auth.route) {
                      popUpTo(navController.graph.startDestinationId) { inclusive = true }
                      launchSingleTop = true
                    }
                  }
                  ProfileEffect.NavigateToAccountSettings ->
                      navController.navigate(Screen.ComingSoon.route)
                  ProfileEffect.NavigateToHelp -> navController.navigate(Screen.ComingSoon.route)
                  ProfileEffect.NavigateToPaymentMethods ->
                      navController.navigate(Screen.ComingSoon.route)
                }
              })
        }

        // ------------------ My Invitations ------------------
        composable(Screen.MyInvitations.route) {
          MyInvitationsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ------------------ Placeholders ------------------
        composable(Screen.ComingSoon.route) {
          ComingSoonScreen(onBack = { navController.popBackStack() })
        }
        // TODO: replace with real detail when ready
        composable(Screen.EventDetail.route) {
          ComingSoonScreen(onBack = { navController.popBackStack() })
        }
      }
}
