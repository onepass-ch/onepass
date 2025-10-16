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
 * ViewModel for the Create Event Form screen. Manages the state of the form and handles event
 * creation logic.
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

    return Event(
        title = state.title,
        description = state.description,
        organizerId = organizerId,
        organizerName = organizerName,
        status = EventStatus.DRAFT,
        location = null, // TODO: Create Location object from location string (requires geocoding)
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

      // Create event in repository
      val result = eventRepository.createEvent(event)

      result.fold(
          onSuccess = { eventId ->
            _uiState.value = CreateEventUiState.Success(eventId)
            resetForm()
          },
          onFailure = { error ->
            _uiState.value = CreateEventUiState.Error(error.message ?: "Failed to create event")
          })
    }
  }

  /** Resets the form to initial state */
  fun resetForm() {
    _formState.value = CreateEventFormState()
    _uiState.value = CreateEventUiState.Idle
  }

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
