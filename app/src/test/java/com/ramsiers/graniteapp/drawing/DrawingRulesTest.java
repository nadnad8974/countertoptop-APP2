package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class DrawingRulesTest {
    @Test
    public void incomingAiUserLabelIsRemoved() throws Exception {
        JSONObject drawing = drawingWithShape(new JSONObject()
                .put("kind", "countertop")
                .put("user_label", "AI guessed island"));

        JSONObject shape = DrawingRules.sanitizeServerDrawing(drawing)
                .getJSONArray("shapes")
                .getJSONObject(0);

        assertFalse(shape.has("user_label"));
        assertEquals("", DrawingRules.visibleUserLabel(shape));
    }

    @Test
    public void aiLabelAndNameNeverRender() throws Exception {
        JSONObject drawing = drawingWithShape(new JSONObject()
                .put("kind", "countertop")
                .put("label", "Long model description")
                .put("name", "AI name"));

        JSONObject shape = DrawingRules.sanitizeServerDrawing(drawing)
                .getJSONArray("shapes")
                .getJSONObject(0);

        assertFalse(shape.has("label"));
        assertFalse(shape.has("name"));
        assertEquals("", DrawingRules.visibleUserLabel(shape));
    }

    @Test
    public void locallyEnteredIslandLabelRenders() throws Exception {
        JSONObject shape = new JSONObject().put("feature_type", "countertop");
        shape.put("user_label", "Island");

        assertEquals("Island", DrawingRules.visibleUserLabel(shape));
    }

    @Test
    public void backsplashNeverRendersText() throws Exception {
        JSONObject shape = new JSONObject()
                .put("feature_type", "backsplash")
                .put("user_label", "Backsplash");

        assertEquals("", DrawingRules.visibleUserLabel(shape));
    }

    @Test
    public void typedStoveProducesStoveRenderingRule() throws Exception {
        JSONObject shape = new JSONObject()
                .put("kind", "opening")
                .put("feature_type", "stove");

        assertTrue(DrawingRules.isStove(shape));
    }

    @Test
    public void ordinaryOpeningDoesNotProduceStoveRenderingRule() throws Exception {
        JSONObject shape = new JSONObject()
                .put("kind", "opening")
                .put("feature_type", "other");

        assertFalse(DrawingRules.isStove(shape));
    }

    @Test
    public void legacyStoveWordingBecomesTypedThenIsRemoved() throws Exception {
        JSONObject drawing = drawingWithShape(new JSONObject()
                .put("kind", "opening")
                .put("label", "Stove opening"));

        JSONObject shape = DrawingRules.sanitizeServerDrawing(drawing)
                .getJSONArray("shapes")
                .getJSONObject(0);

        assertTrue(DrawingRules.isStove(shape));
        assertFalse(shape.has("label"));
    }

    @Test
    public void stoveAndCooktopSynonymsAreTyped() throws Exception {
        assertTrue(DrawingRules.isStove(new JSONObject()
                .put("feature_type", "slide_in_range")));
        assertEquals(
                DrawingRules.FEATURE_COOKTOP,
                DrawingRules.featureType(new JSONObject()
                        .put("feature_type", "cooktop_cutout")));
    }

    @Test
    public void matchingLegacyLabelsCreateInternalLinkWithoutVisibleText() throws Exception {
        JSONObject drawing = drawingWithShape(new JSONObject()
                .put("kind", "countertop")
                .put("label", "Left countertop"));
        JSONArray parts = new JSONArray().put(new JSONObject()
                .put("label", "Left countertop")
                .put("operation", "add")
                .put("length_inches", 72)
                .put("width_inches", 25));

        JSONObject shape = DrawingRules.sanitizeServerDrawing(drawing)
                .getJSONArray("shapes")
                .getJSONObject(0);
        JSONObject part = DrawingRules.sanitizeCalculationParts(parts).getJSONObject(0);

        assertEquals(shape.getString("link_id"), part.getString("link_id"));
        assertFalse(shape.has("label"));
        assertFalse(part.has("label"));
    }

    @Test
    public void explicitLinkIdWinsOverDifferentIdsAndLabels() throws Exception {
        JSONObject drawing = drawingWithShape(new JSONObject()
                .put("id", "shape-a")
                .put("label", "Left piece")
                .put("link_id", "server-link-7"));
        JSONArray parts = new JSONArray().put(new JSONObject()
                .put("id", "formula-z")
                .put("label", "Different wording")
                .put("link_id", "server-link-7")
                .put("operation", "add")
                .put("length_inches", 72)
                .put("width_inches", 25));

        JSONObject shape = DrawingRules.sanitizeServerDrawing(drawing)
                .getJSONArray("shapes")
                .getJSONObject(0);
        JSONObject part = DrawingRules.sanitizeCalculationParts(parts).getJSONObject(0);

        assertEquals("server-link-7", shape.getString("link_id"));
        assertEquals("server-link-7", part.getString("link_id"));
        assertEquals(0, DrawingRules.uniqueLinkedIndex(shape, new JSONArray().put(part)));
    }

    @Test
    public void mismatchedLinkIsNotGuessed() throws Exception {
        JSONObject shape = new JSONObject().put("link_id", "shape-link");
        JSONArray parts = new JSONArray().put(new JSONObject().put("link_id", "part-link"));

        assertEquals(DrawingRules.LINK_NOT_FOUND, DrawingRules.uniqueLinkedIndex(shape, parts));
    }

    @Test
    public void duplicateLinkIsRejectedAsAmbiguous() throws Exception {
        JSONObject shape = new JSONObject().put("link_id", "duplicate");
        JSONArray parts = new JSONArray()
                .put(new JSONObject().put("link_id", "duplicate"))
                .put(new JSONObject().put("link_id", "duplicate"));

        assertEquals(DrawingRules.LINK_AMBIGUOUS, DrawingRules.uniqueLinkedIndex(shape, parts));
    }

    private static JSONObject drawingWithShape(JSONObject shape) throws Exception {
        return new JSONObject().put("shapes", new JSONArray().put(shape));
    }
}
