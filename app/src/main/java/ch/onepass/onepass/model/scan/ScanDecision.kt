package ch.onepass.onepass.model.scan
sealed class ScanDecision {
  data class Accepted(
      val ticketId: String? = null,
      val scannedAtSeconds: Long? = null,
      val remaining: Int? = null
  ) : ScanDecision()

  data class Rejected(val reason: Reason, val scannedAtSeconds: Long? = null) : ScanDecision()

  enum class Reason {
    UNREGISTERED,
    ALREADY_SCANNED,
    BAD_SIGNATURE,
    REVOKED,
    UNKNOWN
  }
}
