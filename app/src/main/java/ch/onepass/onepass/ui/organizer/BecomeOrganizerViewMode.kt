package ch.onepass.onepass.ui.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BecomeOrganizerViewModel(
    private val createOrganizationCallback: (Organization) -> Result<String> = {
      Result.success("org123")
    }
) : ViewModel() {

  private val _formState = MutableStateFlow(BecomeOrganizerFormState())
  val formState: StateFlow<BecomeOrganizerFormState> = _formState.asStateFlow()

  private val _uiState = MutableStateFlow<BecomeOrganizerUiState>(BecomeOrganizerUiState.Idle)
  val uiState: StateFlow<BecomeOrganizerUiState> = _uiState.asStateFlow()

  fun updateName(name: String) {
    _formState.value = _formState.value.copy(name = name)
  }

  fun updateDescription(description: String) {
    _formState.value = _formState.value.copy(description = description)
  }

  fun updateContactEmail(email: String) {
    _formState.value = _formState.value.copy(contactEmail = email)
  }

  fun updateContactPhone(phone: String) {
    _formState.value = _formState.value.copy(contactPhone = phone)
  }

  fun updateWebsite(website: String) {
    _formState.value = _formState.value.copy(website = website)
  }

  fun updateInstagram(instagram: String) {
    _formState.value = _formState.value.copy(instagram = instagram)
  }

  fun updateFacebook(facebook: String) {
    _formState.value = _formState.value.copy(facebook = facebook)
  }

  fun updateTiktok(tiktok: String) {
    _formState.value = _formState.value.copy(tiktok = tiktok)
  }

  fun updateAddress(address: String) {
    _formState.value = _formState.value.copy(address = address)
  }

  private fun validateForm(): List<String> {
    val errors = mutableListOf<String>()
    val state = _formState.value
    if (state.name.isBlank()) errors.add("Name is required")
    if (state.description.isBlank()) errors.add("Description is required")
    if (state.contactEmail.isBlank()) errors.add("Contact email is required")
    return errors
  }

  fun createOrganization(ownerId: String) {
    viewModelScope.launch {
      val errors = validateForm()
      if (errors.isNotEmpty()) {
        _uiState.value = BecomeOrganizerUiState.Error(errors.joinToString("\n"))
        return@launch
      }
      val state = _formState.value
      val org =
          Organization(
              name = state.name,
              description = state.description,
              ownerId = ownerId,
              status = OrganizationStatus.PENDING,
              contactEmail = state.contactEmail,
              contactPhone = state.contactPhone,
              website = state.website,
              instagram = state.instagram,
              facebook = state.facebook,
              tiktok = state.tiktok,
              address = state.address)
      _uiState.value = BecomeOrganizerUiState.Loading
      val result = createOrganizationCallback(org)
      result.fold(
          onSuccess = { _uiState.value = BecomeOrganizerUiState.Success(it) },
          onFailure = { _uiState.value = BecomeOrganizerUiState.Error(it.message ?: "Failed") })
    }
  }

  fun resetForm() {
    _formState.value = BecomeOrganizerFormState()
    _uiState.value = BecomeOrganizerUiState.Idle
  }

  fun clearError() {
    _uiState.value = BecomeOrganizerUiState.Idle
  }

  data class BecomeOrganizerFormState(
      val name: String = "",
      val description: String = "",
      val contactEmail: String = "",
      val contactPhone: String = "",
      val website: String = "",
      val instagram: String = "",
      val facebook: String = "",
      val tiktok: String = "",
      val address: String = ""
  )
}

sealed class BecomeOrganizerUiState {
  object Idle : BecomeOrganizerUiState()

  object Loading : BecomeOrganizerUiState()

  data class Success(val organizationId: String) : BecomeOrganizerUiState()

  data class Error(val message: String) : BecomeOrganizerUiState()
}
