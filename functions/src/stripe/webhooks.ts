/**
 * Stripe webhook handlers for payment events
 */

import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {stripe} from "./config";
import Stripe from "stripe";
import * as dotenv from "dotenv";
import express from "express";

// Load environment variables for local development
dotenv.config();

// Get webhook secret from environment variable
// In production, use: firebase functions:config:set stripe.webhook_secret="whsec_..."
// In local dev, use: .env file with STRIPE_WEBHOOK_SECRET
const webhookSecret = 
  functions.config().stripe?.webhook_secret || 
  process.env.STRIPE_WEBHOOK_SECRET || 
  "";

/**
 * Handles Stripe webhook events.
 * Updates payment status in Firestore based on webhook events.
 *
 * Events handled:
 * - payment_intent.succeeded: Payment completed successfully
 * - payment_intent.payment_failed: Payment failed
 * - payment_intent.canceled: Payment was canceled
 * - account.updated: Connected account status changed
 * 
 * Note: This function must preserve the raw request body for webhook signature verification.
 * We use Express middleware to capture the raw body before JSON parsing.
 */
const app = express();

// Middleware to capture raw body for Stripe webhook signature verification
// This must be added BEFORE any body parsing middleware
// We use express.raw() to get the raw body as a Buffer, and store it in req.rawBody
app.use(
  express.raw({
    type: "application/json",
    verify: (req: express.Request, res: express.Response, buf: Buffer) => {
      // Store the raw body in the request object for Stripe signature verification
      (req as any).rawBody = buf;
    },
  })
);

// Note: We do NOT add express.json() here because we need the raw body for Stripe
app.post("*", async (req: express.Request, res: express.Response) => {
  // Only accept POST requests
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }

  const sigHeader = req.headers["stripe-signature"];
  const sig = Array.isArray(sigHeader) ? sigHeader[0] : sigHeader;

  if (!sig) {
    console.error("Missing stripe-signature header");
    res.status(400).send("Missing signature");
    return;
  }

  // Check if webhook secret is configured
  if (!webhookSecret) {
    console.error("❌ STRIPE_WEBHOOK_SECRET is not configured!");
    console.error("For production: firebase functions:config:set stripe.webhook_secret=\"whsec_...\"");
    console.error("For local dev: Set STRIPE_WEBHOOK_SECRET in functions/.env file");
    res.status(500).send("Webhook secret not configured. Please set STRIPE_WEBHOOK_SECRET.");
    return;
  }

  // Log webhook secret availability (first 10 chars only for security)
  const secretPreview = webhookSecret.substring(0, 10) + "...";
  console.log(`✅ Webhook secret found: ${secretPreview} (length: ${webhookSecret.length})`);

  let event: Stripe.Event;

  try {
    // Verify webhook signature
    // The rawBody should be captured by Express middleware
    const rawBody = (req as any).rawBody;
    
    if (!rawBody) {
      console.error("❌ No rawBody found in request");
      console.error("This means the Express middleware didn't capture the raw body");
      console.error("Request body type:", typeof req.body);
      res.status(400).send("Raw request body not available for signature verification");
      return;
    }
    
    // Convert Buffer to string (Stripe expects string)
    const rawBodyString = Buffer.isBuffer(rawBody) ? rawBody.toString("utf8") : rawBody;
    
    console.log(`✅ Using rawBody for signature verification (length: ${rawBodyString.length})`);
    
    event = stripe.webhooks.constructEvent(
      rawBodyString,
      sig,
      webhookSecret
    );
    
    console.log(`✅ Webhook signature verified successfully for event: ${event.type}`);
  } catch (err: any) {
    console.error("❌ Webhook signature verification failed:", err.message);
    console.error("Error details:", {
      message: err.message,
      type: err.type,
      signatureHeader: sig ? `${sig.substring(0, 20)}...` : "missing",
      webhookSecretLength: webhookSecret.length,
      webhookSecretPrefix: webhookSecret.substring(0, 10),
    });
    console.error("This usually means:");
    console.error("1. The webhook secret is incorrect or doesn't match the endpoint");
    console.error("2. The webhook endpoint URL in Stripe Dashboard doesn't match");
    console.error("3. The request is not from Stripe");
    console.error("4. The raw request body was not preserved (body was parsed as JSON)");
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

        // Create tickets for the user (one per quantity) and update event atomically
        const paymentDoc = await db.collection("payments").doc(paymentIntent.id).get();
        const paymentData = paymentDoc.data();

        if (paymentData) {
          const userId = paymentData.userId as string;
          const eventId = paymentData.eventId as string;
          const ticketTypeId = (paymentData.ticketTypeId as string) || "";
          const quantity = (paymentData.quantity as number) || 1;
          const amount = (paymentData.amount as number) || 0;
          
          // Calculate price per ticket (in the original currency units, not cents)
          const pricePerTicket = quantity > 0 ? amount / 100 / quantity : 0;

          // Use a transaction to atomically create tickets and update event
          await db.runTransaction(async (transaction) => {
            // Get event document within transaction
            const eventDocRef = db.collection("events").doc(eventId);
            const eventDoc = await transaction.get(eventDocRef);
            
            if (!eventDoc.exists) {
              throw new Error(`Event ${eventId} not found`);
            }

            const eventData = eventDoc.data();
            if (!eventData) {
              throw new Error(`Event ${eventId} has no data`);
            }

            // Verify tickets are still available (double-check after payment)
            const ticketsRemaining = eventData.ticketsRemaining ?? 0;
            if (ticketsRemaining < quantity) {
              throw new Error(
                `Insufficient tickets remaining: ${ticketsRemaining} available, ${quantity} requested`
              );
            }

            // If a specific tier is selected, verify and update its remaining count
            let updatedPricingTiers = eventData.pricingTiers || [];
            if (ticketTypeId) {
              const tierIndex = updatedPricingTiers.findIndex(
                (tier: any) => tier.name === ticketTypeId
              );
              
              if (tierIndex === -1) {
                throw new Error(`Pricing tier "${ticketTypeId}" not found`);
              }

              const tier = updatedPricingTiers[tierIndex];
              const tierRemaining = tier.remaining ?? 0;
              
              if (tierRemaining < quantity) {
                throw new Error(
                  `Insufficient tickets in tier "${ticketTypeId}": ${tierRemaining} available, ${quantity} requested`
                );
              }

              // Update the tier's remaining count
              updatedPricingTiers[tierIndex] = {
                ...tier,
                remaining: tierRemaining - quantity,
              };
            }

            // Use endTime from event if available (Firestore Timestamp)
            const expiresAt = eventData.endTime || null;

            // Create one ticket per quantity
            const ticketRefs = [];
            for (let i = 0; i < quantity; i++) {
              const ticketRef = db.collection("tickets").doc();
              ticketRefs.push(ticketRef);
              
              const ticketData = {
                ticketId: ticketRef.id,
                eventId: eventId,
                ownerId: userId, // Using ownerId to match Ticket model
                state: "ISSUED", // Using TicketState enum value
                tierId: ticketTypeId || "", // Using tierId to match Ticket model
                purchasePrice: pricePerTicket,
                issuedAt: admin.firestore.FieldValue.serverTimestamp(),
                expiresAt: expiresAt,
                transferLock: false,
                version: 1,
              };

              transaction.set(ticketRef, ticketData);
            }

            // Atomically update event: increment ticketsIssued, decrement ticketsRemaining, update pricingTiers
            transaction.update(eventDocRef, {
              ticketsIssued: admin.firestore.FieldValue.increment(quantity),
              ticketsRemaining: admin.firestore.FieldValue.increment(-quantity),
              pricingTiers: updatedPricingTiers,
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });

            console.log(
              `Transaction prepared: ${quantity} ticket(s), event ${eventId} updated ` +
              `(ticketsIssued +${quantity}, ticketsRemaining -${quantity})`
            );
          });

          console.log(`✅ Created ${quantity} ticket(s) and updated event ${eventId} atomically for payment ${paymentIntent.id}`);
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

          // Find tickets by payment intent (we need to check payment metadata)
          // Since tickets don't store paymentIntentId, we'll need to find them via eventId and userId
          // from the payment document
          const paymentDoc = await db.collection("payments").doc(paymentIntentId).get();
          const paymentData = paymentDoc.data();
          
          if (paymentData) {
            const userId = paymentData.userId as string;
            const eventId = paymentData.eventId as string;
            
            // Find tickets for this user and event that are still active
            const ticketsQuery = await db
              .collection("tickets")
              .where("ownerId", "==", userId)
              .where("eventId", "==", eventId)
              .where("state", "in", ["ISSUED", "LISTED", "TRANSFERRED"])
              .get();

            const batch = db.batch();
            ticketsQuery.docs.forEach((doc) => {
              batch.update(doc.ref, {
                state: "REVOKED", // Using TicketState enum value
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
              });
            });
            await batch.commit();

            console.log(`Revoked ${ticketsQuery.docs.length} ticket(s) for refunded payment ${paymentIntentId}`);
          }
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

export const stripeWebhook = functions.runWith({
  // Ensure we have enough memory and timeout for webhook processing
  timeoutSeconds: 60,
  memory: "256MB",
}).https.onRequest(app);
