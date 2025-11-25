import * as functions from "firebase-functions";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import { getActiveKey, signPayload } from "./crypto";

const db = admin.firestore();

export const generateUserPass = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required");
  }

  logger.info(`Generating pass for user ${uid}`);

  const userDoc = await db.collection("user").doc(uid).get();
  const existingPass = userDoc.data()?.pass;

  if (existingPass && existingPass.signature) {
    logger.info(`Pass already exists for user ${uid}, returning existing`);
    return existingPass;
  }

  const key = await getActiveKey();
  logger.info(`Using signing key: ${key.kid}`);

  const issuedAt = Math.floor(Date.now() / 1000);
  const version = 1;

  const payloadObj = {
    uid,
    kid: key.kid,
    iat: issuedAt,
    ver: version,
  };
  const payloadJson = JSON.stringify(payloadObj);

  const signature = signPayload(payloadJson, key.privateKey);

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

  await db.collection("user").doc(uid).set(
    { pass },
    { merge: true }
  );

  logger.info(`Pass generated successfully for user ${uid}`);

  return pass;
});