/**
 * Stripe webhook handlers for payment events
 */

import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {stripe} from "./config";
import Stripe from "stripe";
import * as dotenv from "dotenv";

// Load environment variables
dotenv.config();

// Get webhook secret from environment variable
const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET || "";

/**
 * Handles Stripe webhook events.
 * Updates payment status in Firestore based on webhook events.
 *
 * Events handled:
 * - payment_intent.succeeded: Payment completed successfully
 * - payment_intent.payment_failed: Payment failed
 * - payment_intent.canceled: Payment was canceled
 * - account.updated: Connected account status changed
 */
export const stripeWebhook = functions.https.onRequest(async (req, res) => {
  // Only accept POST requests
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }

  const sig = req.headers["stripe-signature"];

  if (!sig) {
    console.error("Missing stripe-signature header");
    res.status(400).send("Missing signature");
    return;
  }

  let event: Stripe.Event;

  try {
    // Verify webhook signature
    event = stripe.webhooks.constructEvent(
      req.rawBody,
      sig,
      webhookSecret || ""
    );
  } catch (err: any) {
    console.error("Webhook signature verification failed:", err.message);
    res.status(400).send(`Webhook Error: ${err.message}`);
    return;
  }

  const db = admin.firestore();

  try {
    // Handle the event
    switch (event.type) {
      case "payment_intent.succeeded": {
        const paymentIntent = event.data.object as Stripe.PaymentIntent;
        console.log(`PaymentIntent ${paymentIntent.id} succeeded`);

        // Update payment status in Firestore
        await db.collection("payments").doc(paymentIntent.id).update({
          status: "succeeded",
          succeededAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Create ticket for the user
        const paymentDoc = await db.collection("payments").doc(paymentIntent.id).get();
        const paymentData = paymentDoc.data();

        if (paymentData) {
          const ticketData = {
            userId: paymentData.userId,
            eventId: paymentData.eventId,
            ticketTypeId: paymentData.ticketTypeId,
            quantity: paymentData.quantity || 1,
            paymentIntentId: paymentIntent.id,
            status: "active",
            purchasedAt: admin.firestore.FieldValue.serverTimestamp(),
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          };

          await db.collection("tickets").add(ticketData);
          console.log(`Created ticket for payment ${paymentIntent.id}`);
        }

        break;
      }

      case "payment_intent.payment_failed": {
        const paymentIntent = event.data.object as Stripe.PaymentIntent;
        console.log(`PaymentIntent ${paymentIntent.id} failed`);

        await db.collection("payments").doc(paymentIntent.id).update({
          status: "failed",
          failureReason: paymentIntent.last_payment_error?.message || "Unknown error",
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        break;
      }

      case "payment_intent.canceled": {
        const paymentIntent = event.data.object as Stripe.PaymentIntent;
        console.log(`PaymentIntent ${paymentIntent.id} canceled`);

        await db.collection("payments").doc(paymentIntent.id).update({
          status: "canceled",
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        break;
      }

      case "account.updated": {
        const account = event.data.object as Stripe.Account;
        console.log(`Account ${account.id} updated`);

        // Find organization with this connected account
        const orgQuery = await db
          .collection("orgs")
          .where("stripeConnectedAccountId", "==", account.id)
          .limit(1)
          .get();

        if (!orgQuery.empty) {
          const orgDoc = orgQuery.docs[0];
          await orgDoc.ref.update({
            stripeAccountStatus: account.details_submitted ? "complete" : "incomplete",
            stripeChargesEnabled: account.charges_enabled,
            stripePayoutsEnabled: account.payouts_enabled,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          console.log(
            `Updated organization ${orgDoc.id} stripe status: ` +
            `details_submitted=${account.details_submitted}`
          );
        }

        break;
      }

      case "charge.refunded": {
        const charge = event.data.object as Stripe.Charge;
        console.log(`Charge ${charge.id} refunded`);

        // Find payment with this payment intent
        const paymentIntentId = charge.payment_intent as string;
        if (paymentIntentId) {
          await db.collection("payments").doc(paymentIntentId).update({
            status: "refunded",
            refundedAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          // Update associated tickets
          const ticketsQuery = await db
            .collection("tickets")
            .where("paymentIntentId", "==", paymentIntentId)
            .get();

          const batch = db.batch();
          ticketsQuery.docs.forEach((doc) => {
            batch.update(doc.ref, {
              status: "refunded",
              refundedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
          });
          await batch.commit();

          console.log(`Refunded tickets for payment ${paymentIntentId}`);
        }

        break;
      }

      default:
        console.log(`Unhandled event type: ${event.type}`);
    }

    // Return a 200 response to acknowledge receipt of the event
    res.json({received: true});
  } catch (error: any) {
    console.error("Error handling webhook:", error);
    res.status(500).send(`Webhook handler failed: ${error.message}`);
  }
});
