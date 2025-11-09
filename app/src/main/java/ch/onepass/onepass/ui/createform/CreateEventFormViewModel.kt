package ch.onepass.onepass.ui.createform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
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

  // Track if we're in edit mode
  private var editingEventId: String? = null
  private var originalEvent: Event? = null

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
    val state = _formState.value

    if (state.title.isBlank()) {
      errors.add("Title is required")
    }
    if (state.description.isBlank()) {
      errors.add("Description is required")
    }
    if (state.date.isBlank()) {
      errors.add("Date is required")
    }
    if (state.startTime.isBlank()) {
      errors.add("Start time is required")
    }
    if (state.endTime.isBlank()) {
      errors.add("End time is required")
    }
    if (state.location.isBlank()) {
      errors.add("Location is required")
    }
    if (state.price.isBlank()) {
      errors.add("Price is required")
    } else {
      state.price.toDoubleOrNull() ?: errors.add("Price must be a valid number")
    }
    if (state.capacity.isBlank()) {
      errors.add("Capacity is required")
    } else {
      state.capacity.toIntOrNull() ?: errors.add("Capacity must be a valid number")
    }

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
      baseEvent.copy(
          title = state.title,
          description = state.description,
          startTime = startTimestamp,
          endTime = endTimestamp,
          capacity = capacity,
          ticketsRemaining = capacity - (baseEvent.ticketsIssued - baseEvent.ticketsRemaining),
          pricingTiers =
              listOf(
                  PricingTier(
                      name = "General", price = price, quantity = capacity, remaining = capacity)))
    } else {
      // Create new event
      Event(
          title = state.title,
          description = state.description,
          organizerId = organizerId,
          organizerName = organizerName,
          status = EventStatus.DRAFT,
          location = null,
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
            resetForm()
          },
          onFailure = { error ->
            val action = if (editingEventId != null) "update" else "create"
            _uiState.value = CreateEventUiState.Error(error.message ?: "Failed to $action event")
          })
    }
  }

  /** Resets the form to initial state */
  fun resetForm() {
    _formState.value = CreateEventFormState()
    _uiState.value = CreateEventUiState.Idle
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
