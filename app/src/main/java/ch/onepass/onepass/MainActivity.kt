package ch.onepass.onepass

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.core.app.ActivityCompat
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.map.MapScreen
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.theme.OnePassTheme
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

    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

    val permission = Manifest.permission.ACCESS_FINE_LOCATION
    val isLocationPermissionGranted =
        ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    if (!isLocationPermissionGranted) {
      requestPermissionLauncher.launch(permission)
    }

    setContent {
      OnePassTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
            color = MaterialTheme.colorScheme.background,
        ) {
          MapScreen(isLocationPermissionGranted = isLocationPermissionGranted)
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
