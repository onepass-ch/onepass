package ch.onepass.onepass.ui.createform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Create/Edit Event Form screen. Manages the state of the form and handles event
 * creation and update logic.
 *
 * @property eventRepository Repository for event operations
 */
class CreateEventFormViewModel(
    // TODO: make sure this is replaced with
    // Dependency Injection in the future (after test)
    private val eventRepository: EventRepository = EventRepositoryFirebase()
) : ViewModel() {

  // Form state
  private val _formState = MutableStateFlow(CreateEventFormState())
  val formState: StateFlow<CreateEventFormState> = _formState.asStateFlow()

  // UI state
  private val _uiState = MutableStateFlow<CreateEventUiState>(CreateEventUiState.Idle)
  val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

  // Field-specific validation errors
  private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
  val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors.asStateFlow()

  // Track if we're in edit mode
  private var editingEventId: String? = null
  private var originalEvent: Event? = null

  /** Enum defining validation errors with their keys and messages */
  enum class ValidationError(val key: String, val message: String) {
    TITLE("title", "Title is required"),
    DESCRIPTION("description", "Description is required"),
    DATE("date", "Date is required"),
    START_TIME("startTime", "Start time is required"),
    END_TIME("endTime", "End time is required"),
    LOCATION("location", "Location is required"),
    PRICE_INVALID("price", "Price must be a valid number"),
    CAPACITY_INVALID("capacity", "Capacity must be a valid number")
  }

  /** Updates the event title */
  fun updateTitle(title: String) {
    _formState.value = _formState.value.copy(title = title)
  }

  /** Updates the event description */
  fun updateDescription(description: String) {
    _formState.value = _formState.value.copy(description = description)
  }

  /** Updates the event start time */
  fun updateStartTime(startTime: String) {
    _formState.value = _formState.value.copy(startTime = startTime)
  }

  /** Updates the event end time */
  fun updateEndTime(endTime: String) {
    _formState.value = _formState.value.copy(endTime = endTime)
  }

  /** Updates the event date */
  fun updateDate(date: String) {
    _formState.value = _formState.value.copy(date = date)
  }

  /** Updates the event location */
  fun updateLocation(location: String) {
    _formState.value = _formState.value.copy(location = location)
  }

  /** Updates the ticket price */
  fun updatePrice(price: String) {
    _formState.value = _formState.value.copy(price = price)
  }

  /** Updates the event capacity */
  fun updateCapacity(capacity: String) {
    _formState.value = _formState.value.copy(capacity = capacity)
  }

  /**
   * Loads an existing event for editing.
   *
   * @param eventId The ID of the event to edit.
   */
  fun loadEvent(eventId: String) {
    viewModelScope.launch {
      _uiState.value = CreateEventUiState.Loading
      try {
        eventRepository.getEventById(eventId).collect { event ->
          if (event != null) {
            editingEventId = eventId
            originalEvent = event
            populateFormFromEvent(event)
            _uiState.value = CreateEventUiState.Idle
          } else {
            _uiState.value = CreateEventUiState.Error("Event not found")
          }
        }
      } catch (e: Exception) {
        _uiState.value = CreateEventUiState.Error(e.message ?: "Failed to load event")
      }
    }
  }

  /**
   * Populates the form fields from an existing event.
   *
   * @param event The event to populate from.
   */
  private fun populateFormFromEvent(event: Event) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val date = event.startTime?.toDate()?.let { dateFormat.format(it) } ?: ""
    val startTime = event.startTime?.toDate()?.let { timeFormat.format(it) } ?: ""
    val endTime = event.endTime?.toDate()?.let { timeFormat.format(it) } ?: ""

    val price = event.pricingTiers.firstOrNull()?.price?.toString() ?: ""

    _formState.value =
        CreateEventFormState(
            title = event.title,
            description = event.description,
            startTime = startTime,
            endTime = endTime,
            date = date,
            location = event.displayLocation,
            price = price,
            capacity = event.capacity.toString())
  }

  /**
   * Validates the form data
   *
   * @return List of validation errors, empty if valid
   */
  private fun validateForm(): List<String> {
    val errors = mutableListOf<String>()
    val fieldErrorsMap = mutableMapOf<String, String>()
    val state = _formState.value

    if (state.title.isBlank()) {
      errors.add(ValidationError.TITLE.message)
      fieldErrorsMap[ValidationError.TITLE.key] = ValidationError.TITLE.message
    }
    if (state.description.isBlank()) {
      errors.add(ValidationError.DESCRIPTION.message)
      fieldErrorsMap[ValidationError.DESCRIPTION.key] = ValidationError.DESCRIPTION.message
    }
    if (state.date.isBlank()) {
      errors.add(ValidationError.DATE.message)
      fieldErrorsMap[ValidationError.DATE.key] = ValidationError.DATE.message
    }
    if (state.startTime.isBlank()) {
      errors.add(ValidationError.START_TIME.message)
      fieldErrorsMap[ValidationError.START_TIME.key] = ValidationError.START_TIME.message
    }
    if (state.endTime.isBlank()) {
      errors.add(ValidationError.END_TIME.message)
      fieldErrorsMap[ValidationError.END_TIME.key] = ValidationError.END_TIME.message
    }
    if (state.location.isBlank()) {
      errors.add(ValidationError.LOCATION.message)
      fieldErrorsMap[ValidationError.LOCATION.key] = ValidationError.LOCATION.message
    }
    if (state.price.isBlank()) {
      errors.add("Price is required")
      fieldErrorsMap["price"] = "Price is required"
    } else if (state.price.toDoubleOrNull() == null) {
      errors.add(ValidationError.PRICE_INVALID.message)
      fieldErrorsMap[ValidationError.PRICE_INVALID.key] = ValidationError.PRICE_INVALID.message
    }
    if (state.capacity.isBlank()) {
      errors.add("Capacity is required")
      fieldErrorsMap["capacity"] = "Capacity is required"
    } else if (state.capacity.toIntOrNull() == null) {
      errors.add(ValidationError.CAPACITY_INVALID.message)
      fieldErrorsMap[ValidationError.CAPACITY_INVALID.key] = ValidationError.CAPACITY_INVALID.message
    }

    // Update field errors state
    _fieldErrors.value = fieldErrorsMap

    return errors
  }

  /**
   * Converts form data to Event object
   *
   * @param organizerId The ID of the user creating the event
   * @param organizerName The name of the organizer
   * @return Event object or null if conversion fails
   */
  private fun formStateToEvent(organizerId: String, organizerName: String): Event? {
    val state = _formState.value

    val capacity = state.capacity.toIntOrNull() ?: return null
    val price = state.price.toDoubleOrNull() ?: return null

    // Parse date and time strings to Firebase Timestamps
    val startTimestamp = parseDateAndTime(state.date, state.startTime)
    val endTimestamp = parseDateAndTime(state.date, state.endTime)

    // If editing, preserve original event data
    val baseEvent = originalEvent

    return if (baseEvent != null) {
      // Update existing event
      // Calculate tickets already sold
      val ticketsSold = baseEvent.capacity - baseEvent.ticketsRemaining
      val newTicketsRemaining = capacity - ticketsSold
      
      baseEvent.copy(
          title = state.title,
          description = state.description,
          location = if (state.location.isNotBlank()) Location(name = state.location) else baseEvent.location,
          startTime = startTimestamp,
          endTime = endTimestamp,
          capacity = capacity,
          ticketsRemaining = newTicketsRemaining,
          pricingTiers =
              listOf(
                  PricingTier(
                      name = "General", price = price, quantity = capacity, remaining = newTicketsRemaining)))
    } else {
      // Create new event
      Event(
          title = state.title,
          description = state.description,
          organizerId = organizerId,
          organizerName = organizerName,
          status = EventStatus.DRAFT,
          location = if (state.location.isNotBlank()) Location(name = state.location) else null,
          startTime = startTimestamp,
          endTime = endTimestamp,
          capacity = capacity,
          ticketsRemaining = capacity,
          ticketsIssued = 0,
          ticketsRedeemed = 0,
          currency = "CHF",
          pricingTiers =
              listOf(
                  PricingTier(
                      name = "General", price = price, quantity = capacity, remaining = capacity)),
          images = emptyList(),
          tags = emptyList())
    }
  }

  /**
   * Helper function to combine date and time strings into a Firebase Timestamp
   *
   * @param dateString Date in format "dd/MM/yyyy" (e.g., "14/10/2025")
   * @param timeString Time in format "HH:mm" (e.g., "14:30")
   * @return Firebase Timestamp or null if parsing fails
   */
  private fun parseDateAndTime(dateString: String, timeString: String): Timestamp? {
    if (dateString.isEmpty() || timeString.isEmpty()) return null

    return try {
      // Combine date and time: "14/10/2025" + "14:30" = "14/10/2025 14:30"
      val dateTimeString = "$dateString $timeString"

      // Parse using SimpleDateFormat
      val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
      val date = dateFormat.parse(dateTimeString) ?: return null

      // Convert Java Date to Firebase Timestamp
      Timestamp(date)
    } catch (e: Exception) {
      null // Return null if parsing fails
    }
  }

  /**
   * Creates a new event with the current form data
   *
   * @param organizerId The ID of the user creating the event
   * @param organizerName The name of the organizer
   */
  fun createEvent(organizerId: String, organizerName: String) {
    saveEvent(organizerId, organizerName)
  }

  /**
   * Saves the event (create or update based on edit mode).
   *
   * @param organizerId The ID of the user creating/editing the event
   * @param organizerName The name of the organizer
   */
  fun saveEvent(organizerId: String, organizerName: String) {
    viewModelScope.launch {
      // Validate form
      val errors = validateForm()
      if (errors.isNotEmpty()) {
        _uiState.value = CreateEventUiState.Error(errors.joinToString("\n"))
        return@launch
      }
      
      // Clear field errors if validation passes
      _fieldErrors.value = emptyMap()

      // Convert form to event
      val event = formStateToEvent(organizerId, organizerName)
      if (event == null) {
        _uiState.value = CreateEventUiState.Error("Failed to create event from form data")
        return@launch
      }

      // Set loading state
      _uiState.value = CreateEventUiState.Loading

      // Create or update event in repository
      val result =
          if (editingEventId != null) {
            eventRepository.updateEvent(event).map {
              editingEventId!!
            } // Return the eventId on success
          } else {
            eventRepository.createEvent(event)
          }

      result.fold(
          onSuccess = { eventId ->
            _uiState.value = CreateEventUiState.Success(eventId)
            // Reset form fields but keep UI state as Success for navigation
            resetFormFields()
          },
          onFailure = { error ->
            val action = if (editingEventId != null) "update" else "create"
            _uiState.value = CreateEventUiState.Error(error.message ?: "Failed to $action event")
          })
    }
  }

  /** Resets only the form fields, not the UI state */
  private fun resetFormFields() {
    _formState.value = CreateEventFormState()
    _fieldErrors.value = emptyMap()
    editingEventId = null
    originalEvent = null
  }

  /** Resets the form to initial state including UI state */
  fun resetForm() {
    _formState.value = CreateEventFormState()
    _uiState.value = CreateEventUiState.Idle
    _fieldErrors.value = emptyMap()
    editingEventId = null
    originalEvent = null
  }

  /** Returns true if we're in edit mode */
  fun isEditMode(): Boolean = editingEventId != null

  /** Clears any error state */
  fun clearError() {
    _uiState.value = CreateEventUiState.Idle
  }
}

/** Data class representing the state of the create event form */
data class CreateEventFormState(
    val title: String = "",
    val description: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val date: String = "",
    val location: String = "",
    val price: String = "",
    val capacity: String = ""
)

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
