package ch.onepass.onepass.ui.feed

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.ui.event.EventCardViewModel
import ch.onepass.onepass.ui.eventfilters.EventFilterViewModel
import ch.onepass.onepass.ui.feed.FeedScreenTestTags.getTestTagForEventItem
import ch.onepass.onepass.ui.feed.FeedScreenTestTags.getTestTagForSearchEvent
import ch.onepass.onepass.ui.feed.FeedScreenTestTags.getTestTagForSearchOrg
import ch.onepass.onepass.ui.feed.FeedScreenTestTags.getTestTagForSearchUser
import ch.onepass.onepass.ui.theme.OnePassTheme
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class FeedScreenSearchBarTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private var lastClick: GlobalSearchItemClick? = null
  private var lastNavigatedEventId: String? = null
  private var notificationsOpened = false
  private var favoritesModeToggled = false

  private val mockFeedViewModel = mockk<FeedViewModel>()
  private val mockFilterViewModel = mockk<EventFilterViewModel>()
  private val mockEventCardViewModel = mockk<EventCardViewModel>()
  private val mockGlobalSearchViewModel = mockk<GlobalSearchViewModel>()

  private fun setContent(
      feedUiState: FeedUIState = FeedUIState(),
      searchUiState: GlobalSearchUiState = GlobalSearchUiState(),
      withGlobalSearch: Boolean = true
  ) {
    lastClick = null
    lastNavigatedEventId = null
    notificationsOpened = false
    favoritesModeToggled = false

    every { mockFeedViewModel.uiState } returns MutableStateFlow(feedUiState)
    every { mockFeedViewModel.loadEvents() } returns Unit
    every { mockFeedViewModel.refreshEvents() } returns Unit
    every { mockFeedViewModel.applyFiltersToCurrentEvents(any()) } returns Unit
    every { mockFeedViewModel.setShowFilterDialog(any()) } answers {}
    every { mockFeedViewModel.toggleFavoritesMode() } answers { favoritesModeToggled = true }

    every { mockFilterViewModel.currentFilters } returns MutableStateFlow(EventFilters())
    every { mockFilterViewModel.clearFilters() } returns Unit
    every { mockFilterViewModel.applyFilters(any()) } returns Unit
    every { mockFilterViewModel.updateLocalFilters(any()) } returns Unit

    every { mockEventCardViewModel.likedEvents } returns MutableStateFlow(emptySet())
    every { mockEventCardViewModel.toggleLike(any()) } returns Unit

    if (withGlobalSearch) {
      every { mockGlobalSearchViewModel.uiState } returns MutableStateFlow(searchUiState)
      every { mockGlobalSearchViewModel.onQueryChanged(any()) } answers
          {
            (mockGlobalSearchViewModel.uiState as MutableStateFlow).value =
                searchUiState.copy(query = firstArg())
          }
    }

    composeTestRule.setContent {
      OnePassTheme {
        val globalSearchVm = if (withGlobalSearch) mockGlobalSearchViewModel else null
        FeedScreen(
            onNavigateToEvent = { id -> lastNavigatedEventId = id },
            globalSearchItemClickListener =
                GlobalSearchItemClickListener { click -> lastClick = click },
            onNavigateToNotifications = { notificationsOpened = true },
            viewModel = mockFeedViewModel,
            filterViewModel = mockFilterViewModel,
            eventCardViewModel = mockEventCardViewModel,
            globalSearchViewModel = globalSearchVm)
      }
    }
  }

  @Test
  fun searchBar_isVisible_andClickable_andHasCorrectProperties() {
    setContent()
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun searchBar_acceptsTextInput_and_switchesToSearchBranch() {
    val searchState =
        GlobalSearchUiState(
            query = "concert",
            events =
                listOf(
                    Event(eventId = "1", title = "Concert Night", status = EventStatus.PUBLISHED)))
    setContent(searchUiState = searchState)
    val searchNode =
        composeTestRule.onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
    searchNode.performClick()
    searchNode.performTextClearance()
    searchNode.performClick()
    searchNode.performTextInput("concert")
    searchNode.assertTextContains("concert", substring = false)
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
  }

  @Test
  fun searchBar_clearThenRetype_works_andUpdatesViewModel() {
    val searchState = GlobalSearchUiState(query = "rock")
    setContent(searchUiState = searchState)
    val searchNode =
        composeTestRule.onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
    searchNode.performClick()
    searchNode.performTextInput("rock")
    searchNode.assertTextContains("rock", substring = false)
    searchNode.performTextClearance()
    searchNode.performClick()
    searchNode.performTextInput("jazz")
    searchNode.assertTextContains("jazz", substring = false)
  }

  @Test
  fun whenNoSearchQuery_feedContentIsShown() {
    val feedState =
        FeedUIState(
            events =
                listOf(Event(eventId = "1", title = "Test Event", status = EventStatus.PUBLISHED)))
    setContent(feedUiState = feedState)
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(getTestTagForEventItem("1")).assertIsDisplayed()
  }

  @Test
  fun whenSearchQueryIsNonBlank_searchBranchIsUsed() {
    val searchState =
        GlobalSearchUiState(
            query = "anything",
            events =
                listOf(
                    Event(eventId = "2", title = "Anything Event", status = EventStatus.PUBLISHED)))
    setContent(searchUiState = searchState)
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("anything")
    composeTestRule.onNodeWithTag(FeedScreenTestTags.EVENT_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(getTestTagForSearchEvent("2")).assertIsDisplayed()
  }

  @Test
  fun whenSearchShowsUsers_userItemsAreClickable() {
    val searchState =
        GlobalSearchUiState(
            query = "john",
            users =
                listOf(
                    StaffSearchResult(
                        id = "user1", email = "john@test.com", displayName = "John Doe")))
    setContent(searchUiState = searchState)
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("john")
    composeTestRule.onNodeWithTag(getTestTagForSearchUser("user1")).performClick()
    assertNotNull(lastClick)
    assertTrue(lastClick is GlobalSearchItemClick.UserClick)
    assertEquals("user1", (lastClick as GlobalSearchItemClick.UserClick).userId)
  }

  @Test
  fun whenSearchShowsOrganizations_orgItemsAreClickable() {
    val searchState =
        GlobalSearchUiState(
            query = "tech", organizations = listOf(Organization(id = "org1", name = "Tech Corp")))
    setContent(searchUiState = searchState)
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("tech")
    composeTestRule.onNodeWithTag(getTestTagForSearchOrg("org1")).performClick()
    assertNotNull(lastClick)
    assertTrue(lastClick is GlobalSearchItemClick.OrganizationClick)
    assertEquals("org1", (lastClick as GlobalSearchItemClick.OrganizationClick).organizationId)
  }

  @Test
  fun whenSearchShowsEvents_eventItemsAreClickable() {
    val searchState =
        GlobalSearchUiState(
            query = "concert",
            events =
                listOf(
                    Event(eventId = "event1", title = "Concert", status = EventStatus.PUBLISHED)))
    setContent(searchUiState = searchState)
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("concert")
    composeTestRule.onNodeWithTag(getTestTagForSearchEvent("event1")).performClick()
    assertEquals("event1", lastNavigatedEventId)
  }

  @Test
  fun whenSearchLoading_showsLoadingIndicator() {
    val searchState = GlobalSearchUiState(query = "loading", isLoading = true)
    setContent(searchUiState = searchState)
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("loading")
    composeTestRule.onNodeWithTag(FeedScreenTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun whenSearchError_showsErrorMessage() {
    val searchState = GlobalSearchUiState(query = "error", error = "Search failed")
    setContent(searchUiState = searchState)
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("error")
    composeTestRule.onNode(hasText("Error:", substring = true)).assertIsDisplayed()
  }

  @Test
  fun whenNoSearchResults_showsEmptyState() {
    val searchState = GlobalSearchUiState(query = "nonexistent")
    setContent(searchUiState = searchState)
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("nonexistent")
    composeTestRule.onNode(hasText("No results found", substring = true)).assertIsDisplayed()
  }

  @Test
  fun whenGlobalSearchViewModelIsNull_searchShowsFeedContent() {
    setContent(
        feedUiState =
            FeedUIState(
                events =
                    listOf(
                        Event(
                            eventId = "1", title = "Feed Event", status = EventStatus.PUBLISHED))),
        withGlobalSearch = false)
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("test")
    composeTestRule.onNodeWithTag(getTestTagForEventItem("1")).assertIsDisplayed()
  }

  @Test
  fun searchResultsAreLimited_toMaxCounts() {
    val searchState =
        GlobalSearchUiState(
            query = "test",
            events =
                List(10) { i ->
                  Event(eventId = "$i", title = "Event $i", status = EventStatus.PUBLISHED)
                },
            users =
                List(10) { i ->
                  StaffSearchResult(
                      id = "user$i", email = "user$i@test.com", displayName = "User $i")
                },
            organizations = List(10) { i -> Organization(id = "org$i", name = "Org $i") })
    setContent(searchUiState = searchState)
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("test")
    val eventItems =
        composeTestRule.onAllNodes(hasTestTagStartingWith("searchEvent_")).fetchSemanticsNodes()
    assertTrue(eventItems.size <= 3)
    val orgItems =
        composeTestRule.onAllNodes(hasTestTagStartingWith("searchOrg_")).fetchSemanticsNodes()
    assertTrue(orgItems.size <= 2)
  }

  @Test
  fun searchResults_displayMixedResults_usersEventsAndOrganizations() {
    // This test covers SearchResultsContent with ALL three result types present
    val searchState =
        GlobalSearchUiState(
            query = "mixed",
            users =
                listOf(
                    StaffSearchResult(
                        id = "user1", email = "user1@test.com", displayName = "User One"),
                    StaffSearchResult(
                        id = "user2", email = "user2@test.com", displayName = "User Two")),
            events =
                listOf(
                    Event(eventId = "event1", title = "Event One", status = EventStatus.PUBLISHED),
                    Event(eventId = "event2", title = "Event Two", status = EventStatus.PUBLISHED)),
            organizations =
                listOf(
                    Organization(id = "org1", name = "Org One"),
                    Organization(id = "org2", name = "Org Two")))

    setContent(searchUiState = searchState)

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("mixed")

    composeTestRule.waitForIdle()

    // Verify all users are displayed
    composeTestRule.onNodeWithTag(getTestTagForSearchUser("user1")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(getTestTagForSearchUser("user2")).assertIsDisplayed()

    // Verify all events are displayed
    composeTestRule.onNodeWithTag(getTestTagForSearchEvent("event1")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(getTestTagForSearchEvent("event2")).assertIsDisplayed()

    // Scroll to organizations and verify they are displayed
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag(getTestTagForSearchOrg("org1")))
    composeTestRule.onNodeWithTag(getTestTagForSearchOrg("org1")).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.EVENT_LIST)
        .performScrollToNode(hasTestTag(getTestTagForSearchOrg("org2")))
    composeTestRule.onNodeWithTag(getTestTagForSearchOrg("org2")).assertIsDisplayed()
  }

  @Test
  fun searchResults_displayMixedResults_andAllAreClickable() {
    // This test verifies that all items in mixed results are clickable
    val searchState =
        GlobalSearchUiState(
            query = "clickable",
            users =
                listOf(
                    StaffSearchResult(
                        id = "user1", email = "user1@test.com", displayName = "User One")),
            events =
                listOf(
                    Event(eventId = "event1", title = "Event One", status = EventStatus.PUBLISHED)),
            organizations = listOf(Organization(id = "org1", name = "Org One")))

    setContent(searchUiState = searchState)

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("clickable")

    composeTestRule.waitForIdle()

    // Click user
    composeTestRule.onNodeWithTag(getTestTagForSearchUser("user1")).performClick()
    assertTrue(lastClick is GlobalSearchItemClick.UserClick)
    assertEquals("user1", (lastClick as GlobalSearchItemClick.UserClick).userId)

    // Click event
    composeTestRule.onNodeWithTag(getTestTagForSearchEvent("event1")).performClick()
    assertEquals("event1", lastNavigatedEventId)

    // Click organization
    lastClick = null
    composeTestRule.onNodeWithTag(getTestTagForSearchOrg("org1")).performClick()
    assertTrue(lastClick is GlobalSearchItemClick.OrganizationClick)
    assertEquals("org1", (lastClick as GlobalSearchItemClick.OrganizationClick).organizationId)
  }

  @Test
  fun searchResults_onlyUsers_noEventsOrOrganizations() {
    // Tests the branch where only users exist (events and organizations are null/empty)
    val searchState =
        GlobalSearchUiState(
            query = "users",
            users =
                listOf(
                    StaffSearchResult(
                        id = "user1", email = "user1@test.com", displayName = "User One")),
            events = emptyList(),
            organizations = emptyList())

    setContent(searchUiState = searchState)

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("users")

    composeTestRule.waitForIdle()

    // User should be displayed
    composeTestRule
        .onNodeWithTag(getTestTagForSearchUser("user1"))
        .performScrollTo()
        .assertIsDisplayed()

    // No events or organizations should be displayed
    composeTestRule.onNodeWithTag(getTestTagForSearchEvent("event1")).assertDoesNotExist()
    composeTestRule.onNodeWithTag(getTestTagForSearchOrg("org1")).assertDoesNotExist()
  }

  @Test
  fun searchResults_onlyEvents_noUsersOrOrganizations() {
    // Tests the branch where only events exist (users and organizations are null/empty)
    val searchState =
        GlobalSearchUiState(
            query = "events",
            users = emptyList(),
            events =
                listOf(
                    Event(eventId = "event1", title = "Event One", status = EventStatus.PUBLISHED)),
            organizations = emptyList())

    setContent(searchUiState = searchState)

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("events")

    composeTestRule.waitForIdle()

    // Event should be displayed
    composeTestRule
        .onNodeWithTag(getTestTagForSearchEvent("event1"))
        .performScrollTo()
        .assertIsDisplayed()

    // No users or organizations should be displayed
    composeTestRule.onNodeWithTag(getTestTagForSearchUser("user1")).assertDoesNotExist()
    composeTestRule.onNodeWithTag(getTestTagForSearchOrg("org1")).assertDoesNotExist()
  }

  @Test
  fun searchResults_onlyOrganizations_noUsersOrEvents() {
    // Tests the branch where only organizations exist (users and events are null/empty)
    val searchState =
        GlobalSearchUiState(
            query = "orgs",
            users = emptyList(),
            events = emptyList(),
            organizations = listOf(Organization(id = "org1", name = "Org One")))

    setContent(searchUiState = searchState)

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("orgs")

    composeTestRule.waitForIdle()

    // Organization should be displayed
    composeTestRule
        .onNodeWithTag(getTestTagForSearchOrg("org1"))
        .performScrollTo()
        .assertIsDisplayed()

    // No users or events should be displayed
    composeTestRule.onNodeWithTag(getTestTagForSearchUser("user1")).assertDoesNotExist()
    composeTestRule.onNodeWithTag(getTestTagForSearchEvent("event1")).assertDoesNotExist()
  }

  @Test
  fun searchResults_nullLists_showsNoResults() {
    // Tests the branch where all result lists are explicitly null
    val searchState =
        GlobalSearchUiState(
            query = "null", users = listOf(), events = listOf(), organizations = listOf())

    setContent(searchUiState = searchState)

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("null")

    composeTestRule.waitForIdle()

    // Should show "No results found"
    composeTestRule.onNode(hasText("No results found")).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun centeredLoadingState_displaysCorrectly_inSearchMode() {
    // Tests CenteredLoadingState helper function in search context
    val searchState = GlobalSearchUiState(query = "loading", isLoading = true)

    setContent(searchUiState = searchState)

    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.SEARCH_TEXT_FIELD, useUnmergedTree = true)
        .performClick()
        .performTextInput("loading")

    composeTestRule.waitForIdle()

    // Verify loading indicator is displayed with correct test tag
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.LOADING_INDICATOR)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun centeredLoadingState_displaysCorrectly_inFeedMode() {
    // Tests CenteredLoadingState helper function in feed context
    val feedState = FeedUIState(isLoading = true, events = emptyList(), isRefreshing = false)

    setContent(feedUiState = feedState, withGlobalSearch = false)

    composeTestRule.waitForIdle()

    // Verify loading indicator is displayed with correct test tag
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.LOADING_INDICATOR)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun eventListContent_withLoadingMore_displaysBottomLoadingIndicator() {
    // Tests EventListContent with isLoadingMore = true
    val feedState =
        FeedUIState(
            isLoading = true, // isLoadingMore will be true (loading && !refreshing)
            isRefreshing = false,
            events =
                listOf(
                    Event(eventId = "event1", title = "Event 1", status = EventStatus.PUBLISHED),
                    Event(eventId = "event2", title = "Event 2", status = EventStatus.PUBLISHED)))

    setContent(feedUiState = feedState, withGlobalSearch = false)

    composeTestRule.waitForIdle()

    // Events should be displayed
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem("event1"))
        .performScrollTo()
        .assertIsDisplayed()

    // Scroll to bottom to see loading indicator
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.EVENT_LIST)
        .performScrollToIndex(2) // Scroll past the events to the loading indicator

    composeTestRule.waitForIdle()

    // Bottom loading indicator should exist
    composeTestRule.onAllNodesWithTag("loading_indicator").assertCountEquals(1)
  }

  @Test
  fun eventListContent_withoutLoadingMore_noBottomLoadingIndicator() {
    // Tests EventListContent with isLoadingMore = false
    val feedState =
        FeedUIState(
            isLoading = false,
            isRefreshing = false,
            events =
                listOf(
                    Event(eventId = "event1", title = "Event 1", status = EventStatus.PUBLISHED),
                    Event(eventId = "event2", title = "Event 2", status = EventStatus.PUBLISHED)))

    setContent(feedUiState = feedState, withGlobalSearch = false)

    composeTestRule.waitForIdle()

    // Events should be displayed
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem("event1"))
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.getTestTagForEventItem("event2"))
        .performScrollTo()
        .assertIsDisplayed()

    // No loading indicator at the bottom
    composeTestRule.onAllNodesWithTag("loading_indicator").assertCountEquals(0)
  }

  private fun hasText(text: String): SemanticsMatcher {
    return SemanticsMatcher("Has text '$text'") { node ->
      val textList =
          node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)
              ?: emptyList()
      textList.any { annotatedString -> annotatedString.text == text }
    }
  }

  private fun hasTestTagStartingWith(prefix: String): SemanticsMatcher =
      SemanticsMatcher("Has test tag starting with \"$prefix\"") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag != null && tag.startsWith(prefix)
      }

  private fun hasText(text: String, substring: Boolean = false): SemanticsMatcher {
    return if (substring) {
      SemanticsMatcher("Contains text '$text'") { node ->
        val textList = node.config.getOrNull(SemanticsProperties.Text) ?: emptyList()
        textList.any { annotatedString -> annotatedString.text.contains(text) }
      }
    } else {
      SemanticsMatcher("Has text '$text'") { node ->
        val textList = node.config.getOrNull(SemanticsProperties.Text) ?: emptyList()
        textList.any { annotatedString -> annotatedString.text == text }
      }
    }
  }
}
