package ch.onepass.onepass.model.pass

import android.util.Base64
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class Pass(
    val uid: String = "",
    val kid: String = "",
    val issuedAt: Long = 0L,
    val lastScannedAt: Long? = null,
    val active: Boolean = true,
    val version: Int = 1,
    val revokedAt: Long? = null,
    val signature: String = "" // base64url(SIGN(payloadJsonBytes))
) {

    companion object {
        /** Unique prefix for the OnePass QR code */
        private const val QR_PREFIX = "onepass:user:v1."

        /** JSON instance (compact; ignore unknown on parse) */
        private val JSON = Json { ignoreUnknownKeys = true }

        /**
         * Parses a OnePass QR string into a Pass.
         * Format: onepass:user:v1.<base64url(payloadJson)>.<base64url(signature)>
         */
        fun parseFromQr(qrText: String): Result<Pass> = runCatching {
            require(qrText.startsWith(QR_PREFIX)) { "Bad prefix" }

            val token = qrText.removePrefix(QR_PREFIX)
            val parts = token.split('.', limit = 2)
            require(parts.size == 2) { "Bad token format" }

            val (payloadB64, signatureB64) = parts
            require(signatureB64.isNotBlank()) { "Empty signature" }

            val payloadBytes = Base64.decode(
                payloadB64,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val payloadJson = String(payloadBytes, Charsets.UTF_8)

            val obj: JsonObject = JSON.parseToJsonElement(payloadJson).jsonObject

            val uid = obj["uid"]?.jsonPrimitive?.content ?: error("uid missing")
            val kid = obj["kid"]?.jsonPrimitive?.content ?: error("kid missing")
            val iat = obj["iat"]?.jsonPrimitive?.asLong() ?: error("iat missing")
            val ver = obj["ver"]?.jsonPrimitive?.asInt() ?: error("ver missing")

            Pass(
                uid = uid,
                kid = kid,
                issuedAt = iat,
                version = ver,
                signature = signatureB64
            )
        }
    }

    /** Date/time formats for human-readable display */
    private val DATE_FMT =
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    private val TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

    // ---------- Computed properties ----------

    val displayIssuedAt: String
        get() =
            issuedAt.takeIf { it > 0L }?.let {
                val inst = Instant.ofEpochSecond(it)
                "${DATE_FMT.format(inst)} • ${TIME_FMT.format(inst)}"
            } ?: "Not issued"

    val displayLastScannedAt: String
        get() =
            lastScannedAt?.takeIf { it > 0L }?.let {
                val inst = Instant.ofEpochSecond(it)
                "${DATE_FMT.format(inst)} • ${TIME_FMT.format(inst)}"
            } ?: "Never scanned"

    val statusText: String
        get() = when {
            revokedAt != null -> "Revoked"
            !active -> "Inactive"
            else -> "Active"
        }

    val isValidNow: Boolean get() = active && revokedAt == null

    val isIncomplete: Boolean
        get() = uid.isBlank() ||
                kid.isBlank() ||
                issuedAt <= 0L ||
                version <= 0 ||
                !isBase64UrlNoPad(signature)

    // ---------- QR payload (signed content) ----------

    /** Compact & stable JSON (no DTO/serializer needed) */
    fun payloadJson(): String {
        val obj = buildJsonObject {
            put("uid", uid)
            put("kid", kid)
            put("iat", issuedAt)
            put("ver", version)
        }
        // ⚠️ Avec kotlinx-serialization 1.4.1, il faut passer un serializer explicite
        return JSON.encodeToString(JsonObject.serializer(), obj)
    }

    /** Base64url encoding of the JSON payload (no padding) */
    fun payloadB64Url(): String =
        b64UrlNoPad(payloadJson().toByteArray(Charsets.UTF_8))

    /** Full string to display inside the QR code */
    val qrText: String
        get() = "$QR_PREFIX${payloadB64Url()}.$signature"

    // ---------- Private utils ----------

    private fun isBase64UrlNoPad(s: String): Boolean {
        if (s.isBlank() || s.length < 4) return false
        if (!Regex("^[A-Za-z0-9_-]+$").matches(s)) return false
        return try {
            Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING); true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun b64UrlNoPad(bytes: ByteArray): String =
        Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
}

/* Safe number parsing extensions for kotlinx-serialization 1.4.1 */
private fun JsonPrimitive.asLong(): Long =
    this.content.toLongOrNull() ?: error("not a long")

private fun JsonPrimitive.asInt(): Int =
    this.content.toIntOrNull() ?: error("not an int")
