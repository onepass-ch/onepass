import * as functions from "firebase-functions/v1";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import { getActiveKey, signPayload } from "./crypto";

const db = admin.firestore();

export const onUserCreated = functions.auth.user().onCreate(async (user: any) => {
  const uid = user.uid;

  logger.info(`New user created: ${uid}, generating pass...`);

  try {
    const key = await getActiveKey();
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

    await db.collection("users").doc(uid).set({ pass }, { merge: true });

    logger.info(`Pass generated automatically for user: ${uid}`);
  } catch (error) {
    logger.error(`Failed to generate pass for ${uid}:`, error);
  }
});