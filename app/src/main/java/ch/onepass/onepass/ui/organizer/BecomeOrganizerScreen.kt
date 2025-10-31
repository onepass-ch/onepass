package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.MarcFontFamily
import ch.onepass.onepass.ui.theme.OnePassTheme

@Composable
fun BecomeOrganizerScreen(
    ownerId: String,
    viewModel: BecomeOrganizerViewModel = viewModel(),
    onOrganizationCreated: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
  val formState by viewModel.formState.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val scrollState = rememberScrollState()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

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
          Text(
              text = "Become an Organizer",
              style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MarcFontFamily),
              color = colorResource(id = R.color.on_background),
              modifier = Modifier.padding(vertical = 16.dp))

          TextField(
              value = formState.name,
              onValueChange = { viewModel.updateName(it) },
              label = { Text("Organization Name*") },
              modifier = Modifier.fillMaxWidth(),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorResource(id = R.color.surface_container),
                      unfocusedContainerColor = colorResource(id = R.color.surface_container_low),
                      focusedTextColor = colorResource(id = R.color.on_background),
                      unfocusedTextColor = colorResource(id = R.color.on_background)),
              shape = RoundedCornerShape(8.dp))
          Spacer(Modifier.height(16.dp))

          TextField(
              value = formState.description,
              onValueChange = { viewModel.updateDescription(it) },
              label = { Text("Description*") },
              modifier = Modifier.fillMaxWidth().height(120.dp),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorResource(id = R.color.surface_container),
                      unfocusedContainerColor = colorResource(id = R.color.surface_container_low),
                      focusedTextColor = colorResource(id = R.color.on_background),
                      unfocusedTextColor = colorResource(id = R.color.on_background)),
              shape = RoundedCornerShape(8.dp))
          Spacer(Modifier.height(16.dp))

          TextField(
              value = formState.contactEmail,
              onValueChange = { viewModel.updateContactEmail(it) },
              label = { Text("Contact Email*") },
              modifier = Modifier.fillMaxWidth(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorResource(id = R.color.surface_container),
                      unfocusedContainerColor = colorResource(id = R.color.surface_container_low),
                      focusedTextColor = colorResource(id = R.color.on_background),
                      unfocusedTextColor = colorResource(id = R.color.on_background)),
              shape = RoundedCornerShape(8.dp))
          Spacer(Modifier.height(16.dp))

          TextField(
              value = formState.contactPhone,
              onValueChange = { viewModel.updateContactPhone(it) },
              label = { Text("Contact Phone") },
              modifier = Modifier.fillMaxWidth(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorResource(id = R.color.surface_container),
                      unfocusedContainerColor = colorResource(id = R.color.surface_container_low),
                      focusedTextColor = colorResource(id = R.color.on_background),
                      unfocusedTextColor = colorResource(id = R.color.on_background)),
              shape = RoundedCornerShape(8.dp))
          Spacer(Modifier.height(16.dp))

          TextField(
              value = formState.website,
              onValueChange = { viewModel.updateWebsite(it) },
              label = { Text("Website") },
              modifier = Modifier.fillMaxWidth(),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorResource(id = R.color.surface_container),
                      unfocusedContainerColor = colorResource(id = R.color.surface_container_low),
                      focusedTextColor = colorResource(id = R.color.on_background),
                      unfocusedTextColor = colorResource(id = R.color.on_background)),
              shape = RoundedCornerShape(8.dp))
          Spacer(Modifier.height(16.dp))

          TextField(
              value = formState.instagram,
              onValueChange = { viewModel.updateInstagram(it) },
              label = { Text("Instagram") },
              modifier = Modifier.fillMaxWidth(),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorResource(id = R.color.surface_container),
                      unfocusedContainerColor = colorResource(id = R.color.surface_container_low),
                      focusedTextColor = colorResource(id = R.color.on_background),
                      unfocusedTextColor = colorResource(id = R.color.on_background)),
              shape = RoundedCornerShape(8.dp))
          Spacer(Modifier.height(16.dp))

          TextField(
              value = formState.facebook,
              onValueChange = { viewModel.updateFacebook(it) },
              label = { Text("Facebook") },
              modifier = Modifier.fillMaxWidth(),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorResource(id = R.color.surface_container),
                      unfocusedContainerColor = colorResource(id = R.color.surface_container_low),
                      focusedTextColor = colorResource(id = R.color.on_background),
                      unfocusedTextColor = colorResource(id = R.color.on_background)),
              shape = RoundedCornerShape(8.dp))
          Spacer(Modifier.height(16.dp))

          TextField(
              value = formState.tiktok,
              onValueChange = { viewModel.updateTiktok(it) },
              label = { Text("TikTok") },
              modifier = Modifier.fillMaxWidth(),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorResource(id = R.color.surface_container),
                      unfocusedContainerColor = colorResource(id = R.color.surface_container_low),
                      focusedTextColor = colorResource(id = R.color.on_background),
                      unfocusedTextColor = colorResource(id = R.color.on_background)),
              shape = RoundedCornerShape(8.dp))
          Spacer(Modifier.height(16.dp))

          TextField(
              value = formState.address,
              onValueChange = { viewModel.updateAddress(it) },
              label = { Text("Address") },
              modifier = Modifier.fillMaxWidth(),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorResource(id = R.color.surface_container),
                      unfocusedContainerColor = colorResource(id = R.color.surface_container_low),
                      focusedTextColor = colorResource(id = R.color.on_background),
                      unfocusedTextColor = colorResource(id = R.color.on_background)),
              shape = RoundedCornerShape(8.dp))
          Spacer(Modifier.height(32.dp))

          Button(
              onClick = { viewModel.createOrganization(ownerId) },
              modifier = Modifier.fillMaxWidth().height(48.dp),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = colorResource(id = R.color.primary))) {
                Text("Submit")
              }
        }

    if (uiState is BecomeOrganizerUiState.Loading) {
      Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colorResource(id = R.color.primary))
          }
    }

    SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
  }
}

@Preview(showBackground = true)
@Composable
fun BecomeOrganizerScreenPreview() {
  OnePassTheme { BecomeOrganizerScreen(ownerId = "user123") }
}
