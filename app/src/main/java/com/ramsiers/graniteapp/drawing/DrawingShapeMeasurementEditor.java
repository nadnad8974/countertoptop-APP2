package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Links exact user-entered measurements to an existing AI-drawn shape. */
public final class DrawingShapeMeasurementEditor {
    private DrawingShapeMeasurementEditor() {
    }

    public static Values existingValues(
            JSONObject drawing,
            JSONArray parts,
            int shapeIndex) {
        JSONObject shape = shapeAt(drawing, shapeIndex);
        if (shape == null) return new Values(0, 0);
        String linkId = shapeLinkId(shape, shapeIndex);
        double length = 0;
        double width = 0;
        JSONObject part = linkedPart(parts, linkId);
        if (part != null) {
            length = positive(part.optDouble("length_inches", 0));
            width = positive(part.optDouble("width_inches", 0));
        }
        JSONArray dimensions = drawing.optJSONArray("dimensions");
        if (dimensions != null) {
            for (int i = 0; i < Math.min(50, dimensions.length()); i++) {
                JSONObject dimension = dimensions.optJSONObject(i);
                if (!matchesDimension(shape, shapeIndex, dimension)) continue;
                double value = positive(dimension.optDouble("value_inches", 0));
                String role = dimension.optString("role", "");
                if ("length".equals(role) && length <= 0) length = value;
                if ("width".equals(role) && width <= 0) width = value;
            }
        }
        return new Values(length, width);
    }

    public static boolean matchesDimension(
            JSONObject shape,
            int shapeIndex,
            JSONObject dimension) {
        if (shape == null || dimension == null) return false;
        String linkId = shapeLinkId(shape, shapeIndex);
        JSONArray partIds = dimension.optJSONArray("part_ids");
        if (partIds == null) return false;
        for (int i = 0; i < Math.min(12, partIds.length()); i++) {
            if (linkId.equals(partIds.optString(i, "").trim())) return true;
        }
        return false;
    }

    public static boolean measureExisting(
            JSONObject drawing,
            JSONArray parts,
            int shapeIndex,
            double length,
            double width,
            String feature,
            String operation) {
        JSONObject shape = shapeAt(drawing, shapeIndex);
        if (shape == null
                || parts == null
                || positive(length) <= 0
                || positive(width) <= 0) return false;
        try {
            String linkId = shapeLinkId(shape, shapeIndex);
            shape.put("link_id", linkId);
            applyFeatureToShape(shape, feature);

            JSONObject part = linkedPart(parts, linkId);
            if (part == null) {
                if (parts.length() >= 40) return false;
                part = new JSONObject();
                parts.put(part);
            }
            if (part.optString("id", "").trim().isEmpty()) part.put("id", linkId);
            part.put("link_id", linkId);
            part.put("feature_type", feature);
            part.put("operation", operation);
            part.put("length_inches", length);
            part.put("width_inches", width);
            part.put("quantity", 1);
            part.put("user_added", true);

            JSONArray dimensions = drawing.optJSONArray("dimensions");
            if (dimensions == null) {
                dimensions = new JSONArray();
                drawing.put("dimensions", dimensions);
            }
            Bounds bounds = bounds(shape);
            upsertDimension(dimensions, shape, shapeIndex, linkId, "length", length, bounds);
            upsertDimension(dimensions, shape, shapeIndex, linkId, "width", width, bounds);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void upsertDimension(
            JSONArray dimensions,
            JSONObject shape,
            int shapeIndex,
            String linkId,
            String role,
            double value,
            Bounds bounds) throws Exception {
        JSONObject target = null;
        for (int i = 0; i < Math.min(50, dimensions.length()); i++) {
            JSONObject candidate = dimensions.optJSONObject(i);
            if (candidate != null
                    && role.equals(candidate.optString("role", ""))
                    && matchesDimension(shape, shapeIndex, candidate)) {
                target = candidate;
                break;
            }
        }
        if (target == null) {
            if (dimensions.length() >= 50) return;
            target = new JSONObject();
            dimensions.put(target);
        }
        target.put("role", role);
        target.put("value_inches", value);
        target.put("label", format(value));
        target.put("part_ids", new JSONArray().put(linkId));
        if ("length".equals(role)) {
            target.put("x1", bounds.left);
            target.put("y1", Math.max(12, bounds.top - 26));
            target.put("x2", bounds.right);
            target.put("y2", Math.max(12, bounds.top - 26));
        } else {
            target.put("x1", Math.max(12, bounds.left - 26));
            target.put("y1", bounds.top);
            target.put("x2", Math.max(12, bounds.left - 26));
            target.put("y2", bounds.bottom);
        }
    }

    private static JSONObject shapeAt(JSONObject drawing, int shapeIndex) {
        JSONArray shapes = drawing == null ? null : drawing.optJSONArray("shapes");
        return shapes == null || shapeIndex < 0 || shapeIndex >= shapes.length()
                ? null
                : shapes.optJSONObject(shapeIndex);
    }

    private static JSONObject linkedPart(JSONArray parts, String linkId) {
        if (parts == null || linkId.isEmpty()) return null;
        JSONObject match = null;
        for (int i = 0; i < Math.min(40, parts.length()); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part == null) continue;
            String candidate = part.optString("link_id", "").trim();
            if (candidate.isEmpty()) candidate = part.optString("id", "").trim();
            if (!linkId.equals(candidate)) continue;
            if (match != null) return null;
            match = part;
        }
        return match;
    }

    private static String shapeLinkId(JSONObject shape, int shapeIndex) {
        String linkId = shape.optString("link_id", "").trim();
        if (linkId.isEmpty()) linkId = shape.optString("id", "").trim();
        if (linkId.isEmpty()) linkId = "user_shape_" + (shapeIndex + 1);
        return linkId.length() <= 64 ? linkId : linkId.substring(0, 64);
    }

    private static void applyFeatureToShape(JSONObject shape, String feature) throws Exception {
        if (DrawingRules.FEATURE_BACKSPLASH.equals(feature)) {
            shape.put("kind", DrawingRules.FEATURE_BACKSPLASH);
            shape.put("feature_type", DrawingRules.FEATURE_BACKSPLASH);
            shape.remove("opening_type");
        } else if (DrawingRules.FEATURE_COUNTERTOP.equals(feature)) {
            shape.put("kind", DrawingRules.FEATURE_COUNTERTOP);
            shape.put("feature_type", DrawingRules.FEATURE_COUNTERTOP);
            shape.remove("opening_type");
        } else {
            shape.put("kind", "opening");
            shape.put("feature_type", feature);
            shape.put("opening_type", feature);
        }
    }

    private static Bounds bounds(JSONObject shape) {
        JSONArray points = shape.optJSONArray("points");
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        if (points != null) {
            for (int i = 0; i < Math.min(32, points.length()); i++) {
                JSONObject point = points.optJSONObject(i);
                if (point == null) continue;
                double x = point.optDouble("x", Double.NaN);
                double y = point.optDouble("y", Double.NaN);
                if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }
        if (!Double.isFinite(left) || right <= left || bottom <= top) {
            return new Bounds(100, 100, 300, 220);
        }
        return new Bounds(left, top, right, bottom);
    }

    private static double positive(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0;
    }

    private static String format(double value) {
        String result = Math.abs(value - Math.rint(value)) < 0.005
                ? String.valueOf((long) Math.rint(value))
                : String.format(Locale.US, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
        return result + "\"";
    }

    public static final class Values {
        public final double length;
        public final double width;

        Values(double length, double width) {
            this.length = length;
            this.width = width;
        }
    }

    private static final class Bounds {
        final double left;
        final double top;
        final double right;
        final double bottom;

        Bounds(double left, double top, double right, double bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
