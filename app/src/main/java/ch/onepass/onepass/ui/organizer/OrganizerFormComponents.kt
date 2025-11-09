package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
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
 * @param testTag Optional test tag for UI testing
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
    testTag: String? = null
) {
  Column {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier =
            Modifier.fillMaxWidth()
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = maxLines == 1,
        maxLines = maxLines)
    errorMessage?.let {
      Text(it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(16.dp))
  }
}

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
    phoneTestTag: String? = null,
    prefixTestTag: String? = null
) {
  Column {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      // Prefix Dropdown
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
                label = { Text("Country") },
                singleLine = true,
                trailingIcon = {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                },
                modifier = Modifier.menuAnchor())

            // Dropdown Menu Items
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = onDropdownDismiss,
                modifier = Modifier.heightIn(max = 300.dp)) {
                  // Populate dropdown with country list
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

      // Phone Number Field
      OutlinedTextField(
          value = phoneValue,
          // Allow only digits in phone number
          onValueChange = { onPhoneChange(it.filter(Char::isDigit)) },
          modifier =
              Modifier.weight(0.55f)
                  .onFocusChanged { onPhoneFocusChanged(it.isFocused) }
                  .then(if (phoneTestTag != null) Modifier.testTag(phoneTestTag) else Modifier),
          placeholder = { Text("Phone") },
          isError = prefixError != null,
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
    }

    prefixError?.let {
      Text(it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
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
fun SubmitButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
  Button(
      onClick = onClick,
      modifier = modifier.fillMaxWidth().height(48.dp),
      colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.primary))) {
        Text(text)
      }
}
