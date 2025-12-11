package ch.onepass.onepass.ui.eventfilters

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.model.eventfilters.TagCategories

/** Test tags for tag filter UI */
object TagFilterTestTags {
  const val TAG_FILTER_SECTION = "tagFilterSection"
  const val TAG_CATEGORY_SECTION = "tagCategorySection"
  const val TAG_CHIP = "tagChip"
}

/**
 * Composable for tag filtering with categorized sections.
 *
 * Displays all available tags grouped by category with interactive chips.
 *
 * @param selectedTags Currently selected tags
 * @param onTagSelectionChange Callback invoked when tag selection changes
 * @param modifier Modifier for the component
 */
@Composable
fun TagFilter(
    selectedTags: Set<String>,
    onTagSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
  FilterSection("Tags") {
    Column(modifier = modifier.fillMaxWidth().testTag(TagFilterTestTags.TAG_FILTER_SECTION)) {
      TagCategories.ALL_CATEGORIES.forEach { category ->
        TagCategorySection(
            category = category,
            selectedTags = selectedTags,
            onTagSelectionChange = onTagSelectionChange)
        Spacer(Modifier.height(16.dp))
      }
    }
  }
}

/**
 * Individual tag category section with header.
 *
 * Displays a category title and all tags in that category as selectable chips.
 *
 * @param category Name of the category to display
 * @param selectedTags Currently selected tags across all categories
 * @param onTagSelectionChange Callback invoked when tag selection changes
 * @param modifier Modifier for the section
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCategorySection(
    category: String,
    selectedTags: Set<String>,
    onTagSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .testTag("${TagFilterTestTags.TAG_CATEGORY_SECTION}_${category}")) {
        // Category header
        Text(
            text = category,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp))

        // Tags in this category as chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()) {
              TagCategories.getTagsByCategory(category).forEach { tagDisplayValue ->
                FilterChip(
                    selected = selectedTags.contains(tagDisplayValue),
                    onClick = {
                      val updated = selectedTags.toMutableSet()
                      if (selectedTags.contains(tagDisplayValue)) {
                        updated.remove(tagDisplayValue)
                      } else {
                        updated.add(tagDisplayValue)
                      }
                      onTagSelectionChange(updated)
                    },
                    label = { Text(tagDisplayValue) },
                    modifier = Modifier.testTag("${TagFilterTestTags.TAG_CHIP}_${tagDisplayValue}"))
              }
            }
      }
}
