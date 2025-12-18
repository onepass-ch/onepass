package ch.onepass.onepass.ui.navigation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.pass.PassRepositoryFirebase
import ch.onepass.onepass.model.scan.TicketScanRepositoryFirebase
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import ch.onepass.onepass.ui.auth.AuthScreen
import ch.onepass.onepass.ui.auth.AuthViewModel
import ch.onepass.onepass.ui.eventdetail.EventDetailScreen
import ch.onepass.onepass.ui.eventdetail.EventDetailViewModel
import ch.onepass.onepass.ui.eventform.createform.CreateEventForm
import ch.onepass.onepass.ui.eventform.createform.CreateEventFormViewModel
import ch.onepass.onepass.ui.eventform.editform.EditEventForm
import ch.onepass.onepass.ui.eventform.editform.EditEventFormViewModel
import ch.onepass.onepass.ui.feed.FeedScreen
import ch.onepass.onepass.ui.feed.GlobalSearchItemClick
import ch.onepass.onepass.ui.feed.GlobalSearchViewModel
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
import ch.onepass.onepass.ui.profile.editprofile.EditProfileScreen
import ch.onepass.onepass.ui.profile.editprofile.EditProfileViewModel
import ch.onepass.onepass.ui.profile.accountsettings.AccountSettingsScreen
import ch.onepass.onepass.ui.profile.accountsettings.AccountSettingsViewModel
import ch.onepass.onepass.ui.scan.ScanScreen
import ch.onepass.onepass.ui.scan.ScannerViewModel
import ch.onepass.onepass.ui.staff.StaffInvitationScreen
import ch.onepass.onepass.ui.staff.StaffInvitationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlin.math.absoluteValue

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
    },
    allowEventExactTime: Boolean = false
) {
  // Get context for DataStore
  val context = LocalContext.current

  // Create a single AuthViewModel instance for the entire nav host
  val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
  val authState by authViewModel.uiState.collectAsState()

  // Swipe navigation manager
  val swipeNavigationManager = remember { SwipeNavigationManager(navController) }

  // Register top-level screens for swipe navigation
  listOf(Screen.Events, Screen.Tickets, Screen.Map, Screen.Profile).forEach { screen ->
    swipeNavigationManager.register(screen)
  }

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
      var blockCardClick by remember { mutableStateOf(false) }

      SwipeWrapper(
          swipeNavigationManager = swipeNavigationManager,
          currentScreen = Screen.Events,
          onHorizontalSwipeStart = { blockCardClick = true },
          onGestureEnd = { blockCardClick = false }) {
            FeedScreen(
                onNavigateToEvent = { eventId ->
                  if (!blockCardClick) {
                    navController.navigate(Screen.EventDetail.route(eventId))
                  }
                },
                globalSearchItemClickListener = { item ->
                  when (item) {
                    is GlobalSearchItemClick.EventClick ->
                        navController.navigate(Screen.EventDetail.route(item.eventId))
                    is GlobalSearchItemClick.OrganizationClick ->
                        navController.navigate(
                            Screen.OrganizationProfile.route(item.organizationId))
                    is GlobalSearchItemClick.UserClick -> {
                      // Do nothing
                    }
                  }
                },
                onNavigateToNotifications = { navController.navigate(Screen.Notification.route) },
                globalSearchViewModel =
                    viewModel(
                        factory =
                            GlobalSearchViewModel.Factory(
                                userRepo = UserRepositoryFirebase(),
                                eventRepo = EventRepositoryFirebase(),
                                orgRepo = OrganizationRepositoryFirebase())))
          }
    }

    // ------------------ Notifications ------------------
    composable(Screen.Notification.route) {
      val notificationViewModel: NotificationsViewModel = viewModel()
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        NotificationsScreen(
            navController = navController,
            viewModel = notificationViewModel,
            onNavigateBack = { navController.popBackStack() })
      }
    }

    // ------------------ Event Detail ------------------
    composable(Screen.EventDetail.route) { backStackEntry ->
      val eventId = backStackEntry.arguments?.getString(Screen.EventDetail.ARG_EVENT_ID) ?: ""
      val eventDetailVm: EventDetailViewModel =
          viewModel(
              factory =
                  viewModelFactory { initializer { EventDetailViewModel(eventId = eventId) } })
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        EventDetailScreen(
            eventId = eventId,
            viewModel = eventDetailVm,
            onNavigateToMap = { navController.navigateToTopLevel(Screen.Map.route) },
            onNavigateToOrganizerProfile = { orgId ->
              navController.navigate(Screen.OrganizationProfile.route(orgId))
            },
            onBack = { navController.popBackStack() })
      }
    }

    // ------------------ Tickets (My Events) ------------------
    composable(Screen.Tickets.route) {
      SwipeWrapper(
          swipeNavigationManager = swipeNavigationManager, currentScreen = Screen.Tickets) {
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
                                        FirebaseFirestore.getInstance(),
                                        FirebaseFunctions.getInstance()),
                                userId = uid)
                          }
                        })
            MyEventsScreen(viewModel = myEventsVm)
          }
    }

    // ------------------ Map ------------------
    composable(Screen.Map.route) {
      SwipeWrapper(
          swipeNavigationManager = swipeNavigationManager,
          currentScreen = Screen.Map,
          canLeave = false) {
            // Each map screen will create its own ViewModel and handle its own location permission
            val mapScreenViewModel: MapViewModel = mapViewModel ?: viewModel()
            MapScreen(
                mapViewModel = mapScreenViewModel,
                onNavigateToEvent = { eventId ->
                  navController.navigate(Screen.EventDetail.route(eventId))
                })
          }
    }

    // ------------------ Profile ------------------
    composable(Screen.Profile.route) {
      SwipeWrapper(
          swipeNavigationManager = swipeNavigationManager, currentScreen = Screen.Profile) {
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
                        navController.navigate(Screen.AccountSettings.route)
                    ProfileEffect.NavigateToPaymentMethods ->
                        navController.navigate(Screen.ComingSoon.route)
                    ProfileEffect.NavigateToEditProfile ->
                        navController.navigate(Screen.EditProfile.route)
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
    }

    // ------------------ Organization Feed ------------------
    composable(Screen.OrganizationFeed.route) {
      val orgFeedVm: OrganizationFeedViewModel = viewModel()
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        OrganizationFeedScreen(
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            viewModel = orgFeedVm,
            onNavigateToOrganization = { organizationId ->
              navController.navigate(Screen.OrganizationDashboard.route(organizationId))
            },
            onNavigateBack = { navController.popBackStack() },
            onFabClick = { navController.navigate(Screen.BecomeOrganizer.route) })
      }
    }

    // ------------------ Organization Dashboard ------------------
    composable(Screen.OrganizationDashboard.route) { backStackEntry ->
      val organizationId =
          backStackEntry.arguments?.getString(Screen.OrganizationDashboard.ARG_ORGANIZATION_ID)
              ?: ""
      val orgDashboardVm: OrganizationDashboardViewModel =
          viewModel(factory = viewModelFactory { initializer { OrganizationDashboardViewModel() } })
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
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
    }

    // ------------------ Organization Profile ------------------
    composable(Screen.OrganizationProfile.route) { backStackEntry ->
      val organizationId =
          backStackEntry.arguments?.getString(Screen.OrganizationProfile.ARG_ORGANIZATION_ID) ?: ""
      val orgProfileVm: OrganizerProfileViewModel = viewModel()

      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
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
                is OrganizerProfileEffect.ShowError ->
                    navController.navigate(Screen.ComingSoon.route)
              }
            },
            onNavigateBack = { navController.popBackStack() })
      }
    }

    // ------------------ Create Event ------------------
    composable(Screen.CreateEvent.route) { backStackEntry ->
      val organizationId =
          backStackEntry.arguments?.getString(Screen.CreateEvent.ARG_ORGANIZATION_ID) ?: ""
      val createEventVm: CreateEventFormViewModel = viewModel()
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        CreateEventForm(
            organizationId = organizationId,
            viewModel = createEventVm,
            onNavigateBack = { navController.popBackStack() },
            onEventCreated = { navController.popBackStack() },
            allowExactTime = allowEventExactTime)
      }
    }

    // ------------------ Edit Event ------------------
    composable(Screen.EditEvent.route) { backStackEntry ->
      val eventId = backStackEntry.arguments?.getString(Screen.EditEvent.ARG_EVENT_ID) ?: ""
      val editEventVm: EditEventFormViewModel = viewModel()
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        EditEventForm(
            eventId = eventId,
            viewModel = editEventVm,
            onNavigateBack = { navController.popBackStack() },
            onEventUpdated = { navController.popBackStack() })
      }
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
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        ScanScreen(viewModel = scannerVm, onNavigateBack = { navController.popBackStack() })
      }
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
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        StaffInvitationScreen(
            viewModel = staffInvitationVm, onNavigateBack = { navController.popBackStack() })
      }
    }

    // ------------------ Become Organizer ------------------
    composable(Screen.BecomeOrganizer.route) {
      val becomeOrganizerVm: OrganizationFormViewModel = viewModel()
      val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
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
    }

    // ------------------ My Invitations ----------------
    composable(Screen.MyInvitations.route) {
      val myInvVm: MyInvitationsViewModel = viewModel()
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        MyInvitationsScreen(viewModel = myInvVm, onNavigateBack = { navController.popBackStack() })
      }
    }

    // ---------------- Edit Organization -------------
    composable(Screen.EditOrganization.route) { backStackEntry ->
      val organizationId =
          backStackEntry.arguments?.getString(Screen.EditOrganization.ARG_ORGANIZATION_ID) ?: ""
      val editVm: OrganizationEditorViewModel = viewModel()
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        EditOrganizationScreen(
            organizationId = organizationId,
            viewModel = editVm,
            onOrganizationUpdated = { navController.popBackStack() },
            onNavigateBack = { navController.popBackStack() })
      }
    }
    composable(Screen.AccountSettings.route) {
      val accountSettingsVm: AccountSettingsViewModel = viewModel()
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        AccountSettingsScreen(
            viewModel = accountSettingsVm,
            onNavigateBack = { navController.popBackStack() },
            onAccountDeleted = {
              // Sign out and go back to auth
              authViewModel.signOut()
              navController.navigate(Screen.Auth.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
              }
            })
      }
    }

    // ------------------ Edit Profile ------------------
    composable(Screen.EditProfile.route) {
      val context = LocalContext.current
      val editVm: EditProfileViewModel =
          viewModel(factory = viewModelFactory { initializer { EditProfileViewModel() } })

      EditProfileScreen(viewModel = editVm, onNavigateBack = { navController.popBackStack() })
    }
    // ------------------ Placeholders ------------------
    composable(Screen.ComingSoon.route) {
      SwipeBackWrapper(onSwipeBack = { navController.popBackStack() }) {
        ComingSoonScreen(onBack = { navController.popBackStack() })
      }
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

// ---------------- SWIPE MANAGEMENT ---------------- //

/**
 * Wrapper composable that detects swipe back gestures.
 *
 * @param onSwipeBack Lambda function to be invoked when a swipe back gesture is detected.
 * @param content Composable content to be wrapped.
 */
@Composable
fun SwipeBackWrapper(onSwipeBack: () -> Unit, content: @Composable () -> Unit) {
  val touchSlop = LocalViewConfiguration.current.touchSlop

  Box(
      modifier =
          Modifier.fillMaxSize().pointerInput(Unit) {
            detectHorizontalSwipe(
                touchSlop = touchSlop,
                onSwipeStart = {},
                onSwipeEnd = { totalDx ->
                  // Back = right swipe only
                  if (totalDx > 0f) {
                    onSwipeBack()
                  }
                })
          }) {
        content()
      }
}

/**
 * Wrapper composable that adds swipe navigation between top-level screens.
 *
 * @param swipeNavigationManager SwipeNavigationManager instance managing the swipeable screens.
 * @param currentScreen The current screen being displayed.
 * @param canLeave Whether swiping away from this screen is allowed.
 * @param content The content composable to display within the swipe wrapper.
 */
@Composable
fun SwipeWrapper(
    swipeNavigationManager: SwipeNavigationManager,
    currentScreen: Screen,
    canLeave: Boolean = true,
    onHorizontalSwipeStart: () -> Unit = {},
    onGestureEnd: () -> Unit = {},
    content: @Composable () -> Unit
) {
  if (!canLeave) {
    content()
    return
  }

  val navController = swipeNavigationManager.navController
  val screens = swipeNavigationManager.screens
  val currentIndex = screens.indexOf(currentScreen)
  val touchSlop = LocalViewConfiguration.current.touchSlop

  Box(
      modifier =
          Modifier.fillMaxSize().pointerInput(currentIndex) {
            detectHorizontalSwipe(
                touchSlop = touchSlop,
                onSwipeStart = onHorizontalSwipeStart,
                onSwipeEnd = { totalDx ->
                  handleNavigation(
                      totalDx = totalDx,
                      currentIndex = currentIndex,
                      screens = screens,
                      navigateTo = { s -> navController.navigateToTopLevel(s) })
                  onGestureEnd()
                })
          }) {
        content()
      }
}

/**
 * Detects a horizontal swipe gesture.
 *
 * @param touchSlop Minimum distance before a swipe is recognized
 * @param onSwipeStart Called when a horizontal swipe is first detected
 * @param onSwipeEnd Called when the gesture ends with the total horizontal delta
 */
private suspend fun PointerInputScope.detectHorizontalSwipe(
    touchSlop: Float,
    onSwipeStart: () -> Unit,
    onSwipeEnd: (totalDx: Float) -> Unit
) {
  awaitEachGesture {
    val down = awaitFirstDown(pass = PointerEventPass.Initial)

    var totalDx = 0f
    var totalDy = 0f
    var isHorizontalSwipe = false

    while (true) {
      val event = awaitPointerEvent(PointerEventPass.Initial)
      val change = event.changes.firstOrNull { it.id == down.id } ?: break
      if (!change.pressed) break

      val delta = change.positionChange()
      totalDx += delta.x
      totalDy += delta.y

      if (!isHorizontalSwipe && isPastHorizontalSlop(totalDx, totalDy, touchSlop)) {
        isHorizontalSwipe = true
        onSwipeStart()
        change.consume()
      } else if (isHorizontalSwipe) {
        change.consume()
      }
    }

    if (isHorizontalSwipe) {
      onSwipeEnd(totalDx)
    }
  }
}

/**
 * Checks whether accumulated movement qualifies as a horizontal swipe.
 *
 * @param dx Total horizontal movement
 * @param dy Total vertical movement
 * @param touchSlop Minimum distance before recognition
 * @return true if movement is mostly horizontal and past slop
 */
private fun isPastHorizontalSlop(dx: Float, dy: Float, touchSlop: Float): Boolean {
  val absDx = dx.absoluteValue
  val absDy = dy.absoluteValue
  return absDx > touchSlop && absDx > absDy * 2f
}

/**
 * Navigates to the adjacent screen based on swipe direction.
 *
 * @param totalDx Total horizontal movement of the swipe
 * @param currentIndex Index of the current screen
 * @param screens List of available screens
 * @param navigateTo Function used to navigate to a route
 */
private fun handleNavigation(
    totalDx: Float,
    currentIndex: Int,
    screens: List<Screen>,
    navigateTo: (String) -> Unit
) {
  when {
    totalDx > 0f && currentIndex > 0 -> navigateTo(screens[currentIndex - 1].route)
    totalDx < 0f && currentIndex < screens.lastIndex -> navigateTo(screens[currentIndex + 1].route)
  }
}
