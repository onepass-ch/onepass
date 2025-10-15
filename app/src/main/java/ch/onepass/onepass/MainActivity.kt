package ch.onepass.onepass

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.core.content.ContextCompat
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.MapEventsTestScript
import ch.onepass.onepass.ui.map.MapScreen
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.mapbox.common.MapboxOptions
import kotlin.getValue

class MainActivity : ComponentActivity() {
  private val mapViewModel: MapViewModel by viewModels()

  private val requestPermissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
          mapViewModel.enableLocationTracking()
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // TEMPORARY - REMOVE AFTER TESTING!
    if (BuildConfig.DEBUG) {
      Handler(Looper.getMainLooper())
          .postDelayed(
              {
                android.util.Log.d("MainActivity", "🕒 2 seconds passed, adding test events...")
                MapEventsTestScript.populateTestEvents()
              },
              2000)
    }

    Firebase.firestore.useEmulator("10.0.2.2", 8080)

    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

    setContent {
      OnePassTheme {
        // Track permission state once when the app starts
        var hasPermission by rememberSaveable {
          mutableStateOf(
              ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                  PackageManager.PERMISSION_GRANTED)
        }
        // Ask for permission once on startup
        LaunchedEffect(Unit) {
          if (!hasPermission) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
          } else {
            mapViewModel.enableLocationTracking()
          }
        }
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
            color = MaterialTheme.colorScheme.background,
        ) {
          MapScreen(isLocationPermissionGranted = hasPermission)
        }
      }
    }
  }

  // --- MapView Lifecycle Delegation ---

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

  override fun onDestroy() {
    super.onDestroy()
    // Map destruction is handled by ViewModel's onCleared()
  }
}
