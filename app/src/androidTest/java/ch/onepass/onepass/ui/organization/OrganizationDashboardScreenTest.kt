package ch.onepass.onepass.ui.organization

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.ui.theme.OnePassTheme
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test

class OrganizationDashboardScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val mockAuth = OrganizationDashboardTestData.createMockAuth().first
  private val mockUser = OrganizationDashboardTestData.createMockAuth().second
  private val testOrg = OrganizationDashboardTestData.createTestOrganization()
  private val testEvent1 = OrganizationDashboardTestData.createTestEvent()
  private val testEvent2 =
      OrganizationDashboardTestData.createTestEvent(
          eventId = "event-2",
          title = "Test Event 2",
          status = ch.onepass.onepass.model.event.EventStatus.DRAFT,
          capacity = 200,
          ticketsRemaining = 200)

  private fun setScreen(
      orgId: String = "test-org-1",
      viewModel: OrganizationDashboardViewModel,
      onNavigateBack: () -> Unit = {},
      onNavigateToProfile: (String) -> Unit = {},
      onNavigateToCreateEvent: (String) -> Unit = {},
      onNavigateToAddStaff: (String) -> Unit = {},
      onNavigateToScanTickets: (String) -> Unit = {},
      onNavigateToEditEvent: (String) -> Unit = {}
  ) {
    composeTestRule.setContent {
      OnePassTheme {
        OrganizationDashboardScreen(
            organizationId = orgId,
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToCreateEvent = onNavigateToCreateEvent,
            onNavigateToAddStaff = onNavigateToAddStaff,
            onNavigateToScanTickets = onNavigateToScanTickets,
            onNavigateToEditEvent = onNavigateToEditEvent)
      }
    }
  }

  private fun waitForTag(tag: String, timeout: Long = 5000) {
    composeTestRule.waitUntil(timeoutMillis = timeout) {
      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
  }

  @Test
  fun organizationDashboardScreen_displaysAllComponents() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(events = listOf(testEvent1, testEvent2)))
    setScreen(viewModel = viewModel)

    waitForTag(OrganizationDashboardTestTags.ORG_SUMMARY_CARD)

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun organizationDashboardScreen_displaysOrganizationSummary() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository())
    setScreen(viewModel = viewModel)

    waitForTag(OrganizationDashboardTestTags.ORG_SUMMARY_CARD)

    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.ORG_SUMMARY_CARD)
        .assertIsDisplayed()
    composeTestRule
        .onNode(hasTestTag(OrganizationDashboardTestTags.ORG_NAME), useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals(testOrg.name.uppercase(Locale.getDefault()))
    composeTestRule
        .onNode(hasTestTag(OrganizationDashboardTestTags.ORG_FOLLOWERS), useUnmergedTree = true)
        .assertTextContains("1.5K FOLLOWERS")
    composeTestRule
        .onNode(hasTestTag(OrganizationDashboardTestTags.ORG_RATING), useUnmergedTree = true)
        .assertTextEquals("4.5")
  }

  @Test
  fun organizationDashboardScreen_displaysManageEventsSection() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(events = listOf(testEvent1, testEvent2)))
    setScreen(viewModel = viewModel)

    waitForTag(OrganizationDashboardTestTags.MANAGE_EVENTS_SECTION)

    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.MANAGE_EVENTS_SECTION)
        .assertIsDisplayed()
  }

  @Test
  fun organizationDashboardScreen_displaysManageStaffSection() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository())
    setScreen(viewModel = viewModel)

    waitForTag(OrganizationDashboardTestTags.MANAGE_STAFF_SECTION)

    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.MANAGE_STAFF_SECTION)
        .assertIsDisplayed()
  }

  @Test
  fun organizationDashboardScreen_displaysLoadingState() {
    val mockOrgRepo =
        object : MockOrganizationRepository(organization = null) {
          override fun getOrganizationById(organizationId: String) = flow {
            delay(2000)
            emit(null)
          }
        }
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = mockOrgRepo,
            eventRepository = MockEventRepository(),
            auth = mockAuth)
    setScreen(viewModel = viewModel)

    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.LOADING_INDICATOR)
        .assertIsDisplayed()
  }

  @Test
  fun organizationDashboardScreen_displaysErrorState() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(shouldThrowError = true),
            eventRepository = MockEventRepository())
    setScreen(viewModel = viewModel)

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(OrganizationDashboardTestTags.ERROR_MESSAGE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Oops!").assertIsDisplayed()
  }

  @Test
  fun organizationDashboardScreen_clicksBackButton() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository())
    var clicked = false
    setScreen(viewModel = viewModel, onNavigateBack = { clicked = true })

    waitForTag(OrganizationDashboardTestTags.BACK_BUTTON)

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.BACK_BUTTON).performClick()
    assert(clicked)
  }

  @Test
  fun organizationDashboardScreen_clicksOrganizationSummaryCard() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository())
    var clickedOrgId: String? = null
    setScreen(viewModel = viewModel, onNavigateToProfile = { clickedOrgId = it })

    waitForTag(OrganizationDashboardTestTags.ORG_SUMMARY_CARD)

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.ORG_SUMMARY_CARD).performClick()
    assert(clickedOrgId == "test-org-1")
  }
}
