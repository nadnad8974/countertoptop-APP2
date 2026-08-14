import assert from "node:assert/strict";
import test from "node:test";

import {
  PaymentRequestError,
  checkoutSessionParameters,
  idempotencyKey,
  paymentServiceBaseUrl,
  validatePaymentRequest
} from "../lib/payment-link.js";

const PAYMENT = {
  amountCents: 576400,
  quoteReference: "rq_0123456789abcdef0123456789abcdef"
};

test("accepts only an amount and opaque quote reference", () => {
  assert.deepEqual(validatePaymentRequest(PAYMENT), PAYMENT);
});

test("rejects customer information", () => {
  assert.throws(
    () => validatePaymentRequest({ ...PAYMENT, phone: "555-555-5555" }),
    PaymentRequestError
  );
  assert.throws(
    () => validatePaymentRequest({ ...PAYMENT, customerName: "Customer" }),
    PaymentRequestError
  );
  assert.throws(
    () => validatePaymentRequest({ ...PAYMENT, drawing: "data:image/jpeg;base64,..." }),
    PaymentRequestError
  );
});

test("builds a one-time ACH-only Checkout Session", () => {
  const parameters = checkoutSessionParameters(PAYMENT, "https://payments.example.com/", 1_800_000_000);
  assert.equal(parameters.mode, "payment");
  assert.equal(parameters.client_reference_id, PAYMENT.quoteReference);
  assert.equal(parameters.line_items[0].price_data.unit_amount, PAYMENT.amountCents);
  assert.equal(parameters.line_items[0].price_data.currency, "usd");
  assert.equal(parameters.metadata.quote_reference, PAYMENT.quoteReference);
  assert.equal(parameters.payment_intent_data.metadata.quote_reference, PAYMENT.quoteReference);
  assert.deepEqual(
    parameters.payment_method_options.us_bank_account.financial_connections.permissions,
    ["payment_method"]
  );
  assert.equal(
    parameters.payment_method_options.us_bank_account.verification_method,
    "automatic"
  );
  assert.equal(parameters.success_url, "https://payments.example.com/payment-complete");
  assert.equal(parameters.cancel_url, "https://payments.example.com/payment-canceled");
  assert.equal(parameters.expires_at, 1_800_082_800);
  assert.equal(parameters.integration_identifier, "ramsiers_android_qmvtzjke");
  assert.deepEqual(parameters.payment_method_types, ["us_bank_account"]);
  assert.equal(JSON.stringify(parameters).includes("customerName"), false);
  assert.equal(JSON.stringify(parameters).includes("phone"), false);
  assert.equal(JSON.stringify(parameters).includes("address"), false);
  assert.equal(JSON.stringify(parameters).includes("drawing"), false);
});

test("uses the opaque reference as the retry identity", () => {
  assert.equal(idempotencyKey(PAYMENT.quoteReference), `ramsiers-${PAYMENT.quoteReference}`);
});

test("requires a trusted HTTPS service URL", () => {
  assert.equal(
    paymentServiceBaseUrl({ VERCEL_PROJECT_PRODUCTION_URL: "payments.example.com" }),
    "https://payments.example.com"
  );
  assert.throws(() => paymentServiceBaseUrl({ PUBLIC_BASE_URL: "http://payments.example.com" }));
});
