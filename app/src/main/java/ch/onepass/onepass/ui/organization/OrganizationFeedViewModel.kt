package ch.onepass.onepass.ui.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.membership.MembershipRepository
import ch.onepass.onepass.model.membership.MembershipRepositoryFirebase
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the organization feed screen.
 *
 * @property organizations List of organizations to display.
 * @property isLoading Whether data is currently being fetched.
 * @property error Error message if fetching failed, null otherwise.
 */
data class OrganizationFeedUIState(
    val organizations: List<Organization> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel for the organization feed screen.
 *
 * @property repository The organization repository for data operations.
 */
class OrganizationFeedViewModel(
    private val repository: OrganizationRepositoryFirebase = OrganizationRepositoryFirebase(),
    private val membershipRepository: MembershipRepository = MembershipRepositoryFirebase()
) : ViewModel() {

  private val _uiState = MutableStateFlow(OrganizationFeedUIState())
  val uiState: StateFlow<OrganizationFeedUIState> = _uiState.asStateFlow()

  /** Loads user's organizations from the repository. */
  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  fun loadUserOrganizations(userId: String) {
    _uiState.update { it.copy(isLoading = true, error = null) }

    viewModelScope.launch {
      try {
        val ownedOrgsFlow = repository.getOrganizationsByOwner(userId)

        val memberOrgsFlow =
            membershipRepository.getOrganizationsByUserFlow(userId).flatMapLatest { memberships ->
              if (memberships.isEmpty()) {
                flowOf(emptyList())
              } else {
                val orgIds = memberships.map { it.orgId }
                // Fetch all organizations concurrently
                val orgFlows = orgIds.map { repository.getOrganizationById(it) }
                combine(orgFlows) { orgs -> orgs.filterNotNull() }
              }
            }

        combine(ownedOrgsFlow, memberOrgsFlow) { ownedOrgs, memberOrgs ->
              (ownedOrgs + memberOrgs).distinctBy { it.id }.sortedByDescending { it.createdAt }
            }
            .collect { organizations ->
              _uiState.update {
                it.copy(organizations = organizations, isLoading = false, error = null)
              }
            }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isLoading = false, error = e.message ?: "Failed to load organizations")
        }
      }
    }
  }

  /** Refreshes the organizations list. */
  fun refreshOrganizations(userId: String) {
    loadUserOrganizations(userId)
  }
}
