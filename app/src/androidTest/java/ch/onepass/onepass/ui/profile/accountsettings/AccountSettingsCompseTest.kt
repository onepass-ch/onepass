package ch.onepass.onepass.ui.profile.accountsettings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ch.onepass.onepass.model.user.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccountSettingsScreenComposeTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser

  @Before
  fun setup() {
    mockUserRepository = mockk(relaxed = true)
    mockAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    every { mockAuth.currentUser } returns mockFirebaseUser
    every { mockFirebaseUser.uid } returns "test-uid"
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private class FakeAccountSettingsViewModel(state: AccountSettingsUiState) :
      AccountSettingsViewModel(mockk(relaxed = true), mockk(relaxed = true)) {
    private val fakeState = MutableStateFlow(state)
    override val uiState: StateFlow<AccountSettingsUiState>
      get() = fakeState
  }

  @Test
  fun loading_showsLoadingOverlay() {
    val state = AccountSettingsUiState(isLoading = true)
    val viewModel = FakeAccountSettingsViewModel(state)

    composeTestRule.setContent {
      AccountSettingsScreen(viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = {})
    }

    // Just check for the CircularProgressIndicator
    composeTestRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        .assertExists()
  }

  @Test
  fun permissionsSection_displaysAllToggles() {
    val state =
        AccountSettingsUiState(
            notificationsEnabled = true, locationEnabled = false, cameraEnabled = true)
    val viewModel = FakeAccountSettingsViewModel(state)

    composeTestRule.setContent {
      AccountSettingsScreen(viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = {})
    }

    composeTestRule.onNodeWithText("PERMISSIONS").assertIsDisplayed()
    composeTestRule.onNodeWithText("Location Access").assertIsDisplayed()
    composeTestRule.onNodeWithText("Camera Access").assertIsDisplayed()
    composeTestRule.onNodeWithText("See events near you on the map").assertIsDisplayed()
    composeTestRule.onNodeWithText("Required for scanning tickets").assertIsDisplayed()
  }

  @Test
  fun notificationToggle_displaysOnTiramisuAndAbove() {
    val state = AccountSettingsUiState(notificationsEnabled = true)
    val viewModel = FakeAccountSettingsViewModel(state)

    composeTestRule.setContent {
      AccountSettingsScreen(viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = {})
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      composeTestRule.onNodeWithText("Push Notifications").assertIsDisplayed()
      composeTestRule.onNodeWithText("Receive updates about your events").assertIsDisplayed()
    } else {
      composeTestRule.onNodeWithText("Push Notifications").assertDoesNotExist()
    }
  }

  @Test
  fun privacySection_displaysPrivacyToggles() {
    val state = AccountSettingsUiState(showEmail = true, analyticsEnabled = false)
    val viewModel = FakeAccountSettingsViewModel(state)

    composeTestRule.setContent {
      AccountSettingsScreen(viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = {})
    }

    composeTestRule.onNodeWithText("PRIVACY").assertIsDisplayed()
    composeTestRule.onNodeWithText("Show Email on Profile").assertIsDisplayed()
    composeTestRule.onNodeWithText("Make your email visible to other users").assertIsDisplayed()
    composeTestRule.onNodeWithText("Data Usage").assertIsDisplayed()
    composeTestRule.onNodeWithText("Allow usage analysis to improve the app").assertIsDisplayed()
  }

  @Test
  fun dangerZone_displaysDeleteSection() {
    val state = AccountSettingsUiState()
    val viewModel = FakeAccountSettingsViewModel(state)

    composeTestRule.setContent {
      AccountSettingsScreen(viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = {})
    }

    // Scroll to the danger zone section and wait for it to settle
    composeTestRule.onNodeWithText("DANGER ZONE").performScrollTo()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("DANGER ZONE").assertIsDisplayed()

    composeTestRule.onNodeWithText("DELETE ACCOUNT").assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            "Permanently remove your account and all associated data. This action cannot be undone.")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete Account").assertIsDisplayed()
  }

  @Test
  fun deleteButton_showsConfirmationDialog() {
    val state = AccountSettingsUiState()
    val viewModel = FakeAccountSettingsViewModel(state)

    composeTestRule.setContent {
      AccountSettingsScreen(viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = {})
    }

    composeTestRule.onNodeWithText("Delete Account").performScrollTo().performClick()

    // Wait for dialog to appear
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText("Are you sure?").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Are you sure?").assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            "You are about to permanently delete your account. You will lose all your tickets, events, and history.")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete Forever").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun deleteDialog_cancelButton_dismissesDialog() {
    val state = AccountSettingsUiState()
    val viewModel = FakeAccountSettingsViewModel(state)

    composeTestRule.setContent {
      AccountSettingsScreen(viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = {})
    }

    composeTestRule.onNodeWithText("Delete Account").performScrollTo().performClick()

    // Wait for dialog to appear
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText("Cancel").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.onNodeWithText("Are you sure?").assertDoesNotExist()
  }

  @Test
  fun backButton_triggersNavigationCallback() {
    val state = AccountSettingsUiState()
    val viewModel = FakeAccountSettingsViewModel(state)
    var navigateBackCalled = false

    composeTestRule.setContent {
      AccountSettingsScreen(
          viewModel = viewModel,
          onNavigateBack = { navigateBackCalled = true },
          onAccountDeleted = {})
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()

    assert(navigateBackCalled)
  }

  @Test
  fun topBar_displaysTitle() {
    val state = AccountSettingsUiState()
    val viewModel = FakeAccountSettingsViewModel(state)

    composeTestRule.setContent {
      AccountSettingsScreen(viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = {})
    }

    composeTestRule.onNodeWithText("Account Settings").assertIsDisplayed()
  }

  @Test
  fun errorMessage_displaysSnackbar() {
    val state = AccountSettingsUiState(error = "Test error message")
    val viewModel = FakeAccountSettingsViewModel(state)

    composeTestRule.setContent {
      AccountSettingsScreen(viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = {})
    }

    composeTestRule.onNodeWithText("Test error message").assertIsDisplayed()
  }

  @Test
  fun accountDeleted_triggersCallback() {
    val state = AccountSettingsUiState(isAccountDeleted = true)
    val viewModel = FakeAccountSettingsViewModel(state)
    var onDeletedCalled = false

    composeTestRule.setContent {
      AccountSettingsScreen(
          viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = { onDeletedCalled = true })
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) { onDeletedCalled }
    assert(onDeletedCalled)
  }

  @Test
  fun permissionToggles_areScrollable() {
    val state = AccountSettingsUiState()
    val viewModel = FakeAccountSettingsViewModel(state)

    composeTestRule.setContent {
      AccountSettingsScreen(viewModel = viewModel, onNavigateBack = {}, onAccountDeleted = {})
    }

    composeTestRule.onNodeWithText("PERMISSIONS").assertIsDisplayed()
    composeTestRule.onNodeWithText("DANGER ZONE").performScrollTo().assertIsDisplayed()
  }
}
