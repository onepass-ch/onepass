package ch.onepass.onepass.ui.components.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays a form field label with a character counter (for full-width fields).
 *
 * @param label The label text (e.g., "Title*", "Description*")
 * @param currentLength Current character count
 * @param maxLength Maximum allowed characters
 * @param modifier Modifier for the root Row
 * @param isError Whether to show error color regardless of length
 * @param testTag Test tag for the counter Text
 */
@Composable
fun FieldLabelWithCounter(
    label: String,
    currentLength: Int,
    maxLength: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    testTag: String? = null
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onBackground))
        Text(
            text = "$currentLength/$maxLength characters",
            style =
                MaterialTheme.typography.bodySmall.copy(
                    color =
                        if (isError || currentLength >= maxLength) colorScheme.error
                        else colorScheme.onSurfaceVariant),
            modifier = Modifier.testTag(testTag ?: ""))
      }
}

/**
 * Compact character counter for narrow fields (e.g., price/capacity).
 *
 * @param label Field label (e.g., "Price", "Capacity")
 * @param currentLength Current character count
 * @param maxLength Maximum allowed characters
 * @param modifier Modifier for the root Column
 * @param isError Whether to show error color
 * @param testTag Test tag for the counter Text
 */
@Composable
fun CompactFieldLabel(
    label: String,
    currentLength: Int,
    maxLength: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    testTag: String? = null
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(2.dp),
      horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodySmall.copy(color = colorScheme.onBackground))
        Text(
            text = "$currentLength/$maxLength",
            style =
                MaterialTheme.typography.labelSmall.copy(
                    color =
                        if (isError || currentLength >= maxLength) colorScheme.error
                        else colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium),
            modifier = Modifier.testTag(testTag ?: ""))
      }
}
