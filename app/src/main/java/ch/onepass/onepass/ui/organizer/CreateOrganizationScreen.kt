package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R

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
}

/**
 * Composable screen for users to fill out a form to create a new organization.
 *
 * @param ownerId ID of the user who wants to create an organization
 * @param viewModel ViewModel managing the form state and submission logic
 * @param onOrganizationCreated Callback invoked when the organization is successfully created
 * @param onNavigateBack Callback to navigate back to the previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrganizationScreen(
    ownerId: String,
    viewModel: OrganizationFormViewModel = viewModel(),
    onOrganizationCreated: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
  val formState by viewModel.formState.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val countryList by viewModel.countryList.collectAsState()
  val selectedCountryCode by viewModel.selectedCountryCode.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  var prefixDropdownExpanded by remember { mutableStateOf(false) }

  // Reset form when the screen is first displayed
  LaunchedEffect(Unit) { viewModel.resetForm() }

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
  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text("Create Organization", color = colorResource(id = R.color.on_background))
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorResource(id = R.color.on_background))
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.background)))
      },
      containerColor = colorResource(id = R.color.background),
      snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(colorResource(id = R.color.background))
                    .padding(padding)) {
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
}
