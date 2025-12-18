package ch.onepass.onepass.ui.organization

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.R
import ch.onepass.onepass.model.organization.POST_MAX_CHARACTERS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the CreatePostComposer and DeletePostConfirmationDialog composables. Tests cover
 * display, interactions, validation, and various states.
 */
@RunWith(AndroidJUnit4::class)
class CreatePostDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  // ========================================
  // CreatePostComposer Display Tests
  // ========================================

  @Test
  fun createPostComposer_displaysDialog() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.DIALOG).assertIsDisplayed()
  }

  @Test
  fun createPostComposer_displaysOrganizationName() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "My Organization", onSubmit = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithText("My Organization").assertIsDisplayed()
  }

  @Test
  fun createPostComposer_displaysContentField() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.CONTENT_FIELD).assertIsDisplayed()
  }

  @Test
  fun createPostComposer_displaysSubmitButton() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.SUBMIT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun createPostComposer_displaysCancelButton() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.CANCEL_BUTTON).assertIsDisplayed()
  }

  @Test
  fun createPostComposer_displaysCharacterCountIndicator() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.CHARACTER_COUNT).assertIsDisplayed()
  }

  // ========================================
  // CreatePostComposer Validation Tests
  // ========================================

  @Test
  fun createPostComposer_submitButtonDisabled_whenContentEmpty() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.SUBMIT_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun createPostComposer_submitButtonEnabled_whenContentValid() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    composeTestRule
        .onNodeWithTag(CreatePostDialogTestTags.CONTENT_FIELD)
        .performTextInput("Valid post content")

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.SUBMIT_BUTTON).assertIsEnabled()
  }

  @Test
  fun createPostComposer_submitButtonDisabled_whenContentTooLong() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    val tooLongContent = "a".repeat(POST_MAX_CHARACTERS + 10)
    composeTestRule
        .onNodeWithTag(CreatePostDialogTestTags.CONTENT_FIELD)
        .performTextInput(tooLongContent)

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.SUBMIT_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun createPostComposer_submitButtonEnabled_whenContentExactlyMaxLength() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    val exactContent = "a".repeat(POST_MAX_CHARACTERS)
    composeTestRule
        .onNodeWithTag(CreatePostDialogTestTags.CONTENT_FIELD)
        .performTextInput(exactContent)

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.SUBMIT_BUTTON).assertIsEnabled()
  }

  // ========================================
  // CreatePostComposer Interaction Tests
  // ========================================

  @Test
  fun createPostComposer_cancelButtonCallsDismiss() {
    var dismissCalled = false

    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(
            organizationName = "Test Org", onSubmit = {}, onDismiss = { dismissCalled = true })
      }
    }

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.CANCEL_BUTTON).performClick()

    assertTrue(dismissCalled)
  }

  @Test
  fun createPostComposer_submitButtonCallsOnSubmit_withContent() {
    var submittedContent: String? = null

    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(
            organizationName = "Test Org",
            onSubmit = { content -> submittedContent = content },
            onDismiss = {})
      }
    }

    composeTestRule
        .onNodeWithTag(CreatePostDialogTestTags.CONTENT_FIELD)
        .performTextInput("My post content")

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.SUBMIT_BUTTON).performClick()

    assertEquals("My post content", submittedContent)
  }

  @Test
  fun createPostComposer_submitButtonDisabled_whenSubmitting() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(
            organizationName = "Test Org", isSubmitting = true, onSubmit = {}, onDismiss = {})
      }
    }

    // When submitting, submit button should be disabled regardless of content
    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.SUBMIT_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun createPostComposer_cancelButtonDisabled_whenSubmitting() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(
            organizationName = "Test Org", isSubmitting = true, onSubmit = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.CANCEL_BUTTON).assertIsNotEnabled()
  }

  // ========================================
  // CreatePostComposer Character Count Tests
  // ========================================

  @Test
  fun createPostComposer_showsRemainingCharacters_whenTyping() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.CONTENT_FIELD).performTextInput("Hello")

    // Should show remaining characters (POST_MAX_CHARACTERS - 5)
    val expectedRemaining = (POST_MAX_CHARACTERS - 5).toString()
    composeTestRule.onNodeWithText(expectedRemaining).assertIsDisplayed()
  }

  // ========================================
  // CreatePostComposer Edge Cases
  // ========================================

  @Test
  fun createPostComposer_handlesSpecialCharacters() {
    var submittedContent: String? = null

    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(
            organizationName = "Test Org",
            onSubmit = { content -> submittedContent = content },
            onDismiss = {})
      }
    }

    composeTestRule
        .onNodeWithTag(CreatePostDialogTestTags.CONTENT_FIELD)
        .performTextInput("Hello @user #tag!")

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.SUBMIT_BUTTON).performClick()

    assertEquals("Hello @user #tag!", submittedContent)
  }

  @Test
  fun createPostComposer_displaysPlaceholder() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(organizationName = "Test Org", onSubmit = {}, onDismiss = {})
      }
    }

    val placeholder = context.getString(R.string.org_post_content_placeholder)
    composeTestRule.onNodeWithText(placeholder).assertIsDisplayed()
  }

  @Test
  fun createPostComposer_displaysWithOrganizationImageUrl() {
    composeTestRule.setContent {
      MaterialTheme {
        CreatePostComposer(
            organizationName = "Test Org",
            organizationImageUrl = "https://example.com/image.jpg",
            onSubmit = {},
            onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag(CreatePostDialogTestTags.DIALOG).assertIsDisplayed()
  }

  // ========================================
  // DeletePostConfirmationDialog Tests
  // ========================================

  @Test
  fun deletePostDialog_displaysTitle() {
    composeTestRule.setContent {
      MaterialTheme { DeletePostConfirmationDialog(onConfirm = {}, onDismiss = {}) }
    }

    val title = context.getString(R.string.org_post_delete_title)
    composeTestRule.onNodeWithText(title).assertIsDisplayed()
  }

  @Test
  fun deletePostDialog_displaysMessage() {
    composeTestRule.setContent {
      MaterialTheme { DeletePostConfirmationDialog(onConfirm = {}, onDismiss = {}) }
    }

    val message = context.getString(R.string.org_post_delete_message)
    composeTestRule.onNodeWithText(message).assertIsDisplayed()
  }

  @Test
  fun deletePostDialog_displaysCancelButton() {
    composeTestRule.setContent {
      MaterialTheme { DeletePostConfirmationDialog(onConfirm = {}, onDismiss = {}) }
    }

    val cancelText = context.getString(R.string.org_post_delete_cancel)
    composeTestRule.onNodeWithText(cancelText).assertIsDisplayed()
  }

  @Test
  fun deletePostDialog_displaysConfirmButton() {
    composeTestRule.setContent {
      MaterialTheme { DeletePostConfirmationDialog(onConfirm = {}, onDismiss = {}) }
    }

    val confirmText = context.getString(R.string.org_post_delete_confirm)
    composeTestRule.onNodeWithText(confirmText).assertIsDisplayed()
  }

  @Test
  fun deletePostDialog_confirmButtonCallsOnConfirm() {
    var confirmCalled = false

    composeTestRule.setContent {
      MaterialTheme {
        DeletePostConfirmationDialog(onConfirm = { confirmCalled = true }, onDismiss = {})
      }
    }

    val confirmText = context.getString(R.string.org_post_delete_confirm)
    composeTestRule.onNodeWithText(confirmText).performClick()

    assertTrue(confirmCalled)
  }

  @Test
  fun deletePostDialog_cancelButtonCallsOnDismiss() {
    var dismissCalled = false

    composeTestRule.setContent {
      MaterialTheme {
        DeletePostConfirmationDialog(onConfirm = {}, onDismiss = { dismissCalled = true })
      }
    }

    val cancelText = context.getString(R.string.org_post_delete_cancel)
    composeTestRule.onNodeWithText(cancelText).performClick()

    assertTrue(dismissCalled)
  }

  @Test
  fun deletePostDialog_buttonsDisabled_whenDeleting() {
    composeTestRule.setContent {
      MaterialTheme {
        DeletePostConfirmationDialog(isDeleting = true, onConfirm = {}, onDismiss = {})
      }
    }

    val cancelText = context.getString(R.string.org_post_delete_cancel)

    composeTestRule.onNodeWithText(cancelText).assertIsNotEnabled()
  }

  @Test
  fun deletePostDialog_showsProgressIndicator_whenDeleting() {
    composeTestRule.setContent {
      MaterialTheme {
        DeletePostConfirmationDialog(isDeleting = true, onConfirm = {}, onDismiss = {})
      }
    }

    // When deleting, the confirm button text should not be visible (replaced by progress indicator)
    val confirmText = context.getString(R.string.org_post_delete_confirm)
    composeTestRule.onNodeWithText(confirmText).assertDoesNotExist()
  }
}
