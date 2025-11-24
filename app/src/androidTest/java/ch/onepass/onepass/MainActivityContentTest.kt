package ch.onepass.onepass

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.ui.map.MapUIState
import ch.onepass.onepass.ui.map.MapViewModel
import com.mapbox.common.MapboxOptions
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityContentTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockMapViewModel: MapViewModel
  private lateinit var mockContext: Context

  @Before
  fun setup() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    mockMapViewModel = mockk(relaxed = true)
    mockContext = mockk(relaxed = true)

    // Mock the static ContextCompat method
    mockkStatic(ContextCompat::class)
  }

  @After
  fun tearDown() {
    unmockkStatic(ContextCompat::class)
  }

  @Test
  fun mainActivityContent_whenPermissionNotGranted_callsSetLocationPermission() {
    val uiStateFlow = MutableStateFlow(MapUIState(hasLocationPermission = false))
    every { mockMapViewModel.uiState } returns uiStateFlow

    // Mock the static method to return PERMISSION_DENIED
    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_DENIED

    composeTestRule.setContent {
      MainActivityContent(mapViewModel = mockMapViewModel, context = mockContext)
    }

    composeTestRule.waitForIdle()

    verify { mockMapViewModel.setLocationPermission(false) }
  }

  @Test
  fun mainActivityContent_whenPermissionGranted_callsSetLocationPermissionTrue() {
    val uiStateFlow = MutableStateFlow(MapUIState(hasLocationPermission = false))
    every { mockMapViewModel.uiState } returns uiStateFlow

    // Mock the static method to return PERMISSION_GRANTED
    every {
      ContextCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns PackageManager.PERMISSION_GRANTED

    composeTestRule.setContent {
      MainActivityContent(mapViewModel = mockMapViewModel, context = mockContext)
    }

    composeTestRule.waitForIdle()

    verify { mockMapViewModel.setLocationPermission(true) }
  }

  @Test
  fun mainActivityContent_whenPermissionAlreadyGrantedInState_doesNotCheckPermission() {
    val uiStateFlow = MutableStateFlow(MapUIState(hasLocationPermission = true))
    every { mockMapViewModel.uiState } returns uiStateFlow

    composeTestRule.setContent {
      MainActivityContent(mapViewModel = mockMapViewModel, context = mockContext)
    }

    composeTestRule.waitForIdle()

    // LaunchedEffect should not execute when permission already granted
    verify(exactly = 0) { mockMapViewModel.setLocationPermission(any()) }
  }
}
