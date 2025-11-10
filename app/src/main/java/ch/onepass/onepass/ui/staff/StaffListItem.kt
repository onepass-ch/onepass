package ch.onepass.onepass.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.staff.StaffSearchResult
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.util.Locale

object StaffTestTags {
  object Item {
    const val LIST_ITEM = "staffItem_listItem"
    const val HEADLINE = "staffItem_headline"
    const val SUPPORTING = "staffItem_supporting"
  }

  object Avatar {
    const val ROOT = "staffItem_avatar_root"
    const val IMAGE = "staffItem_avatar_image"

    object Initials {
      const val TEXT = "staffItem_avatar_initials_text"
    }

    object Loading {
      const val CONTAINER = "staffItem_avatar_loading_container"
      const val TEXT = "staffItem_avatar_loading_text"
    }

    object Error {
      const val CONTAINER = "staffItem_avatar_error_container"
      const val TEXT = "staffItem_avatar_error_text"
    }
  }
}

/**
 * Renders a staff candidate item.
 *
 * Preconditions (must be guaranteed by the caller):
 * - [user.displayName] is non-blank.
 * - [user.email] is non-blank.
 * - [user.avatarUrl] is either a valid URL string or null.
 *
 * All data sanitation must be done at the list layer (VM/mapper).
 */
@Composable
fun StaffListItem(
    user: StaffSearchResult,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?,
    enabled: Boolean = true
) {
  ListItem(
      modifier =
          modifier
              .fillMaxWidth()
              .background(colorResource(id = R.color.staff_list_item_background))
              .then(
                  if (enabled && onClick != null) {
                    Modifier.clickable(role = Role.Button) { onClick() }
                  } else {
                    Modifier
                  })
              .testTag(StaffTestTags.Item.LIST_ITEM),
      leadingContent = {
        Avatar(
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            modifier = Modifier.size(48.dp))
      },
      headlineContent = {
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(StaffTestTags.Item.HEADLINE))
      },
      supportingContent = {
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(StaffTestTags.Item.SUPPORTING))
      })
}

private fun computeMonogram(name: String): String {
  val trimmed = name.trim()
  if (trimmed.isEmpty()) return "?"
  val cp = Character.codePointAt(trimmed, 0)
  val first = String(Character.toChars(cp))
  return first.uppercase(Locale.ROOT)
}

@Composable
private fun Avatar(displayName: String, avatarUrl: String?, modifier: Modifier = Modifier) {
  val initials = remember(displayName) { computeMonogram(displayName) }
  val context = LocalContext.current

  Box(
      modifier =
          modifier
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer)
              .testTag(StaffTestTags.Avatar.ROOT),
      contentAlignment = Alignment.Center) {
        if (avatarUrl.isNullOrBlank()) {
          Text(
              text = initials,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.testTag(StaffTestTags.Avatar.Initials.TEXT))
        } else {
          SubcomposeAsyncImage(
              model = ImageRequest.Builder(context).data(avatarUrl).crossfade(200).build(),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.fillMaxSize().clip(CircleShape).testTag(StaffTestTags.Avatar.IMAGE),
              loading = {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .clip(CircleShape)
                            .testTag(StaffTestTags.Avatar.Loading.CONTAINER),
                    contentAlignment = Alignment.Center) {
                      Text(
                          text = initials,
                          style =
                              MaterialTheme.typography.labelLarge.copy(
                                  fontWeight = FontWeight.SemiBold),
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          modifier = Modifier.testTag(StaffTestTags.Avatar.Loading.TEXT))
                    }
              },
              error = {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .clip(CircleShape)
                            .testTag(StaffTestTags.Avatar.Error.CONTAINER),
                    contentAlignment = Alignment.Center) {
                      Text(
                          text = initials,
                          style =
                              MaterialTheme.typography.labelLarge.copy(
                                  fontWeight = FontWeight.SemiBold),
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          modifier = Modifier.testTag(StaffTestTags.Avatar.Error.TEXT))
                    }
              })
        }
      }
}
