package ch.onepass.onepass.model.organization

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PostRepositoryFirebase] with mocked Firestore.
 *
 * These tests verify the Firebase implementation of PostRepository with mocked dependencies.
 */
class PostRepositoryFirebaseTest {

  private lateinit var repository: PostRepositoryFirebase
  private val mockFirestore: FirebaseFirestore = mockk(relaxed = true)
  private val mockCollection: CollectionReference = mockk(relaxed = true)
  private val mockDocument: DocumentReference = mockk(relaxed = true)

  @Before
  fun setUp() {
    mockkObject(Firebase)
    mockkStatic("com.google.firebase.firestore.ktx.FirestoreKt")
    every { Firebase.firestore } returns mockFirestore
    every { mockFirestore.collection("posts") } returns mockCollection
    every { mockCollection.document() } returns mockDocument
    every { mockCollection.document(any()) } returns mockDocument
    every { mockDocument.id } returns "generated-post-id"

    repository = PostRepositoryFirebase()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun createTestPost(
      id: String = "",
      organizationId: String = "org-123",
      authorId: String = "author-123",
      authorName: String = "Test Author",
      content: String = "Test post content",
      likedBy: List<String> = emptyList(),
      createdAt: Timestamp? = null
  ): Post =
      Post(
          id = id,
          organizationId = organizationId,
          authorId = authorId,
          authorName = authorName,
          content = content,
          likedBy = likedBy,
          createdAt = createdAt)

  // ========================================
  // Tests for createPost
  // ========================================

  @Test
  fun createPost_success_returnsPostId() = runTest {
    val post = createTestPost(content = "Valid post content")
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    val result = repository.createPost(post)

    assertTrue(result.isSuccess)
    assertEquals("generated-post-id", result.getOrNull())
    verify { mockDocument.set(any()) }
  }

  @Test
  fun createPost_sanitizesContent() = runTest {
    val post = createTestPost(content = "  Content with   extra   spaces  ")
    val capturedPost = slot<Post>()
    every { mockDocument.set(capture(capturedPost)) } returns Tasks.forResult(null)

    repository.createPost(post)

    assertEquals("Content with extra spaces", capturedPost.captured.content)
  }

  @Test
  fun createPost_setsCorrectPostId() = runTest {
    val post = createTestPost()
    val capturedPost = slot<Post>()
    every { mockDocument.set(capture(capturedPost)) } returns Tasks.forResult(null)

    repository.createPost(post)

    assertEquals("generated-post-id", capturedPost.captured.id)
  }

  @Test
  fun createPost_clearsTimestamps() = runTest {
    val post = createTestPost(createdAt = Timestamp.now())
    val capturedPost = slot<Post>()
    every { mockDocument.set(capture(capturedPost)) } returns Tasks.forResult(null)

    repository.createPost(post)

    assertEquals(null, capturedPost.captured.createdAt)
    assertEquals(null, capturedPost.captured.updatedAt)
  }

  @Test
  fun createPost_failsWithEmptyContent() = runTest {
    val post = createTestPost(content = "")

    val result = repository.createPost(post)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    assertEquals("Post content cannot be empty", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPost_failsWithBlankContent() = runTest {
    val post = createTestPost(content = "   ")

    val result = repository.createPost(post)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun createPost_failsWithOnlyWhitespace() = runTest {
    val post = createTestPost(content = "\t\n  ")

    val result = repository.createPost(post)

    assertTrue(result.isFailure)
  }

  @Test
  fun createPost_handlesFirestoreError() = runTest {
    val post = createTestPost(content = "Valid content")
    every { mockDocument.set(any()) } returns Tasks.forException(Exception("Firestore error"))

    val result = repository.createPost(post)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("Firestore error") == true)
  }

  @Test
  fun createPost_truncatesLongContent() = runTest {
    val longContent = "a".repeat(200)
    val post = createTestPost(content = longContent)
    val capturedPost = slot<Post>()
    every { mockDocument.set(capture(capturedPost)) } returns Tasks.forResult(null)

    repository.createPost(post)

    assertEquals(POST_MAX_CHARACTERS, capturedPost.captured.content.length)
  }

  // ========================================
  // Tests for deletePost
  // ========================================

  @Test
  fun deletePost_success() = runTest {
    every { mockDocument.delete() } returns Tasks.forResult(null)

    val result = repository.deletePost("post-123")

    assertTrue(result.isSuccess)
    verify { mockCollection.document("post-123") }
    verify { mockDocument.delete() }
  }

  @Test
  fun deletePost_handlesFirestoreError() = runTest {
    every { mockDocument.delete() } returns Tasks.forException(Exception("Delete failed"))

    val result = repository.deletePost("post-123")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("Delete failed") == true)
  }

  @Test
  fun deletePost_callsCorrectDocument() = runTest {
    every { mockDocument.delete() } returns Tasks.forResult(null)

    repository.deletePost("specific-post-id")

    verify { mockCollection.document("specific-post-id") }
  }

  // ========================================
  // Tests for likePost
  // ========================================

  @Test
  fun likePost_success() = runTest {
    every { mockDocument.update(any<Map<String, Any>>()) } returns Tasks.forResult(null)

    val result = repository.likePost("post-123", "user-456")

    assertTrue(result.isSuccess)
    verify { mockCollection.document("post-123") }
    verify { mockDocument.update(any<Map<String, Any>>()) }
  }

  @Test
  fun likePost_handlesFirestoreError() = runTest {
    every { mockDocument.update(any<Map<String, Any>>()) } returns
        Tasks.forException(Exception("Like failed"))

    val result = repository.likePost("post-123", "user-456")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("Like failed") == true)
  }

  @Test
  fun likePost_callsCorrectDocument() = runTest {
    every { mockDocument.update(any<Map<String, Any>>()) } returns Tasks.forResult(null)

    repository.likePost("specific-post-id", "user-123")

    verify { mockCollection.document("specific-post-id") }
  }

  // ========================================
  // Tests for unlikePost
  // ========================================

  @Test
  fun unlikePost_success() = runTest {
    every { mockDocument.update(any<Map<String, Any>>()) } returns Tasks.forResult(null)

    val result = repository.unlikePost("post-123", "user-456")

    assertTrue(result.isSuccess)
    verify { mockCollection.document("post-123") }
    verify { mockDocument.update(any<Map<String, Any>>()) }
  }

  @Test
  fun unlikePost_handlesFirestoreError() = runTest {
    every { mockDocument.update(any<Map<String, Any>>()) } returns
        Tasks.forException(Exception("Unlike failed"))

    val result = repository.unlikePost("post-123", "user-456")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("Unlike failed") == true)
  }

  @Test
  fun unlikePost_callsCorrectDocument() = runTest {
    every { mockDocument.update(any<Map<String, Any>>()) } returns Tasks.forResult(null)

    repository.unlikePost("specific-post-id", "user-123")

    verify { mockCollection.document("specific-post-id") }
  }

  // ========================================
  // Tests for Edge Cases
  // ========================================

  @Test
  fun createPost_handlesSpecialCharacters() = runTest {
    val specialContent = "Hello! @user #hashtag 🎉"
    val post = createTestPost(content = specialContent)
    val capturedPost = slot<Post>()
    every { mockDocument.set(capture(capturedPost)) } returns Tasks.forResult(null)

    val result = repository.createPost(post)

    assertTrue(result.isSuccess)
    assertEquals(specialContent, capturedPost.captured.content)
  }

  @Test
  fun createPost_handlesUnicodeContent() = runTest {
    val unicodeContent = "你好世界 مرحبا"
    val post = createTestPost(content = unicodeContent)
    val capturedPost = slot<Post>()
    every { mockDocument.set(capture(capturedPost)) } returns Tasks.forResult(null)

    val result = repository.createPost(post)

    assertTrue(result.isSuccess)
    assertEquals(unicodeContent, capturedPost.captured.content)
  }

  @Test
  fun createPost_handlesEmoji() = runTest {
    val emojiContent = "🎉🎊🎁 Party!"
    val post = createTestPost(content = emojiContent)
    val capturedPost = slot<Post>()
    every { mockDocument.set(capture(capturedPost)) } returns Tasks.forResult(null)

    val result = repository.createPost(post)

    assertTrue(result.isSuccess)
    assertEquals(emojiContent, capturedPost.captured.content)
  }

  @Test
  fun likePost_withEmptyPostId() = runTest {
    every { mockDocument.update(any<Map<String, Any>>()) } returns Tasks.forResult(null)

    val result = repository.likePost("", "user-123")

    assertTrue(result.isSuccess)
  }

  @Test
  fun unlikePost_withEmptyUserId() = runTest {
    every { mockDocument.update(any<Map<String, Any>>()) } returns Tasks.forResult(null)

    val result = repository.unlikePost("post-123", "")

    assertTrue(result.isSuccess)
  }
}
