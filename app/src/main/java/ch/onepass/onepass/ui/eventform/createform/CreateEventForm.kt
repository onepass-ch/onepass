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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.map.LocationRepository
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.model.storage.StorageRepository
import ch.onepass.onepass.ui.eventform.EventFormFields
import ch.onepass.onepass.ui.eventform.EventFormViewModel
import ch.onepass.onepass.ui.theme.DefaultBackground
import ch.onepass.onepass.ui.theme.EventDateColor
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import android.net.Uri

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
            title = { Text(text = "Create your Event", color = Color.White) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DefaultBackground))
      }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
fun CreateEventButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Button(
      onClick = onClick,
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
              containerColor = Color.Transparent, contentColor = Color.White),
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

@Preview(showBackground = true)
@Composable
private fun CreateEventFormPreview() {
  val previewViewModel = remember { PreviewCreateEventFormViewModel() }
  CreateEventForm(
      organizationId = PreviewCreateEventFormViewModel.PREVIEW_ORG_ID,
      viewModel = previewViewModel,
      onNavigateBack = {},
      onEventCreated = {})
}

private class PreviewCreateEventFormViewModel :
    CreateEventFormViewModel(
        eventRepository = PreviewEventRepository(),
        organizationRepository = PreviewOrganizationRepository(),
        locationRepository = PreviewLocationRepository(),
        storageRepository = PreviewStorageRepository()) {

  init {
    setOrganizationId(PREVIEW_ORG_ID)
    _formState.value =
        EventFormViewModel.EventFormState(
            title = "Student Welcome Party",
            description = "Celebrate the start of the semester with food, music, and networking.",
            startTime = "18:00",
            endTime = "22:00",
            date = "12/09/2025",
            location = "Innovation Hall",
            price = "25",
            capacity = "150",
            selectedLocation = Location(name = "Innovation Hall", region = "Lausanne"))
  }

  companion object {
    const val PREVIEW_ORG_ID = "preview-org"
  }
}

private class PreviewEventRepository : EventRepository {
  override fun getAllEvents(): Flow<List<Event>> = flowOf(emptyList())

  override fun searchEvents(query: String): Flow<List<Event>> = flowOf(emptyList())

  override fun getEventById(eventId: String): Flow<Event?> = flowOf(null)

  override fun getEventsByOrganization(orgId: String): Flow<List<Event>> = flowOf(emptyList())

  override fun getEventsByLocation(center: Location, radiusKm: Double): Flow<List<Event>> =
      flowOf(emptyList())

  override fun getEventsByTag(tag: String): Flow<List<Event>> = flowOf(emptyList())

  override fun getFeaturedEvents(): Flow<List<Event>> = flowOf(emptyList())

  override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> = flowOf(emptyList())

  override suspend fun createEvent(event: Event): Result<String> =
      Result.success("preview-event-id")

  override suspend fun updateEvent(event: Event): Result<Unit> = Result.success(Unit)

  override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)

  override suspend fun addEventImage(eventId: String, imageUrl: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun removeEventImage(eventId: String, imageUrl: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun updateEventImages(eventId: String, imageUrls: List<String>): Result<Unit> =
      Result.success(Unit)
}

private class PreviewOrganizationRepository : OrganizationRepository {
  private val previewOrganization =
      Organization(
          id = PreviewCreateEventFormViewModel.PREVIEW_ORG_ID,
          name = "OnePass Crew",
          description = "We build unforgettable campus experiences.",
          ownerId = "preview-owner",
          status = OrganizationStatus.ACTIVE,
          verified = true,
          website = "https://onepass.app")

  override suspend fun createOrganization(organization: Organization): Result<String> =
      Result.success(organization.id)

  override suspend fun updateOrganization(organization: Organization): Result<Unit> =
      Result.success(Unit)

  override suspend fun deleteOrganization(organizationId: String): Result<Unit> =
      Result.success(Unit)

  override fun getOrganizationById(organizationId: String): Flow<Organization?> =
      flowOf(previewOrganization)

  override fun getOrganizationsByOwner(ownerId: String): Flow<List<Organization>> =
      flowOf(listOf(previewOrganization))

  override fun getOrganizationsByMember(userId: String): Flow<List<Organization>> =
      flowOf(listOf(previewOrganization))

  override fun getOrganizationsByStatus(status: OrganizationStatus): Flow<List<Organization>> =
      flowOf(listOf(previewOrganization))

  override fun searchOrganizations(query: String): Flow<List<Organization>> =
      flowOf(listOf(previewOrganization))

  override fun getVerifiedOrganizations(): Flow<List<Organization>> =
      flowOf(listOf(previewOrganization))

  override suspend fun addMember(
      organizationId: String,
      userId: String,
      role: OrganizationRole
  ): Result<Unit> = Result.success(Unit)

  override suspend fun removeMember(organizationId: String, userId: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun updateMemberRole(
      organizationId: String,
      userId: String,
      newRole: OrganizationRole
  ): Result<Unit> = Result.success(Unit)

  override suspend fun createInvitation(invitation: OrganizationInvitation): Result<String> =
      Result.success(invitation.id)

  override fun getPendingInvitations(organizationId: String): Flow<List<OrganizationInvitation>> =
      flowOf(emptyList())

  override fun getInvitationsByEmail(email: String): Flow<List<OrganizationInvitation>> =
      flowOf(emptyList())

  override suspend fun updateInvitationStatus(
      invitationId: String,
      newStatus: InvitationStatus
  ): Result<Unit> = Result.success(Unit)

  override suspend fun deleteInvitation(invitationId: String): Result<Unit> = Result.success(Unit)

  override suspend fun updateProfileImage(organizationId: String, imageUrl: String?): Result<Unit> =
      Result.success(Unit)
}

private class PreviewLocationRepository : LocationRepository {
  override suspend fun search(query: String): List<Location> =
      listOf(Location(name = "Innovation Hall", region = "Lausanne"))

  override suspend fun reverseGeocode(latitude: Double, longitude: Double): Location? = null
}

private class PreviewStorageRepository : StorageRepository {
  override suspend fun uploadImage(
      uri: Uri,
      path: String,
      onProgress: ((Float) -> Unit)?
  ): Result<String> = Result.success("https://preview-image-url.com/image.jpg")

  override suspend fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

  override suspend fun deleteImageByUrl(downloadUrl: String): Result<Unit> = Result.success(Unit)

  override suspend fun getDownloadUrl(path: String): Result<String> =
      Result.success("https://preview-image-url.com/image.jpg")

  override suspend fun deleteDirectory(directoryPath: String): Result<Int> = Result.success(0)
}
