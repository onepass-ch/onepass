package ch.onepass.onepass.ui.components.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.onepass.onepass.R
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
              .background(colorScheme.onBackground.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
              .clip(RoundedCornerShape(6.dp))
              .clickable { showDialog = true }
              .padding(horizontal = 12.dp, vertical = 14.dp),
      contentAlignment = Alignment.Center) {
        Text(
            text = value.ifEmpty { "${Calendar.HOUR_OF_DAY}:${Calendar.MINUTE}" },
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    color =
                        if (value.isEmpty()) colorScheme.onSurface else colorScheme.onBackground))
      }

  if (showDialog) {
    Dialog(
        onDismissRequest = { showDialog = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
          Surface(
              modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
              shape = RoundedCornerShape(16.dp),
              color = colorScheme.background) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                          text = stringResource(R.string.time_picker_title),
                          style =
                              MaterialTheme.typography.titleLarge.copy(
                                  color = colorScheme.onBackground),
                          modifier = Modifier.padding(bottom = 16.dp))

                      TimePicker(
                          state = timePickerState,
                          colors =
                              TimePickerDefaults.colors(
                                  clockDialColor = colorScheme.surface,
                                  selectorColor = colorScheme.primary,
                                  containerColor = colorScheme.background,
                                  periodSelectorBorderColor = colorScheme.onSurface,
                                  clockDialSelectedContentColor = colorScheme.onBackground,
                                  clockDialUnselectedContentColor = colorScheme.onSurface,
                                  periodSelectorSelectedContainerColor = colorScheme.primary,
                                  periodSelectorUnselectedContainerColor = colorScheme.surface,
                                  periodSelectorSelectedContentColor = colorScheme.onBackground,
                                  periodSelectorUnselectedContentColor = colorScheme.onSurface,
                                  timeSelectorSelectedContainerColor = colorScheme.primary,
                                  timeSelectorUnselectedContainerColor = colorScheme.surface,
                                  timeSelectorSelectedContentColor = colorScheme.onBackground,
                                  timeSelectorUnselectedContentColor = colorScheme.onSurface))

                      Row(
                          modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                          horizontalArrangement = Arrangement.End,
                          verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showDialog = false }) {
                              Text(
                                  stringResource(R.string.dialog_cancel),
                                  style =
                                      MaterialTheme.typography.labelLarge.copy(
                                          color = colorScheme.onBackground))
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
                                    ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary)) {
                                  Text(
                                      stringResource(R.string.dialog_ok),
                                      style = MaterialTheme.typography.labelLarge)
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
              .background(colorScheme.surface, RoundedCornerShape(6.dp))
              .border(1.dp, colorScheme.onSurface, RoundedCornerShape(6.dp))
              .clip(RoundedCornerShape(6.dp))
              .clickable { showDialog = true }
              .padding(horizontal = 16.dp, vertical = 14.dp),
      contentAlignment = Alignment.CenterStart) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = value.ifEmpty { stringResource(R.string.date_picker_placeholder) },
                  style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface))
              Icon(
                  imageVector = ImageVector.vectorResource(R.drawable.choosedateicon),
                  contentDescription = stringResource(R.string.date_picker_icon_description),
                  tint = colorScheme.onSurface,
                  modifier = Modifier.size(20.dp))
            }
      }
  if (showDialog) {
    val datePickerColors =
        DatePickerDefaults.colors(
            containerColor = colorScheme.background,
            titleContentColor = colorScheme.onBackground,
            headlineContentColor = colorScheme.onBackground,
            weekdayContentColor = colorScheme.onSurface,
            subheadContentColor = colorScheme.onBackground,
            yearContentColor = colorScheme.onBackground,
            currentYearContentColor = colorScheme.onBackground,
            selectedYearContentColor = colorScheme.onBackground,
            selectedYearContainerColor = colorScheme.primary,
            dayContentColor = colorScheme.onBackground,
            disabledDayContentColor = colorScheme.onSurface,
            selectedDayContentColor = colorScheme.onBackground,
            disabledSelectedDayContentColor = colorScheme.onSurface,
            selectedDayContainerColor = colorScheme.primary,
            todayContentColor = colorScheme.primary,
            todayDateBorderColor = colorScheme.primary,
            dayInSelectionRangeContentColor = colorScheme.onBackground,
            dayInSelectionRangeContainerColor = colorScheme.primary.copy(alpha = 0.3f),
            dividerColor = colorScheme.onSurface,
            dateTextFieldColors =
                TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    focusedTextColor = colorScheme.onBackground,
                    unfocusedTextColor = colorScheme.onBackground,
                    disabledTextColor = colorScheme.onBackground,
                    cursorColor = colorScheme.primary,
                    focusedIndicatorColor = colorScheme.primary,
                    unfocusedIndicatorColor = colorScheme.onSurface,
                    disabledIndicatorColor = colorScheme.onSurface,
                    focusedLabelColor = colorScheme.primary,
                    unfocusedLabelColor = colorScheme.onSurface,
                    disabledLabelColor = colorScheme.onSurface))

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
              colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)) {
                Text(
                    stringResource(R.string.dialog_ok), style = MaterialTheme.typography.labelLarge)
              }
        },
        dismissButton = {
          TextButton(onClick = { showDialog = false }) {
            Text(
                stringResource(R.string.dialog_cancel),
                style = MaterialTheme.typography.labelLarge.copy(color = colorScheme.onBackground))
          }
        },
        colors = datePickerColors) {
          DatePicker(state = datePickerState, colors = datePickerColors)
        }
  }
}
