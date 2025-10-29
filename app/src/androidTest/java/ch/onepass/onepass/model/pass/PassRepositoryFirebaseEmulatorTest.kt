package ch.onepass.onepass.model.pass

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PassRepositoryFirebaseEmulatorTest : PassFirestoreTestBase() {

  private lateinit var uid: String

  private companion object {
    const val TIMEOUT: Long = 10_000
  }

  @Before
  override fun setUp() {
    super.setUp()
    runBlocking {
      auth.signInAnonymously().await()
      uid = requireNotNull(auth.currentUser?.uid) { "anonymous sign-in failed" }
      clearUserPass(uid)
    }
  }

  private suspend fun writeValidPassAsNumbers(
      uid: String = this.uid,
      kid: String = "kid-001",
      issuedAtSeconds: Long = 1_700_000_000L,
      version: Int = 1,
      active: Boolean = true,
      signature: String = "A_-0",
      lastScannedAtSeconds: Long? = null,
      revokedAtSeconds: Long? = null,
  ) {
    val pass =
        buildMap<String, Any?> {
          put("uid", uid)
          put("kid", kid)
          put("issuedAt", issuedAtSeconds)
          put("version", version)
          put("active", active)
          put("signature", signature)
          if (lastScannedAtSeconds != null) put("lastScannedAt", lastScannedAtSeconds)
          if (revokedAtSeconds != null) put("revokedAt", revokedAtSeconds)
        }
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
  }

  private suspend fun writeValidPassAsTimestamps(
      uid: String = this.uid,
      kid: String = "kid-TS",
      issuedAt: Timestamp = Timestamp(1_700_000_100L, 0),
      lastScannedAt: Timestamp? = null,
      revokedAt: Timestamp? = null,
      active: Boolean = true,
      version: Int = 1,
      signature: String = "A_-0",
  ) {
    val pass =
        buildMap<String, Any?> {
          put("uid", uid)
          put("kid", kid)
          put("issuedAt", issuedAt)
          put("version", version)
          put("active", active)
          put("signature", signature)
          if (lastScannedAt != null) put("lastScannedAt", lastScannedAt)
          if (revokedAt != null) put("revokedAt", revokedAt)
        }
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
  }

  @Test
  fun getUserPass_emitsNull_thenPass() = runBlocking {
    val flow = repository.getUserPass(uid).distinctUntilChanged()
    val first = withTimeout(TIMEOUT) { flow.first() }
    Assert.assertNull(first)
    writeValidPassAsNumbers()
    val second = withTimeout(TIMEOUT) { flow.filterNotNull().first() }
    val p = requireNotNull(second)
    Assert.assertEquals(uid, p.uid)
    Assert.assertEquals("kid-001", p.kid)
    Assert.assertTrue(p.issuedAt > 0)
    Assert.assertTrue(p.version > 0)
    Assert.assertTrue(p.signature.isNotBlank())
  }

  @Test
  fun getUserPass_updatesOnRevokeAndScan() = runBlocking {
    writeValidPassAsNumbers(active = true)
    val flow = repository.getUserPass(uid).distinctUntilChanged().filterNotNull()
    val initial = withTimeout(TIMEOUT) { flow.first() }
    Assert.assertTrue(initial.isValidNow)
    Assert.assertEquals("Active", initial.statusText)
    repository.revokePass(uid).getOrThrow()
    val revoked =
        withTimeout(TIMEOUT) { flow.first { !it.isValidNow && it.statusText == "Revoked" } }
    Assert.assertFalse(revoked.isValidNow)
    Assert.assertEquals("Revoked", revoked.statusText)
    repository.markScanned(uid).getOrThrow()
    val scanned = withTimeout(TIMEOUT) { flow.first { it.lastScannedAt != null } }
    Assert.assertNotNull(scanned.lastScannedAt)
  }

  @Test
  fun getOrCreateSignedPass_returnsExisting() = runBlocking {
    writeValidPassAsNumbers(kid = "kid-existing")
    val res = repository.getOrCreateSignedPass(uid)
    Assert.assertTrue(res.isSuccess)
    val p = res.getOrThrow()
    Assert.assertEquals(uid, p.uid)
    Assert.assertEquals("kid-existing", p.kid)
  }

  @Test
  fun revokePass_setsInactive_andRevokedAt() = runBlocking {
    writeValidPassAsNumbers(active = true)
    repository.revokePass(uid).getOrThrow()
    val snap = firestore.collection("user").document(uid).get().await()
    val passMap = snap.get("pass") as? Map<*, *> ?: error("missing pass")
    Assert.assertEquals(false, passMap["active"])
    Assert.assertNotNull(passMap["revokedAt"])
  }

  @Test
  fun markScanned_setsLastScannedAt() = runBlocking {
    writeValidPassAsNumbers(active = true)
    repository.markScanned(uid).getOrThrow()
    val snap = firestore.collection("user").document(uid).get().await()
    val passMap = snap.get("pass") as? Map<*, *> ?: error("missing pass")
    Assert.assertNotNull(passMap["lastScannedAt"])
  }

  @Test
  fun mapping_supportsTimestampAndNumber_andFiltersIncomplete() = runBlocking {
    firestore
        .collection("user")
        .document(uid)
        .set(
            mapOf(
                "pass" to
                    mapOf(
                        "uid" to uid,
                        "kid" to "",
                        "issuedAt" to 1_700_000_000L,
                        "version" to 1,
                        "active" to true,
                        "signature" to "A_-0")))
        .await()
    val first = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
    Assert.assertNull(first)
    writeValidPassAsTimestamps(
        kid = "kid-TS",
        issuedAt = Timestamp(1_700_000_100L, 0),
        lastScannedAt = Timestamp(1_700_000_200L, 0))
    val p =
        withTimeout(TIMEOUT) {
          repository.getUserPass(uid).filterNotNull().distinctUntilChanged().first()
        }
    Assert.assertEquals("kid-TS", p.kid)
    Assert.assertEquals(1_700_000_100L, p.issuedAt)
    Assert.assertEquals(1_700_000_200L, p.lastScannedAt)
  }

  @Test
  fun getUserPass_returnsNullForMissingDocument() = runBlocking {
    clearUserPass(uid)
    val result = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
    Assert.assertNull(result)
  }

  @Test
  fun getOrCreateSignedPass_failsWhenFunctionNeverWrites() = runBlocking {
    clearUserPass(uid)
    val result = repository.getOrCreateSignedPass(uid)
    Assert.assertTrue(result.isFailure)
  }

  @Test
  fun revokePass_multipleCallsAreIdempotent() = runBlocking {
    writeValidPassAsNumbers(active = true)
    repository.revokePass(uid).getOrThrow()
    repository.revokePass(uid).getOrThrow()
    val snap = firestore.collection("user").document(uid).get().await()
    val passMap = snap.get("pass") as? Map<*, *> ?: error("missing pass")
    Assert.assertEquals(false, passMap["active"])
  }

  @Test
  fun markScanned_multipleTimes_updatesTimestamp() = runBlocking {
    writeValidPassAsNumbers(active = true)
    repository.markScanned(uid).getOrThrow()
    val firstSnap = firestore.collection("user").document(uid).get().await()
    val firstTime = firstSnap.get("pass.lastScannedAt")
    repository.markScanned(uid).getOrThrow()
    val secondSnap = firestore.collection("user").document(uid).get().await()
    val secondTime = secondSnap.get("pass.lastScannedAt")
    Assert.assertNotEquals(firstTime, secondTime)
  }

  @Test
  fun mapping_ignoresIncompletePasses_missingSignatureOrKid() = runBlocking {
    firestore
        .collection("user")
        .document(uid)
        .set(
            mapOf(
                "pass" to
                    mapOf(
                        "uid" to uid,
                        "issuedAt" to 1_700_000_000L,
                        "version" to 1,
                        "active" to true)))
        .await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
    Assert.assertNull(p)
  }

  @Test
  fun getUserPass_reflectsNewWrites() = runBlocking {
    clearUserPass(uid)
    val flow = repository.getUserPass(uid).distinctUntilChanged()
    writeValidPassAsNumbers(kid = "newKID")
    val p = withTimeout(TIMEOUT) { flow.filterNotNull().first() }
    Assert.assertEquals("newKID", p.kid)
  }
}
