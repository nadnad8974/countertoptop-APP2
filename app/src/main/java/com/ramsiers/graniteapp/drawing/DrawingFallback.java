package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds an editable verification guide when the service returns measurements but no outline. */
public final class DrawingFallback {
    private static final double CANVAS_WIDTH = 1000;
    private static final double CANVAS_HEIGHT = 700;
    private static final int MAX_SHAPES = 24;

    private DrawingFallback() {
    }

    /** Starts a safe empty canvas when the service can describe a photo but cannot redraw it. */
    public static JSONObject blankEditable() {
        try {
            return new JSONObject()
                    .put("canvas_width", CANVAS_WIDTH)
                    .put("canvas_height", CANVAS_HEIGHT)
                    .put("units", "inches")
                    .put("partial_user_edit", true)
                    .put("shapes", new JSONArray())
                    .put("dimensions", new JSONArray());
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean hasDrawableShape(JSONObject drawing) {
        if (drawing == null) return false;
        JSONArray shapes = drawing.optJSONArray("shapes");
        if (shapes == null) return false;
        for (int i = 0; i < Math.min(shapes.length(), MAX_SHAPES); i++) {
            JSONObject shape = shapes.optJSONObject(i);
            JSONArray points = shape == null ? null : shape.optJSONArray("points");
            if (points != null && points.length() >= 3) return true;
        }
        return false;
    }

    public static JSONObject fromCalculationParts(JSONArray parts) {
        if (parts == null || parts.length() == 0 || DrawingMath.squareFeet(parts) <= 0) {
            return null;
        }

        try {
            List<PartGeometry> pieces = new ArrayList<>();
            List<PartGeometry> openings = new ArrayList<>();
            for (int i = 0; i < Math.min(parts.length(), 40); i++) {
                JSONObject part = parts.optJSONObject(i);
                if (part == null) continue;
                double length = positive(part.optDouble("length_inches", 0));
                double width = positive(part.optDouble("width_inches", 0));
                if (length <= 0 || width <= 0) continue;

                String id = part.optString("id", "").trim();
                if (id.isEmpty()) id = part.optString("link_id", "").trim();
                if (id.isEmpty()) id = "measured_part_" + (i + 1);
                part.put("id", id);
                part.put("link_id", id);

                PartGeometry geometry = new PartGeometry(part, id, length, width);
                String feature = DrawingRules.featureType(part);
                boolean opening = DrawingRules.FEATURE_SINK.equals(feature)
                        || DrawingRules.FEATURE_COOKTOP.equals(feature)
                        || DrawingRules.FEATURE_STOVE.equals(feature)
                        || "subtract".equals(part.optString("operation", ""));
                if (!opening && DrawingRules.FEATURE_OTHER.equals(feature)) {
                    part.put("feature_type", DrawingRules.FEATURE_COUNTERTOP);
                }
                if (opening) openings.add(geometry);
                else pieces.add(geometry);
            }
            if (pieces.isEmpty()) return null;

            JSONArray shapes = new JSONArray();
            JSONArray dimensions = new JSONArray();
            layoutPieces(pieces, shapes, dimensions);
            layoutOpenings(openings, pieces.get(0), shapes, dimensions);
            if (shapes.length() == 0) return null;

            return new JSONObject()
                    .put("canvas_width", CANVAS_WIDTH)
                    .put("canvas_height", CANVAS_HEIGHT)
                    .put("units", "inches")
                    .put("fallback_generated", true)
                    .put("shapes", shapes)
                    .put("dimensions", dimensions);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void layoutPieces(
            List<PartGeometry> pieces,
            JSONArray shapes,
            JSONArray dimensions) throws Exception {
        int count = Math.min(pieces.size(), 12);
        int columns = count <= 2 ? count : count <= 4 ? 2 : 3;
        int rows = (int) Math.ceil((double) count / columns);
        double cellWidth = 860.0 / columns;
        double cellHeight = 540.0 / Math.max(1, rows);

        for (int i = 0; i < count && shapes.length() < MAX_SHAPES; i++) {
            PartGeometry part = pieces.get(i);
            int column = i % columns;
            int row = i / columns;
            double maximumWidth = Math.max(90, cellWidth - 100);
            double maximumHeight = Math.max(70, cellHeight - 100);
            double scale = Math.min(maximumWidth / part.length, maximumHeight / part.width);
            double drawnWidth = Math.max(70, part.length * scale);
            double drawnHeight = Math.max(45, part.width * scale);
            double centerX = 70 + column * cellWidth + cellWidth / 2;
            double centerY = 75 + row * cellHeight + cellHeight / 2;
            part.left = centerX - drawnWidth / 2;
            part.top = centerY - drawnHeight / 2;
            part.right = centerX + drawnWidth / 2;
            part.bottom = centerY + drawnHeight / 2;

            shapes.put(shape(part, part.left, part.top, part.right, part.bottom));
            addDimensions(dimensions, part, part.left, part.top, part.right, part.bottom);
        }
    }

    private static void layoutOpenings(
            List<PartGeometry> openings,
            PartGeometry host,
            JSONArray shapes,
            JSONArray dimensions) throws Exception {
        if (host.right <= host.left || host.bottom <= host.top) return;
        int count = Math.min(openings.size(), Math.max(0, MAX_SHAPES - shapes.length()));
        double hostWidth = host.right - host.left;
        double hostHeight = host.bottom - host.top;
        for (int i = 0; i < count; i++) {
            PartGeometry opening = openings.get(i);
            double scale = Math.min(
                    hostWidth * 0.28 / opening.length,
                    hostHeight * 0.48 / opening.width);
            double drawnWidth = Math.max(28, Math.min(hostWidth * 0.32, opening.length * scale));
            double drawnHeight = Math.max(24, Math.min(hostHeight * 0.55, opening.width * scale));
            double centerX = host.left + hostWidth * (i + 1) / (count + 1.0);
            double centerY = host.top + hostHeight / 2;
            double left = centerX - drawnWidth / 2;
            double top = centerY - drawnHeight / 2;
            double right = centerX + drawnWidth / 2;
            double bottom = centerY + drawnHeight / 2;
            shapes.put(shape(opening, left, top, right, bottom));
            addDimensions(dimensions, opening, left, top, right, bottom);
        }
    }

    private static JSONObject shape(
            PartGeometry part,
            double left,
            double top,
            double right,
            double bottom) throws Exception {
        String feature = DrawingRules.featureType(part.source);
        String kind = DrawingRules.FEATURE_BACKSPLASH.equals(feature)
                ? DrawingRules.FEATURE_BACKSPLASH
                : DrawingRules.FEATURE_COUNTERTOP.equals(feature)
                ? DrawingRules.FEATURE_COUNTERTOP
                : "opening";
        JSONObject shape = new JSONObject()
                .put("id", "shape_" + part.id)
                .put("link_id", part.id)
                .put("feature_type", feature)
                .put("kind", kind)
                .put("points", rectanglePoints(left, top, right, bottom));
        if ("opening".equals(kind)) shape.put("opening_type", feature);
        return shape;
    }

    private static void addDimensions(
            JSONArray dimensions,
            PartGeometry part,
            double left,
            double top,
            double right,
            double bottom) throws Exception {
        JSONArray ids = new JSONArray().put(part.id);
        dimensions.put(new JSONObject()
                .put("role", "length")
                .put("value_inches", part.length)
                .put("label", formatInches(part.length))
                .put("part_ids", ids)
                .put("x1", left)
                .put("y1", Math.max(12, top - 26))
                .put("x2", right)
                .put("y2", Math.max(12, top - 26)));
        dimensions.put(new JSONObject()
                .put("role", "width")
                .put("value_inches", part.width)
                .put("label", formatInches(part.width))
                .put("part_ids", new JSONArray().put(part.id))
                .put("x1", Math.max(12, left - 26))
                .put("y1", top)
                .put("x2", Math.max(12, left - 26))
                .put("y2", bottom));
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

    private static String formatInches(double value) {
        String formatted = Math.abs(value - Math.rint(value)) < 0.005
                ? String.valueOf((long) Math.rint(value))
                : String.format(Locale.US, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
        return formatted + "\"";
    }

    private static double positive(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0 ? value : 0;
    }

    private static final class PartGeometry {
        final JSONObject source;
        final String id;
        final double length;
        final double width;
        double left;
        double top;
        double right;
        double bottom;

        PartGeometry(JSONObject source, String id, double length, double width) {
            this.source = source;
            this.id = id;
            this.length = length;
            this.width = width;
        }
    }
}
