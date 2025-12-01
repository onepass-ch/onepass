/**
 * Creates a Stripe PaymentIntent for ticket purchases
 */

import * as functions from "firebase-functions";
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
    // Verify user is authenticated
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated to create a payment"
      );
    }

    const userId = context.auth.uid;
    const {amount, currency = DEFAULT_CURRENCY, eventId, ticketTypeId, quantity = 1, description} = data;

    // Validate input
    if (!amount || typeof amount !== "number") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Amount is required and must be a number"
      );
    }

    if (amount < MIN_PAYMENT_AMOUNT) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Amount must be at least ${MIN_PAYMENT_AMOUNT} cents (CHF ${MIN_PAYMENT_AMOUNT / 100})`
      );
    }

    if (amount > MAX_PAYMENT_AMOUNT) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Amount cannot exceed ${MAX_PAYMENT_AMOUNT} cents (CHF ${MAX_PAYMENT_AMOUNT / 100})`
      );
    }

    if (!eventId || typeof eventId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Event ID is required"
      );
    }

    try {
      const db = admin.firestore();

      // Verify event exists
      const eventDoc = await db.collection("events").doc(eventId).get();
      if (!eventDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "Event not found"
        );
      }

      const eventData = eventDoc.data();

      // Get or create Stripe customer for the user
      const userDoc = await db.collection("users").doc(userId).get();
      const userData = userDoc.data();

      let customerId = userData?.stripeCustomerId as string | undefined;

      // Create Stripe customer if doesn't exist
      if (!customerId) {
        const customer = await stripe.customers.create({
          metadata: {
            firebaseUID: userId,
          },
          email: userData?.email,
          name: userData?.displayName,
        });

        customerId = customer.id;

        // Save customer ID to user document
        await db.collection("users").doc(userId).update({
          stripeCustomerId: customerId,
        });

        console.log(`Created Stripe customer ${customerId} for user ${userId}`);
      }

      // Create payment intent
      const paymentIntent = await stripe.paymentIntents.create({
        amount: Math.round(amount),
        currency: currency.toLowerCase(),
        customer: customerId,
        metadata: {
          eventId: eventId,
          userId: userId,
          eventName: eventData?.name || "",
          ticketTypeId: ticketTypeId || "",
          quantity: quantity.toString(),
        },
        description: description || `Ticket purchase for ${eventData?.name || "event"}`,
        automatic_payment_methods: {
          enabled: true,
        },
      });

      // Store payment intent reference in Firestore
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

      console.log(
        `Created payment intent ${paymentIntent.id} for user ${userId}, event ${eventId}, amount ${amount}`
      );

      return {
        clientSecret: paymentIntent.client_secret,
        paymentIntentId: paymentIntent.id,
        customerId: customerId,
      };
    } catch (error: any) {
      console.error("Error creating payment intent:", error);

      // Handle Stripe-specific errors
      if (error.type === "StripeCardError") {
        throw new functions.https.HttpsError("failed-precondition", error.message);
      }

      throw new functions.https.HttpsError(
        "internal",
        `Failed to create payment intent: ${error.message}`
      );
    }
  }
);
