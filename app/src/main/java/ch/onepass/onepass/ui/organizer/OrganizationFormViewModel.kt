package ch.onepass.onepass.ui.organizer

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.membership.MembershipRepository
import ch.onepass.onepass.model.membership.MembershipRepositoryFirebase
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.model.storage.StorageRepository
import ch.onepass.onepass.model.storage.StorageRepositoryFirebase
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import ch.onepass.onepass.utils.InputSanitizer
import ch.onepass.onepass.utils.ValidationUtils
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing the state of a single form field.
 *
 * @param value The current value of the field.
 * @param touched Whether the field has been touched (focused at least once).
 * @param focused Whether the field is currently focused.
 * @param error An optional error message if the field is invalid.
 */
data class FieldState(
    val value: String = "",
    val touched: Boolean = false,
    val focused: Boolean = false,
    val error: String? = null
)

/**
 * Data class representing the state of the Organizer form.
 *
 * @param name The organization name field state.
 * @param description The organization description field state.
 * @param contactEmail The contact email field state.
 * @param contactPhone The contact phone field state.
 * @param phonePrefix The phone prefix field state.
 * @param website The organization website field state.
 * @param instagram The organization Instagram field state.
 * @param facebook The organization Facebook field state.
 * @param tiktok The organization TikTok field state.
 * @param address The organization address field state.
 * @param profileImageUri The URI of the selected profile image.
 * @param coverImageUri The URI of the selected cover image.
 */
data class OrganizationFormState(
    val name: FieldState = FieldState(),
    val description: FieldState = FieldState(),
    val contactEmail: FieldState = FieldState(),
    val contactPhone: FieldState = FieldState(),
    val contactPhonePrefix: MutableState<String> = mutableStateOf(""),
    val website: FieldState = FieldState(),
    val instagram: FieldState = FieldState(),
    val facebook: FieldState = FieldState(),
    val tiktok: FieldState = FieldState(),
    val address: FieldState = FieldState(),
    val profileImageUri: Uri? = null,
    val coverImageUri: Uri? = null
)

/**
 * Data class representing the UI state of the Organizer form.
 *
 * @param isLoading Whether an organization creation operation is in progress.
 * @param successOrganizationId The ID of the created organization on success.
 * @param errorMessage An optional error message if an error occurred.
 */
data class OrganizationFormUiState(
    val isLoading: Boolean = false,
    val successOrganizationId: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel for managing the Organizer form and its submission.
 *
 * @param repository The organization repository for data operations.
 * @param userRepository The user repository for updating user data.
 * @param storageRepository The storage repository for image upload operations.
 * @param membershipRepository The membership repository for user-organization relationships.
 */
class OrganizationFormViewModel(
    private val repository: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val storageRepository: StorageRepository = StorageRepositoryFirebase(),
    private val membershipRepository: MembershipRepository = MembershipRepositoryFirebase()
) : ViewModel() {

  companion object {
    const val MAX_NAME_LENGTH = 50
    const val MAX_DESCRIPTION_LENGTH = 200
    const val MAX_EMAIL_LENGTH = 50
    const val MAX_PHONE_LENGTH = 15
    const val MAX_WEBSITE_LENGTH = 50
    const val MAX_SOCIAL_LENGTH = 50
    const val MAX_ADDRESS_LENGTH = 150
  }

  /** Private form state */
  private val _formState = MutableStateFlow(OrganizationFormState())
  /** Public form state */
  val formState: StateFlow<OrganizationFormState> = _formState.asStateFlow()

  /** Private UI state */
  private val _uiState = MutableStateFlow(OrganizationFormUiState())
  /** Public UI state */
  val uiState: StateFlow<OrganizationFormUiState> = _uiState.asStateFlow()

  /** Phone number utility instance */
  private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
  /** Selected country code state */
  private val _selectedCountryCode = MutableStateFlow("+41")
  /** Public selected country code */
  val selectedCountryCode: StateFlow<String> = _selectedCountryCode.asStateFlow()
  /** List of countries with their names and dialing codes */
  private val _countryList = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
  /** Public country list */
  val countryList: StateFlow<List<Pair<String, Int>>> = _countryList.asStateFlow()
  /** Selected country index state */
  private val _selectedCountryIndex = MutableStateFlow<Int?>(null)

  /** Initial region code for default country selection */
  private val INITIAL_REGION_CODE = 41

  init {
    // Initialize country list using PhoneNumberUtil
    val regions = phoneUtil.supportedRegions
    val countries =
        regions
            .map { region ->
              val code = phoneUtil.getCountryCodeForRegion(region)
              val name = Locale("", region).displayCountry
              name to code
            }
            .sortedBy { it.first }
    _countryList.value = countries

    // Set initial country based on default region code
    val initialIndex = countries.indexOfFirst { it.second == INITIAL_REGION_CODE }
    if (initialIndex != -1) {
      _selectedCountryIndex.value = initialIndex
      _selectedCountryCode.value = "+${countries[initialIndex].second}"
    }
  }

  /**
   * Validates the name field
   *
   * @param value The name to validate
   * @return An error message if invalid, null otherwise
   */
  private fun validateName(value: String) = if (value.isBlank()) "Name is required" else null

  /**
   * Validates the description field
   *
   * @param value The description to validate
   * @return An error message if invalid, null otherwise
   */
  private fun validateDescription(value: String) =
      if (value.isBlank()) "Description is required" else null

  /**
   * Validates the email field
   *
   * @param value The email to validate
   * @return An error message if invalid, null otherwise
   */
  private fun validateEmail(value: String): String? {
    if (value.isBlank()) return "Email is required"
    return if (!ValidationUtils.isValidEmail(value)) {
      "Invalid email"
    } else null
  }

  /**
   * Validates the phone field
   *
   * @param value The phone number to validate
   * @return An error message if invalid, null otherwise
   */
  private fun validatePhone(value: String): String? {
    val countryCode = _selectedCountryIndex.value?.let { _countryList.value.getOrNull(it)?.second }
    val fullNumber = if (countryCode != null) "+$countryCode$value" else value

    return when {
      countryCode == null -> "Phone country code is required"
      value.isBlank() -> "Phone number is required"
      !ValidationUtils.isValidPhone(fullNumber) -> "Invalid phone number"
      else -> null
    }
  }

  /**
   * Validates the website field
   *
   * @param value The website URL to validate
   * @return An error message if invalid, null otherwise
   */
  private fun validateWebsite(value: String): String? {
    if (value.isBlank()) return null
    return if (ValidationUtils.isValidUrl(value)) null else "Invalid website"
  }

  /**
   * Updates the selected country index and code
   *
   * @param index The new selected country index
   */
  fun updateCountryIndex(index: Int) {
    _selectedCountryIndex.value = index
    _selectedCountryCode.value = "+${_countryList.value.getOrNull(index)?.second ?: 41}"

    // Recompute phone error dynamically
    val phoneField = _formState.value.contactPhone
    _formState.value =
        _formState.value.copy(
            contactPhone =
                updateField(
                    phoneField,
                    phoneField.value,
                    ::validatePhone,
                    touched = phoneField.touched,
                    focused = phoneField.focused))
  }

  /**
   * Updates a form field state
   *
   * @param field The current field state
   * @param value The new value for the field
   * @param validate The validation function for the field
   * @param touched Whether the field has been touched
   * @param focused Whether the field is currently focused
   * @return The updated field state
   */
  private fun updateField(
      field: FieldState,
      value: String,
      validate: (String) -> String?,
      touched: Boolean,
      focused: Boolean
  ): FieldState {
    val newTouched = touched || focused
    val error = if (newTouched && !focused) validate(value) else null
    return field.copy(value = value, touched = newTouched, focused = focused, error = error)
  }

  /**
   * Updates the name field
   *
   * @param value The new name value
   */
  fun updateName(value: String) {
    try {
      val sanitized = InputSanitizer.sanitizeTitle(value).take(MAX_NAME_LENGTH)
      val field = _formState.value.name
      _formState.value =
          _formState.value.copy(
              name = updateField(field, sanitized, ::validateName, field.touched, field.focused))
    } catch (_: IllegalArgumentException) {
      val field = _formState.value.name
      _formState.value =
          _formState.value.copy(
              name = field.copy(error = "Name contains invalid characters or dangerous patterns"))
    }
  }

  /**
   * Updates the description field
   *
   * @param value The new description value
   */
  fun updateDescription(value: String) {
    try {
      val sanitized = InputSanitizer.sanitizeDescription(value).take(MAX_DESCRIPTION_LENGTH)
      val field = _formState.value.description
      _formState.value =
          _formState.value.copy(
              description =
                  updateField(
                      field, sanitized, ::validateDescription, field.touched, field.focused))
    } catch (_: IllegalArgumentException) {
      val field = _formState.value.description
      _formState.value =
          _formState.value.copy(
              description =
                  field.copy(
                      error = "Description contains invalid characters or dangerous patterns"))
    }
  }

  /**
   * Updates the contact email field
   *
   * @param value The new contact email value
   */
  fun updateContactEmail(value: String) {
    val sanitized = value.take(MAX_EMAIL_LENGTH).trim()
    val field = _formState.value.contactEmail
    _formState.value =
        _formState.value.copy(
            contactEmail =
                updateField(field, sanitized, ::validateEmail, field.touched, field.focused))
  }

  /**
   * Updates the contact phone field
   *
   * @param value The new contact phone value
   */
  fun updateContactPhone(value: String) {
    val sanitized = InputSanitizer.sanitizeCapacity(value).take(MAX_PHONE_LENGTH)
    val field = _formState.value.contactPhone
    _formState.value =
        _formState.value.copy(
            contactPhone =
                updateField(field, sanitized, ::validatePhone, field.touched, field.focused))
  }

  /**
   * Updates the website field
   *
   * @param value The new website value
   */
  fun updateWebsite(value: String) {
    val sanitized = value.take(MAX_WEBSITE_LENGTH).trim()
    val field = _formState.value.website
    _formState.value =
        _formState.value.copy(
            website =
                updateField(field, sanitized, ::validateWebsite, field.touched, field.focused))
  }

  /**
   * Updates the instagram field
   *
   * @param value The new instagram value
   */
  fun updateInstagram(value: String) {
    val sanitized = value.take(MAX_SOCIAL_LENGTH).trim()
    _formState.value =
        _formState.value.copy(instagram = _formState.value.instagram.copy(value = sanitized))
  }

  /**
   * Updates the facebook field
   *
   * @param value The new facebook value
   */
  fun updateFacebook(value: String) {
    val sanitized = value.take(MAX_SOCIAL_LENGTH).trim()
    _formState.value =
        _formState.value.copy(facebook = _formState.value.facebook.copy(value = sanitized))
  }

  /**
   * Updates the tiktok field
   *
   * @param value The new tiktok value
   */
  fun updateTiktok(value: String) {
    val sanitized = value.take(MAX_SOCIAL_LENGTH).trim()
    _formState.value =
        _formState.value.copy(tiktok = _formState.value.tiktok.copy(value = sanitized))
  }
  /**
   * Updates the address field
   *
   * @param value The new address value
   */
  fun updateAddress(value: String) {
    val sanitized = value.take(MAX_ADDRESS_LENGTH).trim()
    _formState.value =
        _formState.value.copy(address = _formState.value.address.copy(value = sanitized))
  }

  /**
   * Updates the selected profile image URI
   *
   * @param uri The URI of the selected profile image
   */
  fun selectProfileImage(uri: Uri) {
    _formState.value = _formState.value.copy(profileImageUri = uri)
  }

  /**
   * Updates the selected cover image URI
   *
   * @param uri The URI of the selected cover image
   */
  fun selectCoverImage(uri: Uri) {
    _formState.value = _formState.value.copy(coverImageUri = uri)
  }

  /**
   * Uploads the profile image to storage
   *
   * @param organizationId The organization ID to use for the storage path
   * @return Result containing the uploaded image URL or error
   */
  private suspend fun uploadProfileImage(organizationId: String): Result<String?> {
    val imageUri = _formState.value.profileImageUri ?: return Result.success(null)

    // Determine the correct file extension based on the image type
    val extension = storageRepository.getImageExtension(imageUri)
    val storagePath = "organizations/$organizationId/profile_image.$extension"
    return storageRepository
        .uploadImage(imageUri, storagePath)
        .map { url -> url }
        .fold(
            onSuccess = { Result.success(it) },
            onFailure = {
              Result.failure(Exception("Failed to upload profile image: ${it.message}"))
            })
  }

  /**
   * Uploads the cover image to storage
   *
   * @param organizationId The organization ID to use for the storage path
   * @return Result containing the uploaded image URL or error
   */
  private suspend fun uploadCoverImage(organizationId: String): Result<String?> {
    val imageUri = _formState.value.coverImageUri ?: return Result.success(null)

    // Determine the correct file extension based on the image type
    val extension = storageRepository.getImageExtension(imageUri)
    val storagePath = "organizations/$organizationId/cover_image.$extension"
    return storageRepository
        .uploadImage(imageUri, storagePath)
        .map { url -> url }
        .fold(
            onSuccess = { Result.success(it) },
            onFailure = {
              Result.failure(Exception("Failed to upload cover image: ${it.message}"))
            })
  }

  /**
   * Handles focus change for the name field
   *
   * @param focused Whether the field is focused
   */
  fun onFocusChangeName(focused: Boolean) {
    val field = _formState.value.name
    _formState.value =
        _formState.value.copy(
            name = updateField(field, field.value, ::validateName, field.touched, focused))
  }

  /**
   * Handles focus change for the description field
   *
   * @param focused Whether the field is focused
   */
  fun onFocusChangeDescription(focused: Boolean) {
    val field = _formState.value.description
    _formState.value =
        _formState.value.copy(
            description =
                updateField(field, field.value, ::validateDescription, field.touched, focused))
  }

  /**
   * Handles focus change for the email field
   *
   * @param focused Whether the field is focused
   */
  fun onFocusChangeEmail(focused: Boolean) {
    val field = _formState.value.contactEmail
    _formState.value =
        _formState.value.copy(
            contactEmail = updateField(field, field.value, ::validateEmail, field.touched, focused))
  }

  /**
   * Handles focus change for the phone field
   *
   * @param focused Whether the field is focused
   */
  fun onFocusChangePhone(focused: Boolean) {
    val field = _formState.value.contactPhone
    _formState.value =
        _formState.value.copy(
            contactPhone = updateField(field, field.value, ::validatePhone, field.touched, focused))
  }

  /**
   * Handles focus change for the website field
   *
   * @param focused Whether the field is focused
   */
  fun onFocusChangeWebsite(focused: Boolean) {
    val field = _formState.value.website
    _formState.value =
        _formState.value.copy(
            website = updateField(field, field.value, ::validateWebsite, field.touched, focused))
  }

  /**
   * Initializes the form fields from an existing organization
   *
   * @param org The organization to initialize from
   */
  fun initializeFrom(org: Organization) {
    updateName(org.name)
    updateDescription(org.description)
    updateContactEmail(org.contactEmail ?: "")
    updateContactPhone(org.contactPhone ?: "")
    updateWebsite(org.website ?: "")
    updateInstagram(org.instagram ?: "")
    updateFacebook(org.facebook ?: "")
    updateTiktok(org.tiktok ?: "")
    updateAddress(org.address ?: "")
  }

  /**
   * Validates the entire form
   *
   * @return True if the form is valid, false otherwise
   */
  fun validateForm(): Boolean {
    val s = _formState.value

    // Validate all fields
    val nameField = updateField(s.name, s.name.value, ::validateName, true, false)
    val descriptionField =
        updateField(s.description, s.description.value, ::validateDescription, true, false)
    val emailField = updateField(s.contactEmail, s.contactEmail.value, ::validateEmail, true, false)
    val phoneField = updateField(s.contactPhone, s.contactPhone.value, ::validatePhone, true, false)
    val websiteField = updateField(s.website, s.website.value, ::validateWebsite, true, false)

    // Update form state with validated fields
    _formState.value =
        s.copy(
            name = nameField,
            description = descriptionField,
            contactEmail = emailField,
            contactPhone = phoneField,
            website = websiteField)

    // Check if all fields are valid
    return listOf(
            nameField.error,
            descriptionField.error,
            emailField.error,
            phoneField.error,
            websiteField.error)
        .all { it == null }
  }

  /**
   * Updates the form state
   *
   * @param newState the new organisation form state
   */
  fun updateFormState(newState: OrganizationFormState) {
    _formState.value = newState
  }

  /**
   * Creates an organization using the form data
   *
   * @param ownerId The user ID of the organization owner
   */
  fun createOrganization(ownerId: String) {
    // Prevent multiple submissions
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      // Validate form before submission
      if (!validateForm()) {
        _uiState.value = _uiState.value.copy(errorMessage = "Please fix validation errors")
        return@launch
      }

      // Set loading state
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

      try {
        val createdOrgId = createOrganizationEntity(ownerId) ?: return@launch
        handleImageUploads(createdOrgId) ?: return@launch
        addOwnerMembership(createdOrgId, ownerId)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
      }
    }
  }

  /**
   * Creates the organization entity in the repository
   *
   * @param ownerId The user ID of the organization owner
   * @return The created organization ID, or null if creation failed
   */
  private suspend fun createOrganizationEntity(ownerId: String): String? {
    val s = _formState.value
    val org =
        Organization(
            id = "", // Will be set by repository
            name = s.name.value,
            description = s.description.value,
            ownerId = ownerId,
            status = OrganizationStatus.PENDING,
            contactEmail = s.contactEmail.value,
            contactPhone = s.contactPhone.value,
            website = s.website.value,
            instagram = s.instagram.value,
            facebook = s.facebook.value,
            tiktok = s.tiktok.value,
            address = s.address.value,
            profileImageUrl = null, // No images yet
            coverImageUrl = null) // No images yet

    val createResult = repository.createOrganization(org)
    return createResult.getOrElse {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false, errorMessage = it.message ?: "Organization creation failed")
      null
    }
  }

  /**
   * Handles uploading and updating both profile and cover images
   *
   * @param organizationId The ID of the created organization
   * @return The organization ID if successful, or null if any upload/update failed
   */
  private suspend fun handleImageUploads(organizationId: String): String? {
    val s = _formState.value

    // Handle profile image
    if (s.profileImageUri != null && !uploadAndUpdateProfileImage(organizationId)) {
      return null
    }

    // Handle cover image
    if (s.coverImageUri != null && !uploadAndUpdateCoverImage(organizationId)) {
      return null
    }

    return organizationId
  }

  /**
   * Uploads and updates the profile image for an organization
   *
   * @param organizationId The ID of the organization
   * @return True if successful, false otherwise
   */
  private suspend fun uploadAndUpdateProfileImage(organizationId: String): Boolean {
    val profileImageUrl =
        uploadProfileImage(organizationId).getOrElse {
          _uiState.value =
              _uiState.value.copy(
                  isLoading = false, errorMessage = it.message ?: "Failed to upload profile image")
          return false
        }

    if (profileImageUrl != null) {
      repository.updateProfileImage(organizationId, profileImageUrl).getOrElse {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = it.message ?: "Failed to update profile image")
        return false
      }
    }

    return true
  }

  /**
   * Uploads and updates the cover image for an organization
   *
   * @param organizationId The ID of the organization
   * @return True if successful, false otherwise
   */
  private suspend fun uploadAndUpdateCoverImage(organizationId: String): Boolean {
    val coverImageUrl =
        uploadCoverImage(organizationId).getOrElse {
          _uiState.value =
              _uiState.value.copy(
                  isLoading = false, errorMessage = it.message ?: "Failed to upload cover image")
          return false
        }

    if (coverImageUrl != null) {
      repository.updateCoverImage(organizationId, coverImageUrl).getOrElse {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = it.message ?: "Failed to update cover image")
        return false
      }
    }

    return true
  }

  /**
   * Adds the owner as a member to the organization and updates the user's organization list
   *
   * @param organizationId The ID of the organization
   * @param ownerId The ID of the owner user
   */
  private suspend fun addOwnerMembership(organizationId: String, ownerId: String) {
    // Create a dedicated membership entry to keep the memberships collection in sync.
    val membershipResult =
        membershipRepository.addMembership(ownerId, organizationId, OrganizationRole.OWNER)

    membershipResult.fold(
        onSuccess = {
          // Membership created successfully
          _uiState.value =
              _uiState.value.copy(isLoading = true, successOrganizationId = organizationId)
        },
        onFailure = { error ->
          _uiState.value =
              _uiState.value.copy(
                  isLoading = false,
                  successOrganizationId = organizationId,
                  errorMessage =
                      "Organization created, but failed to add member: ${error.message ?: "Unknown error"}")
        })
  }

  /** Resets the form to its initial empty state */
  fun resetForm() {
    _formState.value = OrganizationFormState()
    _uiState.value = OrganizationFormUiState()
  }

  /** Clears success state */
  fun clearSuccess() {
    _uiState.value = _uiState.value.copy(successOrganizationId = null)
  }

  /** Clears error state */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
