package com.ramsiers.graniteapp.payment;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PaymentLinkClientTest {
    @Test
    public void acceptsStripeHostedCheckoutUrl() throws Exception {
        assertEquals(
                "https://checkout.stripe.com/c/pay/cs_test_example",
                PaymentLinkClient.checkoutUrl(
                        "https://checkout.stripe.com/c/pay/cs_test_example"));
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsNonStripeUrl() throws Exception {
        PaymentLinkClient.checkoutUrl("https://example.com/not-stripe");
    }
}
