package ch.onepass.onepass.ui.organizerprofile

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.utils.EventTestData
import ch.onepass.onepass.utils.OrganizationTestData
import com.google.firebase.Timestamp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for OrganizerProfileViewModel using mock repositories.
 * Tests all functionality including organization loading, event management,
 * user interactions, error handling, and state management.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class OrganizerProfileViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  // Mock Organization Repository
  private class MockOrganizationRepository(
      private val organizations: Map<String, Organization> = emptyMap(),
      private val shouldThrowError: Boolean = false
  ) : OrganizationRepository {
    override suspend fun createOrganization(organization: Organization): Result<String> =
        if (shouldThrowError) Result.failure(Exception("Test error"))
        else Result.success(organization.id)

    override suspend fun updateOrganization(organization: Organization): Result<Unit> =
        if (shouldThrowError) Result.failure(Exception("Test error"))
        else Result.success(Unit)

    override suspend fun deleteOrganization(organizationId: String): Result<Unit> =
        if (shouldThrowError) Result.failure(Exception("Test error"))
        else Result.success(Unit)

    override fun getOrganizationById(organizationId: String): Flow<Organization?> {
      if (shouldThrowError) {
        throw Exception("Test error")
      }
      return flowOf(organizations[organizationId])
    }

    override fun getOrganizationsByOwner(ownerId: String): Flow<List<Organization>> =
        flowOf(organizations.values.filter { it.ownerId == ownerId })

    override fun getOrganizationsByMember(userId: String): Flow<List<Organization>> = flowOf(emptyList())

    override fun getOrganizationsByStatus(status: OrganizationStatus): Flow<List<Organization>> =
        flowOf(organizations.values.filter { it.status == status })

    override fun searchOrganizations(query: String): Flow<List<Organization>> = flowOf(emptyList())

    override fun getVerifiedOrganizations(): Flow<List<Organization>> =
        flowOf(organizations.values.filter { it.verified })

    override suspend fun addMember(
        organizationId: String,
        userId: String,
        role: ch.onepass.onepass.model.organization.OrganizationRole
    ): Result<Unit> = Result.success(Unit)

    override suspend fun removeMember(organizationId: String, userId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun updateMemberRole(
        organizationId: String,
        userId: String,
        newRole: ch.onepass.onepass.model.organization.OrganizationRole
    ): Result<Unit> = Result.success(Unit)

    override suspend fun createInvitation(
        invitation: ch.onepass.onepass.model.organization.OrganizationInvitation
    ): Result<String> = Result.success("invite-id")

    override fun getPendingInvitations(organizationId: String): Flow<List<ch.onepass.onepass.model.organization.OrganizationInvitation>> =
        flowOf(emptyList())

    override fun getInvitationsByEmail(email: String): Flow<List<ch.onepass.onepass.model.organization.OrganizationInvitation>> =
        flowOf(emptyList())

    override suspend fun updateInvitationStatus(
        invitationId: String,
        newStatus: ch.onepass.onepass.model.organization.InvitationStatus
    ): Result<Unit> = Result.success(Unit)

    override suspend fun deleteInvitation(invitationId: String): Result<Unit> = Result.success(Unit)
  }

  // Mock Event Repository
  private class MockEventRepository(
      private val eventsByOrg: Map<String, List<Event>> = emptyMap(),
      private val shouldThrowError: Boolean = false
  ) : EventRepository {
    override fun getAllEvents(): Flow<List<Event>> = flowOf(emptyList())

    override fun searchEvents(query: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventById(eventId: String): Flow<Event?> = flowOf(null)

    override fun getEventsByOrganization(orgId: String): Flow<List<Event>> {
      if (shouldThrowError) {
        throw Exception("Test error")
      }
      return flowOf(eventsByOrg[orgId] ?: emptyList())
    }

    override fun getEventsByLocation(center: ch.onepass.onepass.model.map.Location, radiusKm: Double): Flow<List<Event>> =
        flowOf(emptyList())

    override fun getEventsByTag(tag: String): Flow<List<Event>> = flowOf(emptyList())

    override fun getFeaturedEvents(): Flow<List<Event>> = flowOf(emptyList())

    override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> = flowOf(emptyList())

    override suspend fun createEvent(event: Event): Result<String> = Result.success("event-id")

    override suspend fun updateEvent(event: Event): Result<Unit> = Result.success(Unit)

    override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ========================================
  // Tests for Initial State
  // ========================================

  @Test
  fun initialState_hasCorrectDefaults() = runTest {
    val orgRepository = MockOrganizationRepository()
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    val state = viewModel.state.value

    assertEquals("", state.organizationId)
    assertEquals("", state.name)
    assertEquals("", state.description)
    assertNull(state.profileImageUrl)
    assertNull(state.websiteUrl)
    assertNull(state.instagramUrl)
    assertNull(state.tiktokUrl)
    assertNull(state.facebookUrl)
    assertEquals(0, state.followersCount)
    assertFalse(state.isFollowing)
    assertFalse(state.isVerified)
    assertEquals(0, state.eventCount)
    assertEquals(OrganizerProfileTab.UPCOMING, state.selectedTab)
    assertTrue(state.upcomingEvents.isEmpty())
    assertTrue(state.pastEvents.isEmpty())
    assertTrue(state.loading)
    assertNull(state.errorMessage)
  }

  // ========================================
  // Tests for loadOrganizationProfile()
  // ========================================

  @Test
  fun loadOrganizationProfile_validOrganization_loadsSuccessfully() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-valid",
        name = "Test Organizer",
        description = "A test organization",
        ownerId = "owner-1",
        followerCount = 2500,
        verified = true,
        website = "https://test.com",
        instagram = "https://instagram.com/test",
        tiktok = "https://tiktok.com/@test",
        facebook = "https://facebook.com/test",
        profileImageUrl = "https://example.com/profile.jpg"
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-valid" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-valid")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertEquals("org-valid", state.organizationId)
    assertEquals("Test Organizer", state.name)
    assertEquals("A test organization", state.description)
    assertEquals(2500, state.followersCount)
    assertTrue(state.isVerified)
    assertEquals("https://test.com", state.websiteUrl)
    assertEquals("https://instagram.com/test", state.instagramUrl)
    assertEquals("https://tiktok.com/@test", state.tiktokUrl)
    assertEquals("https://facebook.com/test", state.facebookUrl)
    assertEquals("https://example.com/profile.jpg", state.profileImageUrl)
    assertFalse(state.loading)
    assertNull(state.errorMessage)
  }

  @Test
  fun loadOrganizationProfile_nonExistentOrganization_setsErrorMessage() = runTest {
    val orgRepository = MockOrganizationRepository()
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("non-existent-org")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertFalse(state.loading)
    assertEquals("Organization not found", state.errorMessage)
  }

  @Test
  fun loadOrganizationProfile_setsLoadingState() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-loading",
        name = "Loading Test",
        ownerId = "owner-1"
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-loading" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-loading")

    // Check that loading is true immediately (before advanceUntilIdle)
    assertTrue(viewModel.state.value.loading)
    assertNull(viewModel.state.value.errorMessage)

    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.state.value.loading)
  }

  @Test
  fun loadOrganizationProfile_repositoryError_setsErrorMessage() = runTest {
    val orgRepository = MockOrganizationRepository(shouldThrowError = true)
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-error")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertFalse(state.loading)
    assertNotNull(state.errorMessage)
    assertTrue(state.errorMessage!!.contains("Test error"))
  }

  // ========================================
  // Tests for Event Loading and Partitioning
  // ========================================

  @Test
  fun loadOrganizationProfile_loadsUpcomingAndPastEvents() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-with-events",
        name = "Event Organizer",
        ownerId = "owner-1"
    )

    // Create upcoming events (in the future)
    val upcomingEvent1 = EventTestData.createTestEvent(
        eventId = "event-upcoming-1",
        title = "Future Event 1",
        organizerId = "org-with-events",
        startTime = EventTestData.createFutureTimestamp(daysFromNow = 10),
        endTime = EventTestData.createFutureTimestamp(daysFromNow = 10, addHours = 2)
    )
    val upcomingEvent2 = EventTestData.createTestEvent(
        eventId = "event-upcoming-2",
        title = "Future Event 2",
        organizerId = "org-with-events",
        startTime = EventTestData.createFutureTimestamp(daysFromNow = 20),
        endTime = EventTestData.createFutureTimestamp(daysFromNow = 20, addHours = 3)
    )

    // Create past events
    val pastEvent1 = EventTestData.createTestEvent(
        eventId = "event-past-1",
        title = "Past Event 1",
        organizerId = "org-with-events",
        startTime = EventTestData.createPastTimestamp(daysAgo = 10),
        endTime = EventTestData.createPastTimestamp(daysAgo = 10, addHours = 2)
    )
    val pastEvent2 = EventTestData.createTestEvent(
        eventId = "event-past-2",
        title = "Past Event 2",
        organizerId = "org-with-events",
        startTime = EventTestData.createPastTimestamp(daysAgo = 5),
        endTime = EventTestData.createPastTimestamp(daysAgo = 5, addHours = 1)
    )

    val allEvents = listOf(upcomingEvent1, upcomingEvent2, pastEvent1, pastEvent2)

    val orgRepository = MockOrganizationRepository(mapOf("org-with-events" to testOrg))
    val eventRepository = MockEventRepository(mapOf("org-with-events" to allEvents))
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-with-events")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertEquals(4, state.eventCount)
    assertEquals(2, state.upcomingEvents.size)
    assertEquals(2, state.pastEvents.size)

    // Verify upcoming events
    val upcomingTitles = state.upcomingEvents.map { it.title }
    assertTrue(upcomingTitles.contains("Future Event 1"))
    assertTrue(upcomingTitles.contains("Future Event 2"))

    // Verify past events
    val pastTitles = state.pastEvents.map { it.title }
    assertTrue(pastTitles.contains("Past Event 1"))
    assertTrue(pastTitles.contains("Past Event 2"))
  }

  @Test
  fun loadOrganizationProfile_noEvents_showsEmptyLists() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-no-events",
        name = "No Events Org",
        ownerId = "owner-1"
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-no-events" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-no-events")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value

    assertEquals("org-no-events", state.organizationId)
    assertEquals("No Events Org", state.name)
    assertEquals(0, state.eventCount)
    assertTrue(state.upcomingEvents.isEmpty())
    assertTrue(state.pastEvents.isEmpty())
  }

  @Test
  fun loadOrganizationProfile_eventLoadingError_emitsErrorEffect() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-events-error",
        name = "Events Error Org",
        ownerId = "owner-1"
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-events-error" to testOrg))
    val eventRepository = MockEventRepository(shouldThrowError = true)
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    // Start collecting effects BEFORE triggering the action
    val effects = mutableListOf<OrganizerProfileEffect>()
    val job = launch {
      viewModel.effects.collect { effects.add(it) }
    }

    viewModel.loadOrganizationProfile("org-events-error")
    testDispatcher.scheduler.advanceUntilIdle()

    job.cancel()

    assertTrue("Should emit error effect", effects.any { it is OrganizerProfileEffect.ShowError })
  }

  // ========================================
  // Tests for Follow/Unfollow Functionality
  // ========================================

  @Test
  fun onFollowClicked_notFollowing_togglesToFollowing() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-follow",
        name = "Follow Test Org",
        ownerId = "owner-1",
        followerCount = 1000
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-follow" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-follow")
    testDispatcher.scheduler.advanceUntilIdle()

    val initialState = viewModel.state.value
    assertEquals(1000, initialState.followersCount)
    assertFalse(initialState.isFollowing)

    viewModel.onFollowClicked()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue("Should be following after click", viewModel.state.value.isFollowing)
    assertEquals(1001, viewModel.state.value.followersCount)
  }

  @Test
  fun onFollowClicked_alreadyFollowing_togglesToNotFollowing() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-unfollow",
        name = "Unfollow Test Org",
        ownerId = "owner-1",
        followerCount = 500
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-unfollow" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-unfollow")
    testDispatcher.scheduler.advanceUntilIdle()

    // First click to follow
    viewModel.onFollowClicked()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.state.value.isFollowing)
    assertEquals(501, viewModel.state.value.followersCount)

    // Second click to unfollow
    viewModel.onFollowClicked()
    testDispatcher.scheduler.advanceUntilIdle()
    assertFalse("Should be unfollowing after second click", viewModel.state.value.isFollowing)
    assertEquals(500, viewModel.state.value.followersCount)
  }

  @Test
  fun onFollowClicked_multipleClicks_togglesCorrectly() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-multi-follow",
        name = "Multi Follow Test",
        ownerId = "owner-1",
        followerCount = 100
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-multi-follow" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-multi-follow")
    testDispatcher.scheduler.advanceUntilIdle()

    // Click 1: Follow
    viewModel.onFollowClicked()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.state.value.isFollowing)
    assertEquals(101, viewModel.state.value.followersCount)

    // Click 2: Unfollow
    viewModel.onFollowClicked()
    testDispatcher.scheduler.advanceUntilIdle()
    assertFalse(viewModel.state.value.isFollowing)
    assertEquals(100, viewModel.state.value.followersCount)

    // Click 3: Follow again
    viewModel.onFollowClicked()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.state.value.isFollowing)
    assertEquals(101, viewModel.state.value.followersCount)
  }

  // ========================================
  // Tests for Tab Selection
  // ========================================

  @Test
  fun onTabSelected_changesSelectedTab() = runTest {
    val orgRepository = MockOrganizationRepository()
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    assertEquals(OrganizerProfileTab.UPCOMING, viewModel.state.value.selectedTab)

    viewModel.onTabSelected(OrganizerProfileTab.POSTS)
    assertEquals(OrganizerProfileTab.POSTS, viewModel.state.value.selectedTab)

    viewModel.onTabSelected(OrganizerProfileTab.PAST)
    assertEquals(OrganizerProfileTab.PAST, viewModel.state.value.selectedTab)

    viewModel.onTabSelected(OrganizerProfileTab.UPCOMING)
    assertEquals(OrganizerProfileTab.UPCOMING, viewModel.state.value.selectedTab)
  }

  @Test
  fun onTabSelected_selectingSameTab_remainsSelected() = runTest {
    val orgRepository = MockOrganizationRepository()
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    assertEquals(OrganizerProfileTab.UPCOMING, viewModel.state.value.selectedTab)

    viewModel.onTabSelected(OrganizerProfileTab.UPCOMING)
    assertEquals(OrganizerProfileTab.UPCOMING, viewModel.state.value.selectedTab)

    viewModel.onTabSelected(OrganizerProfileTab.UPCOMING)
    assertEquals(OrganizerProfileTab.UPCOMING, viewModel.state.value.selectedTab)
  }

  // ========================================
  // Tests for Event Navigation
  // ========================================

  @Test
  fun onEventClicked_emitsNavigateToEventEffect() = runTest {
    val orgRepository = MockOrganizationRepository()
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    val effects = mutableListOf<OrganizerProfileEffect>()
    val collectJob = launch {
      viewModel.effects.collect { effects.add(it) }
    }

    viewModel.onEventClicked("event-123")
    testDispatcher.scheduler.advanceUntilIdle()

    collectJob.cancel()

    assertTrue("Should emit NavigateToEvent effect", effects.any { it is OrganizerProfileEffect.NavigateToEvent })
    val effect = effects.find { it is OrganizerProfileEffect.NavigateToEvent } as OrganizerProfileEffect.NavigateToEvent
    assertEquals("event-123", effect.eventId)
  }

  @Test
  fun onEventClicked_differentEventIds_emitsCorrectEffects() = runTest {
    val orgRepository = MockOrganizationRepository()
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    val effects = mutableListOf<OrganizerProfileEffect>()
    val collectJob = launch {
      viewModel.effects.collect { effects.add(it) }
    }

    viewModel.onEventClicked("event-001")
    testDispatcher.scheduler.advanceUntilIdle()

    collectJob.cancel()

    assertTrue("Should emit NavigateToEvent effect", effects.any { it is OrganizerProfileEffect.NavigateToEvent })
    val effect1 = effects.find { it is OrganizerProfileEffect.NavigateToEvent } as OrganizerProfileEffect.NavigateToEvent
    assertEquals("event-001", effect1.eventId)
  }

  // ========================================
  // Tests for Website Navigation
  // ========================================

  @Test
  fun onWebsiteClicked_withUrl_emitsOpenWebsiteEffect() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-website",
        name = "Website Test",
        ownerId = "owner-1",
        website = "https://example.com"
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-website" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-website")
    testDispatcher.scheduler.advanceUntilIdle()

    val effects = mutableListOf<OrganizerProfileEffect>()
    val collectJob = launch {
      viewModel.effects.collect { effects.add(it) }
    }

    viewModel.onWebsiteClicked()
    testDispatcher.scheduler.advanceUntilIdle()

    collectJob.cancel()

    assertTrue("Should emit OpenWebsite effect", effects.any { it is OrganizerProfileEffect.OpenWebsite })
    val effect = effects.find { it is OrganizerProfileEffect.OpenWebsite } as OrganizerProfileEffect.OpenWebsite
    assertEquals("https://example.com", effect.url)
  }

  @Test
  fun onWebsiteClicked_withoutUrl_doesNotEmitEffect() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-no-website",
        name = "No Website Test",
        ownerId = "owner-1",
        website = null
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-no-website" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-no-website")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onWebsiteClicked()
    testDispatcher.scheduler.advanceUntilIdle()

    // Should not emit effect when website URL is null - check by trying to collect with timeout
    val effects = mutableListOf<OrganizerProfileEffect>()
    val job = launch {
      viewModel.effects.collect { effects.add(it) }
    }

    testDispatcher.scheduler.advanceUntilIdle()
    job.cancel()

    assertFalse("Should not emit effect when website URL is null", effects.any { it is OrganizerProfileEffect.OpenWebsite })
  }

  // ========================================
  // Tests for Social Media Navigation
  // ========================================

  @Test
  fun onSocialMediaClicked_instagram_withUrl_emitsEffect() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-instagram",
        name = "Instagram Test",
        ownerId = "owner-1",
        instagram = "https://instagram.com/testorg"
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-instagram" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-instagram")
    testDispatcher.scheduler.advanceUntilIdle()

    val effects = mutableListOf<OrganizerProfileEffect>()
    val collectJob = launch {
      viewModel.effects.collect { effects.add(it) }
    }

    viewModel.onSocialMediaClicked("instagram")
    testDispatcher.scheduler.advanceUntilIdle()

    collectJob.cancel()

    assertTrue("Should emit OpenSocialMedia effect", effects.any { it is OrganizerProfileEffect.OpenSocialMedia })
    val socialEffect = effects.find { it is OrganizerProfileEffect.OpenSocialMedia } as OrganizerProfileEffect.OpenSocialMedia
    assertEquals("instagram", socialEffect.platform)
    assertEquals("https://instagram.com/testorg", socialEffect.url)
  }

  @Test
  fun onSocialMediaClicked_tiktok_withUrl_emitsEffect() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-tiktok",
        name = "TikTok Test",
        ownerId = "owner-1",
        tiktok = "https://tiktok.com/@testorg"
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-tiktok" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-tiktok")
    testDispatcher.scheduler.advanceUntilIdle()

    val effects = mutableListOf<OrganizerProfileEffect>()
    val collectJob = launch {
      viewModel.effects.collect { effects.add(it) }
    }

    viewModel.onSocialMediaClicked("tiktok")
    testDispatcher.scheduler.advanceUntilIdle()

    collectJob.cancel()

    assertTrue("Should emit OpenSocialMedia effect", effects.any { it is OrganizerProfileEffect.OpenSocialMedia })
    val socialEffect = effects.find { it is OrganizerProfileEffect.OpenSocialMedia } as OrganizerProfileEffect.OpenSocialMedia
    assertEquals("tiktok", socialEffect.platform)
    assertEquals("https://tiktok.com/@testorg", socialEffect.url)
  }

  @Test
  fun onSocialMediaClicked_facebook_withUrl_emitsEffect() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-facebook",
        name = "Facebook Test",
        ownerId = "owner-1",
        facebook = "https://facebook.com/testorg"
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-facebook" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-facebook")
    testDispatcher.scheduler.advanceUntilIdle()

    val effects = mutableListOf<OrganizerProfileEffect>()
    val collectJob = launch {
      viewModel.effects.collect { effects.add(it) }
    }

    viewModel.onSocialMediaClicked("facebook")
    testDispatcher.scheduler.advanceUntilIdle()

    collectJob.cancel()

    assertTrue("Should emit OpenSocialMedia effect", effects.any { it is OrganizerProfileEffect.OpenSocialMedia })
    val socialEffect = effects.find { it is OrganizerProfileEffect.OpenSocialMedia } as OrganizerProfileEffect.OpenSocialMedia
    assertEquals("facebook", socialEffect.platform)
    assertEquals("https://facebook.com/testorg", socialEffect.url)
  }

  @Test
  fun onSocialMediaClicked_instagram_withoutUrl_doesNotEmitEffect() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-no-instagram",
        name = "No Instagram Test",
        ownerId = "owner-1",
        instagram = null
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-no-instagram" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-no-instagram")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onSocialMediaClicked("instagram")
    testDispatcher.scheduler.advanceUntilIdle()

    val effects = mutableListOf<OrganizerProfileEffect>()
    val job = launch {
      viewModel.effects.collect { effects.add(it) }
    }
    testDispatcher.scheduler.advanceUntilIdle()
    job.cancel()

    assertFalse("Should not emit effect when Instagram URL is null", effects.any { it is OrganizerProfileEffect.OpenSocialMedia })
  }

  @Test
  fun onSocialMediaClicked_caseInsensitive_emitsCorrectEffect() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-case-test",
        name = "Case Test",
        ownerId = "owner-1",
        instagram = "https://instagram.com/testorg"
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-case-test" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-case-test")
    testDispatcher.scheduler.advanceUntilIdle()

    // Collect effect in a coroutine before triggering the action
    val effects = mutableListOf<OrganizerProfileEffect>()
    val collectJob = launch {
      viewModel.effects.collect { effects.add(it) }
    }

    // Test with uppercase
    viewModel.onSocialMediaClicked("INSTAGRAM")
    testDispatcher.scheduler.advanceUntilIdle()
    
    collectJob.cancel()

    assertTrue("Should emit OpenSocialMedia effect", effects.any { it is OrganizerProfileEffect.OpenSocialMedia })
    val socialEffect = effects.find { it is OrganizerProfileEffect.OpenSocialMedia } as OrganizerProfileEffect.OpenSocialMedia
    assertEquals("INSTAGRAM", socialEffect.platform)
    assertEquals("https://instagram.com/testorg", socialEffect.url)
  }

  @Test
  fun onSocialMediaClicked_unknownPlatform_doesNotEmitEffect() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-unknown",
        name = "Unknown Platform Test",
        ownerId = "owner-1"
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-unknown" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-unknown")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onSocialMediaClicked("twitter")
    testDispatcher.scheduler.advanceUntilIdle()

    val effects = mutableListOf<OrganizerProfileEffect>()
    val job = launch {
      viewModel.effects.collect { effects.add(it) }
    }
    testDispatcher.scheduler.advanceUntilIdle()
    job.cancel()

    assertFalse("Should not emit effect for unknown platform", effects.any { it is OrganizerProfileEffect.OpenSocialMedia })
  }

  // ========================================
  // Tests for Follower Count Formatting
  // ========================================

  @Test
  fun followersCountFormatted_lessThan1000_showsExactNumber() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-format-1",
        name = "Format Test 1",
        ownerId = "owner-1",
        followerCount = 999
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-format-1" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-format-1")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals("999", state.followersCountFormatted)
  }

  @Test
  fun followersCountFormatted_exactly1000_showsWithK() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-format-2",
        name = "Format Test 2",
        ownerId = "owner-1",
        followerCount = 1000
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-format-2" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-format-2")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals("1K", state.followersCountFormatted)
  }

  @Test
  fun followersCountFormatted_2500_showsWithDecimal() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-format-3",
        name = "Format Test 3",
        ownerId = "owner-1",
        followerCount = 2500
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-format-3" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-format-3")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals("2.5K", state.followersCountFormatted)
  }

  @Test
  fun followersCountFormatted_999999_showsWithK() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-format-4",
        name = "Format Test 4",
        ownerId = "owner-1",
        followerCount = 999999
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-format-4" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-format-4")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals("999.9K", state.followersCountFormatted)
  }

  @Test
  fun followersCountFormatted_1Million_showsWithM() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-format-5",
        name = "Format Test 5",
        ownerId = "owner-1",
        followerCount = 1000000
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-format-5" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-format-5")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals("1M", state.followersCountFormatted)
  }

  @Test
  fun followersCountFormatted_2500000_showsWithMAndDecimal() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-format-6",
        name = "Format Test 6",
        ownerId = "owner-1",
        followerCount = 2500000
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-format-6" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-format-6")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals("2.5M", state.followersCountFormatted)
  }

  @Test
  fun followersCountFormatted_zero_showsZero() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-format-7",
        name = "Format Test 7",
        ownerId = "owner-1",
        followerCount = 0
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-format-7" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-format-7")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals("0", state.followersCountFormatted)
  }

  @Test
  fun followersCountFormatted_5432_showsWithDecimal() = runTest {
    val testOrg = OrganizationTestData.createTestOrganization(
        id = "org-format-8",
        name = "Format Test 8",
        ownerId = "owner-1",
        followerCount = 5432
    )

    val orgRepository = MockOrganizationRepository(mapOf("org-format-8" to testOrg))
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    viewModel.loadOrganizationProfile("org-format-8")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals("5.4K", state.followersCountFormatted)
  }

  // ========================================
  // Tests for Error Handling
  // ========================================

  @Test
  fun loadOrganizationProfile_clearsErrorMessageOnNewLoad() = runTest {
    val orgRepository = MockOrganizationRepository()
    val eventRepository = MockEventRepository()
    val viewModel = OrganizerProfileViewModel(orgRepository, eventRepository)

    // First, try to load a non-existent organization
    viewModel.loadOrganizationProfile("non-existent")
    testDispatcher.scheduler.advanceUntilIdle()

    val stateWithError = viewModel.state.value
    assertNotNull(stateWithError.errorMessage)

    // Now load a valid organization
    val validOrg = OrganizationTestData.createTestOrganization(
        id = "org-valid-after-error",
        name = "Valid After Error",
        ownerId = "owner-1"
    )
    val orgRepositoryWithValid = MockOrganizationRepository(mapOf("org-valid-after-error" to validOrg))
    val viewModel2 = OrganizerProfileViewModel(orgRepositoryWithValid, eventRepository)

    viewModel2.loadOrganizationProfile("org-valid-after-error")

    // Check that error is cleared immediately when loading starts
    val loadingState = viewModel2.state.value
    assertTrue(loadingState.loading)
    assertNull(loadingState.errorMessage)

    testDispatcher.scheduler.advanceUntilIdle()

    val finalState = viewModel2.state.value
    assertFalse(finalState.loading)
    assertNull(finalState.errorMessage)
  }
}
