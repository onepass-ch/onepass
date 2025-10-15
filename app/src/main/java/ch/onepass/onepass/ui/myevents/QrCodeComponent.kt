package ch.onepass.onepass.ui.myevents

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
  var showQrDialog by remember { mutableStateOf(false) }

  // Generate QR Bitmap from provided data
  val qrBitmap: Bitmap =
      remember(qrData) {
        val size = 200
        val bits = QRCodeWriter().encode(qrData, BarcodeFormat.QR_CODE, size, size)
        createBitmap(size, size).also { bitmap ->
          for (x in 0 until size) {
            for (y in 0 until size) {
              bitmap[x, y] =
                  if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
          }
        }
      }

  if (showQrDialog) {
    Dialog(onDismissRequest = { showQrDialog = false }) {
      Card(
          shape = RoundedCornerShape(16.dp),
          colors =
              CardDefaults.cardColors(
                  containerColor = colorResource(id = R.color.surface_container)),
          modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(300.dp).padding(24.dp),
                contentAlignment = Alignment.Center) {
                  Image(
                      bitmap = qrBitmap.asImageBitmap(),
                      contentDescription = "QR Code Dialog",
                      modifier = Modifier.testTag(MyEventsTestTags.QR_CODE_DIALOG))
                }
          }
    }
  }

  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable { showQrDialog = true }
              .testTag(MyEventsTestTags.QR_CODE_ICON),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.background))) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                listOf(
                                    colorResource(id = R.color.qr_red).copy(alpha = 0.2f),
                                    colorResource(id = R.color.qr_pink).copy(alpha = 0.2f),
                                    colorResource(id = R.color.qr_purple).copy(alpha = 0.2f),
                                    colorResource(id = R.color.qr_lilac).copy(alpha = 0.2f),
                                    colorResource(id = R.color.qr_orange).copy(alpha = 0.2f),
                                    colorResource(id = R.color.qr_yellow).copy(alpha = 0.2f))),
                    ),
            contentAlignment = Alignment.Center) {
              Image(
                  painter = painterResource(id = R.drawable.qr_code_icon),
                  contentDescription = "QR Code Icon",
                  modifier = Modifier.size(40.dp).testTag(MyEventsTestTags.QR_CODE_ICON))
            }
      }
}

@Preview(showBackground = true)
@Composable
fun QrCodePreview() {
  OnePassTheme {
    QrCodeComponent(qrData = "QR-1234", modifier = Modifier.fillMaxWidth().height(100.dp))
  }
}
