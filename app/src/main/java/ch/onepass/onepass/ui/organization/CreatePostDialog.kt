package ch.onepass.onepass.ui.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.onepass.onepass.R
import ch.onepass.onepass.model.organization.POST_MAX_CHARACTERS
import ch.onepass.onepass.model.organization.Post

/** Test tags for CreatePostDialog composables. */
object CreatePostDialogTestTags {
  const val DIALOG = "create_post_dialog"
  const val CONTENT_FIELD = "create_post_content_field"
  const val SUBMIT_BUTTON = "create_post_submit_button"
  const val CANCEL_BUTTON = "create_post_cancel_button"
  const val CHARACTER_COUNT = "create_post_character_count"
  const val AVATAR = "create_post_avatar"
}

/**
 * Twitter-inspired post composer dialog.
 *
 * Allows organizers to compose short posts (max 124 characters) with real-time character count and
 * input sanitization.
 *
 * @param organizationName Name of the organization for display.
 * @param organizationImageUrl Optional profile image URL.
 * @param isSubmitting Whether the post is currently being submitted.
 * @param onSubmit Callback when the submit button is clicked with sanitized content.
 * @param onDismiss Callback when the dialog is dismissed.
 */
@Composable
fun CreatePostComposer(
    organizationName: String,
    organizationImageUrl: String? = null,
    isSubmitting: Boolean = false,
    onSubmit: (content: String) -> Unit,
    onDismiss: () -> Unit
) {
  var content by remember { mutableStateOf("") }

  val characterCount by remember(content) { derivedStateOf { content.length } }

  val isOverLimit by
      remember(characterCount) { derivedStateOf { characterCount > POST_MAX_CHARACTERS } }

  val isContentValid by remember(content) { derivedStateOf { Post.isValidContent(content) } }

  val remainingChars by
      remember(characterCount) { derivedStateOf { POST_MAX_CHARACTERS - characterCount } }

  Dialog(
      onDismissRequest = { if (!isSubmitting) onDismiss() },
      properties =
          DialogProperties(
              dismissOnBackPress = !isSubmitting,
              dismissOnClickOutside = !isSubmitting,
              usePlatformDefaultWidth = false)) {
        Surface(
            modifier =
                Modifier.fillMaxWidth().padding(16.dp).testTag(CreatePostDialogTestTags.DIALOG),
            shape = RoundedCornerShape(16.dp),
            color = colorScheme.surface) {
              Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      IconButton(
                          onClick = onDismiss,
                          enabled = !isSubmitting,
                          modifier = Modifier.testTag(CreatePostDialogTestTags.CANCEL_BUTTON)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription =
                                    stringResource(R.string.org_post_cancel_button),
                                tint = colorScheme.onSurface)
                          }

                      Button(
                          onClick = {
                            Post.sanitizeContent(content)?.let { sanitized -> onSubmit(sanitized) }
                          },
                          enabled = isContentValid && !isSubmitting,
                          colors =
                              ButtonDefaults.buttonColors(
                                  containerColor = colorScheme.primary,
                                  disabledContainerColor = colorScheme.primary.copy(alpha = 0.5f)),
                          shape = RoundedCornerShape(20.dp),
                          modifier =
                              Modifier.height(36.dp)
                                  .testTag(CreatePostDialogTestTags.SUBMIT_BUTTON)) {
                            if (isSubmitting) {
                              CircularProgressIndicator(
                                  modifier = Modifier.size(18.dp),
                                  color = colorScheme.onPrimary,
                                  strokeWidth = 2.dp)
                            } else {
                              Text(
                                  text = stringResource(R.string.org_post_submit_button),
                                  style = MaterialTheme.typography.labelLarge,
                                  fontWeight = FontWeight.Bold)
                            }
                          }
                    }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Composer Body
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                  // Avatar
                  OrganizationAvatar(
                      organizationName = organizationName,
                      imageUrl = organizationImageUrl,
                      size = 40)

                  Spacer(modifier = Modifier.width(12.dp))

                  // Text Input
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = organizationName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface)

                    TextField(
                        value = content,
                        onValueChange = { newValue ->
                          // Allow typing but show warning if over limit
                          content = newValue
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .heightIn(min = 100.dp)
                                .testTag(CreatePostDialogTestTags.CONTENT_FIELD),
                        placeholder = {
                          Text(
                              text = stringResource(R.string.org_post_content_placeholder),
                              style = MaterialTheme.typography.bodyLarge,
                              color = colorScheme.outline)
                        },
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = colorScheme.onSurface,
                                unfocusedTextColor = colorScheme.onSurface),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        enabled = !isSubmitting,
                        singleLine = false,
                        maxLines = 5)
                  }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Footer with character count
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                      // Character Counter Circle
                      CharacterCountIndicator(
                          current = characterCount,
                          max = POST_MAX_CHARACTERS,
                          modifier = Modifier.testTag(CreatePostDialogTestTags.CHARACTER_COUNT))

                      if (characterCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = remainingChars.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color =
                                when {
                                  isOverLimit -> colorScheme.error
                                  remainingChars <= 20 -> colorScheme.tertiary
                                  else -> colorScheme.outline
                                })
                      }
                    }
              }
            }
      }
}

/**
 * Circular progress indicator for character count.
 *
 * @param current Current character count.
 * @param max Maximum allowed characters.
 * @param modifier Optional modifier.
 */
@Composable
private fun CharacterCountIndicator(current: Int, max: Int, modifier: Modifier = Modifier) {
  val progress = (current.toFloat() / max).coerceIn(0f, 1f)
  val isOverLimit = current > max
  val isNearLimit = current > max - 20 && !isOverLimit

  val color =
      when {
        isOverLimit -> colorScheme.error
        isNearLimit -> colorScheme.tertiary
        else -> colorScheme.primary
      }

  Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(
        progress = { if (isOverLimit) 1f else progress },
        modifier = Modifier.size(24.dp),
        color = color,
        trackColor = colorScheme.outlineVariant,
        strokeWidth = 2.dp)
  }
}

/**
 * Confirmation dialog for deleting a post.
 *
 * @param isDeleting Whether the deletion is in progress.
 * @param onConfirm Callback when the delete is confirmed.
 * @param onDismiss Callback when the dialog is dismissed.
 */
@Composable
fun DeletePostConfirmationDialog(
    isDeleting: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = { if (!isDeleting) onDismiss() }) {
    Surface(shape = RoundedCornerShape(16.dp), color = colorScheme.surface) {
      Column(
          modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.org_post_delete_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.org_post_delete_message),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  Button(
                      onClick = onDismiss,
                      enabled = !isDeleting,
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = colorScheme.surfaceVariant,
                              contentColor = colorScheme.onSurface),
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(20.dp)) {
                        Text(stringResource(R.string.org_post_delete_cancel))
                      }

                  Button(
                      onClick = onConfirm,
                      enabled = !isDeleting,
                      colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(20.dp)) {
                        if (isDeleting) {
                          CircularProgressIndicator(
                              modifier = Modifier.size(18.dp),
                              color = colorScheme.onError,
                              strokeWidth = 2.dp)
                        } else {
                          Text(stringResource(R.string.org_post_delete_confirm))
                        }
                      }
                }
          }
    }
  }
}
