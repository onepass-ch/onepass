package ch.onepass.onepass.ui.components.buttons

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R

/**
 * Upload image button component that triggers the gallery/photo picker when clicked.
 *
 * This button uses Android's Photo Picker API to allow users to select images from their gallery.
 * The selected image URI is returned via the [onImageSelected] callback, which can then be
 * uploaded to storage using [ch.onepass.onepass.model.storage.StorageRepository].
 *
 * ## Complete Usage Example for Organization Profile Image:
 * ```kotlin
 * // In your ViewModel:
 * fun uploadProfileImage(uri: Uri, organizationId: String) {
 *     viewModelScope.launch {
 *         // 1. Upload image to Firebase Storage
 *         val result = storageRepository.uploadImage(
 *             uri = uri,
 *             path = "organizations/$organizationId/profile.jpg"
 *         )
 *
 *         result.onSuccess { downloadUrl ->
 *             // 2. Update organization with the image URL
 *             organizationRepository.updateProfileImage(organizationId, downloadUrl)
 *         }
 *     }
 * }
 *
 * // In your Composable:
 * UploadImageButton(
 *     onImageSelected = { uri ->
 *         viewModel.uploadProfileImage(uri, organizationId)
 *     },
 *     buttonText = "Upload Profile Picture"
 * )
 * ```
 *
 * ## Complete Usage Example for Event Images:
 * ```kotlin
 * // In your ViewModel:
 * fun uploadEventImage(uri: Uri, eventId: String) {
 *     viewModelScope.launch {
 *         // 1. Upload image to Firebase Storage
 *         val timestamp = System.currentTimeMillis()
 *         val result = storageRepository.uploadImage(
 *             uri = uri,
 *             path = "events/$eventId/images/$timestamp.jpg"
 *         )
 *
 *         result.onSuccess { downloadUrl ->
 *             // 2. Add image URL to event's images list
 *             eventRepository.addEventImage(eventId, downloadUrl)
 *         }
 *     }
 * }
 *
 * // In your Composable:
 * UploadImageButton(
 *     onImageSelected = { uri ->
 *         viewModel.uploadEventImage(uri, eventId)
 *     },
 *     buttonText = "Add Event Image"
 * )
 * ```
 *
 * @param onImageSelected Callback invoked when an image is selected, providing the image URI.
 * @param modifier Optional modifier for customization.
 * @param buttonText Text to display on the button (default: "Upload Image").
 * @param icon Icon to display on the button (default: Image icon).
 * @param enabled Whether the button is enabled (default: true).
 * @param testTag Test tag for UI testing (default: "uploadImageButton").
 */
@Composable
fun UploadImageButton(
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "Upload Image",
    icon: ImageVector = Icons.Default.Image,
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

  Button(
      onClick = {
        // Launch the photo picker with image-only filter
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
      },
      enabled = enabled,
      modifier = modifier.testTag(testTag),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = colorResource(id = R.color.primary),
              contentColor = Color.White,
              disabledContainerColor = Color.Gray,
              disabledContentColor = Color.White.copy(alpha = 0.6f)),
      shape = RoundedCornerShape(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
              Icon(
                  imageVector = icon,
                  contentDescription = "Upload image icon",
                  modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = buttonText, style = MaterialTheme.typography.bodyMedium)
            }
      }
}

/**
 * Compact circular icon button variant for uploading images.
 *
 * This is a more compact version of [UploadImageButton] that displays only an icon
 * in a circular button. Useful for inline editing or constrained spaces.
 *
 * @param onImageSelected Callback invoked when an image is selected, providing the image URI.
 * @param modifier Optional modifier for customization.
 * @param icon Icon to display (default: Add icon).
 * @param backgroundColor Background color of the button.
 * @param iconTint Color of the icon.
 * @param enabled Whether the button is enabled (default: true).
 * @param testTag Test tag for UI testing (default: "uploadImageIconButton").
 */
@Composable
fun UploadImageIconButton(
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    backgroundColor: Color = Color.Black.copy(alpha = 0.7f),
    iconTint: Color = Color.White,
    enabled: Boolean = true,
    testTag: String = "uploadImageIconButton"
) {
  // Photo picker launcher that handles single image selection
  val photoPickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickVisualMedia(),
          onResult = { uri ->
            // If a URI is returned (user didn't cancel), invoke the callback
            uri?.let { onImageSelected(it) }
          })

  IconButton(
      onClick = {
        // Launch the photo picker with image-only filter
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
      },
      enabled = enabled,
      modifier =
          modifier
              .size(48.dp)
              .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
              .testTag(testTag)) {
        Icon(
            imageVector = icon,
            contentDescription = if (enabled) "Upload image" else "Upload disabled",
            tint = iconTint,
            modifier = Modifier.size(24.dp))
      }
}

