package ch.onepass.onepass

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.onepass.onepass.model.device.DeviceTokenRepositoryFirebase
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.service.DeviceTokenManager
import ch.onepass.onepass.ui.auth.AuthViewModel
import ch.onepass.onepass.ui.map.MapViewModel
import ch.onepass.onepass.ui.navigation.AppNavHost
import ch.onepass.onepass.ui.navigation.BottomNavigationBar
import ch.onepass.onepass.ui.navigation.NavigationDestinations
import ch.onepass.onepass.ui.navigation.navigateToTopLevel
import ch.onepass.onepass.ui.payment.LocalPaymentSheet
import ch.onepass.onepass.ui.payment.createPaymentSheet
import ch.onepass.onepass.ui.profile.ProfileViewModel
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.common.MapboxOptions
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.stripe.android.PaymentConfiguration
import kotlinx.coroutines.launch

/**
 * Main Activity that sets up Mapbox, Stripe, the OnePass theme and hosts the root composable
 * navigation.
 */
class MainActivity : ComponentActivity() {

  private val deviceTokenRepository by lazy { DeviceTokenRepositoryFirebase() }
  private lateinit var authStateListener: FirebaseAuth.AuthStateListener
  private lateinit var deviceTokenManager: DeviceTokenManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Mapbox access token
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    OneSignal.Debug.logLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN
    OneSignal.initWithContext(this, BuildConfig.ONESIGNAL_APP_ID)
    // Request notification permission (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      lifecycleScope.launch { OneSignal.Notifications.requestPermission(fallbackToSettings = true) }
    }
    /**
     * Listens for Firebase authentication state changes. When a user signs in, we wait briefly for
     * OneSignal to initialize, then store their device token for push notification targeting.
     */
    deviceTokenManager =
        DeviceTokenManager(
            deviceTokenRepository = deviceTokenRepository,
            context = this,
            playerIdProvider = { OneSignal.User.pushSubscription.id },
            currentUserIdProvider = { FirebaseAuth.getInstance().currentUser?.uid })
    authStateListener =
        FirebaseAuth.AuthStateListener { auth ->
          if (auth.currentUser != null) {
            Log.d("OneSignal", "User authenticated, storing token...")
            lifecycleScope.launch { deviceTokenManager.storeDeviceToken() }
          } else {
            Log.d("OneSignal", "User signed out")
            deviceTokenManager.resetRetries()
          }
        }

    // Register the auth listener to be notified of sign-in/sign-out events
    FirebaseAuth.getInstance().addAuthStateListener(authStateListener)

    // Initialize Stripe
    val stripePublishableKey = BuildConfig.STRIPE_PUBLISHABLE_KEY
    if (stripePublishableKey.isNotEmpty()) {
      PaymentConfiguration.init(applicationContext, stripePublishableKey)
    }

    // Create PaymentSheet instance early in onCreate to avoid lifecycle registration issues
    val paymentSheet =
        if (stripePublishableKey.isNotEmpty()) {
          createPaymentSheet(this)
        } else {
          null
        }

    setContent {
      OnePassTheme {
        CompositionLocalProvider(LocalPaymentSheet provides paymentSheet) { MainActivityContent() }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    // Clean up: remove auth listener to prevent memory leaks
    FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
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
