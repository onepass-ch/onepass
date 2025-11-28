package ch.onepass.onepass.model.storage

import android.net.Uri

/**
 * Repository interface defining operations for managing image storage in Firebase Storage.
 *
 * This interface provides methods for uploading, downloading, and deleting image files to/from
 * Firebase Storage.
 */
interface StorageRepository {
  /**
   * Uploads an image file to Firebase Storage.
   *
   * The image will be validated for size and content type before upload.
   *
   * @param uri The local URI of the image to upload.
   * @param path The storage path where the image will be saved (e.g., "events/eventId/image.jpg").
   * @param onProgress Optional callback to track upload progress (0.0 to 1.0).
   * @return A [Result] containing the download URL of the uploaded image on success, or an error.
   * @throws IllegalArgumentException if the file size exceeds the maximum allowed size.
   */
  suspend fun uploadImage(
      uri: Uri,
      path: String,
      onProgress: ((Float) -> Unit)? = null
  ): Result<String>

  /**
   * Deletes an image from Firebase Storage.
   *
   * @param path The storage path of the image to delete (e.g., "events/eventId/image.jpg").
   * @return A [Result] indicating success or failure.
   */
  suspend fun deleteImage(path: String): Result<Unit>

  /**
   * Deletes an image from Firebase Storage using its download URL.
   *
   * @param downloadUrl The download URL of the image to delete.
   * @return A [Result] indicating success or failure.
   */
  suspend fun deleteImageByUrl(downloadUrl: String): Result<Unit>

  /**
   * Gets the download URL for an image at the specified path.
   *
   * @param path The storage path of the image.
   * @return A [Result] containing the download URL on success, or an error.
   */
  suspend fun getDownloadUrl(path: String): Result<String>

  /**
   * Deletes all images in a specific directory.
   *
   * @param directoryPath The storage directory path (e.g., "events/eventId/").
   * @return A [Result] containing the number of images deleted on success, or an error.
   */
  suspend fun deleteDirectory(directoryPath: String): Result<Int>
}
