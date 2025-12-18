package ch.onepass.onepass.ui.eventform.createform

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.eventform.EventFormFields
import ch.onepass.onepass.ui.navigation.BackNavigationScaffold
import ch.onepass.onepass.ui.navigation.TopBarConfig

@Composable
fun EventFormScaffold(
    onNavigateBack: () -> Unit,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
  BackNavigationScaffold(
      topBarConfig = TopBarConfig(title = stringResource(R.string.create_event_title)),
      onBack = onNavigateBack,
      containerColor = colorScheme.background) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .background(colorScheme.background)
                      .verticalScroll(scrollState)
                      .padding(start = 22.dp, end = 22.dp, bottom = 48.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
              }
        }
      }
}

@Composable
fun CreateEventButton(modifier: Modifier = Modifier, onClick: () -> Unit, enabled: Boolean = true) {
  Button(
      onClick = onClick,
      enabled = enabled,
      modifier =
          modifier
              .fillMaxWidth()
              .height(48.dp)
              .background(color = colorScheme.background, shape = RoundedCornerShape(size = 5.dp)),
      shape = RoundedCornerShape(5.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = Color.Transparent,
              contentColor = colorScheme.onBackground,
              disabledContainerColor = Color.Transparent,
              disabledContentColor = colorScheme.onBackground.copy(alpha = 0.5f)),
      contentPadding = PaddingValues(0.dp),
      elevation = ButtonDefaults.buttonElevation(0.dp)) {
        Row(
            modifier =
                Modifier.fillMaxSize().background(colorScheme.primary).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(8.dp))
              Text(
                  text = stringResource(R.string.create_event_button),
                  style = MaterialTheme.typography.labelLarge)
            }
      }
}

@Composable
fun CreateEventForm(
    organizationId: String = "",
    viewModel: CreateEventFormViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onEventCreated: () -> Unit = {},
    allowExactTime: Boolean = false
) {
  val uiState by viewModel.uiState.collectAsState()
  val scrollState = rememberScrollState()
  val snackbarHostState = remember { SnackbarHostState() }

  viewModel.updateAllowExactTime(allowExactTime)

  LaunchedEffect(Unit) { viewModel.setOrganizationId(organizationId) }

  LaunchedEffect(uiState) {
    when (val state = uiState) {
      is CreateEventUiState.Success -> {
        onEventCreated()
        viewModel.resetForm()
      }
      is CreateEventUiState.Error -> {
        snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Long)
        viewModel.clearError()
      }
      else -> {}
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    EventFormScaffold(onNavigateBack, scrollState) {
      // Use the shared EventFormFields composable
      EventFormFields(viewModel = viewModel)

      // Create Button - disabled during loading and while success is being handled
      val isSubmitting =
          uiState is CreateEventUiState.Loading || uiState is CreateEventUiState.Success
      CreateEventButton(onClick = { viewModel.createEvent() }, enabled = !isSubmitting)
      Spacer(modifier = Modifier.height(24.dp))
    }

    if (uiState is CreateEventUiState.Loading) {
      Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colorScheme.primary)
          }
    }

    // Snackbar for error messages
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
  }
}
