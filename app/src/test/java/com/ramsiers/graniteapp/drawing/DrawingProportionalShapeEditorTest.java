package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashSet;

public class DrawingProportionalShapeEditorTest {
    @Test
    public void correctedLengthAndWidthResizeLinkedShapeProportionally() throws Exception {
        JSONObject length = dimension("length", 100, 100, 70, 300, 70);
        JSONObject width = dimension("width", 50, 70, 100, 70, 200);
        JSONObject shape = new JSONObject()
                .put("id", "piece_1")
                .put("link_id", "piece_1")
                .put("points", new JSONArray()
                        .put(point(100, 100))
                        .put(point(300, 100))
                        .put(point(300, 200))
                        .put(point(100, 200)));
        JSONObject drawing = new JSONObject()
                .put("shapes", new JSONArray().put(shape))
                .put("dimensions", new JSONArray().put(length).put(width));
        JSONArray parts = new JSONArray().put(new JSONObject()
                .put("id", "piece_1")
                .put("link_id", "piece_1"));

        assertEquals(1, DrawingProportionalShapeEditor.scaleLinkedGeometry(
                drawing, parts, length, 100, 150));
        assertEquals(50, shape.getJSONArray("points").getJSONObject(0).getDouble("x"), 0.001);
        assertEquals(350, shape.getJSONArray("points").getJSONObject(1).getDouble("x"), 0.001);

        assertEquals(1, DrawingProportionalShapeEditor.scaleLinkedGeometry(
                drawing, parts, width, 50, 100));
        assertEquals(50, shape.getJSONArray("points").getJSONObject(0).getDouble("y"), 0.001);
        assertEquals(250, shape.getJSONArray("points").getJSONObject(2).getDouble("y"), 0.001);
        assertEquals(300, shapeWidth(shape), 0.001);
        assertEquals(200, shapeHeight(shape), 0.001);
        assertEquals(1.5, shapeWidth(shape) / shapeHeight(shape), 0.001);
    }

    @Test
    public void unrelatedShapeIsNotResized() throws Exception {
        JSONObject dimension = dimension("length", 100, 0, 0, 100, 0);
        JSONObject otherShape = new JSONObject()
                .put("id", "other")
                .put("link_id", "other")
                .put("points", new JSONArray()
                        .put(point(10, 10))
                        .put(point(20, 10))
                        .put(point(20, 20))
                        .put(point(10, 20)));
        JSONObject drawing = new JSONObject()
                .put("shapes", new JSONArray().put(otherShape))
                .put("dimensions", new JSONArray().put(dimension));

        assertEquals(0, DrawingProportionalShapeEditor.scaleLinkedGeometry(
                drawing, new JSONArray(), dimension, 100, 200));
        assertEquals(10, otherShape.getJSONArray("points").getJSONObject(0).getDouble("x"), 0.001);
    }

    @Test
    public void duplicateArrowsScaleSharedTargetOnlyOnceDuringSave() throws Exception {
        JSONObject firstArrow = dimension("length", 100, 100, 70, 300, 70);
        JSONObject duplicateArrow = dimension("length", 100, 100, 230, 300, 230);
        JSONObject shape = new JSONObject()
                .put("id", "piece_1")
                .put("link_id", "piece_1")
                .put("points", new JSONArray()
                        .put(point(100, 100))
                        .put(point(300, 100))
                        .put(point(300, 200))
                        .put(point(100, 200)));
        JSONObject drawing = new JSONObject()
                .put("shapes", new JSONArray().put(shape))
                .put("dimensions", new JSONArray().put(firstArrow).put(duplicateArrow));
        JSONArray parts = new JSONArray().put(new JSONObject()
                .put("id", "piece_1")
                .put("link_id", "piece_1"));
        HashSet<String> scaledTargets = new HashSet<>();

        assertEquals(1, DrawingProportionalShapeEditor.scaleLinkedGeometryOnce(
                drawing, parts, firstArrow, 100, 150, scaledTargets));
        assertEquals(0, DrawingProportionalShapeEditor.scaleLinkedGeometryOnce(
                drawing, parts, duplicateArrow, 100, 150, scaledTargets));

        assertEquals(300, shapeWidth(shape), 0.001);
        assertEquals(50, shape.getJSONArray("points").getJSONObject(0).getDouble("x"), 0.001);
        assertEquals(350, shape.getJSONArray("points").getJSONObject(1).getDouble("x"), 0.001);
    }

    private static JSONObject dimension(
            String role,
            double value,
            double x1,
            double y1,
            double x2,
            double y2) throws Exception {
        return new JSONObject()
                .put("role", role)
                .put("value_inches", value)
                .put("part_ids", new JSONArray().put("piece_1"))
                .put("x1", x1)
                .put("y1", y1)
                .put("x2", x2)
                .put("y2", y2);
    }

    private static JSONObject point(double x, double y) throws Exception {
        return new JSONObject().put("x", x).put("y", y);
    }

    private static double shapeWidth(JSONObject shape) {
        JSONArray points = shape.optJSONArray("points");
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < points.length(); i++) {
            double value = points.optJSONObject(i).optDouble("x");
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        return maximum - minimum;
    }

    private static double shapeHeight(JSONObject shape) {
        JSONArray points = shape.optJSONArray("points");
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < points.length(); i++) {
            double value = points.optJSONObject(i).optDouble("y");
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        return maximum - minimum;
    }
}
