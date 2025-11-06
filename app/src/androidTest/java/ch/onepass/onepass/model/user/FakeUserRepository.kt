package ch.onepass.onepass.model.user

import ch.onepass.onepass.model.staff.StaffSearchResult

/**
 * Fake implementation of UserRepository for testing purposes.
 *
 * This class provides a simple implementation that can be used in tests
 * where you don't need the full Firebase functionality.
 */
class FakeUserRepository(
    private val currentUser: User? = null,
    private val createdUser: User? = null,
    private val throwOnLoad: Boolean = false
) : UserRepository {

  override suspend fun getCurrentUser(): User? {
    if (throwOnLoad) throw RuntimeException("boom")
    return currentUser
  }

  override suspend fun getOrCreateUser(): User? {
    if (throwOnLoad) throw RuntimeException("boom")
    return createdUser
  }

  override suspend fun updateLastLogin(uid: String) {
    /* no-op */
  }

  override suspend fun searchUsersByDisplayName(
      query: String,
      organizationId: String?
  ): Result<List<StaffSearchResult>> {
    return Result.success(emptyList())
  }

  override suspend fun searchUsersByEmail(
      query: String,
      organizationId: String?
  ): Result<List<StaffSearchResult>> {
    return Result.success(emptyList())
  }
}

