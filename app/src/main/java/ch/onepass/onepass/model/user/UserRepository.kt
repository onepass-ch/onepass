package ch.onepass.onepass.model.user

import ch.onepass.onepass.model.staff.StaffSearchResult

interface UserRepository {
  suspend fun getCurrentUser(): User?

  suspend fun getOrCreateUser(): User?

  suspend fun updateLastLogin(uid: String)

  /**
   * Searches for users by the specified search type.
   *
   * @param query The search query.
   * @param searchType The type of search to perform (display name or email).
   * @param organizationId The organization ID to filter results (optional).
   * @return A [Result] containing a list of [StaffSearchResult] on success, or an error.
   */
  suspend fun searchUsers(
      query: String,
      searchType: UserSearchType,
      organizationId: String? = null
  ): Result<List<StaffSearchResult>>
}
