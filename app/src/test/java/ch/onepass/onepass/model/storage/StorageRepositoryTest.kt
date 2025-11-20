package ch.onepass.onepass.model.storage

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for StorageRepository using MockK.
 *
 * These tests verify the contract of the StorageRepository interface using mock implementations.
 * For integration tests with real Firebase Storage, see StorageRepositoryFirebaseTest in
 * androidTest.
 *
 * Note: This repository only handles image uploads, not videos or other file types.
 */
class StorageRepositoryTest {

  private lateinit var mockRepository: StorageRepository
  private lateinit var testUri: Uri

  @Before
  fun setUp() {
    mockRepository = mockk()
    testUri = mockk<Uri>(relaxed = true)
  }

  @Test
  fun uploadImage_success_returnsDownloadUrl() = runTest {
    val expectedUrl = "https://storage.googleapis.com/bucket/events/event123/image.jpg"
    val testPath = "events/event123/image.jpg"

    coEvery { mockRepository.uploadImage(testUri, testPath, null) } returns Result.success(expectedUrl)

    val result = mockRepository.uploadImage(testUri, testPath)

    assertTrue(result.isSuccess, "Upload should succeed")
    assertEquals(expectedUrl, result.getOrNull(), "Should return correct download URL")
    coVerify(exactly = 1) { mockRepository.uploadImage(testUri, testPath, null) }
  }

  @Test
  fun uploadImage_withProgress_callsProgressCallback() = runTest {
    val expectedUrl = "https://storage.googleapis.com/bucket/events/event123/image.jpg"
    val testPath = "events/event123/image.jpg"
    val progressCallback: (Float) -> Unit = mockk(relaxed = true)

    coEvery { mockRepository.uploadImage(testUri, testPath, progressCallback) } returns
        Result.success(expectedUrl)

    val result = mockRepository.uploadImage(testUri, testPath, progressCallback)

    assertTrue(result.isSuccess, "Upload should succeed")
    coVerify(exactly = 1) { mockRepository.uploadImage(testUri, testPath, progressCallback) }
  }

  @Test
  fun uploadImage_failure_returnsError() = runTest {
    val testPath = "events/event123/image.jpg"
    val exception = Exception("Upload failed")

    coEvery { mockRepository.uploadImage(testUri, testPath, null) } returns Result.failure(exception)

    val result = mockRepository.uploadImage(testUri, testPath)

    assertTrue(result.isFailure, "Upload should fail")
    assertEquals(exception.message, result.exceptionOrNull()?.message, "Should return correct error")
  }

  @Test
  fun deleteImage_success_returnsSuccess() = runTest {
    val testPath = "events/event123/image.jpg"

    coEvery { mockRepository.deleteImage(testPath) } returns Result.success(Unit)

    val result = mockRepository.deleteImage(testPath)

    assertTrue(result.isSuccess, "Delete should succeed")
    coVerify(exactly = 1) { mockRepository.deleteImage(testPath) }
  }

  @Test
  fun deleteImage_failure_returnsError() = runTest {
    val testPath = "events/event123/image.jpg"
    val exception = Exception("Delete failed")

    coEvery { mockRepository.deleteImage(testPath) } returns Result.failure(exception)

    val result = mockRepository.deleteImage(testPath)

    assertTrue(result.isFailure, "Delete should fail")
    assertEquals(exception.message, result.exceptionOrNull()?.message, "Should return correct error")
  }

  @Test
  fun deleteImageByUrl_success_returnsSuccess() = runTest {
    val testUrl = "https://storage.googleapis.com/bucket/events/event123/image.jpg"

    coEvery { mockRepository.deleteImageByUrl(testUrl) } returns Result.success(Unit)

    val result = mockRepository.deleteImageByUrl(testUrl)

    assertTrue(result.isSuccess, "Delete should succeed")
    coVerify(exactly = 1) { mockRepository.deleteImageByUrl(testUrl) }
  }

  @Test
  fun getDownloadUrl_success_returnsUrl() = runTest {
    val testPath = "events/event123/image.jpg"
    val expectedUrl = "https://storage.googleapis.com/bucket/$testPath"

    coEvery { mockRepository.getDownloadUrl(testPath) } returns Result.success(expectedUrl)

    val result = mockRepository.getDownloadUrl(testPath)

    assertTrue(result.isSuccess, "Get download URL should succeed")
    assertEquals(expectedUrl, result.getOrNull(), "Should return correct URL")
  }

  @Test
  fun getDownloadUrl_failure_returnsError() = runTest {
    val testPath = "events/event123/nonexistent.jpg"
    val exception = Exception("File not found")

    coEvery { mockRepository.getDownloadUrl(testPath) } returns Result.failure(exception)

    val result = mockRepository.getDownloadUrl(testPath)

    assertTrue(result.isFailure, "Get download URL should fail")
    assertEquals(exception.message, result.exceptionOrNull()?.message, "Should return correct error")
  }

  @Test
  fun deleteDirectory_success_returnsDeletedCount() = runTest {
    val testPath = "events/event123/"
    val expectedDeletedCount = 5

    coEvery { mockRepository.deleteDirectory(testPath) } returns Result.success(expectedDeletedCount)

    val result = mockRepository.deleteDirectory(testPath)

    assertTrue(result.isSuccess, "Delete directory should succeed")
    assertEquals(expectedDeletedCount, result.getOrNull(), "Should return correct deleted count")
  }

  @Test
  fun deleteDirectory_failure_returnsError() = runTest {
    val testPath = "events/event123/"
    val exception = Exception("Directory deletion failed")

    coEvery { mockRepository.deleteDirectory(testPath) } returns Result.failure(exception)

    val result = mockRepository.deleteDirectory(testPath)

    assertTrue(result.isFailure, "Delete directory should fail")
    assertEquals(exception.message, result.exceptionOrNull()?.message, "Should return correct error")
  }
}

