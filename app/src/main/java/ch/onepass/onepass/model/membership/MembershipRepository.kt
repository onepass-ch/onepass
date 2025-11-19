package ch.onepass.onepass.model.membership

import ch.onepass.onepass.model.organization.OrganizationRole
import kotlinx.coroutines.flow.Flow

/** Repository interface defining operations for managing user-organization memberships. */
interface MembershipRepository {
  /**
   * Adds a new membership relationship between a user and an organization.
   *
   * @param userId The user's unique identifier.
   * @param orgId The organization's unique identifier.
   * @param role The role to assign to the user in the organization.
   * @return A [Result] containing the newly created membership ID on success, or an error.
   */
  suspend fun addMembership(userId: String, orgId: String, role: OrganizationRole): Result<String>

  /**
   * Removes a membership relationship between a user and an organization.
   *
   * @param userId The user's unique identifier.
   * @param orgId The organization's unique identifier.
   * @return A [Result] indicating success or failure.
   */
  suspend fun removeMembership(userId: String, orgId: String): Result<Unit>

  /**
   * Updates an existing membership, typically to change the user's role.
   *
   * @param userId The user's unique identifier.
   * @param orgId The organization's unique identifier.
   * @param newRole The new role to assign to the user.
   * @return A [Result] indicating success or failure.
   */
  suspend fun updateMembership(
      userId: String,
      orgId: String,
      newRole: OrganizationRole
  ): Result<Unit>

  /**
   * Retrieves all users who are members of a specific organization.
   *
   * @param orgId The organization's unique identifier.
   * @return A [Flow] emitting a list of memberships for that organization.
   */
  fun getUsersByOrganization(orgId: String): Flow<List<Membership>>

  /**
   * Retrieves all organizations that a specific user belongs to.
   *
   * @param userId The user's unique identifier.
   * @return A [Flow] emitting a list of memberships for that user.
   */
  fun getOrganizationsByUser(userId: String): Flow<List<Membership>>

  /**
   * Checks if a membership exists with the given criteria.
   *
   * @param userId The user's unique identifier.
   * @param orgId The organization's unique identifier.
   * @param roles Optional list of roles to filter by. If provided, checks if membership exists with
   *   any of these roles. If empty or null, checks for any membership regardless of role.
   * @return A [Flow] emitting true if a matching membership exists, false otherwise.
   */
  fun hasMembership(
      userId: String,
      orgId: String,
      roles: List<OrganizationRole>? = null
  ): Flow<Boolean>
}
