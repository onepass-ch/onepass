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

/** Test tags for EditOrganization screen components */
object EditOrganizerTestTags {
  const val NAME_FIELD = "EditOrgNameField"
  const val DESCRIPTION_FIELD = "EditOrgDescriptionField"
  const val EMAIL_FIELD = "EditOrgEmailField"
  const val PHONE_FIELD = "EditOrgPhoneField"
  const val WEBSITE_FIELD = "EditOrgWebsiteField"
  const val INSTAGRAM_FIELD = "EditOrgInstagramField"
  const val FACEBOOK_FIELD = "EditOrgFacebookField"
  const val TIKTOK_FIELD = "EditOrgTiktokField"
  const val ADDRESS_FIELD = "EditOrgAddressField"
  const val SUBMIT_BUTTON = "EditOrgSubmitButton"
  const val PREFIX_DROPDOWN = "EditOrgPrefixDropdown"
  const val SNACKBAR = "EditOrgSnackbar"
}

/**
 * Composable screen for editing an existing organization's details.
 *
 * @param organizationId ID of the organization to edit
 * @param viewModel ViewModel managing the organization's data and update logic
 * @param formViewModel ViewModel managing the form state
 * @param onOrganizationUpdated Callback invoked when the organization is successfully updated
 */
@Composable
fun EditOrganizationScreen(
    organizationId: String,
    viewModel: EditOrganizationViewModel = viewModel(),
    formViewModel: BecomeOrganizerViewModel = viewModel(),
    onOrganizationUpdated: () -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  val formState by formViewModel.formState.collectAsState()
  val countryList by formViewModel.countryList.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  var prefixDropdownExpanded by remember { mutableStateOf(false) }
  var prefixDisplayText by remember { mutableStateOf("") }

  // Load organization data when the screen is first displayed
  LaunchedEffect(organizationId) { viewModel.loadOrganizationById(organizationId) }

  // Populate form fields when organization data is loaded
  LaunchedEffect(uiState.organization) {
    uiState.organization?.let { org ->
      formViewModel.updateName(org.name)
      formViewModel.updateDescription(org.description)
      formViewModel.updateContactEmail(org.contactEmail ?: "")
      formViewModel.updateContactPhone(org.contactPhone ?: "")
      formViewModel.updateWebsite(org.website ?: "")
      formViewModel.updateInstagram(org.instagram ?: "")
      formViewModel.updateFacebook(org.facebook ?: "")
      formViewModel.updateTiktok(org.tiktok ?: "")
      formViewModel.updateAddress(org.address ?: "")

      // Set country index based on phone prefix
      org.contactPhone?.let {
        val prefix = it.takeWhile { c -> c == '+' || c.isDigit() }
        prefixDisplayText = prefix
      }
    }
  }

  // Handle success and error messages
  LaunchedEffect(uiState.success, uiState.errorMessage) {
    if (uiState.success) {
      onOrganizationUpdated()
      viewModel.clearSuccessFlag()
    }
    // Show error message in snackbar
    uiState.errorMessage?.let {
      scope.launch { snackbarHostState.showSnackbar(it) }
      viewModel.clearError()
    }
  }

  Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    // Main content
    when {
      uiState.isLoading && uiState.organization == null -> {
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          CircularProgressIndicator(modifier = Modifier.padding(32.dp))
        }
      }
      // Show form when organization data is available
      uiState.organization != null -> {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .padding(padding)) {
              Text(
                  text = "Edit Organization",
                  style = MaterialTheme.typography.headlineMedium,
                  color = colorResource(id = R.color.on_background),
                  modifier = Modifier.padding(vertical = 24.dp))

              // Form fields
              FormTextField(
                  value = formState.name.value,
                  onValueChange = formViewModel::updateName,
                  label = "Organization Name*",
                  isError = formState.name.error != null,
                  onFocusChanged = formViewModel::onFocusChangeName,
                  errorMessage = formState.name.error,
                  testTag = EditOrganizerTestTags.NAME_FIELD)

              // Organization Description Field
              FormTextField(
                  value = formState.description.value,
                  onValueChange = formViewModel::updateDescription,
                  label = "Description*",
                  isError = formState.description.error != null,
                  onFocusChanged = formViewModel::onFocusChangeDescription,
                  maxLines = 5,
                  errorMessage = formState.description.error,
                  testTag = EditOrganizerTestTags.DESCRIPTION_FIELD)

              // Contact Email Field
              FormTextField(
                  value = formState.contactEmail.value,
                  onValueChange = formViewModel::updateContactEmail,
                  label = "Contact Email",
                  keyboardType = KeyboardType.Email,
                  isError = formState.contactEmail.error != null,
                  onFocusChanged = formViewModel::onFocusChangeEmail,
                  errorMessage = formState.contactEmail.error,
                  testTag = EditOrganizerTestTags.EMAIL_FIELD)

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
                  phoneTestTag = EditOrganizerTestTags.PHONE_FIELD,
                  prefixTestTag = EditOrganizerTestTags.PREFIX_DROPDOWN)

              // Social Media
              FormTextField(
                  value = formState.website.value,
                  onValueChange = formViewModel::updateWebsite,
                  label = "Website",
                  testTag = EditOrganizerTestTags.WEBSITE_FIELD)
              FormTextField(
                  value = formState.instagram.value,
                  onValueChange = formViewModel::updateInstagram,
                  label = "Instagram",
                  testTag = EditOrganizerTestTags.INSTAGRAM_FIELD)
              FormTextField(
                  value = formState.facebook.value,
                  onValueChange = formViewModel::updateFacebook,
                  label = "Facebook",
                  testTag = EditOrganizerTestTags.FACEBOOK_FIELD)
              FormTextField(
                  value = formState.tiktok.value,
                  onValueChange = formViewModel::updateTiktok,
                  label = "TikTok",
                  testTag = EditOrganizerTestTags.TIKTOK_FIELD)

              // Address Field
              FormTextField(
                  value = formState.address.value,
                  onValueChange = formViewModel::updateAddress,
                  label = "Address",
                  testTag = EditOrganizerTestTags.ADDRESS_FIELD)

              Spacer(Modifier.height(32.dp))

              // Submit Button
              SubmitButton(
                  onClick = {
                    scope.launch {
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
                  modifier = Modifier.testTag(EditOrganizerTestTags.SUBMIT_BUTTON))
            }
      }
    }
  }
}
