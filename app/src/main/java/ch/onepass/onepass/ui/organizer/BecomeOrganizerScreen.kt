package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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

@Composable
fun BecomeOrganizerScreen(
    ownerId: String,
    viewModel: BecomeOrganizerViewModel = viewModel(),
    onOrganizationCreated: () -> Unit = {}
) {
  val formState by viewModel.formState.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val scrollState = rememberScrollState()
  val snackbarHostState = remember { SnackbarHostState() }

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
              style = MaterialTheme.typography.headlineMedium,
              color = colorResource(id = R.color.on_background),
              modifier = Modifier.padding(vertical = 24.dp))

          val nameState = formState.name
          OutlinedTextField(
              value = nameState.value,
              onValueChange = { viewModel.updateName(it) },
              label = { Text("Organization Name*") },
              modifier =
                  Modifier.fillMaxWidth().onFocusChanged {
                    viewModel.onFocusChangeName(it.isFocused)
                  },
              isError = nameState.error != null)
          if (nameState.error != null)
              Text(nameState.error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
          Spacer(Modifier.height(16.dp))

          val descState = formState.description
          OutlinedTextField(
              value = descState.value,
              onValueChange = { viewModel.updateDescription(it) },
              label = { Text("Description*") },
              modifier =
                  Modifier.fillMaxWidth().height(120.dp).onFocusChanged {
                    viewModel.onFocusChangeDescription(it.isFocused)
                  },
              isError = descState.error != null)
          if (descState.error != null)
              Text(descState.error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
          Spacer(Modifier.height(16.dp))

          val emailState = formState.contactEmail
          OutlinedTextField(
              value = emailState.value,
              onValueChange = { viewModel.updateContactEmail(it) },
              label = { Text("Contact Email*") },
              modifier =
                  Modifier.fillMaxWidth().onFocusChanged {
                    viewModel.onFocusChangeEmail(it.isFocused)
                  },
              isError = emailState.error != null,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
          if (emailState.error != null)
              Text(emailState.error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
          Spacer(Modifier.height(16.dp))

          val phoneState = formState.contactPhone
          OutlinedTextField(
              value = phoneState.value,
              onValueChange = { viewModel.updateContactPhone(it) },
              label = { Text("Contact Phone") },
              modifier =
                  Modifier.fillMaxWidth().onFocusChanged {
                    viewModel.onFocusChangePhone(it.isFocused)
                  },
              isError = phoneState.error != null,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
          if (phoneState.error != null)
              Text(phoneState.error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
          Spacer(Modifier.height(16.dp))

          val websiteState = formState.website
          OutlinedTextField(
              value = websiteState.value,
              onValueChange = { viewModel.updateWebsite(it) },
              label = { Text("Website") },
              modifier =
                  Modifier.fillMaxWidth().onFocusChanged {
                    viewModel.onFocusChangeWebsite(it.isFocused)
                  },
              isError = websiteState.error != null)
          if (websiteState.error != null)
              Text(
                  websiteState.error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
          Spacer(Modifier.height(16.dp))

          OutlinedTextField(
              value = formState.instagram.value,
              onValueChange = { viewModel.updateInstagram(it) },
              label = { Text("Instagram") },
              modifier = Modifier.fillMaxWidth())
          Spacer(Modifier.height(16.dp))

          OutlinedTextField(
              value = formState.facebook.value,
              onValueChange = { viewModel.updateFacebook(it) },
              label = { Text("Facebook") },
              modifier = Modifier.fillMaxWidth())
          Spacer(Modifier.height(16.dp))

          OutlinedTextField(
              value = formState.tiktok.value,
              onValueChange = { viewModel.updateTiktok(it) },
              label = { Text("TikTok") },
              modifier = Modifier.fillMaxWidth())
          Spacer(Modifier.height(16.dp))

          OutlinedTextField(
              value = formState.address.value,
              onValueChange = { viewModel.updateAddress(it) },
              label = { Text("Address") },
              modifier = Modifier.fillMaxWidth())
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
