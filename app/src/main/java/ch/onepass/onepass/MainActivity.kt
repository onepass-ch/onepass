package ch.onepass.onepass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.auth.AuthViewModel
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.navigation.AppNavHost
import ch.onepass.onepass.ui.navigation.BottomNavigationBar
import ch.onepass.onepass.ui.navigation.NavigationDestinations
import ch.onepass.onepass.ui.navigation.navigateToTopLevel
import ch.onepass.onepass.ui.profile.ProfileViewModel
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.mapbox.common.MapboxOptions

/**
 * Main Activity that sets up Mapbox, Stripe, the OnePass theme and hosts the root composable
 * navigation.
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Mapbox access token
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    setContent { OnePassTheme { MainActivityContent() } }
  }
}

/**
 * Root composable for the main activity content, responsible for setting up permission handling,
 * ViewModel state collection, and theming for the app.
 */
@Composable
internal fun MainActivityContent() {
  Surface(
      modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
      color = MaterialTheme.colorScheme.background) {
        OnePassApp() // Let each map screen create its own ViewModel
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
    mapViewModel: MapViewModel? = null,
    testAuthButtonTag: String? = null,
    authViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
      initializer { AuthViewModel() }
    },
    navController: NavHostController = rememberNavController(),
    profileViewModelFactory: ViewModelProvider.Factory? = viewModelFactory {
      initializer { ProfileViewModel() }
    }
) {
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
              onNavigate = { screen -> navController.navigateToTopLevel(screen.route) })
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
