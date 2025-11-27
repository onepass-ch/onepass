package ch.onepass.onepass.ui.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.membership.MembershipRepository
import ch.onepass.onepass.model.membership.MembershipRepositoryFirebase
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for a single staff member.
 *
 * @property userId The unique ID of the staff member
 * @property role The role of the staff member in the organization
 * @property userProfile The user's profile information (name, email, avatar), null if loading
 * @property isLoading Whether the user profile is currently being fetched
 */
data class StaffMemberUiState(
    val userId: String,
    val role: OrganizationRole,
    val userProfile: StaffSearchResult? = null,
    val isLoading: Boolean = false
)

/**
 * UI state for the Organization Dashboard screen.
 *
 * @property organization The organization being displayed, null if not loaded
 * @property events List of events belonging to the organization
 * @property staffMembers List of staff members with their roles and profiles
 * @property currentUserRole The current user's role in the organization
 * @property isLoading Whether data is currently being loaded
 * @property error Error message if loading failed, null otherwise
 */
data class OrganizationDashboardUiState(
    val organization: Organization? = null,
    val events: List<Event> = emptyList(),
    val staffMembers: List<StaffMemberUiState> = emptyList(),
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
 * @property membershipRepository Repository for membership operations
 * @property userRepository Repository for user operations
 * @property auth Firebase authentication instance
 */
class OrganizationDashboardViewModel(
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val eventRepository: EventRepository = EventRepositoryFirebase(),
    private val membershipRepository: MembershipRepository = MembershipRepositoryFirebase(),
    private val userRepository: UserRepository = UserRepositoryFirebase(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(OrganizationDashboardUiState())
  val uiState: StateFlow<OrganizationDashboardUiState> = _uiState.asStateFlow()

  private var currentOrganizationId: String? = null
  private val fetchingUserIds = mutableSetOf<String>()

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
        // Load organization details, events, and members concurrently
        combine(
                organizationRepository.getOrganizationById(organizationId),
                eventRepository.getEventsByOrganization(organizationId),
                membershipRepository.getUsersByOrganizationFlow(organizationId)) {
                    org,
                    events,
                    memberships ->
                  Triple(org, events, memberships)
                }
            .collect { (organization, events, memberships) ->
              if (organization == null) {
                _uiState.update { it.copy(isLoading = false, error = "Organization not found") }
                return@collect
              }

              val currentUserId = auth.currentUser?.uid
              val currentUserRole = memberships.find { it.userId == currentUserId }?.role

              // Update staff members list
              val currentStaffList = _uiState.value.staffMembers
              val newStaffList =
                  memberships.map { membership ->
                    // Reuse existing profile if available
                    val existing = currentStaffList.find { it.userId == membership.userId }
                    StaffMemberUiState(
                        userId = membership.userId,
                        role = membership.role,
                        userProfile = existing?.userProfile,
                        isLoading = existing?.userProfile == null)
                  }

              _uiState.update {
                it.copy(
                    organization = organization,
                    events = events,
                    staffMembers = newStaffList,
                    currentUserRole = currentUserRole,
                    isLoading = false,
                    error = null)
              }

              // Fetch missing profiles
              newStaffList.forEach { staffMember ->
                if (staffMember.userProfile == null) {
                  fetchUserProfile(staffMember.userId)
                }
              }
            }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isLoading = false, error = e.message ?: "Failed to load organization")
        }
      }
    }
  }

  private fun fetchUserProfile(userId: String) {
    if (fetchingUserIds.contains(userId)) return
    fetchingUserIds.add(userId)

    viewModelScope.launch {
      try {
        userRepository.getUserById(userId).onSuccess { profile ->
          _uiState.update { state ->
            val updatedList =
                state.staffMembers.map { member ->
                  if (member.userId == userId) {
                    member.copy(userProfile = profile, isLoading = false)
                  } else {
                    member
                  }
                }
            state.copy(staffMembers = updatedList)
          }
        }
      } catch (e: Exception) {
        // If fetch fails, we might want to leave it as loading or set an error state per item
        // For now, we just stop loading indicator so it doesn't spin forever
        _uiState.update { state ->
          val updatedList =
              state.staffMembers.map { member ->
                if (member.userId == userId) {
                  member.copy(isLoading = false)
                } else {
                  member
                }
              }
          state.copy(staffMembers = updatedList)
        }
      } finally {
        fetchingUserIds.remove(userId)
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
    val memberToRemove = currentState.staffMembers.find { it.userId == userId }
    if (memberToRemove?.role == OrganizationRole.OWNER) {
      return
    }

    viewModelScope.launch {
      try {
        membershipRepository
            .removeMembership(userId, organizationId)
            .onSuccess {
              // Update local state
              _uiState.update {
                val updatedMembers = it.staffMembers.filter { member -> member.userId != userId }
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
