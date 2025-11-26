package ch.onepass.onepass.ui.eventform

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.map.LocationRepository
import ch.onepass.onepass.model.map.NominatimLocationRepository
import ch.onepass.onepass.model.storage.StorageRepository
import ch.onepass.onepass.model.storage.StorageRepositoryFirebase
import ch.onepass.onepass.utils.DateTimeUtils
import ch.onepass.onepass.utils.ValidationUtils
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for event form handling. Contains common logic for form state management,
 * validation, and parsing.
 *
 * @property eventRepository Repository for event operations
 * @property storageRepository Repository for image storage operations
 */
abstract class EventFormViewModel(
    protected val eventRepository: EventRepository = EventRepositoryFirebase(),
    protected val locationRepository: LocationRepository = NominatimLocationRepository(),
    protected val storageRepository: StorageRepository = StorageRepositoryFirebase()
) : ViewModel() {

  enum class ValidationError(val key: String, val message: String) {
    TITLE("title", "Title cannot be empty"),
    DESCRIPTION("description", "Description cannot be empty"),
    DATE("date", "Date cannot be empty"),
    START_TIME("startTime", "Start time cannot be empty"),
    END_TIME("endTime", "End time cannot be empty"),
    TIME("time", "End time must be after start time"),
    LOCATION("location", "Location cannot be empty"),
    LOCATION_SELECT("location", "Please select a location from the suggestions"),
    PRICE_EMPTY("price", "Price cannot be empty"),
    PRICE_INVALID("price", "Invalid price format"),
    PRICE_NEGATIVE("price", "Price must be positive"),
    CAPACITY_EMPTY("capacity", "Capacity cannot be empty"),
    CAPACITY_INVALID("capacity", "Invalid capacity format"),
    CAPACITY_NEGATIVE("capacity", "Capacity must be positive");

    fun toError(): Pair<String, String> = key to message
  }

  data class EventFormState(
      val title: String = "",
      val description: String = "",
      val startTime: String = "",
      val endTime: String = "",
      val date: String = "",
      val location: String = "",
      val price: String = "",
      val capacity: String = "",
      val selectedLocation: Location? = null,
      val selectedImageUris: List<Uri> = emptyList()
  )

  data class ParsedFormData(
      val title: String,
      val description: String,
      val price: Double,
      val capacity: Int,
      val startTime: Timestamp,
      val endTime: Timestamp,
      val selectedLocation: Location?
  )

  protected val _formState = MutableStateFlow(EventFormState())
  val formState: StateFlow<EventFormState> = _formState.asStateFlow()

  protected val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
  val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors.asStateFlow()

  // Location search state
  private val _locationSearchResults = MutableStateFlow<List<Location>>(emptyList())
  val locationSearchResults: StateFlow<List<Location>> = _locationSearchResults.asStateFlow()

  private val _isSearchingLocation = MutableStateFlow(false)
  val isSearchingLocation: StateFlow<Boolean> = _isSearchingLocation.asStateFlow()

  private var locationSearchJob: Job? = null
  private val searchDebounceMs = 300L

  /** Updates the event title */
  fun updateTitle(title: String) {
    _formState.value = _formState.value.copy(title = title)
    clearFieldError(ValidationError.TITLE.key)
  }

  /** Updates the event description */
  fun updateDescription(description: String) {
    _formState.value = _formState.value.copy(description = description)
    clearFieldError(ValidationError.DESCRIPTION.key)
  }

  /** Updates the event start time */
  fun updateStartTime(startTime: String) {
    _formState.value = _formState.value.copy(startTime = startTime)
    clearFieldError(ValidationError.START_TIME.key, ValidationError.TIME.key)
  }

  /** Updates the event end time */
  fun updateEndTime(endTime: String) {
    _formState.value = _formState.value.copy(endTime = endTime)
    clearFieldError(ValidationError.END_TIME.key, ValidationError.TIME.key)
  }

  /** Updates the event date */
  fun updateDate(date: String) {
    _formState.value = _formState.value.copy(date = date)
    clearFieldError(ValidationError.DATE.key)
  }

  /** Updates the event location */
  fun updateLocation(location: String) {
    _formState.value = _formState.value.copy(location = location)

    locationSearchJob?.cancel()

    // Clear selected location when user types
    if (_formState.value.selectedLocation != null) {
      _formState.value = _formState.value.copy(selectedLocation = null)
    }

    clearFieldError(ValidationError.LOCATION.key, ValidationError.LOCATION_SELECT.key)

    // Start debounced search
    if (location.isNotBlank()) {
      locationSearchJob =
          viewModelScope.launch {
            delay(searchDebounceMs)
            searchLocation(location)
          }
    } else {
      _locationSearchResults.value = emptyList()
    }
  }

  /** Searches for locations using the location repository */
  private suspend fun searchLocation(query: String) {
    _isSearchingLocation.value = true
    try {
      val results = locationRepository.search(query)
      _locationSearchResults.value = results
    } catch (e: Exception) {
      _locationSearchResults.value = emptyList()
    } finally {
      _isSearchingLocation.value = false
    }
  }

  /** Selects a location from search results */
  fun selectLocation(location: Location) {
    _formState.value = _formState.value.copy(location = location.name, selectedLocation = location)
    _locationSearchResults.value = emptyList()
    clearFieldError(ValidationError.LOCATION.key, ValidationError.LOCATION_SELECT.key)
  }

  /** Updates the ticket price */
  fun updatePrice(price: String) {
    _formState.value = _formState.value.copy(price = price)
    clearFieldError(
        ValidationError.PRICE_EMPTY.key,
        ValidationError.PRICE_INVALID.key,
        ValidationError.PRICE_NEGATIVE.key)
  }

  /** Updates the event capacity */
  fun updateCapacity(capacity: String) {
    _formState.value = _formState.value.copy(capacity = capacity)
    clearFieldError(
        ValidationError.CAPACITY_EMPTY.key,
        ValidationError.CAPACITY_INVALID.key,
        ValidationError.CAPACITY_NEGATIVE.key)
  }

  /**
   * Validates the form data
   *
   * @return Map of validation errors, empty if valid
   */
  protected fun validateForm(): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    val state = _formState.value

    if (state.title.isBlank()) errors += ValidationError.TITLE.toError()
    if (state.description.isBlank()) errors += ValidationError.DESCRIPTION.toError()
    if (state.date.isBlank()) errors += ValidationError.DATE.toError()
    if (state.startTime.isBlank()) errors += ValidationError.START_TIME.toError()
    if (state.endTime.isBlank()) errors += ValidationError.END_TIME.toError()

    if (state.startTime.isNotBlank() &&
        state.endTime.isNotBlank() &&
        state.endTime <= state.startTime)
        errors += ValidationError.TIME.toError()

    if (state.location.isBlank()) {
      errors += ValidationError.LOCATION.toError()
    } else if (state.selectedLocation == null) {
      errors += ValidationError.LOCATION_SELECT.toError()
    }

    if (state.price.isBlank()) {
      errors += ValidationError.PRICE_EMPTY.toError()
    } else {
      if (!ValidationUtils.isPositiveNumber(state.price)) {
        errors +=
            if (state.price.toDoubleOrNull() == null) ValidationError.PRICE_INVALID.toError()
            else ValidationError.PRICE_NEGATIVE.toError()
      }
    }

    if (state.capacity.isBlank()) {
      errors += ValidationError.CAPACITY_EMPTY.toError()
    } else {
      if (!ValidationUtils.isPositiveInteger(state.capacity)) {
        errors +=
            if (state.capacity.toIntOrNull() == null) ValidationError.CAPACITY_INVALID.toError()
            else ValidationError.CAPACITY_NEGATIVE.toError()
      }
    }

    return errors
  }

  /**
   * Validates the form and parses the data if valid.
   *
   * @return ParsedFormData if valid and parsable, null otherwise
   */
  protected fun validateAndParse(): ParsedFormData? {
    val errors = validateForm()
    _fieldErrors.value = errors
    if (errors.isNotEmpty()) {
      return null
    }

    _fieldErrors.value = emptyMap()
    val state = _formState.value

    val capacity = state.capacity.toIntOrNull() ?: return null
    val price = state.price.toDoubleOrNull() ?: return null
    val startTimestamp = parseDateAndTime(state.date, state.startTime) ?: return null
    val endTimestamp = parseDateAndTime(state.date, state.endTime) ?: return null

    return ParsedFormData(
        title = state.title,
        description = state.description,
        price = price,
        capacity = capacity,
        startTime = startTimestamp,
        endTime = endTimestamp,
        selectedLocation = state.selectedLocation)
  }

  /**
   * Helper function to combine date and time strings into a Firebase Timestamp
   *
   * @param dateString Date in format "dd/MM/yyyy" (e.g., "14/10/2025")
   * @param timeString Time in format "HH:mm" (e.g., "14:30")
   * @return Firebase Timestamp or null if parsing fails
   */
  protected fun parseDateAndTime(dateString: String, timeString: String): Timestamp? {
    return DateTimeUtils.parseDateAndTime(dateString, timeString)
  }

  protected fun clearFieldError(vararg fieldKeys: String) {
    _fieldErrors.update { currentErrors ->
      currentErrors.toMutableMap().apply { fieldKeys.forEach { remove(it) } }
    }
  }

  /**
   * Adds a selected image URI to the form state (does not upload yet)
   *
   * @param uri The URI of the selected image
   */
  fun selectImage(uri: Uri) {
    _formState.update { currentState ->
      currentState.copy(selectedImageUris = currentState.selectedImageUris + uri)
    }
  }

  /**
   * Removes an image URI from the form state
   *
   * @param uri The URI to remove
   */
  fun removeImage(uri: Uri) {
    _formState.update { currentState ->
      currentState.copy(selectedImageUris = currentState.selectedImageUris.filter { it != uri })
    }
  }

  /**
   * Uploads selected images to storage
   *
   * @param eventId The event ID to use for the storage path
   * @return Result containing list of uploaded image URLs or error
   */
  protected suspend fun uploadSelectedImages(eventId: String): Result<List<String>> {
    val imageUris = _formState.value.selectedImageUris

    if (imageUris.isEmpty()) {
      return Result.success(emptyList())
    }

    val uploadedUrls = mutableListOf<String>()

    try {
      imageUris.forEachIndexed { index, uri ->
        val storagePath = "events/$eventId/image_$index.jpg"
        val result = storageRepository.uploadImage(uri, storagePath)

        result.fold(
            onSuccess = { url -> uploadedUrls.add(url) },
            onFailure = { error ->
              return Result.failure(
                  Exception("Failed to upload image ${index + 1}: ${error.message}"))
            })
      }

      return Result.success(uploadedUrls)
    } catch (e: Exception) {
      return Result.failure(Exception("Image upload failed: ${e.message}"))
    }
  }

  /** Resets the form to initial state */
  fun resetForm() {
    _formState.value = EventFormState()
  }
}
