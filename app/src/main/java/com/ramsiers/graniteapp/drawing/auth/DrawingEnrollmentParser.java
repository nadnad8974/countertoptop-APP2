package com.ramsiers.graniteapp.drawing.auth;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** Strict parser for private Drawing AI phone-enrollment QR codes. */
public final class DrawingEnrollmentParser {
    private static final String SCHEME = "ramsiers-drawing";
    private static final String AUTHORITY = "enroll";

    private DrawingEnrollmentParser() {
    }

    public static DrawingDeviceCredential parse(String rawValue) {
        if (rawValue == null || rawValue.isEmpty() || !rawValue.equals(rawValue.trim())) {
            throw invalid();
        }

        final URI uri;
        try {
            uri = new URI(rawValue);
        } catch (Exception exception) {
            throw invalid();
        }

        String rawPath = uri.getRawPath();
        if (!SCHEME.equals(uri.getScheme())
                || !AUTHORITY.equals(uri.getRawAuthority())
                || (rawPath != null && !rawPath.isEmpty())
                || uri.getRawFragment() != null
                || uri.getRawQuery() == null) {
            throw invalid();
        }

        String[] pairs = uri.getRawQuery().split("&", -1);
        if (pairs.length != 2) throw invalid();
        Map<String, String> values = new HashMap<>();
        for (String pair : pairs) {
            int separator = pair.indexOf('=');
            if (separator <= 0
                    || separator != pair.lastIndexOf('=')
                    || separator == pair.length() - 1) {
                throw invalid();
            }
            String key = pair.substring(0, separator);
            String value = pair.substring(separator + 1);
            if (!("device".equals(key) || "token".equals(key))
                    || values.put(key, value) != null) {
                throw invalid();
            }
        }

        String deviceId = values.get("device");
        String token = values.get("token");
        if (!DrawingDeviceCredential.isValidDeviceId(deviceId)
                || !DrawingDeviceCredential.isValidToken(token)) {
            throw invalid();
        }
        return new DrawingDeviceCredential(deviceId, token);
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Invalid Drawing AI connection QR code.");
    }
}
