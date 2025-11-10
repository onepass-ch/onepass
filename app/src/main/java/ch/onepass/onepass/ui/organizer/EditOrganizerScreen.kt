package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import kotlinx.coroutines.launch

@Composable
fun EditOrganizationScreen(
    organizationId: String,
    userId: String,
    viewModel: EditOrganizationViewModel = viewModel(),
    formViewModel: BecomeOrganizerViewModel = viewModel(),
    onOrganizationUpdated: () -> Unit = {}
) {
  val formState by formViewModel.formState.collectAsState()
  val uiState by formViewModel.uiState.collectAsState()
  val countryList by formViewModel.countryList.collectAsState()
  val scrollState = rememberScrollState()
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  var prefixDropdownExpanded by remember { mutableStateOf(false) }
  var prefixDisplayText by remember { mutableStateOf("Prefix") }

  LaunchedEffect(userId) {
    // Load the first organization for the user
    // when the screen is first composed
    viewModel.loadFirstOrganizationForUser(userId)
  }

  LaunchedEffect(uiState) {
    // Handle side effects based on UI state changes
    uiState.successOrganizationId?.let { onOrganizationUpdated() }
    uiState.errorMessage?.let {
      coroutineScope.launch { snackbarHostState.showSnackbar(it) }
      formViewModel.clearError()
    }
  }

  Scaffold(
      snackbarHost = {
        // Display snackbar for error messages
        SnackbarHost(snackbarHostState, Modifier.testTag(OrganizerTestTags.SNACKBAR))
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .padding(paddingValues)) {
              Text(
                  text = "Edit Organization",
                  style = MaterialTheme.typography.headlineMedium,
                  color = colorResource(id = R.color.on_background),
                  modifier = Modifier.padding(vertical = 24.dp))

              // Organization Name Field
              FormTextField(
                  value = formState.name.value,
                  onValueChange = formViewModel::updateName,
                  label = "Organization Name*",
                  isError = formState.name.error != null,
                  onFocusChanged = formViewModel::onFocusChangeName,
                  errorMessage = formState.name.error,
                  testTag = OrganizerTestTags.NAME_FIELD)

              // Organization Description Field
              FormTextField(
                  value = formState.description.value,
                  onValueChange = formViewModel::updateDescription,
                  label = "Description*",
                  isError = formState.description.error != null,
                  onFocusChanged = formViewModel::onFocusChangeDescription,
                  maxLines = 5,
                  errorMessage = formState.description.error,
                  testTag = OrganizerTestTags.DESCRIPTION_FIELD)

              // Contact Email Field
              FormTextField(
                  value = formState.contactEmail.value,
                  onValueChange = formViewModel::updateContactEmail,
                  label = "Contact Email",
                  isError = formState.contactEmail.error != null,
                  onFocusChanged = formViewModel::onFocusChangeEmail,
                  keyboardType = KeyboardType.Email,
                  errorMessage = formState.contactEmail.error,
                  testTag = OrganizerTestTags.EMAIL_FIELD)

              // Contact Phone Field with Prefix Dropdown
              PrefixPhoneRow(
                  prefixDisplayText = prefixDisplayText,
                  prefixError = formState.contactPhone.error,
                  countryList = countryList,
                  dropdownExpanded = prefixDropdownExpanded,
                  onDropdownDismiss = { prefixDropdownExpanded = false },
                  onCountrySelected = { index ->
                    formViewModel.updateCountryIndex(index)
                    prefixDisplayText = "+${countryList[index].second}"
                    prefixDropdownExpanded = false
                  },
                  phoneValue = formState.contactPhone.value,
                  onPhoneChange = formViewModel::updateContactPhone,
                  onPhoneFocusChanged = formViewModel::onFocusChangePhone,
                  onPrefixClick = { prefixDropdownExpanded = true },
                  phoneTestTag = OrganizerTestTags.PHONE_FIELD,
                  prefixTestTag = OrganizerTestTags.PREFIX_DROPDOWN)

              // Website Field
              FormTextField(
                  value = formState.website.value,
                  onValueChange = formViewModel::updateWebsite,
                  label = "Website",
                  isError = formState.website.error != null,
                  onFocusChanged = formViewModel::onFocusChangeWebsite,
                  errorMessage = formState.website.error,
                  testTag = OrganizerTestTags.WEBSITE_FIELD)

              // Social Media Fields
              FormTextField(
                  value = formState.instagram.value,
                  onValueChange = formViewModel::updateInstagram,
                  label = "Instagram",
                  testTag = OrganizerTestTags.INSTAGRAM_FIELD)
              FormTextField(
                  value = formState.facebook.value,
                  onValueChange = formViewModel::updateFacebook,
                  label = "Facebook",
                  testTag = OrganizerTestTags.FACEBOOK_FIELD)
              FormTextField(
                  value = formState.tiktok.value,
                  onValueChange = formViewModel::updateTiktok,
                  label = "TikTok",
                  testTag = OrganizerTestTags.TIKTOK_FIELD)

              // Address Field
              FormTextField(
                  value = formState.address.value,
                  onValueChange = formViewModel::updateAddress,
                  label = "Address",
                  testTag = OrganizerTestTags.ADDRESS_FIELD)

              Spacer(Modifier.height(32.dp))

              // Submit Button
              SubmitButton(
                  onClick = {
                    coroutineScope.launch {
                      val data =
                          EditOrganizationData(
                              id = organizationId,
                              name = formState.name.value,
                              description = formState.description.value,
                              contactEmail = formState.contactEmail.value.ifBlank { null },
                              contactPhone = formState.contactPhone.value.ifBlank { null },
                              website = formState.website.value.ifBlank { null },
                              instagram = formState.instagram.value.ifBlank { null },
                              facebook = formState.facebook.value.ifBlank { null },
                              tiktok = formState.tiktok.value.ifBlank { null },
                              address = formState.address.value.ifBlank { null })
                      viewModel.updateOrganization(data)
                    }
                  },
                  text = "Update",
                  modifier = Modifier.testTag(OrganizerTestTags.SUBMIT_BUTTON))
            }
      }
}
