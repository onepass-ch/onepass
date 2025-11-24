package ch.onepass.onepass

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.navigation.AppNavHost
import ch.onepass.onepass.ui.navigation.BottomNavigationBar
import ch.onepass.onepass.ui.navigation.NavigationActions
import ch.onepass.onepass.ui.navigation.NavigationDestinations
import ch.onepass.onepass.ui.profile.ProfileViewModel
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.mapbox.common.MapboxOptions

/**
 * Main Activity that sets up Mapbox, the OnePass theme and hosts the root composable navigation.
 */
class MainActivity : ComponentActivity() {

  // Map screen ViewModel (for lifecycle delegation)
  private val mapViewModel: MapViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Mapbox access token
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

    setContent {
      OnePassTheme { MainActivityContent(mapViewModel = mapViewModel, context = this@MainActivity) }
    }
  }
}

/**
 * Root composable for the main activity content, responsible for setting up permission handling,
 * ViewModel state collection, and theming for the app.
 *
 * @param mapViewModel The [MapViewModel] instance controlling map UI state and logic.
 * @param context The [Context] used for permission checks and launching permission requests.
 */
@Composable
internal fun MainActivityContent(mapViewModel: MapViewModel, context: Context) {
  val uiState by mapViewModel.uiState.collectAsState()

  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        mapViewModel.setLocationPermission(isGranted)
      }

  LaunchedEffect(Unit) {
    if (!uiState.hasLocationPermission) {
      val hasPermission =
          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
              PackageManager.PERMISSION_GRANTED

      mapViewModel.setLocationPermission(hasPermission)

      if (!hasPermission) {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
      }
    }
  }

  Surface(
      modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
      color = MaterialTheme.colorScheme.background) {
        OnePassApp(mapViewModel = mapViewModel)
      }
}

/**
 * Root composable wiring navigation + bottom bar. Uses NavigationActions/NavigationDestinations and
 * AppNavHost.
 *
 * @param mapViewModel ViewModel used by the Map screen to handle lifecycle and map state.
 * @param testAuthButtonTag Optional test tag to display a simplified login button for tests.
 * @param authViewModelFactory Factory that creates the AuthViewModel instance for the auth flow.
 * @param navController Navigation controller used for navigation within the app.
 * @param profileViewModelFactory Optional factory to create the ProfileViewModel instance.
 */
@Composable
fun OnePassApp(
    mapViewModel: MapViewModel,
    testAuthButtonTag: String? = null,
    authViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
      initializer { ch.onepass.onepass.ui.auth.AuthViewModel() }
    },
    navController: NavHostController = rememberNavController(),
    profileViewModelFactory: ViewModelProvider.Factory? = viewModelFactory {
      initializer { ProfileViewModel() }
    }
) {
  val navActions = NavigationActions(navController)

  // Which route are we on?
  val backstack by navController.currentBackStackEntryAsState()
  val currentRoute = backstack?.destination?.route

  // Show bottom bar only on top-level destinations
  val topLevelRoutes = NavigationDestinations.tabs.map { it.destination.route }
  val showBottomBar = currentRoute in topLevelRoutes

  Scaffold(
      bottomBar = {
        if (showBottomBar) {
          BottomNavigationBar(
              currentRoute = currentRoute ?: NavigationDestinations.Screen.Events.route,
              onNavigate = { screen -> navActions.navigateTo(screen) })
        }
      }) { padding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(padding),
            mapViewModel = mapViewModel,
            testAuthButtonTag = testAuthButtonTag,
            authViewModelFactory = authViewModelFactory,
            profileViewModelFactory = profileViewModelFactory)
      }
}
