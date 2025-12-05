package ch.onepass.onepass.ui.eventform.createform

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.ui.eventform.EventFormFields
import ch.onepass.onepass.ui.theme.CardBackground
import ch.onepass.onepass.ui.theme.EventBorderGray
import ch.onepass.onepass.ui.theme.EventDateColor
import ch.onepass.onepass.ui.theme.OnBackground
import ch.onepass.onepass.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScaffold(
    onNavigateBack: () -> Unit,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(text = "Create your Event", color = OnBackground) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnBackground)
              }
            })
      }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .verticalScroll(scrollState)
                      .padding(start = 22.dp, end = 22.dp, bottom = 48.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
              }
        }
      }
}

@Composable
fun CreateEventButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Button(
      onClick = onClick,
      modifier =
          modifier
              .fillMaxWidth()
              .height(48.dp)
              .padding(start = 63.dp, top = 14.dp, end = 63.dp, bottom = 14.dp)
              .background(color = Secondary, shape = RoundedCornerShape(size = 5.dp))
              .background(color = CardBackground, shape = RoundedCornerShape(size = 5.dp))
              .border(
                  width = 1.dp, color = EventBorderGray, shape = RoundedCornerShape(size = 5.dp)),
      shape = RoundedCornerShape(5.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = Color.Transparent, contentColor = OnBackground),
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

  LaunchedEffect(Unit) { viewModel.setOrganizationId(organizationId) }

  LaunchedEffect(uiState) {
    when (uiState) {
      is CreateEventUiState.Success -> {
        onEventCreated()
        viewModel.resetForm()
      }
      is CreateEventUiState.Error -> {
        viewModel.clearError()
      }
      else -> {}
    }
  }

  EventFormScaffold(onNavigateBack, scrollState) {
    // Use the shared EventFormFields composable
    EventFormFields(viewModel = viewModel)

    // Create Button
    CreateEventButton(onClick = { viewModel.createEvent() })
    Spacer(modifier = Modifier.height(24.dp))
  }

  if (uiState is CreateEventUiState.Loading) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = EventDateColor)
        }
  }
}
