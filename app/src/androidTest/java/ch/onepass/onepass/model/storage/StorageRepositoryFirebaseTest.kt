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
    assertTrue("Download URL should contain encoded path", downloadUrl?.contains(encodedPath) == true)

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
  fun deleteDirectory_withMultipleImages_deletesAll() = runTest {
    val directoryPath = "test/directory-${System.currentTimeMillis()}"

    // Upload multiple images to the directory
    val file1 = createTestImageFile()
    val file2 = createTestImageFile()
    val file3 = createTestImageFile()

    val path1 = "$directoryPath/image1.jpg"
    val path2 = "$directoryPath/image2.jpg"
    val path3 = "$directoryPath/image3.jpg"

    repository.uploadImage(Uri.fromFile(file1), path1)
    repository.uploadImage(Uri.fromFile(file2), path2)
    repository.uploadImage(Uri.fromFile(file3), path3)

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

  /**
   * Creates a temporary test image file for testing uploads.
   *
   * @return A temporary File containing test image data.
   */
  private fun createTestImageFile(): File {
    val file = File(context.cacheDir, "test-image-${System.currentTimeMillis()}.jpg")
    // Create a minimal JPEG file (1x1 pixel)
    val jpegHeader =
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
    file.writeBytes(jpegHeader)
    return file
  }
}
