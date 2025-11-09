package ch.onepass.onepass.model.scan

interface TicketScanRepository {
  suspend fun validateByPass(qrText: String, eventId: String): Result<ScanDecision>
}
