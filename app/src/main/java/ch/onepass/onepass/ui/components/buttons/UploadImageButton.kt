package ch.onepass.onepass.ui.components.buttons

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                    color = colorScheme.onBackground, textAlign = TextAlign.Center),
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
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.onBackground,
                    disabledContainerColor = colorScheme.surface.copy(alpha = 0.5f),
                    disabledContentColor = colorScheme.onBackground.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)) {
              Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center,
                  modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload image icon",
                        modifier = Modifier.size(32.dp),
                        tint = colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Upload Image",
                        fontSize = 10.sp,
                        style =
                            TextStyle(
                                textDecoration = TextDecoration.Underline,
                                color = colorScheme.onSurface))
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
