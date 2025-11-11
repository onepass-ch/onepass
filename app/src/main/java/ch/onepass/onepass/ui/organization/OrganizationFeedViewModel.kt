package ch.onepass.onepass.ui.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import kotlinx.coroutines.flow.*
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
) : ViewModel() {

  private val _uiState = MutableStateFlow(OrganizationFeedUIState())
  val uiState: StateFlow<OrganizationFeedUIState> = _uiState.asStateFlow()

  /** Loads user's organizations from the repository. */
  fun loadUserOrganizations(userId: String) {
    _uiState.update { it.copy(isLoading = true, error = null) }

    viewModelScope.launch {
      try {
        combine(
                repository.getOrganizationsByOwner(userId),
                repository.getOrganizationsByMember(userId)) { ownedOrgs, memberOrgs ->
                  (ownedOrgs + memberOrgs).distinctBy { it.id }
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
