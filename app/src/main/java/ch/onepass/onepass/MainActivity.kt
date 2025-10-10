package ch.onepass.onepass

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.credentials.CredentialManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.auth.AuthScreen
import ch.onepass.onepass.ui.theme.OnePassTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      OnePassTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
            color = MaterialTheme.colorScheme.background) {
              OnePassApp()
            }
      }
    }
  }
}

sealed class Screen(val route: String) {
  object Auth : Screen("auth")

  object Main : Screen("main")
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier.semantics { testTag = C.Tag.greeting })
}

@Composable
fun MainScreen() {
  Greeting("OnePass User")
}

/**
 * `OnePassApp` is the main composable function that sets up the whole app UI. It initializes the
 * navigation controller and defines the navigation graph.
 *
 * @param context The context of the application, used for accessing resources and services.
 * @param credentialManager The CredentialManager instance for handling authentication credentials.
 */
@Composable
fun OnePassApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context)
) {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = Screen.Auth.route) {
    composable(Screen.Auth.route) {
      AuthScreen(
          credentialManager = credentialManager,
          onSignedIn = {
            navController.navigate(Screen.Main.route) {
              popUpTo(Screen.Auth.route) { inclusive = true }
            }
          })
    }
    composable(Screen.Main.route) { MainScreen() }
  }
}
