package ch.onepass.onepass.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FakeAuthViewModel : AuthViewModel() {

  private val _fakeUiState = MutableStateFlow(AuthUiState(isLoading = false))
  override val uiState = _fakeUiState.asStateFlow()

  var signInCalled = false

  override fun signIn(context: Context, credentialManager: CredentialManager) {
    signInCalled = true
    viewModelScope.launch { _fakeUiState.value = AuthUiState(isLoading = true) }
  }

  fun setLoading(isLoading: Boolean) {
    _fakeUiState.value = AuthUiState(isLoading = isLoading)
  }
}
