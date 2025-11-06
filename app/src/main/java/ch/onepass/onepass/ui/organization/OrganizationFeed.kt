package ch.onepass.onepass.ui.organization

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.model.organization.Organization

object OrganizationFeedTestTags {
  const val ORGANIZATION_FEED_SCREEN = "organizationFeedScreen"
  const val ORGANIZATION_FEED_TOP_BAR = "organizationFeedTopBar"
  const val ORGANIZATION_FEED_TITLE = "organizationFeedTitle"
  const val ORGANIZATION_LIST = "organizationList"
  const val LOADING_INDICATOR = "loadingIndicator"
  const val ERROR_MESSAGE = "errorMessage"
  const val RETRY_BUTTON = "retryButton"
  const val EMPTY_STATE = "emptyState"
  const val BACK_BUTTON = "backButton"

  fun getTestTagForOrganizationItem(orgId: String) = "organizationItem_$orgId"
}

/**
 * Main Organization Feed Screen composable.
 *
 * Collects view model state and delegates to the scaffold composable so the Scaffold is used in
 * production and previews in the exact same way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationFeedScreen(
    userId: String,
    modifier: Modifier = Modifier,
    onNavigateToOrganization: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: OrganizationFeedViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  // Load organizations when screen is first displayed
  LaunchedEffect(userId) { viewModel.loadUserOrganizations(userId) }

  OrganizationFeedScaffold(
      modifier = modifier.testTag(OrganizationFeedTestTags.ORGANIZATION_FEED_SCREEN),
      organizations = uiState.organizations,
      isLoading = uiState.isLoading,
      error = uiState.error,
      onOrganizationClick = onNavigateToOrganization,
      onNavigateBack = onNavigateBack,
      onRetry = { viewModel.refreshOrganizations(userId) })
}

/**
 * Reusable scaffold that contains the top bar and body content. Accepts plain data so previews can
 * call this directly and production code can simply pass view model state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ModifierParameter")
@Composable
fun OrganizationFeedScaffold(
    organizations: List<Organization>,
    isLoading: Boolean,
    error: String?,
    onOrganizationClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = { OrganizationFeedTopBar(onNavigateBack = onNavigateBack) },
      containerColor = Color(0xFF0A0A0A),
  ) { paddingValues ->
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
      when {
        isLoading && organizations.isEmpty() -> {
          LoadingState()
        }
        error != null && organizations.isEmpty() -> {
          ErrorState(error = error, onRetry = onRetry)
        }
        !isLoading && organizations.isEmpty() -> {
          EmptyOrganizationState()
        }
        else -> {
          OrganizationListContent(
              organizations = organizations,
              onOrganizationClick = onOrganizationClick,
          )
        }
      }
    }
  }
}

/** Top bar with title and back button. */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ModifierParameter")
@Composable
private fun OrganizationFeedTopBar(onNavigateBack: () -> Unit = {}, modifier: Modifier = Modifier) {
  CenterAlignedTopAppBar(
      modifier = modifier.testTag(OrganizationFeedTestTags.ORGANIZATION_FEED_TOP_BAR),
      navigationIcon = {
        IconButton(
            onClick = onNavigateBack,
            modifier = modifier.size(48.dp).testTag(OrganizationFeedTestTags.BACK_BUTTON)) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = Color.White,
                  modifier = modifier.size(24.dp))
            }
      },
      title = {
        Text(
            text = "MY ORGANIZATIONS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp,
            modifier = modifier.testTag(OrganizationFeedTestTags.ORGANIZATION_FEED_TITLE))
      },
      colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0A0A0A)))
}

/** Organization list content with scrollable cards. */
@Composable
private fun OrganizationListContent(
    organizations: List<Organization>,
    onOrganizationClick: (String) -> Unit,
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(OrganizationFeedTestTags.ORGANIZATION_LIST),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    items(items = organizations, key = { it.id }) { organization ->
      OrganizationCard(
          organization = organization,
          onClick = { onOrganizationClick(organization.id) },
          modifier =
              Modifier.testTag(
                  OrganizationFeedTestTags.getTestTagForOrganizationItem(organization.id)))
    }
  }
}

/** Loading state indicator. */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  CircularProgressIndicator(
      modifier = modifier.testTag(OrganizationFeedTestTags.LOADING_INDICATOR),
      color = Color(0xFF841DA4),
  )
}

/** Error state with retry button. */
@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier =
          modifier.fillMaxWidth().padding(32.dp).testTag(OrganizationFeedTestTags.ERROR_MESSAGE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = "Oops!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
    Spacer(modifier = modifier.height(8.dp))
    Text(
        text = error,
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF9CA3AF),
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = modifier.height(24.dp))
    Button(
        onClick = onRetry,
        modifier = modifier.testTag(OrganizationFeedTestTags.RETRY_BUTTON),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color(0xFF841DA4),
                contentColor = Color.White,
            ),
    ) {
      Text(text = "Try Again", fontWeight = FontWeight.Medium)
    }
  }
}

/** Empty state when no organizations are available. */
@Composable
private fun EmptyOrganizationState(modifier: Modifier = Modifier) {
  Column(
      modifier =
          modifier.fillMaxWidth().padding(32.dp).testTag(OrganizationFeedTestTags.EMPTY_STATE),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = "No Organizations",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
    Spacer(modifier = modifier.height(8.dp))
    Text(
        text = "You haven't joined any organizations yet.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF9CA3AF),
        textAlign = TextAlign.Center,
    )
  }
}
