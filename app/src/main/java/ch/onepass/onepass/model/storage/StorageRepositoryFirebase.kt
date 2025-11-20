package ch.onepass.onepass.model.storage

import android.net.Uri
import ch.onepass.onepass.model.storage.StorageRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import java.io.File
import kotlinx.coroutines.tasks.await

/**
 * Firebase Storage implementation of [StorageRepository].
 *
 * This implementation handles uploading, downloading, and deleting image files from Firebase
 * Storage.
 */
class StorageRepositoryFirebase(
    private val storage: FirebaseStorage = Firebase.storage
) : StorageRepository {
  private val storageRef = storage.reference

  companion object {
    /** Maximum file size in bytes (10MB) - reasonable limit for images */
    private const val MAX_FILE_SIZE = 10 * 1024 * 1024L

    /** Supported image MIME types */
    private val SUPPORTED_IMAGE_TYPES =
        setOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp")
  }

  /**
   * Uploads an image file to Firebase Storage.
   *
   * Validates file size before upload and sets appropriate metadata.
   *
   * @param uri The local URI of the image to upload.
   * @param path The storage path where the image will be saved.
   * @param onProgress Optional callback to track upload progress (0.0 to 1.0).
   * @return A [Result] containing the download URL of the uploaded image on success, or an error.
   */
  override suspend fun uploadImage(
      uri: Uri,
      path: String,
      onProgress: ((Float) -> Unit)?
  ): Result<String> = runCatching {
    // Validate file size
    val fileSize = getFileSize(uri)
    if (fileSize > MAX_FILE_SIZE) {
      return Result.failure(
          IllegalArgumentException(
              "Image size (${fileSize / 1024 / 1024}MB) exceeds maximum allowed size (${MAX_FILE_SIZE / 1024 / 1024}MB)"))
    }

    // Determine content type
    val contentType = getImageContentType(uri.toString())

    // Validate content type
    if (contentType !in SUPPORTED_IMAGE_TYPES) {
      return Result.failure(
          IllegalArgumentException("Unsupported image type: $contentType. Supported types: $SUPPORTED_IMAGE_TYPES"))
    }

    val fileRef = storageRef.child(path)

    // Create metadata for the image
    val metadata =
        StorageMetadata.Builder()
            .setContentType(contentType)
            .setCacheControl("public, max-age=31536000")
            .build()

    // Create upload task
    val uploadTask = fileRef.putFile(uri, metadata)

    // Add progress listener if provided (register before await)
    onProgress?.let { progressCallback ->
      uploadTask.addOnProgressListener { taskSnapshot ->
        val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
        progressCallback(progress)
      }
    }

    // Wait for upload to complete
    uploadTask.await()

    // Get and return the download URL
    fileRef.downloadUrl.await().toString()
  }

  /**
   * Deletes an image from Firebase Storage.
   *
   * @param path The storage path of the image to delete.
   * @return A [Result] indicating success or failure.
   */
  override suspend fun deleteImage(path: String): Result<Unit> = runCatching {
    val fileRef = storageRef.child(path)
    fileRef.delete().await()
  }

  /**
   * Deletes an image from Firebase Storage using its download URL.
   *
   * @param downloadUrl The download URL of the image to delete.
   * @return A [Result] indicating success or failure.
   */
  override suspend fun deleteImageByUrl(downloadUrl: String): Result<Unit> = runCatching {
    val fileRef = storage.getReferenceFromUrl(downloadUrl)
    fileRef.delete().await()
  }

  /**
   * Gets the download URL for an image at the specified path.
   *
   * @param path The storage path of the image.
   * @return A [Result] containing the download URL on success, or an error.
   */
  override suspend fun getDownloadUrl(path: String): Result<String> = runCatching {
    val fileRef = storageRef.child(path)
    fileRef.downloadUrl.await().toString()
  }

  /**
   * Deletes all images in a specific directory.
   *
   * Note: Firebase Storage doesn't support directory deletion directly. This method lists all
   * images in the directory and deletes them one by one.
   *
   * @param directoryPath The storage directory path.
   * @return A [Result] containing the number of images deleted on success, or an error.
   */
  override suspend fun deleteDirectory(directoryPath: String): Result<Int> = runCatching {
    val dirRef = storageRef.child(directoryPath)
    val listResult = dirRef.listAll().await()

    var deletedCount = 0

    // Delete all images in the directory
    listResult.items.forEach { item ->
      item.delete().await()
      deletedCount++
    }

    // Recursively delete subdirectories
    listResult.prefixes.forEach { prefix ->
      val subdirPath = prefix.path
      val subdirResult = deleteDirectory(subdirPath)
      subdirResult.getOrNull()?.let { deletedCount += it }
    }

    deletedCount
  }

  /**
   * Gets the file size from a URI.
   *
   * @param uri The URI of the file.
   * @return The file size in bytes.
   * @throws IllegalArgumentException if the file size cannot be determined.
   */
  private fun getFileSize(uri: Uri): Long {
    return when (uri.scheme) {
      "file" -> {
        // For file:// URIs
        val file = File(uri.path ?: throw IllegalArgumentException("Invalid file path"))
        if (!file.exists()) {
          throw IllegalArgumentException("File does not exist: ${uri.path}")
        }
        file.length()
      }
      "content" -> {
        // For content:// URIs, we'll need to query the content resolver
        // This will be handled at a higher level that has access to Context
        // For now, assume a reasonable default or throw
        throw IllegalArgumentException(
            "Cannot determine file size for content:// URI without Context. " +
                "Please implement size checking at the ViewModel/UI layer.")
      }
      else -> throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
    }
  }

  /**
   * Determines the image content type based on the file extension.
   *
   * @param uriString The URI string of the image.
   * @return The MIME type of the image.
   */
  private fun getImageContentType(uriString: String): String {
    return when {
      uriString.endsWith(".jpg", ignoreCase = true) ||
          uriString.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
      uriString.endsWith(".png", ignoreCase = true) -> "image/png"
      uriString.endsWith(".gif", ignoreCase = true) -> "image/gif"
      uriString.endsWith(".webp", ignoreCase = true) -> "image/webp"
      else -> "image/jpeg" // Default to JPEG
    }
  }
}

