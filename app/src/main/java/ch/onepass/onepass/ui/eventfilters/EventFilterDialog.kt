package ch.onepass.onepass.ui.eventfilters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.model.eventfilters.DateRangePresets
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.eventfilters.SwissRegions
import java.util.*

/** Test tags for UI elements in the filter dialog. */
object FeedScreenTestTags {
  const val FILTER_DIALOG = "filterDialog"
  const val REGION_DROPDOWN = "regionDropdown"
  const val DATE_RANGE_PRESETS = "dateRangePresets"
  const val CUSTOM_RANGE_TEXT = "customRangeText"
  const val PICK_DATES_BUTTON = "pickDatesButton"
  const val HIDE_SOLD_OUT_CHECKBOX = "hideSoldOutCheckbox"
  const val APPLY_FILTERS_BUTTON = "applyFiltersButton"
  const val RESET_FILTERS_BUTTON = "resetFiltersButton"
}

/** Constants for inclusiveEndOfDay function */
const val END_OF_DAY_HOUR = 23
const val END_OF_DAY_MINUTE = 59
const val END_OF_DAY_SECOND = 59
const val END_OF_DAY_MILLISECOND = 999

/**
 * Full-screen dialog allowing users to select filters for events.
 *
 * @param viewModel [EventFilterViewModel] providing filter state.
 * @param onApply Callback invoked with updated [EventFilters].
 * @param onDismiss Callback invoked when the dialog is dismissed.
 */
@Composable
fun FilterDialog(
    viewModel: EventFilterViewModel = viewModel(),
    onApply: (EventFilters) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()
  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.9f).testTag(FeedScreenTestTags.FILTER_DIALOG),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
    ) {
      Column(Modifier.padding(24.dp)) {
        Text(
            text = "Filter Events",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Column(Modifier.verticalScroll(rememberScrollState()).weight(1f, false)) {
          RegionFilter(uiState, viewModel::updateLocalFilters, viewModel::toggleRegionDropdown)
          Spacer(Modifier.height(24.dp))
          DateRangeFilter(
              uiState = uiState,
              onFiltersChanged = viewModel::updateLocalFilters,
              onShowDatePickerChange = viewModel::toggleDatePicker,)
          Spacer(Modifier.height(24.dp))
          AvailabilityFilter(uiState.localFilters, viewModel::updateLocalFilters)
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
          TextButton(
              onClick = { viewModel.resetLocalFilters() },
              enabled = uiState.localFilters.hasActiveFilters,
              modifier = Modifier.testTag(FeedScreenTestTags.RESET_FILTERS_BUTTON),
          ) {
            Text("Reset All")
          }
          Button(
              onClick = { onApply(uiState.localFilters) },
              enabled = uiState.localFilters != viewModel.currentFilters.collectAsState().value,
              modifier = Modifier.testTag(FeedScreenTestTags.APPLY_FILTERS_BUTTON),
          ) {
            Text("Apply Filters")
          }
        }
      }
    }
  }
}

/** Displays a dropdown menu for selecting the event region. */
@Composable
private fun RegionFilter(
    uiState: FilterUIState = FilterUIState(),
    onFiltersChanged: (EventFilters) -> Unit = {},
    onExpandedChange: (Boolean) -> Unit = {},
) {
  FilterSection("Region") {
    Box(Modifier.fillMaxWidth()) {
      OutlinedButton(
          onClick = { onExpandedChange(true) },
          modifier = Modifier.fillMaxWidth().testTag(FeedScreenTestTags.REGION_DROPDOWN),
      ) {
        Text(uiState.localFilters.region ?: SwissRegions.ALL_REGIONS, Modifier.weight(1f))
        Icon(Icons.Default.ArrowDropDown, "Select region")
      }
      DropdownMenu(
          expanded = uiState.expandedRegion,
          onDismissRequest = { onExpandedChange(false) },
          modifier = Modifier.fillMaxWidth(0.8f),
      ) {
        DropdownMenuItem(
            text = { Text(SwissRegions.ALL_REGIONS) },
            onClick = {
              onFiltersChanged(uiState.localFilters.copy(region = null))
              onExpandedChange(false)
            },
        )
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        SwissRegions.REGIONS.forEach { region ->
          DropdownMenuItem(
              text = { Text(region) },
              onClick = {
                onFiltersChanged(uiState.localFilters.copy(region = region))
                onExpandedChange(false)
              },
          )
        }
      }
    }
  }
}

/** Displays chips for selecting a date range or custom range via date picker. */
@Composable
private fun DateRangeFilter(
    uiState: FilterUIState = FilterUIState(),
    onFiltersChanged: (EventFilters) -> Unit = {},
    onShowDatePickerChange: (Boolean) -> Unit = {},
) {
  val datePresets =
      listOf(
          "Today" to DateRangePresets.getTodayRange(),
          "Next Weekend" to DateRangePresets.getNextWeekendRange(),
          "Next 7 Days" to DateRangePresets.getNext7DaysRange(),
      )
  FilterSection("Date Range") {
    Column(Modifier.testTag(FeedScreenTestTags.DATE_RANGE_PRESETS)) {
      Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
      ) {
        datePresets.forEach { (label, presetRange) ->
          val isSelected = uiState.localFilters.dateRange == presetRange
          FilterChip(
              selected = isSelected,
              onClick = {
                onFiltersChanged(
                    uiState.localFilters.copy(dateRange = if (isSelected) null else presetRange))
              },
              label = { Text(label) },
              modifier = Modifier.weight(1f),
          )
        }
      }
      Spacer(Modifier.height(16.dp))
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          Text("Custom range", style = MaterialTheme.typography.labelMedium)
          Text(
              formatDateRange(uiState.localFilters.dateRange) ?: "Not set",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag(FeedScreenTestTags.CUSTOM_RANGE_TEXT))
        }
        Button(
            onClick = { onShowDatePickerChange(true) },
            Modifier.height(36.dp).testTag(FeedScreenTestTags.PICK_DATES_BUTTON)) {
              Text("Pick dates")
            }
      }
      if (uiState.showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { onShowDatePickerChange(false) },
            onConfirm = { start, end ->
                onFiltersChanged(uiState.localFilters.copy(dateRange = start..end))
                onShowDatePickerChange(false)
            }
        )
      }
    }
  }
}

/** Shows a checkbox to hide sold out events. */
@Composable
private fun AvailabilityFilter(filters: EventFilters, onFiltersChanged: (EventFilters) -> Unit) {
  FilterSection("Availability") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
      Checkbox(
          checked = filters.hideSoldOut,
          onCheckedChange = { onFiltersChanged(filters.copy(hideSoldOut = it)) },
          modifier = Modifier.testTag(FeedScreenTestTags.HIDE_SOLD_OUT_CHECKBOX),
      )
      Text("Hide sold out events", Modifier.padding(start = 8.dp))
    }
  }
}

/** Section wrapper with a title and content. */
@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
  Column(Modifier.fillMaxWidth()) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp),
    )
    content()
  }
}

/** Dialog to pick a custom start and end date for filtering events. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit = {},
    onConfirm: (Long, Long) -> Unit = { _, _ -> },
) {
  val state = rememberDateRangePickerState()

  AlertDialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
      title = { Text("When ?") },
      text = {
        Column {
          DateRangePicker(
              title = {},
              headline = {
                val start = state.selectedStartDateMillis
                val end = state.selectedEndDateMillis

                val headlineText =
                    when {
                      start == null -> "Select start date"
                      end == null -> "Select end date"
                      else -> formatDateRange(start..end)
                    }

                Text(
                    text = headlineText!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp))
              },
              state = state,
          )
        }
      },
      confirmButton = {
        if (state.selectedStartDateMillis != null && state.selectedEndDateMillis != null) {
          Button(
              onClick = {
                val range =
                    (state.selectedStartDateMillis!!..state.selectedEndDateMillis!!)
                        .inclusiveEndOfDay()
                  onConfirm(range.start, range.endInclusive)
              }) {
                Text("Confirm")
              }
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/** Extends a [ClosedRange] to include the full end-of-day time for the end date. */
fun ClosedRange<Long>.inclusiveEndOfDay(): ClosedRange<Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = this@inclusiveEndOfDay.endInclusive }
    cal.set(Calendar.HOUR_OF_DAY, END_OF_DAY_HOUR)
    cal.set(Calendar.MINUTE, END_OF_DAY_MINUTE)
    cal.set(Calendar.SECOND, END_OF_DAY_SECOND)
    cal.set(Calendar.MILLISECOND, END_OF_DAY_MILLISECOND)
    return this.start..cal.timeInMillis
}
