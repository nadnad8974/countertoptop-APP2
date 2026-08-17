package com.ramsiers.graniteapp.drawing.auth;

import java.util.regex.Pattern;

/** A validated Drawing AI phone credential. The token must never be shown to the user. */
public final class DrawingDeviceCredential {
    private static final Pattern DEVICE_ID =
            Pattern.compile("^[a-z0-9][a-z0-9._-]{7,63}$");
    private static final Pattern TOKEN =
            Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final String deviceId;
    private final String token;

    public DrawingDeviceCredential(String deviceId, String token) {
        if (!isValidDeviceId(deviceId) || !isValidToken(token)) {
            throw new IllegalArgumentException("Invalid Drawing AI phone credential.");
        }
        this.deviceId = deviceId;
        this.token = token;
    }

    public String deviceId() {
        return deviceId;
    }

    public String token() {
        return token;
    }

    public static boolean isValidDeviceId(String value) {
        return value != null && DEVICE_ID.matcher(value).matches();
    }

    public static boolean isValidToken(String value) {
        return value != null && TOKEN.matcher(value).matches();
    }

    @Override
    public String toString() {
        return "DrawingDeviceCredential{deviceId='" + deviceId + "'}";
    }
}
