package ch.onepass.onepass.ui.eventfilters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.eventfilters.EventFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


/** UI state for the filter dialog, including temporary selections. */
data class FilterUIState(
    val localFilters: EventFilters = EventFilters(),
    val expandedRegion: Boolean = false,
    val expandedDateRangePresets: Boolean = false,
    val showDatePicker: Boolean = false,
    val tempStartDate: Long? = null,
    val tempEndDate: Long? = null,
)

/**
 * Shared [ViewModel] to manage event filters both locally in the dialog
 * and globally across screens.
 */
class EventFilterViewModel : ViewModel() {

  /** Current UI state inside the filter dialog. */
  private val _uiState = MutableStateFlow(FilterUIState())
  val uiState: StateFlow<FilterUIState> = _uiState

  /** Global filters applied across the app. */
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

  /** Sets temporary start date for custom date range. */
  fun setTempStartDate(date: Long) {
    _uiState.value = _uiState.value.copy(tempStartDate = date, tempEndDate = null)
  }

  /** Sets temporary end date for custom date range, if valid. */
  fun setTempEndDate(date: Long) {
    val start = _uiState.value.tempStartDate
    if (start != null && date >= start) {
      _uiState.value = _uiState.value.copy(tempEndDate = date)
    }
  }

  /** Confirms selected date range and updates the local filters. */
  fun confirmDateRange() {
    val start = _uiState.value.tempStartDate
    val end = _uiState.value.tempEndDate
    if (start != null && end != null) {
      updateLocalFilters(_uiState.value.localFilters.copy(dateRange = start..end))
      toggleDatePicker(false)
      _uiState.value = _uiState.value.copy(tempStartDate = null, tempEndDate = null)
    }
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
