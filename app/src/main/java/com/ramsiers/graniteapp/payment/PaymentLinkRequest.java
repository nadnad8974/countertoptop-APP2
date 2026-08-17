package com.ramsiers.graniteapp.payment;

import org.json.JSONObject;

public final class PaymentLinkRequest {
    private static final long MIN_AMOUNT_CENTS = 50;
    private static final long MAX_AMOUNT_CENTS = 100_000_000;
    private static final String QUOTE_REFERENCE_PATTERN = "rq_[0-9a-f]{32}";

    private PaymentLinkRequest() {
    }

    public static JSONObject create(long amountCents, String quoteReference) {
        if (amountCents < MIN_AMOUNT_CENTS || amountCents > MAX_AMOUNT_CENTS) {
            throw new IllegalArgumentException("The quote amount is invalid.");
        }
        if (quoteReference == null || !quoteReference.matches(QUOTE_REFERENCE_PATTERN)) {
            throw new IllegalArgumentException("The quote reference is invalid.");
        }

        JSONObject body = new JSONObject();
        try {
            body.put("amountCents", amountCents);
            body.put("quoteReference", quoteReference);
        } catch (Exception exception) {
            throw new IllegalStateException("The payment request could not be created.", exception);
        }
        return body;
    }
}
