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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before

open class PassFirestoreTestBase {

  protected lateinit var repository: PassRepository
  protected lateinit var firestore: FirebaseFirestore

  protected val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

  @OptIn(ExperimentalCoroutinesApi::class) private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  open fun setUp() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    FirebaseApp.getApps(ctx).forEach { it.delete() }
    FirebaseApp.initializeApp(ctx)

    check(FirebaseEmulator.isRunning) {
      "Firebase emulators not reachable. Run: firebase emulators:start --only firestore,auth,functions,ui"
    }

    firestore = FirebaseFirestore.getInstance()
    val functions = FirebaseFunctions.getInstance()

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

  protected suspend fun hasUserPass(uid: String): Boolean {
    val doc = firestore.collection("user").document(uid).get(Source.SERVER).await()
    return doc.contains("pass")
  }

  protected suspend fun clearUserPass(uid: String) {
    val docRef = firestore.collection("user").document(uid)
    val initial = docRef.get(Source.SERVER).await()
    if (!initial.exists() || !initial.contains("pass")) return
    docRef.update(mapOf("pass" to FieldValue.delete())).await()
    // 10 times, could be a different number
    repeat(10) { attempt ->
      val snap = docRef.get(Source.SERVER).await()
      if (!snap.contains("pass")) return
      delay(100L * (attempt + 1)) // 50ms → 100ms
    }
    val left = docRef.get(Source.SERVER).await()
    if (left.contains("pass")) {
      error("Test cleanup failed: 'pass' still present for uid=$uid")
    }
  }
}
