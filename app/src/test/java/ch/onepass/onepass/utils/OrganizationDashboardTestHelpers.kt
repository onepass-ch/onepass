package ch.onepass.onepass.utils

import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.ui.organization.OrganizerProfileEffect
import ch.onepass.onepass.ui.organization.OrganizerProfileUiState
import ch.onepass.onepass.ui.organization.OrganizerProfileViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import io.mockk.every
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared test data and utilities for Organization Dashboard tests. Reduces boilerplate across
 * ViewModel, Screen, and Firestore tests.
 */
object OrganizationDashboardTestData {

  fun createTestOrganization(
      id: String = "test-org-1",
      name: String = "Test Organization",
      ownerId: String = "owner-1",
      followerCount: Int = 1500,
      averageRating: Float = 4.5f,
      eventIds: List<String> = listOf("event-1", "event-2")
  ): Organization =
      Organization(
          id = id,
          name = name,
          description = "Test Description",
          ownerId = ownerId,
          status = OrganizationStatus.ACTIVE,
          verified = true,
          followerCount = followerCount,
          averageRating = averageRating,
          eventIds = eventIds)

  fun createTestEvent(
      eventId: String = "event-1",
      title: String = "Test Event 1",
      organizerId: String = "test-org-1",
      status: EventStatus = EventStatus.PUBLISHED,
      capacity: Int = 100,
      ticketsRemaining: Int = 50
  ): Event =
      Event(
          eventId = eventId,
          title = title,
          description = "Description",
          organizerId = organizerId,
          organizerName = "Test Organization",
          status = status,
          location = Location(GeoPoint(46.5197, 6.6323), "Lausanne"),
          startTime = Timestamp(Date()),
          capacity = capacity,
          ticketsRemaining = ticketsRemaining)

  fun createMockAuth(userId: String = "owner-1"): Pair<FirebaseAuth, FirebaseUser> {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns userId
    return Pair(mockAuth, mockUser)
  }
}

/** Mock Organization Repository with configurable behavior. */
open class MockOrganizationRepository(
    private val organization: Organization? = null,
    private val shouldThrowError: Boolean = false,
    private val removeResult: Result<Unit> = Result.success(Unit)
) : OrganizationRepository {

  override suspend fun createOrganization(organization: Organization): Result<String> =
      Result.success("test-id")

  override suspend fun updateOrganization(organization: Organization): Result<Unit> =
      Result.success(Unit)

  override suspend fun deleteOrganization(organizationId: String): Result<Unit> =
      Result.success(Unit)

  override fun getOrganizationById(organizationId: String): Flow<Organization?> {
    if (shouldThrowError) throw Exception("Test error")
    return flowOf(organization)
  }

  override fun getOrganizationsByOwner(ownerId: String): Flow<List<Organization>> =
      flowOf(emptyList())

  override fun getOrganizationsByStatus(status: OrganizationStatus): Flow<List<Organization>> =
      flowOf(emptyList())

  override fun searchOrganizations(query: String): Flow<List<Organization>> = flowOf(emptyList())

  override fun getVerifiedOrganizations(): Flow<List<Organization>> = flowOf(emptyList())

  override suspend fun createInvitation(invitation: OrganizationInvitation): Result<String> =
      Result.success("test-id")

  override fun getPendingInvitations(organizationId: String): Flow<List<OrganizationInvitation>> =
      flowOf(emptyList())

  override fun getInvitationsByEmail(email: String): Flow<List<OrganizationInvitation>> =
      flowOf(emptyList())

  override suspend fun updateInvitationStatus(
      invitationId: String,
      newStatus: InvitationStatus
  ): Result<Unit> = Result.success(Unit)

  override suspend fun deleteInvitation(invitationId: String): Result<Unit> = Result.success(Unit)

  override suspend fun updateProfileImage(organizationId: String, imageUrl: String?): Result<Unit> =
      Result.success(Unit)

  override suspend fun updateCoverImage(organizationId: String, imageUrl: String?): Result<Unit> =
      Result.success(Unit)
}

/** Mock Event Repository with configurable behavior. */
class MockEventRepository(private val events: List<Event> = emptyList()) : EventRepository {

  override fun getAllEvents(): Flow<List<Event>> = flowOf(events)

  override fun searchEvents(query: String): Flow<List<Event>> = flowOf(emptyList())

  override fun getEventById(eventId: String): Flow<Event?> =
      flowOf(events.find { it.eventId == eventId })

  override fun getEventsByOrganization(orgId: String): Flow<List<Event>> = flowOf(events)

  override fun getEventsByLocation(center: Location, radiusKm: Double): Flow<List<Event>> =
      flowOf(emptyList())

  override fun getEventsByTag(tag: String): Flow<List<Event>> = flowOf(emptyList())

  override fun getFeaturedEvents(): Flow<List<Event>> = flowOf(emptyList())

  override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> = flowOf(emptyList())

  override suspend fun createEvent(event: Event): Result<String> = Result.success("test-id")

  override suspend fun updateEvent(event: Event): Result<Unit> = Result.success(Unit)

  override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)

  override suspend fun addEventImage(eventId: String, imageUrl: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun removeEventImage(eventId: String, imageUrl: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun updateEventImages(eventId: String, imageUrls: List<String>): Result<Unit> =
      Result.success(Unit)
}

/**
 * Organization repository that records requested IDs for assertions in UI tests while reusing the
 * default mock behaviour.
 */
class RecordingOrganizationRepository(
    organization: Organization? = null,
    shouldThrowError: Boolean = false,
    removeResult: Result<Unit> = Result.success(Unit)
) : MockOrganizationRepository(organization, shouldThrowError, removeResult) {

  private val _requestedOrganizationIds = mutableListOf<String>()
  val requestedOrganizationIds: List<String>
    get() = _requestedOrganizationIds

  override fun getOrganizationById(organizationId: String): Flow<Organization?> {
    _requestedOrganizationIds += organizationId
    return super.getOrganizationById(organizationId)
  }
}

/**
 * Event repository that records organization IDs requested through [getEventsByOrganization] for
 * assertions in UI tests.
 */
class RecordingEventRepository(private val events: List<Event> = emptyList()) : EventRepository {

  private val _requestedOrganizationIds = mutableListOf<String>()
  val requestedOrganizationIds: List<String>
    get() = _requestedOrganizationIds

  override fun getAllEvents(): Flow<List<Event>> = flowOf(events)

  override fun searchEvents(query: String): Flow<List<Event>> = flowOf(emptyList())

  override fun getEventById(eventId: String): Flow<Event?> =
      flowOf(events.find { it.eventId == eventId })

  override fun getEventsByOrganization(orgId: String): Flow<List<Event>> {
    _requestedOrganizationIds += orgId
    return flowOf(events)
  }

  override fun getEventsByLocation(center: Location, radiusKm: Double): Flow<List<Event>> =
      flowOf(emptyList())

  override fun getEventsByTag(tag: String): Flow<List<Event>> = flowOf(emptyList())

  override fun getFeaturedEvents(): Flow<List<Event>> = flowOf(emptyList())

  override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> = flowOf(emptyList())

  override suspend fun createEvent(event: Event): Result<String> = Result.success("test-id")

  override suspend fun updateEvent(event: Event): Result<Unit> = Result.success(Unit)

  override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)

  override suspend fun addEventImage(eventId: String, imageUrl: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun removeEventImage(eventId: String, imageUrl: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun updateEventImages(eventId: String, imageUrls: List<String>): Result<Unit> =
      Result.success(Unit)
}

/**
 * Test-friendly [OrganizerProfileViewModel] that exposes state and effect hooks while keeping the
 * original production behaviour.
 */
class MockOrganizerProfileViewModel(
    organization: Organization? = null,
    events: List<Event> = emptyList(),
    initialState: OrganizerProfileUiState = OrganizerProfileUiState(),
    private val mockOrganizationRepository: RecordingOrganizationRepository =
        RecordingOrganizationRepository(organization),
    private val mockEventRepository: RecordingEventRepository = RecordingEventRepository(events),
    extraEffectBuffer: Int = 4
) : OrganizerProfileViewModel(mockOrganizationRepository, mockEventRepository) {

  private val mutableStateFlow = MutableStateFlow(initialState)
  private val mutableEffectsFlow =
      MutableSharedFlow<OrganizerProfileEffect>(replay = 0, extraBufferCapacity = extraEffectBuffer)

  override val state: StateFlow<OrganizerProfileUiState> = mutableStateFlow.asStateFlow()

  private val recordedEffectsInternal = mutableListOf<OrganizerProfileEffect>()
  val recordedEffects: List<OrganizerProfileEffect>
    get() = recordedEffectsInternal

  val requestedOrganizationIds: List<String>
    get() = mockOrganizationRepository.requestedOrganizationIds

  val requestedEventOrganizationIds: List<String>
    get() = mockEventRepository.requestedOrganizationIds

  init {
    replaceStateFlow()
    replaceEffectsFlow()

    viewModelScope.launch { mutableEffectsFlow.collect { recordedEffectsInternal.add(it) } }
  }

  private fun replaceStateFlow() {
    val stateField = OrganizerProfileViewModel::class.java.getDeclaredField("_state")
    stateField.isAccessible = true
    stateField.set(this, mutableStateFlow)
  }

  private fun replaceEffectsFlow() {
    val effectsField = OrganizerProfileViewModel::class.java.getDeclaredField("_effects")
    effectsField.isAccessible = true
    effectsField.set(this, mutableEffectsFlow)

    val exposedEffectsField = OrganizerProfileViewModel::class.java.getDeclaredField("effects")
    exposedEffectsField.isAccessible = true
    exposedEffectsField.set(this, mutableEffectsFlow.asSharedFlow())
  }

  fun emitState(state: OrganizerProfileUiState) {
    mutableStateFlow.value = state
  }

  fun updateState(transform: (OrganizerProfileUiState) -> OrganizerProfileUiState) {
    mutableStateFlow.update(transform)
  }

  fun clearRecordedEffects() {
    recordedEffectsInternal.clear()
  }
}
