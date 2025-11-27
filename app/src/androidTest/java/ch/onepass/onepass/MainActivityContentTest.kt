package ch.onepass.onepass

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import ch.onepass.onepass.resources.C
import ch.onepass.onepass.ui.map.MapViewModel
import com.mapbox.common.MapboxOptions
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivityContentTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockMapViewModel: MapViewModel

  @Before
  fun setup() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    mockMapViewModel = mockk(relaxed = true)
  }

  @Test
  fun mainActivityContent_setsUpOnePassApp() {
    composeTestRule.setContent { MainActivityContent(mapViewModel = mockMapViewModel) }

    composeTestRule.onNodeWithTag(C.Tag.main_screen_container).assertExists()
  }
}
