package com.ramsiers.graniteapp.drawing.auth;

import java.net.URI;

/** Keeps private device credentials on the Ramsiers-owned HTTPS drawing service only. */
public final class DrawingEndpointAuthPolicy {
    private static final String PROTECTED_HOST = "ramsiers-drawing-ai-service.vercel.app";
    private static final String ANALYZE_PATH = "/api/analyze";

    private DrawingEndpointAuthPolicy() {
    }

    public static boolean requiresDeviceAuthentication(String endpoint) {
        if (endpoint == null) return false;
        try {
            URI uri = new URI(endpoint);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && PROTECTED_HOST.equalsIgnoreCase(uri.getHost())
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && uri.getUserInfo() == null
                    && ANALYZE_PATH.equals(uri.getPath())
                    && uri.getFragment() == null;
        } catch (Exception ignored) {
            return false;
        }
    }
}
