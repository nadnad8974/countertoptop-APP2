package com.ramsiers.graniteapp.drawing.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DrawingEnrollmentParserTest {
    private static final String TOKEN = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopq";

    @Test
    public void parsesExactEnrollmentQr() {
        DrawingDeviceCredential credential = DrawingEnrollmentParser.parse(
                "ramsiers-drawing://enroll?device=shop-phone-01&token=" + TOKEN);

        assertEquals("shop-phone-01", credential.deviceId());
        assertEquals(TOKEN, credential.token());
        assertFalse(credential.toString().contains(TOKEN));
    }

    @Test
    public void acceptsParametersInEitherOrder() {
        DrawingDeviceCredential credential = DrawingEnrollmentParser.parse(
                "ramsiers-drawing://enroll?token=" + TOKEN + "&device=phone.01");

        assertEquals("phone.01", credential.deviceId());
    }

    @Test
    public void acceptsDeviceIdBoundaries() {
        DrawingEnrollmentParser.parse(
                "ramsiers-drawing://enroll?device=phone001&token=" + TOKEN);
        DrawingEnrollmentParser.parse(
                "ramsiers-drawing://enroll?device=" + "a".repeat(64) + "&token=" + TOKEN);
    }

    @Test
    public void rejectsWrongLocationOrExtraUrlParts() {
        assertInvalid("https://enroll?device=shop-phone-01&token=" + TOKEN);
        assertInvalid("ramsiers-drawing://other?device=shop-phone-01&token=" + TOKEN);
        assertInvalid("ramsiers-drawing://enroll/?device=shop-phone-01&token=" + TOKEN);
        assertInvalid("ramsiers-drawing://enroll?device=shop-phone-01&token=" + TOKEN + "#x");
    }

    @Test
    public void rejectsMissingDuplicateOrExtraParameters() {
        assertInvalid("ramsiers-drawing://enroll?device=shop-phone-01");
        assertInvalid("ramsiers-drawing://enroll?device=shop-phone-01&device=phone002");
        assertInvalid("ramsiers-drawing://enroll?device=shop-phone-01&token=" + TOKEN + "&x=1");
        assertInvalid("ramsiers-drawing://enroll?device=shop-phone-01&token=" + TOKEN + "&");
    }

    @Test
    public void rejectsInvalidDeviceIds() {
        assertInvalid("ramsiers-drawing://enroll?device=short&token=" + TOKEN);
        assertInvalid("ramsiers-drawing://enroll?device=Shop-phone-01&token=" + TOKEN);
        assertInvalid("ramsiers-drawing://enroll?device=-phone001&token=" + TOKEN);
        assertInvalid("ramsiers-drawing://enroll?device=phone%2D01&token=" + TOKEN);
        assertInvalid("ramsiers-drawing://enroll?device=" + "a".repeat(65) + "&token=" + TOKEN);
    }

    @Test
    public void rejectsInvalidTokensWithoutEchoingThem() {
        assertInvalid("ramsiers-drawing://enroll?device=phone001&token=" + "A".repeat(42));
        assertInvalid("ramsiers-drawing://enroll?device=phone001&token=" + "A".repeat(44));
        assertInvalid("ramsiers-drawing://enroll?device=phone001&token=" + "A".repeat(42) + "=");
        assertInvalid("ramsiers-drawing://enroll?device=phone001&token=" + "A".repeat(42) + "+");
    }

    @Test
    public void rejectsWhitespaceAndNull() {
        assertInvalid(null);
        assertInvalid("");
        assertInvalid(" ramsiers-drawing://enroll?device=phone001&token=" + TOKEN);
        assertInvalid("ramsiers-drawing://enroll?device=phone001&token=" + TOKEN + " ");
    }

    private static void assertInvalid(String raw) {
        try {
            DrawingEnrollmentParser.parse(raw);
        } catch (IllegalArgumentException exception) {
            assertEquals("Invalid Drawing AI connection QR code.", exception.getMessage());
            if (raw != null && raw.contains("token=")) {
                String token = raw.substring(raw.indexOf("token=") + 6);
                assertFalse(exception.getMessage().contains(token));
            }
            return;
        }
        throw new AssertionError("Expected invalid enrollment QR.");
    }
}
