package ch.onepass.onepass.ui.createform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.DefaultBackground
import ch.onepass.onepass.ui.theme.EventDateColor
import ch.onepass.onepass.ui.theme.OnePassTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
  var showDialog by remember { mutableStateOf(false) }
  val calendar = Calendar.getInstance()
  val timePickerState =
      rememberTimePickerState(
          initialHour =
              if (value.isNotEmpty() && value.contains(":")) {
                value.split(":")[0].toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY)
              } else {
                calendar.get(Calendar.HOUR_OF_DAY)
              },
          initialMinute =
              if (value.isNotEmpty() && value.contains(":")) {
                value.split(":")[1].toIntOrNull() ?: calendar.get(Calendar.MINUTE)
              } else {
                calendar.get(Calendar.MINUTE)
              },
          is24Hour = true)

  Box(
      modifier =
          modifier
              .background(Color(0xFF767680).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
              .clip(RoundedCornerShape(6.dp))
              .clickable { showDialog = true }
              .padding(horizontal = 12.dp, vertical = 14.dp),
      contentAlignment = Alignment.Center) {
        Text(
            text = value.ifEmpty { "${Calendar.HOUR_OF_DAY }:${Calendar.MINUTE}" },
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    color = if (value.isEmpty()) Color.Gray else Color.Black))
      }

  if (showDialog) {
    Dialog(
        onDismissRequest = { showDialog = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
          Surface(
              modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
              shape = RoundedCornerShape(16.dp),
              color = Color(0xFF1C1C1C)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                          text = "Select Time",
                          style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                          modifier = Modifier.padding(bottom = 16.dp))

                      TimePicker(
                          state = timePickerState,
                          colors =
                              TimePickerDefaults.colors(
                                  clockDialColor = Color(0xFF262626),
                                  selectorColor = EventDateColor,
                                  containerColor = Color(0xFF1C1C1C),
                                  periodSelectorBorderColor = Color(0xFF404040),
                                  clockDialSelectedContentColor = Color.White,
                                  clockDialUnselectedContentColor = Color.Gray,
                                  periodSelectorSelectedContainerColor = EventDateColor,
                                  periodSelectorUnselectedContainerColor = Color(0xFF262626),
                                  periodSelectorSelectedContentColor = Color.White,
                                  periodSelectorUnselectedContentColor = Color.Gray,
                                  timeSelectorSelectedContainerColor = EventDateColor,
                                  timeSelectorUnselectedContainerColor = Color(0xFF262626),
                                  timeSelectorSelectedContentColor = Color.White,
                                  timeSelectorUnselectedContentColor = Color.Gray))

                      Row(
                          modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                          horizontalArrangement = Arrangement.End,
                          verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showDialog = false }) {
                              Text(
                                  "Cancel",
                                  style =
                                      MaterialTheme.typography.labelLarge.copy(color = Color.White))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                  val hour = String.format("%02d", timePickerState.hour)
                                  val minute = String.format("%02d", timePickerState.minute)
                                  onValueChange("$hour:$minute")
                                  showDialog = false
                                },
                                colors =
                                    ButtonDefaults.buttonColors(containerColor = EventDateColor)) {
                                  Text("OK", style = MaterialTheme.typography.labelLarge)
                                }
                          }
                    }
              }
        }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
  var showDialog by remember { mutableStateOf(false) }

  // Parse existing date or use current date
  val calendar = Calendar.getInstance()
  val initialDateMillis =
      if (value.isNotEmpty()) {
        try {
          val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
          sdf.parse(value)?.time ?: calendar.timeInMillis
        } catch (e: Exception) {
          calendar.timeInMillis
        }
      } else {
        calendar.timeInMillis
      }

  val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

  Box(
      modifier =
          modifier
              .background(Color(0xFF1C1C1C), RoundedCornerShape(6.dp))
              .border(1.dp, Color(0xFF404040), RoundedCornerShape(6.dp))
              .clip(RoundedCornerShape(6.dp))
              .clickable { showDialog = true }
              .padding(horizontal = 16.dp, vertical = 14.dp),
      contentAlignment = Alignment.CenterStart) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = value.ifEmpty { "Select date" },
                  style =
                      MaterialTheme.typography.bodyMedium.copy(
                          color = if (value.isEmpty()) Color(0xFF6C757D) else Color.White))
              Icon(
                  imageVector = ImageVector.vectorResource(R.drawable.choosedateicon),
                  contentDescription = "Select date",
                  tint = Color.Gray,
                  modifier = Modifier.size(20.dp))
            }
      }

  if (showDialog) {
    DatePickerDialog(
        onDismissRequest = { showDialog = false },
        confirmButton = {
          Button(
              onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                  val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                  val date = Date(millis)
                  onValueChange(sdf.format(date))
                }
                showDialog = false
              },
              colors = ButtonDefaults.buttonColors(containerColor = EventDateColor)) {
                Text("OK", style = MaterialTheme.typography.labelLarge)
              }
        },
        dismissButton = {
          TextButton(onClick = { showDialog = false }) {
            Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(color = Color.White))
          }
        },
        colors =
            DatePickerDefaults.colors(
                containerColor = Color(0xFF1C1C1C),
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = Color.Gray,
                subheadContentColor = Color.White,
                yearContentColor = Color.White,
                currentYearContentColor = Color.White,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = EventDateColor,
                dayContentColor = Color.White,
                disabledDayContentColor = Color.Gray,
                selectedDayContentColor = Color.White,
                disabledSelectedDayContentColor = Color.Gray,
                selectedDayContainerColor = EventDateColor,
                todayContentColor = EventDateColor,
                todayDateBorderColor = EventDateColor,
                dayInSelectionRangeContentColor = Color.White,
                dayInSelectionRangeContainerColor = EventDateColor.copy(alpha = 0.3f),
                dividerColor = Color(0xFF404040),
                dateTextFieldColors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF262626),
                        unfocusedContainerColor = Color(0xFF262626),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = EventDateColor,
                        focusedIndicatorColor = EventDateColor,
                        unfocusedIndicatorColor = Color(0xFF404040),
                        focusedLabelColor = EventDateColor,
                        unfocusedLabelColor = Color.Gray))) {
          DatePicker(
              state = datePickerState,
              colors =
                  DatePickerDefaults.colors(
                      containerColor = Color(0xFF1C1C1C),
                      titleContentColor = Color.White,
                      headlineContentColor = Color.White,
                      weekdayContentColor = Color.Gray,
                      subheadContentColor = Color.White,
                      yearContentColor = Color.White,
                      currentYearContentColor = Color.White,
                      selectedYearContentColor = Color.White,
                      selectedYearContainerColor = EventDateColor,
                      dayContentColor = Color.White,
                      disabledDayContentColor = Color.Gray,
                      selectedDayContentColor = Color.White,
                      disabledSelectedDayContentColor = Color.Gray,
                      selectedDayContainerColor = EventDateColor,
                      todayContentColor = EventDateColor,
                      todayDateBorderColor = EventDateColor,
                      dayInSelectionRangeContentColor = Color.White,
                      dayInSelectionRangeContainerColor = EventDateColor.copy(alpha = 0.3f),
                      dividerColor = Color(0xFF404040)))
        }
  }
}

@Composable
fun CreateEventForm(
    modifier: Modifier = Modifier,
    eventId: String? = null,
    viewModel: CreateEventFormViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onEventCreated: () -> Unit = {},
) {
  // Collect state from ViewModel
  val formState by viewModel.formState.collectAsState()
  val uiState by viewModel.uiState.collectAsState()

  // Load event if editing
  LaunchedEffect(eventId) {
    if (eventId != null) {
      viewModel.loadEvent(eventId)
    }
  }

  val scrollState = rememberScrollState()

  // Snackbar state for error messages
  val snackbarHostState = remember { SnackbarHostState() }

  // Handle UI state changes
  LaunchedEffect(uiState) {
    when (val state = uiState) {
      is CreateEventUiState.Success -> {
        // Event created successfully, navigate away
        onEventCreated()
      }
      is CreateEventUiState.Error -> {
        // Show error message
        snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Long)
        viewModel.clearError()
      }
      else -> {
        /* Idle or Loading */
      }
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier =
            Modifier.fillMaxWidth()
                .fillMaxHeight()
                .background(color = DefaultBackground)
                .verticalScroll(scrollState)
                .padding(start = 22.dp, end = 22.dp, bottom = 48.dp)) {
          // Title Header - Responsive
          Column(
              verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
              horizontalAlignment = Alignment.Start,
              modifier =
                  Modifier.fillMaxWidth()
                      .background(color = DefaultBackground)
                      .padding(top = 47.dp, bottom = 6.dp)) {
                Text(
                    text = if (viewModel.isEditMode()) "EDIT YOUR EVENT" else "CREATE YOUR EVENT",
                    style = MaterialTheme.typography.displaySmall.copy(color = Color(0xFFFFFFFF)),
                    modifier = Modifier.fillMaxWidth())
              }

          Spacer(modifier = Modifier.height(16.dp))

          // Title Input
          Column(
              verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
              horizontalAlignment = Alignment.Start,
              modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Title*",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFFFFFFF)))
                TextField(
                    value = formState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    placeholder = {
                      Text(
                          "Amazing event",
                          style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .border(1.dp, Color(0xFF404040), RoundedCornerShape(10.dp))
                            .heightIn(min = 50.dp),
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

          Spacer(modifier = Modifier.height(16.dp))

          // Description Input
          Column(
              modifier = Modifier.fillMaxWidth(),
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
                      value = formState.description,
                      onValueChange = { viewModel.updateDescription(it) },
                      placeholder = {
                        Text(
                            "This is amazing..",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                      },
                      modifier = Modifier.fillMaxSize(),
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

          Spacer(modifier = Modifier.height(16.dp))

          // Date & Time Section
          Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
              horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Date & time*",
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFFFFFFF), textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth())

                // Time Container
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(Color(0xFF1C1C1C), shape = RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF404040), shape = RoundedCornerShape(10.dp))
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      // Start and End Time Row
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.Top) {
                            // Start Time
                            Column(horizontalAlignment = Alignment.Start) {
                              Text(
                                  text = "Start time",
                                  style =
                                      MaterialTheme.typography.bodySmall.copy(color = Color.White))
                              Spacer(modifier = Modifier.height(4.dp))
                              TimePickerField(
                                  value = formState.startTime,
                                  onValueChange = { viewModel.updateStartTime(it) },
                                  modifier = Modifier.width(86.dp).heightIn(min = 60.dp))
                            }

                            // End Time
                            Column(horizontalAlignment = Alignment.Start) {
                              Text(
                                  text = "End time",
                                  style =
                                      MaterialTheme.typography.bodySmall.copy(color = Color.White))
                              Spacer(modifier = Modifier.height(4.dp))
                              TimePickerField(
                                  value = formState.endTime,
                                  onValueChange = { viewModel.updateEndTime(it) },
                                  modifier = Modifier.width(86.dp).heightIn(min = 60.dp))
                            }
                          }
                    }

                // Date Picker
                DatePickerField(
                    value = formState.date,
                    onValueChange = { viewModel.updateDate(it) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp))
              }

          Spacer(modifier = Modifier.height(16.dp))

          // Location Input
          Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
              horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Location*",
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFFFFFFF), textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth())
                TextField(
                    value = formState.location,
                    onValueChange = { viewModel.updateLocation(it) },
                    placeholder = {
                      Text(
                          "Type a location",
                          style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .border(1.dp, Color(0xFF404040), RoundedCornerShape(10.dp))
                            .heightIn(min = 50.dp),
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

          Spacer(modifier = Modifier.height(16.dp))

          // Tickets Section
          Column(
              modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                  // Price Input
                  Column {
                    Text(
                        text = "Price",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier =
                            Modifier.width(90.dp)
                                .height(43.dp)
                                .border(1.dp, Color(0xFF404040), RoundedCornerShape(10.dp))) {
                          TextField(
                              value = formState.price,
                              onValueChange = { viewModel.updatePrice(it) },
                              placeholder = {
                                Text(
                                    "ex: 12",
                                    style =
                                        MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                              },
                              modifier = Modifier.fillMaxSize(),
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

                  // Capacity Input
                  Column {
                    Text(
                        text = "Capacity",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier =
                            Modifier.width(90.dp)
                                .height(43.dp)
                                .border(1.dp, Color(0xFF404040), RoundedCornerShape(10.dp))) {
                          TextField(
                              value = formState.capacity,
                              onValueChange = { viewModel.updateCapacity(it) },
                              placeholder = {
                                Text(
                                    "ex: 250",
                                    style =
                                        MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                              },
                              modifier = Modifier.fillMaxSize(),
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
                }
          }

          Spacer(modifier = Modifier.height(32.dp))

          // Create/Update Ticket Button
          Button(
              onClick = {
                // TODO: Get organizerId and organizerName from AuthViewModel/User context
                // For now using placeholder values
                viewModel.saveEvent(
                    organizerId = "temp-organizer-id", organizerName = "Temporary Organizer")
              },
              modifier =
                  Modifier.fillMaxWidth()
                      .height(48.dp)
                      .padding(start = 63.dp, top = 14.dp, end = 63.dp, bottom = 14.dp)
                      .background(
                          color = Color(0xFF683F88), shape = RoundedCornerShape(size = 5.dp))
                      .background(
                          color = Color(0x94333333), shape = RoundedCornerShape(size = 5.dp))
                      .border(
                          width = 1.dp,
                          color = Color(0xFF242424),
                          shape = RoundedCornerShape(size = 5.dp)),
              shape = RoundedCornerShape(5.dp),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = Color.Transparent, // let our background show
                      contentColor = Color.White),
              contentPadding = PaddingValues(0.dp), // IMPORTANT
              elevation = ButtonDefaults.buttonElevation(0.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                          imageVector = Icons.Default.Add,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(8.dp))
                      Text(
                          text = if (viewModel.isEditMode()) "Update ticket" else "Create ticket",
                          style = MaterialTheme.typography.labelLarge)
                    }
              }

          Spacer(modifier = Modifier.height(24.dp))
        }

    // Loading overlay
    if (uiState is CreateEventUiState.Loading) {
      Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EventDateColor)
          }
    }

    // Snackbar for error messages
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
  }
}

@Preview(name = "Phone - Pixel 7", device = Devices.PIXEL_7_PRO)
@Preview(name = "Tablet - Nexus 9", device = Devices.NEXUS_9)
@Preview(name = "Foldable - Galaxy Fold", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun CreateEventFormPreview() {
  OnePassTheme { CreateEventForm() }
}
