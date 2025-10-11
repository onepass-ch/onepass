package ch.onepass.onepass.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MapScreenIntegrationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun mapScreenFullCoverageTest() {
    val viewModel = MapViewModel()

    // --- Set the content ---
    composeTestRule.setContent {
      MapScreen(mapViewModel = viewModel, isLocationPermissionGranted = true)
    }

    // --- Assert UI components ---
    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON)
        .assertIsDisplayed()
        .performClick() // triggers recenterCamera

    // --- Trigger remaining ViewModel functions ---
    viewModel.enableLocationTracking()
    viewModel.onMapStart()
    viewModel.onMapStop()
    viewModel.onMapLowMemory()
  }

  @Test
  fun mapScreenWithoutLocationPermission() {
    val viewModel = MapViewModel()

    composeTestRule.setContent {
      MapScreen(mapViewModel = viewModel, isLocationPermissionGranted = false)
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).assertIsDisplayed()
  }

  @Test
  fun mapScreenWithLocationPermission() {
    val viewModel = MapViewModel()

    composeTestRule.setContent {
      MapScreen(mapViewModel = viewModel, isLocationPermissionGranted = true)
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).assertIsDisplayed()
  }
}
