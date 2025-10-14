package ch.onepass.onepass.model.pass

import android.util.Base64
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Pass(
    val uid: String = "",
    val kid: String = "",
    val issuedAt: Long = 0L,
    val lastScannedAt: Long? = null,
    val active: Boolean = true,
    val version: Int = 1,
    val revokedAt: Long? = null,
    val signature: String = ""
) {
  // ---------- Local constants ----------

  companion object {
    /** Unique prefix for the OnePass QR code */
    private const val QR_PREFIX = "onepass:user:v1."

    /** Date/time formats for human-readable display */
    private val DATE_FMT =
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    private val TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).withZone(ZoneId.systemDefault())
  }
  // ---------- Computed properties ----------

  /** Human-readable issue date of the pass */
  val displayIssuedAt: String
    get() =
        issuedAt
            .takeIf { it > 0L }
            ?.let {
              val inst = Instant.ofEpochSecond(it)
              "${DATE_FMT.format(inst)} • ${TIME_FMT.format(inst)}"
            } ?: "Not issued"

  /** Human-readable last scanned date of the pass */
  val displayLastScannedAt: String
    get() =
        lastScannedAt
            ?.takeIf { it > 0L }
            ?.let {
              val inst = Instant.ofEpochSecond(it)
              "${DATE_FMT.format(inst)} • ${TIME_FMT.format(inst)}"
            } ?: "Never scanned"

  /** Logical status (active, revoked, etc.) */
  val statusText: String
    get() =
        when {
          revokedAt != null -> "Revoked"
          !active -> "Inactive"
          else -> "Active"
        }

  /** Checks if the pass is currently valid */
  val isValidNow: Boolean
    get() = active && revokedAt == null

  /** Checks if the pass contains all required data */
  val isIncomplete: Boolean
    get() =
        uid.isBlank() ||
            kid.isBlank() ||
            issuedAt <= 0L ||
            version <= 0 ||
            !isBase64UrlNoPad(signature)

  private fun isBase64UrlNoPad(s: String): Boolean {
    if (s.isBlank() || !Regex("^[A-Za-z0-9_-]+$").matches(s)) return false
    return try {
      Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
      true
    } catch (_: IllegalArgumentException) {
      false
    }
  }

  /** Generates the JSON payload content (before encoding) */
  fun payloadJson(): String = """{"uid":"$uid","kid":"$kid","iat":$issuedAt,"ver":$version}"""

  /** Base64url encoding of the JSON payload */
  fun payloadB64Url(): String = b64UrlNoPad(payloadJson().toByteArray(Charsets.UTF_8))

  /** Full string to display inside the QR code */
  val qrText: String
    get() = "$QR_PREFIX${payloadB64Url()}.$signature"

  // ---------- Private utils ----------
  private fun b64UrlNoPad(bytes: ByteArray): String =
      Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
