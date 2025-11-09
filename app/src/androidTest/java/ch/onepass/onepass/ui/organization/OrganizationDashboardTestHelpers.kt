package ch.onepass.onepass.ui.organization

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.organization.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import io.mockk.every
import io.mockk.mockk
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Shared test data and utilities for Organization Dashboard tests. Reduces boilerplate across
 * ViewModel, Screen, and Firestore tests.
 */
object OrganizationDashboardTestData {

  fun createTestOrganization(
      id: String = "test-org-1",
      name: String = "Test Organization",
      ownerId: String = "owner-1",
      members: Map<String, OrganizationMember> =
          mapOf(
              "owner-1" to
                  OrganizationMember(role = OrganizationRole.OWNER, assignedEvents = emptyList()),
              "member-1" to
                  OrganizationMember(role = OrganizationRole.MEMBER, assignedEvents = emptyList()),
              "staff-1" to
                  OrganizationMember(role = OrganizationRole.STAFF, assignedEvents = emptyList())),
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
          members = members,
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

  override fun getOrganizationsByMember(userId: String): Flow<List<Organization>> =
      flowOf(emptyList())

  override fun getOrganizationsByStatus(status: OrganizationStatus): Flow<List<Organization>> =
      flowOf(emptyList())

  override fun searchOrganizations(query: String): Flow<List<Organization>> = flowOf(emptyList())

  override fun getVerifiedOrganizations(): Flow<List<Organization>> = flowOf(emptyList())

  override suspend fun addMember(
      organizationId: String,
      userId: String,
      role: OrganizationRole
  ): Result<Unit> = Result.success(Unit)

  override suspend fun removeMember(organizationId: String, userId: String): Result<Unit> =
      removeResult

  override suspend fun updateMemberRole(
      organizationId: String,
      userId: String,
      newRole: OrganizationRole
  ): Result<Unit> = Result.success(Unit)

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
}
