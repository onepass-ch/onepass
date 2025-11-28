package ch.onepass.onepass.ui.components.buttons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R

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
            contentDescription = if (isLiked) "Unlike" else "Like",
            tint = if (isLiked) colorResource(id = R.color.heart_liked) else Color.White,
            modifier = Modifier.size(24.dp).scale(scale))
      }
}
