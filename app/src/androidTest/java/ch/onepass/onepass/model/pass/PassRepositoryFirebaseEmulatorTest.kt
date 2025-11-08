package ch.onepass.onepass.model.pass

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
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
      uid = getTestUserId("test")
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

  private suspend fun simulateScan(seconds: Long? = null) {
    val value = seconds ?: FieldValue.serverTimestamp()
    firestore
        .collection("user")
        .document(uid)
        .set(mapOf("pass" to mapOf("lastScannedAt" to value)), SetOptions.merge())
        .await()
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

    simulateScan()
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
    simulateScan()
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

  @Test
  fun mapping_supportsNumberForLastScannedAndRevokedAt() = runBlocking {
    val pass =
        mapOf(
            "uid" to uid,
            "kid" to "kid-z",
            "issuedAt" to 1_700_000_400L,
            "version" to 1,
            "active" to false,
            "revokedAt" to 1_700_000_500L,
            "lastScannedAt" to 1_700_000_450L,
            "signature" to "A_-0")
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
    Assert.assertEquals(1_700_000_450L, p.lastScannedAt)
    Assert.assertEquals(1_700_000_500L, p.revokedAt)
    Assert.assertFalse(p.isValidNow)
    Assert.assertEquals("Revoked", p.statusText)
  }

  @Test
  fun mapping_defaultsVersionAndActiveWhenMissing() = runBlocking {
    val pass =
        mapOf("uid" to uid, "kid" to "kid-def", "issuedAt" to 1_700_000_600L, "signature" to "A_-0")
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
    Assert.assertEquals(1, p.version)
    Assert.assertTrue(p.isValidNow)
    Assert.assertEquals("Active", p.statusText)
  }

  @Test
  fun mapping_rejectsIssuedAtZero() = runBlocking {
    val pass =
        mapOf(
            "uid" to uid,
            "kid" to "kid-bad",
            "issuedAt" to 0L,
            "version" to 1,
            "active" to true,
            "signature" to "A_-0")
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
    Assert.assertNull(p)
  }

  @Test
  fun mapping_rejectsInvalidSignature() = runBlocking {
    val pass =
        mapOf(
            "uid" to uid,
            "kid" to "kid-bad2",
            "issuedAt" to 1_700_000_700L,
            "version" to 1,
            "active" to true,
            "signature" to "+++")
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
    Assert.assertNull(p)
  }

  @Test
  fun getUserPass_emitsOnKidChange() = runBlocking {
    writeValidPassAsNumbers(kid = "kid-1")
    val flow = repository.getUserPass(uid).distinctUntilChanged().filterNotNull()
    withTimeout(TIMEOUT) { flow.first() }
    firestore
        .collection("user")
        .document(uid)
        .set(
            mapOf(
                "pass" to
                    mapOf(
                        "uid" to uid,
                        "kid" to "kid-2",
                        "issuedAt" to 1_700_000_000L,
                        "version" to 1,
                        "active" to true,
                        "signature" to "A_-0")))
        .await()
    val updated = withTimeout(TIMEOUT) { flow.first() }
    Assert.assertEquals("kid-2", updated.kid)
  }

  @Test
  fun getOrCreateSignedPass_treatsIncompleteAsMissing_thenFails() = runBlocking {
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
    val res = repository.getOrCreateSignedPass(uid)
    Assert.assertTrue(res.isFailure)
  }

  @Test
  fun mapping_acceptsTimestampsForRevokedAndScan() = runBlocking {
    writeValidPassAsTimestamps(
        kid = "kid-ts2",
        issuedAt = Timestamp(1_700_000_800L, 0),
        lastScannedAt = Timestamp(1_700_000_810L, 0),
        revokedAt = Timestamp(1_700_000_820L, 0),
        active = false)
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
    Assert.assertEquals(1_700_000_810L, p.lastScannedAt)
    Assert.assertEquals(1_700_000_820L, p.revokedAt)
    Assert.assertFalse(p.isValidNow)
  }

  @Test
  fun statusText_inactiveWhenActiveFalseWithoutRevokedAt() = runBlocking {
    writeValidPassAsNumbers(active = false, kid = "kid-inact")
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
    Assert.assertEquals("Inactive", p.statusText)
    Assert.assertFalse(p.isValidNow)
    Assert.assertNull(p.revokedAt)
  }

  @Test
  fun extraUnknownFields_doNotBreakMapping() = runBlocking {
    val pass =
        mapOf(
            "uid" to uid,
            "kid" to "kid-extra",
            "issuedAt" to 1_700_000_900L,
            "version" to 3,
            "active" to true,
            "signature" to "A_-0",
            "foo" to "bar",
            "bar" to 1234)
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
    Assert.assertEquals("kid-extra", p.kid)
    Assert.assertEquals(3, p.version)
    Assert.assertTrue(p.isValidNow)
  }

  @Test
  fun getUserPass_requiresNonBlankUid() = runBlocking {
    try {
      repository.getUserPass("")
      Assert.fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      Assert.assertTrue(e.message?.contains("uid required") == true)
    }
  }

  @Test
  fun getOrCreateSignedPass_requiresNonBlankUid() = runBlocking {
    val result = repository.getOrCreateSignedPass("")
    Assert.assertTrue(result.isFailure)
    Assert.assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun revokePass_requiresNonBlankUid() = runBlocking {
    val result = repository.revokePass("")
    Assert.assertTrue(result.isFailure)
    Assert.assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun mapping_throwsWhenUidMissingInPass() = runBlocking {
    val pass =
        mapOf(
            "kid" to "kid-no-uid",
            "issuedAt" to 1_700_000_000L,
            "version" to 1,
            "active" to true,
            "signature" to "A_-0")
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()

    try {
      withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
      Assert.fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      Assert.assertTrue(e.message?.contains("uid missing") == true)
    }
  }

  @Test
  fun mapping_throwsWhenUidBlankInPass() = runBlocking {
    val pass =
        mapOf(
            "uid" to "  ",
            "kid" to "kid-blank",
            "issuedAt" to 1_700_000_000L,
            "version" to 1,
            "active" to true,
            "signature" to "A_-0")
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()

    try {
      withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
      Assert.fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      Assert.assertTrue(e.message?.contains("uid missing") == true)
    }
  }

  @Test
  fun mapping_handlesNullValues() = runBlocking {
    val pass =
        mapOf(
            "uid" to uid,
            "kid" to "kid-nulls",
            "issuedAt" to 1_700_000_000L,
            "version" to null,
            "active" to null,
            "signature" to "A_-0",
            "lastScannedAt" to null,
            "revokedAt" to null)
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
    Assert.assertEquals(1, p.version)
    Assert.assertTrue(p.active)
    Assert.assertNull(p.lastScannedAt)
    Assert.assertNull(p.revokedAt)
  }

  @Test
  fun mapping_handlesInvalidTypeForIssuedAt() = runBlocking {
    val pass =
        mapOf(
            "uid" to uid,
            "kid" to "kid-bad-issued",
            "issuedAt" to "not-a-number",
            "version" to 1,
            "active" to true,
            "signature" to "A_-0")
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
    Assert.assertNull(p)
  }

  @Test
  fun mapping_trimsWhitespace() = runBlocking {
    val pass =
        mapOf(
            "uid" to "  $uid  ",
            "kid" to "  kid-trim  ",
            "issuedAt" to 1_700_000_000L,
            "version" to 1,
            "active" to true,
            "signature" to "  A_-0  ")
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
    Assert.assertEquals(uid, p.uid)
    Assert.assertEquals("kid-trim", p.kid)
    Assert.assertEquals("A_-0", p.signature)
  }

  @Test
  fun getUserPass_handlesDocumentWithoutPassField() = runBlocking {
    firestore.collection("user").document(uid).set(mapOf("other" to "data")).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
    Assert.assertNull(p)
  }

  @Test
  fun getUserPass_handlesPassFieldAsNonMap() = runBlocking {
    firestore.collection("user").document(uid).set(mapOf("pass" to "not-a-map")).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
    Assert.assertNull(p)
  }

  @Test
  fun mapping_handlesInvalidTimestampTypes() = runBlocking {
    val pass =
        mapOf(
            "uid" to uid,
            "kid" to "kid-bad-ts",
            "issuedAt" to 1_700_000_000L,
            "version" to 1,
            "active" to true,
            "signature" to "A_-0",
            "lastScannedAt" to "invalid",
            "revokedAt" to mapOf("foo" to "bar"))
    firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
    val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
    Assert.assertNull(p.lastScannedAt)
    Assert.assertNull(p.revokedAt)
  }

  @Test
  fun revokePass_worksOnNonExistentDocument() = runBlocking {
    clearUserPass(uid)
    val result = repository.revokePass(uid)
    Assert.assertTrue(result.isSuccess)
    val snap = firestore.collection("user").document(uid).get().await()
    val passMap = snap.get("pass") as? Map<*, *>
    Assert.assertNotNull(passMap)
    Assert.assertEquals(false, passMap?.get("active"))
  }

  @Test
  fun revokePass_preservesExistingFields() = runBlocking {
    writeValidPassAsNumbers(kid = "kid-preserve", version = 5)
    repository.revokePass(uid).getOrThrow()
    val snap = firestore.collection("user").document(uid).get().await()
    val passMap = snap.get("pass") as? Map<*, *> ?: error("missing pass")
    Assert.assertEquals("kid-preserve", passMap["kid"])
    Assert.assertEquals(5L, (passMap["version"] as? Number)?.toLong())
  }
}
