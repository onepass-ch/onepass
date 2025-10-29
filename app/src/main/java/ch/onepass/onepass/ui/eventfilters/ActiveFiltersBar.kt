package ch.onepass.onepass.ui.eventfilters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.model.eventfilters.DateRangePresets
import ch.onepass.onepass.model.eventfilters.EventFilters
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        if (filters.hideSoldOut) {
          FilterChip(
              selected = true,
              onClick = { /* Could add individual removal later */},
              label = {
                Text(
                    "Available Only",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
              },
          )
        }
      }
      TextButton(
          onClick = onClearFilters,
          colors =
              ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
      ) {
        Text(
            "Clear All",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}

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

private fun getDateRangeDisplayText(dateRange: ClosedRange<Long>): String {
  val todayRange = DateRangePresets.getTodayRange()
  val next7Days = DateRangePresets.getNext7DaysRange()
  val weekend = DateRangePresets.getNextWeekendRange()

  return when (dateRange) {
    todayRange -> "Today"
    next7Days -> "Next 7 Days"
    weekend -> "Next Weekend"
    else -> formatDateRange(dateRange) ?: "Not set"
  }
}
