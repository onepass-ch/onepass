package ch.onepass.onepass.ui.myevents

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.pass.PassRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// DataStore extension (top-level, not inside a class)
val Context.passDataStore: DataStore<Preferences> by preferencesDataStore(name = "onepass_cache")

object MyEventsTestTags {
  const val TABS_ROW = "TabsRow"
  const val TAB_CURRENT = "TabCurrent"
  const val TAB_EXPIRED = "TabExpired"
  const val QR_CODE_ICON = "QrCodeIcon"
  const val QR_CODE_DIALOG = "QrCodeDialog"
  const val TICKET_CARD = "TicketCard"
  const val TICKET_TITLE = "TicketTitle"
  const val TICKET_STATUS = "TicketStatus"
  const val TICKET_DATE = "TicketDate"
  const val TICKET_LOCATION = "TicketLocation"
  const val TICKET_DIALOG_TITLE = "TicketDialogTitle"
  const val TICKET_DIALOG_STATUS = "TicketDialogStatus"
  const val TICKET_DIALOG_DATE = "TicketDialogDate"
  const val TICKET_DIALOG_LOCATION = "TicketDialogLocation"
}

class MyEventsViewModel(
  app: Application,
  private val passRepository: PassRepository
) : AndroidViewModel(app) {

  private val QR_CACHE_KEY = stringPreferencesKey("cached_qr_text")

  // UI state
  private val _userQrData = MutableStateFlow<String?>(null)
  val userQrData: StateFlow<String?> = _userQrData

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error

  init {
    // Load cached QR on startup (offline support)
    viewModelScope.launch { loadCachedQr() }
  }

  // Loads or creates the signed pass; caches QR for offline use
  fun loadUserPass(uid: String) {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null

      val result = passRepository.getOrCreateSignedPass(uid)
      result.onSuccess { pass ->
        _userQrData.value = pass.qrText
        saveCachedQr(pass.qrText)
      }.onFailure { e ->
        _error.value = e.message ?: "Unknown error"
        loadCachedQr() // fallback to cache if available
      }

      _isLoading.value = false
    }
  }

  fun refreshPass(uid: String) = loadUserPass(uid)


  private suspend fun saveCachedQr(qrText: String) {
    withContext(Dispatchers.IO) {
      getApplication<Application>().passDataStore.edit { prefs ->
        prefs[QR_CACHE_KEY] = qrText
      }
    }
  }

  private suspend fun loadCachedQr() {
    withContext(Dispatchers.IO) {
      val prefs = getApplication<Application>().passDataStore.data.first()
      _userQrData.value = prefs[QR_CACHE_KEY]
    }
  }
}
