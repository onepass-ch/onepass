package ch.onepass.onepass.ui.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.ui.theme.DefaultBackground
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
@OptIn(ExperimentalMaterial3Api::class)
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

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(OrganizationDashboardTestTags.SCREEN),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = "DASHBOARD",
                  style = MaterialTheme.typography.headlineMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.testTag(OrganizationDashboardTestTags.TITLE))
            },
            navigationIcon = {
              IconButton(
                  onClick = onNavigateBack,
                  modifier = Modifier.testTag(OrganizationDashboardTestTags.BACK_BUTTON)) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_revert),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface)
                  }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = DefaultBackground,
                    titleContentColor = MaterialTheme.colorScheme.onSurface))
      },
      containerColor = DefaultBackground) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          when {
            uiState.isLoading -> {
              CircularProgressIndicator(
                  modifier =
                      Modifier.align(Alignment.Center)
                          .testTag(OrganizationDashboardTestTags.LOADING_INDICATOR),
                  color = EventDateColor)
            }
            uiState.error != null -> {
              ErrorState(
                  error = uiState.error!!,
                  onRetry = { viewModel.loadOrganization(organizationId) },
                  modifier = Modifier.align(Alignment.Center))
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

        // Manage Events Section - Placeholder for PR4
        Text(
            text = "MANAGE EVENTS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(OrganizationDashboardTestTags.MANAGE_EVENTS_SECTION))

        // Manage Staff Section - Placeholder for PR5
        Text(
            text = "MANAGE STAFF",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(OrganizationDashboardTestTags.MANAGE_STAFF_SECTION))
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
 * Displays a generic error state with a title, message, and retry button.
 *
 * @param error The error message to display.
 * @param onRetry Callback invoked when the "Try Again" button is clicked.
 * @param modifier Optional [Modifier] for layout adjustments.
 */
@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(32.dp)
              .testTag(OrganizationDashboardTestTags.ERROR_MESSAGE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = "Oops!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = EventDateColor)) {
              Text(text = "Try Again", fontWeight = FontWeight.Medium)
            }
      }
}
