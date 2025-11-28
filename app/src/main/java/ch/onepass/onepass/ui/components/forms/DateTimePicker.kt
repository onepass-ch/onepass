package ch.onepass.onepass.ui.components.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.EventDateColor
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
              .background(
                  colorResource(id = R.color.time_picker_background).copy(alpha = 0.12f),
                  RoundedCornerShape(6.dp))
              .clip(RoundedCornerShape(6.dp))
              .clickable { showDialog = true }
              .padding(horizontal = 12.dp, vertical = 14.dp),
      contentAlignment = Alignment.Center) {
        Text(
            text = value.ifEmpty { "${Calendar.HOUR_OF_DAY}:${Calendar.MINUTE}" },
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    color =
                        if (value.isEmpty()) colorResource(id = R.color.gray)
                        else colorResource(id = R.color.black)))
      }

  if (showDialog) {
    Dialog(
        onDismissRequest = { showDialog = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
          Surface(
              modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
              shape = RoundedCornerShape(16.dp),
              color = colorResource(id = R.color.date_picker_background)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                          text = "Select Time",
                          style =
                              MaterialTheme.typography.titleLarge.copy(
                                  color = colorResource(id = R.color.white)),
                          modifier = Modifier.padding(bottom = 16.dp))

                      TimePicker(
                          state = timePickerState,
                          colors =
                              TimePickerDefaults.colors(
                                  clockDialColor =
                                      colorResource(id = R.color.date_picker_background_darker),
                                  selectorColor = EventDateColor,
                                  containerColor =
                                      colorResource(id = R.color.date_picker_background),
                                  periodSelectorBorderColor =
                                      colorResource(id = R.color.date_picker_border),
                                  clockDialSelectedContentColor = colorResource(id = R.color.white),
                                  clockDialUnselectedContentColor =
                                      colorResource(id = R.color.gray),
                                  periodSelectorSelectedContainerColor = EventDateColor,
                                  periodSelectorUnselectedContainerColor =
                                      colorResource(id = R.color.date_picker_background_darker),
                                  periodSelectorSelectedContentColor =
                                      colorResource(id = R.color.white),
                                  periodSelectorUnselectedContentColor =
                                      colorResource(id = R.color.gray),
                                  timeSelectorSelectedContainerColor = EventDateColor,
                                  timeSelectorUnselectedContainerColor =
                                      colorResource(id = R.color.date_picker_background_darker),
                                  timeSelectorSelectedContentColor =
                                      colorResource(id = R.color.white),
                                  timeSelectorUnselectedContentColor =
                                      colorResource(id = R.color.gray)))

                      Row(
                          modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                          horizontalArrangement = Arrangement.End,
                          verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showDialog = false }) {
                              Text(
                                  "Cancel",
                                  style =
                                      MaterialTheme.typography.labelLarge.copy(
                                          color = colorResource(id = R.color.white)))
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
              .background(
                  colorResource(id = R.color.date_picker_background), RoundedCornerShape(6.dp))
              .border(
                  1.dp, colorResource(id = R.color.date_picker_border), RoundedCornerShape(6.dp))
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
                          color =
                              if (value.isEmpty())
                                  colorResource(id = R.color.date_picker_text_secondary)
                              else colorResource(id = R.color.white)))
              Icon(
                  imageVector = ImageVector.vectorResource(R.drawable.choosedateicon),
                  contentDescription = "Select date",
                  tint = colorResource(id = R.color.gray),
                  modifier = Modifier.size(20.dp))
            }
      }
  if (showDialog) {
    val datePickerColors =
        DatePickerDefaults.colors(
            containerColor = colorResource(id = R.color.date_picker_background),
            titleContentColor = colorResource(id = R.color.white),
            headlineContentColor = colorResource(id = R.color.white),
            weekdayContentColor = colorResource(id = R.color.gray),
            subheadContentColor = colorResource(id = R.color.white),
            yearContentColor = colorResource(id = R.color.white),
            currentYearContentColor = colorResource(id = R.color.white),
            selectedYearContentColor = colorResource(id = R.color.white),
            selectedYearContainerColor = EventDateColor,
            dayContentColor = colorResource(id = R.color.white),
            disabledDayContentColor = colorResource(id = R.color.gray),
            selectedDayContentColor = colorResource(id = R.color.white),
            disabledSelectedDayContentColor = colorResource(id = R.color.gray),
            selectedDayContainerColor = EventDateColor,
            todayContentColor = EventDateColor,
            todayDateBorderColor = EventDateColor,
            dayInSelectionRangeContentColor = colorResource(id = R.color.white),
            dayInSelectionRangeContainerColor = EventDateColor.copy(alpha = 0.3f),
            dividerColor = colorResource(id = R.color.date_picker_border),
            dateTextFieldColors =
                TextFieldDefaults.colors(
                    focusedContainerColor =
                        colorResource(id = R.color.date_picker_background_darker),
                    unfocusedContainerColor =
                        colorResource(id = R.color.date_picker_background_darker),
                    focusedTextColor = colorResource(id = R.color.white),
                    unfocusedTextColor = colorResource(id = R.color.white),
                    disabledTextColor = colorResource(id = R.color.white),
                    cursorColor = EventDateColor,
                    focusedIndicatorColor = EventDateColor,
                    unfocusedIndicatorColor = colorResource(id = R.color.date_picker_border),
                    disabledIndicatorColor = colorResource(id = R.color.date_picker_border),
                    focusedLabelColor = EventDateColor,
                    unfocusedLabelColor = colorResource(id = R.color.gray),
                    disabledLabelColor = colorResource(id = R.color.gray)))

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
            Text(
                "Cancel",
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        color = colorResource(id = R.color.white)))
          }
        },
        colors = datePickerColors) {
          DatePicker(state = datePickerState, colors = datePickerColors)
        }
  }
}
