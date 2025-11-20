package ch.onepass.onepass.ui.eventform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        Text(
            text = "Title*",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFFFFFFF)))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
              Text(
                  "Amazing event",
                  style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
            },
            modifier =
                Modifier.fillMaxWidth()
                    .border(1.dp, Color(0xFF404040), RoundedCornerShape(10.dp))
                    .heightIn(min = 50.dp)
                    .testTag("title_input_field"),
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
    Text(
        text = "Description*",
        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFFFFFFF)))

    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(122.dp)
                .border(1.dp, Color(0xFF404040), RoundedCornerShape(10.dp))) {
          TextField(
              value = value,
              onValueChange = onValueChange,
              placeholder = {
                Text(
                    "This is amazing..",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
              },
              modifier = Modifier.fillMaxSize().testTag("description_input_field"),
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
                    color = Color(0xFFFFFFFF), textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth())

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .background(Color(0xFF1C1C1C), shape = RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF404040), shape = RoundedCornerShape(10.dp))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.Start) {
                      Text(
                          text = "Start time",
                          style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                      Spacer(modifier = Modifier.height(4.dp))
                      TimePickerField(
                          value = startTimeValue,
                          onValueChange = onStartTimeChange,
                          modifier = Modifier.width(90.dp).height(60.dp))
                    }
                    Column(horizontalAlignment = Alignment.Start) {
                      Text(
                          text = "End time",
                          style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
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
                color = Color(0xFFFFFFFF), textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top) {
          // Price Input
          Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Price",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier.width(90.dp)
                        .height(50.dp)
                        .border(1.dp, Color(0xFF404040), RoundedCornerShape(10.dp))) {
                  TextField(
                      value = priceValue,
                      onValueChange = onPriceChange,
                      placeholder = {
                        Text(
                            "ex: 12",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                      },
                      modifier = Modifier.fillMaxSize().testTag("price_input_field"),
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
                }
            // Reserve space for error to prevent layout shift
            Box(modifier = Modifier.height(20.dp).padding(top = 4.dp)) {
              priceError?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1)
              }
            }
          }

          // Capacity Input
          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                text = "Capacity",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier.width(90.dp)
                        .height(50.dp)
                        .border(1.dp, Color(0xFF404040), RoundedCornerShape(10.dp))) {
                  TextField(
                      value = capacityValue,
                      onValueChange = onCapacityChange,
                      placeholder = {
                        Text(
                            "ex: 100",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                      },
                      modifier = Modifier.fillMaxSize().testTag("capacity_input_field"),
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
                }
            // Reserve space for error to prevent layout shift
            Box(modifier = Modifier.height(20.dp).padding(top = 4.dp)) {
              capacityError?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1)
              }
            }
          }
        }
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
          color = Color.Red,
          style = MaterialTheme.typography.bodySmall,
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
          color = Color.Red,
          style = MaterialTheme.typography.bodySmall,
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
            color = Color.Red,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 12.dp))
      }
      fieldErrors[EventFormViewModel.ValidationError.END_TIME.key]?.let {
        Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
      }
    }
    fieldErrors[EventFormViewModel.ValidationError.TIME.key]?.let {
      Text(
          text = it,
          color = Color.Red,
          style = MaterialTheme.typography.bodySmall,
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
          color = Color.Red,
          style = MaterialTheme.typography.bodySmall,
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
          color = Color.Red,
          style = MaterialTheme.typography.bodySmall,
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
    Spacer(modifier = Modifier.height(32.dp))
  }
}
