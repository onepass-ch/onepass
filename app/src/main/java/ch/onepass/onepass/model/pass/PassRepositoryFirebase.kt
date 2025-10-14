package ch.onepass.onepass.model.pass

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of PassRepository.
 *
 * Data model is stored under: user/{uid}.pass Signing/refresh is delegated to the Cloud Function:
 * "generateUserPass".
 */
class PassRepositoryFirebase(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : PassRepository {

  // --------------------- READ ---------------------

  override fun getUserPass(uid: String): Flow<Pass?> = callbackFlow {
    val reg =
        db.collection("user").document(uid).addSnapshotListener { snap, err ->
          if (err != null) {
            close(err)
            return@addSnapshotListener
          }
          // snap peut être null si le doc n’existe pas encore
          val pass = snap?.toPass()
          trySendBlocking(pass) // push sûr même en cas de légère back-pressure
        }
    awaitClose { reg.remove() }
  }

  // --------------------- CREATE / REFRESH ---------------------

  /**
   * Returns the existing Pass or generates a new one (server-signed) if missing. The QR string
   * itself is not stored here; UI can build it via Pass.qrText.
   */
  override suspend fun getOrCreateSignedPass(uid: String): Result<Pass> = runCatching {
    val docRef = db.collection("user").document(uid)

    // 1) Try existing
    docRef.get().await().toPass()?.let {
      return@runCatching it
    }

    // 2) Otherwise ask server to sign & write
    callGenerateUserPassFunction()

    // 3) Poll Firestore avec backoff (100ms → 600ms)
    repeat(6) { i ->
      docRef.get().await().toPass()?.let {
        return@runCatching it
      }
      delay(100L * (i + 1))
    }

    error("Pass generation failed: not visible after function call")
  }

  // --------------------- UPDATE (business ops) ---------------------

  /**
   * Marks pass as revoked: active=false and revokedAt=now . Prefer a Cloud Function for
   * authoritative timestamps if needed.
   */
  override suspend fun revokePass(uid: String): Result<Unit> = runCatching {
    db.collection("user")
        .document(uid)
        .set(
            mapOf("pass" to mapOf("active" to false, "revokedAt" to FieldValue.serverTimestamp())),
            SetOptions.merge())
        .await()
  }

  /**
   * Updates lastScannedAt (in seconds since epoch). Call this after a successful validation (if you
   * track scans client-side).
   */
  override suspend fun markScanned(uid: String): Result<Unit> = runCatching {
    db.collection("user")
        .document(uid)
        .set(
            mapOf("pass" to mapOf("lastScannedAt" to FieldValue.serverTimestamp())),
            SetOptions.merge())
        .await()
  }

  // --------------------- INTERNAL HELPERS ---------------------

  /**
   * Calls the Cloud Function that signs the pass and writes it under user/{uid}.pass. The function
   * should rely on context.auth.uid and Secret Manager (private key).
   *
   * Expected Function name: "generateUserPass" Expected server behavior: set
   * user/{uid}.pass.{uid,kid,issuedAt,version,active,signature}
   */
  private suspend fun callGenerateUserPassFunction() {
    // If your CF returns something useful you can parse it here,
    // but we always re-read from Firestore as source of truth.
    functions.getHttpsCallable("generateUserPass").call().await()
  }

  /**
   * Maps a user document to a Pass by reading the nested "pass" field. Returns null if the pass is
   * absent or incomplete.
   */
  @Suppress("UNCHECKED_CAST")
  private fun DocumentSnapshot.toPass(): Pass? {
    val m = this.get("pass") as? Map<String, Any?> ?: return null

    fun anyToSeconds(value: Any?): Long? =
        when (value) {
          is Number -> value.toLong()
          is Timestamp -> value.seconds
          else -> null
        }

    val uidFromPass = (m["uid"] as? String)?.trim().orEmpty()
    val kid = (m["kid"] as? String)?.trim().orEmpty()
    val issuedAt = anyToSeconds(m["issuedAt"]) ?: 0L
    val lastScannedAt = anyToSeconds(m["lastScannedAt"])
    val revokedAt = anyToSeconds(m["revokedAt"])
    val active = (m["active"] as? Boolean) ?: true
    val version = (m["version"] as? Number)?.toInt() ?: 1
    val signature = (m["signature"] as? String)?.trim().orEmpty()

    val uid = if (uidFromPass.isNotBlank()) uidFromPass else this.id

    val pass =
        Pass(
            uid = uid,
            kid = kid,
            issuedAt = issuedAt,
            lastScannedAt = lastScannedAt,
            active = active,
            version = version,
            revokedAt = revokedAt,
            signature = signature)

    return if (pass.isIncomplete) null else pass
  }
}
