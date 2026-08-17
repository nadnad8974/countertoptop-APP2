package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fails closed when an AI area formula cannot be reconciled with its editable redraw.
 *
 * <p>This check intentionally runs only when accepting an unedited AI result. User-saved edits
 * remain authoritative. It supports the strict linked response as well as older responses whose
 * physical pieces can be mapped unambiguously by {@link DrawingPieceSummary}.</p>
 */
public final class DrawingMeasurementGuard {
    private static final int MAX_PARTS = 40;
    private static final int MAX_SHAPES = 24;
    private static final int MAX_DIMENSIONS = 50;
    private static final int MAX_IDENTIFIERS = 12;
    private static final double VALUE_TOLERANCE_INCHES = 0.05;
    private static final double TOTAL_TOLERANCE_SQUARE_FEET = 0.02;

    private DrawingMeasurementGuard() {
    }

    public static Result inspect(JSONObject drawing, JSONArray calculationParts) {
        double calculatedSquareFeet = DrawingMath.squareFeet(calculationParts);
        if (!positive(calculatedSquareFeet)) {
            return incomplete(
                    "What are the length and width in inches for each countertop piece?");
        }
        if (drawing == null || !DrawingFallback.hasDrawableShape(drawing)) {
            return incomplete(
                    "What are the length and width in inches for each outlined countertop piece?");
        }

        List<DrawingPieceSummary.Piece> pieces = DrawingPieceSummary.summarize(
                drawing,
                calculationParts);
        if (pieces.isEmpty()) {
            return incomplete(
                    "What are the length and width in inches for each outlined countertop piece?");
        }

        JSONArray shapes = drawing.optJSONArray("shapes");
        Map<Integer, List<DrawingPieceSummary.Piece>> piecesByShape = new HashMap<>();
        double displayedTotal = 0;
        for (DrawingPieceSummary.Piece piece : pieces) {
            displayedTotal += piece.subtract ? -piece.squareFeet : piece.squareFeet;
            if (piece.shapeIndex < 0
                    || shapes == null
                    || piece.shapeIndex >= Math.min(shapes.length(), MAX_SHAPES)
                    || !drawable(shapes.optJSONObject(piece.shapeIndex))) {
                return incomplete(
                        "What are the length and width in inches for piece #"
                                + piece.number
                                + "? Its calculation is not linked to an outlined piece.");
            }
            JSONObject part = calculationParts.optJSONObject(piece.partIndex);
            JSONObject shape = shapes.optJSONObject(piece.shapeIndex);
            if (!compatible(part, shape)) {
                return incomplete(
                        "What are the length and width in inches for piece #"
                                + piece.number
                                + "? Its calculation is linked to a different kind of shape.");
            }
            List<DrawingPieceSummary.Piece> shapePieces = piecesByShape.get(piece.shapeIndex);
            if (shapePieces == null) {
                shapePieces = new ArrayList<>();
                piecesByShape.put(piece.shapeIndex, shapePieces);
            }
            shapePieces.add(piece);
        }
        if (Math.abs(displayedTotal - calculatedSquareFeet)
                > TOTAL_TOLERANCE_SQUARE_FEET) {
            return incomplete(
                    "Please verify every piece's length and width; the displayed pieces do not match the area formula.");
        }

        for (int shapeIndex = 0;
             shapes != null && shapeIndex < Math.min(shapes.length(), MAX_SHAPES);
             shapeIndex++) {
            JSONObject shape = shapes.optJSONObject(shapeIndex);
            if (drawable(shape)
                    && affectsArea(shape)
                    && !piecesByShape.containsKey(shapeIndex)) {
                return incomplete(
                        "What are the length and width in inches for the outlined piece without a calculation?");
            }
        }

        Map<String, Set<Integer>> directPartsByIdentifier = directPartIdentifiers(
                calculationParts);
        Map<String, Set<Integer>> mappedPartsByShapeIdentifier = shapeIdentifiers(
                shapes,
                piecesByShape);
        JSONArray dimensions = drawing.optJSONArray("dimensions");
        for (int dimensionIndex = 0;
             dimensions != null
                     && dimensionIndex < Math.min(dimensions.length(), MAX_DIMENSIONS);
             dimensionIndex++) {
            JSONObject dimension = dimensions.optJSONObject(dimensionIndex);
            if (!measuredDimension(dimension)) continue;
            double value = dimension.optDouble("value_inches", Double.NaN);
            String role = dimension.optString("role", "").trim().toLowerCase(Locale.US);
            if (!dimensionRole(role)) continue;

            Set<String> identifiers = dimensionIdentifiers(dimension);
            boolean linked = false;
            for (String identifier : identifiers) {
                Set<Integer> directParts = directPartsByIdentifier.get(identifier);
                if (directParts != null && !directParts.isEmpty()) {
                    linked = true;
                    for (Integer partIndex : directParts) {
                        JSONObject part = calculationParts.optJSONObject(partIndex);
                        if (!dimensionMatches(part, role, value)) {
                            return linkedConflict(part, role, value);
                        }
                    }
                    continue;
                }

                Set<Integer> shapeParts = mappedPartsByShapeIdentifier.get(identifier);
                if (shapeParts == null || shapeParts.isEmpty()) continue;
                linked = true;
                boolean matchesMappedPart = false;
                for (Integer partIndex : shapeParts) {
                    if (dimensionMatches(
                            calculationParts.optJSONObject(partIndex),
                            role,
                            value)) {
                        matchesMappedPart = true;
                        break;
                    }
                }
                if (!matchesMappedPart) {
                    return nearbyConflict(role, value, shapeParts, calculationParts);
                }
            }
            if (linked) continue;

            int nearbyShapeIndex = uniqueNearbyShapeIndex(drawing, dimension);
            List<DrawingPieceSummary.Piece> nearbyPieces = piecesByShape.get(nearbyShapeIndex);
            if (nearbyPieces == null || nearbyPieces.isEmpty()) continue;
            boolean matchesNearbyPiece = false;
            for (DrawingPieceSummary.Piece piece : nearbyPieces) {
                if (dimensionMatches(
                        calculationParts.optJSONObject(piece.partIndex),
                        role,
                        value)) {
                    matchesNearbyPiece = true;
                    break;
                }
            }
            if (!matchesNearbyPiece) {
                Set<Integer> nearbyPartIndexes = new HashSet<>();
                for (DrawingPieceSummary.Piece piece : nearbyPieces) {
                    nearbyPartIndexes.add(piece.partIndex);
                }
                return nearbyConflict(
                        role,
                        value,
                        nearbyPartIndexes,
                        calculationParts);
            }
        }

        for (int i = 0;
             calculationParts != null && i < Math.min(calculationParts.length(), MAX_PARTS);
             i++) {
            JSONObject part = calculationParts.optJSONObject(i);
            if (!affectsArea(part)) continue;
            String confidence = part.optString("confidence", "").trim();
            if ("low".equalsIgnoreCase(confidence)) {
                return incomplete(
                        "Please verify this piece's length and width in inches because the AI marked them low confidence.");
            }
            String source = part.optString("measurement_source", "").trim();
            boolean allowedStoveDefault = DrawingRules.FEATURE_STOVE.equals(
                    DrawingRules.featureType(part))
                    && "default".equals(source)
                    && nearlyEqual(part.optDouble("length_inches", Double.NaN), 30);
            if (!source.isEmpty() && !"explicit".equals(source) && !allowedStoveDefault) {
                return incomplete(
                        "Please verify this piece's length and width in inches because they were not both read explicitly.");
            }
        }
        return Result.complete();
    }

    private static Result linkedConflict(JSONObject part, String role, double displayedValue) {
        double formulaValue = formulaValue(part, role);
        String requestedRole = "width".equals(role) ? "width" : "length";
        if (positive(formulaValue)) {
            return incomplete(
                    "What is this piece's "
                            + requestedRole
                            + " in inches? The displayed "
                            + format(displayedValue)
                            + "\" does not match the "
                            + format(formulaValue)
                            + "\" calculation.");
        }
        return incomplete(
                "What is this piece's "
                        + requestedRole
                        + " in inches? The displayed measurement and calculation disagree.");
    }

    private static Result nearbyConflict(
            String role,
            double displayedValue,
            Set<Integer> partIndexes,
            JSONArray calculationParts) {
        JSONObject firstPart = null;
        for (Integer partIndex : partIndexes) {
            firstPart = calculationParts.optJSONObject(partIndex);
            if (firstPart != null) break;
        }
        String pair = dimensionPair(firstPart);
        String roleText = "width".equals(role) ? "width" : "length";
        return incomplete(
                "What is this piece's "
                        + roleText
                        + " in inches? The nearby "
                        + format(displayedValue)
                        + "\" annotation does not match"
                        + (pair.isEmpty() ? " its calculation." : " its " + pair + " in calculation."));
    }

    private static Map<String, Set<Integer>> directPartIdentifiers(JSONArray parts) {
        Map<String, Set<Integer>> result = new HashMap<>();
        if (parts == null) return result;
        for (int i = 0; i < Math.min(parts.length(), MAX_PARTS); i++) {
            JSONObject part = parts.optJSONObject(i);
            addIndex(result, part == null ? "" : part.optString("id", ""), i);
            addIndex(result, part == null ? "" : part.optString("link_id", ""), i);
        }
        return result;
    }

    private static Map<String, Set<Integer>> shapeIdentifiers(
            JSONArray shapes,
            Map<Integer, List<DrawingPieceSummary.Piece>> piecesByShape) {
        Map<String, Set<Integer>> result = new HashMap<>();
        if (shapes == null) return result;
        for (Map.Entry<Integer, List<DrawingPieceSummary.Piece>> entry
                : piecesByShape.entrySet()) {
            JSONObject shape = shapes.optJSONObject(entry.getKey());
            if (shape == null) continue;
            for (DrawingPieceSummary.Piece piece : entry.getValue()) {
                addIndex(result, shape.optString("id", ""), piece.partIndex);
                addIndex(result, shape.optString("link_id", ""), piece.partIndex);
            }
        }
        return result;
    }

    private static void addIndex(Map<String, Set<Integer>> values, String identifier, int index) {
        String clean = identifier == null ? "" : identifier.trim();
        if (clean.isEmpty()) return;
        Set<Integer> indexes = values.get(clean);
        if (indexes == null) {
            indexes = new HashSet<>();
            values.put(clean, indexes);
        }
        indexes.add(index);
    }

    private static Set<String> dimensionIdentifiers(JSONObject dimension) {
        Set<String> result = new HashSet<>();
        if (dimension == null) return result;
        JSONArray identifiers = dimension.optJSONArray("part_ids");
        if (identifiers != null) {
            for (int i = 0; i < Math.min(identifiers.length(), MAX_IDENTIFIERS); i++) {
                addIdentifier(result, identifiers.optString(i, ""));
            }
        }
        addIdentifier(result, dimension.optString("part_id", ""));
        addIdentifier(result, dimension.optString("link_id", ""));
        return result;
    }

    private static void addIdentifier(Set<String> values, String value) {
        String clean = value == null ? "" : value.trim();
        if (!clean.isEmpty()) values.add(clean);
    }

    private static boolean dimensionMatches(JSONObject part, String role, double value) {
        if (part == null || !positive(value)) return false;
        double length = part.optDouble("length_inches", Double.NaN);
        double width = part.optDouble("width_inches", Double.NaN);
        if ("length".equals(role)) return nearlyEqual(value, length);
        if ("width".equals(role)) return nearlyEqual(value, width);
        return nearlyEqual(value, length) || nearlyEqual(value, width);
    }

    private static double formulaValue(JSONObject part, String role) {
        if (part == null) return Double.NaN;
        if ("width".equals(role)) return part.optDouble("width_inches", Double.NaN);
        if ("length".equals(role)) return part.optDouble("length_inches", Double.NaN);
        return Double.NaN;
    }

    private static String dimensionPair(JSONObject part) {
        if (part == null) return "";
        double length = part.optDouble("length_inches", Double.NaN);
        double width = part.optDouble("width_inches", Double.NaN);
        if (!positive(length) || !positive(width)) return "";
        return format(length) + " × " + format(width);
    }

    private static int uniqueNearbyShapeIndex(JSONObject drawing, JSONObject dimension) {
        if (!hasCoordinates(dimension)) return -1;
        JSONArray shapes = drawing.optJSONArray("shapes");
        if (shapes == null) return -1;
        double x = (dimension.optDouble("x1") + dimension.optDouble("x2")) / 2.0;
        double y = (dimension.optDouble("y1") + dimension.optDouble("y2")) / 2.0;
        double canvasWidth = positiveOr(drawing.optDouble("canvas_width", 1000), 1000);
        double canvasHeight = positiveOr(drawing.optDouble("canvas_height", 700), 700);
        double maximumDistance = Math.max(
                45,
                Math.min(100, Math.max(canvasWidth, canvasHeight) * 0.08));
        int bestIndex = -1;
        double bestDistance = Double.MAX_VALUE;
        double secondDistance = Double.MAX_VALUE;
        for (int i = 0; i < Math.min(shapes.length(), MAX_SHAPES); i++) {
            JSONObject shape = shapes.optJSONObject(i);
            if (!drawable(shape)) continue;
            double distance = Math.sqrt(distanceToShapeSquared(shape, x, y));
            if (distance < bestDistance) {
                secondDistance = bestDistance;
                bestDistance = distance;
                bestIndex = i;
            } else if (distance < secondDistance) {
                secondDistance = distance;
            }
        }
        if (bestIndex < 0 || bestDistance > maximumDistance) return -1;
        if (secondDistance < Double.MAX_VALUE
                && secondDistance <= bestDistance + 20
                && secondDistance <= bestDistance * 1.4) return -1;
        return bestIndex;
    }

    private static double distanceToShapeSquared(JSONObject shape, double x, double y) {
        JSONArray points = shape == null ? null : shape.optJSONArray("points");
        if (points == null || points.length() < 3) return Double.MAX_VALUE;
        double best = Double.MAX_VALUE;
        int count = Math.min(points.length(), 16);
        for (int i = 0; i < count; i++) {
            JSONObject first = points.optJSONObject(i);
            JSONObject second = points.optJSONObject((i + 1) % count);
            if (first == null || second == null) continue;
            best = Math.min(best, distanceToSegmentSquared(
                    x,
                    y,
                    first.optDouble("x", 0),
                    first.optDouble("y", 0),
                    second.optDouble("x", 0),
                    second.optDouble("y", 0)));
        }
        return best;
    }

    private static double distanceToSegmentSquared(
            double x,
            double y,
            double x1,
            double y1,
            double x2,
            double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 0.0001) {
            double px = x - x1;
            double py = y - y1;
            return px * px + py * py;
        }
        double t = ((x - x1) * dx + (y - y1) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        double px = x - (x1 + t * dx);
        double py = y - (y1 + t * dy);
        return px * px + py * py;
    }

    private static boolean hasCoordinates(JSONObject dimension) {
        if (dimension == null) return false;
        for (String key : new String[]{"x1", "y1", "x2", "y2"}) {
            if (!dimension.has(key)
                    || !Double.isFinite(dimension.optDouble(key, Double.NaN))) return false;
        }
        return true;
    }

    private static boolean measuredDimension(JSONObject dimension) {
        return dimension != null
                && positive(dimension.optDouble("value_inches", Double.NaN));
    }

    private static boolean dimensionRole(String role) {
        return "length".equals(role) || "width".equals(role) || "both".equals(role);
    }

    private static boolean drawable(JSONObject shape) {
        JSONArray points = shape == null ? null : shape.optJSONArray("points");
        return points != null && points.length() >= 3;
    }

    private static boolean affectsArea(JSONObject item) {
        if (item == null) return false;
        String feature = DrawingRules.featureType(item);
        return !DrawingRules.FEATURE_SINK.equals(feature)
                && !DrawingRules.FEATURE_COOKTOP.equals(feature);
    }

    private static boolean compatible(JSONObject part, JSONObject shape) {
        if (part == null || shape == null) return false;
        String partFeature = DrawingRules.featureType(part);
        String shapeFeature = DrawingRules.featureType(shape);
        if (partFeature.equals(shapeFeature)) return true;
        return DrawingRules.FEATURE_OTHER.equals(partFeature)
                && (DrawingRules.FEATURE_COUNTERTOP.equals(shapeFeature)
                || DrawingRules.FEATURE_BACKSPLASH.equals(shapeFeature)
                || DrawingRules.FEATURE_OTHER.equals(shapeFeature));
    }

    private static boolean nearlyEqual(double first, double second) {
        return Double.isFinite(first)
                && Double.isFinite(second)
                && Math.abs(first - second) <= VALUE_TOLERANCE_INCHES;
    }

    private static boolean positive(double value) {
        return Double.isFinite(value) && value > 0;
    }

    private static double positiveOr(double value, double fallback) {
        return positive(value) ? value : fallback;
    }

    private static String format(double value) {
        return Math.abs(value - Math.rint(value)) < 0.005
                ? String.valueOf((long) Math.rint(value))
                : String.format(Locale.US, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private static Result incomplete(String question) {
        return new Result(false, question);
    }

    public static final class Result {
        public final boolean canPrice;
        public final String question;

        private Result(boolean canPrice, String question) {
            this.canPrice = canPrice;
            this.question = question == null ? "" : question.trim();
        }

        private static Result complete() {
            return new Result(true, "");
        }
    }
}
