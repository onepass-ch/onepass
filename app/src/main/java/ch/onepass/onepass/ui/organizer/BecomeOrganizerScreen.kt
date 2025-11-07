package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.OnePassTheme

/**
 * Screen allowing users to become event organizers by submitting organization details.
 *
 * @param ownerId ID of the user becoming an organizer
 * @param viewModel ViewModel managing the form state and submission logic
 * @param onOrganizationCreated Callback invoked upon successful organization creation
 */
@Composable
fun BecomeOrganizerScreen(
    ownerId: String,
    viewModel: BecomeOrganizerViewModel = viewModel(),
    onOrganizationCreated: () -> Unit = {}
) {
  val formState by viewModel.formState.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val countryList by viewModel.countryList.collectAsState()
  val selectedCountryIndex by viewModel.selectedCountryIndex.collectAsState()
  val scrollState = rememberScrollState()
  val snackbarHostState = remember { SnackbarHostState() }
  var prefixDropdownExpanded by remember { mutableStateOf(false) }
  var prefixDisplayText by remember { mutableStateOf("Prefix*") }

  // Handle side effects based on UI state changes
  LaunchedEffect(uiState) {
    when (val state = uiState) {
      is BecomeOrganizerUiState.Success -> onOrganizationCreated()
      is BecomeOrganizerUiState.Error -> {
        snackbarHostState.showSnackbar(state.message)
        viewModel.clearError()
      }
      else -> {}
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(colorResource(id = R.color.background))
                .verticalScroll(scrollState)
                .padding(16.dp)) {
          // Screen title
          Text(
              text = "Become an Organizer",
              style = MaterialTheme.typography.headlineMedium,
              color = colorResource(id = R.color.on_background),
              modifier = Modifier.padding(vertical = 24.dp))

          // Organization name field
          FormTextField(
              value = formState.name.value,
              onValueChange = { viewModel.updateName(it) },
              label = "Organization Name*",
              isError = formState.name.error != null,
              onFocusChanged = { viewModel.onFocusChangeName(it) },
              errorMessage = formState.name.error)

          // Organization description field
          FormTextField(
              value = formState.description.value,
              onValueChange = { viewModel.updateDescription(it) },
              label = "Description*",
              isError = formState.description.error != null,
              onFocusChanged = { viewModel.onFocusChangeDescription(it) },
              maxLines = 5,
              errorMessage = formState.description.error)

          // Contact email field
          FormTextField(
              value = formState.contactEmail.value,
              onValueChange = { viewModel.updateContactEmail(it) },
              label = "Contact Email*",
              isError = formState.contactEmail.error != null,
              onFocusChanged = { viewModel.onFocusChangeEmail(it) },
              keyboardType = KeyboardType.Email,
              errorMessage = formState.contactEmail.error)

          // Phone number with country prefix selection
          PrefixPhoneRow(
              selectedCountryIndex = selectedCountryIndex,
              prefixDisplayText = prefixDisplayText,
              prefixError = formState.contactPhone.error,
              countryList = countryList,
              dropdownExpanded = prefixDropdownExpanded,
              onDropdownDismiss = { prefixDropdownExpanded = false },
              onCountrySelected = { index ->
                viewModel.updateCountryIndex(index)
                prefixDisplayText = "+${countryList[index].second}"
                prefixDropdownExpanded = false
              },
              phoneValue = formState.contactPhone.value,
              onPhoneChange = { viewModel.updateContactPhone(it) },
              onPhoneFocusChanged = { viewModel.onFocusChangePhone(it) },
              onPrefixClick = { prefixDropdownExpanded = true })

          // Optional website field
          FormTextField(
              value = formState.website.value,
              onValueChange = { viewModel.updateWebsite(it) },
              label = "Website",
              isError = formState.website.error != null,
              onFocusChanged = { viewModel.onFocusChangeWebsite(it) },
              errorMessage = formState.website.error)

          // Social media fields
          FormTextField(
              value = formState.instagram.value,
              onValueChange = { viewModel.updateInstagram(it) },
              label = "Instagram")
          FormTextField(
              value = formState.facebook.value,
              onValueChange = { viewModel.updateFacebook(it) },
              label = "Facebook")
          FormTextField(
              value = formState.tiktok.value,
              onValueChange = { viewModel.updateTiktok(it) },
              label = "TikTok")
          FormTextField(
              value = formState.address.value,
              onValueChange = { viewModel.updateAddress(it) },
              label = "Address")

          Spacer(Modifier.height(32.dp))

          SubmitButton(onClick = { viewModel.createOrganization(ownerId) }, text = "Submit")
        }

    // Loading indicator overlay
    if (uiState is BecomeOrganizerUiState.Loading) {
      Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colorResource(id = R.color.primary))
          }
    }

    // SnackBar for errors
    SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
  }
}

/** Preview of the BecomeOrganizerScreen composable. */
@Preview(showBackground = true)
@Composable
fun BecomeOrganizerScreenPreview() {
  OnePassTheme { BecomeOrganizerScreen(ownerId = "user123") }
}
