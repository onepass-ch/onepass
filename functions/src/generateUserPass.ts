import * as functions from "firebase-functions/v1";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import { getActiveKey, signPayload } from "./crypto";

const db = admin.firestore();

export const generateUserPass = functions.https.onCall(async (data: any, context: any) => {
  const uid = context.auth?.uid;
  if (!uid) {
    logger.error("Authentication required - no uid in context");
    throw new functions.https.HttpsError("unauthenticated", "Authentication required");
  }

  logger.info(`Generating pass for user ${uid}`);

  try {
    const userDoc = await db.collection("users").doc(uid).get();
    const existingPass = userDoc.data()?.pass;

    if (existingPass && existingPass.signature) {
      if (!existingPass.active || existingPass.revokedAt) {
        logger.info(`Existing pass for user ${uid} is revoked or inactive, regenerating`);
      } else {
        logger.info(`Valid pass already exists for user ${uid}, returning existing`);
        return existingPass;
      }
    }

    const key = await getActiveKey();
    logger.info(`Using signing key: ${key.kid}`);

    const issuedAt = Math.floor(Date.now() / 1000);
    const version = 1;

    // ============================================================================
    // CRITICAL: Use deterministic JSON serialization to ensure signature validity
    // The order of fields MUST match the order used during verification
    // ============================================================================
    const payloadJson = JSON.stringify({
      uid: uid,
      kid: key.kid,
      iat: issuedAt,
      ver: version
    });

    const signature = signPayload(payloadJson, key.privateKey);

    logger.info(`Generated signature of length: ${signature.length}`);

    const pass = {
      uid,
      kid: key.kid,
      issuedAt,
      version,
      active: true,
      signature,
      lastScannedAt: null,
      revokedAt: null,
    };

    await db.collection("users").doc(uid).set({ pass }, { merge: true });

    logger.info(`Pass generated and saved successfully for user ${uid}`);

    return pass;
  } catch (error) {
    logger.error(`Error generating pass for user ${uid}:`, error);

    if (error instanceof Error) {
      throw new functions.https.HttpsError(
        "internal",
        `Failed to generate pass: ${error.message}`
      );
    }
    throw new functions.https.HttpsError(
      "internal",
      "Failed to generate pass due to an unknown error"
    );
  }
});