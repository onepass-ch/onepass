package ch.onepass.onepass.ui.eventform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.model.event.EventTag
import ch.onepass.onepass.ui.components.buttons.UploadImageButton
import ch.onepass.onepass.ui.components.forms.DatePickerField
import ch.onepass.onepass.ui.components.forms.LocationSearchField
import ch.onepass.onepass.ui.components.forms.TimePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleInputField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
  Column(
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
      horizontalAlignment = Alignment.Start,
      modifier = modifier.fillMaxWidth()) {
        FieldLabelWithCounter(
            label = "Title*",
            currentLength = value.length,
            maxLength = EventFormViewModel.MAX_TITLE_LENGTH,
            isError = value.length >= EventFormViewModel.MAX_TITLE_LENGTH)
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
              Text(
                  "Amazing event",
                  style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface))
            },
            modifier =
                Modifier.fillMaxWidth()
                    .border(1.dp, colorScheme.onSurface, RoundedCornerShape(10.dp))
                    .heightIn(min = 50.dp)
                    .testTag("title_input_field"),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colorScheme.onBackground,
                    unfocusedTextColor = colorScheme.onBackground,
                ),
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true)
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescriptionInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
      horizontalAlignment = Alignment.Start,
  ) {
    FieldLabelWithCounter(
        label = "Description*",
        currentLength = value.length,
        maxLength = EventFormViewModel.MAX_DESCRIPTION_LENGTH,
        isError = value.length >= EventFormViewModel.MAX_DESCRIPTION_LENGTH)

    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(122.dp)
                .border(1.dp, colorScheme.onSurface, RoundedCornerShape(10.dp))) {
          TextField(
              value = value,
              onValueChange = onValueChange,
              placeholder = {
                Text(
                    "This is amazing..",
                    style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface))
              },
              modifier = Modifier.fillMaxSize().testTag("description_input_field"),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorScheme.surface,
                      unfocusedContainerColor = colorScheme.surface,
                      focusedIndicatorColor = Color.Transparent,
                      unfocusedIndicatorColor = Color.Transparent,
                      focusedTextColor = colorScheme.onBackground,
                      unfocusedTextColor = colorScheme.onBackground,
                  ),
              shape = RoundedCornerShape(10.dp),
              textStyle = MaterialTheme.typography.bodyMedium,
              maxLines = 5)
        }
  }
}

@Composable
fun TimeInputField(
    startTimeValue: String,
    endTimeValue: String,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Date & time*",
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onBackground, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth())

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .background(colorScheme.surface, shape = RoundedCornerShape(10.dp))
                    .border(1.dp, colorScheme.onSurface, shape = RoundedCornerShape(10.dp))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.Start) {
                      Text(
                          text = "Start time",
                          style =
                              MaterialTheme.typography.bodyMedium.copy(
                                  color = colorScheme.onSurface))
                      Spacer(modifier = Modifier.height(4.dp))
                      TimePickerField(
                          value = startTimeValue,
                          onValueChange = onStartTimeChange,
                          modifier = Modifier.width(90.dp).height(60.dp))
                    }
                    Column(horizontalAlignment = Alignment.Start) {
                      Text(
                          text = "End time",
                          style =
                              MaterialTheme.typography.bodyMedium.copy(
                                  color = colorScheme.onSurface))
                      Spacer(modifier = Modifier.height(4.dp))
                      TimePickerField(
                          value = endTimeValue,
                          onValueChange = onEndTimeChange,
                          modifier = Modifier.width(90.dp).height(60.dp))
                    }
                  }
            }
      }
}

@Composable
fun DateInputField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
  DatePickerField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier.fillMaxWidth().heightIn(min = 50.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationInputField(
    value: String,
    onValueChange: (String) -> Unit,
    viewModel: EventFormViewModel,
    modifier: Modifier = Modifier
) {
  val locationResults by viewModel.locationSearchResults.collectAsState()
  val isSearching by viewModel.isSearchingLocation.collectAsState()

  Column(modifier = modifier) {
    LocationSearchField(
        query = value,
        onQueryChange = onValueChange,
        onLocationSelected = { location -> viewModel.selectLocation(location) },
        results = locationResults,
        isLoading = isSearching,
        modifier = Modifier.fillMaxWidth(),
        testTag = "location_input_field")
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsInputField(
    modifier: Modifier = Modifier,
    priceValue: String,
    capacityValue: String,
    onPriceChange: (String) -> Unit,
    onCapacityChange: (String) -> Unit,
    priceError: String? = null,
    capacityError: String? = null
) {
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
        text = "Tickets*",
        style =
            MaterialTheme.typography.bodyMedium.copy(
                color = colorScheme.onBackground, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top) {
          // Price Input
          Column(modifier = Modifier.weight(1f)) {
            CompactFieldLabel(
                label = "Price",
                currentLength = priceValue.length,
                maxLength = EventFormViewModel.MAX_PRICE_LENGTH,
                isError = priceError != null)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier.width(90.dp)
                        .height(50.dp)
                        .border(1.dp, colorScheme.onSurface, RoundedCornerShape(10.dp))) {
                  TextField(
                      value = priceValue,
                      onValueChange = onPriceChange,
                      placeholder = {
                        Text(
                            "ex: 12",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = colorScheme.onSurface))
                      },
                      modifier = Modifier.fillMaxSize().testTag("price_input_field"),
                      colors =
                          TextFieldDefaults.colors(
                              focusedContainerColor = colorScheme.surface,
                              unfocusedContainerColor = colorScheme.surface,
                              focusedIndicatorColor = Color.Transparent,
                              unfocusedIndicatorColor = Color.Transparent,
                              focusedTextColor = colorScheme.onBackground,
                              unfocusedTextColor = colorScheme.onBackground,
                          ),
                      shape = RoundedCornerShape(10.dp),
                      textStyle = MaterialTheme.typography.bodyMedium,
                      singleLine = true)
                }
            // Reserve space for error to prevent layout shift
            Box(modifier = Modifier.height(20.dp).padding(top = 4.dp)) {
              priceError?.let {
                Text(
                    text = it,
                    color = colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1)
              }
            }
          }

          // Capacity Input
          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            CompactFieldLabel(
                label = "Capacity",
                currentLength = capacityValue.length,
                maxLength = EventFormViewModel.MAX_CAPACITY_LENGTH,
                isError = capacityError != null)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier.width(90.dp)
                        .height(50.dp)
                        .border(1.dp, colorScheme.onSurface, RoundedCornerShape(10.dp))) {
                  TextField(
                      value = capacityValue,
                      onValueChange = onCapacityChange,
                      placeholder = {
                        Text(
                            "ex: 100",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = colorScheme.onSurface))
                      },
                      modifier = Modifier.fillMaxSize().testTag("capacity_input_field"),
                      colors =
                          TextFieldDefaults.colors(
                              focusedContainerColor = colorScheme.surface,
                              unfocusedContainerColor = colorScheme.surface,
                              focusedIndicatorColor = Color.Transparent,
                              unfocusedIndicatorColor = Color.Transparent,
                              focusedTextColor = colorScheme.onBackground,
                              unfocusedTextColor = colorScheme.onBackground,
                          ),
                      shape = RoundedCornerShape(10.dp),
                      textStyle = MaterialTheme.typography.bodyMedium,
                      singleLine = true)
                }
            // Reserve space for error to prevent layout shift
            Box(modifier = Modifier.height(20.dp).padding(top = 4.dp)) {
              capacityError?.let {
                Text(
                    text = it,
                    color = colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1)
              }
            }
          }
        }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSelectionSection(
    selectedTags: Set<EventTag>,
    onTagToggle: (EventTag) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = "Tags",
        style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onBackground),
        modifier = Modifier.padding(bottom = 8.dp))
    Text(
        text =
            "Select up to ${EventFormViewModel.MAX_TAG_COUNT} tags (${selectedTags.size}/${EventFormViewModel.MAX_TAG_COUNT} selected).",
        style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
        modifier = Modifier.padding(bottom = 16.dp))

    EventTag.categories.forEach { (header, tags) ->
      Text(
          text = header,
          style =
              MaterialTheme.typography.labelMedium.copy(
                  color = colorScheme.onBackground, fontWeight = FontWeight.Bold),
          modifier = Modifier.padding(vertical = 8.dp))
      FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()) {
            tags.forEach { tag ->
              val isSelected = selectedTags.contains(tag)
              FilterChip(
                  selected = isSelected,
                  onClick = { onTagToggle(tag) },
                  label = { Text(tag.displayValue) },
                  modifier =
                      Modifier.semantics {
                        contentDescription =
                            "${tag.displayValue} tag ${if(isSelected) "selected" else "not selected"}"
                      },
                  colors =
                      FilterChipDefaults.filterChipColors(
                          selectedContainerColor = colorScheme.primary,
                          selectedLabelColor = colorScheme.onBackground,
                          containerColor = colorScheme.surface,
                          labelColor = colorScheme.onSurface),
                  border =
                      FilterChipDefaults.filterChipBorder(
                          enabled = true,
                          selected = isSelected,
                          borderColor =
                              if (isSelected) Color.Transparent else colorScheme.onSurface))
            }
          }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

/**
 * Displays a form field label with a character counter.
 *
 * @param label The label text to display (e.g., "Title*", "Description*")
 * @param currentLength The current number of characters in the associated field
 * @param maxLength The maximum allowed number of characters
 * @param modifier The modifier to be applied to the root Row composable
 * @param isError Whether the field is in an error state, overriding length-based error display
 */
@Composable
fun FieldLabelWithCounter(
    label: String,
    currentLength: Int,
    maxLength: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onBackground))
        Text(
            text = "$currentLength/$maxLength characters",
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color =
                        if (isError || currentLength >= maxLength) colorScheme.error
                        else colorScheme.onSurface))
      }
}

/**
 * Compact character counter for fields with limited width. Displays counter above the field in a
 * stacked layout.
 *
 * @param label The field label (e.g., "Price", "Capacity")
 * @param currentLength Current character count
 * @param maxLength Maximum allowed characters
 * @param modifier Modifier to apply
 * @param isError Whether the field is in error state
 */
@Composable
fun CompactFieldLabel(
    label: String,
    currentLength: Int,
    maxLength: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(2.dp),
      horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onBackground))
        Text(
            text = "$currentLength/$maxLength characters",
            style =
                MaterialTheme.typography.labelSmall.copy(
                    color =
                        if (isError || currentLength >= maxLength) colorScheme.error
                        else colorScheme.onSurface,
                    fontSize = 10.sp))
      }
}

@Composable
fun EventFormFields(
    modifier: Modifier = Modifier,
    viewModel: EventFormViewModel,
    fieldTestTags: Map<String, String> = emptyMap()
) {
  val formState by viewModel.formState.collectAsState()
  val fieldErrors by viewModel.fieldErrors.collectAsState()

  Column(modifier = modifier) {
    Spacer(modifier = Modifier.height(8.dp))

    // Title
    TitleInputField(
        value = formState.title,
        onValueChange = { viewModel.updateTitle(it) },
        modifier = Modifier.testTag(fieldTestTags["title"] ?: ""))
    fieldErrors[EventFormViewModel.ValidationError.TITLE.key]?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Description
    DescriptionInputField(
        value = formState.description,
        onValueChange = { viewModel.updateDescription(it) },
        modifier = Modifier.testTag(fieldTestTags["description"] ?: ""))
    fieldErrors[EventFormViewModel.ValidationError.DESCRIPTION.key]?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Time Input
    TimeInputField(
        startTimeValue = formState.startTime,
        endTimeValue = formState.endTime,
        onStartTimeChange = { viewModel.updateStartTime(it) },
        onEndTimeChange = { viewModel.updateEndTime(it) },
        modifier = Modifier.testTag(fieldTestTags["time"] ?: ""))

    Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
      fieldErrors[EventFormViewModel.ValidationError.START_TIME.key]?.let {
        Text(
            text = it,
            color = colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 12.dp))
      }
      fieldErrors[EventFormViewModel.ValidationError.END_TIME.key]?.let {
        Text(text = it, color = colorScheme.error, style = MaterialTheme.typography.bodyMedium)
      }
    }
    fieldErrors[EventFormViewModel.ValidationError.TIME.key]?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Date Input
    DateInputField(
        value = formState.date,
        onValueChange = { viewModel.updateDate(it) },
        modifier = Modifier.testTag(fieldTestTags["date"] ?: ""))

    fieldErrors[EventFormViewModel.ValidationError.DATE.key]?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Location
    LocationInputField(
        value = formState.location,
        onValueChange = { viewModel.updateLocation(it) },
        viewModel = viewModel,
        modifier = Modifier.testTag(fieldTestTags["location"] ?: ""))
    fieldErrors[EventFormViewModel.ValidationError.LOCATION.key]?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Tickets
    TicketsInputField(
        priceValue = formState.price,
        capacityValue = formState.capacity,
        onPriceChange = { viewModel.updatePrice(it) },
        onCapacityChange = { viewModel.updateCapacity(it) },
        priceError = fieldErrors["price"],
        capacityError = fieldErrors["capacity"],
        modifier = Modifier.testTag(fieldTestTags["tickets"] ?: ""))

    Spacer(modifier = Modifier.height(16.dp))

    // Tags Selection
    TagsSelectionSection(
        selectedTags = formState.selectedTags,
        onTagToggle = { viewModel.toggleTag(it) },
        modifier = Modifier.testTag(fieldTestTags["tags_selection"] ?: ""))
    Spacer(modifier = Modifier.height(16.dp))

    // Upload Image Button
    UploadImageButton(
        onImageSelected = { uri -> viewModel.selectImage(uri) },
        enabled = true,
        imageDescription = "Event Image*",
        modifier = Modifier.testTag(fieldTestTags["image_upload"] ?: ""))

    // Display selected images count
    if (formState.selectedImageUris.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text = "${formState.selectedImageUris.size} image(s) selected",
          style = MaterialTheme.typography.bodyMedium,
          color = colorScheme.onBackground,
          modifier = Modifier.padding(start = 8.dp))
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}
