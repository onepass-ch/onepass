package ch.onepass.onepass.model.pass

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import java.lang.reflect.Method
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PassRepositoryFirebaseEmulatorTest : PassFirestoreTestBase() {

    private lateinit var uid: String

    private lateinit var strictRepo: PassRepositoryFirebase
    private lateinit var strictMethod: Method

    private companion object {
        const val TIMEOUT: Long = 10_000
    }

    @Before
    override fun setUp() {
        super.setUp()
        runBlocking {
            // Ensure FirebaseApp is initialized before attempting any Firebase operations
            try {
                if (FirebaseApp.getApps(androidx.test.core.app.ApplicationProvider.getApplicationContext())
                        .isEmpty()) {
                    FirebaseApp.initializeApp(
                        androidx.test.core.app.ApplicationProvider.getApplicationContext())
                }
            } catch (e: Exception) {
                // FirebaseApp might already be initialized, continue
                android.util.Log.w("PassRepoTest", "Firebase init warning: ${e.message}")
            }

            auth.signInAnonymously().await()
            uid = getTestUserId("test")
            clearUserPass(uid)
        }
        strictRepo = PassRepositoryFirebase(firestore, functions)
        strictMethod =
            PassRepositoryFirebase::class
                .java
                .getDeclaredMethod("dataToPassStrict", Any::class.java)
                .apply { isAccessible = true }
    }

    // ---- HELPERS ------------------------------------------------------

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
                put("uid", kid)
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

    @Ignore("Cloud Functions not stable yet - issue #390")
    @Test
    fun getUserPass_updatesOnRevokeAndScan() = runBlocking {
        writeValidPassAsNumbers(active = true)
        val flow = repository.getUserPass(uid).distinctUntilChanged().filterNotNull()
        val initial = withTimeout(TIMEOUT) { flow.first() }
        Assert.assertTrue(initial.isValidNow)
        Assert.assertEquals("Active", initial.statusText)
        repository.revokePass(uid, "Test revocation").getOrThrow()
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

    @Ignore("Cloud Functions not stable yet - issue #390")
    @Test
    fun revokePass_setsInactive_andRevokedAt() = runBlocking {
        writeValidPassAsNumbers(active = true)
        repository.revokePass(uid, "Test revocation").getOrThrow()
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

    @Ignore("Cloud Functions not stable yet - issue #390")
    @Test
    fun revokePass_multipleCallsAreIdempotent() = runBlocking {
        writeValidPassAsNumbers(active = true)
        repository.revokePass(uid, "First revocation").getOrThrow()
        repository.revokePass(uid, "Second revocation").getOrThrow()
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
    fun getUserPass_throwsWhenUidIsBlank() = runBlocking {
        try {
            repository.getUserPass("")
            Assert.fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals("uid required", e.message)
        }
    }

    @Test
    fun getOrCreateSignedPass_throwsWhenUidIsBlank() = runBlocking {
        val result = repository.getOrCreateSignedPass("")
        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun revokePass_throwsWhenUidIsBlank() = runBlocking {
        val result = repository.revokePass("", "Test reason")
        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun revokePass_throwsWhenReasonIsBlank() = runBlocking {
        val result = repository.revokePass(uid, "")
        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun toPass_returnsNullWhenPassFieldMissing() = runBlocking {
        firestore.collection("user").document(uid).set(mapOf("email" to "test@example.com")).await()
        val result = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
        Assert.assertNull(result)
    }

    @Test
    fun mapping_handlesNullTimestampFields() = runBlocking {
        val pass =
            mapOf(
                "uid" to uid,
                "kid" to "kid-null",
                "issuedAt" to 1_700_000_000L,
                "signature" to "A_-0",
                "lastScannedAt" to null,
                "revokedAt" to null)
        firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
        val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
        Assert.assertNull(p.lastScannedAt)
        Assert.assertNull(p.revokedAt)
    }

    @Test
    fun mapping_handlesInvalidTypeForTimestamp() = runBlocking {
        val pass =
            mapOf(
                "uid" to uid,
                "kid" to "kid-invalid",
                "issuedAt" to 1_700_000_000L,
                "signature" to "A_-0",
                "lastScannedAt" to "invalid-string")
        firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
        val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
        Assert.assertNull(p.lastScannedAt)
    }

    @Test
    fun mapping_handlesWhitespaceInStringFields() = runBlocking {
        val pass =
            mapOf(
                "uid" to "  $uid  ",
                "kid" to "  kid-whitespace  ",
                "issuedAt" to 1_700_000_000L,
                "signature" to "  A_-0  ")
        firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
        val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).filterNotNull().first() }
        Assert.assertEquals(uid, p.uid)
        Assert.assertEquals("kid-whitespace", p.kid)
        Assert.assertEquals("A_-0", p.signature)
    }

    @Test
    fun mapping_handlesEmptyKid() = runBlocking {
        val pass = mapOf("uid" to uid, "kid" to "", "issuedAt" to 1_700_000_000L, "signature" to "A_-0")
        firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
        val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
        Assert.assertNull(p)
    }

    @Test
    fun mapping_handlesEmptySignature() = runBlocking {
        val pass =
            mapOf(
                "uid" to uid, "kid" to "kid-empty-sig", "issuedAt" to 1_700_000_000L, "signature" to "")
        firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
        val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
        Assert.assertNull(p)
    }

    @Test
    fun mapping_handlesNegativeVersion() = runBlocking {
        val pass =
            mapOf(
                "uid" to uid,
                "kid" to "kid-neg",
                "issuedAt" to 1_700_000_000L,
                "version" to -1,
                "signature" to "A_-0")
        firestore.collection("user").document(uid).set(mapOf("pass" to pass)).await()
        val p = withTimeout(TIMEOUT) { repository.getUserPass(uid).first() }
        Assert.assertNull(p)
    }

    // ---- NEW TESTS FOR THE PRIVATE MAPPER: dataToPassStrict --------------------

    private fun mapOfPairs(vararg pairs: Pair<String, Any?>): Map<String, Any?> = mapOf(*pairs)

    private fun invokeDataToPassStrict(data: Any?): Pass =
        strictMethod.invoke(strictRepo, data) as Pass

    @Test
    fun dataToPassStrict_happyPath_withNumbers() {
        val data =
            mapOfPairs(
                "uid" to "user-1",
                "kid" to "kid-123",
                "issuedAt" to 1_700_000_000L,
                "active" to true,
                "version" to 2,
                "signature" to "A_-0",
                "lastScannedAt" to 1_700_000_100L,
                "revokedAt" to 1_700_000_200L,
            )

        val p = invokeDataToPassStrict(data)
        Assert.assertEquals("user-1", p.uid)
        Assert.assertEquals("kid-123", p.kid)
        Assert.assertEquals(1_700_000_000L, p.issuedAt)
        Assert.assertEquals(1_700_000_100L, p.lastScannedAt)
        Assert.assertEquals(1_700_000_200L, p.revokedAt)
        Assert.assertTrue(p.active)
        Assert.assertEquals(2, p.version)
        Assert.assertEquals("A_-0", p.signature)
    }

    @Test
    fun dataToPassStrict_acceptsTimestampFields() {
        val data =
            mapOfPairs(
                "uid" to "u2",
                "kid" to "kid-ts",
                "issuedAt" to Timestamp(1_700_000_300L, 0),
                "active" to false,
                "version" to 3,
                "signature" to "A_-0",
                "lastScannedAt" to Timestamp(1_700_000_350L, 0),
                "revokedAt" to Timestamp(1_700_000_360L, 0),
            )

        val p = invokeDataToPassStrict(data)
        Assert.assertEquals(1_700_000_300L, p.issuedAt)
        Assert.assertEquals(1_700_000_350L, p.lastScannedAt)
        Assert.assertEquals(1_700_000_360L, p.revokedAt)
        Assert.assertFalse(p.active)
        Assert.assertEquals(3, p.version)
    }

    @Test
    fun dataToPassStrict_trimsWhitespace_onStrings() {
        val data =
            mapOfPairs(
                "uid" to "  user-3  ",
                "kid" to "  kid-ws  ",
                "issuedAt" to 1_700_000_400L,
                "signature" to "  A_-0  ")

        val p = invokeDataToPassStrict(data)
        Assert.assertEquals("user-3", p.uid)
        Assert.assertEquals("kid-ws", p.kid)
        Assert.assertEquals("A_-0", p.signature)
    }

    @Test
    fun dataToPassStrict_defaultsActiveAndVersion_whenMissing() {
        val data =
            mapOfPairs(
                "uid" to "user-4",
                "kid" to "kid-def",
                "issuedAt" to 1_700_000_500L,
                "signature" to "A_-0")

        val p = invokeDataToPassStrict(data)
        Assert.assertTrue(p.active) // default true
        Assert.assertEquals(1, p.version) // default 1
        Assert.assertEquals(1_700_000_500L, p.issuedAt)
    }

    @Test
    fun dataToPassStrict_handlesNullOptionals() {
        val data =
            mapOfPairs(
                "uid" to "user-5",
                "kid" to "kid-null",
                "issuedAt" to 1_700_000_600L,
                "signature" to "A_-0",
                "lastScannedAt" to null,
                "revokedAt" to null)

        val p = invokeDataToPassStrict(data)
        Assert.assertNull(p.lastScannedAt)
        Assert.assertNull(p.revokedAt)
    }

    @Test
    fun dataToPassStrict_coercesNumberTypes() {
        val data =
            mapOfPairs(
                "uid" to "user-6",
                "kid" to "kid-num",
                "issuedAt" to 1_700_000_700, // Int
                "version" to 2.0, // Double
                "active" to true,
                "signature" to "A_-0",
                "lastScannedAt" to 1_700_000_710.0, // Double
                "revokedAt" to 1_700_000_720 // Int
            )

        val p = invokeDataToPassStrict(data)
        Assert.assertEquals(1_700_000_700L, p.issuedAt)
        Assert.assertEquals(2, p.version)
        Assert.assertEquals(1_700_000_710L, p.lastScannedAt)
        Assert.assertEquals(1_700_000_720L, p.revokedAt)
    }

    @Test
    fun dataToPassStrict_issuedAtMissing_becomesZero() {
        val data = mapOfPairs("uid" to "user-7", "kid" to "kid-no-issued", "signature" to "A_-0")

        val p = invokeDataToPassStrict(data)
        Assert.assertEquals(0L, p.issuedAt)
    }
}