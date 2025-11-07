package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R

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
    errorMessage: String? = null
) {
  Column {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = maxLines == 1,
        maxLines = maxLines)
    errorMessage?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall) }
    Spacer(Modifier.height(16.dp))
  }
}

/**
 * Composable for selecting country prefix and entering phone number.
 *
 * @param selectedCountryIndex Index of the selected country
 * @param prefixDisplayText Text to display for the prefix
 * @param prefixError Optional error message for the prefix/phone
 * @param countryList List of country names and their codes
 * @param dropdownExpanded Whether the dropdown is expanded
 * @param onDropdownDismiss Callback to dismiss the dropdown
 * @param onCountrySelected Callback when a country is selected
 * @param phoneValue Current phone number value
 * @param onPhoneChange Callback for phone number changes
 * @param onPhoneFocusChanged Callback for phone focus changes
 * @param onPrefixClick Callback when prefix field is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrefixPhoneRow(
    selectedCountryIndex: Int,
    prefixDisplayText: String,
    prefixError: String?,
    countryList: List<Pair<String, Int>>,
    dropdownExpanded: Boolean,
    onDropdownDismiss: () -> Unit,
    onCountrySelected: (Int) -> Unit,
    phoneValue: String,
    onPhoneChange: (String) -> Unit,
    onPhoneFocusChanged: (Boolean) -> Unit,
    onPrefixClick: () -> Unit
) {
  Column {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      ExposedDropdownMenuBox(
          expanded = dropdownExpanded,
          onExpandedChange = { expanded -> if (expanded) onPrefixClick() else onDropdownDismiss() },
          modifier = Modifier.weight(0.45f)) {
            OutlinedTextField(
                value = prefixDisplayText,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                textStyle =
                    LocalTextStyle.current.copy(
                        color =
                            if (selectedCountryIndex < 0) Color.Gray
                            else LocalTextStyle.current.color),
                isError = prefixError != null,
                modifier = Modifier.menuAnchor().fillMaxWidth().clickable { onPrefixClick() },
                trailingIcon = {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                })

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = onDropdownDismiss,
                modifier = Modifier.heightIn(max = 300.dp)) {
                  countryList.forEachIndexed { index, (country, code) ->
                    DropdownMenuItem(
                        text = { Text("+$code $country") },
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
          modifier = Modifier.weight(0.55f).onFocusChanged { onPhoneFocusChanged(it.isFocused) },
          placeholder = { Text("Phone*") },
          isError = prefixError != null,
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
    }

    prefixError?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall) }
    Spacer(Modifier.height(16.dp))
  }
}

/**
 * Submit button for the form.
 *
 * @param onClick Callback when the button is clicked
 * @param text Text to display on the button
 */
@Composable
fun SubmitButton(onClick: () -> Unit, text: String) {
  Button(
      onClick = onClick,
      modifier = Modifier.fillMaxWidth().height(48.dp),
      colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.primary))) {
        Text(text)
      }
}
