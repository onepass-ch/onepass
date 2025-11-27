package ch.onepass.onepass.model.user

import ch.onepass.onepass.model.staff.StaffSearchResult

/**
 * Repository interface for managing user-related data and operations.
 *
 * Usage Guide:
 * - Use [getCurrentUser] when you need to retrieve the currently logged-in user's profile.
 * - Use [getOrCreateUser] during the authentication flow to ensure a user record exists.
 * - Use [updateLastLogin] to refresh the last login timestamp for a specific user.
 * - Use [getUserById] to fetch detailed information about a specific user using their unique
 *   identifier (UID).
 * - Use [searchUsers] to find users based on specific criteria like display name or email.
 */
interface UserRepository {

  /**
   * Retrieves the currently authenticated user.
   *
   * @return The [User] object if a user is currently logged in, or null if not.
   */
  suspend fun getCurrentUser(): User?

  /**
   * Retrieves the current user or creates a new one if they do not exist.
   *
   * This is typically used during the sign-in process to ensure the user is registered in the
   * backend.
   *
   * @return The existing or newly created [User] object, or null if the operation fails.
   */
  suspend fun getOrCreateUser(): User?

  /**
   * Updates the last login timestamp for the specified user.
   *
   * @param uid The unique identifier of the user.
   */
  suspend fun updateLastLogin(uid: String)

  /**
   * Retrieves a user by their unique ID.
   *
   * @param uid The unique user ID.
   * @return A [Result] containing the [StaffSearchResult] if found, or null if not found.
   */
  suspend fun getUserById(uid: String): Result<StaffSearchResult?>

  /**
   * Searches for users by the specified search type.
   *
   * @param query The search query (e.g., partial name or email).
   * @param searchType The criteria to search by (e.g., display name or email).
   * @param organizationId An optional organization ID to filter the results.
   * @return A [Result] containing a list of [StaffSearchResult] on success, or an error.
   */
  suspend fun searchUsers(
      query: String,
      searchType: UserSearchType,
      organizationId: String? = null
  ): Result<List<StaffSearchResult>>
}
