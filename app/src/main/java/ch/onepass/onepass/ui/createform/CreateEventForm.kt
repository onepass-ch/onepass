package ch.onepass.onepass.ui.createform

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.createform.CreateEventFormViewModel.ValidationError
import ch.onepass.onepass.ui.theme.DefaultBackground
import ch.onepass.onepass.ui.theme.EventDateColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.text.format

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
                                  val hour =
                                      String.format(
                                          Locale.getDefault(), "%02d", timePickerState.hour)
                                  val minute =
                                      String.format(
                                          Locale.getDefault(), "%02d", timePickerState.minute)
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
      value
          .takeIf { it.isNotEmpty() }
          ?.let {
            try {
              SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it)?.time
            } catch (_: Exception) {
              null
            } ?: calendar.timeInMillis
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
    val datePickerColors =
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
                    disabledTextColor = Color.White,
                    cursorColor = EventDateColor,
                    focusedIndicatorColor = EventDateColor,
                    unfocusedIndicatorColor = Color(0xFF404040),
                    disabledIndicatorColor = Color(0xFF404040),
                    focusedLabelColor = EventDateColor,
                    unfocusedLabelColor = Color.Gray,
                    disabledLabelColor = Color.Gray))

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
        colors = datePickerColors) {
          DatePicker(state = datePickerState, colors = datePickerColors)
        }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScaffold(
    onNavigateBack: () -> Unit,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(text = "Create your Event", color = Color.White) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DefaultBackground))
      }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .background(DefaultBackground)
                      .verticalScroll(scrollState)
                      .padding(start = 22.dp, end = 22.dp, bottom = 48.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
              }
        }
      }
}

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
    modifier: Modifier = Modifier
) {
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
        TextField(
            value = value,
            onValueChange = onValueChange,
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
fun CreateEventButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Button(
      onClick = onClick,
      modifier =
          modifier
              .fillMaxWidth()
              .height(48.dp)
              .padding(start = 63.dp, top = 14.dp, end = 63.dp, bottom = 14.dp)
              .background(color = Color(0xFF683F88), shape = RoundedCornerShape(size = 5.dp))
              .background(color = Color(0x94333333), shape = RoundedCornerShape(size = 5.dp))
              .border(
                  width = 1.dp, color = Color(0xFF242424), shape = RoundedCornerShape(size = 5.dp)),
      shape = RoundedCornerShape(5.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = Color.Transparent, contentColor = Color.White),
      contentPadding = PaddingValues(0.dp),
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
              Text(text = "Create event", style = MaterialTheme.typography.labelLarge)
            }
      }
}

@Composable
fun CreateEventForm(
    organizationId: String,
    viewModel: CreateEventFormViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onEventCreated: () -> Unit = {},
) {
  val formState by viewModel.formState.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val fieldErrors by viewModel.fieldErrors.collectAsState()
  val scrollState = rememberScrollState()

  LaunchedEffect(Unit) { viewModel.setOrganizationId(organizationId) }

  LaunchedEffect(uiState) {
    when (uiState) {
      is CreateEventUiState.Success -> {
        onEventCreated()
        viewModel.resetForm()
      }
      is CreateEventUiState.Error -> {
        viewModel.clearError()
      }
      else -> {}
    }
  }

  EventFormScaffold(onNavigateBack, scrollState) {
    // Title
    TitleInputField(value = formState.title, onValueChange = { viewModel.updateTitle(it) })
    fieldErrors[ValidationError.TITLE.key]?.let {
      Text(
          text = it,
          color = Color.Red,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Description
    DescriptionInputField(
        value = formState.description, onValueChange = { viewModel.updateDescription(it) })
    fieldErrors[ValidationError.DESCRIPTION.key]?.let {
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
        onEndTimeChange = { viewModel.updateEndTime(it) })

    Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
      fieldErrors[ValidationError.START_TIME.key]?.let {
        Text(
            text = it,
            color = Color.Red,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 12.dp))
      }
      fieldErrors[ValidationError.END_TIME.key]?.let {
        Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
      }
    }
    fieldErrors[ValidationError.TIME.key]?.let {
      Text(
          text = it,
          color = Color.Red,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Date Input
    DateInputField(value = formState.date, onValueChange = { viewModel.updateDate(it) })

    fieldErrors[ValidationError.DATE.key]?.let {
      Text(
          text = it,
          color = Color.Red,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Location
    LocationInputField(value = formState.location, onValueChange = { viewModel.updateLocation(it) })
    fieldErrors[ValidationError.LOCATION.key]?.let {
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
        capacityError = fieldErrors["capacity"])
    Spacer(modifier = Modifier.height(32.dp))

    // Create Button
    CreateEventButton(onClick = { viewModel.createEvent() })
    Spacer(modifier = Modifier.height(24.dp))
  }

  if (uiState is CreateEventUiState.Loading) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = EventDateColor)
        }
  }
}
