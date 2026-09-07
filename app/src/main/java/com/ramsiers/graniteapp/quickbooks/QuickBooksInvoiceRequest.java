package com.ramsiers.graniteapp.quickbooks;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable payload queued for QuickBooks Desktop invoice creation.
 * Contains business/customer/quote data only; never payment credentials.
 */
public final class QuickBooksInvoiceRequest {
    public static final class Line {
        public final String itemName;
        public final String description;
        public final long amountCents;

        public Line(String itemName, String description, long amountCents) {
            this.itemName = cleanRequired(itemName, "itemName");
            this.description = clean(description);
            if (amountCents < 0) throw new IllegalArgumentException("amountCents must be >= 0");
            this.amountCents = amountCents;
        }

        JSONObject toJson() throws Exception {
            JSONObject value = new JSONObject();
            value.put("itemName", itemName);
            value.put("description", description);
            value.put("amountCents", amountCents);
            return value;
        }
    }

    public final String jobId;
    public final String customerName;
    public final String phone;
    public final String email;
    public final String projectAddress;
    public final String memo;
    public final long totalCents;
    public final List<Line> lines;

    public QuickBooksInvoiceRequest(
            String jobId,
            String customerName,
            String phone,
            String email,
            String projectAddress,
            String memo,
            long totalCents,
            List<Line> lines) {
        this.jobId = cleanRequired(jobId, "jobId");
        this.customerName = cleanRequired(customerName, "customerName");
        this.phone = clean(phone);
        this.email = clean(email);
        this.projectAddress = clean(projectAddress);
        this.memo = clean(memo);
        if (totalCents <= 0) throw new IllegalArgumentException("totalCents must be > 0");
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("At least one invoice line is required");
        long lineTotal = 0;
        ArrayList<Line> copy = new ArrayList<>(lines.size());
        for (Line line : lines) {
            if (line == null) throw new IllegalArgumentException("Invoice lines cannot be null");
            lineTotal = Math.addExact(lineTotal, line.amountCents);
            copy.add(line);
        }
        if (lineTotal != totalCents) {
            throw new IllegalArgumentException("Invoice line total must equal totalCents");
        }
        this.totalCents = totalCents;
        this.lines = Collections.unmodifiableList(copy);
    }

    public JSONObject toJson() throws Exception {
        JSONObject value = new JSONObject();
        value.put("schemaVersion", 1);
        value.put("jobId", jobId);
        value.put("customerName", customerName);
        value.put("phone", phone);
        value.put("email", email);
        value.put("projectAddress", projectAddress);
        value.put("memo", memo);
        value.put("currency", "USD");
        value.put("totalCents", totalCents);
        JSONArray lineValues = new JSONArray();
        for (Line line : lines) lineValues.put(line.toJson());
        value.put("lines", lineValues);
        return value;
    }

    private static String cleanRequired(String value, String name) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) throw new IllegalArgumentException(name + " is required");
        return cleaned;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
