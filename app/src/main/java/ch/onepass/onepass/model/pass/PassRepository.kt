package ch.onepass.onepass.model.pass

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing a user's universal OnePass (stored at user/{uid}.pass). Generates/reads
 * the signed payload and exposes a reactive stream for UI.
 */
interface PassRepository {

  /** Emits the user's current Pass (or null if not created yet), updating in real-time. */
  fun getUserPass(uid: String): Flow<Pass?>

  /** Returns the existing Pass or generates a new signed one if missing. */
  suspend fun getOrCreateSignedPass(uid: String): Result<Pass>

  /** Marks the pass as revoked (sets active=false and revokedAt=now). */
  suspend fun revokePass(uid: String): Result<Unit>

  /** Updates lastScannedAt using the server timestamp after a successful validation. */
  suspend fun markScanned(uid: String): Result<Unit>
}
