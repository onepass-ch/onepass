package ch.onepass.onepass.ui.accountsettings

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.ui.profile.accountsettings.AccountSettingsViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSettingsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  // Reusable test data
  private val testUserId = "test-uid"
  private val testUser =
      User(
          uid = testUserId,
          email = "test@test.com",
          displayName = "testuser",
          showEmail = true,
          analyticsEnabled = false)

  private lateinit var mockContext: Context

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk(relaxed = true)

    mockkStatic(FirebaseAnalytics::class)
    every { FirebaseAnalytics.getInstance(any()) } returns mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  // ========================================
  // Helper Functions
  // ========================================

  private fun createUserRepository(user: User? = testUser): UserRepository =
      mockk(relaxed = true) {
        coEvery { getCurrentUser() } returns user
        coEvery { updateUserField(any(), any(), any()) } returns Result.success(Unit)
      }

  private fun createUserRepositoryWithUpdateFailure(
      user: User? = testUser,
      errorMessage: String = "Update failed"
  ): UserRepository =
      mockk(relaxed = true) {
        coEvery { getCurrentUser() } returns user
        coEvery { updateUserField(any(), any(), any()) } returns
            Result.failure(Exception(errorMessage))
      }

  private fun createFirebaseAuth(userId: String? = testUserId): FirebaseAuth =
      mockk(relaxed = true) {
        val firebaseUser: FirebaseUser? =
            userId?.let { mockk(relaxed = true) { every { uid } returns it } }
        every { currentUser } returns firebaseUser
      }

  private fun createFirebaseAuthWithDeleteSuccess(userId: String = testUserId): FirebaseAuth =
      mockk(relaxed = true) {
        val mockDeleteTask: Task<Void> = mockk(relaxed = true)
        every { mockDeleteTask.addOnSuccessListener(any()) } answers
            {
              val listener = firstArg<OnSuccessListener<Void>>()
              listener.onSuccess(null)
              mockDeleteTask
            }
        every { mockDeleteTask.addOnFailureListener(any()) } returns mockDeleteTask

        val firebaseUser: FirebaseUser =
            mockk(relaxed = true) {
              every { uid } returns userId
              every { delete() } returns mockDeleteTask
            }
        every { currentUser } returns firebaseUser
      }

  private fun createFirebaseAuthWithDeleteFailure(
      userId: String = testUserId,
      errorMessage: String = "Delete failed"
  ): FirebaseAuth =
      mockk(relaxed = true) {
        val mockDeleteTask: Task<Void> = mockk(relaxed = true)
        every { mockDeleteTask.addOnSuccessListener(any()) } returns mockDeleteTask
        every { mockDeleteTask.addOnFailureListener(any()) } answers
            {
              val listener = firstArg<OnFailureListener>()
              listener.onFailure(Exception(errorMessage))
              mockDeleteTask
            }

        val firebaseUser: FirebaseUser =
            mockk(relaxed = true) {
              every { uid } returns userId
              every { delete() } returns mockDeleteTask
            }
        every { currentUser } returns firebaseUser
      }

  private fun mockPermissions(vararg permissions: Pair<String, Boolean>) {
    mockkStatic(ContextCompat::class)
    permissions.forEach { (permission, granted) ->
      every { ContextCompat.checkSelfPermission(mockContext, permission) } returns
          if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }
  }

  // ========================================
  // Tests for Initial State
  // ========================================

  @Test
  fun initialState_hasCorrectDefaults() = runTest {
    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuth()
    val viewModel = AccountSettingsViewModel(userRepository, auth)

    val state = viewModel.uiState.value

    assertFalse(state.isLoading)
    assertNull(state.error)
    assertFalse(state.isAccountDeleted)
    assertFalse(state.notificationsEnabled)
    assertFalse(state.locationEnabled)
    assertFalse(state.cameraEnabled)
    assertFalse(state.showEmail)
    assertTrue(state.analyticsEnabled)
  }

  // ========================================
  // Tests for Loading User Preferences
  // ========================================

  @Test
  fun init_loadsUserPreferences() = runTest {
    val userRepository = createUserRepository()
    val auth = createFirebaseAuth()
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.showEmail)
    assertFalse(state.analyticsEnabled)
  }

  @Test
  fun init_whenUserIsNull_usesDefaultPreferences() = runTest {
    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuth()
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.showEmail)
    assertTrue(state.analyticsEnabled)
  }

  // ========================================
  // Tests for Permission Checking
  // ========================================

  @Test
  fun checkPermissions_allGranted_updatesStateCorrectly() = runTest {
    mockPermissions(
        "android.permission.POST_NOTIFICATIONS" to true,
        "android.permission.ACCESS_FINE_LOCATION" to true,
        "android.permission.CAMERA" to true)

    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuth()
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.checkPermissions(mockContext)

    val state = viewModel.uiState.first()
    assertTrue(state.cameraEnabled)
    assertTrue(state.locationEnabled)

    unmockkStatic(ContextCompat::class)
  }

  @Test
  fun checkPermissions_allDenied_updatesStateCorrectly() = runTest {
    mockPermissions(
        "android.permission.POST_NOTIFICATIONS" to false,
        "android.permission.ACCESS_FINE_LOCATION" to false,
        "android.permission.CAMERA" to false)

    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuth()
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.checkPermissions(mockContext)

    val state = viewModel.uiState.first()
    assertFalse(state.cameraEnabled)
    assertFalse(state.locationEnabled)

    unmockkStatic(ContextCompat::class)
  }

  // ========================================
  // Tests for Toggle Show Email
  // ========================================

  @Test
  fun toggleShowEmail_updatesStateOptimistically() = runTest {
    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuth()
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleShowEmail(true)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.showEmail)
    coVerify { userRepository.updateUserField(testUserId, "showEmail", true) }
  }

  @Test
  fun toggleShowEmail_revertsOnFailure() = runTest {
    val userRepository = createUserRepositoryWithUpdateFailure(user = null)
    val auth = createFirebaseAuth()
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleShowEmail(true)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.showEmail)
  }

  @Test
  fun toggleShowEmail_whenNoUser_doesNothing() = runTest {
    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuth(userId = null)
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleShowEmail(true)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { userRepository.updateUserField(any(), any(), any()) }
  }

  // ========================================
  // Tests for Toggle Analytics
  // ========================================

  @Test
  fun toggleAnalytics_updatesStateAndCallsFirebase() = runTest {
    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuth()
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleAnalytics(mockContext, false)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.analyticsEnabled)
    coVerify { userRepository.updateUserField(testUserId, "analyticsEnabled", false) }
  }

  @Test
  fun toggleAnalytics_revertsOnFailure() = runTest {
    val userRepository = createUserRepositoryWithUpdateFailure(user = null)
    val auth = createFirebaseAuth()
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleAnalytics(mockContext, false)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.analyticsEnabled)
  }

  @Test
  fun toggleAnalytics_whenNoUser_doesNothing() = runTest {
    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuth(userId = null)
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleAnalytics(mockContext, false)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { userRepository.updateUserField(any(), any(), any()) }
  }

  // ========================================
  // Tests for Delete Account
  // ========================================

  @Test
  fun deleteAccount_setsLoadingAndSucceeds() = runTest {
    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuthWithDeleteSuccess()
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertTrue(state.isAccountDeleted)
  }

  @Test
  fun deleteAccount_handlesFailure() = runTest {
    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuthWithDeleteFailure()
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertFalse(state.isAccountDeleted)
    assertTrue(state.error?.contains("Delete failed") == true)
  }

  @Test
  fun deleteAccount_whenNoUser_setsError() = runTest {
    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuth(userId = null)
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isLoading)
    assertEquals("No user logged in.", state.error)
  }

  // ========================================
  // Tests for Error Handling
  // ========================================

  @Test
  fun clearError_removesErrorMessage() = runTest {
    val userRepository = createUserRepository(user = null)
    val auth = createFirebaseAuth(userId = null)
    val viewModel = AccountSettingsViewModel(userRepository, auth)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.clearError()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.error)
  }
}
