/**
 * Stripe configuration and initialization
 */

import * as functions from "firebase-functions/v1";
import Stripe from "stripe";
import * as dotenv from "dotenv";

// Load environment variables from .env file (for local development only)
dotenv.config();

// Get Stripe secret key from environment variable
// In production, use: firebase functions:config:set stripe.secret_key="sk_..."
// In local dev, use: .env file with STRIPE_SECRET_KEY
const stripeSecretKey = 
  functions.config().stripe?.secret_key || 
  process.env.STRIPE_SECRET_KEY || 
  "";

if (!stripeSecretKey) {
  console.warn(
    "⚠️ Stripe secret key not found in environment variables. " +
    "For production: firebase functions:config:set stripe.secret_key=\"sk_...\" " +
    "For local dev: Set STRIPE_SECRET_KEY in functions/.env file"
  );
}

export const stripe = new Stripe(stripeSecretKey, {
  apiVersion: "2025-02-24.acacia",
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
