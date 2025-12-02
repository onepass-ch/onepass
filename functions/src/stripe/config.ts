/**
 * Stripe configuration and initialization
 */

import Stripe from "stripe";
import * as dotenv from "dotenv";

// Load environment variables from .env file
dotenv.config();

// Get Stripe secret key from environment variable
const stripeSecretKey = process.env.STRIPE_SECRET_KEY || "";

if (!stripeSecretKey) {
  console.warn(
    "⚠️ Stripe secret key not found in environment variables. " +
    "Make sure STRIPE_SECRET_KEY is set in functions/.env file"
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
