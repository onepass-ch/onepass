package ch.onepass.onepass.model.pass

import android.util.Base64
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Represents a signed OnePass user pass (used to generate a QR code).
 *
 * QR string format:
 * ```
 * onepass:user:v1.<base64url(payloadJson)>.<base64url(signature)>
 * ```
 *
 * Notes:
 * - `parseFromQr()` does NOT perform cryptographic signature verification. It only parses the QR
 *   string. Verification must happen on the server side.
 * - The test suite expects lenient parsing (e.g. allows iat <= 0 or ver <= 0), so domain validation
 *   happens in [isIncomplete], not in [parseFromQr].
 */
data class Pass(
    val uid: String = "",
    val kid: String = "",
    val issuedAt: Long = 0L, // seconds since epoch
    val lastScannedAt: Long? = null, // seconds since epoch
    val active: Boolean = true,
    val version: Int = 1,
    val revokedAt: Long? = null, // seconds since epoch
    val signature: String = "" // base64url-encoded signature (no padding)
) {

  companion object {
    /** Fixed prefix used for all OnePass QR codes */
    private const val QR_PREFIX = "onepass:user:v1."

    /** Compact JSON configuration (ignores unknown fields) */
    private val JSON = Json { ignoreUnknownKeys = true }

    /** Shared, immutable formatters (avoid reallocation per instance) */
    private val DATE_FMT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    private val TIME_FMT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).withZone(ZoneId.systemDefault())

    /**
     * Parses a OnePass QR string into a [Pass] instance.
     *
     * Expected format: `onepass:user:v1.<base64url(payloadJson)>.<base64url(signature)>`
     * - No cryptographic verification is done here.
     * - Invalid or missing fields throw exceptions as per test expectations.
     * - Domain-level validation (iat > 0, ver > 0, etc.) is handled via [isIncomplete].
     */
    fun parseFromQr(qrText: String): Result<Pass> = runCatching {
      require(qrText.startsWith(QR_PREFIX)) { "Bad prefix" }

      val token = qrText.removePrefix(QR_PREFIX)
      val parts = token.split('.', limit = 2)
      require(parts.size == 2) { "Bad token format" }

      val (payloadB64, signatureB64) = parts
      require(signatureB64.isNotBlank()) { "Empty signature" }

      val payloadBytes =
          Base64.decode(payloadB64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
      val payloadJson = String(payloadBytes, Charsets.UTF_8)

      val obj: JsonObject = JSON.parseToJsonElement(payloadJson).jsonObject

      val uid = obj["uid"]?.jsonPrimitive?.content ?: error("uid missing")
      val kid = obj["kid"]?.jsonPrimitive?.content ?: error("kid missing")
      val iat = obj["iat"]?.jsonPrimitive?.asLong() ?: error("iat missing")
      val ver = obj["ver"]?.jsonPrimitive?.asInt() ?: error("ver missing")

      Pass(uid = uid, kid = kid, issuedAt = iat, version = ver, signature = signatureB64)
    }
  }

  // ---------- Computed display properties ----------

  /** Human-readable issue date/time, or "Not issued" if invalid. */
  val displayIssuedAt: String
    get() =
        issuedAt
            .takeIf { it > 0L }
            ?.let {
              val inst = Instant.ofEpochSecond(it)
              "${DATE_FMT.format(inst)} • ${TIME_FMT.format(inst)}"
            } ?: "Not issued"

  /** Human-readable last scan date/time, or "Never scanned" if missing. */
  val displayLastScannedAt: String
    get() =
        lastScannedAt
            ?.takeIf { it > 0L }
            ?.let {
              val inst = Instant.ofEpochSecond(it)
              "${DATE_FMT.format(inst)} • ${TIME_FMT.format(inst)}"
            } ?: "Never scanned"

  /** Text status for UI display */
  val statusText: String
    get() =
        when {
          revokedAt != null -> "Revoked"
          !active -> "Inactive"
          else -> "Active"
        }

  /** True if the pass is currently valid (active and not revoked). */
  val isValidNow: Boolean
    get() = active && revokedAt == null

  /**
   * Determines whether the pass data is incomplete or invalid.
   *
   * A pass is incomplete if:
   * - Any required field is missing or empty
   * - IssuedAt <= 0
   * - Version <= 0
   * - Signature is not valid base64url (no padding)
   */
  val isIncomplete: Boolean
    get() =
        uid.isBlank() ||
            kid.isBlank() ||
            issuedAt <= 0L ||
            version <= 0 ||
            !isBase64UrlNoPad(signature)

  // ---------- QR payload (signed content) ----------

  /** Compact, deterministic JSON representation of the payload (for signature). */
  fun payloadJson(): String {
    val obj = buildJsonObject {
      put("uid", uid)
      put("kid", kid)
      put("iat", issuedAt)
      put("ver", version)
    }
    // Explicit serializer ensures stable field order (required by tests)
    return JSON.encodeToString(JsonObject.serializer(), obj)
  }

  /** Base64url (no padding) version of the payload JSON. */
  fun payloadB64Url(): String = b64UrlNoPad(payloadJson().toByteArray(Charsets.UTF_8))

  /** Full string to be encoded into the QR code. */
  val qrText: String
    get() = "$QR_PREFIX${payloadB64Url()}.$signature"

  // ---------- Private helpers ----------

  /** Checks if the string is a valid Base64URL (no padding, valid charset). */
  private fun isBase64UrlNoPad(s: String): Boolean {
    if (s.isBlank() || s.length < 4) return false
    if (!Regex("^[A-Za-z0-9_-]+$").matches(s)) return false
    return try {
      Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
      true
    } catch (_: IllegalArgumentException) {
      false
    }
  }

  /** Encodes bytes to Base64URL (no padding). */
  private fun b64UrlNoPad(bytes: ByteArray): String =
      Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

/** Safe number parsing extensions (for kotlinx-serialization 1.4.x). */
private fun JsonPrimitive.asLong(): Long = this.content.toLongOrNull() ?: error("not a long")

private fun JsonPrimitive.asInt(): Int = this.content.toIntOrNull() ?: error("not an int")
