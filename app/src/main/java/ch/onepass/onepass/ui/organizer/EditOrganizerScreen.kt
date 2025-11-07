package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R

@Composable
fun EditOrganizationScreenUI() {
  var name by remember { mutableStateOf("My Organization") }
  var description by remember { mutableStateOf("Organization description here.") }
  var contactEmail by remember { mutableStateOf("email@example.com") }
  var contactPhone by remember { mutableStateOf("123456789") }
  var website by remember { mutableStateOf("example.com") }
  var instagram by remember { mutableStateOf("insta_handle") }
  var facebook by remember { mutableStateOf("fb_page") }
  var tiktok by remember { mutableStateOf("tiktok_handle") }
  var address by remember { mutableStateOf("123 Street, City") }

  val countryList = listOf("Switzerland" to 41, "USA" to 1, "Germany" to 49)
  var selectedCountryIndex by remember { mutableStateOf(0) }
  var prefixDropdownExpanded by remember { mutableStateOf(false) }
  var prefixDisplayText by remember {
    mutableStateOf("+${countryList[selectedCountryIndex].second}")
  }
  var uiLoading by remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .background(colorResource(id = R.color.background))
                .padding(16.dp)) {
          Text(
              text = "Edit Organization",
              style = MaterialTheme.typography.headlineMedium,
              color = colorResource(id = R.color.on_background),
              modifier = Modifier.padding(vertical = 24.dp))

          FormTextField(value = name, onValueChange = { name = it }, label = "Organization Name*")
          FormTextField(
              value = description,
              onValueChange = { description = it },
              label = "Description*",
              maxLines = 5)
          FormTextField(
              value = contactEmail, onValueChange = { contactEmail = it }, label = "Contact Email*")

          PrefixPhoneRow(
              selectedCountryIndex = selectedCountryIndex,
              prefixDisplayText = prefixDisplayText,
              prefixError = null,
              countryList = countryList,
              dropdownExpanded = prefixDropdownExpanded,
              onDropdownDismiss = { prefixDropdownExpanded = false },
              onCountrySelected = { index ->
                selectedCountryIndex = index
                prefixDisplayText = "+${countryList[index].second}"
                prefixDropdownExpanded = false
              },
              phoneValue = contactPhone,
              onPhoneChange = { contactPhone = it },
              onPhoneFocusChanged = {},
              onPrefixClick = { prefixDropdownExpanded = true })

          FormTextField(value = website, onValueChange = { website = it }, label = "Website")
          FormTextField(value = instagram, onValueChange = { instagram = it }, label = "Instagram")
          FormTextField(value = facebook, onValueChange = { facebook = it }, label = "Facebook")
          FormTextField(value = tiktok, onValueChange = { tiktok = it }, label = "TikTok")
          FormTextField(value = address, onValueChange = { address = it }, label = "Address")

          Spacer(modifier = Modifier.height(32.dp))
          SubmitButton(onClick = { uiLoading = true }, text = "Save Changes")
        }

    if (uiLoading) {
      Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colorResource(id = R.color.primary))
          }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun EditOrganizationScreenPreview() {
  EditOrganizationScreenUI()
}
