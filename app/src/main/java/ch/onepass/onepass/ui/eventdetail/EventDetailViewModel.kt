package ch.onepass.onepass.ui.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.repository.RepositoryProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** ViewModel for EventDetailScreen that manages event and organizer data. */
class EventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository = RepositoryProvider.eventRepository,
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase()
) : ViewModel() {

  private val _event = MutableStateFlow<Event?>(null)
  val event: StateFlow<Event?> = _event.asStateFlow()

  private val _organization = MutableStateFlow<Organization?>(null)
  val organization: StateFlow<Organization?> = _organization.asStateFlow()

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  init {
    loadEvent()
  }

  private fun loadEvent() {
    viewModelScope.launch {
      eventRepository
          .getEventById(eventId)
          .catch { e ->
            _error.value = e.message ?: "Failed to load event"
            _isLoading.value = false
          }
          .collect { event ->
            _event.value = event
            if (event != null) {
              loadOrganization(event.organizerId)
            } else {
              _isLoading.value = false
            }
          }
    }
  }

  private fun loadOrganization(organizerId: String) {
    if (organizerId.isBlank()) {
      _isLoading.value = false
      return
    }

    viewModelScope.launch {
      organizationRepository
          .getOrganizationById(organizerId)
          .catch { e ->
            // Don't set error for organization, just log it
            _isLoading.value = false
          }
          .collect { organization ->
            _organization.value = organization
            _isLoading.value = false
          }
    }
  }
}
