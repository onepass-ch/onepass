package ch.onepass.onepass.ui.organization

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.After
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
  fun organizationFeedScreen_displaysOrganizations_whenDataLoaded() {
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Tech Events Zurich").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Tech Events Zurich").assertExists().assertIsDisplayed()

    composeTestRule.onNodeWithText("Music Nights Geneva").assertExists().assertIsDisplayed()
  }

  @Test
  fun organizationFeedScreen_displaysOrganizationDetails_correctly() {
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }
    composeTestRule.onNodeWithText("Tech Events Zurich").assertExists().assertIsDisplayed()

    composeTestRule.onNodeWithText("Leading tech organizer").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithText("Music Nights Geneva").assertExists().assertIsDisplayed()
  }

  @Test
  fun organizationFeedScreen_organizationCard_clickTriggersNavigation() {
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
    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Tech Events Zurich").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Tech Events Zurich").assertExists().performClick()
    assert(clickedOrgId == "org1")
  }
  // ==================== Empty State Tests ====================
  @Test
  fun organizationFeedScreen_displaysEmptyState_whenNoOrganizations() {
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns flowOf(emptyList())
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.EMPTY_STATE)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("No Organizations").assertExists().assertIsDisplayed()

    composeTestRule
        .onNodeWithText("You haven't joined any organizations yet.")
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
        .onNodeWithTag(OrganizationFeedTestTags.RETRY_BUTTON)
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
    composeTestRule.onNodeWithTag(OrganizationFeedTestTags.RETRY_BUTTON).performClick()
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
        .onNodeWithContentDescription("Back")
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

  @Test
  fun viewModel_refreshOrganizations_reloadsData() {
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)
    viewModel.refreshOrganizations(testUserId)
    coVerify(atLeast = 1) { mockRepository.getOrganizationsByMember(testUserId) }
  }
  // ==================== Scrolling and List Tests ====================
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
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns flowOf(manyOrgs)
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }
    composeTestRule.onNodeWithText("Organization 1").assertExists().assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OrganizationFeedTestTags.ORGANIZATION_LIST)
        .performScrollToNode(hasText("Organization 10"))
    composeTestRule.onNodeWithText("Organization 10").assertExists().assertIsDisplayed()
  }

  @Test
  fun organizationFeedScreen_displaysVerifiedBadge_forVerifiedOrganizations() {
    coEvery { mockRepository.getOrganizationsByMember(testUserId) } returns
        flowOf(testOrganizations)
    viewModel = OrganizationFeedViewModel(mockRepository)
    composeTestRule.setContent {
      OnePassTheme { OrganizationFeedScreen(userId = testUserId, viewModel = viewModel) }
    }
    composeTestRule.onNodeWithContentDescription("Verified").assertExists()
  }
}
