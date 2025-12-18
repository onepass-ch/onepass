package ch.onepass.onepass.model.organization

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining operations for managing organization posts.
 *
 * This repository handles CRUD operations for posts that organizations create to communicate with
 * their community.
 */
interface PostRepository {
  /**
   * Creates a new post for an organization.
   *
   * @param post The [Post] to create (id will be generated).
   * @return A [Result] containing the newly created post's ID on success, or an error.
   */
  suspend fun createPost(post: Post): Result<String>

  /**
   * Deletes a post by its ID.
   *
   * @param postId The unique identifier of the post to delete.
   * @return A [Result] indicating success or failure.
   */
  suspend fun deletePost(postId: String): Result<Unit>

  /**
   * Retrieves all posts for a specific organization, ordered by creation date (newest first).
   *
   * @param organizationId The organization's ID.
   * @return A [Flow] emitting a list of posts for the organization.
   */
  fun getPostsByOrganization(organizationId: String): Flow<List<Post>>

  /**
   * Adds the current user's like to a post. Does nothing if the user has already liked the post.
   *
   * @param postId The post's ID.
   * @param userId The ID of the user liking the post.
   * @return A [Result] indicating success or failure.
   */
  suspend fun likePost(postId: String, userId: String): Result<Unit>

  /**
   * Removes the current user's like from a post. Does nothing if the user hasn't liked the post.
   *
   * @param postId The post's ID.
   * @param userId The ID of the user unliking the post.
   * @return A [Result] indicating success or failure.
   */
  suspend fun unlikePost(postId: String, userId: String): Result<Unit>
}
