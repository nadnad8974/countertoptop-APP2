import Stripe from "stripe";
import {
  PaymentRequestError,
  checkoutSessionParameters,
  idempotencyKey,
  paymentServiceBaseUrl,
  validatePaymentRequest
} from "../lib/payment-link.js";

export default async function handler(request, response) {
  response.setHeader("Cache-Control", "no-store");
  response.setHeader("Content-Type", "application/json; charset=utf-8");

  if (request.method !== "POST") {
    response.setHeader("Allow", "POST");
    return response.status(405).json({ error: "Method not allowed." });
  }

  const contentType = String(request.headers["content-type"] || "").toLowerCase();
  if (!contentType.startsWith("application/json")) {
    return response.status(415).json({ error: "JSON is required." });
  }
  if (request.headers["x-ramsiers-app"] !== "countertop-quote-v1") {
    return response.status(403).json({ error: "This payment request is not allowed." });
  }

  try {
    const payment = validatePaymentRequest(request.body);
    const stripeKey = process.env.STRIPE_RESTRICTED_KEY;
    if (!stripeKey) {
      throw new Error("Stripe is not configured.");
    }

    const stripe = new Stripe(stripeKey, {
      apiVersion: "2026-06-24.dahlia",
      maxNetworkRetries: 2
    });
    const session = await stripe.checkout.sessions.create(
      checkoutSessionParameters(
        payment,
        paymentServiceBaseUrl(process.env),
        Math.floor(Date.now() / 1000)),
      { idempotencyKey: idempotencyKey(payment.quoteReference) }
    );

    if (!session.url || !session.url.startsWith("https://checkout.stripe.com/")) {
      throw new Error("Stripe did not return a secure Checkout URL.");
    }
    return response.status(200).json({ url: session.url });
  } catch (error) {
    if (error instanceof PaymentRequestError) {
      return response.status(400).json({ error: error.message });
    }
    return response.status(503).json({
      error: "A secure payment link could not be created. Please try again."
    });
  }
}
