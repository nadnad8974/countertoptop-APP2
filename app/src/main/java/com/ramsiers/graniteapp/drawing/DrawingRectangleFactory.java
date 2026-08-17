package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Adds a user-drawn rectangle and its two editable dimensions to a verification drawing. */
public final class DrawingRectangleFactory {
    private static final int MAX_SHAPES = 24;
    private static final int MAX_DIMENSIONS = 50;

    private DrawingRectangleFactory() {
    }

    public static boolean append(
            JSONObject drawing,
            String linkId,
            String feature,
            double x1,
            double y1,
            double x2,
            double y2,
            double lengthInches,
            double widthInches) {
        if (drawing == null
                || linkId == null
                || linkId.trim().isEmpty()
                || !positive(lengthInches)
                || !positive(widthInches)) return false;

        double canvasWidth = positiveOr(drawing.optDouble("canvas_width", 1000), 1000);
        double canvasHeight = positiveOr(drawing.optDouble("canvas_height", 700), 700);
        double left = clamp(Math.min(x1, x2), 0, canvasWidth);
        double top = clamp(Math.min(y1, y2), 0, canvasHeight);
        double right = clamp(Math.max(x1, x2), 0, canvasWidth);
        double bottom = clamp(Math.max(y1, y2), 0, canvasHeight);
        if (right - left < 5 || bottom - top < 5) return false;

        try {
            JSONArray shapes = drawing.optJSONArray("shapes");
            if (shapes == null) {
                shapes = new JSONArray();
                drawing.put("shapes", shapes);
            }
            JSONArray dimensions = drawing.optJSONArray("dimensions");
            if (dimensions == null) {
                dimensions = new JSONArray();
                drawing.put("dimensions", dimensions);
            }
            if (shapes.length() >= MAX_SHAPES || dimensions.length() > MAX_DIMENSIONS - 2) {
                return false;
            }

            String safeFeature = canonicalFeature(feature);
            String kind = DrawingRules.FEATURE_BACKSPLASH.equals(safeFeature)
                    ? DrawingRules.FEATURE_BACKSPLASH
                    : DrawingRules.FEATURE_COUNTERTOP.equals(safeFeature)
                    ? DrawingRules.FEATURE_COUNTERTOP
                    : "opening";
            JSONObject shape = new JSONObject()
                    .put("id", "user_shape_" + linkId)
                    .put("link_id", linkId)
                    .put("feature_type", safeFeature)
                    .put("kind", kind)
                    .put("points", rectanglePoints(left, top, right, bottom));
            if ("opening".equals(kind)) shape.put("opening_type", safeFeature);
            shapes.put(shape);

            dimensions.put(dimension(
                    linkId,
                    "length",
                    lengthInches,
                    left,
                    Math.max(12, top - 28),
                    right,
                    Math.max(12, top - 28)));
            dimensions.put(dimension(
                    linkId,
                    "width",
                    widthInches,
                    Math.max(12, left - 28),
                    top,
                    Math.max(12, left - 28),
                    bottom));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static JSONObject dimension(
            String linkId,
            String role,
            double value,
            double x1,
            double y1,
            double x2,
            double y2) throws Exception {
        return new JSONObject()
                .put("role", role)
                .put("value_inches", value)
                .put("label", formatInches(value))
                .put("part_ids", new JSONArray().put(linkId))
                .put("x1", x1)
                .put("y1", y1)
                .put("x2", x2)
                .put("y2", y2);
    }

    private static JSONArray rectanglePoints(
            double left,
            double top,
            double right,
            double bottom) throws Exception {
        return new JSONArray()
                .put(point(left, top))
                .put(point(right, top))
                .put(point(right, bottom))
                .put(point(left, bottom));
    }

    private static JSONObject point(double x, double y) throws Exception {
        return new JSONObject().put("x", x).put("y", y);
    }

    private static String canonicalFeature(String feature) {
        if (DrawingRules.FEATURE_BACKSPLASH.equals(feature)) {
            return DrawingRules.FEATURE_BACKSPLASH;
        }
        if (DrawingRules.FEATURE_SINK.equals(feature)) return DrawingRules.FEATURE_SINK;
        if (DrawingRules.FEATURE_COOKTOP.equals(feature)) return DrawingRules.FEATURE_COOKTOP;
        if (DrawingRules.FEATURE_STOVE.equals(feature)) return DrawingRules.FEATURE_STOVE;
        if (DrawingRules.FEATURE_OTHER.equals(feature)) return DrawingRules.FEATURE_OTHER;
        return DrawingRules.FEATURE_COUNTERTOP;
    }

    private static String formatInches(double value) {
        String formatted = Math.abs(value - Math.rint(value)) < 0.005
                ? String.valueOf((long) Math.rint(value))
                : String.format(Locale.US, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
        return formatted + "\"";
    }

    private static boolean positive(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0;
    }

    private static double positiveOr(double value, double fallback) {
        return positive(value) ? value : fallback;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
