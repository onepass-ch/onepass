package ch.onepass.onepass.ui.myevents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Search bar component for the marketplace.
 *
 * Allows searching for events and organizers with debounced search queries.
 *
 * @param query The current search query.
 * @param onQueryChange Callback when the search query changes.
 * @param onClear Callback when the search is cleared.
 * @param searchResults List of search results.
 * @param onResultSelected Callback when a search result is selected.
 * @param isSearching Whether a search is currently in progress.
 * @param modifier Modifier for styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    searchResults: List<SearchResult>,
    onResultSelected: (SearchResult) -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  // Automatically expand dropdown when results are available
  LaunchedEffect(searchResults, query) {
    if (query.isNotBlank()) {
      expanded = searchResults.isNotEmpty()
    }
  }

  Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { /* Controlled by results */},
        modifier = Modifier.fillMaxWidth()) {
          TextField(
              value = query,
              onValueChange = onQueryChange,
              modifier =
                  Modifier.fillMaxWidth()
                      .border(1.dp, colorScheme.onSurface, RoundedCornerShape(12.dp))
                      .heightIn(min = 50.dp)
                      .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                      .testTag(MyEventsTestTags.MARKET_SEARCH_BAR),
              placeholder = {
                Text(
                    "Search events or organizers...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface))
              },
              leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colorScheme.onSurface)
              },
              trailingIcon = {
                when {
                  isSearching -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colorScheme.primary,
                        strokeWidth = 2.dp)
                  }
                  query.isNotEmpty() -> {
                    IconButton(
                        onClick = {
                          onClear()
                          expanded = false
                        }) {
                          Icon(
                              imageVector = Icons.Default.Clear,
                              contentDescription = "Clear search",
                              tint = colorScheme.onSurface)
                        }
                  }
                }
              },
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorScheme.surface,
                      unfocusedContainerColor = colorScheme.surface,
                      focusedIndicatorColor = Color.Transparent,
                      unfocusedIndicatorColor = Color.Transparent,
                      focusedTextColor = colorScheme.onBackground,
                      unfocusedTextColor = colorScheme.onBackground),
              shape = RoundedCornerShape(12.dp),
              textStyle = MaterialTheme.typography.bodyMedium,
              singleLine = true)

          // Dropdown with search results
          ExposedDropdownMenu(
              expanded = expanded,
              onDismissRequest = { expanded = false },
              modifier = Modifier.fillMaxWidth().background(colorScheme.surface)) {
                searchResults.forEach { result ->
                  DropdownMenuItem(
                      text = { SearchResultItem(result = result) },
                      onClick = {
                        onResultSelected(result)
                        expanded = false
                      },
                      modifier = Modifier.fillMaxWidth())
                }
              }
        }
  }
}

/**
 * Individual search result item in the dropdown.
 *
 * @param result The search result to display.
 */
@Composable
private fun SearchResultItem(result: SearchResult) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start) {
        when (result) {
          is SearchResult.EventResult -> {
            // Event image thumbnail
            Box(
                modifier =
                    Modifier.size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surface),
                contentAlignment = Alignment.Center) {
                  if (result.event.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = result.event.imageUrl,
                        contentDescription = "Event image",
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Crop)
                  } else {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Event",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(24.dp))
                  }
                }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                  text = result.event.title,
                  style = MaterialTheme.typography.bodyMedium,
                  color = colorScheme.onBackground,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
              Text(
                  text = "Event • ${result.event.displayDateTime}",
                  style = MaterialTheme.typography.bodySmall,
                  color = colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
            }
          }
          is SearchResult.OrganizerResult -> {
            // Organizer profile image
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(colorScheme.surface),
                contentAlignment = Alignment.Center) {
                  if (!result.organizer.profileImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = result.organizer.profileImageUrl,
                        contentDescription = "Organizer image",
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop)
                  } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Organizer",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(24.dp))
                  }
                }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                  text = result.organizer.name,
                  style = MaterialTheme.typography.bodyMedium,
                  color = colorScheme.onBackground,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
              Text(
                  text = "Organizer${if (result.organizer.verified) " • Verified" else ""}",
                  style = MaterialTheme.typography.bodySmall,
                  color = colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
}
