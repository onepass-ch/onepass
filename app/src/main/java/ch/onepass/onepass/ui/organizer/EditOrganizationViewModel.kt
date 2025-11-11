package ch.onepass.onepass.ui.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.organization.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * UI state for the EditOrganization screen.
 *
 * @property isLoading Indicates if a loading operation is in progress.
 * @property organization The organization being edited, or null if not loaded.
 * @property success Indicates if the last update operation was successful.
 * @property errorMessage An error message if an error occurred, or null.
 */
data class EditOrganizationUiState(
    val isLoading: Boolean = false,
    val organization: Organization? = null,
    val success: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Data class representing the editable fields of an organization.
 *
 * @property id The unique identifier of the organization.
 * @property name The name of the organization.
 * @property description A short description of the organization.
 * @property contactEmail Optional contact email address.
 * @property contactPhone Optional contact phone number.
 * @property website Optional website URL.
 * @property instagram Optional Instagram profile link or handle.
 * @property facebook Optional Facebook profile link or handle.
 * @property tiktok Optional TikTok profile link or handle.
 * @property address Optional physical address of the organization.
 */
data class EditOrganizationData(
    val id: String,
    val name: String,
    val description: String,
    val contactEmail: String?,
    val contactPhone: String?,
    val website: String?,
    val instagram: String?,
    val facebook: String?,
    val tiktok: String?,
    val address: String?
)

/**
 * ViewModel for editing organization details.
 *
 * @property repository The [OrganizationRepository] used for data operations.
 */
class EditOrganizationViewModel(
    private val repository: OrganizationRepository = OrganizationRepositoryFirebase()
) : ViewModel() {

  /** The UI state exposed to the Composable. */
  private val _uiState = MutableStateFlow(EditOrganizationUiState())
  /** The UI state exposed to the Composable. */
  val uiState: StateFlow<EditOrganizationUiState> = _uiState.asStateFlow()

  /**
   * Load organization details by ID.
   *
   * @param organizationId The ID of the organization to load.
   */
  fun loadOrganizationById(organizationId: String) {
    // Start loading the organization
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      try {
        val org = repository.getOrganizationById(organizationId).firstOrNull()
        // Update the UI state based on whether the organization was found
        if (org != null) {
          _uiState.value = _uiState.value.copy(isLoading = false, organization = org)
        } else {
          _uiState.value =
              _uiState.value.copy(
                  isLoading = false, organization = null, errorMessage = "Organization not found")
        }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
      }
    }
  }

  /**
   * Update organization details.
   *
   * @param data The [EditOrganizationData] containing updated fields.
   */
  fun updateOrganization(data: EditOrganizationData) {
    val currentOrg = _uiState.value.organization
    if (currentOrg == null) {
      // Cannot update if organization is not loaded
      _uiState.value = _uiState.value.copy(errorMessage = "Cannot update: organization not loaded")
      return
    }

    // Proceed with update
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, success = false)
      try {
        val org =
            currentOrg.copy(
                id = data.id,
                name = data.name,
                description = data.description,
                ownerId = currentOrg.ownerId,
                status = currentOrg.status,
                contactEmail = data.contactEmail,
                contactPhone = data.contactPhone,
                website = data.website,
                instagram = data.instagram,
                facebook = data.facebook,
                tiktok = data.tiktok,
                address = data.address,
                createdAt = currentOrg.createdAt ?: Timestamp.now() // fallback if null
                )

        val result = repository.updateOrganization(org)
        // Update the UI state based on the result
        if (result.isSuccess) {
          _uiState.value =
              _uiState.value.copy(isLoading = false, success = true, organization = org)
        } else {
          _uiState.value =
              _uiState.value.copy(
                  isLoading = false,
                  errorMessage = result.exceptionOrNull()?.message ?: "Update failed")
        }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Network error")
      }
    }
  }

  /** Clear the success flag after it has been handled. */
  fun clearSuccessFlag() {
    _uiState.value = _uiState.value.copy(success = false)
  }

  /** Clear the error message after it has been handled. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
