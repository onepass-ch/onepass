package ch.onepass.onepass.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CloseButton(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  Box(
      modifier =
          modifier
              .size(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
              .clickable(
                  interactionSource =
                      remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                      },
                  indication = null,
              ) {
                onDismiss()
              },
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Close",
        tint = Color.White,
        modifier = Modifier.size(24.dp),
    )
  }
}
