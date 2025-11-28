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

/**
 * Retrieves the most recent active signing key from Firestore.
 * Uses orderBy to ensure deterministic key selection for rotation.
 *
 * @returns The active signing key
 * @throws HttpsError if no active key is found
 */
export async function getActiveKey(): Promise<SigningKey> {
  const snapshot = await db
    .collection("keys")
    .where("active", "==", true)
    .orderBy("createdAt", "desc")
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

/**
 * Signs a payload using Ed25519 private key.
 *
 * @param payload - JSON string to sign
 * @param privateKeyBase64 - Base64-encoded Ed25519 private key (64 bytes)
 * @returns Base64url-encoded signature
 * @throws HttpsError if private key format is invalid
 */
export function signPayload(payload: string, privateKeyBase64: string): string {
  const privateKey = util.decodeBase64(privateKeyBase64);

  // Validate Ed25519 private key length (64 bytes)
  if (privateKey.length !== 64) {
    throw new functions.https.HttpsError(
      "internal",
      `Invalid private key length: expected 64 bytes, got ${privateKey.length}`
    );
  }

  const message = util.decodeUTF8(payload);
  const signature = nacl.sign.detached(message, privateKey);

  return util.encodeBase64(signature)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

/**
 * Verifies an Ed25519 signature against a payload.
 *
 * @param payload - Original JSON string that was signed
 * @param signatureBase64Url - Base64url-encoded signature
 * @param kid - Key ID to use for verification
 * @returns true if signature is valid, false otherwise
 */
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

  try {
    const publicKey = util.decodeBase64(keyData.publicKey);

    // Validate Ed25519 public key length (32 bytes)
    if (publicKey.length !== 32) {
      return false;
    }

    const signature = util.decodeBase64(signatureBase64);
    const message = util.decodeUTF8(payload);

    return nacl.sign.detached.verify(message, signature, publicKey);
  } catch (error) {
    // Catch any decoding errors
    return false;
  }
}