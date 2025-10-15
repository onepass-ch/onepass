package ch.onepass.onepass.model.pass


import kotlinx.coroutines.tasks.await
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import ch.onepass.onepass.utils.FirebaseEmulator
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before


open class PassFirestoreTestBase {


  protected lateinit var repository: PassRepository
  protected lateinit var firestore: FirebaseFirestore
  protected lateinit var auth: FirebaseAuth


  @OptIn(ExperimentalCoroutinesApi::class)
  private val testDispatcher = UnconfinedTestDispatcher() // exécute immédiatement, pas de temps virtuel


  @Before
  open fun setUp() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()


    // 🔄 Réinitialise toute instance Firebase pour pouvoir appeler useEmulator() ensuite
    FirebaseApp.getApps(ctx).forEach { it.delete() }
    FirebaseApp.initializeApp(ctx)


    check(FirebaseEmulator.isRunning) {
      "Firebase emulators not reachable. Run: firebase emulators:start --only firestore,auth,functions,ui"
    }


    // Instances
    auth = FirebaseAuth.getInstance()
    firestore = FirebaseFirestore.getInstance()
    val functions = FirebaseFunctions.getInstance()


    // ➜ Configuration émulateurs (avant tout usage)
    val host = FirebaseEmulator.HOST        // 10.0.2.2 sur AVD, 127.0.0.1 en JVM si codé ainsi
    auth.useEmulator(host, 9099)
    firestore.useEmulator(host, 8080)
    firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
      .setPersistenceEnabled(false) // déterministe pour les tests
      .build()
    functions.useEmulator(host, 5001)


    // Repo branché sur ces instances
    repository = PassRepositoryFirebase(
      db = firestore,
      functions = functions
    )


    // 🔧 Nettoyage éventuel en TEMPS RÉEL (pas de runTest / temps virtuel)
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
    runBlocking(testDispatcher) {
      auth.currentUser?.let { clearUserPass(it.uid) }
    }
    FirebaseEmulator.clearFirestoreEmulator()
  }


  // --- Helpers suspend ---


  protected suspend fun hasUserPass(uid: String): Boolean {
    val doc = firestore.collection("user").document(uid).get().await()
    return doc.contains("pass")
  }


  protected suspend fun clearUserPass(uid: String) {
    val docRef = firestore.collection("user").document(uid)
    val snapshot = docRef.get().await()
    if (!snapshot.exists() || !snapshot.contains("pass")) return
    docRef.update(mapOf("pass" to FieldValue.delete())).await()
  }
}
