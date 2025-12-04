package ch.onepass.onepass

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import ch.onepass.onepass.model.device.DeviceToken
import ch.onepass.onepass.model.device.DeviceTokenRepositoryFirebase
import ch.onepass.onepass.resources.C
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
import com.stripe.android.PaymentConfiguration
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main Activity that sets up Mapbox, Stripe, the OnePass theme and hosts the root composable
 * navigation.
 */
class MainActivity : ComponentActivity() {

  private val deviceTokenRepository by lazy { DeviceTokenRepositoryFirebase() }
  private lateinit var authStateListener: FirebaseAuth.AuthStateListener
  private var tokenStoreRetries = 0
  private val MAX_RETRIES = 3

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
    authStateListener =
        FirebaseAuth.AuthStateListener { auth ->
          if (auth.currentUser != null) {
            // User just signed in - store their device token so we can send them notifications
            Log.d("OneSignal", "User authenticated, storing token...")
            lifecycleScope.launch {
              // Wait for OneSignal to fully initialize and obtain a player ID
              delay(2000)
              storeDeviceToken()
            }
          } else {
            // User signed out - reset retry counter for next sign-in attempt
            Log.d("OneSignal", "User signed out")
            tokenStoreRetries = 0
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

  /**
   * Stores the device's OneSignal player ID in Firestore, linked to the current user. This allows
   * the backend to send targeted push notifications to specific users.
   *
   * The player ID is OneSignal's unique identifier for this device. We also store device metadata
   * (model, OS, app version) for debugging and analytics.
   *
   * Retries up to MAX_RETRIES times if the player ID isn't available yet, since OneSignal may still
   * be initializing.
   */
  @SuppressLint("HardwareIds")
  private fun storeDeviceToken() {
    // Get OneSignal's unique identifier for this device (empty if not yet initialized)
    val playerId = OneSignal.User.pushSubscription.id
    val currentUser = FirebaseAuth.getInstance().currentUser

    // OneSignal may not have a player ID yet - retry with exponential backoff
    if (playerId.isEmpty()) {
      if (tokenStoreRetries < MAX_RETRIES) {
        tokenStoreRetries++
        Log.w("OneSignal", "Player ID empty, retry $tokenStoreRetries/$MAX_RETRIES")
        lifecycleScope.launch {
          delay(2000) // Wait before retrying
          storeDeviceToken()
        }
      } else {
        // Give up after MAX_RETRIES - user can still use app, just won't get notifications
        Log.e("OneSignal", "Max retries reached, could not store token")
      }
      return
    }

    // Success - reset retry counter for future token updates
    tokenStoreRetries = 0

    currentUser?.let { user ->
      // Gather device info for debugging and notification targeting
      val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
      val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}" // e.g., "Google Pixel 7"
      val appVersion =
          try {
            packageManager.getPackageInfo(packageName, 0).versionName
          } catch (_: Exception) {
            "unknown"
          }

      // Create token object to store in Firestore at: users/{userId}/device_tokens/{deviceId}
      val deviceToken =
          DeviceToken(
              deviceId = deviceId,
              oneSignalPlayerId = playerId,
              platform = "android",
              deviceModel = deviceModel,
              appVersion = appVersion,
              isActive = true)

      // Save to Firestore asynchronously
      lifecycleScope.launch {
        deviceTokenRepository
            .saveDeviceToken(user.uid, deviceToken)
            .onSuccess { Log.d("OneSignal", "Device token saved: $playerId for device: $deviceId") }
            .onFailure { e -> Log.e("OneSignal", "Failed to save device token", e) }
      }
    }
        ?: run {
          // Edge case: user signed out between auth listener firing and this point
          Log.w("OneSignal", "User not authenticated, cannot store device token")
        }
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
