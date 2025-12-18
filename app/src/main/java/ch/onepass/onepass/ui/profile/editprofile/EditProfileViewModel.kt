package ch.onepass.onepass.ui.profile.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.storage.StorageRepository
import ch.onepass.onepass.model.storage.StorageRepositoryFirebase
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class EditProfileFormState(
    val displayName: String = "",
    val phone: String = "",
    val country: String = "",
    val avatarUrl: String? = null,
    val avatarUri: Uri? = null,
    val initials: String = ""
)

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null
)

class EditProfileViewModel(
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val storageRepository: StorageRepository = StorageRepositoryFirebase(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

  private val _formState = MutableStateFlow(EditProfileFormState())
  val formState: StateFlow<EditProfileFormState> = _formState.asStateFlow()

  private val _uiState = MutableStateFlow(EditProfileUiState())
  val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

  private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
  private val _selectedCountryCode = MutableStateFlow("+41")
  val selectedCountryCode: StateFlow<String> = _selectedCountryCode.asStateFlow()

  private val _countryList = MutableStateFlow<List<Pair<String, String>>>(emptyList())
  val countryList: StateFlow<List<Pair<String, String>>> = _countryList.asStateFlow()

  private val _selectedCountryIndex = MutableStateFlow<Int?>(null)

  private val INITIAL_REGION_CODE = "41"

  init {
    // Initialize country list
    val regions = phoneUtil.supportedRegions
    val countries =
        regions
            .map { region ->
              val code = phoneUtil.getCountryCodeForRegion(region)
              val name = Locale("", region).displayCountry
              name to code.toString()
            }
            .sortedBy { it.first }
    _countryList.value = countries

    val initialIndex = countries.indexOfFirst { it.second == INITIAL_REGION_CODE }
    if (initialIndex != -1) {
      _selectedCountryIndex.value = initialIndex
      _selectedCountryCode.value = "+${countries[initialIndex].second}"
    }
  }

  fun loadProfile() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val user = userRepository.getCurrentUser()
        if (user != null) {
          // Extract phone without prefix
          val phoneWithoutPrefix = user.phoneE164?.removePrefix(_selectedCountryCode.value) ?: ""

          val initials =
              user.displayName
                  .split(" ")
                  .filter { it.isNotBlank() }
                  .take(2)
                  .joinToString("") { it.first().uppercase() }

          _formState.value =
              EditProfileFormState(
                  displayName = user.displayName,
                  phone = phoneWithoutPrefix,
                  country = user.country ?: "",
                  avatarUrl = user.avatarUrl,
                  initials = initials.ifEmpty { "?" })
        }
        _uiState.value = _uiState.value.copy(isLoading = false)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = e.message ?: "Failed to load profile")
      }
    }
  }

  fun saveProfile() {
    if (_formState.value.displayName.isBlank()) {
      _uiState.value = _uiState.value.copy(errorMessage = "Name is required")
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val currentUser = userRepository.getCurrentUser() ?: throw Exception("User not found")

        // Upload avatar if changed
        val finalAvatarUrl =
            if (_formState.value.avatarUri != null) {
              uploadAvatar(currentUser.uid) ?: _formState.value.avatarUrl
            } else {
              _formState.value.avatarUrl
            }

        // Build phone with prefix
        val fullPhone =
            if (_formState.value.phone.isNotBlank()) {
              "${_selectedCountryCode.value}${_formState.value.phone}"
            } else null

        // Update Firestore directly
        val updates =
            mutableMapOf<String, Any?>(
                "displayName" to _formState.value.displayName,
                "phoneE164" to fullPhone,
                "country" to _formState.value.country.ifBlank { null },
                "avatarUrl" to finalAvatarUrl)

        firestore.collection("users").document(currentUser.uid).update(updates).await()

        _uiState.value = _uiState.value.copy(isLoading = false, success = true)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = e.message ?: "Failed to save profile")
      }
    }
  }

  private suspend fun uploadAvatar(userId: String): String? {
    val imageUri = _formState.value.avatarUri ?: return null
    val extension = storageRepository.getImageExtension(imageUri)
    val storagePath = "users/$userId/avatar.$extension"

    return storageRepository.uploadImage(imageUri, storagePath).getOrNull()
  }

  fun selectAvatarImage(uri: Uri) {
    _formState.value = _formState.value.copy(avatarUri = uri)
  }

  fun removeAvatar() {
    _formState.value = _formState.value.copy(avatarUrl = null, avatarUri = null)
  }

  fun updateCountryIndex(index: Int) {
    _selectedCountryIndex.value = index
    _selectedCountryCode.value = "+${_countryList.value.getOrNull(index)?.second ?: "41"}"
  }

  fun updateDisplayName(value: String) {
    _formState.value = _formState.value.copy(displayName = value)
  }

  fun updatePhone(value: String) {
    _formState.value = _formState.value.copy(phone = value)
  }

  fun updateCountry(value: String) {
    _formState.value = _formState.value.copy(country = value)
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
