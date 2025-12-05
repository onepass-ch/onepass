import * as functions from "firebase-functions/v1";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import { verifySignature } from "./crypto";

const db = admin.firestore();

/**
 * Validates a user's entry to an event by verifying their cryptographically signed QR pass.
 *
 * Security Flow:
 * 1. Authenticates the scanner (must be logged in)
 * 2. Authorizes the scanner (must have STAFF, SECURITY, ADMIN, or ORGANIZER role)
 * 3. Parses and validates the QR code format
 * 4. Verifies the Ed25519 cryptographic signature
 * 5. Checks the pass status (active, not revoked)
 * 6. Finds the corresponding ticket for the event
 * 7. Validates the ticket hasn't been redeemed
 * 8. Prevents replay attacks (30-second window)
 * 9. Atomically updates ticket, pass, event counters, and validation logs
 *
 * @param data.qrText - Raw QR code string in format: "onepass:user:v1.<payload>.<signature>"
 * @param data.eventId - The event ID to validate entry for
 * @param context.auth - Firebase Auth context containing the scanner's UID
 *
 * @returns Object with status ("accepted" | "rejected"), reason, ticketId, scannedAt, and remaining capacity
 *
 * @throws HttpsError
 * - "unauthenticated": Scanner not logged in
 * - "invalid-argument": Missing qrText or eventId
 * - "permission-denied": Scanner doesn't have required role
 *
 * @example
 * // Accepted response
 * {
 *   status: "accepted",
 *   ticketId: "T-12345",
 *   scannedAt: 1701234567,
 *   remaining: 42
 * }
 *
 * @example
 * // Rejected response
 * {
 *   status: "rejected",
 *   reason: "ALREADY_SCANNED",
 *   scannedAt: 1701234000
 * }
 */
export const validateEntryByPass = functions.https.onCall(
  async (data: any, context: any) => {
    // ============================================================================
    // 1. AUTHENTICATION CHECK
    // ============================================================================
    const scannerUid = context.auth?.uid;
    if (!scannerUid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Scanner authentication required"
      );
    }

    const { qrText, eventId } = data;
    if (!qrText || !eventId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "qrText and eventId required"
      );
    }

    // ============================================================================
    // 2. AUTHORIZATION CHECK
    // ============================================================================
    const scannerDoc = await db.collection("users").doc(scannerUid).get();
    const scannerData = scannerDoc.data();
    const scannerRole = scannerData?.role;

    const allowedRoles = ["STAFF", "SECURITY", "ADMIN", "ORGANIZER"];
    if (!allowedRoles.includes(scannerRole)) {
      logger.warn(
        `Unauthorized scan attempt by ${scannerUid} (role: ${scannerRole})`
      );
      throw new functions.https.HttpsError(
        "permission-denied",
        "Not authorized to scan tickets"
      );
    }

    logger.info(
      `Scanner ${scannerUid} (${scannerRole}) validating entry for event ${eventId}`
    );

    /**
     * Logs validation attempts to the validations collection for audit trail.
     *
     * @param uid - User ID being validated (null if QR parsing failed)
     * @param ticketId - Ticket ID being validated (null if not found)
     * @param result - Result of validation: "accepted", "rejected", or "error"
     * @param reason - Optional reason for rejection (e.g., "bad_format", "already_scanned")
     */
    const logValidation = async (
      uid: string | null,
      ticketId: string | null,
      result: string,
      reason?: string
    ) => {
      try {
        await db.collection("validations").add({
          eventId,
          uid,
          ticketId,
          result,
          reason,
          scannedBy: scannerUid,
          scannerRole,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
        });
      } catch (error) {
        logger.error("Failed to log validation", error);
      }
    };

    // ============================================================================
    // 3. PARSE QR CODE
    // ============================================================================
    const prefix = "onepass:user:v1.";
    if (!qrText.startsWith(prefix)) {
      await logValidation(null, null, "rejected", "bad_format");
      return { status: "rejected", reason: "BAD_SIGNATURE" };
    }

    const token = qrText.substring(prefix.length);
    const parts = token.split(".");

    if (parts.length !== 2) {
      await logValidation(null, null, "rejected", "bad_format");
      return { status: "rejected", reason: "BAD_SIGNATURE" };
    }

    const [payloadB64Url, signatureB64Url] = parts;

    // ============================================================================
    // 4. DECODE PAYLOAD (Base64URL with proper padding)
    // ============================================================================
    let payloadJson: string;
    let payload: any;

    try {
      // Convert Base64URL to standard Base64 and add padding if needed
      const payloadB64 = payloadB64Url
        .replace(/-/g, "+")
        .replace(/_/g, "/")
        .padEnd(
          payloadB64Url.length + ((4 - (payloadB64Url.length % 4)) % 4),
          "="
        );

      payloadJson = Buffer.from(payloadB64, "base64").toString("utf-8");
      payload = JSON.parse(payloadJson);
    } catch (error) {
      logger.warn("Failed to decode payload", error);
      await logValidation(null, null, "rejected", "bad_format");
      return { status: "rejected", reason: "BAD_SIGNATURE" };
    }

    const { uid, kid } = payload;

    if (!uid || !kid) {
      await logValidation(null, null, "rejected", "bad_format");
      return { status: "rejected", reason: "BAD_SIGNATURE" };
    }

    // ============================================================================
    // 5. VERIFY CRYPTOGRAPHIC SIGNATURE (Ed25519)
    // ============================================================================
    const isValid = await verifySignature(payloadJson, signatureB64Url, kid);
    if (!isValid) {
      logger.warn(`Invalid signature for uid=${uid}`);
      await logValidation(uid, null, "rejected", "bad_signature");
      return { status: "rejected", reason: "BAD_SIGNATURE" };
    }

    // ============================================================================
    // 6. CHECK PASS STATUS
    // ============================================================================
    const userDoc = await db.collection("users").doc(uid).get();
    const pass = userDoc.data()?.pass;

    if (!pass || !pass.active || pass.revokedAt) {
      logger.warn(`Pass revoked or inactive for uid=${uid}`);
      await logValidation(uid, null, "rejected", "revoked");
      return { status: "rejected", reason: "REVOKED" };
    }

    // ============================================================================
    // 7. FIND TICKET FOR EVENT
    // ============================================================================
    const ticketsSnap = await db
      .collection("tickets")
      .where("ownerId", "==", uid)
      .where("eventId", "==", eventId)
      .where("state", "in", ["ISSUED", "TRANSFERRED"])
      .limit(1)
      .get();

    if (ticketsSnap.empty) {
      logger.warn(`No valid ticket for uid=${uid}, event=${eventId}`);
      await logValidation(uid, null, "rejected", "not_registered");
      return { status: "rejected", reason: "UNREGISTERED" };
    }

    const ticketDoc = ticketsSnap.docs[0];
    const ticketId = ticketDoc.id;
    const ticket = ticketDoc.data();

    // ============================================================================
    // 8. CHECK IF ALREADY REDEEMED
    // ============================================================================
    if (ticket.state === "REDEEMED") {
      const redeemedAt = ticket.redeemedAt?.seconds;
      const previousScanner = ticket.scannedBy || "unknown";
      logger.warn(
        `Ticket ${ticketId} already redeemed by ${previousScanner}`
      );
      await logValidation(uid, ticketId, "rejected", "already_scanned");
      return {
        status: "rejected",
        reason: "ALREADY_SCANNED",
        scannedAt: redeemedAt,
      };
    }

    // ============================================================================
    // 9. ANTI-REPLAY PROTECTION (30-second window)
    // ============================================================================
    const now = Date.now();
    const recentValidations = await db
      .collection("validations")
      .where("uid", "==", uid)
      .where("eventId", "==", eventId)
      .where("result", "==", "accepted")
      .where("timestamp", ">=", new Date(now - 30000))
      .limit(1)
      .get();

    if (!recentValidations.empty) {
      logger.warn(`Duplicate scan detected for uid=${uid}, event=${eventId}`);
      return { status: "rejected", reason: "ALREADY_SCANNED" };
    }

    // ============================================================================
    // 10. ATOMIC TRANSACTION (Update ticket, pass, event, validations)
    // ============================================================================
    const scannedAtSeconds = Math.floor(now / 1000);

    try {
      await db.runTransaction(async (transaction) => {
        const eventRef = db.collection("events").doc(eventId);
        const eventSnap = await transaction.get(eventRef);

        if (!eventSnap.exists) {
          throw new Error("Event not found");
        }

        const eventData = eventSnap.data()!;
        const remaining = eventData.ticketsRemaining ?? 0;

        // Prevent entry if event is at full capacity
        if (remaining <= 0) {
          throw new Error("Event at full capacity");
        }

        // Mark ticket as redeemed
        transaction.update(ticketDoc.ref, {
          state: "REDEEMED",
          redeemedAt: admin.firestore.FieldValue.serverTimestamp(),
          scannedBy: scannerUid,
          scannerRole,
        });

        // Update user's pass with last scan timestamp
        transaction.update(db.collection("users").doc(uid), {
          "pass.lastScannedAt": scannedAtSeconds,
        });

        // Increment event counters (atomic)
        transaction.update(eventRef, {
          ticketsRedeemed: admin.firestore.FieldValue.increment(1),
          ticketsRemaining: admin.firestore.FieldValue.increment(-1),
        });

        // Log successful validation
        transaction.set(db.collection("validations").doc(), {
          uid,
          eventId,
          ticketId,
          result: "accepted",
          scannedBy: scannerUid,
          scannerRole,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
        });
      });

      // Fetch updated remaining count
      const updatedEvent = await db.collection("events").doc(eventId).get();
      const remaining = updatedEvent.data()?.ticketsRemaining ?? 0;

      logger.info(
        `Entry validated for uid=${uid}, ticket=${ticketId}, remaining=${remaining}, scanner=${scannerUid}`
      );

      return {
        status: "accepted",
        ticketId,
        scannedAt: scannedAtSeconds,
        remaining: Math.max(0, remaining),
      };
    } catch (error: any) {
      logger.error(`Transaction failed for uid=${uid}:`, error);
      await logValidation(uid, ticketId, "error", error.message);
      return { status: "rejected", reason: "UNKNOWN" };
    }
  }
);