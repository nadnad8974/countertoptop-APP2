package com.ramsiers.graniteapp.quickbooks;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class QuickBooksInvoiceRequestTest {
    @Test
    public void serializesInvoiceWithExactUsdTotal() throws Exception {
        QuickBooksInvoiceRequest request = new QuickBooksInvoiceRequest(
                "job_123",
                "Test Customer",
                "440-555-0100",
                "test@example.com",
                "123 Main St",
                "Ramsiers countertop job",
                125000,
                Arrays.asList(
                        new QuickBooksInvoiceRequest.Line(
                                "Countertop and installation",
                                "20 sq. ft.",
                                120000),
                        new QuickBooksInvoiceRequest.Line(
                                "Sink cutout",
                                "1 cutout",
                                5000)));

        assertEquals("USD", request.toJson().getString("currency"));
        assertEquals(125000L, request.toJson().getLong("totalCents"));
        assertEquals(2, request.toJson().getJSONArray("lines").length());
        assertEquals("job_123", request.toJson().getString("jobId"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMismatchedLineTotal() {
        new QuickBooksInvoiceRequest(
                "job_123",
                "Test Customer",
                "",
                "",
                "",
                "",
                10000,
                Arrays.asList(new QuickBooksInvoiceRequest.Line("Countertop", "", 9999)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingJobId() {
        new QuickBooksInvoiceRequest(
                "",
                "Test Customer",
                "",
                "",
                "",
                "",
                10000,
                Arrays.asList(new QuickBooksInvoiceRequest.Line("Countertop", "", 10000)));
    }
}
