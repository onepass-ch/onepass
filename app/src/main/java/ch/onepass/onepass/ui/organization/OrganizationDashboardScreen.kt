package ch.onepass.onepass.ui.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.ui.components.common.ErrorState
import ch.onepass.onepass.ui.components.common.LoadingState
import ch.onepass.onepass.ui.navigation.BackNavigationScaffold
import ch.onepass.onepass.ui.navigation.TopBarConfig
import ch.onepass.onepass.ui.theme.Success
import ch.onepass.onepass.ui.theme.UnSelected
import ch.onepass.onepass.ui.theme.Warning
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.util.Locale

/** Test tags for identifying composables in UI tests for the Organization Dashboard screen. */
object OrganizationDashboardTestTags {
  const val SCREEN = "org_dashboard_screen"
  const val BACK_BUTTON = "org_dashboard_back_button"
  const val TITLE = "org_dashboard_title"
  const val ORG_SUMMARY_CARD = "org_dashboard_summary_card"
  const val ORG_NAME = "org_dashboard_org_name"
  const val ORG_FOLLOWERS = "org_dashboard_followers"
  const val ORG_RATING = "org_dashboard_rating"
  const val MANAGE_EVENTS_SECTION = "org_dashboard_manage_events"
  const val CREATE_EVENT_BUTTON = "org_dashboard_create_event_button"
  const val YOUR_EVENTS_DROPDOWN = "org_dashboard_your_events_dropdown"
  const val EVENT_CARD = "org_dashboard_event_card"
  const val EVENT_SCAN_BUTTON = "org_dashboard_event_scan_button"
  const val EVENT_EDIT_BUTTON = "org_dashboard_event_edit_button"
  const val MANAGE_STAFF_SECTION = "org_dashboard_manage_staff"
  const val ADD_STAFF_BUTTON = "org_dashboard_add_staff_button"
  const val STAFF_LIST_DROPDOWN = "org_dashboard_staff_list_dropdown"
  const val STAFF_ITEM = "org_dashboard_staff_item"
  const val STAFF_REMOVE_BUTTON = "org_dashboard_staff_remove_button"
  const val LOADING_INDICATOR = "org_dashboard_loading"
  const val ERROR_MESSAGE = "org_dashboard_error"

  fun getEventCardTag(eventId: String?) = "org_dashboard_event_card_$eventId"

  fun getEventScanButtonTag(eventId: String) = "org_dashboard_event_scan_${eventId}"

  fun getEventEditButtonTag(eventId: String) = "org_dashboard_event_edit_${eventId}"

  fun getStaffItemTag(userId: String) = "org_dashboard_staff_item_$userId"

  fun getStaffRemoveButtonTag(userId: String) = "org_dashboard_staff_remove_$userId"
}

/**
 * The main screen for managing an organization's dashboard.
 *
 * This screen displays a summary of the organization, provides navigation to create events and add
 * staff, and shows expandable lists for managing existing events and staff members.
 *
 * @param organizationId The unique identifier of the organization to display.
 * @param modifier Optional [Modifier] for layout adjustments.
 * @param onNavigateBack Callback invoked when the back button is pressed.
 * @param onNavigateToProfile Callback invoked when the summary card is clicked. Passes the
 *   `organizationId`.
 * @param onNavigateToCreateEvent Callback invoked when the "Create new event" button is clicked.
 *   Passes the `organizationId`.
 * @param onNavigateToAddStaff Callback invoked when the "Add new staff" button is clicked. Passes
 *   the `organizationId`.
 * @param onNavigateToScanTickets Callback invoked when an event's "Scan" button is clicked. Passes
 *   the `eventId`.
 * @param onNavigateToEditEvent Callback invoked when an event's "Edit" button is clicked. Passes
 *   the `eventId`.
 * @param viewModel The [OrganizationDashboardViewModel] responsible for fetching and managing the
 *   dashboard data.
 */
@Composable
fun OrganizationDashboardScreen(
    organizationId: String,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToCreateEvent: (String) -> Unit = {},
    onNavigateToAddStaff: (String) -> Unit = {},
    onNavigateToScanTickets: (String) -> Unit = {},
    onNavigateToEditEvent: (String) -> Unit = {},
    viewModel: OrganizationDashboardViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(organizationId) { viewModel.loadOrganization(organizationId) }

  BackNavigationScaffold(
      TopBarConfig(
          title = "DASHBOARD",
          titleTestTag = OrganizationDashboardTestTags.TITLE,
          backButtonTestTag = OrganizationDashboardTestTags.BACK_BUTTON),
      onBack = onNavigateBack,
      modifier = modifier.testTag(OrganizationDashboardTestTags.SCREEN)) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          when {
            uiState.isLoading -> {
              LoadingState(
                  modifier = Modifier.align(Alignment.Center),
                  testTag = OrganizationDashboardTestTags.LOADING_INDICATOR)
            }
            uiState.error != null -> {
              ErrorState(
                  error = uiState.error!!,
                  onRetry = { viewModel.loadOrganization(organizationId) },
                  modifier = Modifier.align(Alignment.Center),
                  testTag = OrganizationDashboardTestTags.ERROR_MESSAGE)
            }
            uiState.organization != null -> {
              DashboardContent(
                  uiState = uiState,
                  onNavigateToProfile = onNavigateToProfile,
                  onNavigateToCreateEvent = onNavigateToCreateEvent,
                  onNavigateToAddStaff = onNavigateToAddStaff,
                  onNavigateToScanTickets = onNavigateToScanTickets,
                  onNavigateToEditEvent = onNavigateToEditEvent,
                  onRemoveStaff = { userId -> viewModel.removeStaffMember(userId) })
            }
          }
        }
      }
}

/**
 * The main scrollable content of the dashboard.
 *
 * @param uiState The current state of the dashboard, containing organization, events, and staff
 *   info.
 * @param onNavigateToProfile Callback for navigating to the organization's profile.
 * @param onNavigateToCreateEvent Callback for navigating to the event creation screen.
 * @param onNavigateToAddStaff Callback for navigating to the add staff screen.
 * @param onNavigateToScanTickets Callback for navigating to the ticket scanning screen.
 * @param onNavigateToEditEvent Callback for navigating to the event editing screen.
 * @param onRemoveStaff Callback for removing a staff member.
 */
@Composable
private fun DashboardContent(
    uiState: OrganizationDashboardUiState,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToCreateEvent: (String) -> Unit,
    onNavigateToAddStaff: (String) -> Unit,
    onNavigateToScanTickets: (String) -> Unit,
    onNavigateToEditEvent: (String) -> Unit,
    onRemoveStaff: (String) -> Unit
) {
  val organization = uiState.organization ?: return
  val scrollState = rememberScrollState()

  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(scrollState)
              .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Organization Card (using standard component)
        OrganizationCard(
            organization = organization, onClick = { onNavigateToProfile(organization.id) })

        // Manage Events Section
        ManageEventsSection(
            events = uiState.events,
            currentUserRole = uiState.currentUserRole,
            onCreateEvent = { onNavigateToCreateEvent(organization.id) },
            onScanTickets = onNavigateToScanTickets,
            onEditEvent = onNavigateToEditEvent)

        // Manage Staff Section
        ManageStaffSection(
            staffMembers = uiState.staffMembers,
            currentUserRole = uiState.currentUserRole,
            onAddStaff = { onNavigateToAddStaff(organization.id) },
            onRemoveStaff = onRemoveStaff)
      }
}

/**
 * Displays the "Manage Events" section, including the "Create new event" button and the expandable
 * "Your events" list.
 *
 * @param events The list of events for the organization.
 * @param currentUserRole The role of the currently logged-in user.
 * @param onCreateEvent Callback for the "Create new event" button.
 * @param onScanTickets Callback for the "Scan" button on an event card.
 * @param onEditEvent Callback for the "Edit" button on an event card.
 */
@Composable
private fun ManageEventsSection(
    events: List<Event>,
    currentUserRole: OrganizationRole?,
    onCreateEvent: () -> Unit,
    onScanTickets: (String) -> Unit,
    onEditEvent: (String) -> Unit
) {
  var eventsExpanded by remember { mutableStateOf(false) }

  Column(
      modifier =
          Modifier.fillMaxWidth().testTag(OrganizationDashboardTestTags.MANAGE_EVENTS_SECTION)) {
        Text(
            text = "MANAGE EVENTS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface)

        Spacer(modifier = Modifier.height(8.dp))

        // Create New Event Button
        Button(
            onClick = onCreateEvent,
            modifier =
                Modifier.fillMaxWidth().testTag(OrganizationDashboardTestTags.CREATE_EVENT_BUTTON),
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
            shape = RoundedCornerShape(6.dp)) {
              Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = "Create",
                  modifier = Modifier.size(20.dp),
                  tint = colorScheme.onBackground)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                  text = "Create new event",
                  style = MaterialTheme.typography.bodyLarge,
                  color = colorScheme.onBackground)
            }

        Spacer(modifier = Modifier.height(8.dp))

        // Your Events Dropdown
        Surface(
            modifier =
                Modifier.fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(6.dp))
                    .clickable { eventsExpanded = !eventsExpanded }
                    .testTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN),
            shape = RoundedCornerShape(6.dp),
            color = colorScheme.surface) {
              Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = "Your events",
                          style = MaterialTheme.typography.bodyLarge,
                          color = colorScheme.onBackground)
                      Icon(
                          imageVector =
                              if (eventsExpanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                          contentDescription = if (eventsExpanded) "Collapse" else "Expand",
                          tint = colorScheme.onBackground)
                    }

                if (eventsExpanded) {
                  HorizontalDivider(color = colorScheme.onSurface, thickness = 1.dp)

                  if (events.isEmpty()) {
                    Text(
                        text = "No events created yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onBackground,
                        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
                  } else {
                    events.forEach { event ->
                      EventCard(
                          event = event,
                          currentUserRole = currentUserRole,
                          onScanTickets = { onScanTickets(event.eventId) },
                          onEditEvent = { onEditEvent(event.eventId) })
                    }
                  }
                }
              }
            }
      }
}

/**
 * Displays a single event card within the "Your events" dropdown.
 *
 * This card shows event details and action buttons ("Scan", "Edit") based on user roles.
 *
 * @param event The [Event] to display.
 * @param currentUserRole The role of the currently logged-in user, used to determine button
 *   visibility.
 * @param onScanTickets Callback for the "Scan" button.
 * @param onEditEvent Callback for the "Edit" button.
 */
@Composable
private fun EventCard(
    event: Event,
    currentUserRole: OrganizationRole?,
    onScanTickets: () -> Unit,
    onEditEvent: () -> Unit
) {
  val canEdit =
      currentUserRole == OrganizationRole.MEMBER || currentUserRole == OrganizationRole.OWNER
  val canScan = currentUserRole != null // All roles can scan

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(16.dp)
              .testTag(OrganizationDashboardTestTags.getEventCardTag(event.eventId))) {
        Text(
            text = event.title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(
                  modifier =
                      Modifier.size(8.dp)
                          .background(
                              color =
                                  when (event.status) {
                                    EventStatus.DRAFT -> UnSelected
                                    EventStatus.PUBLISHED -> Success
                                    else -> Warning
                                  },
                              shape = RoundedCornerShape(2.dp)))
              Text(
                  text = event.status.name.lowercase().replaceFirstChar { it.uppercase() },
                  style = MaterialTheme.typography.bodyMedium,
                  color = colorScheme.onSurface)
            }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = event.displayDateTime,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.primary)

        Text(
            text = event.displayLocation,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.primary)

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (canScan) {
                OutlinedButton(
                    onClick = onScanTickets,
                    modifier =
                        Modifier.weight(1f)
                            .testTag(
                                OrganizationDashboardTestTags.getEventScanButtonTag(event.eventId)),
                    colors =
                        ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface),
                    shape = RoundedCornerShape(4.dp)) {
                      Icon(
                          painter = painterResource(id = R.drawable.qr_code_icon),
                          contentDescription = "Scan",
                          modifier = Modifier.size(20.dp))
                    }
              }

              if (canEdit) {
                Button(
                    onClick = onEditEvent,
                    modifier =
                        Modifier.weight(if (canScan) 1f else 2f)
                            .testTag(
                                OrganizationDashboardTestTags.getEventEditButtonTag(event.eventId)),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = RoundedCornerShape(4.dp)) {
                      Text("Edit event")
                    }
              }
            }
      }

  HorizontalDivider(color = colorScheme.onSurface, thickness = 1.dp)
}

/**
 * Displays the "Manage Staff" section, including the "Add new staff" button and the expandable
 * "Staff list" dropdown.
 *
 * @param staffMembers List of staff members with their profiles.
 * @param currentUserRole The role of the currently logged-in user.
 * @param onAddStaff Callback for the "Add new staff" button.
 * @param onRemoveStaff Callback for removing a staff member.
 */
@Composable
private fun ManageStaffSection(
    staffMembers: List<StaffMemberUiState>,
    currentUserRole: OrganizationRole?,
    onAddStaff: () -> Unit,
    onRemoveStaff: (String) -> Unit
) {
  var staffExpanded by remember { mutableStateOf(false) }
  val canManageStaff = currentUserRole == OrganizationRole.OWNER

  Column(
      modifier =
          Modifier.fillMaxWidth().testTag(OrganizationDashboardTestTags.MANAGE_STAFF_SECTION)) {
        Text(
            text = "MANAGE STAFF",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface)

        Spacer(modifier = Modifier.height(8.dp))

        // Add New Staff Button
        Button(
            onClick = onAddStaff,
            modifier =
                Modifier.fillMaxWidth().testTag(OrganizationDashboardTestTags.ADD_STAFF_BUTTON),
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
            shape = RoundedCornerShape(6.dp)) {
              Icon(
                  painter = painterResource(id = android.R.drawable.ic_input_add),
                  contentDescription = "Add",
                  modifier = Modifier.size(20.dp),
                  tint = colorScheme.onBackground)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                  text = "Add new staff",
                  style = MaterialTheme.typography.bodyLarge,
                  color = colorScheme.onBackground)
            }

        Spacer(modifier = Modifier.height(8.dp))

        // Staff List Dropdown
        Surface(
            modifier =
                Modifier.fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(6.dp))
                    .clickable { staffExpanded = !staffExpanded }
                    .testTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN),
            shape = RoundedCornerShape(6.dp),
            color = colorScheme.surface) {
              Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = "Staff list",
                          style = MaterialTheme.typography.bodyLarge,
                          color = colorScheme.onBackground)
                      Icon(
                          imageVector =
                              if (staffExpanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                          contentDescription = if (staffExpanded) "Collapse" else "Expand",
                          tint = colorScheme.onBackground)
                    }

                if (staffExpanded) {
                  HorizontalDivider(color = colorScheme.onSurface, thickness = 1.dp)

                  if (staffMembers.isEmpty()) {
                    Text(
                        text = "No staff members added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onBackground,
                        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
                  } else {
                    staffMembers.forEach { memberState ->
                      StaffItem(
                          memberState = memberState,
                          canRemove = canManageStaff && memberState.role != OrganizationRole.OWNER,
                          onRemove = { onRemoveStaff(memberState.userId) })
                    }
                  }
                }
              }
            }
      }
}

/**
 * Displays a single staff member in the "Staff list".
 *
 * @param memberState The UI state of the staff member.
 * @param canRemove Whether the current user has permission to remove this member.
 * @param onRemove Callback invoked when the remove button is clicked.
 */
@Composable
private fun StaffItem(memberState: StaffMemberUiState, canRemove: Boolean, onRemove: () -> Unit) {
  if (memberState.isLoading || memberState.userProfile == null) {
    SkeletonStaffItem(userId = memberState.userId)
    HorizontalDivider(color = colorScheme.onSurface, thickness = 1.dp)
    return
  }

  val user = memberState.userProfile
  val role = memberState.role
  val initials =
      remember(user.displayName) {
        if (user.displayName.isBlank()) "?" else user.displayName.take(1).uppercase(Locale.ROOT)
      }

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(16.dp)
              .testTag(OrganizationDashboardTestTags.getStaffItemTag(memberState.userId)),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
          // Avatar
          Box(
              modifier = Modifier.size(40.dp).clip(CircleShape).background(colorScheme.primary),
              contentAlignment = Alignment.Center) {
                if (user.avatarUrl.isNullOrBlank()) {
                  Text(
                      text = initials,
                      style =
                          MaterialTheme.typography.labelLarge.copy(
                              fontWeight = FontWeight.SemiBold),
                      color = colorScheme.onBackground)
                } else {
                  SubcomposeAsyncImage(
                      model =
                          ImageRequest.Builder(LocalContext.current)
                              .data(user.avatarUrl)
                              .crossfade(true)
                              .build(),
                      contentDescription = "Avatar",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize().clip(CircleShape),
                      loading = {
                        Box(modifier = Modifier.fillMaxSize().background(colorScheme.onSurface))
                      })
                }
              }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
                text = user.displayName.ifBlank { "Unknown User" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface)
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onBackground)
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(4.dp),
            color =
                when (role) {
                  OrganizationRole.OWNER -> colorScheme.primary
                  OrganizationRole.ADMIN -> colorScheme.primary
                  OrganizationRole.MEMBER -> colorScheme.primary
                  OrganizationRole.STAFF -> colorScheme.primary
                }) {
              Text(
                  text = role.name,
                  style = MaterialTheme.typography.labelMedium,
                  color = colorScheme.onBackground,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }

        if (canRemove) {
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
              onClick = onRemove,
              modifier =
                  Modifier.size(32.dp)
                      .testTag(
                          OrganizationDashboardTestTags.getStaffRemoveButtonTag(
                              memberState.userId))) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = "Remove",
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp))
              }
        }
      }

  HorizontalDivider(color = colorScheme.onSurface, thickness = 1.dp)
}

@Composable
fun SkeletonStaffItem(userId: String) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(16.dp)
              .testTag(OrganizationDashboardTestTags.getStaffItemTag(userId)),
      verticalAlignment = Alignment.CenterVertically) {

        // Avatar Skeleton
        Box(
            modifier =
                Modifier.size(40.dp)
                    .clip(CircleShape)
                    .background(colorScheme.onSurface)
                    .testTag("skeleton_avatar_$userId"))

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          // Name Skeleton
          Box(
              modifier =
                  Modifier.height(16.dp)
                      .fillMaxWidth(0.5f)
                      .background(colorScheme.onSurface, RoundedCornerShape(4.dp))
                      .testTag("skeleton_name_$userId"))
          Spacer(modifier = Modifier.height(4.dp))
          // Email Skeleton
          Box(
              modifier =
                  Modifier.height(12.dp)
                      .fillMaxWidth(0.7f)
                      .background(colorScheme.onSurface, RoundedCornerShape(4.dp))
                      .testTag("skeleton_email_$userId"))
        }
      }
}
