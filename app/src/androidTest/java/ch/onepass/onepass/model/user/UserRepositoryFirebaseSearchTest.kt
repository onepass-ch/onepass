package ch.onepass.onepass.model.user

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for UserRepositoryFirebase search functions.
 *
 * These tests verify:
 * - searchUsers functionality with DISPLAY_NAME search type
 * - searchUsers functionality with EMAIL search type
 * - Query validation
 * - Payload construction
 * - Response parsing
 * - Error handling
 * - Edge cases
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

  // ========== searchUsers with DISPLAY_NAME Tests ==========

  @Test
  fun searchUsers_withDisplayName_withValidQuery_returnsSuccess() = runTest {
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

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

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
  fun searchUsers_withDisplayName_withOrganizationId_includesInPayload() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())
    val payloadSlot = slot<Map<String, Any>>()

    every { result.data } returns responseData
    every { callable.call(capture(payloadSlot)) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, "org-123")

    assertTrue("Search should succeed", result.isSuccess)

    // Verify function was called and payload contains organizationId
    verify { callable.call(any()) }
    val payload = payloadSlot.captured
    assertEquals("org-123", payload["organizationId"])
    assertEquals("NAME", payload["searchType"])
  }

  @Test
  fun searchUsers_withDisplayName_withoutOrganizationId_omitsFromPayload() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Search should succeed", result.isSuccess)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsers_withDisplayName_trimsQueryString() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    repository.searchUsers("  John  ", UserSearchType.DISPLAY_NAME, null)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsers_withDisplayName_withEmptyQuery_returnsFailure() = runTest {
    val result = repository.searchUsers("", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Should fail with empty query", result.isFailure)
    assertTrue(
        "Error message should mention blank query",
        result.exceptionOrNull()?.message?.contains("blank", ignoreCase = true) == true)
  }

  @Test
  fun searchUsers_withDisplayName_withBlankQuery_returnsFailure() = runTest {
    val result = repository.searchUsers("   ", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Should fail with blank query", result.isFailure)
  }

  @Test
  fun searchUsers_withDisplayName_withEmptyResults_returnsEmptyList() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("Nonexistent", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsers_withDisplayName_withMultipleUsers_returnsAllUsers() = runTest {
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

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 2 users", 2, users!!.size)
    assertEquals("user1", users[0].id)
    assertEquals("user2", users[1].id)
    assertNull("Second user should have null avatarUrl", users[1].avatarUrl)
  }

  @Test
  fun searchUsers_withDisplayName_withMissingAvatarUrl_handlesNull() = runTest {
    val usersData =
        listOf(mapOf("id" to "user1", "email" to "john@example.com", "displayName" to "John Doe"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertNull("avatarUrl should be null", users[0].avatarUrl)
  }

  @Test
  fun searchUsers_withDisplayName_withMissingId_filtersOutUser() = runTest {
    val usersData =
        listOf(
            mapOf("email" to "john@example.com", "displayName" to "John Doe"),
            mapOf("id" to "user2", "email" to "jane@example.com", "displayName" to "Jane Smith"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user (missing id filtered out)", 1, users!!.size)
    assertEquals("user2", users[0].id)
  }

  @Test
  fun searchUsers_withDisplayName_withMissingEmail_usesEmptyString() = runTest {
    val usersData = listOf(mapOf("id" to "user1", "displayName" to "John Doe"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertEquals("", users[0].email)
  }

  @Test
  fun searchUsers_withDisplayName_withMissingDisplayName_usesEmptyString() = runTest {
    val usersData = listOf(mapOf("id" to "user1", "email" to "john@example.com"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertEquals("", users[0].displayName)
  }

  @Test
  fun searchUsers_withDisplayName_whenCloudFunctionThrows_returnsFailure() = runTest {
    every { callable.call(any()) } returns
        Tasks.forException(RuntimeException("Cloud Function error"))

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Should fail when Cloud Function throws", result.isFailure)
    assertNotNull("Exception should be present", result.exceptionOrNull())
  }

  @Test
  fun searchUsers_withDisplayName_withUnexpectedResponseFormat_returnsFailure() = runTest {
    every { result.data } returns "not a map"
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Should fail with unexpected response format", result.isFailure)
    assertTrue(
        "Error message should mention response format",
        result.exceptionOrNull()?.message?.contains("response format", ignoreCase = true) == true)
  }

  @Test
  fun searchUsers_withDisplayName_withNullResponseData_returnsFailure() = runTest {
    every { result.data } returns null
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Should fail with null response data", result.isFailure)
  }

  @Test
  fun searchUsers_withDisplayName_withMissingUsersKey_returnsEmptyList() = runTest {
    val responseData = mapOf<String, Any?>("otherKey" to "value")

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsers_withDisplayName_withUsersNotList_returnsEmptyList() = runTest {
    val responseData = mapOf("users" to "not a list")

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsers_withDisplayName_withInvalidUserData_filtersOutInvalidUsers() = runTest {
    val usersData =
        listOf(
            mapOf("id" to "user1", "email" to "john@example.com", "displayName" to "John Doe"),
            mapOf("invalid" to "data"), // Missing id
            mapOf("id" to "user3", "email" to "jane@example.com", "displayName" to "Jane Smith"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 2 valid users", 2, users!!.size)
    assertEquals("user1", users[0].id)
    assertEquals("user3", users[1].id)
  }

  // ========== searchUsers with EMAIL Tests ==========

  @Test
  fun searchUsers_withEmail_withValidQuery_returnsSuccess() = runTest {
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

    val result = repository.searchUsers("john@example.com", UserSearchType.EMAIL, null)

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
  fun searchUsers_withEmail_withOrganizationId_includesInPayload() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())
    val payloadSlot = slot<Map<String, Any>>()

    every { result.data } returns responseData
    every { callable.call(capture(payloadSlot)) } returns Tasks.forResult(result)

    val result = repository.searchUsers("john@example.com", UserSearchType.EMAIL, "org-123")

    assertTrue("Search should succeed", result.isSuccess)

    verify { callable.call(any()) }
    val payload = payloadSlot.captured
    assertEquals("org-123", payload["organizationId"])
    assertEquals("EMAIL", payload["searchType"])
  }

  @Test
  fun searchUsers_withEmail_withoutOrganizationId_omitsFromPayload() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("john@example.com", UserSearchType.EMAIL, null)

    assertTrue("Search should succeed", result.isSuccess)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsers_withEmail_trimsQueryString() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    repository.searchUsers("  john@example.com  ", UserSearchType.EMAIL, null)

    verify { callable.call(any()) }
  }

  @Test
  fun searchUsers_withEmail_withEmptyQuery_returnsFailure() = runTest {
    val result = repository.searchUsers("", UserSearchType.EMAIL, null)

    assertTrue("Should fail with empty query", result.isFailure)
    assertTrue(
        "Error message should mention blank query",
        result.exceptionOrNull()?.message?.contains("blank", ignoreCase = true) == true)
  }

  @Test
  fun searchUsers_withEmail_withBlankQuery_returnsFailure() = runTest {
    val result = repository.searchUsers("   ", UserSearchType.EMAIL, null)

    assertTrue("Should fail with blank query", result.isFailure)
  }

  @Test
  fun searchUsers_withEmail_withEmptyResults_returnsEmptyList() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("nonexistent@example.com", UserSearchType.EMAIL, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsers_withEmail_withMultipleUsers_returnsAllUsers() = runTest {
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

    val result = repository.searchUsers("example.com", UserSearchType.EMAIL, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 2 users", 2, users!!.size)
    assertEquals("user1", users[0].id)
    assertEquals("user2", users[1].id)
  }

  @Test
  fun searchUsers_withEmail_whenCloudFunctionThrows_returnsFailure() = runTest {
    every { callable.call(any()) } returns
        Tasks.forException(RuntimeException("Cloud Function error"))

    val result = repository.searchUsers("john@example.com", UserSearchType.EMAIL, null)

    assertTrue("Should fail when Cloud Function throws", result.isFailure)
    assertNotNull("Exception should be present", result.exceptionOrNull())
  }

  @Test
  fun searchUsers_withEmail_withUnexpectedResponseFormat_returnsFailure() = runTest {
    every { result.data } returns "not a map"
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("john@example.com", UserSearchType.EMAIL, null)

    assertTrue("Should fail with unexpected response format", result.isFailure)
    assertTrue(
        "Error message should mention response format",
        result.exceptionOrNull()?.message?.contains("response format", ignoreCase = true) == true)
  }

  @Test
  fun searchUsers_withEmail_withNullResponseData_returnsFailure() = runTest {
    every { result.data } returns null
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("john@example.com", UserSearchType.EMAIL, null)

    assertTrue("Should fail with null response data", result.isFailure)
  }

  @Test
  fun searchUsers_withEmail_withMissingUsersKey_returnsEmptyList() = runTest {
    val responseData = mapOf<String, Any?>("otherKey" to "value")

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("john@example.com", UserSearchType.EMAIL, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsers_withEmail_withUsersNotList_returnsEmptyList() = runTest {
    val responseData = mapOf("users" to "not a list")

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("john@example.com", UserSearchType.EMAIL, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertTrue("Users list should be empty", users!!.isEmpty())
  }

  @Test
  fun searchUsers_withEmail_withInvalidUserData_filtersOutInvalidUsers() = runTest {
    val usersData =
        listOf(
            mapOf("id" to "user1", "email" to "john@example.com", "displayName" to "John Doe"),
            mapOf("invalid" to "data"), // Missing id
            mapOf("id" to "user3", "email" to "jane@example.com", "displayName" to "Jane Smith"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("example.com", UserSearchType.EMAIL, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 2 valid users", 2, users!!.size)
    assertEquals("user1", users[0].id)
    assertEquals("user3", users[1].id)
  }

  @Test
  fun searchUsers_withEmail_usesCorrectSearchType() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())
    val payloadSlot = slot<Map<String, Any>>()

    every { result.data } returns responseData
    every { callable.call(capture(payloadSlot)) } returns Tasks.forResult(result)

    repository.searchUsers("john@example.com", UserSearchType.EMAIL, null)

    verify { callable.call(any()) }
    val payload = payloadSlot.captured
    assertEquals("EMAIL", payload["searchType"])
    assertEquals("john@example.com", payload["query"])
    assertFalse("organizationId should not be present", payload.containsKey("organizationId"))
  }

  @Test
  fun searchUsers_withDisplayName_usesCorrectSearchType() = runTest {
    val responseData = mapOf("users" to emptyList<Map<String, Any?>>())
    val payloadSlot = slot<Map<String, Any>>()

    every { result.data } returns responseData
    every { callable.call(capture(payloadSlot)) } returns Tasks.forResult(result)

    repository.searchUsers("John", UserSearchType.DISPLAY_NAME, null)

    verify { callable.call(any()) }
    val payload = payloadSlot.captured
    assertEquals("NAME", payload["searchType"])
    assertEquals("John", payload["query"])
    assertFalse("organizationId should not be present", payload.containsKey("organizationId"))
  }

  @Test
  fun searchUsers_withEmail_withNullEmail_usesEmptyString() = runTest {
    val usersData = listOf(mapOf("id" to "user1", "displayName" to "John Doe"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("john", UserSearchType.EMAIL, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertEquals("", users[0].email)
  }

  @Test
  fun searchUsers_withEmail_withNullDisplayName_usesEmptyString() = runTest {
    val usersData = listOf(mapOf("id" to "user1", "email" to "john@example.com"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("john@example.com", UserSearchType.EMAIL, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user", 1, users!!.size)
    assertEquals("", users[0].displayName)
  }

  @Test
  fun searchUsers_withEmail_withNullId_filtersOutUser() = runTest {
    val usersData =
        listOf(
            mapOf("email" to "john@example.com", "displayName" to "John Doe"),
            mapOf("id" to "user2", "email" to "jane@example.com", "displayName" to "Jane Smith"))

    val responseData = mapOf("users" to usersData)

    every { result.data } returns responseData
    every { callable.call(any()) } returns Tasks.forResult(result)

    val result = repository.searchUsers("example.com", UserSearchType.EMAIL, null)

    assertTrue("Search should succeed", result.isSuccess)
    val users = result.getOrNull()
    assertNotNull("Users list should not be null", users)
    assertEquals("Should return 1 user (missing id filtered out)", 1, users!!.size)
    assertEquals("user2", users[0].id)
  }
}
