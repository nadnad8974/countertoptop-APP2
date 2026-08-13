package com.ramsiers.graniteapp.payment;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class PaymentLinkClient {
    private PaymentLinkClient() {
    }

    public static String create(
            String endpoint,
            long amountCents,
            String quoteReference) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(30_000);
            connection.setReadTimeout(45_000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-Ramsiers-App", "countertop-quote-v1");

            byte[] requestBody = PaymentLinkRequest.create(amountCents, quoteReference)
                    .toString()
                    .getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(requestBody.length);
            try (java.io.OutputStream output = connection.getOutputStream()) {
                output.write(requestBody);
            }

            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            JSONObject response = new JSONObject(readText(responseStream));
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException(response.optString(
                        "error",
                        "A secure payment link could not be created."));
            }
            return checkoutUrl(response.optString("url", ""));
        } finally {
            connection.disconnect();
        }
    }

    static String checkoutUrl(String value) throws Exception {
        URI uri = new URI(value);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"checkout.stripe.com".equalsIgnoreCase(uri.getHost())) {
            throw new IllegalStateException("The payment service returned an invalid link.");
        }
        return uri.toString();
    }

    private static String readText(InputStream stream) throws Exception {
        if (stream == null) return "{}";
        try (InputStream input = stream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
