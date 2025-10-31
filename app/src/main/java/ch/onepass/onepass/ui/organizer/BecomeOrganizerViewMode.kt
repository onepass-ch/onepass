package ch.onepass.onepass.ui.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.organization.OrganizationStatus
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
 * Data class representing the state of the Become Organizer form.
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
data class BecomeOrganizerFormState(
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

/** Sealed class representing the UI state of the Become Organizer screen. */
sealed class BecomeOrganizerUiState {
  /** Idle state, no action in progress */
  object Idle : BecomeOrganizerUiState()

  /** Loading state, action in progress */
  object Loading : BecomeOrganizerUiState()

  /** Success state with the created organization ID */
  data class Success(val organizationId: String) : BecomeOrganizerUiState()

  /** Error state with an error message */
  data class Error(val message: String) : BecomeOrganizerUiState()
}

/**
 * ViewModel for the Become Organizer screen
 *
 * @property repository The organization repository
 */
class BecomeOrganizerViewModel(
    private val repository: OrganizationRepository = OrganizationRepositoryFirebase()
) : ViewModel() {
  /** Internal form state */
  private val _formState = MutableStateFlow(BecomeOrganizerFormState())
  /** Public form state */
  val formState: StateFlow<BecomeOrganizerFormState> = _formState.asStateFlow()

  /** Internal UI state */
  private val _uiState = MutableStateFlow<BecomeOrganizerUiState>(BecomeOrganizerUiState.Idle)
  /** Public UI state */
  val uiState: StateFlow<BecomeOrganizerUiState> = _uiState.asStateFlow()

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
  private fun validateEmail(value: String) =
      if (value.isBlank()) "Email is required"
      else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) "Invalid email"
      else null

  /**
   * Validates the phone field
   *
   * @param value The phone number to validate
   * @return An error message if invalid, null otherwise
   */
  private fun validatePhone(value: String) =
      if (value.isNotBlank() && !value.all { it.isDigit() || it == '+' || it == '-' })
          "Invalid phone"
      else null

  /**
   * Validates the website field
   *
   * @param value The website URL to validate
   * @return An error message if invalid, null otherwise
   */
  private fun validateWebsite(value: String) =
      if (value.isNotBlank() && !value.startsWith("http")) "Invalid website" else null

  /**
   * Updates a field state with new value, touched, focused and error states
   *
   * @param field The current field state
   * @param value The new value
   * @param validate The validation function
   * @param touched Whether the field has been touched
   * @param focused Whether the field is focused
   * @return The updated field state
   */
  private fun updateField(
      field: FieldState,
      value: String,
      validate: (String) -> String?,
      touched: Boolean,
      focused: Boolean
  ): FieldState {
    val error = if (touched && !focused) validate(value) else null
    return field.copy(value = value, touched = touched, focused = focused, error = error)
  }

  /**
   * Updates the name field focus state
   *
   * @param focused True if the name field is focused, false otherwise
   */
  fun onFocusChangeName(focused: Boolean) {
    val field = _formState.value.name
    _formState.value =
        _formState.value.copy(
            name =
                updateField(
                    field,
                    field.value,
                    ::validateName,
                    touched = field.touched || focused,
                    focused = focused))
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
            name =
                updateField(
                    field, value, ::validateName, touched = field.touched, focused = field.focused))
  }

  /**
   * Updates the description field focus state
   *
   * @param focused True if the description field is focused, false otherwise
   */
  fun onFocusChangeDescription(focused: Boolean) {
    val field = _formState.value.description
    _formState.value =
        _formState.value.copy(
            description =
                updateField(
                    field,
                    field.value,
                    ::validateDescription,
                    touched = field.touched || focused,
                    focused = focused))
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
                updateField(
                    field,
                    value,
                    ::validateDescription,
                    touched = field.touched,
                    focused = field.focused))
  }

  /**
   * Updates the contact email field focus state
   *
   * @param focused True if the contact email field is focused, false otherwise
   */
  fun onFocusChangeEmail(focused: Boolean) {
    val field = _formState.value.contactEmail
    _formState.value =
        _formState.value.copy(
            contactEmail =
                updateField(
                    field,
                    field.value,
                    ::validateEmail,
                    touched = field.touched || focused,
                    focused = focused))
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
            contactEmail =
                updateField(
                    field,
                    value,
                    ::validateEmail,
                    touched = field.touched,
                    focused = field.focused))
  }

  /**
   * Updates the contact phone field focus state
   *
   * @param focused True if the contact phone field is focused, false otherwise
   */
  fun onFocusChangePhone(focused: Boolean) {
    val field = _formState.value.contactPhone
    _formState.value =
        _formState.value.copy(
            contactPhone =
                updateField(
                    field,
                    field.value,
                    ::validatePhone,
                    touched = field.touched || focused,
                    focused = focused))
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
            contactPhone =
                updateField(
                    field,
                    value,
                    ::validatePhone,
                    touched = field.touched,
                    focused = field.focused))
  }

  /**
   * Updates the website field focus state
   *
   * @param focused True if the website field is focused, false otherwise
   */
  fun onFocusChangeWebsite(focused: Boolean) {
    val field = _formState.value.website
    _formState.value =
        _formState.value.copy(
            website =
                updateField(
                    field,
                    field.value,
                    ::validateWebsite,
                    touched = field.touched || focused,
                    focused = focused))
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
            website =
                updateField(
                    field,
                    value,
                    ::validateWebsite,
                    touched = field.touched,
                    focused = field.focused))
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
   * Validates the entire form and updates field errors
   *
   * @return True if the form is valid, false otherwise
   */
  private fun validateForm(): Boolean {
    _formState.value =
        _formState.value.copy(
            name =
                updateField(
                    _formState.value.name,
                    _formState.value.name.value,
                    ::validateName,
                    touched = true,
                    focused = false),
            description =
                updateField(
                    _formState.value.description,
                    _formState.value.description.value,
                    ::validateDescription,
                    touched = true,
                    focused = false),
            contactEmail =
                updateField(
                    _formState.value.contactEmail,
                    _formState.value.contactEmail.value,
                    ::validateEmail,
                    touched = true,
                    focused = false),
            contactPhone =
                updateField(
                    _formState.value.contactPhone,
                    _formState.value.contactPhone.value,
                    ::validatePhone,
                    touched = true,
                    focused = false),
            website =
                updateField(
                    _formState.value.website,
                    _formState.value.website.value,
                    ::validateWebsite,
                    touched = true,
                    focused = false))

    val state = _formState.value
    return listOf(
            state.name.error,
            state.description.error,
            state.contactEmail.error,
            state.contactPhone.error,
            state.website.error)
        .all { it == null }
  }

  /**
   * Creates an organization using the form data
   *
   * @param ownerId The user ID of the organization owner
   */
  fun createOrganization(ownerId: String) {
    viewModelScope.launch {
      if (!validateForm()) {
        _uiState.value = BecomeOrganizerUiState.Error("Please fix errors")
        return@launch
      }
      val state = _formState.value
      _uiState.value = BecomeOrganizerUiState.Loading
      val organization =
          Organization(
              name = state.name.value,
              description = state.description.value,
              ownerId = ownerId,
              status = OrganizationStatus.PENDING,
              contactEmail = state.contactEmail.value,
              contactPhone = state.contactPhone.value,
              website = state.website.value,
              instagram = state.instagram.value,
              facebook = state.facebook.value,
              tiktok = state.tiktok.value,
              address = state.address.value)

      // Create the organization using the repository
      val result = repository.createOrganization(organization)
      _uiState.value =
          result.fold(
              onSuccess = { BecomeOrganizerUiState.Success(it) },
              onFailure = { BecomeOrganizerUiState.Error(it.message ?: "Unknown error") })
    }
  }

  /** Clears any error state */
  fun clearError() {
    _uiState.value = BecomeOrganizerUiState.Idle
  }
}
