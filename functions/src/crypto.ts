import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import * as nacl from "tweetnacl";
import * as util from "tweetnacl-util";

const db = admin.firestore();

export interface SigningKey {
  kid: string;
  publicKey: string;
  privateKey: string;
  active: boolean;
}

export async function getActiveKey(): Promise<SigningKey> {
  const snapshot = await db
    .collection("keys")
    .where("active", "==", true)
    .limit(1)
    .get();

  if (snapshot.empty) {
    throw new functions.https.HttpsError("internal", "No active signing key found");
  }

  const doc = snapshot.docs[0];
  return {
    kid: doc.id,
    publicKey: doc.data().publicKey,
    privateKey: doc.data().privateKey,
    active: doc.data().active,
  };
}

export function signPayload(payload: string, privateKeyBase64: string): string {
  const privateKey = util.decodeBase64(privateKeyBase64);
  const message = util.decodeUTF8(payload);
  const signature = nacl.sign.detached(message, privateKey);

  return util.encodeBase64(signature)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

export async function verifySignature(
  payload: string,
  signatureBase64Url: string,
  kid: string
): Promise<boolean> {
  const keyDoc = await db.collection("keys").doc(kid).get();
  if (!keyDoc.exists) {
    return false;
  }

  const keyData = keyDoc.data();
  if (!keyData || keyData.revokedAt) {
    return false;
  }

  const signatureBase64 = signatureBase64Url
    .replace(/-/g, "+")
    .replace(/_/g, "/");

  const publicKey = util.decodeBase64(keyData.publicKey);
  const signature = util.decodeBase64(signatureBase64);
  const message = util.decodeUTF8(payload);

  return nacl.sign.detached.verify(message, signature, publicKey);
}