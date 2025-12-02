/**
 * Creates a Stripe customer for a user
 */

import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {stripe} from "./config";

interface CreateCustomerRequest {
  email?: string;
  displayName?: string;
}

/**
 * Creates a Stripe customer ID for a user.
 * This allows users to save payment methods and view payment history.
 *
 * @param data - Optional customer data (email, displayName)
 * @param context - Firebase auth context
 * @returns Object containing customerId
 */
export const createStripeCustomer = functions.https.onCall(
  async (request) => {
    const data = request.data as CreateCustomerRequest;
    
    // Verify user is authenticated
    if (!request.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const userId = request.auth.uid;
    const db = admin.firestore();

    try {
      // Check if user already has a Stripe customer ID
      const userDoc = await db.collection("users").doc(userId).get();
      const userData = userDoc.data();

      if (userData?.stripeCustomerId) {
        // Customer already exists, return existing ID
        return {
          customerId: userData.stripeCustomerId,
          existing: true,
        };
      }

      // Create new Stripe customer
      const customer = await stripe.customers.create({
        email: data.email || userData?.email || request.auth.token.email,
        name: data.displayName || userData?.displayName || request.auth.token.name,
        metadata: {
          firebaseUID: userId,
        },
      });

      // Save customer ID to user document
      await db.collection("users").doc(userId).update({
        stripeCustomerId: customer.id,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      console.log(`Created Stripe customer ${customer.id} for user ${userId}`);

      return {
        customerId: customer.id,
        existing: false,
      };
    } catch (error: any) {
      console.error("Error creating Stripe customer:", error);
      throw new functions.https.HttpsError(
        "internal",
        `Failed to create Stripe customer: ${error.message}`
      );
    }
  }
);
