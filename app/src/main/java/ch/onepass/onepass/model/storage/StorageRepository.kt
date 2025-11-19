package ch.onepass.onepass.model.storage

import android.net.Uri

/**
 * Repository interface defining operations for managing media storage in Firebase Storage.
 *
 * This interface provides methods for uploading, downloading, and deleting media files
 * (images, videos, etc.) to/from Firebase Storage.
 */
interface StorageRepository {
  /**
   * Uploads an image file to Firebase Storage.
   *
   * @param uri The local URI of the image to upload.
   * @param path The storage path where the image will be saved (e.g., "events/eventId/image.jpg").
   * @param onProgress Optional callback to track upload progress (0.0 to 1.0).
   * @return A [Result] containing the download URL of the uploaded image on success, or an error.
   */
  suspend fun uploadImage(
      uri: Uri,
      path: String,
      onProgress: ((Float) -> Unit)? = null
  ): Result<String>

  /**
   * Uploads a video file to Firebase Storage.
   *
   * @param uri The local URI of the video to upload.
   * @param path The storage path where the video will be saved (e.g., "events/eventId/video.mp4").
   * @param onProgress Optional callback to track upload progress (0.0 to 1.0).
   * @return A [Result] containing the download URL of the uploaded video on success, or an error.
   */
  suspend fun uploadVideo(
      uri: Uri,
      path: String,
      onProgress: ((Float) -> Unit)? = null
  ): Result<String>

  /**
   * Uploads any media file to Firebase Storage.
   *
   * This is a generic method that can handle any file type (images, videos, documents, etc.).
   *
   * @param uri The local URI of the file to upload.
   * @param path The storage path where the file will be saved.
   * @param contentType The MIME type of the file (e.g., "image/jpeg", "video/mp4").
   * @param onProgress Optional callback to track upload progress (0.0 to 1.0).
   * @return A [Result] containing the download URL of the uploaded file on success, or an error.
   */
  suspend fun uploadFile(
      uri: Uri,
      path: String,
      contentType: String,
      onProgress: ((Float) -> Unit)? = null
  ): Result<String>

  /**
   * Deletes a file from Firebase Storage.
   *
   * @param path The storage path of the file to delete (e.g., "events/eventId/image.jpg").
   * @return A [Result] indicating success or failure.
   */
  suspend fun deleteFile(path: String): Result<Unit>

  /**
   * Deletes a file from Firebase Storage using its download URL.
   *
   * @param downloadUrl The download URL of the file to delete.
   * @return A [Result] indicating success or failure.
   */
  suspend fun deleteFileByUrl(downloadUrl: String): Result<Unit>

  /**
   * Gets the download URL for a file at the specified path.
   *
   * @param path The storage path of the file.
   * @return A [Result] containing the download URL on success, or an error.
   */
  suspend fun getDownloadUrl(path: String): Result<String>

  /**
   * Deletes all files in a specific directory.
   *
   * @param directoryPath The storage directory path (e.g., "events/eventId/").
   * @return A [Result] containing the number of files deleted on success, or an error.
   */
  suspend fun deleteDirectory(directoryPath: String): Result<Int>
}

