package ch.onepass.onepass.ui.profile.editprofile

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.storage.StorageRepository
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.model.user.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockStorageRepository: StorageRepository
  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockContext: Context
  private lateinit var viewModel: EditProfileViewModel

  private val testUser =
      User(
          uid = "test-user-id",
          email = "test@example.com",
          displayName = "John Doe",
          phoneE164 = "+41791234567",
          country = "Switzerland",
          avatarUrl = "https://example.com/avatar.jpg")

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockUserRepository = mockk(relaxed = true)
    mockStorageRepository = mockk(relaxed = true)
    mockFirestore = mockk(relaxed = true)
    mockContext = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ========================================
  // Tests for Initialization
  // ========================================

  @Test
  fun initialization_setsDefaultCountryCode() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    val countryCode = viewModel.selectedCountryCode.value
    assertEquals("+41", countryCode)
  }

  @Test
  fun initialization_loadsCountryList() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    val countries = viewModel.countryList.value
    assertTrue(countries.isNotEmpty())
    assertTrue(countries.any { it.first == "Switzerland" && it.second == "41" })
  }

  @Test
  fun initialization_hasCorrectDefaultFormState() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    val formState = viewModel.formState.value
    assertEquals("", formState.displayName)
    assertEquals("", formState.phone)
    assertEquals("", formState.country)
    assertNull(formState.avatarUrl)
    assertNull(formState.avatarUri)
    assertEquals("", formState.initials)
  }

  @Test
  fun initialization_hasCorrectDefaultUiState() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertFalse(uiState.success)
    assertNull(uiState.errorMessage)
  }

  // ========================================
  // Tests for Loading Profile
  // ========================================

  @Test
  fun loadProfile_success_updatesFormState() = runTest {
    coEvery { mockUserRepository.getCurrentUser() } returns testUser
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.loadProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("John Doe", formState.displayName)
    assertEquals("791234567", formState.phone)
    assertEquals("Switzerland", formState.country)
    assertEquals("https://example.com/avatar.jpg", formState.avatarUrl)
    assertEquals("JD", formState.initials)
  }

  @Test
  fun loadProfile_clearsLoadingStateAfterSuccess() = runTest {
    coEvery { mockUserRepository.getCurrentUser() } returns testUser
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.loadProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
  }

  @Test
  fun loadProfile_repositoryThrowsException_setsErrorMessage() = runTest {
    coEvery { mockUserRepository.getCurrentUser() } throws RuntimeException("Network error")
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.loadProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertNotNull(uiState.errorMessage)
    assertTrue(uiState.errorMessage!!.contains("Network error"))
  }

  @Test
  fun loadProfile_extractsInitialsCorrectly_singleName() = runTest {
    val singleNameUser = testUser.copy(displayName = "John")
    coEvery { mockUserRepository.getCurrentUser() } returns singleNameUser
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.loadProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("J", formState.initials)
  }

  @Test
  fun loadProfile_extractsInitialsCorrectly_threeNames() = runTest {
    val threeNameUser = testUser.copy(displayName = "John Michael Doe")
    coEvery { mockUserRepository.getCurrentUser() } returns threeNameUser
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.loadProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("JM", formState.initials)
  }

  @Test
  fun loadProfile_emptyDisplayName_setsQuestionMarkInitials() = runTest {
    val emptyNameUser = testUser.copy(displayName = "")
    coEvery { mockUserRepository.getCurrentUser() } returns emptyNameUser
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.loadProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("?", formState.initials)
  }

  @Test
  fun loadProfile_extractsPhoneWithoutPrefix() = runTest {
    coEvery { mockUserRepository.getCurrentUser() } returns testUser
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.loadProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("791234567", formState.phone)
    assertFalse(formState.phone.startsWith("+"))
  }

  @Test
  fun loadProfile_nullPhone_setsEmptyPhoneString() = runTest {
    val noPhoneUser = testUser.copy(phoneE164 = null)
    coEvery { mockUserRepository.getCurrentUser() } returns noPhoneUser
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.loadProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("", formState.phone)
  }

  @Test
  fun loadProfile_nullCountry_setsEmptyCountryString() = runTest {
    val noCountryUser = testUser.copy(country = null)
    coEvery { mockUserRepository.getCurrentUser() } returns noCountryUser
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.loadProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("", formState.country)
  }

  // ========================================
  // Tests for Form Updates
  // ========================================

  @Test
  fun updateDisplayName_updatesFormState() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.updateDisplayName("Jane Smith")
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("Jane Smith", formState.displayName)
  }

  @Test
  fun updatePhone_updatesFormState() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.updatePhone("791234567")
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("791234567", formState.phone)
  }

  @Test
  fun updateCountry_updatesFormState() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.updateCountry("France")
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("France", formState.country)
  }

  @Test
  fun updateCountry_emptyString_acceptsEmpty() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.updateCountry("")
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("", formState.country)
  }

  @Test
  fun updateCountryIndex_updatesSelectedCountryCode() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    val countries = viewModel.countryList.value
    val franceIndex = countries.indexOfFirst { it.first == "France" }
    assertTrue(franceIndex >= 0)

    viewModel.updateCountryIndex(franceIndex)
    testDispatcher.scheduler.advanceUntilIdle()

    val countryCode = viewModel.selectedCountryCode.value
    assertEquals("+33", countryCode)
  }

  @Test
  fun updateCountryIndex_invalidIndex_usesDefaultCode() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.updateCountryIndex(99999)
    testDispatcher.scheduler.advanceUntilIdle()

    val countryCode = viewModel.selectedCountryCode.value
    assertEquals("+41", countryCode)
  }

  // ========================================
  // Tests for Avatar Management
  // ========================================

  @Test
  fun selectAvatarImage_updatesFormState() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)
    val testUri = mockk<Uri>()

    viewModel.selectAvatarImage(testUri)
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals(testUri, formState.avatarUri)
  }

  @Test
  fun removeAvatar_clearsAvatarData() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)
    val testUri = mockk<Uri>()
    viewModel.selectAvatarImage(testUri)

    viewModel.removeAvatar()
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertNull(formState.avatarUrl)
    assertNull(formState.avatarUri)
  }

  // ========================================
  // Tests for Saving Profile
  // ========================================

  @Test
  fun saveProfile_blankDisplayName_setsErrorMessage() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)
    viewModel.updateDisplayName("")

    viewModel.saveProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertNotNull(uiState.errorMessage)
    assertEquals("Name is required", uiState.errorMessage)
  }

  @Test
  fun clearError_clearsErrorMessage() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)
    viewModel.updateDisplayName("")
    viewModel.saveProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.clearError()
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertNull(uiState.errorMessage)
  }

  @Test
  fun updatePhone_emptyString_acceptsEmpty() = runTest {
    viewModel = EditProfileViewModel(mockUserRepository, mockStorageRepository, mockFirestore)

    viewModel.updatePhone("")
    testDispatcher.scheduler.advanceUntilIdle()

    val formState = viewModel.formState.value
    assertEquals("", formState.phone)
  }
}
