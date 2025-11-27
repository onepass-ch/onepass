package ch.onepass.onepass.ui.organization

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.organization.OrganizationRole
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
  private val testOrg = OrganizationDashboardTestData.createTestOrganization(members = emptyMap())
  private val testEvent1 = OrganizationDashboardTestData.createTestEvent()
  private val testEvent2 =
      OrganizationDashboardTestData.createTestEvent(
          eventId = "event-2",
          title = "Test Event 2",
          status = ch.onepass.onepass.model.event.EventStatus.DRAFT,
          capacity = 200,
          ticketsRemaining = 200)

  private val testMemberships =
      listOf(
          OrganizationDashboardTestData.createTestMembership(
              membershipId = "mem-1", userId = "owner-1", role = OrganizationRole.OWNER),
          OrganizationDashboardTestData.createTestMembership(
              membershipId = "mem-2", userId = "member-1", role = OrganizationRole.MEMBER),
          OrganizationDashboardTestData.createTestMembership(
              membershipId = "mem-3", userId = "staff-1", role = OrganizationRole.STAFF))

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
            eventRepository = MockEventRepository(events = listOf(testEvent1, testEvent2)),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
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
            eventRepository = MockEventRepository(),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
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
            eventRepository = MockEventRepository(events = listOf(testEvent1, testEvent2)),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
    setScreen(viewModel = viewModel)

    waitForTag(OrganizationDashboardTestTags.MANAGE_EVENTS_SECTION)

    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.MANAGE_EVENTS_SECTION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.CREATE_EVENT_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN)
        .assertIsDisplayed()
  }

  @Test
  fun organizationDashboardScreen_displaysManageStaffSection() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
    setScreen(viewModel = viewModel)

    waitForTag(OrganizationDashboardTestTags.MANAGE_STAFF_SECTION)

    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.MANAGE_STAFF_SECTION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.ADD_STAFF_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)
        .assertIsDisplayed()
  }

  @Test
  fun organizationDashboardScreen_expandsYourEventsDropdown() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(events = listOf(testEvent1, testEvent2)),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
    setScreen(viewModel = viewModel)

    waitForTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN)

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN).performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNode(
            hasTestTag(OrganizationDashboardTestTags.getEventCardTag("event-1")),
            useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNode(
            hasTestTag(OrganizationDashboardTestTags.getEventCardTag("event-2")),
            useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun organizationDashboardScreen_expandsStaffListDropdown() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
    setScreen(viewModel = viewModel)

    waitForTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)

    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)
        .performScrollTo()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN).performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNode(
            hasTestTag(OrganizationDashboardTestTags.getStaffItemTag("owner-1")),
            useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNode(
            hasTestTag(OrganizationDashboardTestTags.getStaffItemTag("member-1")),
            useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNode(
            hasTestTag(OrganizationDashboardTestTags.getStaffItemTag("staff-1")),
            useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun organizationDashboardScreen_clicksCreateEventButton() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
    var clicked = false
    setScreen(viewModel = viewModel, onNavigateToCreateEvent = { clicked = true })

    waitForTag(OrganizationDashboardTestTags.CREATE_EVENT_BUTTON)

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.CREATE_EVENT_BUTTON).performClick()
    assert(clicked)
  }

  @Test
  fun organizationDashboardScreen_clicksAddStaffButton() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
    var clicked = false
    setScreen(viewModel = viewModel, onNavigateToAddStaff = { clicked = true })

    waitForTag(OrganizationDashboardTestTags.ADD_STAFF_BUTTON)

    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.ADD_STAFF_BUTTON)
        .performScrollTo()
        .performClick()
    assert(clicked)
  }

  @Test
  fun organizationDashboardScreen_clicksEventScanButton() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(events = listOf(testEvent1)),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository(),
            auth = mockAuth)
    var clickedEventId: String? = null
    setScreen(viewModel = viewModel, onNavigateToScanTickets = { clickedEventId = it })

    waitForTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN)

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodes(
              hasTestTag(OrganizationDashboardTestTags.getEventScanButtonTag("event-1")),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNode(
            hasTestTag(OrganizationDashboardTestTags.getEventScanButtonTag("event-1")),
            useUnmergedTree = true)
        .performClick()

    assert(clickedEventId == "event-1")
  }

  @Test
  fun organizationDashboardScreen_clicksEventEditButton() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(events = listOf(testEvent1)),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository(),
            auth = mockAuth)
    var clickedEventId: String? = null
    setScreen(viewModel = viewModel, onNavigateToEditEvent = { clickedEventId = it })

    waitForTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN)

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodes(
              hasTestTag(OrganizationDashboardTestTags.getEventEditButtonTag("event-1")),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNode(
            hasTestTag(OrganizationDashboardTestTags.getEventEditButtonTag("event-1")),
            useUnmergedTree = true)
        .performClick()

    assert(clickedEventId == "event-1")
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
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository(),
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
            eventRepository = MockEventRepository(),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
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
            eventRepository = MockEventRepository(),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
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
            eventRepository = MockEventRepository(),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
    var clickedOrgId: String? = null
    setScreen(viewModel = viewModel, onNavigateToProfile = { clickedOrgId = it })

    waitForTag(OrganizationDashboardTestTags.ORG_SUMMARY_CARD)

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.ORG_SUMMARY_CARD).performClick()
    assert(clickedOrgId == "test-org-1")
  }

  @Test
  fun organizationDashboardScreen_displaysNoEventsMessage() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(events = emptyList()),
            membershipRepository = MockMembershipRepository(testMemberships),
            userRepository = MockUserRepository())
    setScreen(viewModel = viewModel)

    waitForTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN)

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("No events created yet.").assertIsDisplayed()
  }

  @Test
  fun organizationDashboardScreen_displaysNoStaffMessage() {
    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = MockOrganizationRepository(organization = testOrg),
            eventRepository = MockEventRepository(),
            membershipRepository = MockMembershipRepository(members = emptyList()),
            userRepository = MockUserRepository())
    setScreen(viewModel = viewModel)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("No staff members added yet.").assertIsDisplayed()
  }
}
