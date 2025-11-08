package ch.onepass.onepass.ui.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationMember
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.organization.OrganizationRole
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for the Organization Dashboard screen.
 *
 * @property organization The organization being displayed, null if not loaded
 * @property events List of events belonging to the organization
 * @property staffMembers Map of user IDs to their organization member information
 * @property currentUserRole The current user's role in the organization
 * @property isLoading Whether data is currently being loaded
 * @property error Error message if loading failed, null otherwise
 */
data class OrganizationDashboardUiState(
    val organization: Organization? = null,
    val events: List<Event> = emptyList(),
    val staffMembers: Map<String, OrganizationMember> = emptyMap(),
    val currentUserRole: OrganizationRole? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the Organization Dashboard screen.
 *
 * Manages the state and business logic for displaying organization information, events, and staff
 * members. Handles role-based access control for UI elements.
 *
 * @property organizationRepository Repository for organization operations
 * @property eventRepository Repository for event operations
 * @property auth Firebase authentication instance
 */
class OrganizationDashboardViewModel(
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val eventRepository: EventRepository = EventRepositoryFirebase(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(OrganizationDashboardUiState())
  val uiState: StateFlow<OrganizationDashboardUiState> = _uiState.asStateFlow()

  private var currentOrganizationId: String? = null

  /**
   * Loads the organization and its associated data.
   *
   * @param organizationId The ID of the organization to load
   */
  fun loadOrganization(organizationId: String) {
    if (currentOrganizationId == organizationId && _uiState.value.organization != null) {
      // Already loaded this organization
      return
    }

    currentOrganizationId = organizationId
    _uiState.update { it.copy(isLoading = true, error = null) }

    viewModelScope.launch {
      try {
        // Load organization details
        organizationRepository
            .getOrganizationById(organizationId)
            .combine(eventRepository.getEventsByOrganization(organizationId)) { org, events ->
              Pair(org, events)
            }
            .collect { (organization, events) ->
              if (organization == null) {
                _uiState.update { it.copy(isLoading = false, error = "Organization not found") }
                return@collect
              }

              val currentUserId = auth.currentUser?.uid
              val currentUserRole = currentUserId?.let { organization.members[it]?.role }

              _uiState.update {
                it.copy(
                    organization = organization,
                    events = events,
                    staffMembers = organization.members,
                    currentUserRole = currentUserRole,
                    isLoading = false,
                    error = null)
              }
            }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isLoading = false, error = e.message ?: "Failed to load organization")
        }
      }
    }
  }

  /**
   * Removes a staff member from the organization.
   *
   * Only owners can remove staff members, and owners cannot be removed.
   *
   * @param userId The ID of the user to remove
   */
  fun removeStaffMember(userId: String) {
    val currentState = _uiState.value
    val organizationId = currentState.organization?.id ?: return

    // Verify current user is owner
    if (currentState.currentUserRole != OrganizationRole.OWNER) {
      return
    }

    // Prevent removing owner
    val memberToRemove = currentState.staffMembers[userId]
    if (memberToRemove?.role == OrganizationRole.OWNER) {
      return
    }

    viewModelScope.launch {
      try {
        organizationRepository
            .removeMember(organizationId, userId)
            .onSuccess {
              // Update local state
              _uiState.update {
                val updatedMembers = it.staffMembers.toMutableMap()
                updatedMembers.remove(userId)
                it.copy(staffMembers = updatedMembers)
              }
            }
            .onFailure { error ->
              // Could emit an error event here for showing a snackbar
              _uiState.update { it.copy(error = error.message ?: "Failed to remove staff member") }
            }
      } catch (e: Exception) {
        _uiState.update { it.copy(error = e.message ?: "Failed to remove staff member") }
      }
    }
  }

  /** Refreshes the organization data. */
  fun refresh() {
    currentOrganizationId?.let { loadOrganization(it) }
  }

  /** Clears any error state. */
  fun clearError() {
    _uiState.update { it.copy(error = null) }
  }
}
