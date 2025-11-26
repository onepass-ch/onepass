package ch.onepass.onepass.model.organization

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of OrganizationRepository for testing purposes.
 *
 * This class provides a simple in-memory implementation that can be used in tests where you don't
 * need the full Firebase functionality.
 */
class FakeOrganizationRepository(
    private val shouldThrowOnCreate: Boolean = false,
    private val shouldThrowOnGetInvitations: Boolean = false
) : OrganizationRepository {

  private val organizations = mutableMapOf<String, Organization>()
  private val invitations = mutableMapOf<String, OrganizationInvitation>()

  override suspend fun createOrganization(organization: Organization): Result<String> {
    if (shouldThrowOnCreate) {
      return Result.failure(RuntimeException("Test error"))
    }
    val id = organization.id.ifEmpty { "org_${organizations.size + 1}" }
    organizations[id] = organization.copy(id = id)
    return Result.success(id)
  }

  override suspend fun updateOrganization(organization: Organization): Result<Unit> {
    organizations[organization.id] = organization
    return Result.success(Unit)
  }

  override suspend fun deleteOrganization(organizationId: String): Result<Unit> {
    organizations.remove(organizationId)
    return Result.success(Unit)
  }

  override fun getOrganizationById(organizationId: String): Flow<Organization?> {
    return flowOf(organizations[organizationId])
  }

  override fun getOrganizationsByOwner(ownerId: String): Flow<List<Organization>> {
    return flowOf(organizations.values.filter { it.ownerId == ownerId })
  }

  override fun getOrganizationsByMember(userId: String): Flow<List<Organization>> {
    return flowOf(
        organizations.values.filter { organization -> organization.members.containsKey(userId) })
  }

  override fun getOrganizationsByStatus(status: OrganizationStatus): Flow<List<Organization>> {
    return flowOf(organizations.values.filter { it.status == status })
  }

  override fun searchOrganizations(query: String): Flow<List<Organization>> {
    val trimmedQuery = query.trim().lowercase()
    if (trimmedQuery.isEmpty()) {
      return flowOf(emptyList())
    }
    return flowOf(organizations.values.filter { it.name.lowercase().startsWith(trimmedQuery) })
  }

  override fun getVerifiedOrganizations(): Flow<List<Organization>> {
    return flowOf(organizations.values.filter { it.verified })
  }

  override suspend fun addMember(
      organizationId: String,
      userId: String,
      role: OrganizationRole
  ): Result<Unit> {
    val org =
        organizations[organizationId]
            ?: return Result.failure(IllegalStateException("Organization not found"))
    val updatedMembers = org.members.toMutableMap()
    updatedMembers[userId] = OrganizationMember(role = role)
    organizations[organizationId] = org.copy(members = updatedMembers)
    return Result.success(Unit)
  }

  override suspend fun removeMember(organizationId: String, userId: String): Result<Unit> {
    val org =
        organizations[organizationId]
            ?: return Result.failure(IllegalStateException("Organization not found"))
    val updatedMembers = org.members.toMutableMap()
    updatedMembers.remove(userId)
    organizations[organizationId] = org.copy(members = updatedMembers)
    return Result.success(Unit)
  }

  override suspend fun updateMemberRole(
      organizationId: String,
      userId: String,
      newRole: OrganizationRole
  ): Result<Unit> {
    val org =
        organizations[organizationId]
            ?: return Result.failure(IllegalStateException("Organization not found"))
    val member =
        org.members[userId] ?: return Result.failure(IllegalStateException("Member not found"))
    val updatedMembers = org.members.toMutableMap()
    updatedMembers[userId] = member.copy(role = newRole)
    organizations[organizationId] = org.copy(members = updatedMembers)
    return Result.success(Unit)
  }

  override suspend fun createInvitation(invitation: OrganizationInvitation): Result<String> {
    if (shouldThrowOnCreate) {
      return Result.failure(RuntimeException("Test error"))
    }
    val id = invitation.id.ifEmpty { "inv_${invitations.size + 1}" }
    invitations[id] = invitation.copy(id = id)
    return Result.success(id)
  }

  override fun getPendingInvitations(organizationId: String): Flow<List<OrganizationInvitation>> {
    return flowOf(
        invitations.values.filter {
          it.orgId == organizationId && it.status == InvitationStatus.PENDING
        })
  }

  override fun getInvitationsByEmail(email: String): Flow<List<OrganizationInvitation>> {
    if (shouldThrowOnGetInvitations) {
      return flow { throw RuntimeException("Test error getting invitations") }
    }
    return flowOf(invitations.values.filter { it.inviteeEmail == email })
  }

  override suspend fun updateInvitationStatus(
      invitationId: String,
      newStatus: InvitationStatus
  ): Result<Unit> {
    val invitation =
        invitations[invitationId]
            ?: return Result.failure(IllegalStateException("Invitation not found"))
    invitations[invitationId] = invitation.copy(status = newStatus)
    return Result.success(Unit)
  }

  override suspend fun deleteInvitation(invitationId: String): Result<Unit> {
    invitations.remove(invitationId)
    return Result.success(Unit)
  }

  override suspend fun updateProfileImage(organizationId: String, imageUrl: String?): Result<Unit> {
    val org =
        organizations[organizationId]
            ?: return Result.failure(IllegalStateException("Organization not found"))
    organizations[organizationId] = org.copy(profileImageUrl = imageUrl)
    return Result.success(Unit)
  }

  override suspend fun updateCoverImage(organizationId: String, imageUrl: String?): Result<Unit> {
    val org =
        organizations[organizationId]
            ?: return Result.failure(IllegalStateException("Organization not found"))
    organizations[organizationId] = org.copy(coverImageUrl = imageUrl)
    return Result.success(Unit)
  }

  /** Helper method to add test invitations directly (for test setup). */
  fun addTestInvitation(invitation: OrganizationInvitation) {
    val id = invitation.id.ifEmpty { "inv_${invitations.size + 1}" }
    invitations[id] = invitation.copy(id = id)
  }

  /** Helper method to clear all test data. */
  fun clear() {
    organizations.clear()
    invitations.clear()
  }
}
