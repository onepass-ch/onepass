/**
 * Stripe configuration and initialization
 */

import Stripe from "stripe";
import * as functions from "firebase-functions";

// Initialize Stripe with your secret key from Firebase config
// To set the key: firebase functions:config:set stripe.secret_key="sk_test_..."
const stripeSecretKey = functions.config().stripe?.secret_key;

if (!stripeSecretKey) {
  console.warn(
    "⚠️ Stripe secret key not found in Firebase config. " +
    "Set it with: firebase functions:config:set stripe.secret_key='sk_test_...'"
  );
}

export const stripe = new Stripe(stripeSecretKey || "", {
  apiVersion: "2024-11-20.acacia",
  typescript: true,
});

/**
 * Currency used for payments (Swiss Franc)
 */
export const DEFAULT_CURRENCY = "chf";

/**
 * Minimum amount in cents (CHF 1.00)
 */
export const MIN_PAYMENT_AMOUNT = 100;

/**
 * Maximum amount in cents (CHF 10,000.00)
 */
export const MAX_PAYMENT_AMOUNT = 1000000;
