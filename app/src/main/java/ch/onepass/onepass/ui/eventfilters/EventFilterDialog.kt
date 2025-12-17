package ch.onepass.onepass.ui.eventfilters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.model.eventfilters.DateRangePresets
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.eventfilters.SwissRegions
import ch.onepass.onepass.model.eventfilters.TagFilter
import java.util.Calendar

/** Test tags for UI elements in the filter dialog. */
object EventFilterDialogTestTags {
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
        modifier =
            Modifier.fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .testTag(EventFilterDialogTestTags.FILTER_DIALOG),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
    ) {
      Column(Modifier.padding(24.dp)) {
        Text(
            text = stringResource(R.string.filter_dialog_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
              item {
                RegionFilter(
                    uiState, viewModel::updateLocalFilters, viewModel::toggleRegionDropdown)
              }

              item {
                DateRangeFilter(
                    uiState = uiState,
                    onFiltersChanged = viewModel::updateLocalFilters,
                    onShowDatePickerChange = viewModel::toggleDatePicker,
                )
              }

              item {
                TagFilter(
                    selectedTags = uiState.localFilters.selectedTags,
                    onTagSelectionChange = { tags ->
                      viewModel.updateLocalFilters(uiState.localFilters.copy(selectedTags = tags))
                    })
              }

              item { AvailabilityFilter(uiState.localFilters, viewModel::updateLocalFilters) }
            }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
          TextButton(
              onClick = { viewModel.resetLocalFilters() },
              enabled = uiState.localFilters.hasActiveFilters,
              modifier = Modifier.testTag(EventFilterDialogTestTags.RESET_FILTERS_BUTTON),
          ) {
            Text(stringResource(R.string.filter_dialog_reset_all), color = colorScheme.onBackground)
          }
          Button(
              onClick = { onApply(uiState.localFilters) },
              enabled = uiState.localFilters != viewModel.currentFilters.collectAsState().value,
              modifier = Modifier.testTag(EventFilterDialogTestTags.APPLY_FILTERS_BUTTON),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = colorScheme.primary,
                      contentColor = colorScheme.onBackground)) {
                Text(stringResource(R.string.filter_dialog_apply))
              }
        }
      }
    }
  }
}

/**
 * Displays a dropdown menu for selecting the event region.
 *
 * @param uiState The current UI state containing local filters and dropdown expansion state.
 * @param onFiltersChanged Callback to update the local [EventFilters] state.
 * @param onExpandedChange Callback to toggle the expansion state of the region dropdown.
 */
@Composable
private fun RegionFilter(
    uiState: FilterUIState = FilterUIState(),
    onFiltersChanged: (EventFilters) -> Unit = {},
    onExpandedChange: (Boolean) -> Unit = {},
) {
  FilterSection(stringResource(R.string.filter_dialog_region_title)) {
    Box(Modifier.fillMaxWidth()) {
      OutlinedButton(
          onClick = { onExpandedChange(true) },
          modifier = Modifier.fillMaxWidth().testTag(EventFilterDialogTestTags.REGION_DROPDOWN),
      ) {
        Text(
            text = uiState.localFilters.region ?: SwissRegions.ALL_REGIONS,
            modifier = Modifier.weight(1f),
            color = colorScheme.onBackground)
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = stringResource(R.string.filter_dialog_region_select_description),
            tint = colorScheme.onBackground)
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

/**
 * Displays chips for selecting a date range or custom range via date picker.
 *
 * @param uiState The current UI state containing local filters and date picker visibility.
 * @param onFiltersChanged Callback to update the local [EventFilters] state.
 * @param onShowDatePickerChange Callback to toggle the visibility of the date range picker dialog.
 */
@Composable
private fun DateRangeFilter(
    uiState: FilterUIState = FilterUIState(),
    onFiltersChanged: (EventFilters) -> Unit = {},
    onShowDatePickerChange: (Boolean) -> Unit = {},
) {
  val datePresets =
      listOf(
          stringResource(R.string.filter_dialog_date_today) to DateRangePresets.getTodayRange(),
          stringResource(R.string.filter_dialog_date_next_weekend) to
              DateRangePresets.getNextWeekendRange(),
          stringResource(R.string.filter_dialog_date_next_7_days) to
              DateRangePresets.getNext7DaysRange(),
      )
  FilterSection(stringResource(R.string.filter_dialog_date_range_title)) {
    Column(Modifier.testTag(EventFilterDialogTestTags.DATE_RANGE_PRESETS)) {
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
              label = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Text(label)
                }
              },
              modifier = Modifier.weight(1f).height(45.dp))
        }
      }
      Spacer(Modifier.height(16.dp))
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          Text(
              stringResource(R.string.filter_dialog_custom_range_label),
              style = MaterialTheme.typography.labelMedium)
          Text(
              formatDateRange(uiState.localFilters.dateRange)
                  ?: stringResource(R.string.filter_dialog_not_set),
              style = MaterialTheme.typography.bodySmall,
              color = colorScheme.onBackground,
              modifier = Modifier.testTag(EventFilterDialogTestTags.CUSTOM_RANGE_TEXT))
        }
        Button(
            onClick = { onShowDatePickerChange(true) },
            Modifier.height(36.dp).testTag(EventFilterDialogTestTags.PICK_DATES_BUTTON),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onBackground)) {
              Text(
                  text = stringResource(R.string.filter_dialog_pick_dates),
                  color = colorScheme.onBackground)
            }
      }
      if (uiState.showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { onShowDatePickerChange(false) },
            onConfirm = { start, end ->
              onFiltersChanged(uiState.localFilters.copy(dateRange = start..end))
              onShowDatePickerChange(false)
            })
      }
    }
  }
}

/**
 * Shows a checkbox to hide sold out events.
 *
 * @param filters The current local [EventFilters] state.
 * @param onFiltersChanged Callback to update the local [EventFilters] state when the checkbox is
 *   toggled.
 */
@Composable
private fun AvailabilityFilter(filters: EventFilters, onFiltersChanged: (EventFilters) -> Unit) {
  FilterSection(stringResource(R.string.filter_dialog_availability_title)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
      Checkbox(
          checked = filters.hideSoldOut,
          onCheckedChange = { onFiltersChanged(filters.copy(hideSoldOut = it)) },
          modifier = Modifier.testTag(EventFilterDialogTestTags.HIDE_SOLD_OUT_CHECKBOX),
      )
      Text(stringResource(R.string.filter_dialog_hide_sold_out), Modifier.padding(start = 8.dp))
    }
  }
}

/**
 * Section wrapper with a title and content.
 *
 * @param title The title of the filter section.
 * @param content The composable content displayed within the section.
 */
@Composable
fun FilterSection(title: String, content: @Composable () -> Unit) {
  Column(Modifier.fillMaxWidth()) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp),
    )
    content()
  }
}

/**
 * Dialog to pick a custom start and end date for filtering events.
 *
 * @param onDismiss Callback invoked when the dialog is dismissed (e.g., via Cancel button or
 *   outside click).
 * @param onConfirm Callback invoked when the date range is confirmed, receives start and end date
 *   as timestamps (Long).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit = {},
    onConfirm: (Long, Long) -> Unit = { _, _ -> },
) {
  val state = rememberDateRangePickerState()

  AlertDialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
      title = { Text(stringResource(R.string.filter_dialog_date_picker_title)) },
      text = {
        Column {
          DateRangePicker(
              title = {},
              headline = {
                val start = state.selectedStartDateMillis
                val end = state.selectedEndDateMillis

                val headlineText =
                    when {
                      start == null -> stringResource(R.string.filter_dialog_select_start_date)
                      end == null -> stringResource(R.string.filter_dialog_select_end_date)
                      else -> formatDateRange(start..end)
                    }

                Text(
                    text = headlineText!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp))
              },
              state = state,
              colors =
                  DatePickerDefaults.colors(
                      containerColor = colorScheme.surface,

                      // Header / title
                      headlineContentColor = colorScheme.primary,
                      titleContentColor = colorScheme.onBackground,

                      // Weekdays + labels
                      weekdayContentColor = colorScheme.onBackground.copy(alpha = 0.7f),
                      subheadContentColor = colorScheme.onBackground.copy(alpha = 0.7f),

                      // Days
                      dayContentColor = colorScheme.onBackground,
                      selectedDayContainerColor = colorScheme.primary,
                      selectedDayContentColor = colorScheme.onBackground,

                      // Today indicator
                      todayDateBorderColor = colorScheme.primary,
                      todayContentColor = colorScheme.primary))
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
              },
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = colorScheme.primary,
                      contentColor = colorScheme.onBackground)) {
                Text(
                    stringResource(R.string.filter_dialog_confirm),
                    color = colorScheme.onBackground)
              }
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) {
          Text(stringResource(R.string.filter_dialog_cancel), color = colorScheme.onBackground)
        }
      })
}

/**
 * Extends a [ClosedRange] of milliseconds to include the full end-of-day time for the end date.
 *
 * This ensures that events scheduled throughout the selected end day are included in the filter.
 *
 * @return A new [ClosedRange] where the start is the original start and the end is set to
 *   23:59:59.999 on the original end date.
 * @receiver The original [ClosedRange] of milliseconds.
 */
fun ClosedRange<Long>.inclusiveEndOfDay(): ClosedRange<Long> {
  val cal = Calendar.getInstance().apply { timeInMillis = this@inclusiveEndOfDay.endInclusive }
  cal.set(Calendar.HOUR_OF_DAY, END_OF_DAY_HOUR)
  cal.set(Calendar.MINUTE, END_OF_DAY_MINUTE)
  cal.set(Calendar.SECOND, END_OF_DAY_SECOND)
  cal.set(Calendar.MILLISECOND, END_OF_DAY_MILLISECOND)
  return this.start..cal.timeInMillis
}
