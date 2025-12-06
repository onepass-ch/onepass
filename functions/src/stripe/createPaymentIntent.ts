/**
 * Creates a Stripe PaymentIntent for ticket purchases
 */

import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {stripe, DEFAULT_CURRENCY, MIN_PAYMENT_AMOUNT, MAX_PAYMENT_AMOUNT} from "./config";

interface CreatePaymentIntentRequest {
  amount: number;
  currency?: string;
  eventId: string;
  ticketTypeId?: string;
  quantity?: number;
  description?: string;
}

/**
 * Creates a payment intent for purchasing event tickets.
 * The client uses the returned clientSecret to complete the payment.
 *
 * @param data - Payment intent creation request
 * @param context - Firebase auth context
 * @returns Object containing clientSecret and paymentIntentId
 */
export const createPaymentIntent = functions.https.onCall(
  async (data: CreatePaymentIntentRequest, context) => {
    // Log function invocation
    console.log("🎫 createPaymentIntent called with data:", {
      amount: data?.amount,
      currency: data?.currency,
      eventId: data?.eventId,
      ticketTypeId: data?.ticketTypeId,
      quantity: data?.quantity,
      hasDescription: !!data?.description,
    });
    
    // Verify user is authenticated
    if (!context.auth) {
      console.error("❌ Authentication failed: No auth token provided");
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated to create a payment"
      );
    }

    const userId = context.auth.uid;
    console.log(`✓ User authenticated: ${userId}`);
    
    const {amount, currency = DEFAULT_CURRENCY, eventId, ticketTypeId, quantity = 1, description} = data;

    // Validate input
    if (!amount || typeof amount !== "number") {
      console.error("❌ Invalid amount:", amount);
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Amount is required and must be a number"
      );
    }

    if (amount < MIN_PAYMENT_AMOUNT) {
      console.error(`❌ Amount too small: ${amount} cents (minimum ${MIN_PAYMENT_AMOUNT})`);
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Amount must be at least ${MIN_PAYMENT_AMOUNT} cents (CHF ${MIN_PAYMENT_AMOUNT / 100})`
      );
    }

    if (amount > MAX_PAYMENT_AMOUNT) {
      console.error(`❌ Amount too large: ${amount} cents (maximum ${MAX_PAYMENT_AMOUNT})`);
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Amount cannot exceed ${MAX_PAYMENT_AMOUNT} cents (CHF ${MAX_PAYMENT_AMOUNT / 100})`
      );
    }

    if (!eventId || typeof eventId !== "string") {
      console.error("❌ Invalid eventId:", eventId);
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Event ID is required"
      );
    }

    console.log(`✓ Input validation passed - Amount: ${amount} cents, Event: ${eventId}`);

    try {
      const db = admin.firestore();

      // Verify event exists
      console.log(`🔍 Looking up event: ${eventId}`);
      const eventDoc = await db.collection("events").doc(eventId).get();
      if (!eventDoc.exists) {
        console.error(`❌ Event not found: ${eventId}`);
        throw new functions.https.HttpsError(
          "not-found",
          "Event not found"
        );
      }

      const eventData = eventDoc.data();
      console.log(`✓ Event found: ${eventData?.title || eventData?.name || "Untitled"}`);

      // Check ticket availability before creating payment
      const ticketsRemaining = eventData?.ticketsRemaining ?? 0;
      if (ticketsRemaining <= 0) {
        console.error(`❌ No tickets remaining for event: ${eventId} (ticketsRemaining: ${ticketsRemaining})`);
        throw new functions.https.HttpsError(
          "failed-precondition",
          "No tickets remaining for this event"
        );
      }

      // If a specific tier is selected, check its availability
      if (ticketTypeId) {
        const pricingTiers = eventData?.pricingTiers || [];
        const selectedTier = pricingTiers.find((tier: any) => tier.name === ticketTypeId);
        
        if (!selectedTier) {
          console.error(`❌ Pricing tier not found: ${ticketTypeId}`);
          throw new functions.https.HttpsError(
            "not-found",
            `Pricing tier "${ticketTypeId}" not found for this event`
          );
        }

        const tierRemaining = selectedTier.remaining ?? 0;
        if (tierRemaining < quantity) {
          console.error(`❌ Insufficient tickets in tier: ${ticketTypeId} (remaining: ${tierRemaining}, requested: ${quantity})`);
          throw new functions.https.HttpsError(
            "failed-precondition",
            `Only ${tierRemaining} ticket(s) remaining in tier "${ticketTypeId}"`
          );
        }
      } else {
        // If no specific tier, check if requested quantity is available
        if (ticketsRemaining < quantity) {
          console.error(`❌ Insufficient tickets available: ${ticketsRemaining} remaining, ${quantity} requested`);
          throw new functions.https.HttpsError(
            "failed-precondition",
            `Only ${ticketsRemaining} ticket(s) remaining for this event`
          );
        }
      }

      console.log(`✓ Ticket availability check passed - Event: ${ticketsRemaining} remaining, Quantity: ${quantity}`);

      // Get or create user document and Stripe customer
      console.log(`🔍 Looking up user document: ${userId}`);
      const userDocRef = db.collection("users").doc(userId);
      const userDoc = await userDocRef.get();
      let userData = userDoc.data();

      // If user document doesn't exist, create it with basic info from auth token
      if (!userDoc.exists || !userData) {
        console.log(`⚠️ User document not found, creating new document for ${userId}`);
        // Get user info from Firebase Auth (if available)
        const authUser = await admin.auth().getUser(userId).catch((err) => {
          console.error(`⚠️ Could not fetch auth user: ${err.message}`);
          return null;
        });
        
        userData = {
          email: authUser?.email || "",
          displayName: authUser?.displayName || "",
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        };
        
        // Create the user document
        await userDocRef.set(userData, { merge: true });
        console.log(`✓ Created user document for ${userId} with email: ${userData.email}`);
      } else {
        console.log(`✓ User document found: ${userData?.email || "no email"}`);
      }

      let customerId = userData?.stripeCustomerId as string | undefined;

      // Create Stripe customer if doesn't exist
      if (!customerId) {
        console.log(`🔨 Creating new Stripe customer for user ${userId}`);
        try {
          const customer = await stripe.customers.create({
            metadata: {
              firebaseUID: userId,
            },
            email: userData?.email || undefined,
            name: userData?.displayName || undefined,
          });

          customerId = customer.id;

          // Save customer ID to user document
          await userDocRef.update({
            stripeCustomerId: customerId,
          });

          console.log(`✓ Created Stripe customer ${customerId} for user ${userId}`);
        } catch (stripeError: any) {
          console.error("❌ Failed to create Stripe customer:", {
            message: stripeError.message,
            type: stripeError.type,
            code: stripeError.code,
            statusCode: stripeError.statusCode,
          });
          throw stripeError;
        }
      } else {
        console.log(`✓ Using existing Stripe customer: ${customerId}`);
      }

      // Create payment intent
      console.log(`🔨 Creating payment intent - Amount: ${amount}, Currency: ${currency}`);
      const paymentIntent = await stripe.paymentIntents.create({
        amount: Math.round(amount),
        currency: currency.toLowerCase(),
        customer: customerId,
        metadata: {
          eventId: eventId,
          userId: userId,
          eventName: eventData?.title || eventData?.name || "",
          ticketTypeId: ticketTypeId || "",
          quantity: quantity.toString(),
        },
        description: description || `Ticket purchase for ${eventData?.title || eventData?.name || "event"}`,
        automatic_payment_methods: {
          enabled: true,
        },
      });

      console.log(`✓ Payment intent created: ${paymentIntent.id}, status: ${paymentIntent.status}`);

      // Store payment intent reference in Firestore
      console.log(`💾 Storing payment record in Firestore`);
      await db.collection("payments").doc(paymentIntent.id).set({
        userId: userId,
        eventId: eventId,
        amount: amount,
        currency: currency,
        status: paymentIntent.status,
        stripePaymentIntentId: paymentIntent.id,
        ticketTypeId: ticketTypeId || null,
        quantity: quantity,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      console.log(`✅ Payment intent created successfully: ${paymentIntent.id} for user ${userId}, event ${eventId}, amount ${amount} cents`);

      return {
        clientSecret: paymentIntent.client_secret,
        paymentIntentId: paymentIntent.id,
        customerId: customerId,
      };
    } catch (error: any) {
      // Comprehensive error logging
      console.error("❌❌❌ Error creating payment intent:", {
        message: error.message,
        type: error.type,
        code: error.code,
        statusCode: error.statusCode,
        stack: error.stack,
        name: error.name,
      });

      // If this is already a Firebase Functions error, re-throw it
      if (error.httpErrorCode) {
        throw error;
      }

      // Handle Stripe-specific errors
      if (error.type && error.type.includes("Stripe")) {
        console.error("❌ Stripe API error:", {
          type: error.type,
          code: error.code,
          param: error.param,
          message: error.message,
        });
        throw new functions.https.HttpsError(
          "failed-precondition", 
          `Stripe error: ${error.message}`
        );
      }

      // Generic error
      throw new functions.https.HttpsError(
        "internal",
        `Failed to create payment intent: ${error.message || "Unknown error"}`
      );
    }
  }
);
