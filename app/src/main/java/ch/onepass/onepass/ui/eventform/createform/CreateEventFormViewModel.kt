package ch.onepass.onepass.ui.eventform.createform

import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.LocationRepository
import ch.onepass.onepass.model.map.NominatimLocationRepository
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.storage.StorageRepository
import ch.onepass.onepass.model.storage.StorageRepositoryFirebase
import ch.onepass.onepass.ui.eventform.EventFormViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * ViewModel for the Create Event Form screen. Manages the state of the form and handles event
 * creation logic.
 */
open class CreateEventFormViewModel(
    eventRepository: EventRepository = EventRepositoryFirebase(),
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase(),
    locationRepository: LocationRepository = NominatimLocationRepository(),
    storageRepository: StorageRepository = StorageRepositoryFirebase()
) : EventFormViewModel(eventRepository, locationRepository, storageRepository) {

  // UI state
  private val _uiState = MutableStateFlow<CreateEventUiState>(CreateEventUiState.Idle)
  val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

  private val _organizationId = MutableStateFlow<String?>(null)

  /** Sets the organization ID for event creation */
  fun setOrganizationId(organizationId: String) {
    _organizationId.value = organizationId
  }

  /** Creates a new event with the current form data */
  fun createEvent() {
    if (_uiState.value is CreateEventUiState.Loading ||
        _uiState.value is CreateEventUiState.Success)
        return

    android.util.Log.d("CreateEventFormVM", "createEvent() called")
    viewModelScope.launch {
      val orgId = _organizationId.value
      android.util.Log.d("CreateEventFormVM", "Organization ID: $orgId")
      if (orgId == null) {
        android.util.Log.e("CreateEventFormVM", "No organization selected")
        _uiState.value = CreateEventUiState.Error("No organization selected")
        return@launch
      }

      val parsed = validateAndParse()
      if (parsed == null) {
        val message =
            if (fieldErrors.value.isNotEmpty()) {
              val errors = fieldErrors.value.entries.joinToString(", ") { "${it.key}: ${it.value}" }
              android.util.Log.e("CreateEventFormVM", "Validation errors: $errors")
              "Please fix validation errors"
            } else {
              android.util.Log.e("CreateEventFormVM", "Failed to parse form data")
              "Failed to create event from form data"
            }
        _uiState.value = CreateEventUiState.Error(message)
        return@launch
      }

      android.util.Log.d("CreateEventFormVM", "Form validated successfully")
      // Set loading state
      _uiState.value = CreateEventUiState.Loading

      // Load organization data
      android.util.Log.d("CreateEventFormVM", "Fetching organization...")
      val organization = organizationRepository.getOrganizationById(orgId).firstOrNull()
      if (organization == null) {
        android.util.Log.e("CreateEventFormVM", "Organization not found")
        _uiState.value = CreateEventUiState.Error("Organization not found")
        return@launch
      }
      android.util.Log.d("CreateEventFormVM", "Organization found: ${organization.name}")

      // Build new Event from parsed data without images first
      val event =
          Event(
              eventId = "", // Will be set by repository
              title = parsed.title,
              description = parsed.description,
              organizerId = organization.id,
              organizerName = organization.name,
              status = EventStatus.PUBLISHED,
              location = parsed.selectedLocation,
              startTime = parsed.startTime,
              endTime = parsed.endTime,
              capacity = parsed.capacity,
              ticketsRemaining = parsed.capacity,
              ticketsIssued = 0,
              ticketsRedeemed = 0,
              currency = "CHF",
              pricingTiers =
                  listOf(
                      PricingTier(
                          name = "General",
                          price = parsed.price,
                          quantity = parsed.capacity,
                          remaining = parsed.capacity)),
              images = emptyList(), // No images yet
              tags = parsed.tags)

      android.util.Log.d("CreateEventFormVM", "Creating event in repository...")
      // Create event in repository first to get the real event ID
      val createResult = eventRepository.createEvent(event)

      val createdEventId =
          createResult.getOrElse { error ->
            android.util.Log.e("CreateEventFormVM", "Event creation failed: ${error.message}")
            _uiState.value = CreateEventUiState.Error(error.message ?: "Failed to create event")
            return@launch
          }

      android.util.Log.d("CreateEventFormVM", "Event created with ID: $createdEventId")

      // Upload images to storage using the created event ID
      if (_formState.value.selectedImageUris.isNotEmpty()) {
        android.util.Log.d("CreateEventFormVM", "Uploading images...")

        // Start the upload and collect the state
        startImageUpload(createdEventId)

        // Collect the upload state to handle the result
        imageUploadState.collect { uploadState ->
          when (uploadState) {
            is ImageUploadState.Idle -> {
              // Still waiting for upload to start
            }
            is ImageUploadState.Uploading -> {
              // Upload in progress
              android.util.Log.d("CreateEventFormVM", "Image upload in progress...")
            }
            is ImageUploadState.Success -> {
              android.util.Log.d(
                  "CreateEventFormVM", "Images uploaded: ${uploadState.urls.size} images")

              // Update event with uploaded image URLs
              android.util.Log.d("CreateEventFormVM", "Updating event with image URLs...")
              val updateResult = eventRepository.updateEventImages(createdEventId, uploadState.urls)
              updateResult.fold(
                  onSuccess = {
                    android.util.Log.d(
                        "CreateEventFormVM", "Event images updated successfully: $createdEventId")
                    _uiState.value = CreateEventUiState.Success(createdEventId)
                    resetImageUploadState()
                  },
                  onFailure = { error ->
                    android.util.Log.e(
                        "CreateEventFormVM", "Failed to update event images: ${error.message}")
                    _uiState.value =
                        CreateEventUiState.Error(error.message ?: "Failed to update event images")
                    resetImageUploadState()
                  })
              return@collect
            }
            is ImageUploadState.Error -> {
              android.util.Log.e("CreateEventFormVM", "Image upload failed: ${uploadState.message}")
              _uiState.value = CreateEventUiState.Error(uploadState.message)
              resetImageUploadState()
              return@collect
            }
          }
        }
      } else {
        android.util.Log.d("CreateEventFormVM", "No images to upload")
        _uiState.value = CreateEventUiState.Success(createdEventId)
      }
    }
  }
  /** Clears any error state */
  fun clearError() {
    _uiState.value = CreateEventUiState.Idle
  }
}

/** Sealed class representing the UI state of event creation */
sealed class CreateEventUiState {
  /** Initial state, no operation in progress */
  object Idle : CreateEventUiState()

  /** Event creation is in progress */
  object Loading : CreateEventUiState()

  /** Event creation succeeded */
  data class Success(val eventId: String) : CreateEventUiState()

  /** Event creation failed */
  data class Error(val message: String) : CreateEventUiState()
}
