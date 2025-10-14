package ch.onepass.onepass.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.R
import ch.onepass.onepass.model.auth.AuthRepositoryFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val errorMessage: String? = null
)

open class AuthViewModel(
    private val authRepository: AuthRepositoryFirebase = AuthRepositoryFirebase()
) : ViewModel() {

  private val _uiState = MutableStateFlow(AuthUiState())
  open val uiState = _uiState.asStateFlow()

  fun getServerClientId(context: Context): String {
    return context.getString(R.string.default_web_client_id)
  }

  open fun signIn(context: Context, credentialManager: CredentialManager) {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // set google signin option
        val googleOption = authRepository.getGoogleSignInOption(getServerClientId(context))

        // create credential request
        val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()

        // call system login ui
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        // use firebase login
        val loginResult = authRepository.signInWithGoogle(credential)
        loginResult
            .onSuccess { _uiState.value = _uiState.value.copy(isSignedIn = true) }
            .onFailure { _uiState.value = _uiState.value.copy(errorMessage = it.message) }
      } catch (e: Exception) {
        _uiState.value = AuthUiState(errorMessage = e.message)
      } finally {
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      authRepository.signOut()
      _uiState.value = _uiState.value.copy(isSignedIn = false)
    }
  }
}
