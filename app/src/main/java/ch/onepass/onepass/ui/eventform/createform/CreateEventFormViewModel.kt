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

      // Generate event ID first for image uploads
      val eventId = java.util.UUID.randomUUID().toString()
      android.util.Log.d("CreateEventFormVM", "Generated event ID: $eventId")

      // Upload images to storage if any selected
      android.util.Log.d("CreateEventFormVM", "Uploading images...")
      val imageUploadResult = uploadSelectedImages(eventId)
      val imageUrls =
          imageUploadResult.getOrElse {
            android.util.Log.e("CreateEventFormVM", "Image upload failed: ${it.message}")
            _uiState.value = CreateEventUiState.Error(it.message ?: "Failed to upload images")
            return@launch
          }
      android.util.Log.d("CreateEventFormVM", "Images uploaded: ${imageUrls.size} images")

      // Build new Event from parsed data with real organization data and uploaded images
      val event =
          Event(
              eventId = eventId,
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
              images = imageUrls,
              tags = emptyList())

      android.util.Log.d("CreateEventFormVM", "Creating event in repository...")
      // Create event in repository
      val result = eventRepository.createEvent(event)

      result.fold(
          onSuccess = { createdEventId -> 
            android.util.Log.d("CreateEventFormVM", "Event created successfully: $createdEventId")
            _uiState.value = CreateEventUiState.Success(createdEventId) 
          },
          onFailure = { error ->
            android.util.Log.e("CreateEventFormVM", "Event creation failed: ${error.message}")
            _uiState.value = CreateEventUiState.Error(error.message ?: "Failed to create event")
          })
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
