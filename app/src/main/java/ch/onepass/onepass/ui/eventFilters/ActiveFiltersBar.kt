package ch.onepass.onepass.ui.eventFilters

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.model.eventFilters.EventFilters

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
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
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
                    "Date Range",
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
