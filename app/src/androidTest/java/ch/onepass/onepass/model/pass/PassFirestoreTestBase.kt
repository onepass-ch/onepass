package ch.onepass.onepass.model.pass

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import ch.onepass.onepass.utils.FirebaseEmulator
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass

open class PassFirestoreTestBase {

  protected lateinit var repository: PassRepository
  protected lateinit var firestore: FirebaseFirestore
  protected lateinit var functions: FirebaseFunctions

  protected val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

  @OptIn(ExperimentalCoroutinesApi::class) private val testDispatcher = UnconfinedTestDispatcher()

  private val testRunId = UUID.randomUUID().toString().take(8)

  protected fun getTestUserId(suffix: String = "default"): String {
    return "test_${testRunId}_$suffix"
  }

  companion object {
    private var keySeeded = false

    @JvmStatic
    @BeforeClass
    fun setUpEmulator() {
      check(FirebaseEmulator.isRunning) {
        "Firebase emulators not reachable. Run: firebase emulators:start --only firestore,auth,functions,ui"
      }
    }
  }

  @Before
  open fun setUp() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    FirebaseApp.getApps(ctx).forEach { it.delete() }
    FirebaseApp.initializeApp(ctx)

    check(FirebaseEmulator.isRunning) {
      "Firebase emulators not reachable. Run: firebase emulators:start --only firestore,auth,functions,ui"
    }

    firestore = FirebaseFirestore.getInstance()
    functions = FirebaseFunctions.getInstance()

    val host = FirebaseEmulator.HOST
    auth.useEmulator(host, 9099)
    firestore.useEmulator(host, 8080)
    firestore.firestoreSettings =
        FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
    functions.useEmulator(host, 5001)

    repository = PassRepositoryFirebase(db = firestore, functions = functions)

    runBlocking(testDispatcher) {
      // Seed signing key once for all tests
      if (!keySeeded) {
        ensureSigningKeyExists()
        keySeeded = true
      }

      auth.currentUser?.let { user ->
        if (hasUserPass(user.uid)) {
          Log.w("PassFirestoreTestBase", "Cleaning existing pass for user ${user.uid}")
          clearUserPass(user.uid)
        }
      }
    }
  }

  @After
  open fun tearDown() {
    runBlocking(testDispatcher) { auth.currentUser?.let { clearUserPass(it.uid) } }
    FirebaseEmulator.clearFirestoreEmulator()
  }

  protected suspend fun ensureSigningKeyExists() {
    val existingKeys = firestore.collection("keys").whereEqualTo("active", true).get().await()

    if (!existingKeys.isEmpty) {
      Log.d("PassFirestoreTestBase", "Test signing key already exists")
      return
    }

    Log.d("PassFirestoreTestBase", "Seeding test signing key")
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
    Log.d("PassFirestoreTestBase", "Test signing key created successfully")
  }

  protected suspend fun hasUserPass(uid: String): Boolean {
    val doc = firestore.collection("user").document(uid).get(Source.SERVER).await()
    return doc.contains("pass")
  }

  protected suspend fun clearUserPass(uid: String) {
    val docRef = firestore.collection("user").document(uid)
    val initial = docRef.get(Source.SERVER).await()
    if (!initial.exists() || !initial.contains("pass")) return
    docRef.update(mapOf("pass" to FieldValue.delete())).await()
    repeat(10) { attempt ->
      delay(100L * (attempt + 1))
      val snap = docRef.get(Source.SERVER).await()
      if (!snap.contains("pass")) return
    }
    val left = docRef.get(Source.SERVER).await()
    if (left.contains("pass")) {
      error("Test cleanup failed: 'pass' still present for uid=$uid")
    }
  }
}
