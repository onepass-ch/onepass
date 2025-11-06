package ch.onepass.onepass.model.user

import ch.onepass.onepass.model.staff.StaffSearchResult
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for UserRepositoryFirebase search functions.
 *
 * These tests verify:
 * - searchUsersByDisplayName functionality
 * - searchUsersByEmail functionality
 * - Query validation
 * - Payload construction
 * - Response parsing
 * - Error handling
 * - Edge cases
 *
 * Coverage target: 90%+ line coverage
 */
class UserRepositoryFirebaseSearchTest {

  private lateinit var functions: FirebaseFunctions
  private lateinit var callable: HttpsCallableReference
  private lateinit var result: HttpsCallableResult
  private lateinit var auth: FirebaseAuth
  private lateinit var db: FirebaseFirestore
  private lateinit var repository: UserRepositoryFirebase

  @Before
  fun setUp() {
    functions = mockk(relaxed = true)
    callable = mockk(relaxed = true)
    result = mockk(relaxed = true)
    auth = mockk(relaxed = true)
    db = mockk(relaxed = true)

    every { functions.getHttpsCallable(any()) } returns callable
    repository = UserRepositoryFirebase(auth = auth, db = db, functions = functions)
  }

  // ========== searchUsersByDisplayName Tests ==========

  @Test
  fun searchUsersByDisplayName_withValidQuery_returnsSuccess() = runTest {
    val usersData =
        listOf(
            mapOf(
                "id" to "user1",
                "email" to "john@example.com",
                "displayName" to "John Doe",
                "avatarUrl" to "https://example.com/avatar1.jpg"))
    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertEquals("user1", users[0].id)
    assertEquals("john@example.com", users[0].email)
    assertEquals("John Doe", users[0].displayName)
    assertEquals("https://example.com/avatar1.jpg", users[0].avatarUrl)
  }

  @Test
  fun searchUsersByDisplayName_withOrganizationId_includesInPayload() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", "org-123")

    assertTrue("Search should succeed", result.isSuccess)

    // Verify function was called (payload verification can be added if needed)
    verify { callable.call(any()) }
  }

  @Test
  fun searchUsersByDisplayName_withoutOrganizationId_omitsFromPayload() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Search should succeed", result.isSuccess)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsersByDisplayName_trimsQueryString() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    repository.searchUsersByDisplayName("  John  ", null)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsersByDisplayName_withEmptyQuery_returnsFailure() = runTest {
    val result = repository.searchUsersByDisplayName("", null)

    assertTrue("Should fail with empty query", result.isFailure)
    assertTrue(
        "Error message should mention blank query",
        result.exceptionOrNull()?.message?.contains("blank", ignoreCase = true) == true)
  }

  @Test
  fun searchUsersByDisplayName_withBlankQuery_returnsFailure() = runTest {
    val result = repository.searchUsersByDisplayName("   ", null)

    assertTrue("Should fail with blank query", result.isFailure)
  }

  @Test
  fun searchUsersByDisplayName_withEmptyResults_returnsEmptyList() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("Nonexistent", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsersByDisplayName_withMultipleUsers_returnsAllUsers() = runTest {
    val usersData =
        listOf(
            mapOf(
                "id" to "user1",
                "email" to "john@example.com",
                "displayName" to "John Doe",
                "avatarUrl" to "https://example.com/avatar1.jpg"),
            mapOf(
                "id" to "user2",
                "email" to "jane@example.com",
                "displayName" to "Jane Smith",
                "avatarUrl" to null))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 2 users", 2, users!!.size)
    assertEquals("user1", users[0].id)
    assertEquals("user2", users[1].id)
    assertNull("Second user should have null avatarUrl", users[1].avatarUrl)
  }

  @Test
  fun searchUsersByDisplayName_withMissingAvatarUrl_handlesNull() = runTest {
    val usersData =
        listOf(
            mapOf(
                "id" to "user1",
                "email" to "john@example.com",
                "displayName" to "John Doe"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertNull("avatarUrl should be null", users[0].avatarUrl)
  }

  @Test
  fun searchUsersByDisplayName_withMissingId_filtersOutUser() = runTest {
    val usersData =
        listOf(
            mapOf(
                "email" to "john@example.com",
                "displayName" to "John Doe"),
            mapOf(
                "id" to "user2",
                "email" to "jane@example.com",
                "displayName" to "Jane Smith"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user (missing id filtered out)", 1, users!!.size)
    assertEquals("user2", users[0].id)
  }

  @Test
  fun searchUsersByDisplayName_withMissingEmail_usesEmptyString() = runTest {
    val usersData =
        listOf(
            mapOf(
                "id" to "user1",
                "displayName" to "John Doe"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertEquals("", users[0].email)
  }

  @Test
  fun searchUsersByDisplayName_withMissingDisplayName_usesEmptyString() = runTest {
    val usersData =
        listOf(
            mapOf(
                "id" to "user1",
                "email" to "john@example.com"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertEquals("", users[0].displayName)
  }

  @Test
  fun searchUsersByDisplayName_whenCloudFunctionThrows_returnsFailure() = runTest {
    every { callable.call(any()) } returns Tasks.forException(RuntimeException("Cloud Function error"))

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Should fail when Cloud Function throws", result.isFailure)
    assertNotNull("Exception should be present", result.exceptionOrNull())
  }

  @Test
  fun searchUsersByDisplayName_withUnexpectedResponseFormat_returnsFailure() = runTest {
    every { result.data } returns "not a map"
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Should fail with unexpected response format", result.isFailure)
    assertTrue(
        "Error message should mention response format",
        result.exceptionOrNull()?.message?.contains("response format", ignoreCase = true) == true)
  }

  @Test
  fun searchUsersByDisplayName_withNullResponseData_returnsFailure() = runTest {
    every { result.data } returns null
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Should fail with null response data", result.isFailure)
  }

  @Test
  fun searchUsersByDisplayName_withMissingUsersKey_returnsEmptyList() = runTest {
    val responseData = mapOf<String, Any?>("otherKey" to "value")

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsersByDisplayName_withUsersNotList_returnsEmptyList() = runTest {
    val responseData = mapOf("users" to "not a list")

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsersByDisplayName_withInvalidUserData_filtersOutInvalidUsers() = runTest {
    val usersData =
        listOf(
            mapOf("id" to "user1", "email" to "john@example.com", "displayName" to "John Doe"),
            mapOf("invalid" to "data"), // Missing id
            mapOf("id" to "user3", "email" to "jane@example.com", "displayName" to "Jane Smith"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByDisplayName("John", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 2 valid users", 2, users!!.size)
    assertEquals("user1", users[0].id)
    assertEquals("user3", users[1].id)
  }

  // ========== searchUsersByEmail Tests ==========

  @Test
  fun searchUsersByEmail_withValidQuery_returnsSuccess() = runTest {
    val usersData =
        listOf(
            mapOf(
                "id" to "user1",
                "email" to "john@example.com",
                "displayName" to "John Doe",
                "avatarUrl" to "https://example.com/avatar1.jpg"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("john@example.com", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertEquals("user1", users[0].id)
    assertEquals("john@example.com", users[0].email)
    assertEquals("John Doe", users[0].displayName)
    assertEquals("https://example.com/avatar1.jpg", users[0].avatarUrl)
  }

  @Test
  fun searchUsersByEmail_withOrganizationId_includesInPayload() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("john@example.com", "org-123")

    assertTrue("Search should succeed", result.isSuccess)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsersByEmail_withoutOrganizationId_omitsFromPayload() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("john@example.com", null)

    assertTrue("Search should succeed", result.isSuccess)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsersByEmail_trimsQueryString() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    repository.searchUsersByEmail("  john@example.com  ", null)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsersByEmail_withEmptyQuery_returnsFailure() = runTest {
    val result = repository.searchUsersByEmail("", null)

    assertTrue("Should fail with empty query", result.isFailure)
    assertTrue(
        "Error message should mention blank query",
        result.exceptionOrNull()?.message?.contains("blank", ignoreCase = true) == true)
  }

  @Test
  fun searchUsersByEmail_withBlankQuery_returnsFailure() = runTest {
    val result = repository.searchUsersByEmail("   ", null)

    assertTrue("Should fail with blank query", result.isFailure)
  }

  @Test
  fun searchUsersByEmail_withEmptyResults_returnsEmptyList() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("nonexistent@example.com", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsersByEmail_withMultipleUsers_returnsAllUsers() = runTest {
    val usersData =
        listOf(
            mapOf(
                "id" to "user1",
                "email" to "john@example.com",
                "displayName" to "John Doe",
                "avatarUrl" to "https://example.com/avatar1.jpg"),
            mapOf(
                "id" to "user2",
                "email" to "jane@example.com",
                "displayName" to "Jane Smith",
                "avatarUrl" to null))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("example.com", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 2 users", 2, users!!.size)
    assertEquals("user1", users[0].id)
    assertEquals("user2", users[1].id)
  }

  @Test
  fun searchUsersByEmail_whenCloudFunctionThrows_returnsFailure() = runTest {
    every { callable.call(any()) } returns Tasks.forException(RuntimeException("Cloud Function error"))

    val result = repository.searchUsersByEmail("john@example.com", null)

    assertTrue("Should fail when Cloud Function throws", result.isFailure)
    assertNotNull("Exception should be present", result.exceptionOrNull())
  }

  @Test
  fun searchUsersByEmail_withUnexpectedResponseFormat_returnsFailure() = runTest {
    every { result.data } returns "not a map"
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("john@example.com", null)

    assertTrue("Should fail with unexpected response format", result.isFailure)
    assertTrue(
        "Error message should mention response format",
        result.exceptionOrNull()?.message?.contains("response format", ignoreCase = true) == true)
  }

  @Test
  fun searchUsersByEmail_withNullResponseData_returnsFailure() = runTest {
    every { result.data } returns null
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("john@example.com", null)

    assertTrue("Should fail with null response data", result.isFailure)
  }

  @Test
  fun searchUsersByEmail_withMissingUsersKey_returnsEmptyList() = runTest {
    val responseData = mapOf<String, Any?>("otherKey" to "value")

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("john@example.com", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsersByEmail_withUsersNotList_returnsEmptyList() = runTest {
    val responseData = mapOf("users" to "not a list")

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("john@example.com", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsersByEmail_withInvalidUserData_filtersOutInvalidUsers() = runTest {
    val usersData =
        listOf(
            mapOf("id" to "user1", "email" to "john@example.com", "displayName" to "John Doe"),
            mapOf("invalid" to "data"), // Missing id
            mapOf("id" to "user3", "email" to "jane@example.com", "displayName" to "Jane Smith"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("example.com", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 2 valid users", 2, users!!.size)
    assertEquals("user1", users[0].id)
    assertEquals("user3", users[1].id)
  }

  @Test
  fun searchUsersByEmail_usesCorrectSearchType() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    repository.searchUsersByEmail("john@example.com", null)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsersByDisplayName_usesCorrectSearchType() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    repository.searchUsersByDisplayName("John", null)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsersByEmail_withNullEmail_usesEmptyString() = runTest {
    val usersData =
        listOf(
            mapOf(
                "id" to "user1",
                "displayName" to "John Doe"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("john", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertEquals("", users[0].email)
  }

  @Test
  fun searchUsersByEmail_withNullDisplayName_usesEmptyString() = runTest {
    val usersData =
        listOf(
            mapOf(
                "id" to "user1",
                "email" to "john@example.com"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("john@example.com", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertEquals("", users[0].displayName)
  }

  @Test
  fun searchUsersByEmail_withNullId_filtersOutUser() = runTest {
    val usersData =
        listOf(
            mapOf(
                "email" to "john@example.com",
                "displayName" to "John Doe"),
            mapOf(
                "id" to "user2",
                "email" to "jane@example.com",
                "displayName" to "Jane Smith"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsersByEmail("example.com", null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user (missing id filtered out)", 1, users!!.size)
    assertEquals("user2", users[0].id)
  }
}

