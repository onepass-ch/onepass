package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/**
 * Composable screen for editing an existing organization's details.
 *
 * @param organizationId ID of the organization to edit
 * @param viewModel ViewModel managing the organization's data and update logic
 * @param formViewModel ViewModel managing the form state
 * @param onOrganizationUpdated Callback invoked when the organization is successfully updated
 * @param onNavigateBack Callback to navigate back to the previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrganizationScreen(
    organizationId: String,
    viewModel: OrganizationEditorViewModel = viewModel(),
    formViewModel: OrganizationFormViewModel = viewModel(),
    onOrganizationUpdated: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  val formState by formViewModel.formState.collectAsState()
  val countryList by formViewModel.countryList.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var prefixDropdownExpanded by remember { mutableStateOf(false) }

  // Load organization data when the screen is first displayed
  LaunchedEffect(organizationId) { viewModel.loadOrganizationById(organizationId) }

  LaunchedEffect(uiState.organization) {
    uiState.organization?.let { org ->
      // Update the form state with the organization values.
      // For each field, we copy the existing form state and set the value from `org`.
      formViewModel.updateFormState(
          formViewModel.formState.value.copy(
              name = formViewModel.formState.value.name.copy(value = org.name),
              description = formViewModel.formState.value.description.copy(value = org.description),
              contactEmail =
                  formViewModel.formState.value.contactEmail.copy(value = org.contactEmail ?: ""),
              contactPhone =
                  formViewModel.formState.value.contactPhone.copy(
                      value = org.contactPhone?.removePrefix(org.phonePrefix ?: "") ?: ""),
              website = formViewModel.formState.value.website.copy(value = org.website ?: ""),
              instagram = formViewModel.formState.value.instagram.copy(value = org.instagram ?: ""),
              facebook = formViewModel.formState.value.facebook.copy(value = org.facebook ?: ""),
              tiktok = formViewModel.formState.value.tiktok.copy(value = org.tiktok ?: ""),
              address = formViewModel.formState.value.address.copy(value = org.address ?: "")))

      // Set only prefix separately
      formViewModel.formState.value.contactPhonePrefix.value = org.phonePrefix ?: ""
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
  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Edit Organization", color = colorScheme.onBackground) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onBackground)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background))
      },
      containerColor = colorScheme.background,
      snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(colorScheme.background).padding(padding)) {
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
                prefixDisplayText = formState.contactPhonePrefix.value,
                prefixError = formState.contactPhone.error,
                dropdownExpanded = prefixDropdownExpanded,
                onCountrySelected = {
                  formViewModel.updateCountryIndex(it)
                  formState.contactPhonePrefix.value = "+${countryList[it].second}"
                  prefixDropdownExpanded = false
                },
                onPrefixClick = { prefixDropdownExpanded = true },
                onDropdownDismiss = { prefixDropdownExpanded = false },
                onSubmit = {
                  val data = OrganizationEditorData.fromForm(organizationId, formState)
                  viewModel.updateOrganization(data)
                },
                submitText = "Update",
                viewModel = formViewModel,
                isLoading = uiState.isLoading,
                modifier = Modifier.padding(padding))
          }
        }
      }
}
