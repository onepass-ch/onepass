/**
 * Creates a Stripe PaymentIntent for marketplace ticket purchases (P2P)
 * 
 * This function handles the complex flow of:
 * 1. Validating the ticket is listed and available
 * 2. Reserving the ticket to prevent race conditions
 * 3. Creating a Stripe PaymentIntent
 * 4. Storing the marketplace transaction record
 */

import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {stripe, DEFAULT_CURRENCY, MIN_PAYMENT_AMOUNT, MAX_PAYMENT_AMOUNT} from "./config";

// Reservation timeout in milliseconds (5 minutes)
const RESERVATION_TIMEOUT_MS = 5 * 60 * 1000;

interface CreateMarketplacePaymentIntentRequest {
  ticketId: string;
  description?: string;
}

interface MarketplacePaymentRecord {
  paymentIntentId: string;
  ticketId: string;
  eventId: string;
  sellerId: string;
  buyerId: string;
  amount: number;
  currency: string;
  status: string;
  type: "marketplace";
  reservedUntil: admin.firestore.Timestamp;
  createdAt: admin.firestore.FieldValue;
  updatedAt: admin.firestore.FieldValue;
}

/**
 * Creates a payment intent for purchasing a listed ticket from the marketplace.
 * Uses a Firestore transaction to reserve the ticket and prevent race conditions.
 *
 * @param data - Request containing ticketId
 * @param context - Firebase auth context
 * @returns Object containing clientSecret, paymentIntentId, and ticket details
 */
export const createMarketplacePaymentIntent = functions.https.onCall(
  async (data: CreateMarketplacePaymentIntentRequest, context) => {
    console.log("🛒 createMarketplacePaymentIntent called with data:", {
      ticketId: data?.ticketId,
      hasDescription: !!data?.description,
    });

    // Verify user is authenticated
    if (!context.auth) {
      console.error("❌ Authentication failed: No auth token provided");
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated to purchase a ticket"
      );
    }

    const buyerId = context.auth.uid;
    console.log(`✓ Buyer authenticated: ${buyerId}`);

    const {ticketId, description} = data;

    // Validate input
    if (!ticketId || typeof ticketId !== "string") {
      console.error("❌ Invalid ticketId:", ticketId);
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Ticket ID is required"
      );
    }

    const db = admin.firestore();

    try {
      // Use a transaction to atomically reserve the ticket
      const result = await db.runTransaction(async (transaction) => {
        // Get ticket document
        const ticketRef = db.collection("tickets").doc(ticketId);
        const ticketDoc = await transaction.get(ticketRef);

        if (!ticketDoc.exists) {
          throw new functions.https.HttpsError(
            "not-found",
            "Ticket not found"
          );
        }

        const ticketData = ticketDoc.data();
        if (!ticketData) {
          throw new functions.https.HttpsError(
            "not-found",
            "Ticket data not found"
          );
        }

        console.log(`✓ Ticket found: ${ticketId}, state: ${ticketData.state}`);

        // Verify ticket is listed for sale
        if (ticketData.state !== "LISTED") {
          throw new functions.https.HttpsError(
            "failed-precondition",
            "Ticket is not available for purchase"
          );
        }

        // Verify listing price exists
        const listingPrice = ticketData.listingPrice;
        if (!listingPrice || typeof listingPrice !== "number" || listingPrice <= 0) {
          throw new functions.https.HttpsError(
            "failed-precondition",
            "Ticket does not have a valid listing price"
          );
        }

        // Verify buyer is not the seller
        const sellerId = ticketData.ownerId;
        if (sellerId === buyerId) {
          throw new functions.https.HttpsError(
            "failed-precondition",
            "You cannot purchase your own ticket"
          );
        }

        // Check if ticket is already reserved by another user
        const reservedBy = ticketData.reservedBy;
        const reservedUntil = ticketData.reservedUntil;
        const now = admin.firestore.Timestamp.now();

        if (reservedBy && reservedBy !== buyerId && reservedUntil) {
          // Check if reservation is still valid
          if (reservedUntil.toMillis() > now.toMillis()) {
            throw new functions.https.HttpsError(
              "failed-precondition",
              "Ticket is currently reserved by another buyer. Please try again later."
            );
          }
          // Reservation expired, we can proceed
          console.log(`⏰ Previous reservation expired, proceeding with new reservation`);
        }

        // Convert price to cents for Stripe
        const amountInCents = Math.round(listingPrice * 100);

        // Validate amount
        if (amountInCents < MIN_PAYMENT_AMOUNT) {
          throw new functions.https.HttpsError(
            "invalid-argument",
            `Amount must be at least CHF ${MIN_PAYMENT_AMOUNT / 100}`
          );
        }

        if (amountInCents > MAX_PAYMENT_AMOUNT) {
          throw new functions.https.HttpsError(
            "invalid-argument",
            `Amount cannot exceed CHF ${MAX_PAYMENT_AMOUNT / 100}`
          );
        }

        // Get event details for description
        const eventId = ticketData.eventId;
        let eventName = "Event Ticket";
        
        if (eventId) {
          const eventDoc = await transaction.get(db.collection("events").doc(eventId));
          if (eventDoc.exists) {
            const eventData = eventDoc.data();
            eventName = eventData?.title || eventData?.name || "Event Ticket";
          }
        }

        // Calculate reservation expiry
        const reservationExpiry = admin.firestore.Timestamp.fromMillis(
          now.toMillis() + RESERVATION_TIMEOUT_MS
        );

        // Reserve the ticket within the transaction
        transaction.update(ticketRef, {
          reservedBy: buyerId,
          reservedUntil: reservationExpiry,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        console.log(`✓ Ticket reserved for buyer ${buyerId} until ${reservationExpiry.toDate()}`);

        return {
          ticketId,
          eventId,
          sellerId,
          eventName,
          listingPrice,
          amountInCents,
          reservationExpiry,
          currency: ticketData.currency || DEFAULT_CURRENCY,
        };
      });

      // Get or create Stripe customer for buyer
      const userDocRef = db.collection("users").doc(buyerId);
      const userDoc = await userDocRef.get();
      let userData = userDoc.data();

      // Create user document if doesn't exist
      if (!userDoc.exists || !userData) {
        console.log(`⚠️ Buyer document not found, creating for ${buyerId}`);
        const authUser = await admin.auth().getUser(buyerId).catch(() => null);
        
        userData = {
          email: authUser?.email || "",
          displayName: authUser?.displayName || "",
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        };
        
        await userDocRef.set(userData, {merge: true});
      }

      let customerId = userData?.stripeCustomerId as string | undefined;

      // Create Stripe customer if doesn't exist
      if (!customerId) {
        console.log(`🔨 Creating new Stripe customer for buyer ${buyerId}`);
        const customer = await stripe.customers.create({
          metadata: {
            firebaseUID: buyerId,
          },
          email: userData?.email || undefined,
          name: userData?.displayName || undefined,
        });

        customerId = customer.id;
        await userDocRef.update({stripeCustomerId: customerId});
        console.log(`✓ Created Stripe customer ${customerId}`);
      }

      // Create payment intent for marketplace purchase
      console.log(`🔨 Creating marketplace payment intent - Amount: ${result.amountInCents} cents`);
      
      const paymentIntent = await stripe.paymentIntents.create({
        amount: result.amountInCents,
        currency: result.currency.toLowerCase(),
        customer: customerId,
        metadata: {
          type: "marketplace",
          ticketId: result.ticketId,
          eventId: result.eventId,
          sellerId: result.sellerId,
          buyerId: buyerId,
          eventName: result.eventName,
        },
        description: description || `Marketplace purchase: ${result.eventName}`,
        automatic_payment_methods: {
          enabled: true,
        },
      });

      console.log(`✓ Payment intent created: ${paymentIntent.id}`);

      // Store marketplace payment record
      const paymentRecord: MarketplacePaymentRecord = {
        paymentIntentId: paymentIntent.id,
        ticketId: result.ticketId,
        eventId: result.eventId,
        sellerId: result.sellerId,
        buyerId: buyerId,
        amount: result.amountInCents,
        currency: result.currency,
        status: paymentIntent.status,
        type: "marketplace",
        reservedUntil: result.reservationExpiry,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      };

      await db.collection("payments").doc(paymentIntent.id).set(paymentRecord);
      console.log(`✓ Payment record saved: ${paymentIntent.id}`);

      console.log(`✅ Marketplace payment intent created: ${paymentIntent.id}`);

      return {
        clientSecret: paymentIntent.client_secret,
        paymentIntentId: paymentIntent.id,
        customerId: customerId,
        ticketId: result.ticketId,
        eventName: result.eventName,
        amount: result.listingPrice,
        currency: result.currency,
      };

    } catch (error: any) {
      console.error("❌ Error creating marketplace payment intent:", {
        message: error.message,
        code: error.code,
        type: error.type,
      });

      // If it's already a Firebase Functions error, re-throw it
      if (error.httpErrorCode) {
        throw error;
      }

      // Handle Stripe-specific errors
      if (error.type && error.type.includes("Stripe")) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          `Payment error: ${error.message}`
        );
      }

      throw new functions.https.HttpsError(
        "internal",
        `Failed to create payment: ${error.message || "Unknown error"}`
      );
    }
  }
);

/**
 * Cancels a marketplace ticket reservation.
 * Called when the user abandons the payment flow.
 *
 * @param data - Request containing ticketId
 * @param context - Firebase auth context
 */
export const cancelMarketplaceReservation = functions.https.onCall(
  async (data: {ticketId: string}, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const userId = context.auth.uid;
    const {ticketId} = data;

    if (!ticketId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Ticket ID is required"
      );
    }

    const db = admin.firestore();
    const ticketRef = db.collection("tickets").doc(ticketId);

    await db.runTransaction(async (transaction) => {
      const ticketDoc = await transaction.get(ticketRef);
      
      if (!ticketDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Ticket not found");
      }

      const ticketData = ticketDoc.data();
      
      // Only the user who reserved can cancel
      if (ticketData?.reservedBy !== userId) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "You can only cancel your own reservation"
        );
      }

      // Clear reservation
      transaction.update(ticketRef, {
        reservedBy: admin.firestore.FieldValue.delete(),
        reservedUntil: admin.firestore.FieldValue.delete(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    console.log(`✓ Reservation cancelled for ticket ${ticketId} by user ${userId}`);

    return {success: true};
  }
);
