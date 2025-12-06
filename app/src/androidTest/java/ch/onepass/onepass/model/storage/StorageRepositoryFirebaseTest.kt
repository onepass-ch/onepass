package ch.onepass.onepass.model.storage

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import ch.onepass.onepass.utils.FirebaseEmulator
import java.io.File
import java.net.URLEncoder
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for StorageRepositoryFirebase using Firebase Emulator.
 *
 * These tests verify image upload, download, and delete operations against a real Firebase Storage
 * instance running in the emulator.
 *
 * Prerequisites:
 * - Firebase emulators must be running (firebase emulators:start)
 * - Test uses anonymous authentication
 *
 * Note: These tests create temporary image files for testing upload functionality.
 */
class StorageRepositoryFirebaseTest {

  private lateinit var repository: StorageRepository
  private lateinit var context: Context
  private val uploadedFiles = mutableListOf<String>()

  @Before
  fun setUp() {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running for Storage tests. " +
          "Start emulators with: firebase emulators:start"
    }

    context = ApplicationProvider.getApplicationContext()
    repository = StorageRepositoryFirebase()

    runTest {
      // Sign in anonymously for testing
      FirebaseEmulator.auth.signInAnonymously().await()
    }
  }

  @After
  fun tearDown() {
    runTest {
      // Clean up all uploaded files
      uploadedFiles.forEach { path ->
        try {
          repository.deleteImage(path)
        } catch (e: Exception) {
          // Ignore cleanup errors
        }
      }
      uploadedFiles.clear()

      // Sign out
      FirebaseEmulator.auth.signOut()
    }
  }

  @Test
  fun uploadImage_success_returnsDownloadUrl() = runTest {
    // Create a temporary test image file
    val testFile = createTestImageFile()
    val testUri = Uri.fromFile(testFile)
    val testPath = "test/images/test-image-${System.currentTimeMillis()}.jpg"

    val result = repository.uploadImage(testUri, testPath)

    assertTrue("Upload should succeed", result.isSuccess)
    val downloadUrl = result.getOrNull()
    assertNotNull("Download URL should not be null", downloadUrl)
    // Firebase Storage URLs encode the path with %2F for slashes
    val encodedPath = URLEncoder.encode(testPath, "UTF-8")
    assertTrue(
        "Download URL should contain encoded path", downloadUrl?.contains(encodedPath) == true)

    uploadedFiles.add(testPath)

    // Clean up test file
    testFile.delete()
  }

  @Test
  fun uploadImage_withProgress_tracksProgress() = runTest {
    val testFile = createTestImageFile()
    val testUri = Uri.fromFile(testFile)
    val testPath = "test/images/test-image-progress-${System.currentTimeMillis()}.jpg"

    val progressValues = mutableListOf<Float>()
    val result =
        repository.uploadImage(testUri, testPath) { progress -> progressValues.add(progress) }

    assertTrue("Upload should succeed", result.isSuccess)
    assertNotNull("Download URL should not be null", result.getOrNull())

    // Note: For small files, progress callback might not be called or might jump to 1.0 quickly
    // We just verify that if it's called, values are between 0 and 1
    if (progressValues.isNotEmpty()) {
      assertTrue("Progress values should be between 0 and 1", progressValues.all { it in 0f..1f })
    }

    uploadedFiles.add(testPath)
    testFile.delete()
  }

  @Test
  fun getDownloadUrl_existingFile_returnsUrl() = runTest {
    // First upload a file
    val testFile = createTestImageFile()
    val testUri = Uri.fromFile(testFile)
    val testPath = "test/images/test-image-url-${System.currentTimeMillis()}.jpg"

    val uploadResult = repository.uploadImage(testUri, testPath)
    assertTrue("Upload should succeed", uploadResult.isSuccess)
    uploadedFiles.add(testPath)

    // Then get its download URL
    val result = repository.getDownloadUrl(testPath)

    assertTrue("Get download URL should succeed", result.isSuccess)
    val downloadUrl = result.getOrNull()
    assertNotNull("Download URL should not be null", downloadUrl)
    assertTrue("Download URL should be valid", downloadUrl?.startsWith("http") == true)

    testFile.delete()
  }

  @Test
  fun getDownloadUrl_nonExistentFile_returnsError() = runTest {
    val nonExistentPath = "test/images/non-existent-${System.currentTimeMillis()}.jpg"

    val result = repository.getDownloadUrl(nonExistentPath)

    assertTrue("Get download URL should fail", result.isFailure)
    assertNotNull("Error should not be null", result.exceptionOrNull())
  }

  @Test
  fun deleteImage_existingFile_success() = runTest {
    // First upload an image
    val testFile = createTestImageFile()
    val testUri = Uri.fromFile(testFile)
    val testPath = "test/images/test-image-delete-${System.currentTimeMillis()}.jpg"

    val uploadResult = repository.uploadImage(testUri, testPath)
    assertTrue("Upload should succeed", uploadResult.isSuccess)

    // Then delete it
    val deleteResult = repository.deleteImage(testPath)

    assertTrue("Delete should succeed", deleteResult.isSuccess)

    // Verify image is deleted by trying to get its download URL
    val getUrlResult = repository.getDownloadUrl(testPath)
    assertTrue("Get URL should fail after deletion", getUrlResult.isFailure)

    testFile.delete()
  }

  @Test
  fun deleteImageByUrl_existingFile_success() = runTest {
    // First upload an image
    val testFile = createTestImageFile()
    val testUri = Uri.fromFile(testFile)
    val testPath = "test/images/test-image-delete-url-${System.currentTimeMillis()}.jpg"

    val uploadResult = repository.uploadImage(testUri, testPath)
    assertTrue("Upload should succeed", uploadResult.isSuccess)
    val downloadUrl = uploadResult.getOrNull()!!

    // Then delete it by URL
    val deleteResult = repository.deleteImageByUrl(downloadUrl)

    assertTrue("Delete should succeed", deleteResult.isSuccess)

    // Verify image is deleted
    val getUrlResult = repository.getDownloadUrl(testPath)
    assertTrue("Get URL should fail after deletion", getUrlResult.isFailure)

    testFile.delete()
  }

  @Test
  fun deleteDirectory_withMultipleImages_deletesAll() =
      runTest(timeout = 30.seconds) {
        val directoryPath = "test/directory-${System.currentTimeMillis()}"

        // Upload multiple images to the directory
        val file1 = createTestImageFile()
        val file2 = createTestImageFile()
        val file3 = createTestImageFile()

        val path1 = "$directoryPath/image1.jpg"
        val path2 = "$directoryPath/image2.jpg"
        val path3 = "$directoryPath/image3.jpg"

        // Await all uploads and verify they succeeded
        val upload1 = repository.uploadImage(Uri.fromFile(file1), path1)
        assertTrue("Upload 1 should succeed", upload1.isSuccess)

        val upload2 = repository.uploadImage(Uri.fromFile(file2), path2)
        assertTrue("Upload 2 should succeed", upload2.isSuccess)

        val upload3 = repository.uploadImage(Uri.fromFile(file3), path3)
        assertTrue("Upload 3 should succeed", upload3.isSuccess)

        // Delete the entire directory
        val result = repository.deleteDirectory(directoryPath)

        assertTrue("Delete directory should succeed", result.isSuccess)
        val deletedCount = result.getOrNull()
        assertEquals("Should delete 3 images", 3, deletedCount)

        // Verify all images are deleted
        assertTrue("Image 1 should be deleted", repository.getDownloadUrl(path1).isFailure)
        assertTrue("Image 2 should be deleted", repository.getDownloadUrl(path2).isFailure)
        assertTrue("Image 3 should be deleted", repository.getDownloadUrl(path3).isFailure)

        // Clean up test files
        file1.delete()
        file2.delete()
        file3.delete()
      }

  @Test
  fun uploadMultipleImages_success() = runTest {
    val testFile1 = createTestImageFile()
    val testFile2 = createTestImageFile()

    val testUri1 = Uri.fromFile(testFile1)
    val testUri2 = Uri.fromFile(testFile2)

    val testPath1 = "test/images/multi-1-${System.currentTimeMillis()}.jpg"
    val testPath2 = "test/images/multi-2-${System.currentTimeMillis()}.jpg"

    val result1 = repository.uploadImage(testUri1, testPath1)
    val result2 = repository.uploadImage(testUri2, testPath2)

    assertTrue("First upload should succeed", result1.isSuccess)
    assertTrue("Second upload should succeed", result2.isSuccess)

    assertNotNull("First download URL should not be null", result1.getOrNull())
    assertNotNull("Second download URL should not be null", result2.getOrNull())

    uploadedFiles.add(testPath1)
    uploadedFiles.add(testPath2)

    testFile1.delete()
    testFile2.delete()
  }

  @Test
  fun uploadImage_fileTooLarge_returnsError() = runTest {
    // Create a file that exceeds the 10MB limit
    val largeFile = File(context.cacheDir, "large-image-${System.currentTimeMillis()}.jpg")
    // Write 11MB of data (10MB limit + 1MB)
    val largeData = ByteArray(11 * 1024 * 1024)
    largeFile.writeBytes(largeData)

    val testUri = Uri.fromFile(largeFile)
    val testPath = "test/images/large-image-${System.currentTimeMillis()}.jpg"

    val result = repository.uploadImage(testUri, testPath)

    assertTrue("Upload should fail for oversized file", result.isFailure)
    assertTrue(
        "Should throw IllegalArgumentException",
        result.exceptionOrNull() is IllegalArgumentException)
    assertTrue(
        "Error message should mention size limit",
        result.exceptionOrNull()?.message?.contains("exceeds maximum allowed size") == true)

    // Clean up
    largeFile.delete()
  }

  @Test
  fun uploadImage_pngFormat_success() = runTest {
    val testFile = createTestImageFile("png")
    val testUri = Uri.fromFile(testFile)
    val testPath = "test/images/test-image-${System.currentTimeMillis()}.png"

    val result = repository.uploadImage(testUri, testPath)

    assertTrue("Upload should succeed for PNG", result.isSuccess)
    assertNotNull("Download URL should not be null", result.getOrNull())

    uploadedFiles.add(testPath)
    testFile.delete()
  }

  @Test
  fun uploadImage_webpFormat_success() = runTest {
    val testFile = createTestImageFile("webp")
    val testUri = Uri.fromFile(testFile)
    val testPath = "test/images/test-image-${System.currentTimeMillis()}.webp"

    val result = repository.uploadImage(testUri, testPath)

    assertTrue("Upload should succeed for WEBP", result.isSuccess)
    assertNotNull("Download URL should not be null", result.getOrNull())

    uploadedFiles.add(testPath)
    testFile.delete()
  }

  @Test
  fun uploadImage_gifFormat_success() = runTest {
    val testFile = createTestImageFile("gif")
    val testUri = Uri.fromFile(testFile)
    val testPath = "test/images/test-image-${System.currentTimeMillis()}.gif"

    val result = repository.uploadImage(testUri, testPath)

    assertTrue("Upload should succeed for GIF", result.isSuccess)
    assertNotNull("Download URL should not be null", result.getOrNull())

    uploadedFiles.add(testPath)
    testFile.delete()
  }

  @Test
  fun uploadImage_nonExistentFile_returnsError() = runTest {
    // Create a URI to a file that doesn't exist
    val nonExistentFile = File(context.cacheDir, "non-existent-${System.currentTimeMillis()}.jpg")
    val testUri = Uri.fromFile(nonExistentFile)
    val testPath = "test/images/non-existent-${System.currentTimeMillis()}.jpg"

    val result = repository.uploadImage(testUri, testPath)

    assertTrue("Upload should fail for non-existent file", result.isFailure)
    assertTrue(
        "Should throw IllegalArgumentException",
        result.exceptionOrNull() is IllegalArgumentException)
    assertTrue(
        "Error message should mention file doesn't exist",
        result.exceptionOrNull()?.message?.contains("does not exist") == true)
  }

  /**
   * Creates a temporary test image file for testing uploads.
   *
   * @param extension The file extension (jpg, png, gif, webp)
   * @return A temporary File containing test image data.
   */
  private fun createTestImageFile(extension: String = "jpg"): File {
    val file = File(context.cacheDir, "test-image-${System.currentTimeMillis()}.$extension")

    // Create minimal valid image data based on format
    val imageData =
        when (extension.lowercase()) {
          "jpg",
          "jpeg" -> {
            // Minimal JPEG file (1x1 pixel)
            byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(),
                0xFF.toByte(),
                0xE0.toByte(),
                0x00,
                0x10,
                0x4A,
                0x46,
                0x49,
                0x46,
                0x00,
                0x01,
                0x01,
                0x00,
                0x00,
                0x01,
                0x00,
                0x01,
                0x00,
                0x00,
                0xFF.toByte(),
                0xD9.toByte())
          }
          "png" -> {
            // Minimal PNG file (1x1 pixel)
            byteArrayOf(
                0x89.toByte(),
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A,
                0x00,
                0x00,
                0x00,
                0x0D,
                0x49,
                0x48,
                0x44,
                0x52,
                0x00,
                0x00,
                0x00,
                0x01,
                0x00,
                0x00,
                0x00,
                0x01,
                0x08,
                0x06,
                0x00,
                0x00,
                0x00,
                0x1F.toByte(),
                0x15.toByte(),
                0xC4.toByte(),
                0x89.toByte(),
                0x00,
                0x00,
                0x00,
                0x0A,
                0x49,
                0x44,
                0x41,
                0x54,
                0x78,
                0x9C.toByte(),
                0x63,
                0x00,
                0x01,
                0x00,
                0x00,
                0x05,
                0x00,
                0x01,
                0x0D,
                0x0A,
                0x2D,
                0xB4.toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                0x49,
                0x45,
                0x4E,
                0x44,
                0xAE.toByte(),
                0x42,
                0x60,
                0x82.toByte())
          }
          "gif" -> {
            // Minimal GIF file (1x1 pixel)
            byteArrayOf(
                0x47,
                0x49,
                0x46,
                0x38,
                0x39,
                0x61,
                0x01,
                0x00,
                0x01,
                0x00,
                0x80.toByte(),
                0x00,
                0x00,
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0x00,
                0x00,
                0x00,
                0x21.toByte(),
                0xF9.toByte(),
                0x04,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x2C,
                0x00,
                0x00,
                0x00,
                0x00,
                0x01,
                0x00,
                0x01,
                0x00,
                0x00,
                0x02,
                0x02,
                0x44,
                0x01,
                0x00,
                0x3B)
          }
          "webp" -> {
            // Minimal WEBP file (1x1 pixel)
            byteArrayOf(
                0x52,
                0x49,
                0x46,
                0x46,
                0x1A,
                0x00,
                0x00,
                0x00,
                0x57,
                0x45,
                0x42,
                0x50,
                0x56,
                0x50,
                0x38,
                0x20,
                0x0E,
                0x00,
                0x00,
                0x00,
                0x30,
                0x01,
                0x00,
                0x9D.toByte(),
                0x01,
                0x2A,
                0x01,
                0x00,
                0x01,
                0x00,
                0x35,
                0xA4.toByte(),
                0x00,
                0x03,
                0x70,
                0x00)
          }
          else -> throw IllegalArgumentException("Unsupported format: $extension")
        }

    file.writeBytes(imageData)
    return file
  }
}
