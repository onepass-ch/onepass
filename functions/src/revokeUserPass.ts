// functions/src/revokeUserPass.ts
import * as functions from "firebase-functions/v1";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

const db = admin.firestore();

/**
 * Revokes a user's pass, preventing them from entering events.
 *
 * Security:
 * 1. Authenticates the caller (must be logged in)
 * 2. Authorizes the caller (must have ADMIN role)
 * 3. Revokes the target user's pass
 * 4. Logs the revocation to audit trail
 *
 * @param data.targetUid - The user ID whose pass should be revoked
 * @param data.reason - Reason for revocation (e.g., "Fraudulent activity", "Refund requested")
 * @param context.auth - Firebase Auth context containing the admin's UID
 *
 * @returns Object with success status
 *
 * @throws HttpsError
 * - "unauthenticated": Caller not logged in
 * - "invalid-argument": Missing targetUid or reason
 * - "permission-denied": Caller doesn't have ADMIN role
 * - "not-found": Target user doesn't exist or has no pass
 *
 * @example
 * // Success response
 * { success: true }
 */
export const revokeUserPass = functions.https.onCall(
  async (data: any, context: any) => {
    // ============================================================================
    // 1. AUTHENTICATION CHECK
    // ============================================================================
    const adminUid = context.auth?.uid;
    if (!adminUid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentication required"
      );
    }

    const { targetUid, reason } = data;
    if (!targetUid || !reason) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "targetUid and reason required"
      );
    }

    logger.info(
      `Admin ${adminUid} attempting to revoke pass for user ${targetUid}`
    );

    // ============================================================================
    // 2. AUTHORIZATION CHECK
    // ============================================================================
    const adminDoc = await db.collection("users").doc(adminUid).get();
    const adminData = adminDoc.data();
    const adminRole = adminData?.role;

    if (adminRole !== "ADMIN") {
      logger.warn(
        `Unauthorized revocation attempt by ${adminUid} (role: ${adminRole})`
      );
      throw new functions.https.HttpsError(
        "permission-denied",
        "Only admins can revoke passes"
      );
    }

    // ============================================================================
    // 3. VERIFY TARGET USER EXISTS AND HAS A PASS
    // ============================================================================
    const targetUserDoc = await db.collection("users").doc(targetUid).get();

    if (!targetUserDoc.exists) {
      throw new functions.https.HttpsError(
        "not-found",
        `User ${targetUid} not found`
      );
    }

    const targetUserData = targetUserDoc.data();
    const pass = targetUserData?.pass;

    if (!pass) {
      throw new functions.https.HttpsError(
        "not-found",
        `User ${targetUid} has no pass`
      );
    }

    // Check if already revoked
    if (!pass.active || pass.revokedAt) {
      logger.info(`Pass for user ${targetUid} is already revoked`);
      return {
        success: true,
        message: "Pass was already revoked",
      };
    }

    // ============================================================================
    // 4. REVOKE PASS AND LOG TO AUDIT TRAIL
    // ============================================================================
    const now = Date.now();
    const revokedAtSeconds = Math.floor(now / 1000);

    try {
      await db.runTransaction(async (transaction) => {
        // Update user's pass
        transaction.update(db.collection("users").doc(targetUid), {
          "pass.active": false,
          "pass.revokedAt": revokedAtSeconds,
          "pass.revokedBy": adminUid,
          "pass.revocationReason": reason,
        });

        // Log to audit trail
        transaction.set(db.collection("validations").doc(), {
          uid: targetUid,
          eventId: null,
          ticketId: null,
          result: "pass_revoked",
          reason: reason,
          revokedBy: adminUid,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
        });
      });

      logger.info(
        ` Pass revoked for user ${targetUid} by admin ${adminUid}. Reason: ${reason}`
      );

      return {
        success: true,
        message: "Pass revoked successfully",
      };
    } catch (error: any) {
      logger.error(`Failed to revoke pass for user ${targetUid}:`, error);
      throw new functions.https.HttpsError(
        "internal",
        `Failed to revoke pass: ${error.message}`
      );
    }
  }
);