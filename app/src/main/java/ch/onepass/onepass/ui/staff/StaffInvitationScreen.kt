package ch.onepass.onepass.ui.staff

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.user.UserSearchType
import ch.onepass.onepass.ui.components.common.EmptyState
import ch.onepass.onepass.ui.components.common.LoadingState
import ch.onepass.onepass.ui.theme.DefaultBackground

object StaffInvitationTestTags {
  const val SCREEN = "staffInvitation_screen"
  const val TOP_BAR = "staffInvitation_topBar"
  const val BACK_BUTTON = "staffInvitation_backButton"
  const val TITLE = "staffInvitation_title"
  const val TAB_ROW = "staffInvitation_tabRow"
  const val TAB_DISPLAY_NAME = "staffInvitation_tab_displayName"
  const val TAB_EMAIL = "staffInvitation_tab_email"
  const val SEARCH_FIELD = "staffInvitation_searchField"
  const val RESULTS_LIST = "staffInvitation_resultsList"
  const val EMPTY_STATE = "staffInvitation_emptyState"
  const val LOADING_INDICATOR = "staffInvitation_loadingIndicator"
  const val ERROR_MESSAGE = "staffInvitation_errorMessage"
}

/**
 * Screen for inviting staff members to an organization.
 *
 * @param viewModel ViewModel for managing screen state.
 * @param onNavigateBack Callback to navigate back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffInvitationScreen(
    viewModel: StaffInvitationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val selectedTabIndex = if (uiState.selectedTab == UserSearchType.DISPLAY_NAME) 0 else 1

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(StaffInvitationTestTags.SCREEN),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = "Add staff",
                  color = Color.White,
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.testTag(StaffInvitationTestTags.TITLE))
            },
            navigationIcon = {
              IconButton(
                  onClick = onNavigateBack,
                  modifier = Modifier.testTag(StaffInvitationTestTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White)
                  }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DefaultBackground),
            modifier = Modifier.testTag(StaffInvitationTestTags.TOP_BAR))
      },
      containerColor = DefaultBackground) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
              // Tab Row
              TabRow(
                  modifier = Modifier.fillMaxWidth().testTag(StaffInvitationTestTags.TAB_ROW),
                  selectedTabIndex = selectedTabIndex,
                  containerColor = colorResource(id = R.color.screen_surface),
                  contentColor = colorResource(id = R.color.on_surface),
                  indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        height = 4.dp,
                        color = colorResource(id = R.color.tab_indicator))
                  }) {
                    Tab(
                        text = {
                          Text(
                              text = "Search by username",
                              style = MaterialTheme.typography.titleMedium,
                              color =
                                  if (uiState.selectedTab == UserSearchType.DISPLAY_NAME)
                                      colorResource(id = R.color.tab_selected)
                                  else
                                      colorResource(id = R.color.tab_unselected).copy(alpha = 0.6f))
                        },
                        selected = uiState.selectedTab == UserSearchType.DISPLAY_NAME,
                        onClick = { viewModel.selectTab(UserSearchType.DISPLAY_NAME) },
                        modifier = Modifier.testTag(StaffInvitationTestTags.TAB_DISPLAY_NAME))
                    Tab(
                        text = {
                          Text(
                              text = "Search by e-mail",
                              style = MaterialTheme.typography.titleMedium,
                              color =
                                  if (uiState.selectedTab == UserSearchType.EMAIL)
                                      colorResource(id = R.color.tab_selected)
                                  else
                                      colorResource(id = R.color.tab_unselected).copy(alpha = 0.6f))
                        },
                        selected = uiState.selectedTab == UserSearchType.EMAIL,
                        onClick = { viewModel.selectTab(UserSearchType.EMAIL) },
                        modifier = Modifier.testTag(StaffInvitationTestTags.TAB_EMAIL))
                  }

              // Search Input Field
              SearchInputField(
                  value = uiState.searchQuery,
                  onValueChange = viewModel::updateSearchQuery,
                  placeholder =
                      if (uiState.selectedTab == UserSearchType.DISPLAY_NAME) "Search by name"
                      else "Search by email",
                  modifier = Modifier.fillMaxWidth().padding(top = 16.dp))

              // Error Message
              uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag(StaffInvitationTestTags.ERROR_MESSAGE))
              }

              // Results List or Empty State
              Box(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                when {
                  uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                      LoadingState(testTag = StaffInvitationTestTags.LOADING_INDICATOR)
                    }
                  }
                  uiState.searchQuery.isBlank() -> {
                    EmptyState(
                        title = "Search Users",
                        message = "Enter a search query to find users",
                        modifier = Modifier.fillMaxSize(),
                        testTag = StaffInvitationTestTags.EMPTY_STATE)
                  }
                  uiState.searchResults.isEmpty() -> {
                    EmptyState(
                        title = "No Users Found",
                        message = "Try a different search query.",
                        modifier = Modifier.fillMaxSize(),
                        testTag = StaffInvitationTestTags.EMPTY_STATE)
                  }
                  else -> {
                    LazyColumn(
                        modifier =
                            Modifier.fillMaxSize().testTag(StaffInvitationTestTags.RESULTS_LIST),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                          items(items = uiState.searchResults, key = { it.id }) { user ->
                            val isInvited = user.id in uiState.invitedUserIds
                            val isAlreadyInvited = user.id in uiState.alreadyInvitedUserIds

                            StaffListItem(
                                user = user,
                                onClick =
                                    if (isInvited || isAlreadyInvited) null
                                    else {
                                      { viewModel.onUserSelected(user) }
                                    },
                                enabled = !(isInvited || isAlreadyInvited),
                                modifier = Modifier.fillMaxWidth())
                          }
                        }
                  }
                }
              }
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
  TextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = {
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
      },
      modifier =
          modifier
              .fillMaxWidth()
              .border(1.dp, Color(0xFF404040), RoundedCornerShape(10.dp))
              .heightIn(min = 50.dp)
              .testTag(StaffInvitationTestTags.SEARCH_FIELD),
      colors =
          TextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF1C1C1C),
              unfocusedContainerColor = Color(0xFF1C1C1C),
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
          ),
      shape = RoundedCornerShape(10.dp),
      textStyle = MaterialTheme.typography.bodySmall,
      singleLine = true)
}
