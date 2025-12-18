package ch.onepass.onepass.model.organization

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/** Maximum character limit for post content. */
const val POST_MAX_CHARACTERS = 124

/**
 * Represents a post created by an organization to communicate with their community.
 *
 * Posts are short announcements or messages (max 124 characters) that organization owners can share
 * with their followers and members.
 *
 * @property id Unique identifier for the post (Firestore document ID).
 * @property organizationId ID of the organization that created the post.
 * @property authorId User ID of the author who created the post.
 * @property authorName Display name of the author at the time of posting.
 * @property content The main text content of the post (max 124 characters).
 * @property likedBy List of user IDs who have liked this post.
 * @property createdAt Timestamp when the post was created (server-set).
 * @property updatedAt Timestamp when the post was last updated (server-set).
 */
data class Post(
    val id: String = "",
    val organizationId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val likedBy: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
  /** Number of likes on the post (derived from likedBy list size). */
  val likesCount: Int
    get() = likedBy.size

  /** Checks if a specific user has liked this post. */
  fun isLikedBy(userId: String): Boolean = likedBy.contains(userId)
  /**
   * Returns a formatted timestamp string for display. Shows relative time if recent, otherwise
   * shows the date.
   */
  val displayTimestamp: String
    get() {
      val postTime = createdAt?.toDate() ?: return ""
      val now = System.currentTimeMillis()
      val diffMs = now - postTime.time
      val diffMinutes = diffMs / (1000 * 60)
      val diffHours = diffMs / (1000 * 60 * 60)
      val diffDays = diffMs / (1000 * 60 * 60 * 24)

      return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "${diffMinutes}m"
        diffHours < 24 -> "${diffHours}h"
        diffDays < 7 -> "${diffDays}d"
        else -> {
          val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
          sdf.format(postTime)
        }
      }
    }

  companion object {
    /**
     * Sanitizes and validates post content.
     * - Trims whitespace
     * - Removes excessive newlines
     * - Enforces character limit
     *
     * @param input Raw input string.
     * @return Sanitized content string, or null if invalid.
     */
    fun sanitizeContent(input: String): String? {
      val sanitized =
          input
              .trim()
              .replace(Regex("\\s+"), " ") // Replace multiple whitespace with single space
              .take(POST_MAX_CHARACTERS)

      return if (sanitized.isBlank()) null else sanitized
    }

    /**
     * Checks if content is valid for posting.
     *
     * @param content The content to validate.
     * @return True if content is valid, false otherwise.
     */
    fun isValidContent(content: String): Boolean {
      val trimmed = content.trim()
      return trimmed.isNotBlank() && trimmed.length <= POST_MAX_CHARACTERS
    }
  }
}
