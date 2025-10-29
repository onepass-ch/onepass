package ch.onepass.onepass.model.organization

import kotlinx.coroutines.flow.Flow

/** Repository interface defining operations for managing organizations. */
interface OrganizationRepository {
  /**
   * Creates a new organization in the repository.
   *
   * @param organization The [Organization] to create.
   * @return A [Result] containing the newly created organization's ID on success, or an error.
   */
  suspend fun createOrganization(organization: Organization): Result<String>

  /**
   * Updates an existing organization.
   *
   * @param organization The updated [Organization] (must include valid [id]).
   * @return A [Result] indicating success or failure.
   */
  suspend fun updateOrganization(organization: Organization): Result<Unit>

  /**
   * Deletes an organization by its ID.
   *
   * @param organizationId The unique identifier of the organization to delete.
   * @return A [Result] indicating success or failure.
   */
  suspend fun deleteOrganization(organizationId: String): Result<Unit>

  /**
   * Retrieves a specific organization by its unique ID.
   *
   * @param organizationId The unique identifier of the organization.
   * @return A [Flow] emitting the organization or null if not found.
   */
  fun getOrganizationById(organizationId: String): Flow<Organization?>

  /**
   * Retrieves all organizations owned by a specific user.
   *
   * @param ownerId The owner's unique ID.
   * @return A [Flow] emitting a list of organizations owned by that user.
   */
  fun getOrganizationsByOwner(ownerId: String): Flow<List<Organization>>

  /**
   * Retrieves all organizations where a user is a member.
   *
   * @param userId The user's unique ID.
   * @return A [Flow] emitting a list of organizations the user belongs to.
   */
  fun getOrganizationsByMember(userId: String): Flow<List<Organization>>

  /**
   * Retrieves all organizations with a specific status.
   *
   * @param status The [OrganizationStatus] to filter by.
   * @return A [Flow] emitting a list of organizations with the given status.
   */
  fun getOrganizationsByStatus(status: OrganizationStatus): Flow<List<Organization>>

  /**
   * Searches for organizations whose names match the given query (case-insensitive prefix match).
   * Returns an empty list if the query is empty or contains only whitespace.
   *
   * @param query The search term. Empty or whitespace-only queries return empty results.
   * @return A [Flow] emitting a list of matching organizations.
   */
  fun searchOrganizations(query: String): Flow<List<Organization>>

  /**
   * Retrieves verified organizations.
   *
   * @return A [Flow] emitting a list of verified organizations.
   */
  fun getVerifiedOrganizations(): Flow<List<Organization>>

  /**
   * Adds a member to an organization.
   *
   * @param organizationId The organization's ID.
   * @param userId The user's ID to add as a member.
   * @param role The role to assign to the member.
   * @return A [Result] indicating success or failure.
   */
  suspend fun addMember(
      organizationId: String,
      userId: String,
      role: OrganizationRole
  ): Result<Unit>

  /**
   * Removes a member from an organization.
   *
   * @param organizationId The organization's ID.
   * @param userId The user's ID to remove.
   * @return A [Result] indicating success or failure.
   */
  suspend fun removeMember(organizationId: String, userId: String): Result<Unit>

  /**
   * Updates a member's role within an organization.
   *
   * @param organizationId The organization's ID.
   * @param userId The user's ID.
   * @param newRole The new role to assign.
   * @return A [Result] indicating success or failure.
   */
  suspend fun updateMemberRole(
      organizationId: String,
      userId: String,
      newRole: OrganizationRole
  ): Result<Unit>

  /**
   * Creates an invitation to join an organization.
   *
   * @param invitation The [OrganizationInvitation] to create.
   * @return A [Result] containing the invitation ID on success, or an error.
   */
  suspend fun createInvitation(invitation: OrganizationInvitation): Result<String>

  /**
   * Retrieves pending invitations for a specific organization.
   *
   * @param organizationId The organization's ID.
   * @return A [Flow] emitting a list of pending invitations.
   */
  fun getPendingInvitations(organizationId: String): Flow<List<OrganizationInvitation>>

  /**
   * Retrieves invitations for a specific email address. Returns invitations across all statuses
   * (pending, accepted, rejected, expired, revoked).
   *
   * @param email The email address to search for.
   * @return A [Flow] emitting a list of invitations for that email across all statuses.
   */
  fun getInvitationsByEmail(email: String): Flow<List<OrganizationInvitation>>

  /**
   * Updates an invitation's status.
   *
   * @param invitationId The invitation's ID.
   * @param newStatus The new status to set.
   * @return A [Result] indicating success or failure.
   */
  suspend fun updateInvitationStatus(
      invitationId: String,
      newStatus: InvitationStatus
  ): Result<Unit>

  /**
   * Deletes an invitation.
   *
   * @param invitationId The invitation's ID.
   * @return A [Result] indicating success or failure.
   */
  suspend fun deleteInvitation(invitationId: String): Result<Unit>
}
