/**
 * Gets the Stripe Connect account status and dashboard link
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {stripe} from "./config";

/**
 * Gets the Stripe Connect account status and dashboard link
 */
export const getConnectedAccountStatus = functions.https.onCall(
  async (data: { organizationId: string }, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
    }

    const userId = context.auth.uid;
    const {organizationId} = data;

    if (!organizationId) {
      throw new functions.https.HttpsError("invalid-argument", "Organization ID is required");
    }

    const db = admin.firestore();

    try {
      const orgDoc = await db.collection("organizations").doc(organizationId).get();
      if (!orgDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Organization not found");
      }

      const orgData = orgDoc.data();

      // Verify user is the owner
      if (orgData?.ownerId !== userId) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Only organization owners can view account status"
        );
      }

      const accountId = orgData?.stripeConnectedAccountId;
      if (!accountId) {
        return {
          exists: false,
          status: orgData?.stripeAccountStatus || "not_created",
        };
      }

      // Get account details from Stripe
      const account = await stripe.accounts.retrieve(accountId);

      // Create login link for the dashboard (only if account is complete)
      let dashboardUrl = null;
      if (account.details_submitted) {
        const loginLink = await stripe.accounts.createLoginLink(accountId);
        dashboardUrl = loginLink.url;
      }

      return {
        exists: true,
        accountId: accountId,
        detailsSubmitted: account.details_submitted,
        chargesEnabled: account.charges_enabled,
        payoutsEnabled: account.payouts_enabled,
        dashboardUrl: dashboardUrl,
      };
    } catch (error: any) {
      console.error("Error getting account status:", error);
      throw new functions.https.HttpsError(
        "internal",
        `Failed to get account status: ${error.message}`
      );
    }
  }
);
