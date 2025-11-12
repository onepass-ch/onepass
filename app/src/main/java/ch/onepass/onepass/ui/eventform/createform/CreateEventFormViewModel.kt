package ch.onepass.onepass.ui.eventform.createform

import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.ui.eventform.EventFormViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Create Event Form screen. Manages the state of the form and handles event
 * creation logic.
 */
class CreateEventFormViewModel(
    eventRepository: EventRepository = EventRepositoryFirebase(),
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase()
) : EventFormViewModel(eventRepository) {

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
    viewModelScope.launch {
      val orgId = _organizationId.value
      if (orgId == null) {
        _uiState.value = CreateEventUiState.Error("No organization selected")
        return@launch
      }

      val parsed = validateAndParse()
      if (parsed == null) {
        val message =
            if (fieldErrors.value.isNotEmpty()) {
              "Please fix validation errors"
            } else {
              "Failed to create event from form data"
            }
        _uiState.value = CreateEventUiState.Error(message)
        return@launch
      }

      // Load organization data
      organizationRepository.getOrganizationById(orgId).collect { organization ->
        if (organization == null) {
          _uiState.value = CreateEventUiState.Error("Organization not found")
          return@collect
        }

        // Build new Event from parsed data with real organization data
        val event =
            Event(
                title = parsed.title,
                description = parsed.description,
                organizerId = organization.id,
                organizerName = organization.name,
                status = EventStatus.DRAFT,
                location =
                    null, // TODO: Create Location object from location string (requires geocoding)
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
                images = emptyList(),
                tags = emptyList())

        // Set loading state
        _uiState.value = CreateEventUiState.Loading

        // Create event in repository
        val result = eventRepository.createEvent(event)

        result.fold(
            onSuccess = { eventId -> _uiState.value = CreateEventUiState.Success(eventId) },
            onFailure = { error ->
              _uiState.value = CreateEventUiState.Error(error.message ?: "Failed to create event")
            })
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
