package ch.onepass.onepass.ui.components.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.model.map.Location

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onLocationSelected: (Location) -> Unit,
    results: List<Location>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "location_search_field"
) {
  var expanded by remember { mutableStateOf(false) }

  // Automatically expand when results are available and query is not blank
  // and user is not just clicking a result
  LaunchedEffect(results, query) {
    if (query.isNotBlank()) {
      expanded = results.isNotEmpty()
    }
  }

  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Location*",
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onBackground, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth())

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
              // We control expansion manually based on results
              // expanded = !expanded
            },
            modifier = Modifier.fillMaxWidth()) {
              Column(
                  modifier =
                      Modifier.fillMaxWidth()
                          .border(1.dp, colorScheme.onSurface, RoundedCornerShape(10.dp))
                          .background(colorScheme.surface, RoundedCornerShape(10.dp))
                          .padding(horizontal = 16.dp, vertical = 8.dp)) {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = {
                          Text(
                              "Type to search location",
                              style =
                                  MaterialTheme.typography.bodySmall.copy(
                                      color = colorScheme.onSurface))
                        },
                        trailingIcon = {
                          if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .heightIn(min = 30.dp)
                                .menuAnchor()
                                .testTag(testTag),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = colorScheme.surface,
                                unfocusedContainerColor = colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = colorScheme.onBackground,
                                unfocusedTextColor = colorScheme.onBackground),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true)

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()) {
                          results.forEach { location ->
                            DropdownMenuItem(
                                text = {
                                  Text(
                                      text = location.name,
                                      maxLines = 2,
                                      overflow = TextOverflow.Ellipsis,
                                      color = colorScheme.onBackground)
                                },
                                onClick = {
                                  onLocationSelected(location)
                                  expanded = false
                                },
                                modifier = Modifier.fillMaxWidth())
                          }
                        }
                  }
            }
      }
}
