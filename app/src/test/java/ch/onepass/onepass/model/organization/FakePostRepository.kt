package ch.onepass.onepass.model.organization

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [PostRepository] for testing purposes.
 *
 * Provides an in-memory implementation with configurable behavior for:
 * - Success/failure simulation
 * - Tracking method calls for verification
 * - Pre-populating with test data
 */
class FakePostRepository(
    private val shouldFail: Boolean = false,
    private val failureMessage: String = "Test error"
) : PostRepository {

    /** Internal storage for posts, keyed by post ID */
    private val postsStore = mutableMapOf<String, Post>()

    /** MutableStateFlow to emit post list updates */
    private val postsFlow = MutableStateFlow<List<Post>>(emptyList())

    /** Tracks the number of times createPost was called */
    var createPostCallCount = 0
        private set

    /** Tracks the number of times deletePost was called */
    var deletePostCallCount = 0
        private set

    /** Tracks the number of times likePost was called */
    var likePostCallCount = 0
        private set

    /** Tracks the number of times unlikePost was called */
    var unlikePostCallCount = 0
        private set

    /** Last post ID that was liked */
    var lastLikedPostId: String? = null
        private set

    /** Last user ID that liked a post */
    var lastLikedByUserId: String? = null
        private set

    /** Last post ID that was unliked */
    var lastUnlikedPostId: String? = null
        private set

    /** Last user ID that unliked a post */
    var lastUnlikedByUserId: String? = null
        private set

    /** Last post that was created */
    var lastCreatedPost: Post? = null
        private set

    /** Last post ID that was deleted */
    var lastDeletedPostId: String? = null
        private set

    /**
     * Adds a post to the fake repository for testing.
     */
    fun addPost(post: Post) {
        postsStore[post.id] = post
        updateFlow()
    }

    /**
     * Adds multiple posts to the fake repository.
     */
    fun addPosts(posts: List<Post>) {
        posts.forEach { postsStore[it.id] = it }
        updateFlow()
    }

    /**
     * Clears all posts from the fake repository.
     */
    fun clearPosts() {
        postsStore.clear()
        updateFlow()
    }

    /**
     * Resets all call tracking counters.
     */
    fun resetCallCounts() {
        createPostCallCount = 0
        deletePostCallCount = 0
        likePostCallCount = 0
        unlikePostCallCount = 0
        lastLikedPostId = null
        lastLikedByUserId = null
        lastUnlikedPostId = null
        lastUnlikedByUserId = null
        lastCreatedPost = null
        lastDeletedPostId = null
    }

    /**
     * Gets a post by ID (helper for tests).
     */
    fun getPost(postId: String): Post? = postsStore[postId]

    /**
     * Gets all posts in the repository (helper for tests).
     */
    fun getAllPosts(): List<Post> = postsStore.values.toList()

    private fun updateFlow() {
        postsFlow.value = postsStore.values.toList()
    }

    override suspend fun createPost(post: Post): Result<String> {
        createPostCallCount++
        lastCreatedPost = post

        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            val postId = if (post.id.isBlank()) "generated_${System.currentTimeMillis()}" else post.id
            val newPost = post.copy(id = postId)
            postsStore[postId] = newPost
            updateFlow()
            Result.success(postId)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        deletePostCallCount++
        lastDeletedPostId = postId

        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            postsStore.remove(postId)
            updateFlow()
            Result.success(Unit)
        }
    }

    override fun getPostsByOrganization(organizationId: String): Flow<List<Post>> {
        return postsFlow.map { posts ->
            posts.filter { it.organizationId == organizationId }
                .sortedByDescending { it.createdAt }
        }
    }

    override suspend fun likePost(postId: String, userId: String): Result<Unit> {
        likePostCallCount++
        lastLikedPostId = postId
        lastLikedByUserId = userId

        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            val post = postsStore[postId]
            if (post != null && !post.likedBy.contains(userId)) {
                postsStore[postId] = post.copy(likedBy = post.likedBy + userId)
                updateFlow()
            }
            Result.success(Unit)
        }
    }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        unlikePostCallCount++
        lastUnlikedPostId = postId
        lastUnlikedByUserId = userId

        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            val post = postsStore[postId]
            if (post != null && post.likedBy.contains(userId)) {
                postsStore[postId] = post.copy(likedBy = post.likedBy - userId)
                updateFlow()
            }
            Result.success(Unit)
        }
    }
}
