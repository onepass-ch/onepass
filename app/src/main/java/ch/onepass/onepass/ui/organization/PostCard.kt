package ch.onepass.onepass.ui.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.organization.Post
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/** Test tags for PostCard composables. */
object PostCardTestTags {
  const val POST_CARD = "post_card"
  const val POST_CONTENT = "post_content"
  const val POST_AUTHOR = "post_author"
  const val POST_TIMESTAMP = "post_timestamp"
  const val POST_LIKE_BUTTON = "post_like_button"
  const val POST_LIKES_COUNT = "post_likes_count"
  const val POST_DELETE_BUTTON = "post_delete_button"
  const val POST_MORE_BUTTON = "post_more_button"

  fun getPostCardTag(postId: String) = "post_card_$postId"
}

/**
 * Modern, Twitter-inspired post card with larger design.
 *
 * Features a clean layout with organization avatar, content, and engagement actions at the bottom.
 *
 * @param post The [Post] to display.
 * @param organizationName Name of the organization for display.
 * @param organizationImageUrl Optional profile image URL of the organization.
 * @param isLiked Whether the current user has liked this post.
 * @param canDelete Whether the current user can delete this post (owner).
 * @param onLikeClick Callback when the like button is clicked.
 * @param onDeleteClick Callback when the delete button is clicked.
 * @param modifier Optional [Modifier] for layout adjustments.
 */
@Composable
fun PostCard(
    post: Post,
    organizationName: String,
    organizationImageUrl: String? = null,
    isLiked: Boolean = false,
    canDelete: Boolean = false,
    onLikeClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }

  Card(
      modifier = modifier.fillMaxWidth().testTag(PostCardTestTags.getPostCardTag(post.id)),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Header: Avatar + Name/Time + More button
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                  // Large Avatar
                  OrganizationAvatar(
                      organizationName = organizationName,
                      imageUrl = organizationImageUrl,
                      size = 52)

                  Spacer(modifier = Modifier.width(14.dp))

                  Column {
                    // Organization name with verified badge style
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = organizationName,
                          style =
                              MaterialTheme.typography.titleMedium.copy(
                                  fontWeight = FontWeight.Bold, fontSize = 16.sp),
                          color = colorScheme.onSurface,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          modifier = Modifier.testTag(PostCardTestTags.POST_AUTHOR))
                      Spacer(modifier = Modifier.width(4.dp))
                      Icon(
                          imageVector = Icons.Filled.Verified,
                          contentDescription = null,
                          tint = colorScheme.primary,
                          modifier = Modifier.size(18.dp))
                    }

                    // Timestamp
                    Text(
                        text = post.displayTimestamp,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = colorScheme.outline,
                        modifier = Modifier.testTag(PostCardTestTags.POST_TIMESTAMP))
                  }
                }

                // More Options button
                if (canDelete) {
                  Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier =
                            Modifier.size(36.dp).testTag(PostCardTestTags.POST_MORE_BUTTON)) {
                          Icon(
                              imageVector = Icons.Filled.MoreHoriz,
                              contentDescription = stringResource(R.string.org_post_more_options),
                              tint = colorScheme.outline,
                              modifier = Modifier.size(22.dp))
                        }

                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                      DropdownMenuItem(
                          text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(
                                  imageVector = Icons.Filled.Delete,
                                  contentDescription = null,
                                  tint = colorScheme.error,
                                  modifier = Modifier.size(20.dp))
                              Spacer(modifier = Modifier.width(12.dp))
                              Text(
                                  text = stringResource(R.string.org_post_delete),
                                  color = colorScheme.error,
                                  style =
                                      MaterialTheme.typography.bodyMedium.copy(
                                          fontWeight = FontWeight.Medium))
                            }
                          },
                          onClick = {
                            showMenu = false
                            onDeleteClick()
                          },
                          modifier = Modifier.testTag(PostCardTestTags.POST_DELETE_BUTTON))
                    }
                  }
                }
              }

          Spacer(modifier = Modifier.height(14.dp))

          // Post Content - Larger text
          Text(
              text = post.content,
              style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
              color = colorScheme.onSurface,
              modifier = Modifier.fillMaxWidth().testTag(PostCardTestTags.POST_CONTENT))

          Spacer(modifier = Modifier.height(16.dp))

          // Divider
          HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

          Spacer(modifier = Modifier.height(12.dp))

          // Action Row - Like button
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Start,
              verticalAlignment = Alignment.CenterVertically) {
                // Like action
                LikeButton(
                    isLiked = isLiked,
                    likesCount = post.likesCount,
                    onClick = onLikeClick,
                    modifier = Modifier.testTag(PostCardTestTags.POST_LIKE_BUTTON))
              }
        }
      }
}

/** Like button with count display. */
@Composable
private fun LikeButton(
    isLiked: Boolean,
    likesCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val tint = if (isLiked) colorScheme.error else colorScheme.outline

  Row(
      modifier =
          modifier
              .clip(RoundedCornerShape(20.dp))
              .clickable { onClick() }
              .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription =
                if (isLiked) {
                  stringResource(R.string.org_post_unlike)
                } else {
                  stringResource(R.string.org_post_like)
                },
            tint = tint,
            modifier = Modifier.size(24.dp))
        if (likesCount > 0) {
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = formatCount(likesCount),
              style =
                  MaterialTheme.typography.labelLarge.copy(
                      fontSize = 14.sp, fontWeight = FontWeight.Medium),
              color = tint,
              modifier = Modifier.testTag(PostCardTestTags.POST_LIKES_COUNT))
        }
      }
}

/** Formats a count for display (e.g., 1000 -> "1K") */
private fun formatCount(count: Int): String {
  return when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
  }
}

/**
 * Circular avatar showing organization profile image or initials.
 *
 * @param organizationName Name of the organization for initials.
 * @param imageUrl Optional profile image URL.
 * @param size Size of the avatar in dp.
 */
@Composable
fun OrganizationAvatar(organizationName: String, imageUrl: String?, size: Int = 52) {
  val initials = organizationName.take(1).uppercase()

  Box(
      modifier = Modifier.size(size.dp).clip(CircleShape).background(colorScheme.primary),
      contentAlignment = Alignment.Center) {
        if (imageUrl.isNullOrBlank()) {
          Text(
              text = initials,
              style =
                  MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.Bold, fontSize = (size / 2.5).sp),
              color = colorScheme.onPrimary)
        } else {
          SubcomposeAsyncImage(
              model =
                  ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
              contentDescription = stringResource(R.string.org_post_avatar_description),
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(size.dp).clip(CircleShape),
              loading = {
                Box(
                    modifier =
                        Modifier.size(size.dp).background(colorScheme.surfaceVariant, CircleShape))
              })
        }
      }
}
