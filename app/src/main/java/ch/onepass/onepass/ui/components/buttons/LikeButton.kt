package ch.onepass.onepass.ui.components.buttons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.HeartLiked

/**
 * Like button component with heart icon that toggles between liked and unliked states
 *
 * @param isLiked Current liked state
 * @param onLikeToggle Callback when the button is clicked
 * @param modifier Optional modifier for customization
 */
@Composable
fun LikeButton(isLiked: Boolean, onLikeToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
  // Animation for scale effect when clicked
  val scale by
      animateFloatAsState(
          targetValue = if (isLiked) 1.2f else 1f,
          animationSpec = tween(durationMillis = 200),
          label = "likeButtonScale")

  Box(
      modifier =
          modifier
              .size(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
              .clickable(
                  interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    onLikeToggle(!isLiked)
                  },
      contentAlignment = Alignment.Center) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription =
                if (isLiked) stringResource(R.string.button_unlike_description)
                else stringResource(R.string.button_like_description),
            tint = if (isLiked) HeartLiked else colorScheme.onBackground,
            modifier = Modifier.size(24.dp).scale(scale))
      }
}
