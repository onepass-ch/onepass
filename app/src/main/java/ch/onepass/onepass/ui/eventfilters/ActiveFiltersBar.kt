package ch.onepass.onepass.ui.eventfilters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.eventfilters.DateRangePresets
import ch.onepass.onepass.model.eventfilters.EventFilters
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ActiveFiltersConfig {
  const val MAX_VISIBLE_TAGS = 3
}

/**
 * Displays the currently active event filters as chips with an option to clear all.
 *
 * @param filters The current [EventFilters] applied.
 * @param onClearFilters Callback invoked when "Clear All" is pressed.
 * @param modifier Optional [Modifier] for styling.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveFiltersBar(
    filters: EventFilters,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.small,
      tonalElevation = 4.dp,
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
      ) {
        filters.region?.let { region ->
          FilterChip(
              selected = true,
              onClick = { /* Could add individual removal later */},
              label = {
                Text(
                    region,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
              },
          )
        }
        filters.dateRange?.let {
          FilterChip(
              selected = true,
              onClick = { /* Could add individual removal later */},
              label = {
                Text(
                    text = getDateRangeDisplayText(filters.dateRange),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
              },
          )
        }
        filters.selectedTags.take(ActiveFiltersConfig.MAX_VISIBLE_TAGS).forEach { tag ->
          FilterChip(
              selected = true,
              onClick = { /* Could add individual removal later */},
              label = {
                Text(
                    tag,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
              },
          )
        }
        // Show count chip if more than ActiveFiltersConfig.MAX_VISIBLE_TAGS tags
        if (filters.selectedTags.size > ActiveFiltersConfig.MAX_VISIBLE_TAGS) {
          FilterChip(
              selected = true,
              onClick = { /* Could add individual removal later */},
              label = {
                Text(
                    stringResource(
                        R.string.filters_more_tags,
                        filters.selectedTags.size - ActiveFiltersConfig.MAX_VISIBLE_TAGS),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
              },
          )
        }
        if (filters.hideSoldOut) {
          FilterChip(
              selected = true,
              onClick = { /* Could add individual removal later */},
              label = {
                Text(
                    stringResource(R.string.filters_available_only),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
              },
          )
        }
      }
      TextButton(
          onClick = onClearFilters,
          colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.primary),
      ) {
        Text(
            stringResource(R.string.filters_clear_all),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}

/** Formats a date range as a human-readable string. */
internal fun formatDateRange(dateRange: ClosedRange<Long>?): String? {
  return dateRange?.let {
    val startDate =
        Instant.ofEpochMilli(it.start)
            .atZone(ZoneId.systemDefault()) // Use user's timezone
            .toLocalDate()
    val endDate = Instant.ofEpochMilli(it.endInclusive).atZone(ZoneId.systemDefault()).toLocalDate()

    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    "${startDate.format(formatter)} - ${endDate.format(formatter)}"
  }
}

/** Returns a display text for common date ranges like "Today" or "Next 7 Days". */
@Composable
private fun getDateRangeDisplayText(dateRange: ClosedRange<Long>): String {
  val todayRange = DateRangePresets.getTodayRange()
  val next7Days = DateRangePresets.getNext7DaysRange()
  val weekend = DateRangePresets.getNextWeekendRange()

  return when (dateRange) {
    todayRange -> stringResource(R.string.filters_date_today)
    next7Days -> stringResource(R.string.filters_date_next_7_days)
    weekend -> stringResource(R.string.filters_date_next_weekend)
    else -> formatDateRange(dateRange) ?: stringResource(R.string.filters_date_not_set)
  }
}
