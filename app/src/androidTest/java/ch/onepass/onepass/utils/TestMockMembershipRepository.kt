package ch.onepass.onepass.utils

import ch.onepass.onepass.model.membership.Membership
import ch.onepass.onepass.model.membership.MembershipRepository
import ch.onepass.onepass.model.organization.OrganizationRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Mock implementation of MembershipRepository for testing.
 *
 * This class provides a mock implementation that can be used in tests where you need to test
 * membership-related functionality with configurable behavior.
 */
class TestMockMembershipRepository(
    private val addMembershipResult: Result<String> = Result.success("membership-1"),
    private val organizationsByUser: Map<String, List<Membership>> = emptyMap(),
    private val usersByOrganization: Map<String, List<Membership>> = emptyMap(),
    private val getOrganizationsByUserError: Throwable? = null
) : MembershipRepository {
  val addMembershipCalls = mutableListOf<Triple<String, String, OrganizationRole>>()

  override suspend fun addMembership(
      userId: String,
      orgId: String,
      role: OrganizationRole
  ): Result<String> {
    addMembershipCalls.add(Triple(userId, orgId, role))
    return addMembershipResult
  }

  override suspend fun removeMembership(userId: String, orgId: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun updateMembership(
      userId: String,
      orgId: String,
      newRole: OrganizationRole
  ): Result<Unit> = Result.success(Unit)

  override suspend fun getUsersByOrganization(orgId: String): Result<List<Membership>> =
      Result.success(usersByOrganization[orgId] ?: emptyList())

  override fun getUsersByOrganizationFlow(orgId: String): Flow<List<Membership>> = flow {
    emit(emptyList())
  }

  override suspend fun getOrganizationsByUser(userId: String): Result<List<Membership>> {
    if (getOrganizationsByUserError != null) {
      return Result.failure(getOrganizationsByUserError)
    }
    return Result.success(organizationsByUser[userId] ?: emptyList())
  }

  override fun getOrganizationsByUserFlow(userId: String): Flow<List<Membership>> = flow {
    emit(organizationsByUser[userId] ?: emptyList())
  }

  override suspend fun hasMembership(
      userId: String,
      orgId: String,
      roles: List<OrganizationRole>
  ): Boolean {
    val memberships = organizationsByUser[userId] ?: emptyList()
    return memberships.any { it.orgId == orgId && it.role in roles }
  }
}
