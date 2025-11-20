package ch.onepass.onepass.ui.components.forms

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                    color = Color(0xFFFFFFFF), textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth())

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
              // We control expansion manually based on results
              // expanded = !expanded
            },
            modifier = Modifier.fillMaxWidth()) {
              TextField(
                  value = query,
                  onValueChange = onQueryChange,
                  modifier =
                      Modifier.fillMaxWidth()
                          .border(1.dp, Color(0xFF404040), RoundedCornerShape(10.dp))
                          .heightIn(min = 50.dp)
                          .menuAnchor()
                          .testTag(testTag),
                  placeholder = {
                    Text(
                        "Type to search location",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                  },
                  trailingIcon = {
                    if (isLoading) {
                      CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                  },
                  colors =
                      TextFieldDefaults.colors(
                          focusedContainerColor = Color(0xFF1C1C1C),
                          unfocusedContainerColor = Color(0xFF1C1C1C),
                          focusedIndicatorColor = Color.Transparent,
                          unfocusedIndicatorColor = Color.Transparent,
                          focusedTextColor = Color.White,
                          unfocusedTextColor = Color.White,
                      ),
                  shape = RoundedCornerShape(10.dp),
                  textStyle = MaterialTheme.typography.bodySmall,
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
                                overflow = TextOverflow.Ellipsis)
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
