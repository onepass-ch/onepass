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

        // Get payment record to determine type
        const paymentDoc = await db.collection("payments").doc(paymentIntent.id).get();
        const paymentData = paymentDoc.data();

        if (paymentData) {
          // Check if this is a marketplace purchase
          if (paymentData.type === "marketplace") {
            // Handle marketplace ticket transfer
            await handleMarketplacePurchaseSuccess(db, paymentIntent, paymentData);
          } else {
            // Handle regular event ticket purchase
            await handleEventTicketPurchaseSuccess(db, paymentIntent, paymentData);
          }
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

        // Release marketplace reservation if applicable
        const failedPaymentDoc = await db.collection("payments").doc(paymentIntent.id).get();
        const failedPaymentData = failedPaymentDoc.data();
        if (failedPaymentData?.type === "marketplace" && failedPaymentData.ticketId) {
          await releaseMarketplaceReservation(db, failedPaymentData.ticketId, failedPaymentData.buyerId);
        }

        break;
      }

      case "payment_intent.canceled": {
        const paymentIntent = event.data.object as Stripe.PaymentIntent;
        console.log(`PaymentIntent ${paymentIntent.id} canceled`);

        await db.collection("payments").doc(paymentIntent.id).update({
          status: "canceled",
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Release marketplace reservation if applicable
        const canceledPaymentDoc = await db.collection("payments").doc(paymentIntent.id).get();
        const canceledPaymentData = canceledPaymentDoc.data();
        if (canceledPaymentData?.type === "marketplace" && canceledPaymentData.ticketId) {
          await releaseMarketplaceReservation(db, canceledPaymentData.ticketId, canceledPaymentData.buyerId);
        }

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

/**
 * Releases a marketplace ticket reservation.
 * Called when payment fails or is canceled.
 */
async function releaseMarketplaceReservation(
  db: admin.firestore.Firestore,
  ticketId: string,
  buyerId: string
): Promise<void> {
  try {
    const ticketRef = db.collection("tickets").doc(ticketId);
    
    await db.runTransaction(async (transaction) => {
      const ticketDoc = await transaction.get(ticketRef);
      
      if (!ticketDoc.exists) {
        console.log(`⚠️ Ticket ${ticketId} not found for reservation release`);
        return;
      }

      const ticketData = ticketDoc.data();
      
      // Only release if reserved by this buyer
      if (ticketData?.reservedBy === buyerId) {
        transaction.update(ticketRef, {
          reservedBy: admin.firestore.FieldValue.delete(),
          reservedUntil: admin.firestore.FieldValue.delete(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        console.log(`✓ Released reservation for ticket ${ticketId}`);
      } else {
        console.log(`⚠️ Ticket ${ticketId} reservation not held by buyer ${buyerId}`);
      }
    });
  } catch (error: any) {
    console.error(`❌ Failed to release reservation for ticket ${ticketId}:`, error.message);
  }
}

/**
 * Handles successful marketplace ticket purchase.
 * Transfers ticket ownership from seller to buyer atomically.
 */
async function handleMarketplacePurchaseSuccess(
  db: admin.firestore.Firestore,
  paymentIntent: Stripe.PaymentIntent,
  paymentData: admin.firestore.DocumentData
): Promise<void> {
  const ticketId = paymentData.ticketId as string;
  const buyerId = paymentData.buyerId as string;
  const sellerId = paymentData.sellerId as string;
  const amount = paymentData.amount as number;

  console.log(`🛒 Processing marketplace purchase: ticket ${ticketId}, seller ${sellerId} -> buyer ${buyerId}`);

  await db.runTransaction(async (transaction) => {
    const ticketRef = db.collection("tickets").doc(ticketId);
    const ticketDoc = await transaction.get(ticketRef);

    if (!ticketDoc.exists) {
      throw new Error(`Ticket ${ticketId} not found`);
    }

    const ticketData = ticketDoc.data();
    if (!ticketData) {
      throw new Error(`Ticket ${ticketId} has no data`);
    }

    // Verify ticket is still in LISTED state and reserved by the buyer
    if (ticketData.state !== "LISTED") {
      throw new Error(`Ticket ${ticketId} is no longer listed (state: ${ticketData.state})`);
    }

    // Verify the ticket was reserved by this buyer
    if (ticketData.reservedBy && ticketData.reservedBy !== buyerId) {
      throw new Error(`Ticket ${ticketId} is reserved by a different user`);
    }

    // Verify the seller still owns the ticket
    if (ticketData.ownerId !== sellerId) {
      throw new Error(`Ticket ${ticketId} owner mismatch`);
    }

    // Transfer ownership: update ticket with new owner
    transaction.update(ticketRef, {
      ownerId: buyerId,
      state: "TRANSFERRED",
      previousOwnerId: sellerId,
      transferredAt: admin.firestore.FieldValue.serverTimestamp(),
      transferPrice: amount / 100, // Convert cents back to currency
      listingPrice: admin.firestore.FieldValue.delete(),
      listedAt: admin.firestore.FieldValue.delete(),
      reservedBy: admin.firestore.FieldValue.delete(),
      reservedUntil: admin.firestore.FieldValue.delete(),
      version: (ticketData.version || 1) + 1,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Create a transfer record for audit trail
    const transferRef = db.collection("ticketTransfers").doc();
    transaction.set(transferRef, {
      transferId: transferRef.id,
      ticketId: ticketId,
      eventId: ticketData.eventId,
      fromUserId: sellerId,
      toUserId: buyerId,
      amount: amount / 100,
      currency: paymentData.currency || "chf",
      paymentIntentId: paymentIntent.id,
      transferredAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    console.log(`✅ Ticket ${ticketId} transferred from ${sellerId} to ${buyerId}`);
  });

  console.log(`✅ Marketplace purchase completed: ${paymentIntent.id}`);
}

/**
 * Handles successful event ticket purchase (original ticket creation flow).
 * Creates new tickets and updates event availability.
 */
async function handleEventTicketPurchaseSuccess(
  db: admin.firestore.Firestore,
  paymentIntent: Stripe.PaymentIntent,
  paymentData: admin.firestore.DocumentData
): Promise<void> {
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

export const stripeWebhook = functions.runWith({
  // Ensure we have enough memory and timeout for webhook processing
  timeoutSeconds: 60,
  memory: "256MB",
}).https.onRequest(app);
