package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Deterministic labels for the pieces that add to or deduct from drawing square footage. */
public final class DrawingPieceSummary {
    private static final int MAX_PARTS = 40;
    private static final int MAX_SHAPES = 24;
    private static final int MAX_PART_IDS = 12;

    private DrawingPieceSummary() {
    }

    public static List<Piece> summarize(JSONObject drawing, JSONArray calculationParts) {
        ArrayList<Piece> result = new ArrayList<>();
        if (calculationParts == null) return result;
        JSONArray shapes = drawing == null ? null : drawing.optJSONArray("shapes");
        ArrayList<FormulaPart> formulaParts = formulaParts(calculationParts, shapes);
        HashSet<Integer> overlapPartIndexes = new HashSet<>();
        double[] displayLength = new double[MAX_PARTS];
        double[] displayWidth = new double[MAX_PARTS];
        double[] overlapSquareInches = new double[MAX_PARTS];
        for (FormulaPart formula : formulaParts) {
            displayLength[formula.partIndex] = formula.length;
            displayWidth[formula.partIndex] = formula.width;
        }
        identifyOverlapAdjustments(
                drawing,
                formulaParts,
                overlapPartIndexes,
                displayLength,
                displayWidth,
                overlapSquareInches);
        int number = 1;
        for (FormulaPart formula : formulaParts) {
            if (overlapPartIndexes.contains(formula.partIndex)) continue;
            int shapeIndex = formula.directShapeIndex >= 0
                    ? formula.directShapeIndex
                    : inferredShapeIndex(drawing, formula, formulaParts);
            double squareInches = formula.length * formula.width * formula.quantity;
            if (!formula.subtract) {
                squareInches -= overlapSquareInches[formula.partIndex];
            }
            if (!positive(squareInches)) continue;
            result.add(new Piece(
                    number++,
                    formula.partIndex,
                    shapeIndex,
                    preferredDimensionIndex(drawing, formula, formulaParts),
                    formula.identifiers,
                    displayLength[formula.partIndex],
                    displayWidth[formula.partIndex],
                    formula.quantity,
                    squareInches / 144.0,
                    formula.subtract));
        }
        return result;
    }

    public static int nextNumber(JSONObject drawing, JSONArray calculationParts) {
        return summarize(drawing, calculationParts).size() + 1;
    }

    public static int nextNumber(JSONArray calculationParts) {
        int counted = 0;
        if (calculationParts != null) {
            for (int i = 0; i < Math.min(MAX_PARTS, calculationParts.length()); i++) {
                if (contributesSquareFeet(calculationParts.optJSONObject(i))) counted++;
            }
        }
        return counted + 1;
    }

    public static Piece forShape(List<Piece> pieces, int shapeIndex) {
        if (pieces == null) return null;
        for (Piece piece : pieces) {
            if (piece != null && piece.shapeIndex == shapeIndex) return piece;
        }
        return null;
    }

    public static Piece forDimension(List<Piece> pieces, JSONObject dimension) {
        if (pieces == null || dimension == null) return null;
        Set<String> dimensionIdentifiers = dimensionIdentifiers(dimension);
        if (dimensionIdentifiers.isEmpty()) return null;
        Piece result = null;
        for (Piece piece : pieces) {
            if (piece == null || !piece.matchesAny(dimensionIdentifiers)) continue;
            if (result != null) return null;
            result = piece;
        }
        return result;
    }

    public static int firstDimensionIndex(
            JSONObject drawing,
            Piece piece,
            String role,
            Set<Integer> excludedIndexes) {
        JSONArray dimensions = drawing == null ? null : drawing.optJSONArray("dimensions");
        if (dimensions == null || piece == null) return -1;
        for (int i = 0; i < Math.min(50, dimensions.length()); i++) {
            if (excludedIndexes != null && excludedIndexes.contains(i)) continue;
            JSONObject dimension = dimensions.optJSONObject(i);
            if (dimension == null) continue;
            String dimensionRole = dimension.optString("role", "");
            boolean roleMatches = role.equals(dimensionRole)
                    || ("both".equals(dimensionRole)
                    && ("length".equals(role) || "width".equals(role)));
            if (!roleMatches) continue;
            if (piece.matchesAny(dimensionIdentifiers(dimension))) return i;
        }
        return -1;
    }

    private static ArrayList<FormulaPart> formulaParts(
            JSONArray calculationParts,
            JSONArray shapes) {
        ArrayList<FormulaPart> result = new ArrayList<>();
        for (int partIndex = 0;
             partIndex < Math.min(MAX_PARTS, calculationParts.length());
             partIndex++) {
            JSONObject part = calculationParts.optJSONObject(partIndex);
            if (!contributesSquareFeet(part)) continue;
            double length = part.optDouble("length_inches", 0);
            double width = part.optDouble("width_inches", 0);
            double quantity = part.optDouble("quantity", 1);
            boolean subtract = DrawingRules.FEATURE_STOVE.equals(DrawingRules.featureType(part))
                    || "subtract".equals(part.optString("operation", ""));
            result.add(new FormulaPart(
                    partIndex,
                    part,
                    identifiers(part),
                    length,
                    width,
                    quantity,
                    subtract,
                    uniqueMatchingShapeIndex(part, shapes)));
        }
        return result;
    }

    private static void identifyOverlapAdjustments(
            JSONObject drawing,
            List<FormulaPart> formulas,
            Set<Integer> overlapPartIndexes,
            double[] displayLength,
            double[] displayWidth,
            double[] overlapSquareInches) {
        JSONArray dimensions = drawing == null ? null : drawing.optJSONArray("dimensions");
        if (dimensions == null) return;
        for (int dimensionIndex = 0;
             dimensionIndex < Math.min(50, dimensions.length());
             dimensionIndex++) {
            JSONObject dimension = dimensions.optJSONObject(dimensionIndex);
            if (dimension == null || !"both".equals(dimension.optString("role", ""))) continue;
            Set<String> linkedIdentifiers = dimensionIdentifiers(dimension);
            ArrayList<FormulaPart> linkedAdds = new ArrayList<>();
            FormulaPart linkedSubtract = null;
            boolean ambiguousSubtract = false;
            for (FormulaPart formula : formulas) {
                if (!formula.matchesAny(linkedIdentifiers)) continue;
                if (!formula.subtract) {
                    linkedAdds.add(formula);
                } else if (isOverlapCandidate(formula, dimension)) {
                    if (linkedSubtract == null) linkedSubtract = formula;
                    else ambiguousSubtract = true;
                }
            }
            if (ambiguousSubtract || linkedSubtract == null || linkedAdds.size() < 2) continue;
            if (overlapPartIndexes.contains(linkedSubtract.partIndex)) continue;
            FormulaPart target = overlapTarget(
                    drawing,
                    formulas,
                    linkedAdds,
                    linkedSubtract);
            if (target == null) continue;
            double adjustmentArea = linkedSubtract.length
                    * linkedSubtract.width
                    * linkedSubtract.quantity;
            double targetArea = target.length * target.width * target.quantity;
            if (!positive(adjustmentArea) || adjustmentArea >= targetArea) continue;
            boolean reducedPhysicalDimension = false;
            if (nearlyEqual(displayWidth[target.partIndex], linkedSubtract.width)
                    && displayLength[target.partIndex] > linkedSubtract.length) {
                displayLength[target.partIndex] -= linkedSubtract.length;
                reducedPhysicalDimension = true;
            } else if (nearlyEqual(displayLength[target.partIndex], linkedSubtract.length)
                    && displayWidth[target.partIndex] > linkedSubtract.width) {
                displayWidth[target.partIndex] -= linkedSubtract.width;
                reducedPhysicalDimension = true;
            }
            if (!reducedPhysicalDimension) continue;
            overlapPartIndexes.add(linkedSubtract.partIndex);
            overlapSquareInches[target.partIndex] += adjustmentArea;
        }
    }

    private static FormulaPart overlapTarget(
            JSONObject drawing,
            List<FormulaPart> formulas,
            List<FormulaPart> linkedAdds,
            FormulaPart overlap) {
        JSONArray shapes = drawing == null ? null : drawing.optJSONArray("shapes");
        if (shapes == null || overlap == null) return null;
        int sharedShapeIndex = -1;
        for (FormulaPart add : linkedAdds) {
            int shapeIndex = add.directShapeIndex >= 0
                    ? add.directShapeIndex
                    : inferredShapeIndex(drawing, add, formulas);
            if (shapeIndex < 0) return null;
            if (sharedShapeIndex < 0) sharedShapeIndex = shapeIndex;
            else if (sharedShapeIndex != shapeIndex) return null;
        }
        JSONObject sharedShape = shapes.optJSONObject(sharedShapeIndex);
        JSONArray points = sharedShape == null ? null : sharedShape.optJSONArray("points");
        if (points == null || points.length() <= 4) return null;

        FormulaPart best = null;
        double bestArea = Double.MAX_VALUE;
        for (FormulaPart add : linkedAdds) {
            if (!nearlyEqual(add.quantity, 1.0)) continue;
            boolean canRemoveLength = nearlyEqual(add.width, overlap.width)
                    && add.length > overlap.length;
            boolean canRemoveWidth = nearlyEqual(add.length, overlap.length)
                    && add.width > overlap.width;
            if (!canRemoveLength && !canRemoveWidth) continue;
            double area = add.length * add.width;
            if (area < bestArea) {
                best = add;
                bestArea = area;
            }
        }
        return best;
    }

    private static boolean isOverlapCandidate(FormulaPart formula, JSONObject dimension) {
        if (formula == null
                || !formula.subtract
                || formula.directShapeIndex >= 0
                || !nearlyEqual(formula.quantity, 1.0)
                || !DrawingRules.FEATURE_OTHER.equals(DrawingRules.featureType(formula.part))) {
            return false;
        }
        double sharedValue = dimension.optDouble("value_inches", Double.NaN);
        return positive(sharedValue)
                && nearlyEqual(formula.length, sharedValue)
                && nearlyEqual(formula.width, sharedValue);
    }

    private static int preferredDimensionIndex(
            JSONObject drawing,
            FormulaPart formula,
            List<FormulaPart> formulas) {
        JSONArray dimensions = drawing == null ? null : drawing.optJSONArray("dimensions");
        if (dimensions == null || formula == null) return -1;
        int firstExclusive = -1;
        int firstMatching = -1;
        for (int i = 0; i < Math.min(50, dimensions.length()); i++) {
            JSONObject dimension = dimensions.optJSONObject(i);
            if (dimension == null
                    || !formula.matchesAny(dimensionIdentifiers(dimension))) continue;
            if (firstMatching < 0) firstMatching = i;
            boolean exclusive = matchingFormulaCount(dimension, formulas) == 1;
            if (!exclusive) continue;
            if (firstExclusive < 0) firstExclusive = i;
            if ("length".equals(dimension.optString("role", ""))) return i;
        }
        return firstExclusive >= 0 ? firstExclusive : firstMatching;
    }

    private static int inferredShapeIndex(
            JSONObject drawing,
            FormulaPart formula,
            List<FormulaPart> formulas) {
        JSONArray shapes = drawing == null ? null : drawing.optJSONArray("shapes");
        JSONArray dimensions = drawing == null ? null : drawing.optJSONArray("dimensions");
        if (shapes == null || dimensions == null || formula == null) return -1;
        ArrayList<JSONObject> guides = new ArrayList<>();
        for (int i = 0; i < Math.min(50, dimensions.length()); i++) {
            JSONObject dimension = dimensions.optJSONObject(i);
            if (dimension == null
                    || !formula.matchesAny(dimensionIdentifiers(dimension))) continue;
            if (matchingFormulaCount(dimension, formulas) == 1) guides.add(dimension);
        }
        if (guides.isEmpty()) return -1;
        int bestIndex = -1;
        double bestDistance = Double.MAX_VALUE;
        double secondDistance = Double.MAX_VALUE;
        for (int shapeIndex = 0;
             shapeIndex < Math.min(MAX_SHAPES, shapes.length());
             shapeIndex++) {
            JSONObject shape = shapes.optJSONObject(shapeIndex);
            if (!compatibleShape(formula.part, shape)) continue;
            double totalDistance = 0;
            int usedGuides = 0;
            for (JSONObject guide : guides) {
                double x = (guide.optDouble("x1", 0) + guide.optDouble("x2", 0)) / 2.0;
                double y = (guide.optDouble("y1", 0) + guide.optDouble("y2", 0)) / 2.0;
                double distance = distanceToShapeSquared(shape, x, y);
                if (!Double.isFinite(distance)) continue;
                totalDistance += Math.sqrt(distance);
                usedGuides++;
            }
            if (usedGuides == 0) continue;
            double averageDistance = totalDistance / usedGuides;
            if (averageDistance < bestDistance) {
                secondDistance = bestDistance;
                bestDistance = averageDistance;
                bestIndex = shapeIndex;
            } else if (averageDistance < secondDistance) {
                secondDistance = averageDistance;
            }
        }
        if (bestIndex < 0 || bestDistance > 80.0) return -1;
        if (secondDistance < Double.MAX_VALUE
                && secondDistance <= bestDistance + 25.0
                && secondDistance <= bestDistance * 1.5) return -1;
        return bestIndex;
    }

    private static boolean compatibleShape(JSONObject part, JSONObject shape) {
        if (part == null || shape == null) return false;
        String partFeature = DrawingRules.featureType(part);
        String shapeFeature = DrawingRules.featureType(shape);
        if (DrawingRules.FEATURE_OTHER.equals(partFeature)) {
            return "add".equals(part.optString("operation", ""))
                    && (DrawingRules.FEATURE_COUNTERTOP.equals(shapeFeature)
                    || DrawingRules.FEATURE_BACKSPLASH.equals(shapeFeature)
                    || DrawingRules.FEATURE_OTHER.equals(shapeFeature));
        }
        return partFeature.equals(shapeFeature)
                || (DrawingRules.FEATURE_COUNTERTOP.equals(partFeature)
                && DrawingRules.FEATURE_OTHER.equals(shapeFeature));
    }

    private static double distanceToShapeSquared(JSONObject shape, double x, double y) {
        JSONArray points = shape == null ? null : shape.optJSONArray("points");
        if (points == null || points.length() < 3) return Double.NaN;
        double best = Double.MAX_VALUE;
        int count = Math.min(16, points.length());
        for (int i = 0; i < count; i++) {
            JSONObject first = points.optJSONObject(i);
            JSONObject second = points.optJSONObject((i + 1) % count);
            if (first == null || second == null) continue;
            double distance = distanceToSegmentSquared(
                    x,
                    y,
                    first.optDouble("x", 0),
                    first.optDouble("y", 0),
                    second.optDouble("x", 0),
                    second.optDouble("y", 0));
            best = Math.min(best, distance);
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

    private static int matchingFormulaCount(
            JSONObject dimension,
            List<FormulaPart> formulas) {
        Set<String> linkedIdentifiers = dimensionIdentifiers(dimension);
        int matches = 0;
        for (FormulaPart formula : formulas) {
            if (formula.matchesAny(linkedIdentifiers)) matches++;
        }
        return matches;
    }

    private static boolean contributesSquareFeet(JSONObject part) {
        if (part == null) return false;
        String feature = DrawingRules.featureType(part);
        if (DrawingRules.FEATURE_SINK.equals(feature)
                || DrawingRules.FEATURE_COOKTOP.equals(feature)) return false;
        double length = part.optDouble("length_inches", Double.NaN);
        double width = part.optDouble("width_inches", Double.NaN);
        double quantity = part.optDouble("quantity", 1);
        if (!positive(length) || !positive(width) || !positive(quantity)) return false;
        if (DrawingRules.FEATURE_STOVE.equals(feature)) return true;
        String operation = part.optString("operation", "");
        return "add".equals(operation) || "subtract".equals(operation);
    }

    private static int uniqueMatchingShapeIndex(
            JSONObject part,
            JSONArray shapes) {
        if (part == null || shapes == null) return -1;
        int linkedIndex = DrawingRules.uniqueLinkedIndex(part, shapes);
        if (linkedIndex < 0 || linkedIndex >= MAX_SHAPES) return -1;
        return linkedIndex;
    }

    private static Set<String> identifiers(JSONObject item) {
        HashSet<String> result = new HashSet<>();
        if (item == null) return result;
        add(result, item.optString("id", ""));
        add(result, item.optString("link_id", ""));
        return result;
    }

    private static Set<String> dimensionIdentifiers(JSONObject dimension) {
        HashSet<String> result = new HashSet<>();
        if (dimension == null) return result;
        JSONArray partIds = dimension.optJSONArray("part_ids");
        if (partIds != null) {
            for (int i = 0; i < Math.min(MAX_PART_IDS, partIds.length()); i++) {
                add(result, partIds.optString(i, ""));
            }
        }
        add(result, dimension.optString("part_id", ""));
        return result;
    }

    private static boolean intersects(Set<String> first, Set<String> second) {
        for (String value : first) {
            if (second.contains(value)) return true;
        }
        return false;
    }

    private static void add(Set<String> values, String value) {
        String clean = value == null ? "" : value.trim();
        if (!clean.isEmpty()) values.add(clean);
    }

    private static boolean positive(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0;
    }

    private static boolean nearlyEqual(double first, double second) {
        return Double.isFinite(first)
                && Double.isFinite(second)
                && Math.abs(first - second) <= 0.01;
    }

    public static String formatInches(double value) {
        return formatDimensionNumber(value) + "\"";
    }

    private static String formatDimensionNumber(double value) {
        return Math.abs(value - Math.rint(value)) < 0.005
                ? String.valueOf((long) Math.rint(value))
                : String.format(Locale.US, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    public static String formatSquareFeet(double value) {
        return String.format(Locale.US, "%.2f sq ft", Math.abs(value));
    }

    public static String formatDimensionPair(double lengthInches, double widthInches) {
        return formatDimensionNumber(lengthInches)
                + " × "
                + formatDimensionNumber(widthInches)
                + " in";
    }

    public static final class Piece {
        public final int number;
        public final int partIndex;
        public final int shapeIndex;
        /** Best exclusive dimension guide for placing this label inside a composite shape. */
        public final int preferredDimensionIndex;
        public final double lengthInches;
        public final double widthInches;
        public final double quantity;
        public final double squareFeet;
        public final boolean subtract;
        private final Set<String> identifiers;

        private Piece(
                int number,
                int partIndex,
                int shapeIndex,
                int preferredDimensionIndex,
                Set<String> identifiers,
                double lengthInches,
                double widthInches,
                double quantity,
                double squareFeet,
                boolean subtract) {
            this.number = number;
            this.partIndex = partIndex;
            this.shapeIndex = shapeIndex;
            this.preferredDimensionIndex = preferredDimensionIndex;
            this.identifiers = new HashSet<>(identifiers);
            this.lengthInches = lengthInches;
            this.widthInches = widthInches;
            this.quantity = quantity;
            this.squareFeet = squareFeet;
            this.subtract = subtract;
        }

        public String dimensionsText() {
            return formatInches(lengthInches) + " × " + formatInches(widthInches);
        }

        public String squareFeetText() {
            return (subtract ? "−" : "") + formatSquareFeet(squareFeet);
        }

        /** Three centered canvas-label lines; dimension arrows retain {@link #dimensionsText()}. */
        public String[] labelLines() {
            return new String[]{
                    "#" + number,
                    formatDimensionPair(lengthInches, widthInches),
                    squareFeetText()
            };
        }

        public boolean matchesIdentifier(String identifier) {
            return identifier != null && identifiers.contains(identifier.trim());
        }

        private boolean matchesAny(Set<String> values) {
            return intersects(identifiers, values);
        }
    }

    private static final class FormulaPart {
        final int partIndex;
        final JSONObject part;
        final Set<String> identifiers;
        final double length;
        final double width;
        final double quantity;
        final boolean subtract;
        final int directShapeIndex;

        FormulaPart(
                int partIndex,
                JSONObject part,
                Set<String> identifiers,
                double length,
                double width,
                double quantity,
                boolean subtract,
                int directShapeIndex) {
            this.partIndex = partIndex;
            this.part = part;
            this.identifiers = new HashSet<>(identifiers);
            this.length = length;
            this.width = width;
            this.quantity = quantity;
            this.subtract = subtract;
            this.directShapeIndex = directShapeIndex;
        }

        boolean matchesAny(Set<String> values) {
            return intersects(identifiers, values);
        }
    }
}
