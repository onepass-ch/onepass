package ch.onepass.onepass.ui.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
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
 * @param website The organization website field state.
 * @param instagram The organization Instagram field state.
 * @param facebook The organization Facebook field state.
 * @param tiktok The organization TikTok field state.
 * @param address The organization address field state.
 */
data class OrganizationFormState(
    val name: FieldState = FieldState(),
    val description: FieldState = FieldState(),
    val contactEmail: FieldState = FieldState(),
    val contactPhone: FieldState = FieldState(),
    val website: FieldState = FieldState(),
    val instagram: FieldState = FieldState(),
    val facebook: FieldState = FieldState(),
    val tiktok: FieldState = FieldState(),
    val address: FieldState = FieldState()
)

/**
 * Data class representing the UI state of the Organizer form.
 *
 * @param successOrganizationId The ID of the created organization on success.
 * @param errorMessage An optional error message if an error occurred.
 */
class OrganizationFormUiState(
    val successOrganizationId: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel for managing the Organizer form and its submission.
 *
 * @param repository The organization repository for data operations.
 * @param userRepository The user repository for updating user data.
 */
class OrganizationFormViewModel(
    private val repository: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val userRepository: UserRepository = UserRepositoryFirebase()
) : ViewModel() {

  /** Private form state */
  private val _formState = MutableStateFlow(OrganizationFormState())
  /** Public form state */
  val formState: StateFlow<OrganizationFormState> = _formState.asStateFlow()
  /** Public UI state */
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

  /** Regex pattern for validating website URLs */
  private val REGEX_WEBSITE_URL = """^https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}.*$""".toRegex()
  /** Regex pattern for validating phone numbers */
  private val REGEX_PHONE = """^\+\d{1,4}\d{4,14}$""".toRegex()
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
    if (value.isBlank()) return null
    return if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
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

    return if (countryCode == null || value.isBlank() || !REGEX_PHONE.matches(fullNumber)) {
      "Invalid phone number"
    } else null
  }

  /**
   * Validates the website field
   *
   * @param value The website URL to validate
   * @return An error message if invalid, null otherwise
   */
  private fun validateWebsite(value: String): String? {
    if (value.isBlank()) return null
    val withScheme =
        if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
    return if (REGEX_WEBSITE_URL.matches(withScheme)) null else "Invalid website"
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
    val field = _formState.value.name
    _formState.value =
        _formState.value.copy(
            name = updateField(field, value, ::validateName, field.touched, field.focused))
  }

  /**
   * Updates the description field
   *
   * @param value The new description value
   */
  fun updateDescription(value: String) {
    val field = _formState.value.description
    _formState.value =
        _formState.value.copy(
            description =
                updateField(field, value, ::validateDescription, field.touched, field.focused))
  }

  /**
   * Updates the contact email field
   *
   * @param value The new contact email value
   */
  fun updateContactEmail(value: String) {
    val field = _formState.value.contactEmail
    _formState.value =
        _formState.value.copy(
            contactEmail = updateField(field, value, ::validateEmail, field.touched, field.focused))
  }

  /**
   * Updates the contact phone field
   *
   * @param value The new contact phone value
   */
  fun updateContactPhone(value: String) {
    val field = _formState.value.contactPhone
    _formState.value =
        _formState.value.copy(
            contactPhone = updateField(field, value, ::validatePhone, field.touched, field.focused))
  }

  /**
   * Updates the website field
   *
   * @param value The new website value
   */
  fun updateWebsite(value: String) {
    val field = _formState.value.website
    _formState.value =
        _formState.value.copy(
            website = updateField(field, value, ::validateWebsite, field.touched, field.focused))
  }

  /**
   * Updates the instagram field
   *
   * @param value The new instagram value
   */
  fun updateInstagram(value: String) {
    _formState.value =
        _formState.value.copy(instagram = _formState.value.instagram.copy(value = value))
  }

  /**
   * Updates the facebook field
   *
   * @param value The new facebook value
   */
  fun updateFacebook(value: String) {
    _formState.value =
        _formState.value.copy(facebook = _formState.value.facebook.copy(value = value))
  }

  /**
   * Updates the tiktok field
   *
   * @param value The new tiktok value
   */
  fun updateTiktok(value: String) {
    _formState.value = _formState.value.copy(tiktok = _formState.value.tiktok.copy(value = value))
  }

  /**
   * Updates the address field
   *
   * @param value The new address value
   */
  fun updateAddress(value: String) {
    _formState.value = _formState.value.copy(address = _formState.value.address.copy(value = value))
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
   * Creates an organization using the form data
   *
   * @param ownerId The user ID of the organization owner
   */
  fun createOrganization(ownerId: String) {
    viewModelScope.launch {
      // Validate form before submission
      if (!validateForm()) {
        _uiState.value = OrganizationFormUiState(errorMessage = "Please fix errors")
        return@launch
      }
      val s = _formState.value
      _uiState.value = OrganizationFormUiState()
      try {
        // Construct organization object
        val org =
            Organization(
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
                address = s.address.value)
        val result = repository.createOrganization(org)

        // Update UI state based on result
        result.fold(
            onSuccess = { orgId ->
              // Add the current user as OWNER member to the organization
              val addMemberResult = repository.addMember(orgId, ownerId, OrganizationRole.OWNER)
              
              addMemberResult.fold(
                  onSuccess = {
                    // Member added successfully, now update user's organizationIds
                    try {
                      userRepository.addOrganizationToUser(ownerId, orgId)
                      _uiState.value = OrganizationFormUiState(successOrganizationId = orgId)
                    } catch (e: Exception) {
                      // Organization created and member added, but failed to update user's org list
                      _uiState.value = OrganizationFormUiState(
                          successOrganizationId = orgId,
                          errorMessage = "Organization created, but failed to update user profile: ${e.message}")
                    }
                  },
                  onFailure = { error ->
                    // Organization created but failed to add member
                    _uiState.value = OrganizationFormUiState(
                        successOrganizationId = orgId,
                        errorMessage = "Organization created, but failed to add member: ${error.message ?: "Unknown error"}")
                  })
            },
            onFailure = {
              _uiState.value = OrganizationFormUiState(errorMessage = it.message ?: "Unknown error")
            })
      } catch (e: Exception) {
        _uiState.value = OrganizationFormUiState(errorMessage = e.message ?: "Unknown error")
      }
    }
  }

  /** Resets the form to its initial empty state */
  fun resetForm() {
    _formState.value = OrganizationFormState() // Reset to initial empty state
  }

  /** Clears any success state */
  fun clearSuccess() {
    _uiState.value = OrganizationFormUiState()
  }

  /** Clears any error state */
  fun clearError() {
    _uiState.value = OrganizationFormUiState()
  }
}
