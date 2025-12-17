package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.ui.navigation.BackNavigationScaffold
import ch.onepass.onepass.ui.navigation.TopBarConfig

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

  BackNavigationScaffold(
      TopBarConfig(
          title = "Create Organization",
      ),
      onBack = onNavigateBack,
      containerColor = colorScheme.background,
  ) { padding ->
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colorScheme.background) { scaffoldPadding ->
          Box(
              modifier =
                  Modifier.fillMaxSize()
                      .background(colorScheme.background)
                      .padding(scaffoldPadding)
                      .padding(padding)) {
                OrganizerForm(
                    formState = formState,
                    countryList = countryList,
                    prefixDisplayText = selectedCountryCode,
                    prefixError = formState.contactPhone.error,
                    dropdownExpanded = prefixDropdownExpanded,
                    onCountrySelected = {
                      viewModel.updateCountryIndex(it)
                      prefixDropdownExpanded = false
                    },
                    onPrefixClick = { prefixDropdownExpanded = true },
                    onDropdownDismiss = { prefixDropdownExpanded = false },
                    onSubmit = { viewModel.createOrganization(ownerId) },
                    submitText = "Submit",
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize())
              }
        }
  }
}
