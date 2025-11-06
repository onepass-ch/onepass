package ch.onepass.onepass.model.user

import ch.onepass.onepass.model.staff.StaffSearchResult

interface UserRepository {
  suspend fun getCurrentUser(): User?

  suspend fun getOrCreateUser(): User?

  suspend fun updateLastLogin(uid: String)

  /**
   * Searches for users by display name.
   *
   * @param query The search query (display name).
   * @param organizationId The organization ID to filter results (optional).
   * @return A [Result] containing a list of [StaffSearchResult] on success, or an error.
   */
  suspend fun searchUsersByDisplayName(
      query: String,
      organizationId: String? = null
  ): Result<List<StaffSearchResult>>

  /**
   * Searches for users by email address.
   *
   * @param query The search query (email address).
   * @param organizationId The organization ID to filter results (optional).
   * @return A [Result] containing a list of [StaffSearchResult] on success, or an error.
   */
  suspend fun searchUsersByEmail(
      query: String,
      organizationId: String? = null
  ): Result<List<StaffSearchResult>>
}
