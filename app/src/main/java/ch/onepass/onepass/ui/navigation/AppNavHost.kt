package ch.onepass.onepass.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ch.onepass.onepass.model.pass.PassRepositoryFirebase
import ch.onepass.onepass.model.scan.TicketScanRepositoryFirebase
import ch.onepass.onepass.ui.auth.AuthScreen
import ch.onepass.onepass.ui.auth.AuthViewModel
import ch.onepass.onepass.ui.eventdetail.EventDetailScreen
import ch.onepass.onepass.ui.eventdetail.EventDetailViewModel
import ch.onepass.onepass.ui.eventform.createform.CreateEventForm
import ch.onepass.onepass.ui.eventform.createform.CreateEventFormViewModel
import ch.onepass.onepass.ui.eventform.editform.EditEventForm
import ch.onepass.onepass.ui.eventform.editform.EditEventFormViewModel
import ch.onepass.onepass.ui.feed.FeedScreen
import ch.onepass.onepass.ui.map.MapScreen
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.myevents.MyEventsScreen
import ch.onepass.onepass.ui.myevents.MyEventsViewModel
import ch.onepass.onepass.ui.myevents.passDataStore
import ch.onepass.onepass.ui.myinvitations.MyInvitationsScreen
import ch.onepass.onepass.ui.myinvitations.MyInvitationsViewModel
import ch.onepass.onepass.ui.navigation.NavigationDestinations.Screen
import ch.onepass.onepass.ui.notification.NotificationsScreen
import ch.onepass.onepass.ui.notification.NotificationsViewModel
import ch.onepass.onepass.ui.organization.OrganizationDashboardScreen
import ch.onepass.onepass.ui.organization.OrganizationDashboardViewModel
import ch.onepass.onepass.ui.organization.OrganizationFeedScreen
import ch.onepass.onepass.ui.organization.OrganizationFeedViewModel
import ch.onepass.onepass.ui.organization.OrganizerProfileEffect
import ch.onepass.onepass.ui.organization.OrganizerProfileScreen
import ch.onepass.onepass.ui.organization.OrganizerProfileViewModel
import ch.onepass.onepass.ui.organizer.CreateOrganizationScreen
import ch.onepass.onepass.ui.organizer.EditOrganizationScreen
import ch.onepass.onepass.ui.organizer.OrganizationEditorViewModel
import ch.onepass.onepass.ui.organizer.OrganizationFormViewModel
import ch.onepass.onepass.ui.profile.ProfileEffect
import ch.onepass.onepass.ui.profile.ProfileScreen
import ch.onepass.onepass.ui.profile.ProfileViewModel
import ch.onepass.onepass.ui.scan.ScanScreen
import ch.onepass.onepass.ui.scan.ScannerViewModel
import ch.onepass.onepass.ui.staff.StaffInvitationScreen
import ch.onepass.onepass.ui.staff.StaffInvitationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

/**
 * Navigation host that registers all app routes and wires view models to screens.
 *
 * @param navController NavHostController used to drive navigation.
 * @param modifier Modifier applied to the NavHost container.
 * @param mapViewModel Map screen ViewModel instance to pass into MapScreen.
 * @param testAuthButtonTag Optional test tag; when provided a simple login button is shown for
 *   testing.
 * @param authViewModelFactory Factory used to create the shared AuthViewModel instance.
 * @param profileViewModelFactory Factory used to create ProfileViewModel instances.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel? = null,
    testAuthButtonTag: String? = null,
    authViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
      initializer { AuthViewModel() }
    },
    profileViewModelFactory: ViewModelProvider.Factory? = viewModelFactory {
      initializer { ProfileViewModel() }
    }
) {
  // Get context for DataStore
  val context = LocalContext.current

  // Create a single AuthViewModel instance for the entire nav host
  val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
  val authState by authViewModel.uiState.collectAsState()

  // Determine start destination based on auth state
  val startDestination = if (authState.isSignedIn) Screen.Events.route else Screen.Auth.route

  NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {

    // ------------------ Auth ------------------
    composable(Screen.Auth.route) {
      if (testAuthButtonTag != null) {
        Box(modifier = Modifier.fillMaxSize()) {
          Button(
              onClick = { navController.navigate(Screen.Events.route) { launchSingleTop = true } },
              modifier = Modifier.semantics { testTag = testAuthButtonTag }) {
                Text("Login")
              }
        }
      } else {

        AuthScreen(
            onSignedIn = {
              navController.navigate(Screen.Events.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
                launchSingleTop = true
              }
            },
            authViewModel = authViewModel)
      }
    }

    // ------------------ Events (Feed) ------------------
    composable(Screen.Events.route) {
      FeedScreen(
          onNavigateToEvent = { eventId ->
            navController.navigate(Screen.EventDetail.route(eventId))
          },
          onNavigateToNotifications = { navController.navigate(Screen.Notification.route) })
    }

    // ------------------ Notifications ------------------
    composable(Screen.Notification.route) {
      val notificationViewModel: NotificationsViewModel = viewModel()
      NotificationsScreen(
          navController = navController,
          viewModel = notificationViewModel,
          onNavigateBack = { navController.popBackStack() })
    }

    // ------------------ Event Detail ------------------
    composable(Screen.EventDetail.route) { backStackEntry ->
      val eventId = backStackEntry.arguments?.getString(Screen.EventDetail.ARG_EVENT_ID) ?: ""
      val eventDetailVm: EventDetailViewModel =
          viewModel(
              factory =
                  viewModelFactory { initializer { EventDetailViewModel(eventId = eventId) } })
      EventDetailScreen(
          eventId = eventId,
          viewModel = eventDetailVm,
          onNavigateToMap = { navController.navigateToTopLevel(Screen.Map.route) },
          onNavigateToOrganizerProfile = { orgId ->
            navController.navigate(Screen.OrganizationProfile.route(orgId))
          },
          onBack = { navController.popBackStack() })
    }

      // ------------------ Tickets (My Events) ------------------
      composable(Screen.Tickets.route) {
          val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "LOCAL_TEST_UID"

          val myEventsVm: MyEventsViewModel =
              viewModel(
                  factory =
                      viewModelFactory {
                          initializer {
                              MyEventsViewModel(
                                  dataStore = context.passDataStore,
                                  passRepository =
                                      PassRepositoryFirebase(
                                          FirebaseFirestore.getInstance(), FirebaseFunctions.getInstance()),
                                  userId = uid)
                          }
                      })
          MyEventsScreen(viewModel = myEventsVm)  // ← SUPPRIME userQrData
      }

    // ------------------ Map ------------------
    composable(Screen.Map.route) {
      // Each map screen will create its own ViewModel and handle its own location permission
      val mapScreenViewModel: MapViewModel = mapViewModel ?: viewModel()
      MapScreen(
          mapViewModel = mapScreenViewModel,
          onNavigateToEvent = { eventId ->
            navController.navigate(Screen.EventDetail.route(eventId))
          })
    }

    // ------------------ Profile ------------------
    composable(Screen.Profile.route) {
      val profileVm: ProfileViewModel = viewModel(factory = profileViewModelFactory)
      ProfileScreen(
          viewModel = profileVm,
          onEffect = { effect ->
            when (effect) {
              ProfileEffect.NavigateToBecomeOrganizer ->
                  navController.navigate(Screen.BecomeOrganizer.route)
              ProfileEffect.NavigateToMyOrganizations ->
                  navController.navigate(Screen.OrganizationFeed.route)
              ProfileEffect.NavigateToAccountSettings ->
                  navController.navigate(Screen.ComingSoon.route)
              ProfileEffect.NavigateToPaymentMethods ->
                  navController.navigate(Screen.ComingSoon.route)
              ProfileEffect.NavigateToHelp -> navController.navigate(Screen.ComingSoon.route)
              ProfileEffect.SignOut -> {
                authViewModel.signOut()
                navController.navigate(Screen.Auth.route) {
                  popUpTo(navController.graph.startDestinationId) { inclusive = true }
                  launchSingleTop = true
                }
              }
              ProfileEffect.NavigateToMyInvitations -> {
                navController.navigate(Screen.MyInvitations.route)
              }
            }
          })
    }

    // ------------------ Organization Feed ------------------
    composable(Screen.OrganizationFeed.route) {
      val orgFeedVm: OrganizationFeedViewModel = viewModel()
      OrganizationFeedScreen(
          userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
          viewModel = orgFeedVm,
          onNavigateToOrganization = { organizationId ->
            navController.navigate(Screen.OrganizationDashboard.route(organizationId))
          },
          onNavigateBack = { navController.popBackStack() },
          onFabClick = { navController.navigate(Screen.BecomeOrganizer.route) })
    }

    // ------------------ Organization Dashboard ------------------
    composable(Screen.OrganizationDashboard.route) { backStackEntry ->
      val organizationId =
          backStackEntry.arguments?.getString(Screen.OrganizationDashboard.ARG_ORGANIZATION_ID)
              ?: ""
      val orgDashboardVm: OrganizationDashboardViewModel =
          viewModel(factory = viewModelFactory { initializer { OrganizationDashboardViewModel() } })
      OrganizationDashboardScreen(
          organizationId = organizationId,
          viewModel = orgDashboardVm,
          onNavigateBack = { navController.popBackStack() },
          onNavigateToProfile = { orgId ->
            navController.navigate(Screen.OrganizationProfile.route(orgId))
          },
          onNavigateToCreateEvent = {
            navController.navigate(Screen.CreateEvent.route(organizationId))
          },
          onNavigateToAddStaff = { orgId ->
            navController.navigate(Screen.StaffInvitation.route(organizationId))
          },
          onNavigateToScanTickets = { eventId ->
            navController.navigate(Screen.Scan.route(eventId))
          },
          onNavigateToEditEvent = { eventId ->
            navController.navigate(Screen.EditEvent.route(eventId))
          })
    }

    // ------------------ Organization Profile ------------------
    composable(Screen.OrganizationProfile.route) { backStackEntry ->
      val organizationId =
          backStackEntry.arguments?.getString(Screen.OrganizationProfile.ARG_ORGANIZATION_ID) ?: ""
      val orgProfileVm: OrganizerProfileViewModel = viewModel()

      OrganizerProfileScreen(
          organizationId = organizationId,
          viewModel = orgProfileVm,
          onEffect = { effect ->
            when (effect) {
              is OrganizerProfileEffect.NavigateToEditOrganization ->
                  navController.navigate(Screen.EditOrganization.route(effect.organizationId))
              is OrganizerProfileEffect.NavigateToEvent ->
                  navController.navigate(Screen.EventDetail.route(effect.eventId))
              is OrganizerProfileEffect.OpenSocialMedia ->
                  navController.navigate(Screen.ComingSoon.route)
              is OrganizerProfileEffect.OpenWebsite ->
                  navController.navigate(Screen.ComingSoon.route)
              is OrganizerProfileEffect.ShowError -> navController.navigate(Screen.ComingSoon.route)
            }
          })
    }

    // ------------------ Create Event ------------------
    composable(Screen.CreateEvent.route) { backStackEntry ->
      val organizationId =
          backStackEntry.arguments?.getString(Screen.CreateEvent.ARG_ORGANIZATION_ID) ?: ""
      val createEventVm: CreateEventFormViewModel = viewModel()
      CreateEventForm(
          organizationId = organizationId,
          viewModel = createEventVm,
          onNavigateBack = { navController.popBackStack() },
          onEventCreated = { navController.popBackStack() })
    }

    // ------------------ Edit Event ------------------
    composable(Screen.EditEvent.route) { backStackEntry ->
      val eventId = backStackEntry.arguments?.getString(Screen.EditEvent.ARG_EVENT_ID) ?: ""
      val editEventVm: EditEventFormViewModel = viewModel()
      EditEventForm(
          eventId = eventId,
          viewModel = editEventVm,
          onNavigateBack = { navController.popBackStack() },
          onEventUpdated = { navController.popBackStack() })
    }

    // ------------------ Scan Tickets ------------------
    composable(Screen.Scan.route) { backStackEntry ->
      val eventId = backStackEntry.arguments?.getString(Screen.Scan.ARG_EVENT_ID) ?: ""
      val scannerVm: ScannerViewModel =
          viewModel(
              factory =
                  viewModelFactory {
                    initializer {
                      ScannerViewModel(eventId = eventId, repo = TicketScanRepositoryFirebase())
                    }
                  })
      ScanScreen(viewModel = scannerVm)
    }

    // ------------------ Staff Invitation ------------------
    composable(Screen.StaffInvitation.route) { backStackEntry ->
      val organizationId =
          backStackEntry.arguments?.getString(Screen.StaffInvitation.ARG_ORGANIZATION_ID) ?: ""
      val staffInvitationVm: StaffInvitationViewModel =
          viewModel(
              factory =
                  viewModelFactory {
                    initializer { StaffInvitationViewModel(organizationId = organizationId) }
                  })
      StaffInvitationScreen(
          viewModel = staffInvitationVm, onNavigateBack = { navController.popBackStack() })
    }

    // ------------------ Become Organizer ------------------
    composable(Screen.BecomeOrganizer.route) {
      val becomeOrganizerVm: OrganizationFormViewModel = viewModel()
      val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
      CreateOrganizationScreen(
          ownerId = currentUserId,
          viewModel = becomeOrganizerVm,
          onOrganizationCreated = {
            navController.navigate(Screen.OrganizationFeed.route) {
              popUpTo(Screen.BecomeOrganizer.route) { inclusive = true }
            }
          },
          onNavigateBack = { navController.popBackStack() })
    }

    // ------------------ My Invitations ----------------
    composable(Screen.MyInvitations.route) {
      val myInvVm: MyInvitationsViewModel = viewModel()
      MyInvitationsScreen(viewModel = myInvVm, onNavigateBack = { navController.popBackStack() })
    }

    // ---------------- Edit Organization -------------
    composable(Screen.EditOrganization.route) { backStackEntry ->
      val organizationId =
          backStackEntry.arguments?.getString(Screen.EditOrganization.ARG_ORGANIZATION_ID) ?: ""
      val editVm: OrganizationEditorViewModel = viewModel()
      EditOrganizationScreen(
          organizationId = organizationId,
          viewModel = editVm,
          onOrganizationUpdated = { navController.popBackStack() },
          onNavigateBack = { navController.popBackStack() })
    }

    // ------------------ Placeholders ------------------
    composable(Screen.ComingSoon.route) {
      ComingSoonScreen(onBack = { navController.popBackStack() })
    }
  }
}

/**
 * Navigates to a top-level destination, clearing any existing back stack to avoid multiple copies
 * of the same destination.
 *
 * @param route The route string of the top-level destination to navigate to.
 */
fun NavHostController.navigateToTopLevel(route: String) {
  this.navigate(route) {
    launchSingleTop = true
    restoreState = false
    popUpTo(route) { inclusive = true }
  }
}
