package ch.onepass.onepass.ui.components.buttons

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.R

/**
 * Upload image button with full-width design.
 *
 * @param onImageSelected Callback invoked when an image is selected, providing the image URI.
 * @param modifier Optional modifier for customization.
 * @param enabled Whether the button is enabled (default: true).
 * @param testTag Test tag for UI testing (default: "uploadImageButton").
 */
@Composable
fun UploadImageButton(
    modifier: Modifier = Modifier,
    imageDescription: String = "Image*",
    onImageSelected: (Uri) -> Unit,
    enabled: Boolean = true,
    testTag: String = "uploadImageButton"
) {
  // Photo picker launcher that handles single image selection
  val photoPickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickVisualMedia(),
          onResult = { uri ->
            // If a URI is returned (user didn't cancel), invoke the callback
            uri?.let { onImageSelected(it) }
          })

  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally) {

        // Text label above the button
        Text(
            text = imageDescription,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = colorResource(id = R.color.white), textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth())

        OutlinedButton(
            onClick = {
              // Launch the photo picker with image-only filter
              photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            enabled = enabled,
            modifier = modifier.fillMaxWidth().height(125.dp).testTag(testTag),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF1C1C1C),
                    contentColor = Color(0xFFA3A3A3),
                    disabledContainerColor = Color(0xFF1C1C1C).copy(alpha = 0.5f),
                    disabledContentColor = Color(0xFFA3A3A3).copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Color(0xFF404040)),
            shape = RoundedCornerShape(8.dp)) {
              Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center,
                  modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload image icon",
                        modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Upload Image",
                        fontSize = 10.sp,
                        style = TextStyle(textDecoration = TextDecoration.Underline))
                  }
            }
      }
}

@Preview(showBackground = true, name = "Upload Image Button")
@Composable
private fun UploadImageButtonPreview() {
  MaterialTheme {
    Box(modifier = Modifier.padding(16.dp)) { UploadImageButton(onImageSelected = {}) }
  }
}
