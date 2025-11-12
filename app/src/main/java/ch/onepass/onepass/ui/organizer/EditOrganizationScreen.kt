package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/** Test tags for EditOrganizationScreen components */
object EditOrganizationTestTags {
  const val NAME_FIELD = "EditOrganizationNameField"
  const val DESCRIPTION_FIELD = "EditOrganizationDescriptionField"
  const val EMAIL_FIELD = "EditOrganizationEmailField"
  const val PHONE_FIELD = "EditOrganizationPhoneField"
  const val WEBSITE_FIELD = "EditOrganizationWebsiteField"
  const val INSTAGRAM_FIELD = "EditOrganizationInstagramField"
  const val FACEBOOK_FIELD = "EditOrganizationFacebookField"
  const val TIKTOK_FIELD = "EditOrganizationTiktokField"
  const val ADDRESS_FIELD = "EditOrganizationAddressField"
  const val SUBMIT_BUTTON = "EditOrganizationSubmitButton"
  const val PREFIX_DROPDOWN = "EditOrganizationPrefixDropdown"
  const val SNACKBAR = "EditOrganizationSnackbar"
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
    viewModel: OrganizationEditorViewModel = viewModel(),
    formViewModel: OrganizationFormViewModel = viewModel(),
    onOrganizationUpdated: () -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  val formState by formViewModel.formState.collectAsState()
  val countryList by formViewModel.countryList.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var prefixDropdownExpanded by remember { mutableStateOf(false) }
  var prefixDisplayText by remember { mutableStateOf("") }

  // Load organization data when the screen is first displayed
  LaunchedEffect(organizationId) { viewModel.loadOrganizationById(organizationId) }

  // Initialize form fields when organization data is loaded
  LaunchedEffect(uiState.organization) {
    uiState.organization?.let { org ->
      formViewModel.initializeFrom(org)
      org.contactPhone?.let { prefixDisplayText = it.takeWhile { c -> c == '+' || c.isDigit() } }
    }
  }

  // Handle success and error events
  LaunchedEffect(uiState.success, uiState.errorMessage) {
    if (uiState.success) {
      onOrganizationUpdated()
      viewModel.clearSuccessFlag()
    }
    uiState.errorMessage?.let {
      scope.launch { snackbarHostState.showSnackbar(it) }
      viewModel.clearError()
    }
  }

  // Main content
  Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    if (uiState.isLoading && uiState.organization == null) {
      // Show loading indicator while fetching organization data
      Box(Modifier.fillMaxSize().padding(padding)) {
        CircularProgressIndicator(Modifier.padding(32.dp))
      }
    } else if (uiState.organization != null) {
      // Show the organization edit form
      OrganizerForm(
          title = "Edit Organization",
          formState = formState,
          countryList = countryList,
          prefixDisplayText = prefixDisplayText,
          prefixError = formState.contactPhone.error,
          dropdownExpanded = prefixDropdownExpanded,
          onCountrySelected = {
            formViewModel.updateCountryIndex(it)
            prefixDisplayText = "+${countryList[it].second}"
            prefixDropdownExpanded = false
          },
          onPrefixClick = { prefixDropdownExpanded = true },
          onDropdownDismiss = { prefixDropdownExpanded = false },
          onSubmit = {
            val data = OrganizationEditorData.fromForm(organizationId, formState)
            viewModel.updateOrganization(data)
          },
          submitText = "Update",
          testTags = EditOrganizationTestTags,
          viewModel = formViewModel,
          modifier = Modifier.padding(padding))
    }
  }
}
