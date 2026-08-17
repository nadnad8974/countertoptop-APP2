package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class DrawingMeasurementGuardTest {
    @Test
    public void rejectsNearbyExplicitDimensionThatContradictsTenByFourteenFormula()
            throws Exception {
        JSONArray parts = new JSONArray().put(part("P4", 10, 14));
        JSONObject drawing = drawing(
                new JSONArray().put(shape("P4", 100, 100, 200, 300)),
                new JSONArray()
                        .put(dimension("P4", "length", 10, 100, 70, 200, 70))
                        .put(dimension("P4", "width", 14, 70, 100, 70, 300))
                        .put(dimension(
                                "unknown_piece",
                                "length",
                                25.5,
                                225,
                                100,
                                225,
                                300)));

        DrawingMeasurementGuard.Result result = DrawingMeasurementGuard.inspect(
                drawing,
                parts);

        assertFalse(result.canPrice);
        assertTrue(result.question.contains("25.5\""));
        assertTrue(result.question.contains("10 × 14"));
    }

    @Test
    public void rejectsDimensionLinkedToFormulaWithDifferentValue() throws Exception {
        JSONArray parts = new JSONArray().put(part("island", 102, 45));
        JSONObject drawing = drawing(
                new JSONArray().put(shape("island", 200, 200, 700, 500)),
                new JSONArray()
                        .put(dimension("island", "length", 102, 200, 170, 700, 170))
                        .put(dimension("island", "width", 14, 730, 200, 730, 500)));

        DrawingMeasurementGuard.Result result = DrawingMeasurementGuard.inspect(
                drawing,
                parts);

        assertFalse(result.canPrice);
        assertTrue(result.question.contains("14\""));
        assertTrue(result.question.contains("45\""));
    }

    @Test
    public void acceptsStrictLinkedResultWithOnlyOneDisplayedDimension() throws Exception {
        JSONArray parts = new JSONArray().put(part("wall_run", 100, 25.5)
                .put("confidence", "high")
                .put("measurement_source", "explicit"));
        JSONObject drawing = drawing(
                new JSONArray().put(shape("wall_run", 80, 80, 620, 220)),
                new JSONArray().put(dimension(
                        "wall_run",
                        "length",
                        100,
                        80,
                        55,
                        620,
                        55)));

        DrawingMeasurementGuard.Result result = DrawingMeasurementGuard.inspect(
                drawing,
                parts);

        assertTrue(result.question, result.canPrice);
    }

    @Test
    public void rejectsFormulaThatCannotBeMappedToAnOutline() throws Exception {
        JSONArray parts = new JSONArray().put(part("formula_only", 60, 25.5));
        JSONObject drawing = drawing(
                new JSONArray().put(shape("different_piece", 80, 80, 620, 220)),
                new JSONArray());

        DrawingMeasurementGuard.Result result = DrawingMeasurementGuard.inspect(
                drawing,
                parts);

        assertFalse(result.canPrice);
        assertTrue(result.question.contains("not linked"));
    }

    @Test
    public void acceptsGeneratedMeasuredFallback() throws Exception {
        JSONArray parts = new JSONArray().put(part("counter", 120, 30));
        JSONObject drawing = DrawingFallback.fromCalculationParts(parts);

        DrawingMeasurementGuard.Result result = DrawingMeasurementGuard.inspect(
                drawing,
                parts);

        assertTrue(result.question, result.canPrice);
    }

    @Test
    public void acceptsLegacyCompositeShapeWhenItsPiecesMapUnambiguously()
            throws Exception {
        JSONArray parts = new JSONArray()
                .put(unlinkedPart("P0", "countertop", "add", 100, 25.5))
                .put(unlinkedPart("P1", "countertop", "add", 45, 25.5))
                .put(unlinkedPart("P2", "other", "subtract", 25.5, 25.5));
        JSONObject composite = new JSONObject()
                .put("id", "S0")
                .put("link_id", "S0")
                .put("feature_type", "countertop")
                .put("kind", "countertop")
                .put("points", new JSONArray()
                        .put(point(95, 120))
                        .put(point(535, 105))
                        .put(point(540, 245))
                        .put(point(235, 270))
                        .put(point(235, 450))
                        .put(point(95, 465)));
        JSONObject drawing = drawing(
                new JSONArray().put(composite),
                new JSONArray()
                        .put(dimension("P0", "length", 100, 100, 80, 535, 65))
                        .put(dimension("P1", "length", 45, 60, 120, 60, 465))
                        .put(new JSONObject()
                                .put("part_ids", new JSONArray()
                                        .put("P0")
                                        .put("P1")
                                        .put("P2"))
                                .put("role", "both")
                                .put("value_inches", 25.5)
                                .put("x1", 95)
                                .put("y1", 500)
                                .put("x2", 235)
                                .put("y2", 490)));

        DrawingMeasurementGuard.Result result = DrawingMeasurementGuard.inspect(
                drawing,
                parts);

        assertTrue(result.question, result.canPrice);
    }

    private static JSONObject part(String id, double length, double width) throws Exception {
        return new JSONObject()
                .put("id", id)
                .put("link_id", id)
                .put("feature_type", "countertop")
                .put("operation", "add")
                .put("length_inches", length)
                .put("width_inches", width)
                .put("quantity", 1);
    }

    private static JSONObject unlinkedPart(
            String id,
            String feature,
            String operation,
            double length,
            double width) throws Exception {
        JSONObject result = part(id, length, width)
                .put("feature_type", feature)
                .put("operation", operation);
        result.remove("link_id");
        return result;
    }

    private static JSONObject drawing(JSONArray shapes, JSONArray dimensions) throws Exception {
        return new JSONObject()
                .put("canvas_width", 1000)
                .put("canvas_height", 700)
                .put("shapes", shapes)
                .put("dimensions", dimensions);
    }

    private static JSONObject shape(
            String id,
            double left,
            double top,
            double right,
            double bottom) throws Exception {
        return new JSONObject()
                .put("id", id)
                .put("link_id", id)
                .put("feature_type", "countertop")
                .put("kind", "countertop")
                .put("points", new JSONArray()
                        .put(point(left, top))
                        .put(point(right, top))
                        .put(point(right, bottom))
                        .put(point(left, bottom)));
    }

    private static JSONObject dimension(
            String partId,
            String role,
            double value,
            double x1,
            double y1,
            double x2,
            double y2) throws Exception {
        return new JSONObject()
                .put("part_ids", new JSONArray().put(partId))
                .put("role", role)
                .put("value_inches", value)
                .put("x1", x1)
                .put("y1", y1)
                .put("x2", x2)
                .put("y2", y2);
    }

    private static JSONObject point(double x, double y) throws Exception {
        return new JSONObject().put("x", x).put("y", y);
    }
}
