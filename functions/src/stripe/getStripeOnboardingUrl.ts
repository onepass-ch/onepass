/**
 * Gets or regenerates the Stripe onboarding URL for an organization
 */

import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {stripe} from "./config";

interface GetOnboardingUrlRequest {
  organizationId: string;
}

/**
 * Gets the Stripe onboarding URL for an organization.
 * If the URL is expired or doesn't exist, generates a new one.
 * 
 * The owner uses this URL to complete Stripe setup:
 * - Add bank account information
 * - Verify business details
 * - Accept Stripe terms
 */
export const getStripeOnboardingUrl = functions.https.onCall(
  async (request) => {
    // Verify user is authenticated
    if (!request.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const userId = request.auth.uid;
    const data = request.data as GetOnboardingUrlRequest;
    const {organizationId} = data;

    if (!organizationId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Organization ID is required"
      );
    }

    const db = admin.firestore();

    try {
      // Get organization document
      const orgDoc = await db.collection("organizations").doc(organizationId).get();
      
      if (!orgDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "Organization not found"
        );
      }

      const orgData = orgDoc.data();

      // Verify user is the owner
      if (orgData?.ownerId !== userId) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Only the organization owner can access the onboarding URL"
        );
      }

      // Check if organization has a Stripe account
      const stripeAccountId = orgData?.stripeConnectedAccountId;
      
      if (!stripeAccountId) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Organization does not have a Stripe account. Please contact support."
        );
      }

      // Check if onboarding is already complete
      if (orgData?.stripeAccountStatus === "complete") {
        // Account is already set up, return dashboard URL instead
        const loginLink = await stripe.accounts.createLoginLink(stripeAccountId);
        
        return {
          onboardingComplete: true,
          dashboardUrl: loginLink.url,
        };
      }

      // Generate new onboarding link
      const accountLink = await stripe.accountLinks.create({
        account: stripeAccountId,
        refresh_url: `https://onepass.app/organization/${organizationId}/stripe/refresh`,
        return_url: `https://onepass.app/organization/${organizationId}/stripe/complete`,
        type: "account_onboarding",
      });

      // Store the new URL in Firestore
      await db
        .collection("organizations")
        .doc(organizationId)
        .collection("private")
        .doc("stripe")
        .set({
          onboardingUrl: accountLink.url,
          onboardingUrlCreatedAt: admin.firestore.FieldValue.serverTimestamp(),
          onboardingUrlExpiresAt: admin.firestore.Timestamp.fromMillis(
            Date.now() + 24 * 60 * 60 * 1000 // 24 hours
          ),
        }, { merge: true });

      console.log(
        `Generated onboarding URL for organization ${organizationId}`
      );

      return {
        onboardingComplete: false,
        onboardingUrl: accountLink.url,
      };
    } catch (error: any) {
      console.error("Error getting onboarding URL:", error);
      
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
      
      throw new functions.https.HttpsError(
        "internal",
        `Failed to get onboarding URL: ${error.message}`
      );
    }
  }
);
