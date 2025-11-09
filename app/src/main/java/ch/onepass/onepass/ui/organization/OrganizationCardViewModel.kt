package ch.onepass.onepass.ui.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrganizerCardUiState(
    val isLoading: Boolean = true,
    val organizations: List<Organization> = emptyList(),
    val error: String? = null
)

open class OrganizerCardViewModel(
    private val repo: OrganizationRepositoryFirebase = OrganizationRepositoryFirebase()
) : ViewModel() {

  private val _ui = MutableStateFlow(OrganizerCardUiState())
  open val ui: StateFlow<OrganizerCardUiState> = _ui

  open fun load() {
    viewModelScope.launch {
      _ui.update { it.copy(isLoading = true, error = null) }
      repo
          .getVerifiedOrganizations()
          .catch { e ->
            _ui.update { it.copy(isLoading = false, error = e.message ?: "Failed to load") }
          }
          .collect { orgs ->
            _ui.value = OrganizerCardUiState(isLoading = false, organizations = orgs)
          }
    }
  }
}
