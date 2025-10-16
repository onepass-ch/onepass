package ch.onepass.onepass

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.navigation.AppNavHost
import ch.onepass.onepass.ui.navigation.BottomNavigationBar
import ch.onepass.onepass.ui.navigation.NavigationActions
import ch.onepass.onepass.ui.navigation.NavigationDestinations
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.mapbox.common.MapboxOptions

class MainActivity : ComponentActivity() {

  // Map screen ViewModel (for lifecycle delegation)
  private val mapViewModel: MapViewModel by viewModels()

  // Location permission launcher
  private val requestPermissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) mapViewModel.enableLocationTracking()
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Mapbox access token
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

    setContent {
      OnePassTheme {
        // Track permission state once (saveable across config changes)
        var hasLocationPermission by rememberSaveable {
          mutableStateOf(
              ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                  PackageManager.PERMISSION_GRANTED)
        }

        // Ask for permission if not granted; enable tracking otherwise
        if (!hasLocationPermission) {
          requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
          // The launcher callback will call enableLocationTracking() if granted
        } else {
          mapViewModel.enableLocationTracking()
        }

        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
            color = MaterialTheme.colorScheme.background) {
              OnePassApp(
                  mapViewModel = mapViewModel, isLocationPermissionGranted = hasLocationPermission)
            }
      }
    }
  }

  // --- MapView lifecycle delegation via ViewModel ---
  override fun onStart() {
    super.onStart()
    mapViewModel.onMapStart()
  }

  override fun onStop() {
    super.onStop()
    mapViewModel.onMapStop()
  }

  override fun onLowMemory() {
    super.onLowMemory()
    mapViewModel.onMapLowMemory()
  }
}

/**
 * Root composable wiring navigation + bottom bar. Uses NavigationActions/NavigationDestinations and
 * AppNavHost.
 */
@Composable
private fun OnePassApp(
    mapViewModel: MapViewModel,
    isLocationPermissionGranted: Boolean,
) {
  val navController = rememberNavController()
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
            isLocationPermissionGranted = isLocationPermissionGranted)
      }
}
