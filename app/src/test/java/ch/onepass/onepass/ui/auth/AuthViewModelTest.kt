package ch.onepass.onepass.ui.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import ch.onepass.onepass.model.auth.AuthRepositoryFirebase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AuthViewModelTest {

  private lateinit var viewModel: AuthViewModel
  private lateinit var mockAuthRepository: AuthRepositoryFirebase
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockContext: Context

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockAuthRepository = mockk(relaxed = true)
    mockCredentialManager = mockk()
    mockContext = mockk(relaxed = true)
    viewModel = AuthViewModel(mockAuthRepository)

    every { mockContext.getString(any()) } returns "test_client_id"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `signIn success updates uiState correctly`() = runTest {
    // Given
    val mockGoogleIdOption = mockk<GetSignInWithGoogleOption>()
    val mockCredentialResponse = mockk<GetCredentialResponse>()
    val mockFirebaseUser = mockk<FirebaseUser>()
    val mockCredential = mockk<Credential>()

    every { mockCredentialResponse.credential } returns mockCredential
    coEvery { mockAuthRepository.getGoogleSignInOption(any()) } returns mockGoogleIdOption
    coEvery { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) } returns
        mockCredentialResponse
    coEvery { mockAuthRepository.signInWithGoogle(any()) } returns Result.success(mockFirebaseUser)

    // When
    viewModel.signIn(mockContext, mockCredentialManager)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    val uiState = viewModel.uiState.value
    assertTrue(uiState.isSignedIn)
    assertFalse(uiState.isLoading)
  }

  @Test
  fun `signIn failure updates uiState with error`() = runTest {
    // Given
    val errorMessage = "Sign in failed"
    val mockGoogleIdOption = mockk<GetSignInWithGoogleOption>()
    val mockException = mockk<GetCredentialException>()
    every { mockException.message } returns errorMessage

    coEvery { mockAuthRepository.getGoogleSignInOption(any()) } returns mockGoogleIdOption
    coEvery { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
        mockException

    // When
    viewModel.signIn(mockContext, mockCredentialManager)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    val uiState = viewModel.uiState.value
    assertFalse(uiState.isSignedIn)
    assertFalse(uiState.isLoading)
    assertNotNull(uiState.errorMessage)
    assertTrue(uiState.errorMessage!!.contains(errorMessage))
  }

  @Test
  fun `signOut updates uiState correctly`() = runTest {
    // When
    viewModel.signOut()
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify { mockAuthRepository.signOut() }
    val uiState = viewModel.uiState.value
    assertFalse(uiState.isSignedIn)
  }
}
