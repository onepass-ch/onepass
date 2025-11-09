package ch.onepass.onepass.model.pass

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of PassRepository for the USER app.
 * - getUserPass(): listens to pass live updates (auto-refresh UI).
 * - getOrCreateSignedPass(): creates the pass the first time via Cloud Function "generateUserPass"
 *   which RETURNS the created pass (no client polling).
 * - revokePass(): lets the user deactivate their pass (optional).
 *
 * SECURITY NOTE: lastScannedAt is updated server-side by the Cloud Function "validateEntry" after
 * ticket verification, not by the client (prevents fraud).
 */
class PassRepositoryFirebase(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : PassRepository {

  /** Centralized collection reference to avoid typos. */
  private val users = db.collection("user")

  // --------------------- READ (live) ---------------------

  override fun getUserPass(uid: String): Flow<Pass?> {
    require(uid.isNotBlank()) { "uid required" }
    return docSnapshotFlow(users.document(uid)) { snap -> snap.toPass() }
  }

  // --------------------- CREATE / FETCH ---------------------

  /**
   * Returns the existing Pass or generates a new one (server-signed) if missing. The Cloud Function
   * writes to Firestore and RETURNS the pass directly; no polling needed.
   */
  override suspend fun getOrCreateSignedPass(uid: String): Result<Pass> = runCatching {
    require(uid.isNotBlank()) { "uid required" }

    // 1) If present in Firestore, return it
    users.document(uid).get().await().toPass()?.let {
      return@runCatching it
    }

    // 2) Else call the CF which creates AND returns the pass
    val data = functions.getHttpsCallable("generateUserPass").call().await().data
    val pass = dataToPassStrict(data)
    require(!pass.isIncomplete) { "Function returned incomplete pass" }
    pass
  }

  // --------------------- UPDATE (optional business op) ---------------------

  /** Marks pass as revoked (active=false, revokedAt=server time). */
  override suspend fun revokePass(uid: String): Result<Unit> = runCatching {
    require(uid.isNotBlank()) { "uid required" }
    users
        .document(uid)
        .set(
            mapOf("pass" to mapOf("active" to false, "revokedAt" to FieldValue.serverTimestamp())),
            SetOptions.merge())
        .await()
  }

  // --------------------- INTERNAL HELPERS ---------------------

  /**
   * Maps the Cloud Function response (expected to be a Map) to a Pass. Required: uid, kid, issuedAt
   * (secs), version, active, signature. Optional: lastScannedAt, revokedAt.
   */
  private fun dataToPassStrict(data: Any?): Pass {
    val m: Map<String, Any?> =
        (data as? Map<*, *>)
            ?.entries
            ?.mapNotNull { (k, v) -> (k as? String)?.let { it to v } }
            ?.toMap() ?: error("Invalid function response: expected a map")

    fun anyToSeconds(value: Any?): Long? =
        when (value) {
          is Number -> value.toLong()
          is Timestamp -> value.seconds
          else -> null
        }

    val uid = (m["uid"] as? String)?.trim().orEmpty()
    require(uid.isNotBlank()) { "Function response invalid: uid missing" }

    val kid = (m["kid"] as? String)?.trim().orEmpty()
    val issuedAt = anyToSeconds(m["issuedAt"]) ?: 0L
    val active = (m["active"] as? Boolean) ?: true
    val version = (m["version"] as? Number)?.toInt() ?: 1
    val signature = (m["signature"] as? String)?.trim().orEmpty()
    val lastScannedAt = anyToSeconds(m["lastScannedAt"])
    val revokedAt = anyToSeconds(m["revokedAt"])

    return Pass(
        uid = uid,
        kid = kid,
        issuedAt = issuedAt,
        lastScannedAt = lastScannedAt,
        active = active,
        version = version,
        revokedAt = revokedAt,
        signature = signature)
  }

  /**
   * Maps a user document to a Pass by reading the nested "pass" field. Returns null if the pass is
   * absent or incomplete. NOTE: uid MUST be present in pass; otherwise we throw to surface the data
   * issue.
   */
  private fun DocumentSnapshot.toPass(): Pass? {
    val raw = this.get("pass") as? Map<*, *> ?: return null

    @Suppress("UNCHECKED_CAST")
    val m = raw as Map<String, Any?> // narrowed suppression on this single cast

    fun anyToSeconds(value: Any?): Long? =
        when (value) {
          is Number -> value.toLong()
          is Timestamp -> value.seconds
          else -> null
        }

    val uidFromPass = (m["uid"] as? String)?.trim().orEmpty()
    require(uidFromPass.isNotBlank()) {
      "Invalid Firestore data: pass.uid missing for document ${this.id}"
    }

    val kid = (m["kid"] as? String)?.trim().orEmpty()
    val issuedAt = anyToSeconds(m["issuedAt"]) ?: 0L
    val lastScannedAt = anyToSeconds(m["lastScannedAt"])
    val revokedAt = anyToSeconds(m["revokedAt"])
    val active = (m["active"] as? Boolean) ?: true
    val version = (m["version"] as? Number)?.toInt() ?: 1
    val signature = (m["signature"] as? String)?.trim().orEmpty()

    val pass =
        Pass(
            uid = uidFromPass,
            kid = kid,
            issuedAt = issuedAt,
            lastScannedAt = lastScannedAt,
            active = active,
            version = version,
            revokedAt = revokedAt,
            signature = signature)

    return if (pass.isIncomplete) null else pass
  }

  /** Helper to create a Flow from a Firestore *document* using a snapshot listener. */
  private fun <T> docSnapshotFlow(
      docRef: DocumentReference,
      map: (DocumentSnapshot) -> T
  ): Flow<T> = callbackFlow {
    val listener =
        docRef.addSnapshotListener { snap, err ->
          if (err != null) {
            close(err)
            return@addSnapshotListener
          }
          if (snap != null) {
            trySendBlocking(map(snap))
          }
        }
    awaitClose { listener.remove() }
  }
}
