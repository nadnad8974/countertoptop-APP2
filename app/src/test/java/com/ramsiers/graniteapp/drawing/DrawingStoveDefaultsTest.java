package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class DrawingStoveDefaultsTest {
    @Test
    public void bothMissingDefaultsOnlyAcrossRunLength() throws Exception {
        JSONObject part = new JSONObject()
                .put("four_burner_symbol", true);
        JSONArray parts = new JSONArray().put(part);

        DrawingStoveDefaults.apply(parts, null);

        assertEquals("stove", part.getString("feature_type"));
        assertEquals(30, part.getDouble("length_inches"), 0.001);
        assertFalse(part.has("width_inches"));
        assertEquals("subtract", part.getString("operation"));
        assertTrue(part.getBoolean("stove_default_applied"));
    }

    @Test
    public void missingLengthWithExplicitDepthDefaultsOnlyLength() throws Exception {
        JSONObject part = new JSONObject()
                .put("four_burner_symbol", true)
                .put("width_inches", 25.5);
        JSONArray parts = new JSONArray().put(part);

        DrawingStoveDefaults.apply(parts, null);

        assertEquals("stove", part.getString("feature_type"));
        assertEquals(30, part.getDouble("length_inches"), 0.001);
        assertEquals(25.5, part.getDouble("width_inches"), 0.001);
    }

    @Test
    public void explicitCooktopMarkingDoesNotBecomeStove() throws Exception {
        JSONObject part = new JSONObject()
                .put("feature_type", "cooktop")
                .put("label", "Cooktop opening")
                .put("width_inches", 25.5);

        DrawingStoveDefaults.apply(new JSONArray().put(part), null);

        assertEquals("cooktop", part.getString("feature_type"));
        assertFalse(part.has("stove_default_applied"));
    }

    @Test
    public void strictLinkedCooktopObjectsStayIgnoredEvenWithFourBurners() throws Exception {
        JSONObject countertop = new JSONObject()
                .put("feature_type", "countertop")
                .put("operation", "add")
                .put("length_inches", 120)
                .put("width_inches", 24);
        JSONObject cooktopPart = new JSONObject()
                .put("feature_type", "cooktop")
                .put("link_id", "cooktop_1")
                .put("burner_count", 4);
        JSONObject cooktopShape = new JSONObject()
                .put("kind", "opening")
                .put("opening_type", "cooktop")
                .put("link_id", "cooktop_1")
                .put("burner_count", 4);
        JSONArray parts = new JSONArray().put(countertop).put(cooktopPart);
        JSONObject drawing = new JSONObject()
                .put("shapes", new JSONArray().put(cooktopShape));

        DrawingStoveDefaults.apply(parts, drawing);

        assertEquals("cooktop", cooktopPart.getString("feature_type"));
        assertEquals("cooktop", cooktopShape.getString("opening_type"));
        assertFalse(cooktopPart.has("stove_default_applied"));
        assertFalse(cooktopShape.has("stove_default_applied"));
        assertFalse(cooktopPart.has("length_inches"));
        assertFalse(cooktopPart.has("width_inches"));
        assertEquals(20, DrawingMath.squareFeet(parts), 0.001);
    }

    @Test
    public void explicitLengthWithMissingDepthDoesNotInventDepth() throws Exception {
        JSONObject part = new JSONObject()
                .put("feature_type", "stove")
                .put("length_inches", 36);

        DrawingStoveDefaults.apply(new JSONArray().put(part), null);

        assertEquals(36, part.getDouble("length_inches"), 0.001);
        assertFalse(part.has("width_inches"));
    }

    @Test
    public void explicitLengthAndDepthRemainUnchanged() throws Exception {
        JSONObject part = new JSONObject()
                .put("feature_type", "stove")
                .put("length_inches", 36)
                .put("width_inches", 25.5);

        DrawingStoveDefaults.apply(new JSONArray().put(part), null);

        assertEquals(36, part.getDouble("length_inches"), 0.001);
        assertEquals(25.5, part.getDouble("width_inches"), 0.001);
    }
}
