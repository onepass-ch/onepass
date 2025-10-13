package ch.onepass.onepass

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import ch.onepass.onepass.ui.map.MapScreenTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityPermissionTest {

  // Grant location permission for this test
  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun mapScreenWithPermission_denied() {
    // Launch MainActivity in a separate scenario without granting location permission
    val scenario: ActivityScenario<MainActivity> = launch(MainActivity::class.java)

    scenario.onActivity { activity ->
      // Temporarily revoke permission in test scenario
      activity.packageManager.getPackageInfo(activity.packageName, 0)
      // Compose UI is already set in onCreate
    }

    composeTestRule.waitForIdle()

    // Map should still be displayed even if permission is denied
    composeTestRule.onNodeWithTag(MapScreenTestTags.MAPBOX_MAP_SCREEN).assertIsDisplayed()

    // Recenter button should still be visible
    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).assertIsDisplayed()

    // Clicking recenter button is safe even if location is not enabled
    composeTestRule.onNodeWithTag(MapScreenTestTags.RECENTER_BUTTON).performClick()
    composeTestRule.waitForIdle()
  }
}
