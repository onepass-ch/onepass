package ch.onepass.onepass.ui.organization

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.R
import ch.onepass.onepass.model.organization.Post
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the PostCard composable.
 * Tests cover display, interactions, and various states.
 */
@RunWith(AndroidJUnit4::class)
class PostCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun createTestPost(
        id: String = "post1",
        content: String = "This is a test post",
        likedBy: List<String> = emptyList(),
        createdAt: Timestamp? = Timestamp.now()
    ): Post = Post(
        id = id,
        organizationId = "org1",
        authorId = "author1",
        authorName = "Test Author",
        content = content,
        likedBy = likedBy,
        createdAt = createdAt
    )

    // ========================================
    // Display Tests
    // ========================================

    @Test
    fun postCard_displaysOrganizationName() {
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Test Organization"
                )
            }
        }

        composeTestRule.onNodeWithText("Test Organization").assertIsDisplayed()
    }

    @Test
    fun postCard_displaysPostContent() {
        val post = createTestPost(content = "Hello, this is my post!")

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org"
                )
            }
        }

        composeTestRule.onNodeWithText("Hello, this is my post!").assertIsDisplayed()
    }

    @Test
    fun postCard_displaysTimestamp() {
        // Use a recent timestamp so it shows "Just now"
        val post = createTestPost(createdAt = Timestamp.now())

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org"
                )
            }
        }

        // The timestamp should be displayed (exact text depends on how recent)
        composeTestRule.onNodeWithTag(PostCardTestTags.POST_TIMESTAMP).assertIsDisplayed()
    }

    @Test
    fun postCard_displaysLikesCount_whenGreaterThanZero() {
        val post = createTestPost(likedBy = listOf("user1", "user2", "user3"))

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org"
                )
            }
        }

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun postCard_doesNotDisplayLikesCount_whenZero() {
        val post = createTestPost(likedBy = emptyList())

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org"
                )
            }
        }

        // Should not display "0" for likes
        composeTestRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun postCard_displaysVerifiedBadge() {
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Verified Org"
                )
            }
        }

        // Verified badge is always shown
        composeTestRule.onNodeWithTag(PostCardTestTags.POST_AUTHOR).assertIsDisplayed()
    }

    // ========================================
    // Like Button Tests
    // ========================================

    @Test
    fun postCard_likeButton_callsOnLikeClick() {
        var clickCount = 0
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org",
                    onLikeClick = { clickCount++ }
                )
            }
        }

        composeTestRule.onNodeWithTag(PostCardTestTags.POST_LIKE_BUTTON).performClick()

        assertEquals(1, clickCount)
    }

    @Test
    fun postCard_likeButton_showsFilledHeart_whenLiked() {
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org",
                    isLiked = true
                )
            }
        }

        val unlikeDesc = context.getString(R.string.org_post_unlike)
        composeTestRule.onNodeWithContentDescription(unlikeDesc).assertIsDisplayed()
    }

    @Test
    fun postCard_likeButton_showsOutlinedHeart_whenNotLiked() {
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org",
                    isLiked = false
                )
            }
        }

        val likeDesc = context.getString(R.string.org_post_like)
        composeTestRule.onNodeWithContentDescription(likeDesc).assertIsDisplayed()
    }

    @Test
    fun postCard_likeButton_togglesState() {
        var isLiked by mutableStateOf(false)
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org",
                    isLiked = isLiked,
                    onLikeClick = { isLiked = !isLiked }
                )
            }
        }

        val likeDesc = context.getString(R.string.org_post_like)
        val unlikeDesc = context.getString(R.string.org_post_unlike)

        // Initially not liked
        composeTestRule.onNodeWithContentDescription(likeDesc).assertIsDisplayed()

        // Click to like
        composeTestRule.onNodeWithTag(PostCardTestTags.POST_LIKE_BUTTON).performClick()
        composeTestRule.onNodeWithContentDescription(unlikeDesc).assertIsDisplayed()

        // Click to unlike
        composeTestRule.onNodeWithTag(PostCardTestTags.POST_LIKE_BUTTON).performClick()
        composeTestRule.onNodeWithContentDescription(likeDesc).assertIsDisplayed()
    }

    // ========================================
    // Delete Button Tests
    // ========================================

    @Test
    fun postCard_moreButton_isHidden_whenCannotDelete() {
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org",
                    canDelete = false
                )
            }
        }

        composeTestRule.onNodeWithTag(PostCardTestTags.POST_MORE_BUTTON).assertDoesNotExist()
    }

    @Test
    fun postCard_moreButton_isVisible_whenCanDelete() {
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org",
                    canDelete = true
                )
            }
        }

        composeTestRule.onNodeWithTag(PostCardTestTags.POST_MORE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun postCard_moreButton_opensDropdownMenu() {
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org",
                    canDelete = true
                )
            }
        }

        // Click the more button
        composeTestRule.onNodeWithTag(PostCardTestTags.POST_MORE_BUTTON).performClick()

        // Delete option should appear
        val deleteText = context.getString(R.string.org_post_delete)
        composeTestRule.onNodeWithText(deleteText).assertIsDisplayed()
    }

    @Test
    fun postCard_deleteOption_callsOnDeleteClick() {
        var deleteClicked = false
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org",
                    canDelete = true,
                    onDeleteClick = { deleteClicked = true }
                )
            }
        }

        // Open menu and click delete
        composeTestRule.onNodeWithTag(PostCardTestTags.POST_MORE_BUTTON).performClick()
        composeTestRule.onNodeWithTag(PostCardTestTags.POST_DELETE_BUTTON).performClick()

        assertTrue(deleteClicked)
    }

    // ========================================
    // Content Edge Cases
    // ========================================

    @Test
    fun postCard_handlesLongContent() {
        val longContent = "This is a very long post that contains many words. " +
                "It should be displayed properly without breaking the layout. " +
                "The text should wrap to multiple lines."
        val post = createTestPost(content = longContent)

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org"
                )
            }
        }

        composeTestRule.onNodeWithText(longContent).assertIsDisplayed()
    }

    @Test
    fun postCard_handlesSpecialCharacters() {
        val specialContent = "Hello! @user #hashtag 🎉 <test> & more"
        val post = createTestPost(content = specialContent)

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org"
                )
            }
        }

        composeTestRule.onNodeWithText(specialContent).assertIsDisplayed()
    }

    @Test
    fun postCard_handlesEmoji() {
        val emojiContent = "🎉🎊🎁 Party time! 🥳"
        val post = createTestPost(content = emojiContent)

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org"
                )
            }
        }

        composeTestRule.onNodeWithText(emojiContent).assertIsDisplayed()
    }

    @Test
    fun postCard_handlesLongOrganizationName() {
        val post = createTestPost()
        val longOrgName = "This Is A Very Long Organization Name That Should Be Truncated"

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = longOrgName
                )
            }
        }

        // Should exist even if truncated
        composeTestRule.onNodeWithTag(PostCardTestTags.POST_AUTHOR).assertIsDisplayed()
    }

    // ========================================
    // Avatar Tests
    // ========================================

    @Test
    fun postCard_displaysAvatarWithInitial_whenNoImageUrl() {
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Test Org",
                    organizationImageUrl = null
                )
            }
        }

        // The avatar with initial "T" should be displayed
        composeTestRule.onNodeWithText("T").assertIsDisplayed()
    }

    @Test
    fun postCard_displaysAvatarImage_whenImageUrlProvided() {
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Test Org",
                    organizationImageUrl = "https://example.com/image.jpg"
                )
            }
        }

        // When an image URL is provided, the initial should not be displayed
        composeTestRule.onNodeWithText("T").assertDoesNotExist()
    }

    // ========================================
    // Test Tags Tests
    // ========================================

    @Test
    fun postCard_hasCorrectTestTag() {
        val post = createTestPost(id = "unique-post-id")

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org"
                )
            }
        }

        composeTestRule.onNodeWithTag(PostCardTestTags.getPostCardTag("unique-post-id"))
            .assertIsDisplayed()
    }

    @Test
    fun postCard_contentHasCorrectTestTag() {
        val post = createTestPost()

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org"
                )
            }
        }

        composeTestRule.onNodeWithTag(PostCardTestTags.POST_CONTENT).assertIsDisplayed()
    }

    // ========================================
    // Large Like Count Test
    // ========================================

    @Test
    fun postCard_formatsLargeLikeCount() {
        // Create a post with 1500 likes
        val likedBy = (1..1500).map { "user$it" }
        val post = createTestPost(likedBy = likedBy)

        composeTestRule.setContent {
            MaterialTheme {
                PostCard(
                    post = post,
                    organizationName = "Org"
                )
            }
        }

        // Should show formatted count like "1.5K"
        composeTestRule.onNodeWithText("1.5K").assertIsDisplayed()
    }
}
