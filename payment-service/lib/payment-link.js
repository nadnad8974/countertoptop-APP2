const MIN_AMOUNT_CENTS = 50;
const MAX_AMOUNT_CENTS = 100_000_000;
const QUOTE_REFERENCE = /^rq_[0-9a-f]{32}$/;
const ALLOWED_FIELDS = new Set(["amountCents", "quoteReference"]);

export function validatePaymentRequest(body) {
  if (!body || typeof body !== "object" || Array.isArray(body)) {
    throw new PaymentRequestError("The payment request is invalid.");
  }

  const fields = Object.keys(body);
  if (fields.some((field) => !ALLOWED_FIELDS.has(field))) {
    throw new PaymentRequestError("The payment request contains unsupported information.");
  }
  if (!Number.isInteger(body.amountCents)
      || body.amountCents < MIN_AMOUNT_CENTS
      || body.amountCents > MAX_AMOUNT_CENTS) {
    throw new PaymentRequestError("The quote amount is invalid.");
  }
  if (typeof body.quoteReference !== "string"
      || !QUOTE_REFERENCE.test(body.quoteReference)) {
    throw new PaymentRequestError("The quote reference is invalid.");
  }

  return {
    amountCents: body.amountCents,
    quoteReference: body.quoteReference
  };
}

export function checkoutSessionParameters(payment, publicBaseUrl, nowSeconds) {
  const baseUrl = normalizedBaseUrl(publicBaseUrl);
  return {
    mode: "payment",
    payment_method_types: ["us_bank_account"],
    client_reference_id: payment.quoteReference,
    line_items: [
      {
        quantity: 1,
        price_data: {
          currency: "usd",
          unit_amount: payment.amountCents,
          product_data: {
            name: "Ramsier's Granite & Quartz — completed countertop job"
          }
        }
      }
    ],
    metadata: {
      quote_reference: payment.quoteReference
    },
    payment_intent_data: {
      metadata: {
        quote_reference: payment.quoteReference
      }
    },
    payment_method_options: {
      us_bank_account: {
        financial_connections: {
          permissions: ["payment_method"]
        },
        verification_method: "automatic"
      }
    },
    success_url: `${baseUrl}/payment-complete`,
    cancel_url: `${baseUrl}/payment-canceled`,
    expires_at: nowSeconds + (23 * 60 * 60),
    integration_identifier: "ramsiers_android_qmvtzjke"
  };
}

export function idempotencyKey(quoteReference) {
  return `ramsiers-${quoteReference}`;
}

export function paymentServiceBaseUrl(environment) {
  if (environment.PUBLIC_BASE_URL) {
    return normalizedBaseUrl(environment.PUBLIC_BASE_URL);
  }
  const vercelHost = environment.VERCEL_PROJECT_PRODUCTION_URL || environment.VERCEL_URL;
  if (!vercelHost) {
    throw new Error("PUBLIC_BASE_URL is not configured.");
  }
  return normalizedBaseUrl(`https://${vercelHost}`);
}

function normalizedBaseUrl(value) {
  const url = new URL(value);
  if (url.protocol !== "https:") {
    throw new Error("The public payment service URL must use HTTPS.");
  }
  return url.toString().replace(/\/$/, "");
}

export class PaymentRequestError extends Error {}
