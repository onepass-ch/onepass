package ch.onepass.onepass.ui.eventform.editform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.eventform.EventFormFields
import ch.onepass.onepass.ui.navigation.BackNavigationScaffold
import ch.onepass.onepass.ui.theme.DefaultBackground
import ch.onepass.onepass.ui.theme.EventDateColor

object EditEventFormTestTags {
  const val SCREEN = "edit_event_form_screen"
  const val LOADING_INDICATOR = "edit_loading_indicator"
  const val LOAD_ERROR_SECTION = "edit_load_error_section"
  const val FORM_COLUMN = "edit_form_column"
  const val TITLE_FIELD = "edit_title_field"
  const val DESCRIPTION_FIELD = "edit_description_field"
  const val TIME_FIELD = "edit_time_field"
  const val DATE_FIELD = "edit_date_field"
  const val LOCATION_FIELD = "edit_location_field"
  const val TICKETS_FIELD = "edit_tickets_field"
  const val UPDATE_BUTTON = "edit_update_button"
  const val ERROR_DIALOG = "edit_error_dialog"
  const val RETRY_BUTTON = "edit_retry_button"
  const val ERROR_DIALOG_OK_BUTTON = "edit_error_dialog_ok_button"
  const val BACK_BUTTON = "edit_back_button"
}

val fieldTestTags =
    mapOf(
        "title" to EditEventFormTestTags.TITLE_FIELD,
        "description" to EditEventFormTestTags.DESCRIPTION_FIELD,
        "time" to EditEventFormTestTags.TIME_FIELD,
        "date" to EditEventFormTestTags.DATE_FIELD,
        "location" to EditEventFormTestTags.LOCATION_FIELD,
        "tickets" to EditEventFormTestTags.TICKETS_FIELD
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateEventButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
  Button(
      onClick = onClick,
      modifier =
          modifier
              .fillMaxWidth()
              .height(48.dp)
              .padding(start = 63.dp, top = 14.dp, end = 63.dp, bottom = 14.dp)
              .background(
                  color = colorResource(id = R.color.edit_update_btn_bg_primary),
                  shape = RoundedCornerShape(size = 5.dp))
              .background(
                  color = colorResource(id = R.color.edit_update_btn_bg_overlay),
                  shape = RoundedCornerShape(size = 5.dp)),
      shape = RoundedCornerShape(5.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = Color.Transparent, contentColor = colorResource(id = R.color.white)),
      contentPadding = PaddingValues(0.dp),
      elevation = ButtonDefaults.buttonElevation(0.dp),
      enabled = !isLoading) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = colorResource(id = R.color.white),
                    strokeWidth = 2.dp)
              } else {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp))
              }
              Spacer(Modifier.width(8.dp))
              Text(
                  text = if (isLoading) "Updating..." else "Update event",
                  style = MaterialTheme.typography.labelLarge)
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventForm(
    eventId: String,
    viewModel: EditEventFormViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onEventUpdated: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(eventId) { viewModel.loadEvent(eventId) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is EditEventUiState.Success -> onEventUpdated()
            else -> {}
        }
    }

    BackNavigationScaffold(
        title = "Edit Event",
        onBack = onNavigateBack,
        containerColor = DefaultBackground,
        modifier = Modifier.testTag(EditEventFormTestTags.SCREEN),
        backButtonTestTag = EditEventFormTestTags.BACK_BUTTON
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is EditEventUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(EditEventFormTestTags.LOADING_INDICATOR),
                        color = EventDateColor
                    )
                }

                is EditEventUiState.LoadError -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp)
                            .testTag(EditEventFormTestTags.LOAD_ERROR_SECTION),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load event",
                            style = MaterialTheme.typography.titleMedium,
                            color = colorResource(id = R.color.white)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (uiState as EditEventUiState.LoadError).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorResource(id = R.color.gray)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadEvent(eventId) },
                            modifier = Modifier.testTag(EditEventFormTestTags.RETRY_BUTTON),
                            colors = ButtonDefaults.buttonColors(containerColor = EventDateColor)
                        ) {
                            Text("Retry")
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DefaultBackground)
                            .verticalScroll(scrollState)
                            .padding(start = 22.dp, end = 22.dp, bottom = 48.dp)
                            .testTag(EditEventFormTestTags.FORM_COLUMN)
                    ) {
                        EventFormFields(
                            viewModel = viewModel,
                            fieldTestTags = fieldTestTags
                        )

                        UpdateEventButton(
                            onClick = { viewModel.updateEvent() },
                            isLoading = uiState is EditEventUiState.Updating,
                            modifier = Modifier.testTag(EditEventFormTestTags.UPDATE_BUTTON)
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Error dialog on update failure
    if (uiState is EditEventUiState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Update Failed") },
            text = { Text((uiState as EditEventUiState.Error).message) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearError() },
                    modifier = Modifier.testTag(EditEventFormTestTags.ERROR_DIALOG_OK_BUTTON)
                ) {
                    Text("OK")
                }
            },
            modifier = Modifier.testTag(EditEventFormTestTags.ERROR_DIALOG)
        )
    }
}
