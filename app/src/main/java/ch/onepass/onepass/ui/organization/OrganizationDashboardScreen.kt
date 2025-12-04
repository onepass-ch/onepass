package ch.onepass.onepass.ui.organization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.organization.OrganizationMember
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.ui.components.common.ErrorState
import ch.onepass.onepass.ui.components.common.LoadingState
import ch.onepass.onepass.ui.navigation.BackNavigationScaffold
import ch.onepass.onepass.ui.theme.EventDateColor
import ch.onepass.onepass.ui.theme.TextSecondary
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
      title = "DASHBOARD",
      onBack = onNavigateBack,
      modifier = modifier.testTag(OrganizationDashboardTestTags.SCREEN),
      titleTestTag = OrganizationDashboardTestTags.TITLE,
      backButtonTestTag = OrganizationDashboardTestTags.BACK_BUTTON) { paddingValues ->
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
        // Organization Summary Card
        OrganizationSummaryCard(
            organizationName = organization.name,
            followers = organization.followerCount,
            rating = organization.averageRating,
            onClick = { onNavigateToProfile(organization.id) })

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
 * Displays a summary card for the organization.
 *
 * @param organizationName The name of the organization.
 * @param followers The number of followers.
 * @param rating The average rating of the organization.
 * @param onClick Callback invoked when the card is clicked.
 */
@Composable
private fun OrganizationSummaryCard(
    organizationName: String,
    followers: Int,
    rating: Float,
    onClick: () -> Unit
) {
  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
              .clickable(onClick = onClick)
              .testTag(OrganizationDashboardTestTags.ORG_SUMMARY_CARD),
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
              // Placeholder for organization logo/image
              Box(
                  modifier =
                      Modifier.size(72.dp)
                          .background(
                              MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(6.dp)))

              Spacer(modifier = Modifier.width(16.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = organizationName.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(OrganizationDashboardTestTags.ORG_NAME))

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = "${followers / 1000f}K FOLLOWERS",
                          style = MaterialTheme.typography.bodyMedium,
                          color = TextSecondary,
                          modifier = Modifier.testTag(OrganizationDashboardTestTags.ORG_FOLLOWERS))

                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.btn_star_big_on),
                                contentDescription = "Rating",
                                tint = EventDateColor,
                                modifier = Modifier.size(16.dp))
                            Text(
                                text = rating.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier =
                                    Modifier.testTag(OrganizationDashboardTestTags.ORG_RATING))
                          }
                    }
              }
            }
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
            color = MaterialTheme.colorScheme.onSurface)

        Spacer(modifier = Modifier.height(8.dp))

        // Create New Event Button
        Button(
            onClick = onCreateEvent,
            modifier =
                Modifier.fillMaxWidth().testTag(OrganizationDashboardTestTags.CREATE_EVENT_BUTTON),
            colors = ButtonDefaults.buttonColors(containerColor = EventDateColor),
            shape = RoundedCornerShape(6.dp)) {
              Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = "Create",
                  modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = "Create new event", style = MaterialTheme.typography.bodyLarge)
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
            color = MaterialTheme.colorScheme.surfaceVariant) {
              Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = "Your events",
                          style = MaterialTheme.typography.bodyLarge,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Icon(
                          imageVector =
                              if (eventsExpanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                          contentDescription = if (eventsExpanded) "Collapse" else "Expand",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                if (eventsExpanded) {
                  HorizontalDivider(
                      color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                  if (events.isEmpty()) {
                    Text(
                        text = "No events created yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
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
            color = MaterialTheme.colorScheme.onSurface,
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
                                    EventStatus.DRAFT -> EventDateColor
                                    EventStatus.PUBLISHED -> EventDateColor
                                    else -> TextSecondary
                                  },
                              shape = RoundedCornerShape(2.dp)))
              Text(
                  text = event.status.name.lowercase().replaceFirstChar { it.uppercase() },
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface)
            }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = event.displayDateTime,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary)

        Text(
            text = event.displayLocation,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary)

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
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                    colors = ButtonDefaults.buttonColors(containerColor = EventDateColor),
                    shape = RoundedCornerShape(4.dp)) {
                      Text("Edit event")
                    }
              }
            }
      }

  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
}

/**
 * Displays the "Manage Staff" section, including the "Add new staff" button and the expandable
 * "Staff list" dropdown.
 *
 * @param staffMembers A map of user IDs to [OrganizationMember] objects.
 * @param currentUserRole The role of the currently logged-in user.
 * @param onAddStaff Callback for the "Add new staff" button.
 * @param onRemoveStaff Callback for removing a staff member.
 */
@Composable
private fun ManageStaffSection(
    staffMembers: Map<String, OrganizationMember>,
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
            color = MaterialTheme.colorScheme.onSurface)

        Spacer(modifier = Modifier.height(8.dp))

        // Add New Staff Button
        Button(
            onClick = onAddStaff,
            modifier =
                Modifier.fillMaxWidth().testTag(OrganizationDashboardTestTags.ADD_STAFF_BUTTON),
            colors = ButtonDefaults.buttonColors(containerColor = EventDateColor),
            shape = RoundedCornerShape(6.dp)) {
              Icon(
                  painter = painterResource(id = android.R.drawable.ic_input_add),
                  contentDescription = "Add",
                  modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = "Add new staff", style = MaterialTheme.typography.bodyLarge)
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
            color = MaterialTheme.colorScheme.surfaceVariant) {
              Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = "Staff list",
                          style = MaterialTheme.typography.bodyLarge,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Icon(
                          imageVector =
                              if (staffExpanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                          contentDescription = if (staffExpanded) "Collapse" else "Expand",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                if (staffExpanded) {
                  HorizontalDivider(
                      color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                  if (staffMembers.isEmpty()) {
                    Text(
                        text = "No staff members added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
                  } else {
                    staffMembers.forEach { (userId, member) ->
                      StaffItem(
                          userId = userId,
                          email = userId, // In real implementation, fetch from user repository once
                          // implemented (current user repository doesn't offer such
                          // methods)
                          role = member.role,
                          canRemove = canManageStaff && member.role != OrganizationRole.OWNER,
                          onRemove = { onRemoveStaff(userId) })
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
 * @param userId The unique ID of the staff member.
 * @param email The email (or display name) of the staff member.
 * @param role The [OrganizationRole] of the staff member.
 * @param canRemove Whether the current user has permission to remove this member.
 * @param onRemove Callback invoked when the remove button is clicked.
 */
@Composable
private fun StaffItem(
    userId: String,
    email: String,
    role: OrganizationRole,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(16.dp)
              .testTag(OrganizationDashboardTestTags.getStaffItemTag(userId)),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = email,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
            shape = RoundedCornerShape(4.dp),
            color =
                when (role) {
                  OrganizationRole.OWNER -> EventDateColor
                  OrganizationRole.MEMBER -> MaterialTheme.colorScheme.primary
                  OrganizationRole.STAFF -> MaterialTheme.colorScheme.secondary
                }) {
              Text(
                  text = role.name,
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }

        if (canRemove) {
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
              onClick = onRemove,
              modifier =
                  Modifier.size(32.dp)
                      .testTag(OrganizationDashboardTestTags.getStaffRemoveButtonTag(userId))) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = "Remove",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp))
              }
        }
      }

  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
}
