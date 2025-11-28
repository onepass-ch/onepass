package ch.onepass.onepass

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import ch.onepass.onepass.resources.C
import com.mapbox.common.MapboxOptions
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivityContentTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
  }

  @Test
  fun mainActivityContent_setsUpOnePassApp() {
    composeTestRule.setContent { MainActivityContent() }

    composeTestRule.onNodeWithTag(C.Tag.main_screen_container).assertExists()
  }
}
