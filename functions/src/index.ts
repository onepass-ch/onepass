/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

// import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

admin.initializeApp();

export { validateEntryByPass } from "./validateEntryByPass";
export { generateUserPass } from "./generateUserPass";
export { onUserCreated } from "./onUserCreated";

// Stripe payment functions
export {
  createPaymentIntent,
  createMarketplacePaymentIntent,
  cancelMarketplaceReservation,
  stripeWebhook,
} from "./stripe";
