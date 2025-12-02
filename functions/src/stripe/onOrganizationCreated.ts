/**
 * Firestore trigger: Automatically creates a Stripe Connect account when an organization is created
 */

import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {stripe} from "./config";

/**
 * Triggered automatically when a new organization document is created.
 * Creates a Stripe Connect account and updates the organization with the account ID.
 * 
 * This ensures every organization has a Stripe account ready to receive payments.
 */
export const onOrganizationCreated = functions.firestore
  .document("organizations/{organizationId}")
  .onCreate(async (snapshot, context) => {
    const organizationId = context.params.organizationId;
    const orgData = snapshot.data();

    console.log(`Organization ${organizationId} created, setting up Stripe account...`);

    try {
      // Create Stripe Connect account for the organization
      const account = await stripe.accounts.create({
        type: "standard", // Standard account - organization has full control
        country: orgData.country || "CH", // Default to Switzerland
        email: orgData.contactEmail || undefined,
        metadata: {
          organizationId: organizationId,
          ownerId: orgData.ownerId,
          createdAt: new Date().toISOString(),
        },
        business_profile: {
          name: orgData.name,
          url: orgData.website || undefined,
        },
        // Add business type if you have it
        business_type: "company", // or "individual" based on your needs
      });

      console.log(`Created Stripe account ${account.id} for organization ${organizationId}`);

      // Update organization document with Stripe account ID
      await snapshot.ref.update({
        stripeConnectedAccountId: account.id,
        stripeAccountStatus: "incomplete", // User hasn't completed onboarding yet
        stripeChargesEnabled: false,
        stripePayoutsEnabled: false,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      console.log(`Updated organization ${organizationId} with Stripe account ID`);

      // Optional: Create an account link for onboarding
      // You can store this in a separate collection for the owner to access
      const accountLink = await stripe.accountLinks.create({
        account: account.id,
        refresh_url: `https://onepass.app/organization/${organizationId}/stripe/refresh`,
        return_url: `https://onepass.app/organization/${organizationId}/stripe/complete`,
        type: "account_onboarding",
      });

      // Store the onboarding link in a subcollection for the owner to access
      await admin.firestore()
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
        });

      console.log(`Stored onboarding URL for organization ${organizationId}`);

    } catch (error: any) {
      console.error(
        `Failed to create Stripe account for organization ${organizationId}:`,
        error
      );

      // Update organization with error status so the owner knows there was a problem
      await snapshot.ref.update({
        stripeAccountStatus: "error",
        stripeAccountError: error.message || "Failed to create Stripe account",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Don't throw error - we don't want to fail the entire organization creation
      // The organization still exists, just without payment capabilities yet
      // The owner can retry later
    }
  });
