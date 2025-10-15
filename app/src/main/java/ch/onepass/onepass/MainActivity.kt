package ch.onepass.onepass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import ch.onepass.onepass.ui.profile.*
import ch.onepass.onepass.ui.theme.OnePassTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {

  // --- Simple fake VM for demo/run purposes only ---
  private class FakeProfileViewModel(
    initial: ProfileUiState = ProfileUiState(
      displayName = "Will Smith",
      email = "willsmith@email.com",
      avatarUrl = null,
      initials = "WS",
      stats = ProfileStats(events = 12, upcoming = 3, saved = 7),
      isOrganizer = false,
      loading = false
    )
  ) : ProfileViewModel() {
    private val stateFlow = MutableStateFlow(initial)
    override val state: StateFlow<ProfileUiState> = stateFlow
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val demoVm = FakeProfileViewModel()

    setContent {
      OnePassTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          // Show the Profile screen with demo data
          ProfileScreen(
            viewModel = demoVm,
            onEffect = { /* TODO: no-op for now; hook up when navigation is ready */ }
          )
        }
      }
    }
  }

  // If you previously forwarded Mapbox lifecycle here, you can remove/keep these empty.
  override fun onStart() { super.onStart() }
  override fun onStop() { super.onStop() }
  override fun onLowMemory() { super.onLowMemory() }
  override fun onDestroy() { super.onDestroy() }
}
