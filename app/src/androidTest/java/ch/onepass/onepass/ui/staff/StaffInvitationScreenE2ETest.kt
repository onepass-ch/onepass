package ch.onepass.onepass.ui.staff

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ch.onepass.onepass.model.organization.FakeOrganizationRepository
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.model.user.FakeUserRepository
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.model.user.UserSearchType
import ch.onepass.onepass.ui.staff.StaffTestTags.Item
import ch.onepass.onepass.ui.theme.OnePassTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StaffInvitationScreenE2ETest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val testDispatcher = StandardTestDispatcher()
  private val orgId = "org_e2e"
  private val currentUser = User(uid = "u123", email = "me@onepass.ch", displayName = "Me")

  private lateinit var userRepo: FakeUserRepository
  private lateinit var orgRepo: FakeOrganizationRepository
  private lateinit var viewModel: StaffInvitationViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    userRepo = FakeUserRepository(currentUser = currentUser, createdUser = currentUser)
    orgRepo = FakeOrganizationRepository()
    viewModel =
        StaffInvitationViewModel(
            organizationId = orgId, userRepository = userRepo, organizationRepository = orgRepo)

    composeRule.setContent {
      OnePassTheme { StaffInvitationScreen(viewModel = viewModel, onNavigateBack = {}) }
    }
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun advanceDebounce() {
    testDispatcher.scheduler.advanceTimeBy(500)
    testDispatcher.scheduler.advanceUntilIdle()
    composeRule.waitForIdle()
  }

  @Test
  fun e2e_searchInvite_success_thenAlreadyInvitedOnSecondClick() {
    val user1 = StaffSearchResult("1", "john@example.com", "John Doe", null)
    val user2 = StaffSearchResult("2", "jane@example.com", "Jane Smith", null)
    userRepo.setSearchResults(listOf(user1, user2))

    // Switch to email tab and search
    composeRule.onNodeWithTag(StaffInvitationTestTags.TAB_EMAIL).performClick()
    composeRule.onNodeWithTag(StaffInvitationTestTags.SEARCH_FIELD).performTextInput("john@example.com")

    advanceDebounce()

    // Results visible
    composeRule.onNodeWithText("John Doe").assertIsDisplayed()
    composeRule.onNodeWithTag(StaffInvitationTestTags.RESULTS_LIST).assertIsDisplayed()

    // Invite John
    composeRule.onAllNodesWithTag(Item.LIST_ITEM).fetchSemanticsNodes()
    composeRule.onNodeWithText("John Doe").performClick()
    testDispatcher.scheduler.advanceUntilIdle()
    composeRule.waitForIdle()

    // Repository should contain one invitation for John
    // (Fake repo doesn't expose state directly, but we can infer via second click behavior)

    // Second click should yield AlreadyInvited message
    composeRule.onNodeWithText("John Doe").performClick()
    testDispatcher.scheduler.advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(StaffInvitationTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun e2e_searchError_showsErrorMessage() {
    userRepo.setSearchError(RuntimeException("Network error"))

    composeRule.onNodeWithTag(StaffInvitationTestTags.SEARCH_FIELD).performTextInput("alice")

    advanceDebounce()

    composeRule.onNodeWithTag(StaffInvitationTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun e2e_alreadyInvitedPending_showsAlreadyInvitedMessage() {
    val target = StaffSearchResult("10", "already@onepass.ch", "Already Invited", null)
    userRepo.setSearchResults(listOf(target))

    // Pre-populate a pending invitation in fake repo
    val pre =
        OrganizationInvitation(
            id = "inv1",
            orgId = orgId,
            inviteeEmail = target.email,
            role = OrganizationRole.STAFF,
            invitedBy = currentUser.uid,
            status = InvitationStatus.PENDING)
    orgRepo.addTestInvitation(pre)

    composeRule.onNodeWithTag(StaffInvitationTestTags.SEARCH_FIELD).performTextInput("already")
    advanceDebounce()

    composeRule.onNodeWithText("Already Invited").performClick()
    testDispatcher.scheduler.advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(StaffInvitationTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun e2e_alreadyMemberAccepted_showsAlreadyMemberMessage() {
    val target = StaffSearchResult("20", "member@onepass.ch", "Existing Member", null)
    userRepo.setSearchResults(listOf(target))

    val accepted =
        OrganizationInvitation(
            id = "inv2",
            orgId = orgId,
            inviteeEmail = target.email,
            role = OrganizationRole.STAFF,
            invitedBy = currentUser.uid,
            status = InvitationStatus.ACCEPTED)
    orgRepo.addTestInvitation(accepted)

    composeRule.onNodeWithTag(StaffInvitationTestTags.SEARCH_FIELD).performTextInput("member")
    advanceDebounce()

    composeRule.onNodeWithText("Existing Member").performClick()
    testDispatcher.scheduler.advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(StaffInvitationTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun e2e_createInvitationError_showsError() {
    // Recreate VM with failing org repo
    orgRepo = FakeOrganizationRepository(shouldThrowOnCreate = true)
    viewModel =
        StaffInvitationViewModel(
            organizationId = orgId, userRepository = userRepo, organizationRepository = orgRepo)
    composeRule.setContent {
      OnePassTheme { StaffInvitationScreen(viewModel = viewModel, onNavigateBack = {}) }
    }

    val target = StaffSearchResult("30", "fail@onepass.ch", "Create Fails", null)
    userRepo.setSearchResults(listOf(target))

    composeRule.onNodeWithTag(StaffInvitationTestTags.SEARCH_FIELD).performTextInput("fail")
    advanceDebounce()

    composeRule.onNodeWithText("Create Fails").performClick()
    testDispatcher.scheduler.advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(StaffInvitationTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }
}
