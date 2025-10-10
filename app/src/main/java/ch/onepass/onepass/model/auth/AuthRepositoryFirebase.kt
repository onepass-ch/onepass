// Code from EPFL CS-311 Bootcamp-B3-Solution (2025-2026 fall)

package ch.onepass.onepass.model.auth

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserRepositoryFirebase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class AuthRepositoryFirebase(
  private val auth: FirebaseAuth = Firebase.auth,
  private val helper: GoogleSignInHelper = DefaultGoogleSignInHelper(),
  private val userRepository: UserRepository = UserRepositoryFirebase()
) : AuthRepository {

  override var currentUser: User? = null

  fun getGoogleSignInOption(serverClientId: String) =
    GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()

  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
    return try {
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val idToken = helper.extractIdTokenCredential(credential.data).idToken
        val firebaseCred = helper.toFirebaseCredential(idToken)

        val user =
          auth.signInWithCredential(firebaseCred).await().user
            ?: return Result.failure(
              IllegalStateException("Login failed : Could not retrieve user information")
            )

        currentUser = userRepository.getOrCreateUser(user)

        return Result.success(user)
      } else {
        return Result.failure(
          IllegalStateException("Login failed: Credential is not of type Google ID")
        )
      }
    } catch (e: Exception) {
      Result.failure(
        IllegalStateException("Login failed: ${e.localizedMessage ?: "Unexpected error."}")
      )
    }
  }

  override fun signOut(): Result<Unit> {
    return try {
      auth.signOut()
      currentUser = null
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(
        IllegalStateException("Logout failed: ${e.localizedMessage ?: "Unexpected error."}")
      )
    }
  }
}
