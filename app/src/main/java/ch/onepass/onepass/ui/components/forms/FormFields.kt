package ch.onepass.onepass.ui.components.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.ui.theme.Error

/**
 * Reusable text field component for the form.
 *
 * @param value Current text value
 * @param onValueChange Callback for text changes
 * @param label Label to display
 * @param isError Whether the field is in an error state
 * @param onFocusChanged Callback for focus changes
 * @param keyboardType Keyboard type for input
 * @param maxLines Maximum number of lines
 * @param errorMessage Optional error message to display
 * @param modifier Optional modifier for styling
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = 1,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
  Column(modifier = modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = colorScheme.onBackground) },
        modifier =
            Modifier.fillMaxWidth()
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = maxLines == 1,
        maxLines = maxLines)
    errorMessage?.let { Text(it, color = Error, style = MaterialTheme.typography.bodySmall) }
    Spacer(Modifier.height(16.dp))
  }
}

/**
 * Composable row for selecting phone prefix and entering phone number.
 *
 * @param prefixDisplayText Text to display for the selected prefix
 * @param prefixError Optional error message for the prefix field
 * @param countryList List of country names and their corresponding codes
 * @param dropdownExpanded Whether the prefix dropdown is expanded
 * @param onDropdownDismiss Callback to dismiss the dropdown
 * @param onCountrySelected Callback when a country is selected
 * @param phoneValue Current phone number value
 * @param onPhoneChange Callback for phone number changes
 * @param onPhoneFocusChanged Callback for phone field focus changes
 * @param onPrefixClick Callback when the prefix field is clicked
 * @param modifier Optional modifier for the row
 * @param phoneTestTag Optional test tag for the phone field
 * @param prefixTestTag Optional test tag for the prefix field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrefixPhoneRow(
    prefixDisplayText: String,
    prefixError: String?,
    countryList: List<Pair<String, Int>>,
    dropdownExpanded: Boolean,
    onDropdownDismiss: () -> Unit,
    onCountrySelected: (Int) -> Unit,
    phoneValue: String,
    onPhoneChange: (String) -> Unit,
    onPhoneFocusChanged: (Boolean) -> Unit,
    onPrefixClick: () -> Unit,
    modifier: Modifier = Modifier,
    phoneTestTag: String? = null,
    prefixTestTag: String? = null
) {
  Column(modifier = modifier) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      ExposedDropdownMenuBox(
          expanded = dropdownExpanded,
          onExpandedChange = { if (it) onPrefixClick() else onDropdownDismiss() },
          modifier =
              Modifier.weight(0.45f)
                  .then(if (prefixTestTag != null) Modifier.testTag(prefixTestTag) else Modifier)) {
            OutlinedTextField(
                value = prefixDisplayText,
                onValueChange = {},
                readOnly = true,
                isError = prefixError != null,
                placeholder = { Text("Country", color = colorScheme.onBackground) },
                singleLine = true,
                trailingIcon = {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth().height(56.dp))

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = onDropdownDismiss,
                modifier = Modifier.heightIn(max = 300.dp).background(colorScheme.surface)) {
                  countryList.forEachIndexed { index, (country, code) ->
                    DropdownMenuItem(
                        text = { Text("+$code $country", color = colorScheme.onBackground) },
                        onClick = {
                          onCountrySelected(index)
                          onDropdownDismiss()
                        })
                  }
                }
          }

      Spacer(modifier = Modifier.width(8.dp))

      OutlinedTextField(
          value = phoneValue,
          onValueChange = { onPhoneChange(it.filter(Char::isDigit)) },
          modifier =
              Modifier.weight(0.55f)
                  .fillMaxWidth()
                  .height(56.dp)
                  .onFocusChanged { onPhoneFocusChanged(it.isFocused) }
                  .then(if (phoneTestTag != null) Modifier.testTag(phoneTestTag) else Modifier),
          placeholder = { Text("Phone", color = colorScheme.onBackground) },
          isError = prefixError != null,
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
    }

    prefixError?.let { Text(it, color = Error, style = MaterialTheme.typography.bodySmall) }

    Spacer(Modifier.height(16.dp))
  }
}

/**
 * Submit button for the form.
 *
 * @param onClick Callback when the button is clicked
 * @param text Text to display on the button
 * @param modifier Optional modifier for styling
 * @param isLoading When true, a loading indicator is shown and the button is disabled
 * @param enabled Controls the enabled state of the button (takes precedence over isLoading)
 */
@Composable
fun SubmitButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
  Button(
      onClick = onClick,
      modifier = modifier.fillMaxWidth().height(48.dp),
      enabled = enabled && !isLoading,
      colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)) {
        if (isLoading) {
          CircularProgressIndicator(
              modifier = Modifier.size(24.dp).testTag("submit_loading_indicator"),
              color = colorScheme.onBackground,
              strokeWidth = 2.dp)
        } else {
          Text(text, color = colorScheme.onBackground)
        }
      }
}
