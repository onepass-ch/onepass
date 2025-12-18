package ch.onepass.onepass.model.organization

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Post data class and its companion object functions. Tests cover model
 * defaults, content sanitization, validation, and display formatting.
 */
class PostTest {

  // ========================================
  // Tests for Default Values
  // ========================================

  @Test
  fun postHasCorrectDefaults() {
    val post = Post()

    assertEquals("", post.id)
    assertEquals("", post.organizationId)
    assertEquals("", post.authorId)
    assertEquals("", post.authorName)
    assertEquals("", post.content)
    assertTrue(post.likedBy.isEmpty())
    assertEquals(0, post.likesCount)
    assertNull(post.createdAt)
    assertNull(post.updatedAt)
  }

  @Test
  fun postMaxCharactersConstant_is124() {
    assertEquals(124, POST_MAX_CHARACTERS)
  }

  // ========================================
  // Tests for likesCount and isLikedBy
  // ========================================

  @Test
  fun likesCount_returnsZero_whenLikedByIsEmpty() {
    val post = Post(likedBy = emptyList())
    assertEquals(0, post.likesCount)
  }

  @Test
  fun likesCount_returnsCorrectCount_whenUsersHaveLiked() {
    val post = Post(likedBy = listOf("user1", "user2", "user3"))
    assertEquals(3, post.likesCount)
  }

  @Test
  fun likesCount_returnsOne_forSingleLike() {
    val post = Post(likedBy = listOf("user1"))
    assertEquals(1, post.likesCount)
  }

  @Test
  fun isLikedBy_returnsFalse_whenUserNotInList() {
    val post = Post(likedBy = listOf("user1", "user2"))
    assertFalse(post.isLikedBy("user3"))
  }

  @Test
  fun isLikedBy_returnsTrue_whenUserInList() {
    val post = Post(likedBy = listOf("user1", "user2"))
    assertTrue(post.isLikedBy("user1"))
    assertTrue(post.isLikedBy("user2"))
  }

  @Test
  fun isLikedBy_returnsFalse_whenListEmpty() {
    val post = Post(likedBy = emptyList())
    assertFalse(post.isLikedBy("anyUser"))
  }

  @Test
  fun isLikedBy_isCaseSensitive() {
    val post = Post(likedBy = listOf("User1"))
    assertTrue(post.isLikedBy("User1"))
    assertFalse(post.isLikedBy("user1"))
  }

  // ========================================
  // Tests for Content Sanitization
  // ========================================

  @Test
  fun sanitizeContent_trimsLeadingAndTrailingWhitespace() {
    val result = Post.sanitizeContent("  Hello World  ")
    assertEquals("Hello World", result)
  }

  @Test
  fun sanitizeContent_replacesMultipleSpacesWithSingle() {
    val result = Post.sanitizeContent("Hello    World")
    assertEquals("Hello World", result)
  }

  @Test
  fun sanitizeContent_replacesTabsAndNewlinesWithSpace() {
    val result = Post.sanitizeContent("Hello\tWorld\nTest")
    assertEquals("Hello World Test", result)
  }

  @Test
  fun sanitizeContent_truncatesToMaxCharacters() {
    val longContent = "a".repeat(200)
    val result = Post.sanitizeContent(longContent)
    assertNotNull(result)
    assertEquals(POST_MAX_CHARACTERS, result!!.length)
  }

  @Test
  fun sanitizeContent_returnsNull_forEmptyString() {
    val result = Post.sanitizeContent("")
    assertNull(result)
  }

  @Test
  fun sanitizeContent_returnsNull_forBlankString() {
    val result = Post.sanitizeContent("   ")
    assertNull(result)
  }

  @Test
  fun sanitizeContent_returnsNull_forOnlyWhitespace() {
    val result = Post.sanitizeContent("\t\n  \t")
    assertNull(result)
  }

  @Test
  fun sanitizeContent_preservesValidContent() {
    val content = "This is a valid post!"
    val result = Post.sanitizeContent(content)
    assertEquals(content, result)
  }

  @Test
  fun sanitizeContent_handlesExactlyMaxCharacters() {
    val exactContent = "a".repeat(POST_MAX_CHARACTERS)
    val result = Post.sanitizeContent(exactContent)
    assertNotNull(result)
    assertEquals(POST_MAX_CHARACTERS, result!!.length)
  }

  @Test
  fun sanitizeContent_handlesSpecialCharacters() {
    val content = "Hello! @user #hashtag 🎉"
    val result = Post.sanitizeContent(content)
    assertEquals(content, result)
  }

  // ========================================
  // Tests for Content Validation
  // ========================================

  @Test
  fun isValidContent_returnsFalse_forEmptyString() {
    assertFalse(Post.isValidContent(""))
  }

  @Test
  fun isValidContent_returnsFalse_forBlankString() {
    assertFalse(Post.isValidContent("   "))
  }

  @Test
  fun isValidContent_returnsTrue_forValidContent() {
    assertTrue(Post.isValidContent("This is valid content"))
  }

  @Test
  fun isValidContent_returnsTrue_forSingleCharacter() {
    assertTrue(Post.isValidContent("a"))
  }

  @Test
  fun isValidContent_returnsTrue_forExactlyMaxCharacters() {
    val exactContent = "a".repeat(POST_MAX_CHARACTERS)
    assertTrue(Post.isValidContent(exactContent))
  }

  @Test
  fun isValidContent_returnsFalse_forOverMaxCharacters() {
    val longContent = "a".repeat(POST_MAX_CHARACTERS + 1)
    assertFalse(Post.isValidContent(longContent))
  }

  @Test
  fun isValidContent_considersTrimmingBeforeValidation() {
    // Content with leading/trailing spaces that when trimmed is valid
    assertTrue(Post.isValidContent("  valid  "))
  }

  @Test
  fun isValidContent_returnsFalse_forOnlySpacesOverLimit() {
    // Even if just spaces, after trimming it's blank
    assertFalse(Post.isValidContent("   "))
  }

  // ========================================
  // Tests for Display Timestamp
  // ========================================

  @Test
  fun displayTimestamp_returnsEmptyString_whenCreatedAtIsNull() {
    val post = Post(createdAt = null)
    assertEquals("", post.displayTimestamp)
  }

  @Test
  fun displayTimestamp_showsJustNow_forRecentPosts() {
    // Create a timestamp for 30 seconds ago
    val thirtySecondsAgo = Timestamp((System.currentTimeMillis() / 1000) - 30, 0)
    val post = Post(createdAt = thirtySecondsAgo)
    assertEquals("Just now", post.displayTimestamp)
  }

  @Test
  fun displayTimestamp_showsMinutes_forPostsLessThanOneHourOld() {
    // Create a timestamp for 5 minutes ago
    val fiveMinutesAgo = Timestamp((System.currentTimeMillis() / 1000) - (5 * 60), 0)
    val post = Post(createdAt = fiveMinutesAgo)
    assertEquals("5m", post.displayTimestamp)
  }

  @Test
  fun displayTimestamp_showsHours_forPostsLessThanOneDayOld() {
    // Create a timestamp for 3 hours ago
    val threeHoursAgo = Timestamp((System.currentTimeMillis() / 1000) - (3 * 60 * 60), 0)
    val post = Post(createdAt = threeHoursAgo)
    assertEquals("3h", post.displayTimestamp)
  }

  @Test
  fun displayTimestamp_showsDays_forPostsLessThanOneWeekOld() {
    // Create a timestamp for 2 days ago
    val twoDaysAgo = Timestamp((System.currentTimeMillis() / 1000) - (2 * 24 * 60 * 60), 0)
    val post = Post(createdAt = twoDaysAgo)
    assertEquals("2d", post.displayTimestamp)
  }

  // ========================================
  // Tests for Data Class Functionality
  // ========================================

  @Test
  fun postCopy_createsIndependentCopy() {
    val original = Post(id = "post1", content = "Original content", likedBy = listOf("user1"))
    val copy = original.copy(content = "Modified content")

    assertEquals("Original content", original.content)
    assertEquals("Modified content", copy.content)
    assertEquals(original.id, copy.id)
    assertEquals(original.likedBy, copy.likedBy)
  }

  @Test
  fun postEquality_basedOnAllFields() {
    val timestamp = Timestamp.now()
    val post1 =
        Post(
            id = "post1",
            organizationId = "org1",
            content = "Test",
            likedBy = listOf("user1"),
            createdAt = timestamp)
    val post2 =
        Post(
            id = "post1",
            organizationId = "org1",
            content = "Test",
            likedBy = listOf("user1"),
            createdAt = timestamp)
    val post3 =
        Post(
            id = "post2",
            organizationId = "org1",
            content = "Test",
            likedBy = listOf("user1"),
            createdAt = timestamp)

    assertEquals(post1, post2)
    assertNotEquals(post1, post3)
  }

  @Test
  fun postToString_includesImportantFields() {
    val post = Post(id = "test_id", organizationId = "org_123", content = "Hello World")
    val stringRepresentation = post.toString()

    assertTrue(stringRepresentation.contains("test_id"))
    assertTrue(stringRepresentation.contains("org_123"))
    assertTrue(stringRepresentation.contains("Hello World"))
  }

  @Test
  fun postHashCode_sameForEqualPosts() {
    val timestamp = Timestamp.now()
    val post1 = Post(id = "post1", content = "Test", createdAt = timestamp)
    val post2 = Post(id = "post1", content = "Test", createdAt = timestamp)

    assertEquals(post1.hashCode(), post2.hashCode())
  }

  // ========================================
  // Tests for Edge Cases
  // ========================================

  @Test
  fun post_canHaveManyLikes() {
    val manyUsers = (1..1000).map { "user$it" }
    val post = Post(likedBy = manyUsers)
    assertEquals(1000, post.likesCount)
  }

  @Test
  fun post_likedByListCanContainDuplicates_butShouldNotInPractice() {
    // This tests the model's behavior, though duplicates shouldn't happen in practice
    val post = Post(likedBy = listOf("user1", "user1", "user1"))
    assertEquals(3, post.likesCount) // The model counts all entries
  }

  @Test
  fun sanitizeContent_handlesUnicodeContent() {
    val unicodeContent = "你好世界 🌍 مرحبا"
    val result = Post.sanitizeContent(unicodeContent)
    assertEquals(unicodeContent, result)
  }

  @Test
  fun sanitizeContent_handlesEmoji() {
    val emojiContent = "🎉🎊🎁 Party time! 🥳"
    val result = Post.sanitizeContent(emojiContent)
    assertEquals(emojiContent, result)
  }
}
