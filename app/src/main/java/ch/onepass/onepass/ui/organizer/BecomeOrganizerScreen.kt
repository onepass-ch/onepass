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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.OnePassTheme

/** Test tags for BecomeOrganizer screen components */
object BecomeOrganizerTestTags {
  const val NAME_FIELD = "BecomeOrgNameField"
  const val DESCRIPTION_FIELD = "BecomeOrgDescriptionField"
  const val EMAIL_FIELD = "BecomeOrgEmailField"
  const val PHONE_FIELD = "BecomeOrgPhoneField"
  const val WEBSITE_FIELD = "BecomeOrgWebsiteField"
  const val INSTAGRAM_FIELD = "BecomeOrgInstagramField"
  const val FACEBOOK_FIELD = "BecomeOrgFacebookField"
  const val TIKTOK_FIELD = "BecomeOrgTiktokField"
  const val ADDRESS_FIELD = "BecomeOrgAddressField"
  const val SUBMIT_BUTTON = "BecomeOrgSubmitButton"
  const val PREFIX_DROPDOWN = "BecomeOrgPrefixDropdown"
  const val SNACKBAR = "BecomeOrgSnackbar"
}

/**
 * Composable screen for users to fill out a form to become an event organizer.
 *
 * @param ownerId ID of the user who wants to become an organizer
 * @param viewModel ViewModel managing the form state and submission logic
 * @param onOrganizationCreated Callback invoked when the organization is successfully created
 */
@Composable
fun BecomeOrganizerScreen(
    ownerId: String,
    viewModel: BecomeOrganizerViewModel = viewModel(),
    onOrganizationCreated: (String) -> Unit = {}
) {
  val formState by viewModel.formState.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val countryList by viewModel.countryList.collectAsState()
  val selectedCountryCode by viewModel.selectedCountryCode.collectAsState()
  val scrollState = rememberScrollState()
  val snackbarHostState = remember { SnackbarHostState() }
  var prefixDropdownExpanded by remember { mutableStateOf(false) }

  // Handle successful organization creation
  LaunchedEffect(uiState.successOrganizationId) {
    uiState.successOrganizationId?.let { newOrgId ->
      onOrganizationCreated(newOrgId)
      viewModel.clearSuccess()
    }
  }

  // Handle error messages
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearError()
    }
  }

  Scaffold(
      snackbarHost = {
        // Snackbar host to show error messages
        SnackbarHost(snackbarHostState, Modifier.testTag(BecomeOrganizerTestTags.SNACKBAR))
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .padding(paddingValues)) {
              Text(
                  text = "Become an Organizer",
                  style = MaterialTheme.typography.headlineMedium,
                  color = colorResource(id = R.color.on_background),
                  modifier = Modifier.padding(vertical = 24.dp))

              // Organization Name Field
              FormTextField(
                  value = formState.name.value,
                  onValueChange = viewModel::updateName,
                  label = "Organization Name*",
                  isError = formState.name.error != null,
                  onFocusChanged = viewModel::onFocusChangeName,
                  errorMessage = formState.name.error,
                  testTag = BecomeOrganizerTestTags.NAME_FIELD)

              // Organization Description Field
              FormTextField(
                  value = formState.description.value,
                  onValueChange = viewModel::updateDescription,
                  label = "Description*",
                  isError = formState.description.error != null,
                  onFocusChanged = viewModel::onFocusChangeDescription,
                  maxLines = 5,
                  errorMessage = formState.description.error,
                  testTag = BecomeOrganizerTestTags.DESCRIPTION_FIELD)

              // Contact Email Field
              FormTextField(
                  value = formState.contactEmail.value,
                  onValueChange = viewModel::updateContactEmail,
                  label = "Contact Email",
                  isError = formState.contactEmail.error != null,
                  onFocusChanged = viewModel::onFocusChangeEmail,
                  keyboardType = KeyboardType.Email,
                  errorMessage = formState.contactEmail.error,
                  testTag = BecomeOrganizerTestTags.EMAIL_FIELD)

              // Contact Phone Field with Prefix Dropdown
              PrefixPhoneRow(
                  prefixDisplayText = selectedCountryCode,
                  prefixError = formState.contactPhone.error,
                  countryList = countryList,
                  dropdownExpanded = prefixDropdownExpanded,
                  onDropdownDismiss = { prefixDropdownExpanded = false },
                  onCountrySelected = { index ->
                    viewModel.updateCountryIndex(index)
                    prefixDropdownExpanded = false
                  },
                  phoneValue = formState.contactPhone.value,
                  onPhoneChange = viewModel::updateContactPhone,
                  onPhoneFocusChanged = viewModel::onFocusChangePhone,
                  onPrefixClick = { prefixDropdownExpanded = true },
                  phoneTestTag = BecomeOrganizerTestTags.PHONE_FIELD,
                  prefixTestTag = BecomeOrganizerTestTags.PREFIX_DROPDOWN)

              // Website Field
              FormTextField(
                  value = formState.website.value,
                  onValueChange = viewModel::updateWebsite,
                  label = "Website",
                  isError = formState.website.error != null,
                  onFocusChanged = viewModel::onFocusChangeWebsite,
                  errorMessage = formState.website.error,
                  testTag = BecomeOrganizerTestTags.WEBSITE_FIELD)

              // Social Media Fields
              FormTextField(
                  value = formState.instagram.value,
                  onValueChange = viewModel::updateInstagram,
                  label = "Instagram",
                  testTag = BecomeOrganizerTestTags.INSTAGRAM_FIELD)
              FormTextField(
                  value = formState.facebook.value,
                  onValueChange = viewModel::updateFacebook,
                  label = "Facebook",
                  testTag = BecomeOrganizerTestTags.FACEBOOK_FIELD)
              FormTextField(
                  value = formState.tiktok.value,
                  onValueChange = viewModel::updateTiktok,
                  label = "TikTok",
                  testTag = BecomeOrganizerTestTags.TIKTOK_FIELD)

              // Address Field
              FormTextField(
                  value = formState.address.value,
                  onValueChange = viewModel::updateAddress,
                  label = "Address",
                  testTag = BecomeOrganizerTestTags.ADDRESS_FIELD)

              Spacer(Modifier.height(32.dp))

              // Submit Button
              SubmitButton(
                  onClick = { viewModel.createOrganization(ownerId) },
                  text = "Submit",
                  modifier = Modifier.testTag(BecomeOrganizerTestTags.SUBMIT_BUTTON))
            }
      }
}

@Preview(showBackground = true)
@Composable
fun BecomeOrganizerScreenPreview() {
  OnePassTheme { BecomeOrganizerScreen(ownerId = "user123") }
}
