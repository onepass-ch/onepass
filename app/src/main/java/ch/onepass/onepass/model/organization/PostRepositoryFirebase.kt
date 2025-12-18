package ch.onepass.onepass.model.organization

import android.util.Log
import ch.onepass.onepass.model.firestore.firestoreFlow
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of [PostRepository].
 *
 * This repository handles all post-related CRUD operations using Firebase Firestore.
 * Posts are stored in a top-level "posts" collection and queried by organization ID.
 */
class PostRepositoryFirebase : PostRepository {
    private val postsCollection = Firebase.firestore.collection("posts")

    companion object {
        private const val TAG = "PostRepository"
    }

    override suspend fun createPost(post: Post): Result<String> = runCatching {
        // Validate and sanitize content before saving
        val sanitizedContent = Post.sanitizeContent(post.content)
            ?: throw IllegalArgumentException("Post content cannot be empty")
        
        val docRef = postsCollection.document()
        val postWithMetadata = post.copy(
            id = docRef.id,
            content = sanitizedContent,
            createdAt = null, // Will be set by @ServerTimestamp
            updatedAt = null
        )
        docRef.set(postWithMetadata).await()
        Log.d(TAG, "Post created successfully with ID: ${docRef.id}")
        docRef.id
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        postsCollection.document(postId).delete().await()
        Log.d(TAG, "Post deleted successfully: $postId")
    }

    override fun getPostsByOrganization(organizationId: String): Flow<List<Post>> = 
        firestoreFlow<Post> {
            postsCollection.whereEqualTo("organizationId", organizationId)
        }.map { posts ->
            // Sort locally to avoid requiring a composite index
            posts.sortedByDescending { it.createdAt }
        }

    override suspend fun likePost(postId: String, userId: String): Result<Unit> = runCatching {
        postsCollection.document(postId)
            .update(
                mapOf(
                    "likedBy" to FieldValue.arrayUnion(userId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
        Log.d(TAG, "Post liked by user $userId: $postId")
    }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> = runCatching {
        postsCollection.document(postId)
            .update(
                mapOf(
                    "likedBy" to FieldValue.arrayRemove(userId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
        Log.d(TAG, "Post unliked by user $userId: $postId")
    }
}
