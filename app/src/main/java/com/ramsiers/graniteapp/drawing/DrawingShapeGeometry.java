package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Pure geometry operations for the editable verification drawing. */
public final class DrawingShapeGeometry {
    private static final int MAX_SHAPES = 24;
    private static final int MAX_POINTS = 32;
    private static final int MAX_DIMENSIONS = 50;
    private static final int MAX_PART_IDS = 12;
    private static final double DEFAULT_CANVAS_WIDTH = 1000;
    private static final double DEFAULT_CANVAS_HEIGHT = 700;
    private static final double DEFAULT_RECTANGLE_TOLERANCE_FRACTION = 0.15;

    private DrawingShapeGeometry() {
    }

    /** Snaps only four-corner shapes already close to an axis-aligned rectangle. */
    public static int snapNearAxisAlignedRectangles(JSONObject drawing) {
        JSONArray shapes = drawing == null ? null : drawing.optJSONArray("shapes");
        if (shapes == null) return 0;
        int changed = 0;
        for (int i = 0; i < Math.min(MAX_SHAPES, shapes.length()); i++) {
            if (snapNearAxisAlignedRectangle(shapes.optJSONObject(i))) changed++;
        }
        return changed;
    }

    public static boolean snapNearAxisAlignedRectangle(JSONObject shape) {
        return snapNearAxisAlignedRectangle(shape, DEFAULT_RECTANGLE_TOLERANCE_FRACTION);
    }

    public static boolean snapNearAxisAlignedRectangle(
            JSONObject shape,
            double toleranceFraction) {
        JSONArray points = shape == null ? null : shape.optJSONArray("points");
        if (points == null
                || points.length() != 4
                || !finite(toleranceFraction)
                || toleranceFraction <= 0
                || toleranceFraction > 0.25) return false;

        ArrayList<Point> validPoints = new ArrayList<>();
        Bounds bounds = new Bounds();
        for (int i = 0; i < 4; i++) {
            JSONObject point = points.optJSONObject(i);
            double x = point == null ? Double.NaN : point.optDouble("x", Double.NaN);
            double y = point == null ? Double.NaN : point.optDouble("y", Double.NaN);
            if (!finite(x) || !finite(y)) return false;
            Point validPoint = new Point(x, y);
            validPoints.add(validPoint);
            bounds.include(x, y);
        }
        if (!bounds.valid() || bounds.width() < 5 || bounds.height() < 5) return false;

        Point[] corners = {
                new Point(bounds.left, bounds.top),
                new Point(bounds.right, bounds.top),
                new Point(bounds.right, bounds.bottom),
                new Point(bounds.left, bounds.bottom)
        };
        double tolerance = Math.min(
                24,
                Math.max(2, Math.min(bounds.width(), bounds.height()) * toleranceFraction));
        boolean[] matchedCorners = new boolean[4];
        for (Point point : validPoints) {
            int nearestCorner = -1;
            double nearestDistance = Double.POSITIVE_INFINITY;
            for (int cornerIndex = 0; cornerIndex < corners.length; cornerIndex++) {
                double distance = distance(point.x, point.y, corners[cornerIndex].x, corners[cornerIndex].y);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestCorner = cornerIndex;
                }
            }
            if (nearestCorner < 0
                    || nearestDistance > tolerance
                    || matchedCorners[nearestCorner]) return false;
            matchedCorners[nearestCorner] = true;
        }

        try {
            shape.put("points", rectanglePoints(bounds));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Cleans a redraw for presentation without changing any calculation measurements. Four-point
     * pieces become exact rectangles. Rectilinear L/U pieces keep every corner while their edges
     * become horizontal or vertical. Linked length/width arrows are cardinalized as well.
     */
    public static int orthogonalizeDrawing(JSONObject drawing) {
        if (drawing == null) return 0;
        int changed = 0;
        JSONArray shapes = drawing.optJSONArray("shapes");
        if (shapes != null) {
            for (int i = 0; i < Math.min(MAX_SHAPES, shapes.length()); i++) {
                if (orthogonalizeShape(shapes.optJSONObject(i))) changed++;
            }
        }
        JSONArray dimensions = drawing.optJSONArray("dimensions");
        if (dimensions != null) {
            for (int i = 0; i < Math.min(MAX_DIMENSIONS, dimensions.length()); i++) {
                JSONObject dimension = dimensions.optJSONObject(i);
                if (dimension == null) continue;
                double x1 = dimension.optDouble("x1", Double.NaN);
                double y1 = dimension.optDouble("y1", Double.NaN);
                double x2 = dimension.optDouble("x2", Double.NaN);
                double y2 = dimension.optDouble("y2", Double.NaN);
                if (!finite(x1) || !finite(y1) || !finite(x2) || !finite(y2)) continue;
                try {
                    if (Math.abs(x2 - x1) >= Math.abs(y2 - y1)) {
                        if (Math.abs(y1 - y2) > 0.0001) {
                            double y = (y1 + y2) / 2;
                            dimension.put("y1", y);
                            dimension.put("y2", y);
                            changed++;
                        }
                    } else if (Math.abs(x1 - x2) > 0.0001) {
                        double x = (x1 + x2) / 2;
                        dimension.put("x1", x);
                        dimension.put("x2", x);
                        changed++;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return changed;
    }

    public static boolean orthogonalizeShape(JSONObject shape) {
        JSONArray points = shape == null ? null : shape.optJSONArray("points");
        if (points == null || points.length() < 4 || points.length() > 16) return false;
        int count = points.length();
        Bounds originalBounds = new Bounds();
        ArrayList<Point> original = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            JSONObject source = points.optJSONObject(i);
            double x = source == null ? Double.NaN : source.optDouble("x", Double.NaN);
            double y = source == null ? Double.NaN : source.optDouble("y", Double.NaN);
            if (!finite(x) || !finite(y)) return false;
            original.add(new Point(x, y));
            originalBounds.include(x, y);
        }
        if (!originalBounds.valid()
                || originalBounds.width() < 5
                || originalBounds.height() < 5) return false;

        if (count % 2 != 0) return false;

        boolean[] horizontal = new boolean[count];
        for (int i = 0; i < count; i++) {
            Point start = original.get(i);
            Point end = original.get((i + 1) % count);
            double dx = Math.abs(end.x - start.x);
            double dy = Math.abs(end.y - start.y);
            double major = Math.max(dx, dy);
            if (major < 2 || Math.min(dx, dy) / major > 0.36) return false;
            horizontal[i] = dx >= dy;
        }
        for (int i = 0; i < count; i++) {
            if (horizontal[i] == horizontal[(i + 1) % count]) return false;
        }
        if (count == 4) return snapNearAxisAlignedRectangle(shape, 0.25);

        double[] edgeCoordinate = new double[count];
        for (int i = 0; i < count; i++) {
            Point start = original.get(i);
            Point end = original.get((i + 1) % count);
            edgeCoordinate[i] = horizontal[i]
                    ? (start.y + end.y) / 2
                    : (start.x + end.x) / 2;
        }
        ArrayList<Point> candidate = new ArrayList<>();
        double maximumDisplacement = Math.max(
                24,
                Math.min(originalBounds.width(), originalBounds.height()) * 0.22);
        for (int i = 0; i < count; i++) {
            int previousEdge = (i - 1 + count) % count;
            double x = horizontal[previousEdge] ? edgeCoordinate[i] : edgeCoordinate[previousEdge];
            double y = horizontal[previousEdge] ? edgeCoordinate[previousEdge] : edgeCoordinate[i];
            Point source = original.get(i);
            if (distance(source.x, source.y, x, y) > maximumDisplacement) return false;
            candidate.add(new Point(x, y));
        }
        if (!validOrthogonalCandidate(original, candidate)) return false;

        try {
            JSONArray result = new JSONArray();
            for (Point candidatePoint : candidate) {
                result.put(point(candidatePoint.x, candidatePoint.y));
            }
            shape.put("points", result);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean validOrthogonalCandidate(
            List<Point> original,
            List<Point> candidate) {
        if (original.size() != candidate.size() || candidate.size() < 4) return false;
        for (int i = 0; i < candidate.size(); i++) {
            Point a = candidate.get(i);
            Point b = candidate.get((i + 1) % candidate.size());
            boolean horizontal = Math.abs(a.y - b.y) <= 0.0001;
            boolean vertical = Math.abs(a.x - b.x) <= 0.0001;
            if (horizontal == vertical || distance(a.x, a.y, b.x, b.y) < 2) return false;
        }
        double originalArea = signedPolygonArea(original);
        double candidateArea = signedPolygonArea(candidate);
        if (Math.abs(originalArea) < 25 || Math.abs(candidateArea) < 25) return false;
        if (Math.signum(originalArea) != Math.signum(candidateArea)) return false;
        double areaRatio = Math.abs(candidateArea / originalArea);
        if (areaRatio < 0.65 || areaRatio > 1.35) return false;
        return !selfIntersects(candidate);
    }

    private static double signedPolygonArea(List<Point> points) {
        double twiceArea = 0;
        for (int i = 0; i < points.size(); i++) {
            Point a = points.get(i);
            Point b = points.get((i + 1) % points.size());
            twiceArea += a.x * b.y - b.x * a.y;
        }
        return twiceArea / 2;
    }

    private static boolean selfIntersects(List<Point> points) {
        int count = points.size();
        for (int first = 0; first < count; first++) {
            int firstEnd = (first + 1) % count;
            for (int second = first + 1; second < count; second++) {
                int secondEnd = (second + 1) % count;
                if (first == second
                        || firstEnd == second
                        || secondEnd == first) continue;
                if (segmentsIntersect(
                        points.get(first),
                        points.get(firstEnd),
                        points.get(second),
                        points.get(secondEnd))) return true;
            }
        }
        return false;
    }

    private static boolean segmentsIntersect(Point a, Point b, Point c, Point d) {
        double first = cross(a, b, c);
        double second = cross(a, b, d);
        double third = cross(c, d, a);
        double fourth = cross(c, d, b);
        return first * second < -0.0001 && third * fourth < -0.0001;
    }

    private static double cross(Point a, Point b, Point c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    /** Returns the smallest exact polygon containing the point. */
    public static int findContainingShape(JSONArray shapes, double x, double y) {
        if (shapes == null || !finite(x) || !finite(y)) return -1;
        int bestIndex = -1;
        double bestArea = Double.POSITIVE_INFINITY;
        for (int i = 0; i < Math.min(MAX_SHAPES, shapes.length()); i++) {
            JSONArray points = points(shapes.optJSONObject(i));
            if (points == null || !contains(points, x, y)) continue;
            double area = polygonArea(points);
            if (area < bestArea) {
                bestArea = area;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /** Returns the nearest polygon edge inside a bounded touch tolerance. */
    public static int findNearestShape(
            JSONArray shapes,
            double x,
            double y,
            double maximumDistance) {
        if (shapes == null
                || !finite(x)
                || !finite(y)
                || !finite(maximumDistance)
                || maximumDistance < 0) return -1;
        int bestIndex = -1;
        double bestDistanceSquared = maximumDistance * maximumDistance;
        double bestArea = Double.POSITIVE_INFINITY;
        for (int i = 0; i < Math.min(MAX_SHAPES, shapes.length()); i++) {
            JSONArray points = points(shapes.optJSONObject(i));
            if (points == null) continue;
            double candidateDistance = polygonEdgeDistanceSquared(points, x, y);
            double area = polygonArea(points);
            if (candidateDistance < bestDistanceSquared - 0.0001
                    || Math.abs(candidateDistance - bestDistanceSquared) <= 0.0001
                    && area < bestArea) {
                bestDistanceSquared = candidateDistance;
                bestArea = area;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    public static int findShapeAtOrNear(
            JSONArray shapes,
            double x,
            double y,
            double maximumDistance) {
        int containing = findContainingShape(shapes, x, y);
        return containing >= 0
                ? containing
                : findNearestShape(shapes, x, y, maximumDistance);
    }

    /**
     * Moves one shape and only its linked dimension arrows. A single clamped delta keeps the
     * complete group inside the drawing canvas.
     */
    public static MoveResult moveShapeAndLinkedDimensions(
            JSONObject drawing,
            int shapeIndex,
            double requestedDeltaX,
            double requestedDeltaY) {
        if (drawing == null || !finite(requestedDeltaX) || !finite(requestedDeltaY)) {
            return MoveResult.notMoved();
        }
        JSONArray shapes = drawing.optJSONArray("shapes");
        JSONObject shape = shapes == null
                || shapeIndex < 0
                || shapeIndex >= Math.min(MAX_SHAPES, shapes.length())
                ? null
                : shapes.optJSONObject(shapeIndex);
        JSONArray shapePoints = points(shape);
        if (shapePoints == null) return MoveResult.notMoved();

        Set<String> identifiers = shapeIdentifiers(shape);
        List<JSONObject> linkedDimensions = linkedDimensions(
                drawing.optJSONArray("dimensions"),
                identifiers);
        Bounds groupBounds = new Bounds();
        includePoints(groupBounds, shapePoints);
        for (JSONObject dimension : linkedDimensions) includeDimension(groupBounds, dimension);
        if (!groupBounds.valid()) return MoveResult.notMoved();

        double canvasWidth = positiveOr(
                drawing.optDouble("canvas_width", DEFAULT_CANVAS_WIDTH),
                DEFAULT_CANVAS_WIDTH);
        double canvasHeight = positiveOr(
                drawing.optDouble("canvas_height", DEFAULT_CANVAS_HEIGHT),
                DEFAULT_CANVAS_HEIGHT);
        double deltaX = clampedDelta(
                requestedDeltaX,
                -groupBounds.left,
                canvasWidth - groupBounds.right);
        double deltaY = clampedDelta(
                requestedDeltaY,
                -groupBounds.top,
                canvasHeight - groupBounds.bottom);
        if (Math.abs(deltaX) < 0.0001 && Math.abs(deltaY) < 0.0001) {
            return new MoveResult(false, 0, 0);
        }

        translatePoints(shapePoints, deltaX, deltaY);
        for (JSONObject dimension : linkedDimensions) {
            translateCoordinate(dimension, "x1", deltaX);
            translateCoordinate(dimension, "x2", deltaX);
            translateCoordinate(dimension, "y1", deltaY);
            translateCoordinate(dimension, "y2", deltaY);
        }
        return new MoveResult(true, deltaX, deltaY);
    }

    /**
     * Rebuilds proportional geometry from an immutable baseline. Repeated live edits therefore
     * never compound, and length/width roles remain horizontal/vertical even if an AI arrow is
     * slightly crooked.
     */
    public static JSONObject rebuildLinkedGeometryFromBaseline(
            JSONObject baselineDrawing,
            JSONArray parts,
            Map<Integer, Double> requestedDimensionValues) {
        if (baselineDrawing == null || requestedDimensionValues == null) return null;
        final JSONObject rebuilt;
        try {
            rebuilt = new JSONObject(baselineDrawing.toString());
        } catch (Exception ignored) {
            return null;
        }
        JSONArray baselineDimensions = baselineDrawing.optJSONArray("dimensions");
        JSONArray rebuiltDimensions = rebuilt.optJSONArray("dimensions");
        if (baselineDimensions == null || rebuiltDimensions == null) return rebuilt;

        TreeMap<Integer, Double> orderedRequests = new TreeMap<>(requestedDimensionValues);
        HashMap<String, Double> valuesByTarget = new HashMap<>();
        for (Map.Entry<Integer, Double> request : orderedRequests.entrySet()) {
            int dimensionIndex = request.getKey() == null ? -1 : request.getKey();
            double requestedValue = request.getValue() == null
                    ? Double.NaN
                    : request.getValue();
            JSONObject dimension = dimensionIndex < 0
                    || dimensionIndex >= baselineDimensions.length()
                    ? null
                    : baselineDimensions.optJSONObject(dimensionIndex);
            if (dimension == null || !positive(requestedValue)) continue;
            String target = exactDimensionTarget(dimension);
            Double existing = valuesByTarget.get(target);
            if (existing != null && Math.abs(existing - requestedValue) > 0.05) return null;
            valuesByTarget.put(target, requestedValue);
        }

        HashMap<String, Double> resizedShapeAxes = new HashMap<>();
        for (Map.Entry<Integer, Double> request : orderedRequests.entrySet()) {
            int dimensionIndex = request.getKey() == null ? -1 : request.getKey();
            double requestedValue = request.getValue() == null
                    ? Double.NaN
                    : request.getValue();
            JSONObject baselineDimension = dimensionIndex < 0
                    || dimensionIndex >= baselineDimensions.length()
                    ? null
                    : baselineDimensions.optJSONObject(dimensionIndex);
            if (baselineDimension == null || !positive(requestedValue)) continue;
            double baselineValue = baselineDimension.optDouble("value_inches", Double.NaN);
            if (!positive(baselineValue)) continue;
            if (Math.abs(requestedValue - baselineValue) > 0.0001) {
                ResizeMatch match = resolveResizeShape(
                        baselineDrawing,
                        parts,
                        baselineDimension);
                if (match.ambiguous) return null;
                if (match.shapeIndex >= 0) {
                    int axis = dimensionAxis(baselineDimension);
                    if (axis < 0) return null;
                    double pixelsPerInch = pixelsPerInchForDimension(
                            baselineDrawing,
                            baselineDimension,
                            baselineValue);
                    if (!positive(pixelsPerInch)) return null;
                    double delta = (requestedValue - baselineValue) * pixelsPerInch;
                    String shapeAxis = match.shapeIndex + "\u0000" + axis;
                    Double appliedDelta = resizedShapeAxes.get(shapeAxis);
                    if (appliedDelta != null && Math.abs(appliedDelta - delta) > 0.05) {
                        return null;
                    }
                    if (appliedDelta == null) {
                        JSONObject resizedShape = rebuilt.optJSONArray("shapes")
                                .optJSONObject(match.shapeIndex);
                        BoundaryMove moved = moveTerminalBoundary(
                                rebuilt,
                                resizedShape,
                                axis,
                                delta);
                        if (moved == null) return null;
                        moveAttachedDimensionEnds(
                                rebuilt.optJSONArray("dimensions"),
                                baselineDimension,
                                exactDimensionTarget(baselineDimension),
                                axis,
                                requestedValue,
                                moved.oldStart,
                                moved.oldTerminal,
                                moved.delta,
                                linkedIdentifiers(baselineDimension, parts));
                        resizedShapeAxes.put(shapeAxis, delta);
                    }
                }
            }
        }

        for (Map.Entry<Integer, Double> request : orderedRequests.entrySet()) {
            int dimensionIndex = request.getKey() == null ? -1 : request.getKey();
            double requestedValue = request.getValue() == null
                    ? Double.NaN
                    : request.getValue();
            JSONObject dimension = dimensionIndex < 0
                    || dimensionIndex >= rebuiltDimensions.length()
                    ? null
                    : rebuiltDimensions.optJSONObject(dimensionIndex);
            if (dimension == null || !positive(requestedValue)) continue;
            try {
                dimension.put("value_inches", requestedValue);
                dimension.put("label", formatInches(requestedValue));
            } catch (Exception ignored) {
            }
        }
        return rebuilt;
    }

    /**
     * Resolves one resize to one exact shape. Stable IDs win. Legacy AI drawings may omit those
     * links, so a single nearby compatible shape can be used without writing a guessed link back
     * into the drawing. Any tie remains deliberately unresolved.
     */
    private static ResizeMatch resolveResizeShape(
            JSONObject drawing,
            JSONArray parts,
            JSONObject dimension) {
        JSONArray shapes = drawing == null ? null : drawing.optJSONArray("shapes");
        if (shapes == null || dimension == null) return ResizeMatch.none();
        Set<String> identifiers = linkedIdentifiers(dimension, parts);
        int exactIndex = -1;
        for (int i = 0; i < Math.min(MAX_SHAPES, shapes.length()); i++) {
            if (!matchesShape(shapes.optJSONObject(i), identifiers)) continue;
            if (exactIndex >= 0) return ResizeMatch.ambiguous();
            exactIndex = i;
        }
        if (exactIndex >= 0) return ResizeMatch.match(exactIndex);

        JSONObject linkedPart = uniqueDimensionPart(dimension, parts);
        if (linkedPart == null) return ResizeMatch.none();
        int axis = dimensionAxis(dimension);
        double x1 = dimension.optDouble("x1", Double.NaN);
        double y1 = dimension.optDouble("y1", Double.NaN);
        double x2 = dimension.optDouble("x2", Double.NaN);
        double y2 = dimension.optDouble("y2", Double.NaN);
        if (axis < 0 || !finite(x1) || !finite(y1) || !finite(x2) || !finite(y2)) {
            return ResizeMatch.none();
        }
        double midpointX = (x1 + x2) / 2;
        double midpointY = (y1 + y2) / 2;
        double span = dimensionAxisSpan(dimension);
        double maximumDistance = Math.min(180, Math.max(80, span * 0.4));
        int bestIndex = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        double secondDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < Math.min(MAX_SHAPES, shapes.length()); i++) {
            JSONObject shape = shapes.optJSONObject(i);
            JSONArray shapePoints = points(shape);
            Bounds shapeBounds = bounds(shapePoints);
            if (shapePoints == null
                    || !shapeBounds.valid()
                    || !compatibleLegacyShape(linkedPart, shape)
                    || !dimensionProjectionOverlaps(dimension, shapeBounds, axis)) continue;
            double candidate = Math.sqrt(polygonEdgeDistanceSquared(
                    shapePoints,
                    midpointX,
                    midpointY));
            if (candidate < bestDistance) {
                secondDistance = bestDistance;
                bestDistance = candidate;
                bestIndex = i;
            } else if (candidate < secondDistance) {
                secondDistance = candidate;
            }
        }
        if (bestIndex < 0 || bestDistance > maximumDistance) return ResizeMatch.none();
        if (finite(secondDistance)
                && secondDistance <= bestDistance + 25
                && secondDistance <= Math.max(10, bestDistance * 1.5)) {
            return ResizeMatch.ambiguous();
        }
        return ResizeMatch.match(bestIndex);
    }

    private static JSONObject uniqueDimensionPart(JSONObject dimension, JSONArray parts) {
        if (dimension == null || parts == null) return null;
        Set<String> rawIdentifiers = rawDimensionIdentifiers(dimension);
        if (rawIdentifiers.isEmpty()) return null;
        JSONObject result = null;
        for (int i = 0; i < Math.min(40, parts.length()); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part == null) continue;
            String id = part.optString("id", "").trim();
            String linkId = part.optString("link_id", "").trim();
            if (!rawIdentifiers.contains(id) && !rawIdentifiers.contains(linkId)) continue;
            if (result != null) return null;
            result = part;
        }
        return result;
    }

    private static boolean compatibleLegacyShape(JSONObject part, JSONObject shape) {
        if (part == null || shape == null) return false;
        String partFeature = DrawingRules.featureType(part);
        String shapeFeature = DrawingRules.featureType(shape);
        if (DrawingRules.FEATURE_SINK.equals(partFeature)
                || DrawingRules.FEATURE_COOKTOP.equals(partFeature)
                || DrawingRules.FEATURE_STOVE.equals(partFeature)) {
            return partFeature.equals(shapeFeature);
        }
        if (DrawingRules.FEATURE_BACKSPLASH.equals(partFeature)) {
            return DrawingRules.FEATURE_BACKSPLASH.equals(shapeFeature);
        }
        return DrawingRules.FEATURE_COUNTERTOP.equals(shapeFeature)
                || DrawingRules.FEATURE_OTHER.equals(shapeFeature);
    }

    private static boolean dimensionProjectionOverlaps(
            JSONObject dimension,
            Bounds shapeBounds,
            int axis) {
        double first = axis == 0
                ? dimension.optDouble("x1", Double.NaN)
                : dimension.optDouble("y1", Double.NaN);
        double second = axis == 0
                ? dimension.optDouble("x2", Double.NaN)
                : dimension.optDouble("y2", Double.NaN);
        if (!finite(first) || !finite(second)) return false;
        double minimum = Math.min(first, second);
        double maximum = Math.max(first, second);
        double shapeMinimum = axis == 0 ? shapeBounds.left : shapeBounds.top;
        double shapeMaximum = axis == 0 ? shapeBounds.right : shapeBounds.bottom;
        double overlap = Math.min(maximum, shapeMaximum) - Math.max(minimum, shapeMinimum);
        return overlap >= Math.min(12, Math.min(maximum - minimum, shapeMaximum - shapeMinimum) * 0.2);
    }

    private static BoundaryMove moveTerminalBoundary(
            JSONObject drawing,
            JSONObject shape,
            int axis,
            double requestedDelta) {
        if (drawing == null || shape == null || axis < 0 || !finite(requestedDelta)) return null;
        orthogonalizeShape(shape);
        JSONArray shapePoints = points(shape);
        Bounds shapeBounds = bounds(shapePoints);
        if (shapePoints == null || !shapeBounds.valid()) return null;
        double start = axis == 0 ? shapeBounds.left : shapeBounds.top;
        double terminal = axis == 0 ? shapeBounds.right : shapeBounds.bottom;
        double tolerance = 0.75;
        ArrayList<Integer> terminalIndexes = new ArrayList<>();
        double previousCoordinate = -Double.MAX_VALUE;
        for (int i = 0; i < Math.min(MAX_POINTS, shapePoints.length()); i++) {
            JSONObject point = shapePoints.optJSONObject(i);
            if (point == null) continue;
            double coordinate = point.optDouble(axis == 0 ? "x" : "y", Double.NaN);
            if (!finite(coordinate)) return null;
            if (Math.abs(coordinate - terminal) <= tolerance) terminalIndexes.add(i);
            else if (coordinate < terminal - tolerance) {
                previousCoordinate = Math.max(previousCoordinate, coordinate);
            }
        }
        if (terminalIndexes.size() != 2
                || !consecutiveIndexes(terminalIndexes, shapePoints.length())) return null;
        double newTerminal = terminal + requestedDelta;
        double canvasLimit = axis == 0
                ? positiveOr(drawing.optDouble("canvas_width", DEFAULT_CANVAS_WIDTH),
                DEFAULT_CANVAS_WIDTH)
                : positiveOr(drawing.optDouble("canvas_height", DEFAULT_CANVAS_HEIGHT),
                DEFAULT_CANVAS_HEIGHT);
        if (!finite(newTerminal)
                || newTerminal > canvasLimit
                || newTerminal <= previousCoordinate + 5) return null;
        String key = axis == 0 ? "x" : "y";
        try {
            for (Integer index : terminalIndexes) {
                shapePoints.optJSONObject(index).put(key, newTerminal);
            }
        } catch (Exception ignored) {
            return null;
        }
        return new BoundaryMove(start, terminal, newTerminal - terminal);
    }

    private static boolean consecutiveIndexes(List<Integer> indexes, int count) {
        if (indexes == null || indexes.size() != 2 || count < 3) return false;
        int first = indexes.get(0);
        int second = indexes.get(1);
        return second == first + 1 || first == 0 && second == count - 1;
    }

    private static void moveAttachedDimensionEnds(
            JSONArray dimensions,
            JSONObject sourceDimension,
            String sourceTarget,
            int axis,
            double requestedValue,
            double oldStart,
            double oldTerminal,
            double delta,
            Set<String> sourceIdentifiers) {
        if (dimensions == null || sourceDimension == null || axis < 0) return;
        String coordinateKey = axis == 0 ? "x" : "y";
        for (int i = 0; i < Math.min(MAX_DIMENSIONS, dimensions.length()); i++) {
            JSONObject dimension = dimensions.optJSONObject(i);
            if (dimension == null) continue;
            int dimensionAxis = dimensionAxis(dimension);
            String target = exactDimensionTarget(dimension);
            if (sourceTarget.equals(target) && dimensionAxis == axis) {
                moveMaximumEndpoint(dimension, coordinateKey, delta);
                try {
                    dimension.put("value_inches", requestedValue);
                    dimension.put("label", formatInches(requestedValue));
                } catch (Exception ignored) {
                }
                continue;
            }
            if (dimensionAxis == axis
                    || !intersects(rawDimensionIdentifiers(dimension), sourceIdentifiers)) continue;
            double first = dimension.optDouble(coordinateKey + "1", Double.NaN);
            double second = dimension.optDouble(coordinateKey + "2", Double.NaN);
            if (!finite(first) || !finite(second)) continue;
            double farDistance = Math.max(
                    Math.abs(first - oldTerminal),
                    Math.abs(second - oldTerminal));
            double startDistance = Math.max(
                    Math.abs(first - oldStart),
                    Math.abs(second - oldStart));
            if (farDistance > 80 || farDistance >= startDistance) continue;
            translateCoordinate(dimension, coordinateKey + "1", delta);
            translateCoordinate(dimension, coordinateKey + "2", delta);
        }
    }

    private static void moveMaximumEndpoint(JSONObject dimension, String coordinateKey, double delta) {
        double first = dimension.optDouble(coordinateKey + "1", Double.NaN);
        double second = dimension.optDouble(coordinateKey + "2", Double.NaN);
        if (!finite(first) || !finite(second)) return;
        if (first > second) translateCoordinate(dimension, coordinateKey + "1", delta);
        else translateCoordinate(dimension, coordinateKey + "2", delta);
    }

    private static int dimensionAxis(JSONObject dimension) {
        if (dimension == null) return -1;
        double x1 = dimension.optDouble("x1", Double.NaN);
        double y1 = dimension.optDouble("y1", Double.NaN);
        double x2 = dimension.optDouble("x2", Double.NaN);
        double y2 = dimension.optDouble("y2", Double.NaN);
        if (!finite(x1) || !finite(y1) || !finite(x2) || !finite(y2)) return -1;
        double horizontal = Math.abs(x2 - x1);
        double vertical = Math.abs(y2 - y1);
        if (Math.max(horizontal, vertical) < 1) return -1;
        return horizontal >= vertical ? 0 : 1;
    }

    private static double dimensionAxisSpan(JSONObject dimension) {
        int axis = dimensionAxis(dimension);
        if (axis < 0) return 0;
        return axis == 0
                ? Math.abs(dimension.optDouble("x2", 0) - dimension.optDouble("x1", 0))
                : Math.abs(dimension.optDouble("y2", 0) - dimension.optDouble("y1", 0));
    }

    private static double pixelsPerInchForDimension(
            JSONObject drawing,
            JSONObject dimension,
            double baselineValue) {
        double local = dimensionAxisSpan(dimension) / baselineValue;
        if (local >= 0.1 && local <= 50) return local;
        return estimateDrawingPixelsPerInch(drawing);
    }

    private static String exactDimensionTarget(JSONObject dimension) {
        ArrayList<String> identifiers = new ArrayList<>(rawDimensionIdentifiers(dimension));
        Collections.sort(identifiers);
        return (dimension == null ? "" : dimension.optString("role", "other"))
                + "\u0000"
                + identifiers;
    }

    private static Set<String> rawDimensionIdentifiers(JSONObject dimension) {
        Set<String> result = new HashSet<>();
        if (dimension == null) return result;
        JSONArray partIds = dimension.optJSONArray("part_ids");
        if (partIds != null) {
            for (int i = 0; i < Math.min(MAX_PART_IDS, partIds.length()); i++) {
                addIdentifier(result, partIds.optString(i, ""));
            }
        }
        addIdentifier(result, dimension.optString("part_id", ""));
        return result;
    }

    /**
     * Rebuilds one selected shape from the immutable edit-session baseline using the exact
     * Length/Width fields. If one old measurement was missing, the known axis supplies the
     * canvas pixels-per-inch scale. The top/left edge stays fixed while only the measured far
     * boundary moves. A fully unmeasured composite shape is rejected instead of guessing which
     * arm should change; a simple rectangle can still use an anchored area/aspect fallback.
     */
    public static JSONObject rebuildMeasuredShapeFromBaseline(
            JSONObject baselineDrawing,
            int shapeIndex,
            double oldLength,
            double oldWidth,
            double newLength,
            double newWidth) {
        if (baselineDrawing == null
                || shapeIndex < 0
                || !nonNegative(oldLength)
                || !nonNegative(oldWidth)
                || !positive(newLength)
                || !positive(newWidth)) return null;
        final JSONObject rebuilt;
        try {
            rebuilt = new JSONObject(baselineDrawing.toString());
        } catch (Exception ignored) {
            return null;
        }
        JSONArray shapes = rebuilt.optJSONArray("shapes");
        JSONObject shape = shapes == null
                || shapeIndex >= Math.min(MAX_SHAPES, shapes.length())
                ? null
                : shapes.optJSONObject(shapeIndex);
        JSONArray shapePoints = points(shape);
        if (shapePoints == null) return null;
        snapNearAxisAlignedRectangle(shape);
        shapePoints = points(shape);
        Bounds bounds = bounds(shapePoints);
        if (!bounds.valid() || bounds.width() < 5 || bounds.height() < 5) return null;

        Set<String> identifiers = shapeIdentifiers(shape);
        JSONArray rebuiltDimensions = rebuilt.optJSONArray("dimensions");
        JSONObject lengthDimension = firstLinkedDimensionForRole(
                rebuiltDimensions,
                identifiers,
                "length");
        JSONObject widthDimension = firstLinkedDimensionForRole(
                rebuiltDimensions,
                identifiers,
                "width");
        int lengthAxis = lengthDimension == null ? 0 : dimensionAxis(lengthDimension);
        int widthAxis = widthDimension == null ? 1 : dimensionAxis(widthDimension);
        if (lengthAxis < 0 || widthAxis < 0 || lengthAxis == widthAxis) return null;

        double drawingPixelsPerInch = estimateDrawingPixelsPerInch(baselineDrawing);
        if (!positive(oldLength)
                && !positive(oldWidth)
                && shapePoints.length() != 4) {
            return null;
        }
        double lengthPixelsPerInch = positive(oldLength)
                ? (lengthAxis == 0 ? bounds.width() : bounds.height()) / oldLength
                : drawingPixelsPerInch;
        double widthPixelsPerInch = positive(oldWidth)
                ? (widthAxis == 0 ? bounds.width() : bounds.height()) / oldWidth
                : drawingPixelsPerInch;
        if (!positive(lengthPixelsPerInch) || !positive(widthPixelsPerInch)) {
            if (shapePoints.length() != 4) return null;
            double currentAspect = bounds.width() / bounds.height();
            double requestedAspect = newLength / newWidth;
            double aspectChange = requestedAspect / currentAspect;
            if (!positive(aspectChange)) return null;
            double scaleX = Math.sqrt(aspectChange);
            double scaleY = 1 / scaleX;
            lengthPixelsPerInch = bounds.width() * scaleX / newLength;
            widthPixelsPerInch = bounds.height() * scaleY / newWidth;
        }

        double lengthCurrentExtent = lengthAxis == 0 ? bounds.width() : bounds.height();
        BoundaryMove lengthMove = moveTerminalBoundary(
                rebuilt,
                shape,
                lengthAxis,
                newLength * lengthPixelsPerInch - lengthCurrentExtent);
        if (lengthMove == null) return null;
        if (lengthDimension != null) {
            moveAttachedDimensionEnds(
                    rebuiltDimensions,
                    lengthDimension,
                    exactDimensionTarget(lengthDimension),
                    lengthAxis,
                    newLength,
                    lengthMove.oldStart,
                    lengthMove.oldTerminal,
                    lengthMove.delta,
                    identifiers);
        }

        Bounds afterLengthBounds = bounds(points(shape));
        double widthCurrentExtent = widthAxis == 0
                ? afterLengthBounds.width()
                : afterLengthBounds.height();
        BoundaryMove widthMove = moveTerminalBoundary(
                rebuilt,
                shape,
                widthAxis,
                newWidth * widthPixelsPerInch - widthCurrentExtent);
        if (widthMove == null) return null;
        if (widthDimension != null) {
            moveAttachedDimensionEnds(
                    rebuiltDimensions,
                    widthDimension,
                    exactDimensionTarget(widthDimension),
                    widthAxis,
                    newWidth,
                    widthMove.oldStart,
                    widthMove.oldTerminal,
                    widthMove.delta,
                    identifiers);
        }
        return rebuilt;
    }

    private static JSONObject firstLinkedDimensionForRole(
            JSONArray dimensions,
            Set<String> identifiers,
            String role) {
        if (dimensions == null || identifiers == null || identifiers.isEmpty()) return null;
        JSONObject result = null;
        for (int i = 0; i < Math.min(MAX_DIMENSIONS, dimensions.length()); i++) {
            JSONObject dimension = dimensions.optJSONObject(i);
            if (dimension == null
                    || !role.equals(dimension.optString("role", ""))
                    || !dimensionMatches(dimension, identifiers)) continue;
            if (result != null && dimensionAxis(result) != dimensionAxis(dimension)) return null;
            result = dimension;
        }
        return result;
    }

    /** Median canvas scale used to keep edited pieces proportional to the other pieces. */
    public static double estimateDrawingPixelsPerInch(JSONObject drawing) {
        JSONArray shapes = drawing == null ? null : drawing.optJSONArray("shapes");
        JSONArray dimensions = drawing == null ? null : drawing.optJSONArray("dimensions");
        if (shapes == null || dimensions == null) return 0;
        ArrayList<Double> scales = new ArrayList<>();
        HashSet<String> usedTargets = new HashSet<>();
        for (int i = 0; i < Math.min(MAX_DIMENSIONS, dimensions.length()); i++) {
            JSONObject dimension = dimensions.optJSONObject(i);
            if (dimension == null) continue;
            String role = dimension.optString("role", "other");
            double inches = dimension.optDouble("value_inches", Double.NaN);
            if ((!"length".equals(role) && !"width".equals(role)) || !positive(inches)) {
                continue;
            }
            Set<String> identifiers = new HashSet<>();
            JSONArray partIds = dimension.optJSONArray("part_ids");
            if (partIds != null) {
                for (int partIndex = 0; partIndex < Math.min(MAX_PART_IDS, partIds.length()); partIndex++) {
                    addIdentifier(identifiers, partIds.optString(partIndex, ""));
                }
            }
            addIdentifier(identifiers, dimension.optString("part_id", ""));
            ArrayList<String> sortedIdentifiers = new ArrayList<>(identifiers);
            Collections.sort(sortedIdentifiers);
            String uniqueTarget = sortedIdentifiers.toString() + "\u0000" + role;
            if (!usedTargets.add(uniqueTarget)) continue;

            double extent = dimensionAxisSpan(dimension);
            int matchingShapes = 0;
            if (!positive(extent)) {
                for (int shapeIndex = 0;
                     shapeIndex < Math.min(MAX_SHAPES, shapes.length());
                     shapeIndex++) {
                    JSONObject shape = shapes.optJSONObject(shapeIndex);
                    if (!matchesShape(shape, identifiers)) continue;
                    Bounds bounds = bounds(points(shape));
                    if (!bounds.valid()) continue;
                    int axis = dimensionAxis(dimension);
                    extent = axis == 0 ? bounds.width() : axis == 1 ? bounds.height() : 0;
                    matchingShapes++;
                }
                if (matchingShapes != 1) extent = 0;
            }
            double scale = extent / inches;
            if (scale >= 0.1 && scale <= 50) scales.add(scale);
        }
        if (scales.isEmpty()) return 0;
        Collections.sort(scales);
        int middle = scales.size() / 2;
        return scales.size() % 2 == 1
                ? scales.get(middle)
                : (scales.get(middle - 1) + scales.get(middle)) / 2;
    }

    /** Resizes a newly drawn rectangle to the drawing's shared scale, anchored at its top-left. */
    public static double[] rectangleBoundsForMeasurements(
            JSONObject drawing,
            double left,
            double top,
            double right,
            double bottom,
            double lengthInches,
            double widthInches) {
        if (drawing == null
                || !finite(left)
                || !finite(top)
                || !finite(right)
                || !finite(bottom)
                || !positive(lengthInches)
                || !positive(widthInches)) return null;
        double originalWidth = Math.abs(right - left);
        double originalHeight = Math.abs(bottom - top);
        if (originalWidth < 5 || originalHeight < 5) return null;

        double pixelsPerInch = estimateDrawingPixelsPerInch(drawing);
        double targetWidth;
        double targetHeight;
        if (positive(pixelsPerInch)) {
            targetWidth = lengthInches * pixelsPerInch;
            targetHeight = widthInches * pixelsPerInch;
        } else {
            double area = originalWidth * originalHeight;
            double aspect = lengthInches / widthInches;
            targetWidth = Math.sqrt(area * aspect);
            targetHeight = Math.sqrt(area / aspect);
        }
        if (!positive(targetWidth) || !positive(targetHeight)) return null;

        double canvasWidth = positiveOr(
                drawing.optDouble("canvas_width", DEFAULT_CANVAS_WIDTH),
                DEFAULT_CANVAS_WIDTH);
        double canvasHeight = positiveOr(
                drawing.optDouble("canvas_height", DEFAULT_CANVAS_HEIGHT),
                DEFAULT_CANVAS_HEIGHT);
        double resultLeft = Math.min(left, right);
        double resultTop = Math.min(top, bottom);
        if (resultLeft < 0
                || resultTop < 0
                || resultLeft + targetWidth > canvasWidth
                || resultTop + targetHeight > canvasHeight) return null;
        return new double[]{
                resultLeft,
                resultTop,
                resultLeft + targetWidth,
                resultTop + targetHeight
        };
    }

    private static Set<String> linkedIdentifiers(JSONObject dimension, JSONArray parts) {
        Set<String> identifiers = new HashSet<>();
        JSONArray partIds = dimension.optJSONArray("part_ids");
        if (partIds != null) {
            for (int i = 0; i < Math.min(MAX_PART_IDS, partIds.length()); i++) {
                addIdentifier(identifiers, partIds.optString(i, ""));
            }
        }
        addIdentifier(identifiers, dimension.optString("part_id", ""));
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

    private static boolean matchesShape(JSONObject shape, Set<String> identifiers) {
        if (shape == null) return false;
        for (String identifier : shapeIdentifiers(shape)) {
            if (identifiers.contains(identifier)) return true;
        }
        return false;
    }

    private static Set<String> shapeIdentifiers(JSONObject shape) {
        Set<String> identifiers = new HashSet<>();
        if (shape == null) return identifiers;
        addIdentifier(identifiers, shape.optString("link_id", ""));
        addIdentifier(identifiers, shape.optString("id", ""));
        return identifiers;
    }

    private static void addIdentifier(Set<String> identifiers, String value) {
        String clean = value == null ? "" : value.trim();
        if (!clean.isEmpty()) identifiers.add(clean);
    }

    private static boolean intersects(Set<String> first, Set<String> second) {
        if (first == null || second == null) return false;
        for (String value : first) {
            if (second.contains(value)) return true;
        }
        return false;
    }

    private static List<JSONObject> linkedDimensions(
            JSONArray dimensions,
            Set<String> identifiers) {
        ArrayList<JSONObject> result = new ArrayList<>();
        if (dimensions == null || identifiers.isEmpty()) return result;
        for (int i = 0; i < Math.min(MAX_DIMENSIONS, dimensions.length()); i++) {
            JSONObject dimension = dimensions.optJSONObject(i);
            if (dimension != null && dimensionMatches(dimension, identifiers)) {
                result.add(dimension);
            }
        }
        return result;
    }

    private static boolean dimensionMatches(JSONObject dimension, Set<String> identifiers) {
        JSONArray partIds = dimension.optJSONArray("part_ids");
        if (partIds != null) {
            for (int i = 0; i < Math.min(MAX_PART_IDS, partIds.length()); i++) {
                if (identifiers.contains(partIds.optString(i, "").trim())) return true;
            }
        }
        return identifiers.contains(dimension.optString("part_id", "").trim());
    }

    private static JSONArray points(JSONObject shape) {
        JSONArray points = shape == null ? null : shape.optJSONArray("points");
        return points == null || points.length() < 3 ? null : points;
    }

    private static void includePoints(Bounds bounds, JSONArray points) {
        if (points == null) return;
        for (int i = 0; i < Math.min(MAX_POINTS, points.length()); i++) {
            JSONObject point = points.optJSONObject(i);
            if (point == null) continue;
            bounds.include(
                    point.optDouble("x", Double.NaN),
                    point.optDouble("y", Double.NaN));
        }
    }

    private static void includeDimension(Bounds bounds, JSONObject dimension) {
        bounds.include(
                dimension.optDouble("x1", Double.NaN),
                dimension.optDouble("y1", Double.NaN));
        bounds.include(
                dimension.optDouble("x2", Double.NaN),
                dimension.optDouble("y2", Double.NaN));
    }

    private static Bounds bounds(JSONArray points) {
        Bounds bounds = new Bounds();
        includePoints(bounds, points);
        return bounds;
    }

    private static void translatePoints(JSONArray points, double deltaX, double deltaY) {
        for (int i = 0; i < Math.min(MAX_POINTS, points.length()); i++) {
            JSONObject point = points.optJSONObject(i);
            if (point == null) continue;
            translateCoordinate(point, "x", deltaX);
            translateCoordinate(point, "y", deltaY);
        }
    }

    private static void translateCoordinate(JSONObject object, String key, double delta) {
        double value = object.optDouble(key, Double.NaN);
        if (!finite(value)) return;
        try {
            object.put(key, value + delta);
        } catch (Exception ignored) {
        }
    }

    private static double clampedDelta(double requested, double minimum, double maximum) {
        if (minimum > maximum) return 0;
        return Math.max(minimum, Math.min(maximum, requested));
    }

    private static boolean contains(JSONArray points, double x, double y) {
        int count = Math.min(MAX_POINTS, points.length());
        boolean inside = false;
        for (int i = 0, j = count - 1; i < count; j = i++) {
            JSONObject current = points.optJSONObject(i);
            JSONObject previous = points.optJSONObject(j);
            if (current == null || previous == null) continue;
            double xi = current.optDouble("x", Double.NaN);
            double yi = current.optDouble("y", Double.NaN);
            double xj = previous.optDouble("x", Double.NaN);
            double yj = previous.optDouble("y", Double.NaN);
            if (!finite(xi) || !finite(yi) || !finite(xj) || !finite(yj)) continue;
            if (pointSegmentDistanceSquared(x, y, xi, yi, xj, yj) <= 0.0001) return true;
            boolean crosses = (yi > y) != (yj > y)
                    && x < (xj - xi) * (y - yi) / (yj - yi) + xi;
            if (crosses) inside = !inside;
        }
        return inside;
    }

    private static double polygonEdgeDistanceSquared(JSONArray points, double x, double y) {
        double result = Double.POSITIVE_INFINITY;
        int count = Math.min(MAX_POINTS, points.length());
        for (int i = 0; i < count; i++) {
            JSONObject a = points.optJSONObject(i);
            JSONObject b = points.optJSONObject((i + 1) % count);
            if (a == null || b == null) continue;
            double candidate = pointSegmentDistanceSquared(
                    x,
                    y,
                    a.optDouble("x", Double.NaN),
                    a.optDouble("y", Double.NaN),
                    b.optDouble("x", Double.NaN),
                    b.optDouble("y", Double.NaN));
            result = Math.min(result, candidate);
        }
        return result;
    }

    private static double pointSegmentDistanceSquared(
            double x,
            double y,
            double x1,
            double y1,
            double x2,
            double y2) {
        if (!finite(x1) || !finite(y1) || !finite(x2) || !finite(y2)) {
            return Double.POSITIVE_INFINITY;
        }
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 0.0001) return distanceSquared(x, y, x1, y1);
        double t = ((x - x1) * dx + (y - y1) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        return distanceSquared(x, y, x1 + t * dx, y1 + t * dy);
    }

    private static double polygonArea(JSONArray points) {
        double area = 0;
        int count = Math.min(MAX_POINTS, points.length());
        for (int i = 0; i < count; i++) {
            JSONObject a = points.optJSONObject(i);
            JSONObject b = points.optJSONObject((i + 1) % count);
            if (a == null || b == null) continue;
            double ax = a.optDouble("x", Double.NaN);
            double ay = a.optDouble("y", Double.NaN);
            double bx = b.optDouble("x", Double.NaN);
            double by = b.optDouble("y", Double.NaN);
            if (!finite(ax) || !finite(ay) || !finite(bx) || !finite(by)) continue;
            area += ax * by - bx * ay;
        }
        return Math.abs(area) / 2;
    }

    private static JSONArray rectanglePoints(Bounds bounds) throws Exception {
        return new JSONArray()
                .put(point(bounds.left, bounds.top))
                .put(point(bounds.right, bounds.top))
                .put(point(bounds.right, bounds.bottom))
                .put(point(bounds.left, bounds.bottom));
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

    private static double positiveOr(double value, double fallback) {
        return positive(value) ? value : fallback;
    }

    private static boolean positive(double value) {
        return finite(value) && value > 0;
    }

    private static boolean nonNegative(double value) {
        return finite(value) && value >= 0;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(distanceSquared(x1, y1, x2, y2));
    }

    private static double distanceSquared(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    public static final class MoveResult {
        public final boolean moved;
        public final double deltaX;
        public final double deltaY;

        private MoveResult(boolean moved, double deltaX, double deltaY) {
            this.moved = moved;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }

        private static MoveResult notMoved() {
            return new MoveResult(false, 0, 0);
        }
    }

    private static final class ResizeMatch {
        final int shapeIndex;
        final boolean ambiguous;

        private ResizeMatch(int shapeIndex, boolean ambiguous) {
            this.shapeIndex = shapeIndex;
            this.ambiguous = ambiguous;
        }

        static ResizeMatch match(int shapeIndex) {
            return new ResizeMatch(shapeIndex, false);
        }

        static ResizeMatch none() {
            return new ResizeMatch(-1, false);
        }

        static ResizeMatch ambiguous() {
            return new ResizeMatch(-1, true);
        }
    }

    private static final class BoundaryMove {
        final double oldStart;
        final double oldTerminal;
        final double delta;

        BoundaryMove(double oldStart, double oldTerminal, double delta) {
            this.oldStart = oldStart;
            this.oldTerminal = oldTerminal;
            this.delta = delta;
        }
    }

    private static final class Point {
        final double x;
        final double y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Bounds {
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;

        void include(double x, double y) {
            if (!finite(x) || !finite(y)) return;
            left = Math.min(left, x);
            top = Math.min(top, y);
            right = Math.max(right, x);
            bottom = Math.max(bottom, y);
        }

        boolean valid() {
            return finite(left) && finite(top) && finite(right) && finite(bottom)
                    && right >= left && bottom >= top;
        }

        double width() {
            return right - left;
        }

        double height() {
            return bottom - top;
        }

        double centerX() {
            return (left + right) / 2;
        }

        double centerY() {
            return (top + bottom) / 2;
        }
    }
}
