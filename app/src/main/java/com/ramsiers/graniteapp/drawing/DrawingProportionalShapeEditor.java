package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/** Keeps linked redraw geometry proportional when a user corrects a dimension. */
public final class DrawingProportionalShapeEditor {
    private DrawingProportionalShapeEditor() {
    }

    public static int scaleLinkedGeometry(
            JSONObject drawing,
            JSONArray parts,
            JSONObject changedDimension,
            double oldValue,
            double newValue) {
        if (drawing == null
                || changedDimension == null
                || !positive(oldValue)
                || !positive(newValue)) return 0;
        double ratio = newValue / oldValue;
        if (!Double.isFinite(ratio) || ratio <= 0 || Math.abs(ratio - 1) < 0.0001) return 0;

        double x1 = changedDimension.optDouble("x1", Double.NaN);
        double y1 = changedDimension.optDouble("y1", Double.NaN);
        double x2 = changedDimension.optDouble("x2", Double.NaN);
        double y2 = changedDimension.optDouble("y2", Double.NaN);
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lineLength = Math.sqrt(dx * dx + dy * dy);
        if (!Double.isFinite(lineLength) || lineLength < 1) return 0;
        double axisX = dx / lineLength;
        double axisY = dy / lineLength;

        Set<String> identifiers = linkedIdentifiers(changedDimension, parts);
        if (identifiers.isEmpty()) return 0;
        JSONArray shapes = drawing.optJSONArray("shapes");
        if (shapes == null) return 0;

        Set<Integer> matchedShapeIndexes = new HashSet<>();
        double centerX = 0;
        double centerY = 0;
        int pointCount = 0;
        for (int i = 0; i < Math.min(24, shapes.length()); i++) {
            JSONObject shape = shapes.optJSONObject(i);
            if (!matchesShape(shape, identifiers)) continue;
            JSONArray points = shape.optJSONArray("points");
            if (points == null || points.length() < 3) continue;
            matchedShapeIndexes.add(i);
            for (int p = 0; p < Math.min(32, points.length()); p++) {
                JSONObject point = points.optJSONObject(p);
                if (point == null) continue;
                double x = point.optDouble("x", Double.NaN);
                double y = point.optDouble("y", Double.NaN);
                if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
                centerX += x;
                centerY += y;
                pointCount++;
            }
        }
        if (matchedShapeIndexes.isEmpty() || pointCount == 0) return 0;
        centerX /= pointCount;
        centerY /= pointCount;

        String role = changedDimension.optString("role", "other");
        boolean uniform = "both".equals(role);
        for (int shapeIndex : matchedShapeIndexes) {
            JSONObject shape = shapes.optJSONObject(shapeIndex);
            transformPoints(shape.optJSONArray("points"), centerX, centerY, axisX, axisY, ratio, uniform);
        }

        JSONArray dimensions = drawing.optJSONArray("dimensions");
        if (dimensions != null) {
            for (int i = 0; i < Math.min(50, dimensions.length()); i++) {
                JSONObject dimension = dimensions.optJSONObject(i);
                if (!matchesDimension(dimension, identifiers)) continue;
                transformDimension(dimension, centerX, centerY, axisX, axisY, ratio, uniform);
            }
        }
        return matchedShapeIndexes.size();
    }

    /**
     * Scales each linked calculation target at most once during a multi-field save. Duplicate
     * arrows may present the same value more than once, but their shared geometry must not be
     * multiplied once per field.
     */
    public static int scaleLinkedGeometryOnce(
            JSONObject drawing,
            JSONArray parts,
            JSONObject changedDimension,
            double oldValue,
            double newValue,
            Set<String> scaledTargets) {
        if (changedDimension == null
                || scaledTargets == null
                || !positive(oldValue)
                || !positive(newValue)) return 0;
        JSONArray partIds = changedDimension.optJSONArray("part_ids");
        if (partIds == null || partIds.length() == 0) return 0;

        String role = changedDimension.optString("role", "other");
        JSONArray unscaledPartIds = new JSONArray();
        Set<String> pendingTargets = new HashSet<>();
        for (int i = 0; i < Math.min(12, partIds.length()); i++) {
            String partId = partIds.optString(i, "").trim();
            if (partId.isEmpty()) continue;
            boolean hasUnscaledTarget = false;
            if ("length".equals(role) || "both".equals(role)) {
                String target = targetKey(partId, "length");
                if (!scaledTargets.contains(target)) {
                    pendingTargets.add(target);
                    hasUnscaledTarget = true;
                }
            }
            if ("width".equals(role) || "both".equals(role)) {
                String target = targetKey(partId, "width");
                if (!scaledTargets.contains(target)) {
                    pendingTargets.add(target);
                    hasUnscaledTarget = true;
                }
            }
            if (hasUnscaledTarget) unscaledPartIds.put(partId);
        }
        if (unscaledPartIds.length() == 0) return 0;

        JSONObject unscaledDimension = new JSONObject();
        try {
            unscaledDimension.put("role", role);
            unscaledDimension.put("part_ids", unscaledPartIds);
            unscaledDimension.put("x1", changedDimension.optDouble("x1", Double.NaN));
            unscaledDimension.put("y1", changedDimension.optDouble("y1", Double.NaN));
            unscaledDimension.put("x2", changedDimension.optDouble("x2", Double.NaN));
            unscaledDimension.put("y2", changedDimension.optDouble("y2", Double.NaN));
        } catch (Exception ignored) {
            return 0;
        }
        int scaledShapes = scaleLinkedGeometry(
                drawing,
                parts,
                unscaledDimension,
                oldValue,
                newValue);
        if (scaledShapes > 0) scaledTargets.addAll(pendingTargets);
        return scaledShapes;
    }

    private static String targetKey(String partId, String target) {
        return partId + "\u0000" + target;
    }

    private static Set<String> linkedIdentifiers(JSONObject dimension, JSONArray parts) {
        Set<String> identifiers = new HashSet<>();
        JSONArray partIds = dimension.optJSONArray("part_ids");
        if (partIds != null) {
            for (int i = 0; i < Math.min(12, partIds.length()); i++) {
                addIdentifier(identifiers, partIds.optString(i, ""));
            }
        }
        if (parts == null) return identifiers;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < Math.min(40, parts.length()); i++) {
                JSONObject part = parts.optJSONObject(i);
                if (part == null) continue;
                String id = part.optString("id", "").trim();
                String linkId = part.optString("link_id", "").trim();
                if (identifiers.contains(id) || identifiers.contains(linkId)) {
                    addIdentifier(identifiers, id);
                    addIdentifier(identifiers, linkId);
                }
            }
        }
        return identifiers;
    }

    private static void addIdentifier(Set<String> identifiers, String value) {
        String cleaned = value == null ? "" : value.trim();
        if (!cleaned.isEmpty()) identifiers.add(cleaned);
    }

    private static boolean matchesShape(JSONObject shape, Set<String> identifiers) {
        if (shape == null) return false;
        return identifiers.contains(shape.optString("link_id", "").trim())
                || identifiers.contains(shape.optString("id", "").trim());
    }

    private static boolean matchesDimension(JSONObject dimension, Set<String> identifiers) {
        if (dimension == null) return false;
        JSONArray partIds = dimension.optJSONArray("part_ids");
        if (partIds == null) return false;
        for (int i = 0; i < Math.min(12, partIds.length()); i++) {
            if (identifiers.contains(partIds.optString(i, "").trim())) return true;
        }
        return false;
    }

    private static void transformPoints(
            JSONArray points,
            double centerX,
            double centerY,
            double axisX,
            double axisY,
            double ratio,
            boolean uniform) {
        if (points == null) return;
        for (int i = 0; i < Math.min(32, points.length()); i++) {
            JSONObject point = points.optJSONObject(i);
            if (point == null) continue;
            transformPoint(point, "x", "y", centerX, centerY, axisX, axisY, ratio, uniform);
        }
    }

    private static void transformDimension(
            JSONObject dimension,
            double centerX,
            double centerY,
            double axisX,
            double axisY,
            double ratio,
            boolean uniform) {
        transformPoint(dimension, "x1", "y1", centerX, centerY, axisX, axisY, ratio, uniform);
        transformPoint(dimension, "x2", "y2", centerX, centerY, axisX, axisY, ratio, uniform);
    }

    private static void transformPoint(
            JSONObject object,
            String xKey,
            String yKey,
            double centerX,
            double centerY,
            double axisX,
            double axisY,
            double ratio,
            boolean uniform) {
        double x = object.optDouble(xKey, Double.NaN);
        double y = object.optDouble(yKey, Double.NaN);
        if (!Double.isFinite(x) || !Double.isFinite(y)) return;
        double relativeX = x - centerX;
        double relativeY = y - centerY;
        double scaledX;
        double scaledY;
        if (uniform) {
            scaledX = relativeX * ratio;
            scaledY = relativeY * ratio;
        } else {
            double parallel = relativeX * axisX + relativeY * axisY;
            double perpendicular = -relativeX * axisY + relativeY * axisX;
            scaledX = axisX * parallel * ratio - axisY * perpendicular;
            scaledY = axisY * parallel * ratio + axisX * perpendicular;
        }
        try {
            object.put(xKey, centerX + scaledX);
            object.put(yKey, centerY + scaledY);
        } catch (Exception ignored) {
        }
    }

    private static boolean positive(double value) {
        return Double.isFinite(value) && value > 0;
    }
}
