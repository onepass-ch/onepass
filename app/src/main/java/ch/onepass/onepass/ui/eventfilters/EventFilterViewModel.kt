package ch.onepass.onepass.ui.eventfilters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.eventfilters.EventFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Shared ViewModel for managing event filters across multiple screens. */
class EventFilterViewModel : ViewModel() {
  private val _currentFilters = MutableStateFlow(EventFilters())
  val currentFilters: StateFlow<EventFilters> = _currentFilters

  /** Applies new filters and notifies observers. */
  fun applyFilters(filters: EventFilters) {
    viewModelScope.launch { _currentFilters.value = filters }
  }

  /** Clears all active filters. */
  fun clearFilters() {
    viewModelScope.launch { _currentFilters.value = EventFilters() }
  }
}
