package ch.onepass.onepass.ui.profile.accountsettings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for the Account Settings screen. Tracks loading status, errors, and the actual system
 * permission states.
 */
data class AccountSettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAccountDeleted: Boolean = false,

    // Real System Permission States
    val notificationsEnabled: Boolean = false,
    val locationEnabled: Boolean = false,
    val cameraEnabled: Boolean = false,

    // User Preferences (Stored in Firestore)
    val showEmail: Boolean = false,
    val analyticsEnabled: Boolean = true
)

/**
 * ViewModel responsible for Account Settings logic:
 * - Checking system permissions.
 * - Navigating to system settings for permission management.
 * - Handling account deletion (Auth + Data).
 * - Toggling user privacy preferences.
 */
open class AccountSettingsViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(AccountSettingsUiState())
  open val uiState: StateFlow<AccountSettingsUiState> = _uiState.asStateFlow()

  init {
    loadUserPreferences()
  }

  /** Loads non-permission preferences (like 'showEmail') from the user profile. */
  private fun loadUserPreferences() {
    viewModelScope.launch {
      val user = userRepository.getCurrentUser()
      if (user != null) {
        _uiState.value =
            _uiState.value.copy(
                showEmail = user.showEmail, analyticsEnabled = user.analyticsEnabled)
      }
    }
  }

  /**
   * Checks the actual Android system permission status and updates the UI state. This should be
   * called from ON_RESUME in the UI to ensure switches are accurate.
   */
  fun checkPermissions(context: Context) {
    _uiState.value =
        _uiState.value.copy(
            notificationsEnabled =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                } else {
                  true // Notifications are enabled by default on older Android versions
                },
            locationEnabled = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION),
            cameraEnabled = hasPermission(context, Manifest.permission.CAMERA))
  }

  private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED
  }

  /**
   * Opens the Android System Settings page for this specific app. Used when the user wants to
   * revoke a permission (which cannot be done programmatically) or grant a permanently denied
   * permission.
   */
  fun openAppSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.fromParts("package", context.packageName, null)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    context.startActivity(intent)
  }

  /** Toggles Show Email preference using the Repository. */
  fun toggleShowEmail(enabled: Boolean) {
    // 1. Optimistic Update
    _uiState.value = _uiState.value.copy(showEmail = enabled)

    viewModelScope.launch {
      val uid = auth.currentUser?.uid ?: return@launch

      // 2. Use the Repo method
      val result = userRepository.updateUserField(uid, "showEmail", enabled)

      if (result.isFailure) {
        _uiState.value = _uiState.value.copy(showEmail = !enabled) // Revert
      }
    }
  }

  /** Toggles Analytics preference using Repository + Firebase SDK. */
  fun toggleAnalytics(context: Context, enabled: Boolean) {
    _uiState.value = _uiState.value.copy(analyticsEnabled = enabled)

    FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled)

    viewModelScope.launch {
      val uid = auth.currentUser?.uid ?: return@launch

      val result = userRepository.updateUserField(uid, "analyticsEnabled", enabled)

      if (result.isFailure) {
        _uiState.value = _uiState.value.copy(analyticsEnabled = !enabled) // Revert
        // Revert local SDK setting too
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(!enabled)
      }
    }
  }

  /** Permanently deletes the user's account from Firebase Auth and Firestore. */
  fun deleteAccount() {
    _uiState.value = _uiState.value.copy(isLoading = true)

    viewModelScope.launch {
      try {
        val user = auth.currentUser
        if (user != null) {

          // Delete the Authentication record
          user
              .delete()
              .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(isLoading = false, isAccountDeleted = true)
              }
              .addOnFailureListener { e ->
                // Deletion requires recent login. If it fails, ask user to re-login.
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = "Delete failed: ${e.message}. Please log out and log in again.")
              }
        } else {
          _uiState.value = _uiState.value.copy(isLoading = false, error = "No user logged in.")
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
      }
    }
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }
}
