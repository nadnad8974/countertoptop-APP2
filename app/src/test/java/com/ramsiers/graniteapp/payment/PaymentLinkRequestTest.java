package com.ramsiers.graniteapp.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.json.JSONObject;
import org.junit.Test;

public class PaymentLinkRequestTest {
    @Test
    public void sendsOnlyAmountAndOpaqueReference() {
        JSONObject body = PaymentLinkRequest.create(
                576400,
                "rq_0123456789abcdef0123456789abcdef");

        assertEquals(2, body.length());
        assertEquals(576400, body.getLong("amountCents"));
        assertEquals(
                "rq_0123456789abcdef0123456789abcdef",
                body.getString("quoteReference"));
        assertFalse(body.has("customer"));
        assertFalse(body.has("phone"));
        assertFalse(body.has("address"));
        assertFalse(body.has("drawing"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCustomerNameAsQuoteReference() {
        PaymentLinkRequest.create(576400, "Dan-Ramsier");
    }
}
