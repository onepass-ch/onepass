package ch.onepass.onepass.ui.eventform.editform

import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.LocationRepository
import ch.onepass.onepass.model.map.NominatimLocationRepository
import ch.onepass.onepass.model.storage.StorageRepository
import ch.onepass.onepass.model.storage.StorageRepositoryFirebase
import ch.onepass.onepass.ui.eventform.EventFormViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * ViewModel for the Edit Event Form screen. Manages the state of the form and handles event update
 * logic.
 */
class EditEventFormViewModel(
    eventRepository: EventRepository = EventRepositoryFirebase(),
    locationRepository: LocationRepository = NominatimLocationRepository(),
    storageRepository: StorageRepository = StorageRepositoryFirebase()
) : EventFormViewModel(eventRepository, locationRepository, storageRepository) {

  // UI state
  private val _uiState = MutableStateFlow<EditEventUiState>(EditEventUiState.Idle)
  val uiState: StateFlow<EditEventUiState> = _uiState.asStateFlow()

  private var originalEvent: Event? = null

  /** Loads the event data into the form */
  fun loadEvent(eventId: String) {
    viewModelScope.launch {
      _uiState.value = EditEventUiState.Loading

      try {
        val event = eventRepository.getEventById(eventId).firstOrNull()

        if (event == null) {
          _uiState.value = EditEventUiState.LoadError("Event not found")
          return@launch
        }

        originalEvent = event
        _formState.value = event.toFormState()
        _uiState.value = EditEventUiState.Idle
      } catch (e: Exception) {
        _uiState.value = EditEventUiState.LoadError(e.message ?: "Failed to load event")
      }
    }
  }

  /** Updates the event with current form data */
  fun updateEvent() {
    if (_uiState.value is EditEventUiState.Updating) return

    viewModelScope.launch {
      // Original event check first
      val original = originalEvent
      if (original == null) {
        _uiState.value = EditEventUiState.Error("Original event not loaded")
        return@launch
      }

      val parsed = validateAndParse()
      if (parsed == null) {
        val message =
            if (fieldErrors.value.isNotEmpty()) {
              "Please fix validation errors"
            } else {
              "Failed to process form data"
            }
        _uiState.value = EditEventUiState.Error(message)
        return@launch
      }

      _uiState.value = EditEventUiState.Updating

      // Upload new images if any selected
      if (_formState.value.selectedImageUris.isNotEmpty()) {
        // Start the upload and collect the state
        startImageUpload(original.eventId)

        // Collect the upload state to handle the result
        imageUploadState.collect { uploadState ->
          when (uploadState) {
            is ImageUploadState.Idle -> {
              // Still waiting for upload to start
            }
            is ImageUploadState.Uploading -> {
              // Upload in progress
            }
            is ImageUploadState.Success -> {
              // Combine existing images with newly uploaded ones
              val allImageUrls = original.images + uploadState.urls

              // Create updated event from parsed data
              val updatedEvent =
                  original.copy(
                      title = parsed.title,
                      description = parsed.description,
                      startTime = parsed.startTime,
                      endTime = parsed.endTime,
                      capacity = parsed.capacity,
                      location = parsed.selectedLocation,
                      images = allImageUrls,
                      pricingTiers =
                          listOf(
                              PricingTier(
                                  name = "General",
                                  price = parsed.price,
                                  quantity = parsed.capacity,
                                  remaining = parsed.capacity - original.ticketsIssued)))

              // Update event in repository
              val result = eventRepository.updateEvent(updatedEvent)

              result.fold(
                  onSuccess = {
                    originalEvent = updatedEvent
                    _uiState.value = EditEventUiState.Success
                    resetImageUploadState()
                  },
                  onFailure = { error ->
                    _uiState.value =
                        EditEventUiState.Error(error.message ?: "Failed to update event")
                    resetImageUploadState()
                  })
              return@collect
            }
            is ImageUploadState.Error -> {
              _uiState.value = EditEventUiState.Error(uploadState.message)
              resetImageUploadState()
              return@collect
            }
          }
        }
      } else {
        // No new images to upload, just update the event
        // Create updated event from parsed data
        val updatedEvent =
            original.copy(
                title = parsed.title,
                description = parsed.description,
                startTime = parsed.startTime,
                endTime = parsed.endTime,
                capacity = parsed.capacity,
                location = parsed.selectedLocation,
                images = original.images,
                tags = parsed.tags,
                pricingTiers =
                    listOf(
                        PricingTier(
                            name = "General",
                            price = parsed.price,
                            quantity = parsed.capacity,
                            remaining = parsed.capacity - original.ticketsIssued)))

        // Update event in repository
        val result = eventRepository.updateEvent(updatedEvent)

        result.fold(
            onSuccess = {
              originalEvent = updatedEvent
              _uiState.value = EditEventUiState.Success
            },
            onFailure = { error ->
              _uiState.value = EditEventUiState.Error(error.message ?: "Failed to update event")
            })
      }
    }
  }

  /** Clears any error state */
  fun clearError() {
    if (_uiState.value is EditEventUiState.Error) {
      _uiState.value = EditEventUiState.Idle
    }
  }
}

/** Sealed class representing the UI state of event editing */
sealed class EditEventUiState {
  /** Initial state, no operation in progress */
  object Idle : EditEventUiState()

  /** Event loading in progress */
  object Loading : EditEventUiState()

  /** Event successfully updated */
  object Success : EditEventUiState()

  /** Event update failed */
  data class Error(val message: String) : EditEventUiState()

  /** Event loading failed */
  data class LoadError(val message: String) : EditEventUiState()

  /** Event updating in progress */
  object Updating : EditEventUiState()
}

/** Extension function to convert Event to EventFormState */
private fun Event.toFormState(): EventFormViewModel.EventFormState {
  val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
  val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

  val dateString = startTime?.toDate()?.let { dateFormat.format(it) } ?: ""
  val startTimeString = startTime?.toDate()?.let { timeFormat.format(it) } ?: ""
  val endTimeString = endTime?.toDate()?.let { timeFormat.format(it) } ?: ""
  val lowestPrice = this.lowestPrice

  return EventFormViewModel.EventFormState(
      title = title,
      description = description,
      startTime = startTimeString,
      endTime = endTimeString,
      date = dateString,
      location = location?.name ?: "",
      price = if (lowestPrice > 0u) lowestPrice.toString() else "0",
      capacity = capacity.toString(),
      selectedLocation = this.location,
      selectedImageUris = emptyList() // Existing images are in Event.images, not form state
      )
}
