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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ch.onepass.onepass.ui.auth.AuthScreen
import ch.onepass.onepass.ui.auth.AuthViewModel
import ch.onepass.onepass.ui.createform.CreateEventForm
import ch.onepass.onepass.ui.feed.FeedScreen
import ch.onepass.onepass.ui.map.MapScreen
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.myevents.MyEventsScreen
import ch.onepass.onepass.ui.myevents.Ticket
import ch.onepass.onepass.ui.myevents.TicketStatus
import ch.onepass.onepass.ui.navigation.NavigationDestinations.Screen
import ch.onepass.onepass.ui.profile.ProfileEffect
import ch.onepass.onepass.ui.profile.ProfileScreen
import ch.onepass.onepass.ui.profile.ProfileViewModel

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
                          navController.navigate(Screen.Events.route) {
                              launchSingleTop = true
                          }
                      },
                      modifier = Modifier.semantics { testTag = testAuthButtonTag }
                  ) {
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
          // TODO: replace sample data with repository-backed tickets + real user QR
          val currentTickets =
              listOf(
                  Ticket(
                      title = "Lausanne Party",
                      status = TicketStatus.CURRENTLY,
                      dateTime = "Dec 15, 2024 • 9:00 PM",
                      location = "Lausanne, Flon"))
          val expiredTickets =
              listOf(
                  Ticket(
                      title = "Morges Party",
                      status = TicketStatus.EXPIRED,
                      dateTime = "Nov 10, 2024 • 8:00 PM",
                      location = "Morges"))

          MyEventsScreen(
              userQrData = "USER-QR-DEMO",
              currentTickets = currentTickets,
              expiredTickets = expiredTickets)
        }

        // ------------------ Map ------------------
        composable(Screen.Map.route) {
          MapScreen(
              mapViewModel = mapViewModel,
              isLocationPermissionGranted = isLocationPermissionGranted)
        }

        // ------------------ Profile ------------------
        composable(Screen.Profile.route) {
          // Create a screen-scoped VM (replace with Hilt if you use it)
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
