package ch.onepass.onepass.model.storage

import android.net.Uri

/**
 * Fake implementation of [StorageRepository] for testing purposes.
 *
 * This fake repository simulates successful or failed image upload operations without actual
 * network calls, making it suitable for unit tests.
 *
 * @property shouldFailUpload If true, all upload operations will fail with an exception.
 */
class FakeStorageRepository : StorageRepository {
  var shouldFailUpload = false

  override suspend fun uploadImage(
      uri: Uri,
      path: String,
      onProgress: ((Float) -> Unit)?
  ): Result<String> {
    return if (shouldFailUpload) {
      Result.failure(Exception("Upload failed"))
    } else {
      // Return a fake download URL
      Result.success("https://fake-storage.com/$path")
    }
  }

  override suspend fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

  override suspend fun deleteImageByUrl(downloadUrl: String): Result<Unit> = Result.success(Unit)

  override suspend fun getDownloadUrl(path: String): Result<String> =
      Result.success("https://fake-storage.com/$path")

  override suspend fun deleteDirectory(directoryPath: String): Result<Int> = Result.success(0)
}
