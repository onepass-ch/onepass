package ch.onepass.onepass.ui.organization

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.organization.*
import ch.onepass.onepass.ui.organization.FirestoreTestHelper.waitForTag
import ch.onepass.onepass.ui.theme.OnePassTheme
import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import java.util.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OrganizationDashboardScreenFirestoreTest : FirestoreTestBase() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var userId: String
  private lateinit var orgRepository: OrganizationRepository

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      FirebaseEmulator.auth.signInAnonymously().await()
      userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
      orgRepository = OrganizationRepositoryFirebase()
    }
  }

  private suspend fun createAndLoadDashboard(
      orgCustomizer: (Organization) -> Organization = { it }
  ): Pair<String, OrganizationDashboardViewModel> {
    val org = orgCustomizer(FirestoreTestHelper.createFirestoreOrganization(userId))
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = orgRepository,
            eventRepository = repository,
            auth = FirebaseEmulator.auth)

    setDashboardScreen(orgId, viewModel)
    viewModel.loadOrganization(orgId)
    composeTestRule.waitForIdle()

    return Pair(orgId, viewModel)
  }

  @Test
  fun dashboardScreen_displaysOrganization_fromFirestore() = runTest {
    createAndLoadDashboard { it.copy(name = "Test Organization") }

    composeTestRule.waitForTag(OrganizationDashboardTestTags.ORG_SUMMARY_CARD)
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.ORG_SUMMARY_CARD)
        .assertIsDisplayed()
        .assertTextContains("TEST ORGANIZATION")
  }

  @Test
  fun dashboardScreen_displaysEvents_fromFirestore() = runTest {
    val (orgId, _) = createAndLoadDashboard()

    val event = FirestoreTestHelper.createFirestoreEvent(orgId, "Test Event")
    repository.createEvent(event).getOrThrow()

    composeTestRule.waitForTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN)
    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN).performClick()
    composeTestRule.onNodeWithText("TEST EVENT").assertIsDisplayed()
  }

  @Test
  fun dashboardScreen_displaysMultipleEvents_fromFirestore() = runTest {
    val (orgId, _) = createAndLoadDashboard()

    listOf("Event One", "Event Two", "Event Three").forEach { title ->
      repository.createEvent(FirestoreTestHelper.createFirestoreEvent(orgId, title)).getOrThrow()
    }

    composeTestRule.waitForTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN)
    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN).performClick()

    listOf("EVENT ONE", "EVENT TWO", "EVENT THREE").forEach { title ->
      composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }
  }

  @Test
  fun dashboardScreen_displaysStaffMembers_fromFirestore() = runTest {
    val member1Id = "member-${UUID.randomUUID()}"
    val member2Id = "member-${UUID.randomUUID()}"

    createAndLoadDashboard { org ->
      org.copy(
          members =
              mapOf(
                  userId to OrganizationMember(role = OrganizationRole.OWNER),
                  member1Id to OrganizationMember(role = OrganizationRole.MEMBER),
                  member2Id to OrganizationMember(role = OrganizationRole.STAFF)))
    }

    composeTestRule.waitForTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)
        .performScrollTo()
        .performClick()

    composeTestRule.onNodeWithText(member1Id).assertIsDisplayed()
    composeTestRule.onNodeWithText(member2Id).assertIsDisplayed()
  }

  @Test
  fun dashboardScreen_removeStaffMember_updatesFirestore() = runTest {
    val staffId = "staff-${UUID.randomUUID()}"

    createAndLoadDashboard { org ->
      org.copy(
          members =
              mapOf(
                  userId to OrganizationMember(role = OrganizationRole.OWNER),
                  staffId to OrganizationMember(role = OrganizationRole.STAFF)))
    }

    composeTestRule.waitForTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)
        .performScrollTo()
        .performClick()

    composeTestRule.onNodeWithText(staffId).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.getStaffRemoveButtonTag(staffId))
        .performClick()
    composeTestRule.waitForIdle()
    kotlinx.coroutines.delay(1000)

    composeTestRule.onNodeWithText(staffId).assertDoesNotExist()
  }

  @Test
  fun dashboardScreen_cannotRemoveOwner() = runTest {
    createAndLoadDashboard()

    composeTestRule.waitForTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)
        .performScrollTo()
        .performClick()

    composeTestRule.onNodeWithText(userId).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.getStaffRemoveButtonTag(userId))
        .assertDoesNotExist()
  }

  @Test
  fun dashboardScreen_memberCanEditEvents() = runTest {
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.auth.signInAnonymously().await()
    val memberId = FirebaseEmulator.auth.currentUser?.uid ?: "member-id"

    val org =
        FirestoreTestHelper.createFirestoreOrganization(
            ownerId = userId,
            members =
                mapOf(
                    userId to OrganizationMember(role = OrganizationRole.OWNER),
                    memberId to OrganizationMember(role = OrganizationRole.MEMBER)))
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val event = FirestoreTestHelper.createFirestoreEvent(orgId)
    val eventId = repository.createEvent(event).getOrThrow()

    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = orgRepository,
            eventRepository = repository,
            auth = FirebaseEmulator.auth)

    setDashboardScreen(orgId, viewModel)
    viewModel.loadOrganization(orgId)
    composeTestRule.waitForIdle()

    composeTestRule.waitForTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN)
    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN).performClick()

    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.getEventEditButtonTag(eventId))
        .assertIsDisplayed()
  }

  @Test
  fun dashboardScreen_staffCannotEditEvents() = runTest {
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.auth.signInAnonymously().await()
    val staffId = FirebaseEmulator.auth.currentUser?.uid ?: "staff-id"

    val org =
        FirestoreTestHelper.createFirestoreOrganization(
            ownerId = userId,
            members =
                mapOf(
                    userId to OrganizationMember(role = OrganizationRole.OWNER),
                    staffId to OrganizationMember(role = OrganizationRole.STAFF)))
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val event = FirestoreTestHelper.createFirestoreEvent(orgId)
    val eventId = repository.createEvent(event).getOrThrow()

    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = orgRepository,
            eventRepository = repository,
            auth = FirebaseEmulator.auth)

    setDashboardScreen(orgId, viewModel)
    viewModel.loadOrganization(orgId)
    composeTestRule.waitForIdle()

    composeTestRule.waitForTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN)
    composeTestRule.onNodeWithTag(OrganizationDashboardTestTags.YOUR_EVENTS_DROPDOWN).performClick()

    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.getEventEditButtonTag(eventId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.getEventScanButtonTag(eventId))
        .assertIsDisplayed()
  }

  @Test
  fun dashboardScreen_nonOwnerCannotManageStaff() = runTest {
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.auth.signInAnonymously().await()
    val memberId = FirebaseEmulator.auth.currentUser?.uid ?: "member-id"

    val staffId = "staff-${UUID.randomUUID()}"

    val org =
        FirestoreTestHelper.createFirestoreOrganization(
            ownerId = userId,
            members =
                mapOf(
                    userId to OrganizationMember(role = OrganizationRole.OWNER),
                    memberId to OrganizationMember(role = OrganizationRole.MEMBER),
                    staffId to OrganizationMember(role = OrganizationRole.STAFF)))
    val orgId = orgRepository.createOrganization(org).getOrThrow()

    val viewModel =
        OrganizationDashboardViewModel(
            organizationRepository = orgRepository,
            eventRepository = repository,
            auth = FirebaseEmulator.auth)

    setDashboardScreen(orgId, viewModel)
    viewModel.loadOrganization(orgId)
    composeTestRule.waitForIdle()

    composeTestRule.waitForTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.STAFF_LIST_DROPDOWN)
        .performScrollTo()
        .performClick()

    composeTestRule.onNodeWithText(staffId).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OrganizationDashboardTestTags.getStaffRemoveButtonTag(staffId))
        .assertDoesNotExist()
  }

  private fun setDashboardScreen(
      organizationId: String,
      viewModel: OrganizationDashboardViewModel
  ) {
    composeTestRule.setContent {
      OnePassTheme {
        OrganizationDashboardScreen(organizationId = organizationId, viewModel = viewModel)
      }
    }
  }
}
