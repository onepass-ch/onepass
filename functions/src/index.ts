/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

import * as functions from "firebase-functions";
import {setGlobalOptions} from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

// Start writing functions
// https://firebase.google.com/docs/functions/typescript

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

export const searchUsers = functions.https.onCall(async (payload: any, context: any) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const { query, searchType, organizationId } = payload || {};
  const trimmed = (query ?? "").trim();
  if (!trimmed) {
    throw new functions.https.HttpsError("invalid-argument", "Query cannot be blank.");
  }

  let q: any = db.collection("users");
  if (searchType === "EMAIL") {
    q = q
      .where("emailLower", ">=", trimmed.toLowerCase())
      .where("emailLower", "<", trimmed.toLowerCase() + "\uf8ff");
  } else if (searchType === "NAME") {
    q = q
      .where("displayNameLower", ">=", trimmed.toLowerCase())
      .where("displayNameLower", "<", trimmed.toLowerCase() + "\uf8ff");
  } else {
    throw new functions.https.HttpsError("invalid-argument", "Invalid searchType.");
  }

  const snapshot = await q.limit(50).get();
  const results = snapshot.docs.map((d: any) => ({ id: d.id, ...d.data() }));

  // If organizationId is not null, exclude all users in the organization
  let exclude = new Set<string>();
  if (organizationId) {
    const membersSnap = await db
      .collection("orgs")
      .doc(organizationId)
      .collection("members")
      .get();
    exclude = new Set(membersSnap.docs.map((d) => d.id));
  }

  const users = results
    .filter((u: any) => !exclude.has(u.id))
    .map((u: any) => ({
      id: u.id,
      email: u.email ?? "",
      displayName: u.displayName ?? "",
      avatarUrl: u.avatarUrl ?? null,
    }));

  return { users };
});

export { generateUserPass } from "./generateUserPass";
export { onUserCreated } from "./onUserCreated";

// Stripe payment functions
export {
  createPaymentIntent,
  createStripeCustomer,
  getConnectedAccountStatus,
  getStripeOnboardingUrl,
  stripeWebhook,
  onOrganizationCreated, // Automatic trigger when organization is created
} from "./stripe";