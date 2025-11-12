package ch.onepass.onepass.model.auth

import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthRepositoryFirebaseTest {

  @Test
  fun getGoogleSignInOption_includesProvidedServerClientId() {
    val repository = AuthRepositoryFirebase()
    val serverClientId = "client-id-123"

    val option = repository.getGoogleSignInOption(serverClientId)

    assertEquals(serverClientId, option.serverClientId)
  }

  @Test
  fun signInWithGoogle_returnsUser_whenCredentialIsValid() = runTest {
    val credentialData = Bundle()
    val credential = CustomCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, credentialData)
    val helper = mockk<GoogleSignInHelper>()
    val auth = mockk<FirebaseAuth>()
    val googleIdTokenCredential = mockk<GoogleIdTokenCredential>()
    val firebaseCredential = mockk<AuthCredential>()
    val authResult = mockk<AuthResult>()
    val firebaseUser = mockk<FirebaseUser>()
    val userRepository = mockk<UserRepositoryFirebase>()

    every { helper.extractIdTokenCredential(credentialData) } returns googleIdTokenCredential
    every { googleIdTokenCredential.idToken } returns "id-token"
    every { helper.toFirebaseCredential("id-token") } returns firebaseCredential
    every { auth.signInWithCredential(firebaseCredential) } returns Tasks.forResult(authResult)
    every { authResult.user } returns firebaseUser
    coEvery { userRepository.getOrCreateUser() } returns null

    val repository = AuthRepositoryFirebase(auth, helper) { userRepository }

    val result = repository.signInWithGoogle(credential)

    assertTrue(result.isSuccess)
    assertSame(firebaseUser, result.getOrNull())
    verify(exactly = 1) { auth.signInWithCredential(firebaseCredential) }
    coVerify(exactly = 1) { userRepository.getOrCreateUser() }
  }

  @Test
  fun signInWithGoogle_returnsFailure_whenCredentialTypeIsWrong() = runTest {
    val credential = CustomCredential("not-google", Bundle())
    val helper = mockk<GoogleSignInHelper>(relaxed = true)
    val auth = mockk<FirebaseAuth>(relaxed = true)
    val repository = AuthRepositoryFirebase(auth, helper) { mockk(relaxed = true) }

    val result = repository.signInWithGoogle(credential)

    assertTrue(result.isFailure)
  }

  @Test
  fun signInWithGoogle_returnsFailure_whenFirebaseUserIsNull() = runTest {
    val data = Bundle()
    val credential = CustomCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, data)
    val helper = mockk<GoogleSignInHelper>()
    val auth = mockk<FirebaseAuth>()
    val googleIdTokenCredential = mockk<GoogleIdTokenCredential>()
    val firebaseCredential = mockk<AuthCredential>()
    val authResult = mockk<AuthResult>()
    val userRepository = mockk<UserRepositoryFirebase>()

    every { helper.extractIdTokenCredential(data) } returns googleIdTokenCredential
    every { googleIdTokenCredential.idToken } returns "another-id-token"
    every { helper.toFirebaseCredential("another-id-token") } returns firebaseCredential
    every { auth.signInWithCredential(firebaseCredential) } returns Tasks.forResult(authResult)
    every { authResult.user } returns null

    val repository = AuthRepositoryFirebase(auth, helper) { userRepository }

    val result = repository.signInWithGoogle(credential)

    assertTrue(result.isFailure)
    coVerify(exactly = 0) { userRepository.getOrCreateUser() }
  }

  @Test
  fun signInWithGoogle_returnsFailure_whenFirebaseThrows() = runTest {
    val data = Bundle()
    val credential = CustomCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, data)
    val helper = mockk<GoogleSignInHelper>()
    val auth = mockk<FirebaseAuth>()
    val googleIdTokenCredential = mockk<GoogleIdTokenCredential>()
    val firebaseCredential = mockk<AuthCredential>()
    val exception = IllegalArgumentException("boom")
    val userRepository = mockk<UserRepositoryFirebase>()

    every { helper.extractIdTokenCredential(data) } returns googleIdTokenCredential
    every { googleIdTokenCredential.idToken } returns "token"
    every { helper.toFirebaseCredential("token") } returns firebaseCredential
    every { auth.signInWithCredential(firebaseCredential) } returns
        Tasks.forException<AuthResult>(exception)

    val repository = AuthRepositoryFirebase(auth, helper) { userRepository }

    val result = repository.signInWithGoogle(credential)

    assertTrue(result.isFailure)
    val error = result.exceptionOrNull()
    assertTrue(error is IllegalStateException)
    assertTrue(error?.message?.contains("boom") == true)
    coVerify(exactly = 0) { userRepository.getOrCreateUser() }
  }

  @Test
  fun signOut_returnsSuccess_whenSignOutCompletes() {
    val auth = mockk<FirebaseAuth>(relaxUnitFun = true)
    val repository = AuthRepositoryFirebase(auth, mockk(relaxed = true)) { mockk(relaxed = true) }

    val result = repository.signOut()

    assertTrue(result.isSuccess)
    verify(exactly = 1) { auth.signOut() }
  }

  @Test
  fun signOut_returnsFailure_whenFirebaseThrows() {
    val auth = mockk<FirebaseAuth>()
    val repository = AuthRepositoryFirebase(auth, mockk(relaxed = true)) { mockk(relaxed = true) }
    every { auth.signOut() } throws IllegalStateException("sign-out failed")

    val result = repository.signOut()

    assertTrue(result.isFailure)
    val error = result.exceptionOrNull()
    assertTrue(error is IllegalStateException)
    assertEquals("Logout failed: sign-out failed", error?.message)
  }

  @Test
  fun isUserSignedIn_returnsTrue_whenUserExists() {
    val auth = mockk<FirebaseAuth>()
    val firebaseUser = mockk<FirebaseUser>()
    every { auth.currentUser } returns firebaseUser
    val repository = AuthRepositoryFirebase(auth, mockk(relaxed = true)) { mockk(relaxed = true) }

    val result = repository.isUserSignedIn()

    assertTrue(result)
  }

  @Test
  fun isUserSignedIn_returnsFalse_whenUserIsNull() {
    val auth = mockk<FirebaseAuth>()
    every { auth.currentUser } returns null
    val repository = AuthRepositoryFirebase(auth, mockk(relaxed = true)) { mockk(relaxed = true) }

    val result = repository.isUserSignedIn()

    assertTrue(!result)
  }
}
