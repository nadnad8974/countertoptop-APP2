package com.ramsiers.graniteapp.drawing.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DrawingEndpointAuthPolicyTest {
    @Test
    public void onlyExactProtectedHttpsEndpointCanReceiveDeviceCredential() {
        assertTrue(DrawingEndpointAuthPolicy.requiresDeviceAuthentication(
                "https://ramsiers-drawing-ai-service.vercel.app/api/analyze"));
        assertTrue(DrawingEndpointAuthPolicy.requiresDeviceAuthentication(
                "https://ramsiers-drawing-ai-service.vercel.app:443/api/analyze"));

        assertFalse(DrawingEndpointAuthPolicy.requiresDeviceAuthentication(
                "https://ramsiers-drawing-ai.nadnad8974.chatgpt.site/api/analyze"));
        assertFalse(DrawingEndpointAuthPolicy.requiresDeviceAuthentication(
                "http://ramsiers-drawing-ai-service.vercel.app/api/analyze"));
        assertFalse(DrawingEndpointAuthPolicy.requiresDeviceAuthentication(
                "https://evil.ramsiers-drawing-ai-service.vercel.app/api/analyze"));
        assertFalse(DrawingEndpointAuthPolicy.requiresDeviceAuthentication(
                "https://ramsiers-drawing-ai-service.vercel.app.evil.test/api/analyze"));
        assertFalse(DrawingEndpointAuthPolicy.requiresDeviceAuthentication(
                "https://ramsiers-drawing-ai-service.vercel.app/not-analyze"));
        assertFalse(DrawingEndpointAuthPolicy.requiresDeviceAuthentication(null));
    }
}
