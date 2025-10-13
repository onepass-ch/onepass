package ch.onepass.onepass.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.mapbox.common.MapboxOptions
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapScreenIntegrationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    // Set the Mapbox token before any tests run
    MapboxOptions.accessToken = ch.onepass.onepass.BuildConfig.MAPBOX_ACCESS_TOKEN
  }

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
