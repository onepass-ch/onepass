package ch.onepass.onepass.ui.eventfilters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.eventfilters.EventFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FilterUIState(
    val localFilters: EventFilters = EventFilters(),
    val expandedRegion: Boolean = false,
    val expandedDateRangePresets: Boolean = false,
    val showDatePicker: Boolean = false,
)

/** Shared ViewModel for managing event filters across multiple screens. */
class EventFilterViewModel : ViewModel() {

  // --- UI state ---
  private val _uiState = MutableStateFlow(FilterUIState())
  val uiState: StateFlow<FilterUIState> = _uiState

  // --- Business-level filters (used across screens) ---
  private val _currentFilters = MutableStateFlow(EventFilters())
  val currentFilters: StateFlow<EventFilters> = _currentFilters

  /** Update only the local filters in the dialog. */
  fun updateLocalFilters(newFilters: EventFilters) {
    _uiState.value = _uiState.value.copy(localFilters = newFilters)
  }

  /** Toggle region dropdown visibility. */
  fun toggleRegionDropdown(expanded: Boolean) {
    _uiState.value = _uiState.value.copy(expandedRegion = expanded)
  }

  /** Toggle date picker visibility. */
  fun toggleDatePicker(visible: Boolean) {
    _uiState.value = _uiState.value.copy(showDatePicker = visible)
  }

  /** Reset the local filters (inside the dialog). */
  fun resetLocalFilters() {
    _uiState.value = _uiState.value.copy(localFilters = EventFilters())
  }

  /** Apply new filters globally. */
  fun applyFilters(filters: EventFilters) {
    viewModelScope.launch { _currentFilters.value = filters }
  }

  /** Clears all active filters globally. */
  fun clearFilters() {
    viewModelScope.launch {
      _currentFilters.value = EventFilters()
      _uiState.value = FilterUIState(localFilters = EventFilters())
    }
  }
}
