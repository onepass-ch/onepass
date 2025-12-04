package ch.onepass.onepass.ui.eventform.createform

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.eventform.EventFormFields
import ch.onepass.onepass.ui.navigation.BackNavigationScaffold
import ch.onepass.onepass.ui.theme.DefaultBackground
import ch.onepass.onepass.ui.theme.EventDateColor
import com.mapbox.maps.extension.style.expressions.dsl.generated.color

@Composable
fun EventFormScaffold(
    onNavigateBack: () -> Unit,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
  BackNavigationScaffold(
      title = "Create your Event", onBack = onNavigateBack, containerColor = DefaultBackground) {
          paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .background(DefaultBackground)
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
              .background(
                  color = colorResource(id = R.color.eventform_bg_purple),
                  shape = RoundedCornerShape(size = 5.dp))
              .background(
                  color = colorResource(id = R.color.eventform_bg_overlay),
                  shape = RoundedCornerShape(size = 5.dp))
              .border(
                  width = 1.dp,
                  color = colorResource(id = R.color.eventform_border),
                  shape = RoundedCornerShape(size = 5.dp)),
      shape = RoundedCornerShape(5.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = Color.Transparent,
              contentColor = Color.White,
              disabledContainerColor = Color.Transparent,
              disabledContentColor = Color.White.copy(alpha = 0.5f)),
      contentPadding = PaddingValues(0.dp),
      elevation = ButtonDefaults.buttonElevation(0.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(8.dp))
              Text(text = "Create event", style = MaterialTheme.typography.labelLarge)
            }
      }
}

@Composable
fun CreateEventForm(
    organizationId: String = "",
    viewModel: CreateEventFormViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onEventCreated: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()
  val scrollState = rememberScrollState()
  val snackbarHostState = remember { SnackbarHostState() }

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

      // Create Button - disabled during loading
      CreateEventButton(
          onClick = { viewModel.createEvent() }, enabled = uiState !is CreateEventUiState.Loading)
      Spacer(modifier = Modifier.height(24.dp))
    }

    if (uiState is CreateEventUiState.Loading) {
      Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EventDateColor)
          }
    }

    // Snackbar for error messages
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
  }
}
