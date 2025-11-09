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
   * Revokes (deactivates) the user's pass. Sets active=false and revokedAt=serverTimestamp() in
   * Firestore.
   */
  suspend fun revokePass(uid: String): Result<Unit>
}
