package ch.onepass.onepass.ui.organization

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OrganizationFeedTest {
  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var mockRepository: OrganizationRepositoryFirebase
  private lateinit var viewModel: OrganizationFeedViewModel
  private val testUserId = "testUser123"
  private val testOrganizations =
      listOf(
          Organization(
              id = "org1",
              name = "Tech Events Zurich",
              description = "Leading tech organizer",
              verified = true,
              followerCount = 2500,
              averageRating = 4.5f,
              createdAt = Timestamp.now()),
          Organization(
              id = "org2",
              name = "Music Nights Geneva",
              description = "Unforgettable music experiences",
              verified = false,
              followerCount = 15000,
              averageRating = 4.8f,
              createdAt = Timestamp.now()))

  @Before
  fun setup() {
    mockRepository = mockk(relaxed = true)
    coEvery { mockRepository.getOrganizationsByOwner(any()) } returns flowOf(emptyList())
  }

  @After
  fun tearDown() {
    unmockkAll()
  }
  // ==================== Loading State Tests ====================
  @Test
  fun organizationFeedScreen_displaysLoadingState_whenInitiallyLoading() {
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns flowOf()
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.LOADING_INDICATOR)
        .assertExists()
        .assertIsDisplayed()
  }
  // ==================== Success State Tests ====================
  @Test
  fun organizationFeedScreen_displaysOrganizationsWithDetails_whenDataLoaded() {
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)

    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }

    // Wait for data to load
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Tech Events Zurich").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()

    // Verify first organization with details
    composeTestRule.onNodeWithText("Tech Events Zurich").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("Leading tech organizer").assertExists().assertIsDisplayed()

    // Verify second organization with details
    composeTestRule.onNodeWithText("Music Nights Geneva").assertExists().assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Unforgettable music experiences")
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun organizationFeedScreen_organizationCard_clickTriggersNavigation() {
    coEvery { mockRepository.getOrganizationsByOwner(testUserId) } returns flowOf(emptyList())
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)
    var clickedOrgId = ""
    composeTestRule.setContent {
      OnePassTheme {
        OrganizationFeedScreen(
            userId = testUserId,
            viewModel = viewModel,
            onNavigateToOrganization = { orgId -> clickedOrgId = orgId })
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Tech Events Zurich").assertExists().performClick()
    assert(clickedOrgId == "org1")
  }

  // ==================== Empty State Tests ====================
  @Test
  fun organizationFeedScreen_displaysEmptyState_whenNoOrganizations() {
    coEvery { mockRepository.getOrganizationsByOwner(testUserId) } returns flowOf(emptyList())
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns flowOf(emptyList())
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }

    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.EMPTY_STATE)
        .assertExists()
        .assertIsDisplayed()
  }

  // ==================== Error State Tests ====================
  @Test
  fun organizationFeedScreen_displaysErrorState_whenLoadingFails() {
    val errorMessage = "Network connection failed"
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } throws Exception(errorMessage)
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.ERROR_MESSAGE)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("Oops!").assertExists().assertIsDisplayed()

    composeTestRule.onNodeWithText(errorMessage).assertExists().assertIsDisplayed()

    composeTestRule
        .onNodeWithTag("${OrganizationFeedTestTags.ERROR_MESSAGE}_retry_button")
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun organizationFeedScreen_retryButton_reloadsOrganizations() {
    val errorMessage = "Network error"
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } throws
        Exception(errorMessage) andThen
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }
    composeTestRule.onNodeWithTag(OrganizationFeedTestTags.ERROR_MESSAGE).assertExists()
    composeTestRule
        .onNodeWithTag("${OrganizationFeedTestTags.ERROR_MESSAGE}_retry_button")
        .performClick()
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(OrganizationFeedTestTags.ORGANIZATION_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }
  // ==================== Top Bar Tests ====================
  @Test
  fun organizationFeedScreen_displaysTopBarWithTitle() {
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.ORGANIZATION_FEED_TOP_BAR)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.ORGANIZATION_FEED_TITLE)
        .assertExists()
        .assertIsDisplayed()
        .assertTextEquals("MY ORGANIZATIONS")
  }

  @Test
  fun organizationFeedScreen_backButton_triggersNavigation() {
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)

    var backPressed = false
    composeTestRule.setContent {
      OnePassTheme {
        OrganizationFeedScreen(
            userId = testUserId, viewModel = viewModel, onNavigateBack = { backPressed = true })
      }
    }
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.BACK_BUTTON)
        .assertExists()
        .assertIsDisplayed()
        .performClick()
    assert(backPressed)
  }
  // ==================== ViewModel Tests ====================
  @Test
  fun viewModel_loadsOrganizations_onInit() {
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)
    viewModel.loadUserOrganizations(testUserId)

    composeTestRule.waitForIdle()
    coVerify { mockRepository.getOrganizationsByMember(testUserId) }
  }

  @Test
  fun viewModel_handlesRepositoryError_correctly() {
    val errorMessage = "Database error"
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } throws Exception(errorMessage)
    viewModel = OrganizationFeedViewModel(mockRepository)
    viewModel.loadUserOrganizations(testUserId)
    composeTestRule.waitForIdle()
    assert(viewModel.uiState.value.error == errorMessage)
    assert(!viewModel.uiState.value.isLoading)
  }

  // ==================== Scrolling and List Tests ====================
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun organizationFeedScreen_scrollableList_worksCorrectly() {
    val manyOrgs =
        (1..10).map { i ->
          Organization(
              id = "org$i",
              name = "Organization $i",
              description = "Description $i",
              verified = i % 2 == 0,
              followerCount = i * 1000,
              averageRating = 4f + (i % 5) * 0.2f,
              createdAt = Timestamp.now())
        }

    // Mock both flows to ensure combine() works properly
    coEvery { mockRepository.getOrganizationsByOwner(testUserId) } returns flowOf(emptyList())
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns flowOf(manyOrgs)

    viewModel = OrganizationFeedViewModel(mockRepository)

    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }

    // Wait for the first organization to be rendered (this implicitly waits for the list to exist)
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule.onAllNodesWithText("Organization 1").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.waitForIdle()

    // Verify first item is visible
    composeTestRule.onNodeWithText("Organization 1").assertExists().assertIsDisplayed()

    // Verify the list exists
    composeTestRule.onNodeWithTag(OrganizationFeedTestTags.ORGANIZATION_LIST).assertExists()

    // Scroll to last item
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.ORGANIZATION_LIST)
        .performScrollToNode(hasText("Organization 10"))

    composeTestRule.waitForIdle()

    // Verify last item is now visible
    composeTestRule.onNodeWithText("Organization 10").assertExists().assertIsDisplayed()
  }

  @Test
  fun organizationFeedScreen_displaysVerifiedBadge_forVerifiedOrganizations() {
    coEvery { mockRepository.getOrganizationsByOwner(testUserId) } returns flowOf(emptyList())
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Verified").assertExists()
  }

  @Test
  fun organizationFeedScaffold_callsOnOrganizationClick() {
    val testOrg =
        Organization(
            id = "org-123",
            name = "Test Organization",
            description = "Test description",
            verified = true,
            status = OrganizationStatus.ACTIVE)

    var clickedOrgId: String? = null

    composeTestRule.setContent {
      OnePassTheme {
        OrganizationFeedScaffold(
            organizations = listOf(testOrg),
            isLoading = false,
            error = null,
            onOrganizationClick = { clickedOrgId = it },
            onNavigateBack = {},
            onRetry = {})
      }
    }

    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.getTestTagForOrganizationItem("org-123"))
        .performClick()

    assert(clickedOrgId == "org-123") { "onOrganizationClick was not called with correct orgId" }
  }

  @Test
  fun organizationFeedScaffold_showsErrorState_whenErrorAndNoOrganizations() {
    composeTestRule.setContent {
      OnePassTheme {
        OrganizationFeedScaffold(
            organizations = emptyList(),
            isLoading = false,
            error = "Network error occurred",
            onOrganizationClick = {},
            onNavigateBack = {},
            onRetry = {})
      }
    }

    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.ERROR_MESSAGE)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("Oops!").assertExists()
    composeTestRule.onNodeWithText("Network error occurred").assertExists()
  }

  @Test
  fun errorState_callsOnRetry_whenRetryButtonClicked() {
    var retryCalled = false

    composeTestRule.setContent {
      OnePassTheme {
        OrganizationFeedScaffold(
            organizations = emptyList(),
            isLoading = false,
            error = "Connection failed",
            onOrganizationClick = {},
            onNavigateBack = {},
            onRetry = { retryCalled = true })
      }
    }

    composeTestRule
        .onNodeWithTag("${OrganizationFeedTestTags.ERROR_MESSAGE}_retry_button")
        .assertExists()
        .performClick()

    assert(retryCalled) { "onRetry was not called" }
  }

  @Test
  fun organizationListContent_displaysMultipleOrganizations() {
    val orgs =
        listOf(
            Organization(
                id = "org-1",
                name = "Org 1",
                description = "Desc 1",
                verified = true,
                status = OrganizationStatus.ACTIVE),
            Organization(
                id = "org-2",
                name = "Org 2",
                description = "Desc 2",
                verified = false,
                status = OrganizationStatus.ACTIVE),
            Organization(
                id = "org-3",
                name = "Org 3",
                description = "Desc 3",
                verified = true,
                status = OrganizationStatus.ACTIVE))

    composeTestRule.setContent {
      OnePassTheme {
        OrganizationFeedScaffold(
            organizations = orgs,
            isLoading = false,
            error = null,
            onOrganizationClick = {},
            onNavigateBack = {},
            onRetry = {})
      }
    }

    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.ORGANIZATION_LIST)
        .assertExists()
        .assertIsDisplayed()

    // Verify all organizations are displayed
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.getTestTagForOrganizationItem("org-1"))
        .assertExists()
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.getTestTagForOrganizationItem("org-2"))
        .assertExists()
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.getTestTagForOrganizationItem("org-3"))
        .assertExists()
  }

  @Test
  fun fab_is_displayed_and_click_triggers_callback() {
    var clicked = false

    composeTestRule.setContent {
      OnePassTheme {
        OrganizationFeedScaffold(
            organizations = emptyList(),
            isLoading = false,
            error = null,
            onOrganizationClick = {},
            onFabClick = { clicked = true },
            onNavigateBack = {},
            onRetry = {})
      }
    }

    composeTestRule.onNodeWithTag(OrganizationFeedTestTags.ADD_ORG_FAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizationFeedTestTags.ADD_ORG_FAB).performClick()

    composeTestRule.runOnIdle { assertTrue("FAB click should trigger callback", clicked) }
  }
}
