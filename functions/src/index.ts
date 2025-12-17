/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

admin.initializeApp();


export { generateUserPass } from "./generateUserPass";
export { onUserCreated } from "./onUserCreated";
export { searchUsers } from "./searchUsers";
export { validatingEntryByPass } from "./validatingEntryByPass";

// Stripe payment functions
export {
  createPaymentIntent,
  createMarketplacePaymentIntent,
  cancelMarketplaceReservation,
  stripeWebhook,
} from "./stripe";

/**
 * Returns the current Firebase server time in milliseconds.
 * Used by the client to calculate the clock skew.
 */
export const getServerTime = functions.https.onCall((data, context) => {
    // Return the current server time in milliseconds
    return {
        timestamp: admin.firestore.Timestamp.now().toMillis()
    };
});
