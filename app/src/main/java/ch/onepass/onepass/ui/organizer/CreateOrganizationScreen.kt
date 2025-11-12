package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/** Test tags for CreateOrganization screen components */
object CreateOrganizationTestTags {
  const val NAME_FIELD = "CreateOrganizationNameField"
  const val DESCRIPTION_FIELD = "CreateOrganizationDescriptionField"
  const val EMAIL_FIELD = "CreateOrganizationEmailField"
  const val PHONE_FIELD = "CreateOrganizationPhoneField"
  const val WEBSITE_FIELD = "CreateOrganizationWebsiteField"
  const val INSTAGRAM_FIELD = "CreateOrganizationInstagramField"
  const val FACEBOOK_FIELD = "CreateOrganizationFacebookField"
  const val TIKTOK_FIELD = "CreateOrganizationTiktokField"
  const val ADDRESS_FIELD = "CreateOrganizationAddressField"
  const val SUBMIT_BUTTON = "CreateOrganizationSubmitButton"
  const val PREFIX_DROPDOWN = "CreateOrganizationPrefixDropdown"
  const val SNACKBAR = "CreateOrganizationSnackbar"
}

/**
 * Composable screen for users to fill out a form to create a new organization.
 *
 * @param ownerId ID of the user who wants to create an organization
 * @param viewModel ViewModel managing the form state and submission logic
 * @param onOrganizationCreated Callback invoked when the organization is successfully created
 */
@Composable
fun CreateOrganizationScreen(
    ownerId: String,
    viewModel: OrganizationFormViewModel = viewModel(),
    onOrganizationCreated: (String) -> Unit = {}
) {
  val formState by viewModel.formState.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val countryList by viewModel.countryList.collectAsState()
  val selectedCountryCode by viewModel.selectedCountryCode.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  var prefixDropdownExpanded by remember { mutableStateOf(false) }

  // Handle success and error events
  LaunchedEffect(uiState.successOrganizationId) {
    uiState.successOrganizationId?.let {
      onOrganizationCreated(it)
      viewModel.clearSuccess()
    }
  }

  // Show error snackbar
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearError()
    }
  }

  // Main content
  Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    OrganizerForm(
        title = "Create an Organization",
        formState = formState,
        countryList = countryList,
        prefixDisplayText = selectedCountryCode,
        prefixError = formState.contactPhone.error,
        dropdownExpanded = prefixDropdownExpanded,
        onCountrySelected = {
          // Update country code in ViewModel when a country is selected
          viewModel.updateCountryIndex(it)
          prefixDropdownExpanded = false
        },
        onPrefixClick = { prefixDropdownExpanded = true },
        onDropdownDismiss = { prefixDropdownExpanded = false },
        onSubmit = { viewModel.createOrganization(ownerId) },
        submitText = "Submit",
        testTags = CreateOrganizationTestTags,
        viewModel = viewModel,
        modifier = Modifier.padding(padding))
  }
}
