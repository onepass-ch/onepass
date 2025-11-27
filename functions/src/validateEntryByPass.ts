import * as functions from "firebase-functions/v1";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import { verifySignature } from "./crypto";

const db = admin.firestore();

export const validateEntryByPass = functions.https.onCall(async (data: any, context: any) => {
  const { qrText, eventId } = data;

  if (!qrText || !eventId) {
    throw new functions.https.HttpsError("invalid-argument", "qrText and eventId required");
  }

  logger.info(`Validating entry for event ${eventId}`);

  // Parse QR code
  const prefix = "onepass:user:v1.";
  if (!qrText.startsWith(prefix)) {
    return { status: "rejected", reason: "BAD_SIGNATURE" };
  }

  const token = qrText.substring(prefix.length);
  const parts = token.split(".");

  if (parts.length !== 2) {
    return { status: "rejected", reason: "BAD_SIGNATURE" };
  }

  const [payloadB64Url, signatureB64Url] = parts;

  // Decode payload
  const payloadB64 = payloadB64Url.replace(/-/g, "+").replace(/_/g, "/");
  const payloadJson = Buffer.from(payloadB64, "base64").toString("utf-8");
  let payload: any;

  try {
    payload = JSON.parse(payloadJson);
  } catch (error) {
    return { status: "rejected", reason: "BAD_SIGNATURE" };
  }

  const { uid, kid } = payload;

  if (!uid || !kid) {
    return { status: "rejected", reason: "BAD_SIGNATURE" };
  }

  // Verify signature
  const isValid = await verifySignature(payloadJson, signatureB64Url, kid);
  if (!isValid) {
    logger.warn(`Invalid signature for uid=${uid}`);
    return { status: "rejected", reason: "BAD_SIGNATURE" };
  }

  // Check pass status
  const userDoc = await db.collection("users").doc(uid).get();  // ← CORRIGÉ
  const pass = userDoc.data()?.pass;

  if (!pass || !pass.active || pass.revokedAt) {
    logger.warn(`Pass revoked or inactive for uid=${uid}`);
    return { status: "rejected", reason: "REVOKED" };
  }

  // Anti-replay check (30 seconds)
  const now = Date.now();
  const recentValidations = await db
    .collection("validations")
    .where("uid", "==", uid)
    .where("eventId", "==", eventId)
    .where("timestamp", ">=", new Date(now - 30000))
    .limit(1)
    .get();

  if (!recentValidations.empty) {
    logger.warn(`Duplicate scan detected for uid=${uid}, event=${eventId}`);
    return { status: "rejected", reason: "ALREADY_SCANNED" };
  }

  // Find ticket (using uppercase enum values from Kotlin)
  const ticketsSnap = await db
    .collection("tickets")
    .where("ownerId", "==", uid)
    .where("eventId", "==", eventId)
    .where("state", "in", ["ISSUED", "TRANSFERRED"])
    .limit(1)
    .get();

  if (ticketsSnap.empty) {
    // Create failed validation record
    await db.collection("validations").add({
      uid,
      eventId,
      result: "not_registered",
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });
    logger.warn(`No valid ticket for uid=${uid}, event=${eventId}`);
    return { status: "rejected", reason: "UNREGISTERED" };
  }

  const ticketDoc = ticketsSnap.docs[0];
  const ticketId = ticketDoc.id;

  // Transaction: update ticket, pass, event
  const scannedAtSeconds = Math.floor(now / 1000);

  try {
    await db.runTransaction(async (transaction) => {
      const eventRef = db.collection("events").doc(eventId);
      const eventSnap = await transaction.get(eventRef);

      if (!eventSnap.exists) {
        throw new Error("Event not found");
      }

      // Update ticket to REDEEMED (uppercase)
      transaction.update(ticketDoc.ref, {
        state: "REDEEMED",
        redeemedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Update pass
      transaction.update(db.collection("users").doc(uid), {  // ← CORRIGÉ
        "pass.lastScannedAt": scannedAtSeconds,
      });

      // Increment event counters
      transaction.update(eventRef, {
        ticketsRedeemed: admin.firestore.FieldValue.increment(1),
        ticketsRemaining: admin.firestore.FieldValue.increment(-1),
      });

      // Create success validation record
      transaction.set(db.collection("validations").doc(), {
        uid,
        eventId,
        ticketId,
        result: "ok",
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    // Get updated remaining count
    const eventDoc = await db.collection("events").doc(eventId).get();
    const eventData = eventDoc.data();
    const remaining = eventData?.ticketsRemaining ?? 0;

    logger.info(`Entry validated for uid=${uid}, ticket=${ticketId}`);

    return {
      status: "accepted",
      ticketId,
      scannedAt: scannedAtSeconds,
      remaining: Math.max(0, remaining),
    };
  } catch (error: any) {
    logger.error(`Transaction failed for uid=${uid}:`, error);
    return { status: "rejected", reason: "UNKNOWN" };
  }
});