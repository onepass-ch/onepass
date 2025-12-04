package ch.onepass.onepass.model.pass

import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for interacting with user passes (OnePass). The implementation (e.g.,
 * PassRepositoryFirebase) handles Firestore and Cloud Functions.
 */
interface PassRepository {

  /**
   * Returns a real-time flow of the user's pass. Emits updates automatically when Firestore data
   * changes.
   */
  fun getUserPass(uid: String): Flow<Pass?>

  /**
   * Returns the existing pass or creates a new one via the Cloud Function `generateUserPass`. The
   * function returns the server-signed pass immediately.
   */
  suspend fun getOrCreateSignedPass(uid: String): Result<Pass>

  /**
   * Revokes a user's pass via secure Cloud Function (admin only). Sets active=false, revokedAt,
   * revokedBy, and logs to audit trail.
   *
   * @param targetUid The user ID whose pass should be revoked
   * @param reason Reason for revocation (e.g., "Fraudulent activity", "Refund requested")
   * @return Result indicating success or failure
   */
  suspend fun revokePass(targetUid: String, reason: String): Result<Unit>
}
