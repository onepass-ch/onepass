package ch.onepass.onepass.utils

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * An object to manage the connection to Firebase Emulators for Android tests.
 *
 * This object will automatically use the emulators if they are running when the tests start.
 */
object FirebaseEmulator {
  val auth
    get() = Firebase.auth

  val firestore
    get() = Firebase.firestore

  const val HOST = "10.0.2.2"
  const val EMULATORS_PORT = 4400
  const val FIRESTORE_PORT = 8080
  const val AUTH_PORT = 9099
  const val STORAGE_PORT = 9199

  val projectID by lazy { FirebaseApp.getInstance().options.projectId }

  private val httpClient = OkHttpClient()

  private val firestoreEndpoint by lazy {
    "http://${HOST}:$FIRESTORE_PORT/emulator/v1/projects/$projectID/databases/(default)/documents"
  }

  private val authEndpoint by lazy {
    "http://${HOST}:$AUTH_PORT/emulator/v1/projects/$projectID/accounts"
  }

  private val emulatorsEndpoint = "http://$HOST:$EMULATORS_PORT/emulators"

  private var keySeeded = false

  private fun areEmulatorsRunning(): Boolean =
      runCatching {
            val client = httpClient
            val request = Request.Builder().url(emulatorsEndpoint).build()
            client.newCall(request).execute().isSuccessful
          }
          .getOrNull() == true

  val isRunning = areEmulatorsRunning()

  init {
    if (isRunning) {
      auth.useEmulator(HOST, AUTH_PORT)
      firestore.useEmulator(HOST, FIRESTORE_PORT)
      Firebase.storage.useEmulator(HOST, STORAGE_PORT)
      assert(Firebase.firestore.firestoreSettings.host.contains(HOST)) {
        "Failed to connect to Firebase Firestore Emulator."
      }
    }
  }

  private fun clearEmulator(endpoint: String) {
    val client = httpClient
    val request = Request.Builder().url(endpoint).delete().build()
    val response = client.newCall(request).execute()

    assert(response.isSuccessful) { "Failed to clear emulator at $endpoint" }
  }

  fun clearAuthEmulator() {
    clearEmulator(authEndpoint)
  }

  fun clearFirestoreEmulator() {
    clearEmulator(firestoreEndpoint)
  }

  /**
   * Ensures that at least one active signing key exists in Firestore. Prevents "No active signing
   * key found" errors in tests that generate user passes. This is called automatically by test base
   * classes.
   */
  suspend fun ensureSigningKeyExists() {
    if (!isRunning || keySeeded) return

    val existingKeys = firestore.collection("keys").whereEqualTo("active", true).get().await()

    if (!existingKeys.isEmpty) {
      Log.d("FirebaseEmulator", "Test signing key already exists")
      keySeeded = true
      return
    }

    Log.d("FirebaseEmulator", "Seeding test signing key")
    firestore
        .collection("keys")
        .document("test-key-01")
        .set(
            mapOf(
                "kid" to "test-key-01",
                "publicKey" to "sMJlPpZyv1oNbluv+zOHhzFKpeVbAWsqKEMgyySbhDO=",
                "privateKey" to
                    "mXR5sz6O8sRFXASEQjeWKo9yySD36yxXOhi3iaeNO7qwwkg+InK/Wg1+X8DsHQtwgZtYQNx0fA==",
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp()))
        .await()

    keySeeded = true
    Log.d("FirebaseEmulator", "Test signing key created successfully")
  }
}
