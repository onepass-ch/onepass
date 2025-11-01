package ch.onepass.onepass.ui.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FieldState(
    val value: String = "",
    val touched: Boolean = false,
    val focused: Boolean = false,
    val error: String? = null
)

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

sealed class BecomeOrganizerUiState {
  object Idle : BecomeOrganizerUiState()

  object Loading : BecomeOrganizerUiState()

  data class Success(val organizationId: String) : BecomeOrganizerUiState()

  data class Error(val message: String) : BecomeOrganizerUiState()
}

class BecomeOrganizerViewModel(
    private val createOrganizationCallback: (Organization) -> Result<String> = {
      Result.success("org123")
    }
) : ViewModel() {

  private val _formState = MutableStateFlow(BecomeOrganizerFormState())
  val formState: StateFlow<BecomeOrganizerFormState> = _formState.asStateFlow()

  private val _uiState = MutableStateFlow<BecomeOrganizerUiState>(BecomeOrganizerUiState.Idle)
  val uiState: StateFlow<BecomeOrganizerUiState> = _uiState.asStateFlow()

  private fun validateName(value: String) = if (value.isBlank()) "Name is required" else null

  private fun validateDescription(value: String) =
      if (value.isBlank()) "Description is required" else null

  private fun validateEmail(value: String) =
      if (value.isBlank()) "Email is required"
      else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) "Invalid email"
      else null

  private fun validatePhone(value: String) =
      if (value.isNotBlank() && !value.all { it.isDigit() || it == '+' || it == '-' })
          "Invalid phone"
      else null

  private fun validateWebsite(value: String) =
      if (value.isNotBlank() && !value.startsWith("http")) "Invalid website" else null

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

  fun updateName(value: String) {
    val field = _formState.value.name
    _formState.value =
        _formState.value.copy(
            name =
                updateField(
                    field, value, ::validateName, touched = field.touched, focused = field.focused))
  }

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

  fun updateInstagram(value: String) {
    _formState.value =
        _formState.value.copy(instagram = _formState.value.instagram.copy(value = value))
  }

  fun updateFacebook(value: String) {
    _formState.value =
        _formState.value.copy(facebook = _formState.value.facebook.copy(value = value))
  }

  fun updateTiktok(value: String) {
    _formState.value = _formState.value.copy(tiktok = _formState.value.tiktok.copy(value = value))
  }

  fun updateAddress(value: String) {
    _formState.value = _formState.value.copy(address = _formState.value.address.copy(value = value))
  }

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

  fun createOrganization(ownerId: String) {
    viewModelScope.launch {
      if (!validateForm()) {
        _uiState.value = BecomeOrganizerUiState.Error("Please fix errors")
        return@launch
      }
      val state = _formState.value
      val org =
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
      _uiState.value = BecomeOrganizerUiState.Loading
      val result = createOrganizationCallback(org)
      result.fold(
          onSuccess = { _uiState.value = BecomeOrganizerUiState.Success(it) },
          onFailure = { _uiState.value = BecomeOrganizerUiState.Error(it.message ?: "Failed") })
    }
  }

  fun clearError() {
    _uiState.value = BecomeOrganizerUiState.Idle
  }
}
