package ch.onepass.onepass.model.pass

import android.util.Base64
import java.security.SecureRandom
import org.junit.Assert.*
import org.junit.Test

/** Unit tests for the Pass data class. No backticked function names (Android-safe). */
class PassDataTest {

  private val rng = SecureRandom()
  private val urlAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-"

  /** Generates a random base64url-encoded signature (64 bytes). */
  private fun randomSigB64Url(): String {
    val bytes = ByteArray(64).also { rng.nextBytes(it) }
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }

  /** Generates a random URL-safe token. */
  private fun randomAlphaNum(n: Int): String {
    val sb = StringBuilder(n)
    repeat(n) {
      val ch = urlAlphabet[rng.nextInt(urlAlphabet.length)]
      sb.append(ch)
    }
    return sb.toString()
  }

  /** Creates a minimal valid Pass ready for QR generation. */
  private fun createValidPass(
    uid: String = "user-123",
    kid: String = "kid-001",
    issuedAt: Long = 1_700_000_000L,
    version: Int = 1,
    signature: String = randomSigB64Url(),
    active: Boolean = true,
    revokedAt: Long? = null,
    lastScannedAt: Long? = null,
  ): Pass =
    Pass(
      uid = uid,
      kid = kid,
      issuedAt = issuedAt,
      lastScannedAt = lastScannedAt,
      active = active,
      version = version,
      revokedAt = revokedAt,
      signature = signature)

  // ---------------------------------------------------------------
  // Basic validation
  // ---------------------------------------------------------------

  @Test
  fun isIncomplete_trueWhenRequiredFieldsMissing() {
    val p1 = Pass() // all defaults
    assertTrue(p1.isIncomplete)

    val p2 = Pass(uid = "u", kid = "", issuedAt = 1700000000L, version = 1, signature = "x")
    assertTrue(p2.isIncomplete)

    val p3 = Pass(uid = "", kid = "k", issuedAt = 1700000000L, version = 1, signature = "x")
    assertTrue(p3.isIncomplete)

    val p4 = Pass(uid = "u", kid = "k", issuedAt = 0L, version = 1, signature = "x")
    assertTrue(p4.isIncomplete)

    val p5 = Pass(uid = "u", kid = "k", issuedAt = 1700000000L, version = 0, signature = "x")
    assertTrue(p5.isIncomplete)
  }

  @Test
  fun isIncomplete_trueForNegativeVersionOrIssuedAt() {
    val p1 = createValidPass(version = -1)
    assertTrue(p1.isIncomplete)

    val p2 = createValidPass(issuedAt = -10L)
    assertTrue(p2.isIncomplete)
  }

  @Test
  fun isIncomplete_falseForValidPass() {
    val pass = createValidPass()
    assertFalse(pass.isIncomplete)
  }

  @Test
  fun isIncomplete_trueForInvalidSignatureChars() {
    val badSig = "abc+def/ghi==" // invalid base64url chars and padding
    val pass = createValidPass(signature = badSig)
    assertTrue(pass.isIncomplete)
  }

  @Test
  fun isIncomplete_trueForPaddedSignature() {
    val padded = "abcd=="
    val pass = createValidPass(signature = padded)
    assertTrue(pass.isIncomplete)
  }

  @Test
  fun isIncomplete_trueForWhitespaceInSignature() {
    val withSpace = "ab cd"
    val pass = createValidPass(signature = withSpace)
    assertTrue(pass.isIncomplete)
  }

  @Test
  fun isIncomplete_trueForEmptySignature() {
    val empty = ""
    val pass = createValidPass(signature = empty)
    assertTrue(pass.isIncomplete)
  }

  @Test
  fun isIncomplete_trueForTooShortSignature() {
    // isBase64UrlNoPad exige length >= 4
    val tooShort = "A_-"
    val pass = createValidPass(signature = tooShort)
    assertTrue(pass.isIncomplete)
  }

  @Test
  fun isIncomplete_falseForDashUnderscoreAlphabet() {
    // Minimal but valid base64url that decodes without padding in Android Base64
    val okSig = "A_-0"
    val pass = createValidPass(signature = okSig)
    assertFalse(pass.isIncomplete)
  }

  // ---------------------------------------------------------------
  // Payload & encoding
  // ---------------------------------------------------------------

  @Test
  fun payloadJson_expectedStructure() {
    val pass =
      createValidPass(uid = "user-123", kid = "kid-001", issuedAt = 1700000000L, version = 1)
    val expected = """{"uid":"user-123","kid":"kid-001","iat":1700000000,"ver":1}"""
    assertEquals(expected, pass.payloadJson())
  }

  @Test
  fun payloadB64Url_encodesAndDecodesDeterministically() {
    val pass = createValidPass(uid = "u", kid = "k", issuedAt = 1700000000L, version = 1)
    val b64 = pass.payloadB64Url()

    val decodedBytes = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    val decoded = String(decodedBytes, Charsets.UTF_8)
    assertEquals(pass.payloadJson(), decoded)

    // Same payload fields -> same payloadB64Url (signature shouldn't affect payload encoding)
    val pass2 = createValidPass(uid = "u", kid = "k", issuedAt = 1700000000L, version = 1)
    assertEquals(b64, pass2.payloadB64Url())
  }

  @Test
  fun payloadB64Url_hasNoPadding() {
    val pass = createValidPass()
    val b64 = pass.payloadB64Url()
    assertFalse(b64.contains("="))
  }

  @Test
  fun payloadB64Url_isUrlSafeAlphabetOnly() {
    val pass = createValidPass()
    val b64 = pass.payloadB64Url()
    assertTrue(Regex("^[A-Za-z0-9_-]+$").matches(b64))
  }

  // ---------------------------------------------------------------
  // QR string
  // ---------------------------------------------------------------

  @Test
  fun qrText_hasCorrectPrefixAndStructure() {
    val pass = createValidPass()
    val qr = pass.qrText

    assertTrue(qr.startsWith("onepass:user:v1."))

    val afterPrefix = qr.removePrefix("onepass:user:v1.")
    val parts = afterPrefix.split('.')
    assertEquals("QR should have payload and signature separated by '.'", 2, parts.size)

    val (payloadB64, sigB64) = parts
    assertTrue(payloadB64.isNotBlank())
    assertTrue(sigB64.isNotBlank())
  }

  @Test
  fun qrText_payloadAndSignatureIntegrity() {
    val pass = createValidPass()
    val afterPrefix = pass.qrText.removePrefix("onepass:user:v1.")
    val (payloadB64, sigB64) = afterPrefix.split('.')

    // Signature in QR must equal the pass signature
    assertEquals(pass.signature, sigB64)

    // Payload must decode to payloadJson and must not be padded
    assertFalse(payloadB64.contains("="))
    val decoded =
      String(
        Base64.decode(payloadB64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
        Charsets.UTF_8)
    assertEquals(pass.payloadJson(), decoded)
  }

  // ---------------------------------------------------------------
  // parseFromQr() coverage
  // ---------------------------------------------------------------

  @Test
  fun parseFromQr_roundTrip_valid() {
    val original = createValidPass()
    val parsed = Pass.parseFromQr(original.qrText).getOrThrow()
    assertEquals(original.uid, parsed.uid)
    assertEquals(original.kid, parsed.kid)
    assertEquals(original.issuedAt, parsed.issuedAt)
    assertEquals(original.version, parsed.version)
    assertEquals(original.signature, parsed.signature)
  }

  @Test
  fun parseFromQr_roundTrip_randomized() {
    repeat(20) {
      val original = createValidPass(
        uid = randomAlphaNum(8),
        kid = randomAlphaNum(6),
        signature = randomSigB64Url()
      )
      val parsed = Pass.parseFromQr(original.qrText).getOrThrow()
      assertEquals(original.uid, parsed.uid)
      assertEquals(original.kid, parsed.kid)
      assertEquals(original.issuedAt, parsed.issuedAt)
      assertEquals(original.version, parsed.version)
      assertEquals(original.signature, parsed.signature)
      assertFalse(parsed.isIncomplete)
    }
  }

  @Test
  fun parseFromQr_defaultsForOmittedStateFields() {
    val p = createValidPass()
    val parsed = Pass.parseFromQr(p.qrText).getOrThrow()
    // These fields are not carried in the QR payload => default values of data class
    assertTrue(parsed.active)
    assertNull(parsed.revokedAt)
    assertNull(parsed.lastScannedAt)
  }

  @Test
  fun parseFromQr_failsOnBadPrefix() {
    val p = createValidPass()
    val bad = p.qrText.replace("onepass:user:v1.", "badprefix:")
    val ex = Pass.parseFromQr(bad).exceptionOrNull()
    assertNotNull(ex)
    assertTrue(ex!!.message?.contains("Bad prefix") == true)
  }

  @Test
  fun parseFromQr_failsOnBadTokenFormat_noDot() {
    val p = createValidPass()
    val onlyPayload = "onepass:user:v1.${p.payloadB64Url()}" // missing ".<signature>"
    val ex = Pass.parseFromQr(onlyPayload).exceptionOrNull()
    assertNotNull(ex)
    assertTrue(ex!!.message?.contains("Bad token format") == true)
  }

  @Test
  fun parseFromQr_failsOnEmptySignature() {
    val p = createValidPass()
    val qr = "onepass:user:v1.${p.payloadB64Url()}."
    val ex = Pass.parseFromQr(qr).exceptionOrNull()
    assertNotNull(ex)
    assertTrue(ex!!.message?.contains("Empty signature") == true)
  }

  @Test
  fun parseFromQr_failsOnBadBase64Payload_typeCheck() {
    val p = createValidPass()
    val qr = "onepass:user:v1.@@@.${p.signature}" // invalid base64url
    val ex = Pass.parseFromQr(qr).exceptionOrNull()
    assertNotNull(ex)
    // Base64.decode throws IllegalArgumentException, which is surfaced by runCatching
    assertTrue(ex is IllegalArgumentException)
  }

  @Test
  fun parseFromQr_failsOnMissingFields_uid() {
    // Build a payload JSON without "uid"
    val json = """{"kid":"k","iat":1700000000,"ver":1}"""
    val payloadB64 =
      Base64.encodeToString(
        json.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
      )
    val qr = "onepass:user:v1.$payloadB64.${randomSigB64Url()}"
    val ex = Pass.parseFromQr(qr).exceptionOrNull()
    assertNotNull(ex)
    assertTrue(ex!!.message?.contains("uid missing") == true)
  }

  @Test
  fun parseFromQr_failsOnMissingFields_kid() {
    val json = """{"uid":"u","iat":1700000000,"ver":1}"""
    val payloadB64 =
      Base64.encodeToString(
        json.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
      )
    val qr = "onepass:user:v1.$payloadB64.${randomSigB64Url()}"
    val ex = Pass.parseFromQr(qr).exceptionOrNull()
    assertNotNull(ex)
    assertTrue(ex!!.message?.contains("kid missing") == true)
  }

  @Test
  fun parseFromQr_failsOnMissingFields_iat() {
    val json = """{"uid":"u","kid":"k","ver":1}"""
    val payloadB64 =
      Base64.encodeToString(
        json.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
      )
    val qr = "onepass:user:v1.$payloadB64.${randomSigB64Url()}"
    val ex = Pass.parseFromQr(qr).exceptionOrNull()
    assertNotNull(ex)
    assertTrue(ex!!.message?.contains("iat missing") == true)
  }

  @Test
  fun parseFromQr_failsOnMissingFields_ver() {
    val json = """{"uid":"u","kid":"k","iat":1700000000}"""
    val payloadB64 =
      Base64.encodeToString(
        json.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
      )
    val qr = "onepass:user:v1.$payloadB64.${randomSigB64Url()}"
    val ex = Pass.parseFromQr(qr).exceptionOrNull()
    assertNotNull(ex)
    assertTrue(ex!!.message?.contains("ver missing") == true)
  }

  @Test
  fun parseFromQr_parsedWithInvalidDomainValues_isIncomplete_verNonPositif() {
    // ver <= 0 est parsé, mais devrait rendre l'objet incohérent
    val json = """{"uid":"u","kid":"k","iat":1700000000,"ver":0}"""
    val payloadB64 =
      Base64.encodeToString(
        json.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
      )
    val qr = "onepass:user:v1.$payloadB64.${randomSigB64Url()}"
    val parsed = Pass.parseFromQr(qr).getOrThrow()
    assertTrue(parsed.isIncomplete)
  }

  @Test
  fun parseFromQr_parsedWithInvalidDomainValues_isIncomplete_iatNonPositif() {
    // iat <= 0 est parsé, mais devrait rendre l'objet incohérent
    val json = """{"uid":"u","kid":"k","iat":0,"ver":1}"""
    val payloadB64 =
      Base64.encodeToString(
        json.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
      )
    val qr = "onepass:user:v1.$payloadB64.${randomSigB64Url()}"
    val parsed = Pass.parseFromQr(qr).getOrThrow()
    assertTrue(parsed.isIncomplete)
  }

  // ---------------------------------------------------------------
  // Display & status
  // ---------------------------------------------------------------

  @Test
  fun displayIssuedAt_notIssuedWhenZero() {
    val pass = createValidPass(issuedAt = 0L)
    assertEquals("Not issued", pass.displayIssuedAt)
  }

  @Test
  fun displayLastScannedAt_neverScannedWhenNull() {
    val pass = createValidPass(lastScannedAt = null)
    assertEquals("Never scanned", pass.displayLastScannedAt)
  }

  @Test
  fun displayLastScannedAt_zeroMeansNeverScanned() {
    val pass = createValidPass(lastScannedAt = 0L)
    assertEquals("Never scanned", pass.displayLastScannedAt)
  }

  @Test
  fun displayIssuedAt_hasReadableDateWhenPositive() {
    val pass = createValidPass(issuedAt = 1_700_000_000L) // 2023-11-14 UTC
    val text = pass.displayIssuedAt
    assertFalse(text.contains("Not issued"))
    // Avoid locale flakiness: just check it contains a 4-digit year and the separator " • "
    assertTrue(text.contains(Regex("\\d{4}")))
    assertTrue(text.contains(" • "))
  }

  @Test
  fun displayLastScannedAt_hasReadableDateWhenPositive() {
    val pass = createValidPass(lastScannedAt = 1_700_000_000L)
    val text = pass.displayLastScannedAt
    assertFalse(text.contains("Never scanned"))
    assertTrue(text.contains(Regex("\\d{4}")))
    assertTrue(text.contains(" • "))
  }

  @Test
  fun statusText_matchesState() {
    val active = createValidPass(active = true, revokedAt = null)
    assertEquals("Active", active.statusText)

    val inactive = createValidPass(active = false, revokedAt = null)
    assertEquals("Inactive", inactive.statusText)

    val revoked = createValidPass(active = true, revokedAt = 1700000001L)
    assertEquals("Revoked", revoked.statusText)
  }

  @Test
  fun statusText_revokedOverridesInactive() {
    val pass = createValidPass(active = false, revokedAt = 1700000001L)
    assertEquals("Revoked", pass.statusText)
  }

  @Test
  fun statusText_revokedAtZeroStillCountsAsRevoked() {
    val pass = createValidPass(revokedAt = 0L)
    // Current implementation treats any non-null revokedAt as revoked
    assertEquals("Revoked", pass.statusText)
  }

  @Test
  fun isValidNow_trueOnlyWhenActiveAndNotRevoked() {
    val valid = createValidPass(active = true, revokedAt = null)
    assertTrue(valid.isValidNow)

    val inactive = createValidPass(active = false, revokedAt = null)
    assertFalse(inactive.isValidNow)

    val revoked = createValidPass(active = true, revokedAt = 1700000123L)
    assertFalse(revoked.isValidNow)
  }
}
