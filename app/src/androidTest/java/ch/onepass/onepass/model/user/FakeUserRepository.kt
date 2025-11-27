package ch.onepass.onepass.model.user

import ch.onepass.onepass.model.staff.StaffSearchResult

/**
 * Fake implementation of UserRepository for testing purposes.
 *
 * This class provides a simple implementation that can be used in tests where you don't need the
 * full Firebase functionality.
 */
class FakeUserRepository(
    // These parameters are mutable (var) to support setter methods (updateCurrentUser,
    // updateCreatedUser, setThrowOnLoad) that allow tests to modify the repository state
    // dynamically during test execution.
    private var currentUser: User? = null,
    private var createdUser: User? = null,
    private var throwOnLoad: Boolean = false
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

  // Simulate a database table for users
  private val usersMap = mutableMapOf<String, StaffSearchResult>()

  /** Add a user to the fake database. */
  fun addTestUser(user: StaffSearchResult) {
    usersMap[user.id] = user
  }

  /** Configurable get user by id results for testing. Default implementation looks up in usersMap. */
  private var getUserByIdFunction: (String) -> Result<StaffSearchResult?> = { uid ->
    Result.success(usersMap[uid])
  }

  override suspend fun getUserById(uid: String): Result<StaffSearchResult?> {
    return getUserByIdFunction(uid)
  }

  /** Override getUserById to return specific result regardless of input ID. */
  fun setGetUserByIdResult(result: StaffSearchResult?) {
    getUserByIdFunction = { Result.success(result) }
  }

  /** Override getUserById to return an error. */
  fun setGetUserByIdError(error: Throwable) {
    getUserByIdFunction = { Result.failure(error) }
  }

  /** Configurable search results for testing. Set this to customize search behavior in tests. */
  private var searchResultsFunction:
      (String, UserSearchType, String?) -> Result<List<StaffSearchResult>> =
      { _, _, _ ->
        Result.success(emptyList())
      }

  override suspend fun searchUsers(
      query: String,
      searchType: UserSearchType,
      organizationId: String?
  ): Result<List<StaffSearchResult>> {
    return searchResultsFunction(query, searchType, organizationId)
  }

  /** Override searchUsers to return specific results. */
  fun setSearchResults(results: List<StaffSearchResult>) {
    searchResultsFunction = { _, _, _ -> Result.success(results) }
  }

  /** Override searchUsers to return an error. */
  fun setSearchError(error: Throwable) {
    searchResultsFunction = { _, _, _ -> Result.failure(error) }
  }

  /** Set a custom search function for more complex test scenarios. */
  fun setSearchFunction(
      function: (String, UserSearchType, String?) -> Result<List<StaffSearchResult>>
  ) {
    searchResultsFunction = function
  }

  /** Update the current user for testing retry scenarios. */
  fun updateCurrentUser(user: User?) {
    currentUser = user
  }

  /** Update the created user for testing retry scenarios. */
  fun updateCreatedUser(user: User?) {
    createdUser = user
  }

  /** Set whether to throw an exception on load for testing error scenarios. */
  fun setThrowOnLoad(value: Boolean) {
    throwOnLoad = value
  }

  /**
   * Resets all state to initial values.
   *
   * Useful in test teardown or between test cases to ensure a clean state.
   */
  fun reset() {
    currentUser = null
    createdUser = null
    throwOnLoad = false
    usersMap.clear()
    searchResultsFunction = { _, _, _ -> Result.success(emptyList()) }
    getUserByIdFunction = { uid -> Result.success(usersMap[uid]) }
  }
}
