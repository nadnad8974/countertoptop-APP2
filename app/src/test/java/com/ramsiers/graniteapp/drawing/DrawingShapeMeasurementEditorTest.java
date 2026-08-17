package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class DrawingShapeMeasurementEditorTest {
    @Test
    public void addsMeasurementsToExistingUndimensionedRectangle() throws Exception {
        JSONObject drawing = drawingWithRectangle();
        JSONArray parts = new JSONArray();

        assertTrue(DrawingShapeMeasurementEditor.measureExisting(
                drawing,
                parts,
                0,
                100,
                25.5,
                DrawingRules.FEATURE_COUNTERTOP,
                "add"));

        assertEquals(1, parts.length());
        assertEquals(100, parts.getJSONObject(0).getDouble("length_inches"), 0.001);
        assertEquals(25.5, parts.getJSONObject(0).getDouble("width_inches"), 0.001);
        assertEquals(17.71, DrawingMath.squareFeet(parts), 0.001);
        assertEquals(2, drawing.getJSONArray("dimensions").length());
        assertEquals(
                parts.getJSONObject(0).getString("link_id"),
                drawing.getJSONArray("shapes").getJSONObject(0).getString("link_id"));
    }

    @Test
    public void updatesSelectedRectangleWithoutDuplicatingIt() throws Exception {
        JSONObject drawing = drawingWithRectangle();
        JSONArray parts = new JSONArray();
        DrawingShapeMeasurementEditor.measureExisting(
                drawing,
                parts,
                0,
                100,
                25.5,
                DrawingRules.FEATURE_COUNTERTOP,
                "add");

        assertTrue(DrawingShapeMeasurementEditor.measureExisting(
                drawing,
                parts,
                0,
                102,
                25.5,
                DrawingRules.FEATURE_COUNTERTOP,
                "add"));

        assertEquals(1, parts.length());
        assertEquals(1, drawing.getJSONArray("shapes").length());
        assertEquals(2, drawing.getJSONArray("dimensions").length());
        assertEquals(102, parts.getJSONObject(0).getDouble("length_inches"), 0.001);
    }

    private static JSONObject drawingWithRectangle() throws Exception {
        JSONObject shape = new JSONObject()
                .put("id", "right_piece")
                .put("kind", "countertop")
                .put("points", new JSONArray()
                        .put(point(500, 100))
                        .put(point(760, 100))
                        .put(point(760, 300))
                        .put(point(500, 300)));
        return new JSONObject()
                .put("canvas_width", 1000)
                .put("canvas_height", 700)
                .put("shapes", new JSONArray().put(shape))
                .put("dimensions", new JSONArray());
    }

    private static JSONObject point(double x, double y) throws Exception {
        return new JSONObject().put("x", x).put("y", y);
    }
}
