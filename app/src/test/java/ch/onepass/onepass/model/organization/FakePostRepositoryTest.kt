package ch.onepass.onepass.model.organization

import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [FakePostRepository]. Ensures the fake implementation behaves correctly for
 * testing purposes.
 */
class FakePostRepositoryTest {

  private lateinit var repository: FakePostRepository

  @Before
  fun setUp() {
    repository = FakePostRepository()
  }

  // ========================================
  // Tests for createPost
  // ========================================

  @Test
  fun createPost_addsPostToRepository() = runTest {
    val post = createTestPost("post1", "org1")

    val result = repository.createPost(post)

    assertTrue(result.isSuccess)
    assertNotNull(repository.getPost("post1"))
  }

  @Test
  fun createPost_incrementsCallCount() = runTest {
    assertEquals(0, repository.createPostCallCount)

    repository.createPost(createTestPost("post1", "org1"))
    assertEquals(1, repository.createPostCallCount)

    repository.createPost(createTestPost("post2", "org1"))
    assertEquals(2, repository.createPostCallCount)
  }

  @Test
  fun createPost_tracksLastCreatedPost() = runTest {
    val post = createTestPost("post1", "org1", "Test content")

    repository.createPost(post)

    assertEquals(post, repository.lastCreatedPost)
  }

  @Test
  fun createPost_returnsFailure_whenConfiguredToFail() = runTest {
    val failingRepository =
        FakePostRepository(shouldFail = true, failureMessage = "Creation failed")
    val post = createTestPost("post1", "org1")

    val result = failingRepository.createPost(post)

    assertTrue(result.isFailure)
    assertEquals("Creation failed", result.exceptionOrNull()?.message)
  }

  @Test
  fun createPost_generatesId_whenPostIdIsBlank() = runTest {
    val post = createTestPost("", "org1")

    val result = repository.createPost(post)

    assertTrue(result.isSuccess)
    assertTrue(result.getOrNull()!!.startsWith("generated_"))
  }

  // ========================================
  // Tests for deletePost
  // ========================================

  @Test
  fun deletePost_removesPostFromRepository() = runTest {
    val post = createTestPost("post1", "org1")
    repository.addPost(post)
    assertNotNull(repository.getPost("post1"))

    val result = repository.deletePost("post1")

    assertTrue(result.isSuccess)
    assertNull(repository.getPost("post1"))
  }

  @Test
  fun deletePost_incrementsCallCount() = runTest {
    repository.addPost(createTestPost("post1", "org1"))
    assertEquals(0, repository.deletePostCallCount)

    repository.deletePost("post1")

    assertEquals(1, repository.deletePostCallCount)
  }

  @Test
  fun deletePost_tracksLastDeletedPostId() = runTest {
    repository.addPost(createTestPost("post1", "org1"))

    repository.deletePost("post1")

    assertEquals("post1", repository.lastDeletedPostId)
  }

  @Test
  fun deletePost_returnsFailure_whenConfiguredToFail() = runTest {
    val failingRepository = FakePostRepository(shouldFail = true, failureMessage = "Delete failed")
    failingRepository.addPost(createTestPost("post1", "org1"))

    val result = failingRepository.deletePost("post1")

    assertTrue(result.isFailure)
    assertEquals("Delete failed", result.exceptionOrNull()?.message)
  }

  // ========================================
  // Tests for getPostsByOrganization
  // ========================================

  @Test
  fun getPostsByOrganization_returnsPostsForOrganization() = runTest {
    repository.addPosts(
        listOf(
            createTestPost("post1", "org1"),
            createTestPost("post2", "org1"),
            createTestPost("post3", "org2")))

    val posts = repository.getPostsByOrganization("org1").first()

    assertEquals(2, posts.size)
    assertTrue(posts.all { it.organizationId == "org1" })
  }

  @Test
  fun getPostsByOrganization_returnsEmptyList_whenNoPostsExist() = runTest {
    val posts = repository.getPostsByOrganization("nonexistent").first()

    assertTrue(posts.isEmpty())
  }

  @Test
  fun getPostsByOrganization_emitsUpdates_whenPostsChange() = runTest {
    // Initial empty state
    var posts = repository.getPostsByOrganization("org1").first()
    assertTrue(posts.isEmpty())

    // Add a post
    repository.addPost(createTestPost("post1", "org1"))
    posts = repository.getPostsByOrganization("org1").first()
    assertEquals(1, posts.size)

    // Add another post
    repository.addPost(createTestPost("post2", "org1"))
    posts = repository.getPostsByOrganization("org1").first()
    assertEquals(2, posts.size)
  }

  // ========================================
  // Tests for likePost
  // ========================================

  @Test
  fun likePost_addsUserToLikedByList() = runTest {
    val post = createTestPost("post1", "org1")
    repository.addPost(post)

    repository.likePost("post1", "user1")

    val updatedPost = repository.getPost("post1")
    assertNotNull(updatedPost)
    assertTrue(updatedPost!!.likedBy.contains("user1"))
    assertEquals(1, updatedPost.likesCount)
  }

  @Test
  fun likePost_doesNotAddDuplicate_whenAlreadyLiked() = runTest {
    val post = createTestPost("post1", "org1", likedBy = listOf("user1"))
    repository.addPost(post)

    repository.likePost("post1", "user1")

    val updatedPost = repository.getPost("post1")
    assertEquals(1, updatedPost!!.likedBy.count { it == "user1" })
  }

  @Test
  fun likePost_incrementsCallCount() = runTest {
    repository.addPost(createTestPost("post1", "org1"))
    assertEquals(0, repository.likePostCallCount)

    repository.likePost("post1", "user1")

    assertEquals(1, repository.likePostCallCount)
  }

  @Test
  fun likePost_tracksLastLikedInfo() = runTest {
    repository.addPost(createTestPost("post1", "org1"))

    repository.likePost("post1", "user123")

    assertEquals("post1", repository.lastLikedPostId)
    assertEquals("user123", repository.lastLikedByUserId)
  }

  @Test
  fun likePost_returnsFailure_whenConfiguredToFail() = runTest {
    val failingRepository = FakePostRepository(shouldFail = true)
    failingRepository.addPost(createTestPost("post1", "org1"))

    val result = failingRepository.likePost("post1", "user1")

    assertTrue(result.isFailure)
  }

  // ========================================
  // Tests for unlikePost
  // ========================================

  @Test
  fun unlikePost_removesUserFromLikedByList() = runTest {
    val post = createTestPost("post1", "org1", likedBy = listOf("user1", "user2"))
    repository.addPost(post)

    repository.unlikePost("post1", "user1")

    val updatedPost = repository.getPost("post1")
    assertNotNull(updatedPost)
    assertFalse(updatedPost!!.likedBy.contains("user1"))
    assertTrue(updatedPost.likedBy.contains("user2"))
    assertEquals(1, updatedPost.likesCount)
  }

  @Test
  fun unlikePost_doesNothing_whenUserNotInList() = runTest {
    val post = createTestPost("post1", "org1", likedBy = listOf("user1"))
    repository.addPost(post)

    repository.unlikePost("post1", "user2")

    val updatedPost = repository.getPost("post1")
    assertEquals(1, updatedPost!!.likesCount)
  }

  @Test
  fun unlikePost_incrementsCallCount() = runTest {
    repository.addPost(createTestPost("post1", "org1", likedBy = listOf("user1")))
    assertEquals(0, repository.unlikePostCallCount)

    repository.unlikePost("post1", "user1")

    assertEquals(1, repository.unlikePostCallCount)
  }

  @Test
  fun unlikePost_tracksLastUnlikedInfo() = runTest {
    repository.addPost(createTestPost("post1", "org1", likedBy = listOf("user123")))

    repository.unlikePost("post1", "user123")

    assertEquals("post1", repository.lastUnlikedPostId)
    assertEquals("user123", repository.lastUnlikedByUserId)
  }

  // ========================================
  // Tests for Helper Methods
  // ========================================

  @Test
  fun addPosts_addMultiplePostsAtOnce() {
    val posts =
        listOf(
            createTestPost("post1", "org1"),
            createTestPost("post2", "org2"),
            createTestPost("post3", "org1"))

    repository.addPosts(posts)

    assertEquals(3, repository.getAllPosts().size)
  }

  @Test
  fun clearPosts_removesAllPosts() {
    repository.addPosts(listOf(createTestPost("post1", "org1"), createTestPost("post2", "org2")))
    assertEquals(2, repository.getAllPosts().size)

    repository.clearPosts()

    assertTrue(repository.getAllPosts().isEmpty())
  }

  @Test
  fun resetCallCounts_resetsAllTracking() = runTest {
    repository.addPost(createTestPost("post1", "org1"))
    repository.createPost(createTestPost("post2", "org1"))
    repository.deletePost("post1")
    repository.likePost("post2", "user1")
    repository.unlikePost("post2", "user1")

    repository.resetCallCounts()

    assertEquals(0, repository.createPostCallCount)
    assertEquals(0, repository.deletePostCallCount)
    assertEquals(0, repository.likePostCallCount)
    assertEquals(0, repository.unlikePostCallCount)
    assertNull(repository.lastCreatedPost)
    assertNull(repository.lastDeletedPostId)
    assertNull(repository.lastLikedPostId)
    assertNull(repository.lastUnlikedPostId)
  }

  // ========================================
  // Helper Functions
  // ========================================

  private fun createTestPost(
      id: String,
      organizationId: String,
      content: String = "Test content",
      likedBy: List<String> = emptyList()
  ): Post =
      Post(
          id = id,
          organizationId = organizationId,
          authorId = "author1",
          authorName = "Test Author",
          content = content,
          likedBy = likedBy,
          createdAt = Timestamp.now())
}
