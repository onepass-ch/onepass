package ch.onepass.onepass.model.scan

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Calls Cloud Function to validate a QR pass for an event.
 *
 * This class does not parse the QR, does not check Firestore. It only sends the raw QR to backend
 * and maps the response.
 */
class TicketScanRepositoryFirebase : TicketScanRepository {

    private val functions = Firebase.functions

    override suspend fun validateByPass(qrText: String, eventId: String): Result<ScanDecision> =
        runCatching {

            // Verify user authentication before calling Cloud Function
            val currentUser =
                FirebaseAuth.getInstance().currentUser
                    ?: throw IllegalStateException("Please login to scan tickets")

            // Force refresh token to ensure it's valid
            try {
                currentUser.getIdToken(true).await()
            } catch (e: Exception) {
                // Distinguish between network errors and auth errors
                when (e) {
                    is FirebaseNetworkException,
                    is IOException,
                    is UnknownHostException,
                    is SocketTimeoutException -> {
                        throw IOException("Network connection failed. Please check your internet connection.")
                    }
                    is FirebaseAuthException -> {
                        throw IllegalStateException("Session expired - please login again")
                    }
                    else -> {
                        // Check if message contains network-related keywords
                        val message = e.message?.lowercase() ?: ""
                        if (message.contains("network") ||
                            message.contains("connection") ||
                            message.contains("internet") ||
                            message.contains("timeout")) {
                            throw IOException("Network connection failed. Please check your internet connection.")
                        }
                        throw IllegalStateException("Session expired. Please login again")
                    }
                }
            }

            // Basic client-side guardrails - prevents useless requests
            require(qrText.isNotBlank()) { "QR empty" }
            require(eventId.isNotBlank()) { "Event ID empty" }

            // Payload sent to Cloud Function (no parsing here)
            val payload = mapOf("qrText" to qrText, "eventId" to eventId)

            // Calls backend function: verifies signature + finds ticket + logs scan
            val result = functions.getHttpsCallable(FN_VALIDATE).call(payload).await()

            // Cloud Function always returns a JSON map
            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?> ?: error("Unexpected CF response format")

            val status =
                (data[KEY_STATUS] as? String)?.lowercase() ?: error("Missing status in CF response")

            // Accepted flow: backend confirms first scan + updates database
            if (status == "accepted") {
                return@runCatching ScanDecision.Accepted(
                    ticketId = data[KEY_TICKET_ID] as? String,
                    scannedAtSeconds = (data[KEY_SCANNED_AT] as? Number)?.toLong(),
                    remaining = (data[KEY_REMAINING] as? Number)?.toInt())
            }

            // Rejected flow: backend explains reason (unregistered / already scanned / etc.)
            val reasonStr = (data[KEY_REASON] as? String)?.uppercase() ?: "UNKNOWN"
            val reason =
                runCatching { ScanDecision.Reason.valueOf(reasonStr) }
                    .getOrDefault(ScanDecision.Reason.UNKNOWN)
            ScanDecision.Rejected(
                reason = reason, scannedAtSeconds = (data[KEY_SCANNED_AT] as? Number)?.toLong())
        }

    private companion object {
        const val FN_VALIDATE = "validateEntryByPassV2"

        const val KEY_STATUS = "status"
        const val KEY_REASON = "reason"
        const val KEY_TICKET_ID = "ticketId"
        const val KEY_SCANNED_AT = "scannedAt"
        const val KEY_REMAINING = "remaining"
    }
}