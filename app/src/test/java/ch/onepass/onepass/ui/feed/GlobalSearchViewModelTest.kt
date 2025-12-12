package ch.onepass.onepass.ui.feed

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.user.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchViewModelTest {

  private val mockEventRepo = mockk<EventRepository>()
  private val mockOrgRepo = mockk<OrganizationRepository>()
  private val mockUserRepo = mockk<UserRepository>()

  private lateinit var viewModel: GlobalSearchViewModel
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    viewModel =
        GlobalSearchViewModel(
            userRepo = mockUserRepo, eventRepo = mockEventRepo, orgRepo = mockOrgRepo)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun on_query_changed_correctly_updates_events_and_organizations() = runTest {
    val event1 = Event(eventId = "1", title = "Event One", status = EventStatus.PUBLISHED)
    val event2 = Event(eventId = "2", title = "Event Two", status = EventStatus.PUBLISHED)
    val org1 = Organization(id = "o1", name = "Org One")
    val org2 = Organization(id = "o2", name = "Org Two")

    // Setup mocks for "event" query
    coEvery { mockEventRepo.getEventsByStatus(EventStatus.PUBLISHED) } returns
        flowOf(listOf(event1, event2))
    coEvery { mockOrgRepo.searchOrganizations("event") } returns flowOf(emptyList())
    coEvery { mockUserRepo.searchUsers(any(), any(), any()) } returns Result.success(emptyList())

    // Query: "event"
    viewModel.onQueryChanged("event")
    val stateAfterEventQuery = viewModel.uiState.first { !it.isLoading }

    assertEquals("event", stateAfterEventQuery.query)
    assertEquals(2, stateAfterEventQuery.events.size)
    assertEquals(0, stateAfterEventQuery.organizations.size)
    assertEquals(0, stateAfterEventQuery.users.size)
    assertEquals(false, stateAfterEventQuery.isLoading)
    assertEquals(null, stateAfterEventQuery.error)

    // Setup mocks for "org" query
    coEvery { mockOrgRepo.searchOrganizations("org") } returns flowOf(listOf(org1, org2))
    coEvery { mockEventRepo.getEventsByStatus(EventStatus.PUBLISHED) } returns flowOf(emptyList())

    // Query: "org"
    viewModel.onQueryChanged("org")
    val stateAfterOrgQuery = viewModel.uiState.first { !it.isLoading }

    assertEquals("org", stateAfterOrgQuery.query)
    assertEquals(0, stateAfterOrgQuery.events.size)
    assertEquals(2, stateAfterOrgQuery.organizations.size)
    assertEquals(0, stateAfterOrgQuery.users.size)
    assertEquals(false, stateAfterOrgQuery.isLoading)
    assertEquals(null, stateAfterOrgQuery.error)
  }

  @Test
  fun fetch_next_page_does_not_trigger_when_query_is_blank() = runTest {
    // Initial state with blank query
    viewModel.onQueryChanged(" ")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // State should remain unchanged
    assertEquals("", state.query.trim())
    assertEquals(0, state.events.size)
    assertEquals(0, state.organizations.size)
    assertEquals(0, state.users.size)
  }

  @Test
  fun ui_state_sets_error_when_repository_throws() = runTest {
    // Setup mocks to force failure
    coEvery { mockEventRepo.getEventsByStatus(EventStatus.PUBLISHED) } throws Exception("Failed")
    coEvery { mockOrgRepo.searchOrganizations(any()) } returns flowOf(emptyList())
    coEvery { mockUserRepo.searchUsers(any(), any(), any()) } returns Result.success(emptyList())

    // Trigger search
    viewModel.onQueryChanged("failTest")
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify error state
    val state = viewModel.uiState.value
    assertEquals("failTest", state.query)
    assertEquals("Failed", state.error)
    assertEquals(0, state.events.size)
    assertEquals(0, state.organizations.size)
    assertEquals(0, state.users.size)
    assertEquals(false, state.isLoading)
  }

  @Test
  fun fuzzy_and_prefix_matching_updates_events_correctly() = runTest {
    val event1 = Event(eventId = "1", title = "Kotlin Coroutines", status = EventStatus.PUBLISHED)
    val event2 = Event(eventId = "2", title = "Compose Tutorial", status = EventStatus.PUBLISHED)
    val event3 = Event(eventId = "3", title = "Kotlin Flow Basics", status = EventStatus.PUBLISHED)

    coEvery { mockEventRepo.getEventsByStatus(EventStatus.PUBLISHED) } returns
        flowOf(listOf(event1, event2, event3))
    coEvery { mockOrgRepo.searchOrganizations(any()) } returns flowOf(emptyList())
    coEvery { mockUserRepo.searchUsers(any(), any(), any()) } returns Result.success(emptyList())

    // Exact match on first word
    viewModel.onQueryChanged("Kotlin")
    var state = viewModel.uiState.first { !it.isLoading }
    assertEquals(2, state.events.size) // event1 + event3

    // Prefix match on middle word
    viewModel.onQueryChanged("Tut") // should match "Compose Tutorial"
    state = viewModel.uiState.first { !it.isLoading }
    assertEquals(1, state.events.size)
    assertEquals("Compose Tutorial", state.events.first().title)

    // Fuzzy match (small typo)
    viewModel.onQueryChanged("Corutines") // typo for "Coroutines"
    state = viewModel.uiState.first { !it.isLoading }
    assertEquals(1, state.events.size)
    assertEquals("Kotlin Coroutines", state.events.first().title)

    // Non-matching query
    viewModel.onQueryChanged("Swift")
    state = viewModel.uiState.first { !it.isLoading }
    assertEquals(0, state.events.size)
  }
}
