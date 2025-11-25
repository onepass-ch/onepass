package ch.onepass.onepass.ui.myevents

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Composable that displays a QR code icon which, when clicked, shows a dialog with the full QR
 * code.
 *
 * @param qrData The data to encode in the QR code
 * @param modifier Modifier for styling
 */
@Composable
fun QrCodeComponent(qrData: String, modifier: Modifier = Modifier) {
  // State to track whether the QR code is expanded or collapsed
  var isExpanded by remember { mutableStateOf(false) }

  // Calculate heights based on screen size
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp
  val expandedHeight = screenHeight * 0.55f

  // Animated height for the card
  val animatedHeight by
      animateDpAsState(
          targetValue = if (isExpanded) expandedHeight else 150.dp,
          animationSpec =
              spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
          label = "heightAnim")

  // Animated scale for the QR code image
  val qrScale by
      animateFloatAsState(
          targetValue = if (isExpanded) 1f else 0.1f,
          animationSpec =
              spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))

  // Generate QR code bitmap
  val qrBitmap: Bitmap =
      remember(qrData) {
        val size = 800
        val bits = QRCodeWriter().encode(qrData, BarcodeFormat.QR_CODE, size, size)
        createBitmap(size, size).also { bmp ->
          for (x in 0 until size) {
            for (y in 0 until size) {
              bmp[x, y] =
                  if (bits[x, y]) android.graphics.Color.WHITE
                  else android.graphics.Color.TRANSPARENT
            }
          }
        }
      }

  // QR code card with animated size and content
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .size(animatedHeight, animatedHeight)
              .clickable { isExpanded = !isExpanded }
              .testTag(MyEventsTestTags.QR_CODE_CARD),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.background))) {
        // Background with gradient and QR code image
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        // Background gradient colors
                        Brush.horizontalGradient(
                            listOf(
                                colorResource(id = R.color.qr_red).copy(alpha = 0.2f),
                                colorResource(id = R.color.qr_pink).copy(alpha = 0.2f),
                                colorResource(id = R.color.qr_purple).copy(alpha = 0.2f),
                                colorResource(id = R.color.qr_lilac).copy(alpha = 0.2f),
                                colorResource(id = R.color.qr_orange).copy(alpha = 0.2f),
                                colorResource(id = R.color.qr_yellow).copy(alpha = 0.2f)))),
            contentAlignment = Alignment.Center) {
              if (isExpanded) {
                // Show generated QR code when expanded
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier =
                        Modifier.size(screenHeight * 0.6f).graphicsLayer {
                          scaleX = qrScale
                          scaleY = qrScale
                        })
              } else {
                // Show placeholder icon when collapsed
                Image(
                    painter = painterResource(id = R.drawable.qr_code_icon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp))
              }
            }
      }
}

@Preview(showBackground = true)
@Composable
fun QrCodePreview() {
  OnePassTheme { QrCodeComponent(qrData = "QR-1234", modifier = Modifier.fillMaxWidth()) }
}
