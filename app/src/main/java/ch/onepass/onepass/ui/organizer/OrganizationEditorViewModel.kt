package ch.onepass.onepass.ui.organizer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.storage.StorageRepository
import ch.onepass.onepass.model.storage.StorageRepositoryFirebase
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
data class OrganizationEditorUiState(
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
 * @property profileImageUri Optional URI of the selected profile image.
 * @property coverImageUri Optional URI of the selected cover image.
 */
data class OrganizationEditorData(
    val id: String,
    val name: String,
    val description: String,
    val contactEmail: String?,
    val contactPhone: String?,
    val phonePrefix: String?,
    val website: String?,
    val instagram: String?,
    val facebook: String?,
    val tiktok: String?,
    val address: String?,
    val profileImageUri: Uri?,
    val coverImageUri: Uri?
) {
  companion object {
    /**
     * Create [OrganizationEditorData] from [OrganizationFormState].
     *
     * @param id The unique identifier of the organization.
     * @param formState The form state containing the editable fields.
     * @return An instance of [OrganizationEditorData] populated from the form state.
     */
    fun fromForm(id: String, formState: OrganizationFormState): OrganizationEditorData {
      return OrganizationEditorData(
          id = id,
          name = formState.name.value,
          description = formState.description.value,
          contactEmail = formState.contactEmail.value.ifBlank { null },
          contactPhone = formState.contactPhone.value.ifBlank { null },
          phonePrefix = formState.contactPhonePrefix.value.ifBlank { null },
          website = formState.website.value.ifBlank { null },
          instagram = formState.instagram.value.ifBlank { null },
          facebook = formState.facebook.value.ifBlank { null },
          tiktok = formState.tiktok.value.ifBlank { null },
          address = formState.address.value.ifBlank { null },
          profileImageUri = formState.profileImageUri,
          coverImageUri = formState.coverImageUri)
    }
  }
}

/**
 * ViewModel for editing organization details.
 *
 * @property repository The [OrganizationRepository] used for data operations.
 * @property storageRepository The [StorageRepository] used for image upload operations.
 */
class OrganizationEditorViewModel(
    private val repository: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val storageRepository: StorageRepository = StorageRepositoryFirebase()
) : ViewModel() {

  /** The UI state exposed to the Composable. */
  private val _uiState = MutableStateFlow(OrganizationEditorUiState())
  /** The UI state exposed to the Composable. */
  val uiState: StateFlow<OrganizationEditorUiState> = _uiState.asStateFlow()

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
   * Uploads the profile image to storage if a new one is selected.
   *
   * @param organizationId The organization ID to use for the storage path.
   * @param imageUri The URI of the image to upload, or null if no new image selected.
   * @return Result containing the uploaded image URL or null if no upload needed.
   */
  private suspend fun uploadProfileImage(organizationId: String, imageUri: Uri?): Result<String?> {
    if (imageUri == null) {
      return Result.success(null)
    }

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
   * Uploads the cover image to storage if a new one is selected.
   *
   * @param organizationId The organization ID to use for the storage path.
   * @param imageUri The URI of the image to upload, or null if no new image selected.
   * @return Result containing the uploaded image URL or null if no upload needed.
   */
  private suspend fun uploadCoverImage(organizationId: String, imageUri: Uri?): Result<String?> {
    if (imageUri == null) {
      return Result.success(null)
    }

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
   * Update organization details.
   *
   * @param data The [OrganizationEditorData] containing updated fields.
   */
  fun updateOrganization(data: OrganizationEditorData) {
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
        // Upload new images if selected
        val profileImageUrl =
            uploadProfileImage(data.id, data.profileImageUri).getOrElse {
              _uiState.value =
                  _uiState.value.copy(
                      isLoading = false,
                      errorMessage = it.message ?: "Failed to upload profile image")
              return@launch
            }

        val coverImageUrl =
            uploadCoverImage(data.id, data.coverImageUri).getOrElse {
              _uiState.value =
                  _uiState.value.copy(
                      isLoading = false,
                      errorMessage = it.message ?: "Failed to upload cover image")
              return@launch
            }

        // Update organization with new data and uploaded image URLs if new images were selected
        val org =
            currentOrg.copy(
                id = data.id,
                name = data.name,
                description = data.description,
                ownerId = currentOrg.ownerId,
                status = currentOrg.status,
                contactEmail = data.contactEmail,
                contactPhone = data.contactPhone,
                phonePrefix = data.phonePrefix,
                website = data.website,
                instagram = data.instagram,
                facebook = data.facebook,
                tiktok = data.tiktok,
                address = data.address,
                profileImageUrl = profileImageUrl ?: currentOrg.profileImageUrl,
                coverImageUrl = coverImageUrl ?: currentOrg.coverImageUrl,
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
