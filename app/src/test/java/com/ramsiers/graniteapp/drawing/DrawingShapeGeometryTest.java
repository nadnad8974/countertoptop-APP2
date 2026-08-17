package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DrawingShapeGeometryTest {
    @Test
    public void snapsNearRectangleButPreservesLAndAngledShapes() throws Exception {
        JSONObject nearRectangle = shape(
                "near",
                points(10, 10, 110, 12, 108, 60, 12, 58));
        assertTrue(DrawingShapeGeometry.snapNearAxisAlignedRectangle(nearRectangle));
        assertRectangle(nearRectangle, 10, 10, 110, 60);

        JSONObject lShape = shape(
                "ell",
                points(10, 10, 100, 10, 100, 40, 40, 40, 40, 100, 10, 100));
        String originalLPoints = lShape.getJSONArray("points").toString();
        assertFalse(DrawingShapeGeometry.snapNearAxisAlignedRectangle(lShape));
        assertEquals(originalLPoints, lShape.getJSONArray("points").toString());

        JSONObject angled = shape(
                "angled",
                points(50, 10, 100, 50, 50, 90, 0, 50));
        String originalAngledPoints = angled.getJSONArray("points").toString();
        assertFalse(DrawingShapeGeometry.snapNearAxisAlignedRectangle(angled));
        assertEquals(originalAngledPoints, angled.getJSONArray("points").toString());
    }

    @Test
    public void orthogonalizesSkewedRectangleAndLShapeAndCardinalizesArrows() throws Exception {
        JSONObject rectangle = shape(
                "rectangle",
                points(10, 12, 111, 4, 116, 61, 8, 66));
        JSONObject ell = shape(
                "ell",
                points(150, 100, 401, 88, 405, 151, 250, 158, 247, 300, 142, 305));
        JSONObject length = dimension("rectangle", "length", 100, 10, 2, 111, -4);
        JSONObject width = dimension("rectangle", "width", 50, 2, 12, 8, 66);
        JSONObject drawing = new JSONObject()
                .put("shapes", new JSONArray().put(rectangle).put(ell))
                .put("dimensions", new JSONArray().put(length).put(width));

        int changed = DrawingShapeGeometry.orthogonalizeDrawing(drawing);

        assertTrue(changed >= 4);
        assertAxisAlignedRectangle(rectangle);
        assertOrthogonalPolygon(ell);
        assertEquals(6, ell.getJSONArray("points").length());
        assertEquals(length.getDouble("y1"), length.getDouble("y2"), 0.001);
        assertEquals(width.getDouble("x1"), width.getDouble("x2"), 0.001);
        String once = drawing.toString();
        DrawingShapeGeometry.orthogonalizeDrawing(drawing);
        assertEquals(once, drawing.toString());
    }

    @Test
    public void preservesGenuinelyDiagonalMultiCornerShape() throws Exception {
        JSONObject angled = shape(
                "angled",
                points(50, 0, 100, 50, 90, 110, 40, 150, 0, 100, 10, 40));
        String original = angled.getJSONArray("points").toString();

        assertFalse(DrawingShapeGeometry.orthogonalizeShape(angled));
        assertEquals(original, angled.getJSONArray("points").toString());
    }

    @Test
    public void preservesGenuinelyAngledFourCornerShape() throws Exception {
        JSONObject angled = shape(
                "angled-four",
                points(50, 10, 120, 70, 70, 140, 0, 80));
        String original = angled.getJSONArray("points").toString();

        assertFalse(DrawingShapeGeometry.orthogonalizeShape(angled));
        assertEquals(original, angled.getJSONArray("points").toString());
    }

    @Test
    public void cardinalizesArrowsAlongTheirExistingDirectionWithoutCollapsingThem() throws Exception {
        JSONObject verticalLength = dimension("piece", "length", 45, 10, 10, 12, 110);
        JSONObject horizontalWidth = dimension("piece", "width", 25, 20, 40, 120, 42);
        JSONObject drawing = new JSONObject()
                .put("shapes", new JSONArray())
                .put("dimensions", new JSONArray().put(verticalLength).put(horizontalWidth));

        DrawingShapeGeometry.orthogonalizeDrawing(drawing);

        assertEquals(verticalLength.getDouble("x1"), verticalLength.getDouble("x2"), 0.001);
        assertTrue(Math.abs(verticalLength.getDouble("y2") - verticalLength.getDouble("y1")) > 90);
        assertEquals(horizontalWidth.getDouble("y1"), horizontalWidth.getDouble("y2"), 0.001);
        assertTrue(Math.abs(horizontalWidth.getDouble("x2") - horizontalWidth.getDouble("x1")) > 90);
    }

    @Test
    public void exactAndNearHitTestingSelectThinShapesReliably() throws Exception {
        JSONArray shapes = new JSONArray()
                .put(shape("large", points(0, 0, 400, 0, 400, 100, 0, 100)))
                .put(shape("thin", points(100, 100, 300, 100, 300, 110, 100, 110)));

        assertEquals(1, DrawingShapeGeometry.findContainingShape(shapes, 150, 105));
        assertEquals(1, DrawingShapeGeometry.findNearestShape(shapes, 150, 125, 20));
        assertEquals(1, DrawingShapeGeometry.findShapeAtOrNear(shapes, 150, 125, 20));
        assertEquals(-1, DrawingShapeGeometry.findShapeAtOrNear(shapes, 500, 500, 20));
    }

    @Test
    public void movementUsesOneClampedDeltaForShapeAndLinkedDimensions() throws Exception {
        JSONObject selected = shape("piece", points(10, 20, 40, 20, 40, 40, 10, 40));
        JSONObject linked = dimension("piece", "length", 30, 10, 10, 40, 10);
        JSONObject unrelated = dimension("other", "length", 20, 60, 60, 80, 60);
        JSONObject drawing = new JSONObject()
                .put("canvas_width", 100)
                .put("canvas_height", 100)
                .put("shapes", new JSONArray().put(selected))
                .put("dimensions", new JSONArray().put(linked).put(unrelated));

        DrawingShapeGeometry.MoveResult result =
                DrawingShapeGeometry.moveShapeAndLinkedDimensions(drawing, 0, -50, 100);

        assertTrue(result.moved);
        assertEquals(-10, result.deltaX, 0.001);
        assertEquals(60, result.deltaY, 0.001);
        assertRectangle(selected, 0, 80, 30, 100);
        assertEquals(0, linked.getDouble("x1"), 0.001);
        assertEquals(70, linked.getDouble("y1"), 0.001);
        assertEquals(60, unrelated.getDouble("x1"), 0.001);
        assertEquals(60, unrelated.getDouble("y1"), 0.001);
    }

    @Test
    public void baselineResizeIsRectangularCardinalAndDoesNotCompound() throws Exception {
        JSONObject shape = shape(
                "piece",
                points(100, 100, 300, 102, 299, 200, 101, 198));
        JSONObject length = dimension("piece", "length", 50, 100, 70, 300, 73);
        JSONObject width = dimension("piece", "width", 25, 75, 100, 75, 200);
        JSONObject baseline = new JSONObject()
                .put("shapes", new JSONArray().put(shape))
                .put("dimensions", new JSONArray().put(length).put(width));
        JSONArray parts = new JSONArray().put(new JSONObject()
                .put("id", "piece")
                .put("link_id", "piece"));
        Map<Integer, Double> edits = new HashMap<>();
        edits.put(0, 75d);

        JSONObject first = DrawingShapeGeometry.rebuildLinkedGeometryFromBaseline(
                baseline, parts, edits);
        JSONObject second = DrawingShapeGeometry.rebuildLinkedGeometryFromBaseline(
                baseline, parts, edits);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(300, width(first.getJSONArray("shapes").getJSONObject(0)), 0.001);
        assertEquals(300, width(second.getJSONArray("shapes").getJSONObject(0)), 0.001);
        assertEquals(100, height(first.getJSONArray("shapes").getJSONObject(0)), 0.001);
        assertRectangle(first.getJSONArray("shapes").getJSONObject(0), 100, 100, 400, 200);
        assertEquals(100, first.getJSONArray("dimensions")
                .getJSONObject(0).getDouble("x1"), 0.001);
        assertEquals(400, first.getJSONArray("dimensions")
                .getJSONObject(0).getDouble("x2"), 0.001);
        assertEquals(75, first.getJSONArray("dimensions")
                .getJSONObject(1).getDouble("x1"), 0.001);
        assertAxisAlignedRectangle(first.getJSONArray("shapes").getJSONObject(0));
        assertEquals(200, width(shape), 0.001);
    }

    @Test
    public void verticalArrowKeepsTopFixedEvenWhenFormulaRoleIsLength() throws Exception {
        JSONObject baselineShape = shape(
                "piece",
                points(100, 100, 150, 100, 150, 150, 100, 150));
        JSONObject verticalLength = dimension("piece", "length", 50, 180, 100, 180, 150);
        JSONObject horizontalWidth = dimension("piece", "width", 50, 100, 80, 150, 80);
        JSONObject baseline = new JSONObject()
                .put("canvas_width", 500)
                .put("canvas_height", 500)
                .put("shapes", new JSONArray().put(baselineShape))
                .put("dimensions", new JSONArray().put(verticalLength).put(horizontalWidth));
        Map<Integer, Double> edits = new HashMap<>();
        edits.put(0, 75d);

        JSONObject resized = DrawingShapeGeometry.rebuildLinkedGeometryFromBaseline(
                baseline,
                new JSONArray().put(new JSONObject().put("id", "piece")),
                edits);

        assertNotNull(resized);
        assertRectangle(
                resized.getJSONArray("shapes").getJSONObject(0),
                100,
                100,
                150,
                175);
        assertEquals(100, resized.getJSONArray("dimensions")
                .getJSONObject(0).getDouble("y1"), 0.001);
        assertEquals(175, resized.getJSONArray("dimensions")
                .getJSONObject(0).getDouble("y2"), 0.001);
        assertEquals(horizontalWidth.toString(),
                resized.getJSONArray("dimensions").getJSONObject(1).toString());
    }

    @Test
    public void legacyUnlinkedLResizesOnlyTheArrowEndArmWithoutCreatingALink()
            throws Exception {
        JSONObject ell = shape(
                "S0",
                points(100, 100, 200, 100, 200, 125, 125, 125, 125, 200, 100, 200));
        JSONObject other = shape(
                "S1",
                points(500, 300, 650, 300, 650, 400, 500, 400));
        JSONObject topLength = dimension("P0", "length", 50, 100, 80, 200, 80);
        JSONObject legLength = dimension("P1", "length", 75, 80, 100, 80, 200);
        JSONObject sharedDepth = new JSONObject()
                .put("part_ids", new JSONArray().put("P0").put("P1").put("P2"))
                .put("role", "both")
                .put("value_inches", 25)
                .put("x1", 100)
                .put("y1", 220)
                .put("x2", 125)
                .put("y2", 220);
        JSONObject baseline = new JSONObject()
                .put("canvas_width", 1000)
                .put("canvas_height", 700)
                .put("shapes", new JSONArray().put(ell).put(other))
                .put("dimensions", new JSONArray()
                        .put(topLength)
                        .put(legLength)
                        .put(sharedDepth));
        JSONArray parts = new JSONArray()
                .put(new JSONObject()
                        .put("id", "P0")
                        .put("feature_type", "other")
                        .put("operation", "add")
                        .put("length_inches", 50)
                        .put("width_inches", 25))
                .put(new JSONObject()
                        .put("id", "P1")
                        .put("feature_type", "other")
                        .put("operation", "add")
                        .put("length_inches", 75)
                        .put("width_inches", 25))
                .put(new JSONObject()
                        .put("id", "P2")
                        .put("feature_type", "other")
                        .put("operation", "subtract")
                        .put("length_inches", 25)
                        .put("width_inches", 25));
        Map<Integer, Double> edits = new HashMap<>();
        edits.put(0, 75d);

        JSONObject resized = DrawingShapeGeometry.rebuildLinkedGeometryFromBaseline(
                baseline,
                parts,
                edits);

        assertNotNull(resized);
        JSONObject resizedEll = resized.getJSONArray("shapes").getJSONObject(0);
        assertEquals(150, width(resizedEll), 0.001);
        assertEquals(100, resizedEll.getJSONArray("points")
                .getJSONObject(0).getDouble("x"), 0.001);
        assertEquals(250, resizedEll.getJSONArray("points")
                .getJSONObject(1).getDouble("x"), 0.001);
        assertEquals(250, resizedEll.getJSONArray("points")
                .getJSONObject(2).getDouble("x"), 0.001);
        assertEquals(125, resizedEll.getJSONArray("points")
                .getJSONObject(3).getDouble("x"), 0.001);
        assertEquals(legLength.toString(),
                resized.getJSONArray("dimensions").getJSONObject(1).toString());
        assertEquals(sharedDepth.toString(),
                resized.getJSONArray("dimensions").getJSONObject(2).toString());
        assertEquals("S0", resizedEll.getString("link_id"));
    }

    @Test
    public void ambiguousLegacyShapesAreNotMoved() throws Exception {
        JSONObject upper = shape(
                "upper",
                points(0, 30, 50, 30, 50, 80, 0, 80));
        JSONObject lower = shape(
                "lower",
                points(0, 120, 50, 120, 50, 170, 0, 170));
        JSONObject guide = dimension("legacy", "length", 50, 0, 100, 50, 100);
        JSONObject baseline = new JSONObject()
                .put("shapes", new JSONArray().put(upper).put(lower))
                .put("dimensions", new JSONArray().put(guide));
        JSONArray parts = new JSONArray().put(new JSONObject()
                .put("id", "legacy")
                .put("feature_type", "other")
                .put("operation", "add")
                .put("length_inches", 50)
                .put("width_inches", 25));
        Map<Integer, Double> edits = new HashMap<>();
        edits.put(0, 75d);
        String original = baseline.toString();

        assertNull(DrawingShapeGeometry.rebuildLinkedGeometryFromBaseline(
                baseline,
                parts,
                edits));
        assertEquals(original, baseline.toString());
    }

    @Test
    public void baselineResizeComposesLengthAndWidthAndScalesDuplicateTargetOnce() throws Exception {
        JSONObject shape = shape("piece", points(100, 100, 300, 100, 300, 200, 100, 200));
        JSONObject firstLength = dimension("piece", "length", 50, 100, 70, 300, 70);
        JSONObject duplicateLength = dimension("piece", "length", 50, 100, 230, 300, 230);
        JSONObject width = dimension("piece", "width", 25, 70, 100, 70, 200);
        JSONObject baseline = new JSONObject()
                .put("shapes", new JSONArray().put(shape))
                .put("dimensions", new JSONArray()
                        .put(firstLength)
                        .put(duplicateLength)
                        .put(width));
        JSONArray parts = new JSONArray().put(new JSONObject()
                .put("id", "piece")
                .put("link_id", "piece"));
        Map<Integer, Double> edits = new HashMap<>();
        edits.put(0, 75d);
        edits.put(1, 75d);
        edits.put(2, 50d);

        JSONObject resized = DrawingShapeGeometry.rebuildLinkedGeometryFromBaseline(
                baseline, parts, edits);

        assertNotNull(resized);
        JSONObject resizedShape = resized.getJSONArray("shapes").getJSONObject(0);
        assertEquals(300, width(resizedShape), 0.001);
        assertEquals(200, height(resizedShape), 0.001);
        assertEquals(75, resized.getJSONArray("dimensions")
                .getJSONObject(1).getDouble("value_inches"), 0.001);
    }

    @Test
    public void conflictingDuplicateTargetsAreRejected() throws Exception {
        JSONObject baseline = new JSONObject()
                .put("shapes", new JSONArray().put(
                        shape("piece", points(0, 0, 100, 0, 100, 50, 0, 50))))
                .put("dimensions", new JSONArray()
                        .put(dimension("piece", "length", 50, 0, 0, 100, 0))
                        .put(dimension("piece", "length", 50, 0, 50, 100, 50)));
        Map<Integer, Double> edits = new HashMap<>();
        edits.put(0, 75d);
        edits.put(1, 80d);

        assertNull(DrawingShapeGeometry.rebuildLinkedGeometryFromBaseline(
                baseline,
                new JSONArray().put(new JSONObject().put("id", "piece")),
                edits));
    }

    @Test
    public void editedPieceUsesItsImmutableBaselineScale() throws Exception {
        JSONObject referenceOne = shape(
                "reference_1",
                points(0, 0, 300, 0, 300, 60, 0, 60));
        JSONObject referenceTwo = shape(
                "reference_2",
                points(0, 100, 300, 100, 300, 160, 0, 160));
        JSONObject edited = shape(
                "edited",
                points(400, 0, 500, 0, 500, 60, 400, 60));
        JSONObject drawing = new JSONObject()
                .put("shapes", new JSONArray()
                        .put(referenceOne)
                        .put(referenceTwo)
                        .put(edited))
                .put("dimensions", new JSONArray()
                        .put(dimension("reference_1", "length", 100, 0, 0, 300, 0))
                        .put(dimension("reference_2", "length", 100, 0, 100, 300, 100))
                        .put(dimension("edited", "length", 50, 400, 0, 500, 0)));
        JSONArray parts = new JSONArray()
                .put(new JSONObject().put("id", "reference_1"))
                .put(new JSONObject().put("id", "reference_2"))
                .put(new JSONObject().put("id", "edited"));
        Map<Integer, Double> edits = new HashMap<>();
        edits.put(2, 75d);

        JSONObject resized = DrawingShapeGeometry.rebuildLinkedGeometryFromBaseline(
                drawing,
                parts,
                edits);

        assertNotNull(resized);
        assertEquals(
                150,
                width(resized.getJSONArray("shapes").getJSONObject(2)),
                0.001);
        assertEquals(400, centerX(resized.getJSONArray("shapes").getJSONObject(2))
                - width(resized.getJSONArray("shapes").getJSONObject(2)) / 2, 0.001);
        assertEquals(
                300,
                width(resized.getJSONArray("shapes").getJSONObject(0)),
                0.001);
    }

    @Test
    public void newRectangleUsesDrawingScaleAndRequestedAspect() throws Exception {
        JSONObject reference = shape(
                "reference",
                points(0, 0, 300, 0, 300, 75, 0, 75));
        JSONObject drawing = new JSONObject()
                .put("canvas_width", 1000)
                .put("canvas_height", 700)
                .put("shapes", new JSONArray().put(reference))
                .put("dimensions", new JSONArray()
                        .put(dimension("reference", "length", 100, 0, 0, 300, 0))
                        .put(dimension("reference", "width", 25, 0, 0, 0, 75)));

        double[] result = DrawingShapeGeometry.rectangleBoundsForMeasurements(
                drawing,
                400,
                300,
                500,
                400,
                50,
                20);

        assertNotNull(result);
        assertEquals(150, result[2] - result[0], 0.001);
        assertEquals(60, result[3] - result[1], 0.001);
        assertEquals(400, result[0], 0.001);
        assertEquals(300, result[1], 0.001);
        assertEquals(550, result[2], 0.001);
        assertEquals(360, result[3], 0.001);
        assertNull(DrawingShapeGeometry.rectangleBoundsForMeasurements(
                drawing,
                900,
                300,
                950,
                350,
                50,
                20));
    }

    @Test
    public void selectedShapeInfersMissingLengthFromKnownWidthScale() throws Exception {
        JSONObject baselineShape = shape(
                "piece",
                points(100, 100, 300, 102, 299, 200, 101, 198));
        JSONObject length = dimension("piece", "length", 0, 100, 70, 300, 72);
        JSONObject width = dimension("piece", "width", 25, 75, 100, 75, 200);
        JSONObject baseline = new JSONObject()
                .put("shapes", new JSONArray().put(baselineShape))
                .put("dimensions", new JSONArray().put(length).put(width));

        JSONObject resized = DrawingShapeGeometry.rebuildMeasuredShapeFromBaseline(
                baseline,
                0,
                0,
                25,
                75,
                25);

        assertNotNull(resized);
        JSONObject resizedShape = resized.getJSONArray("shapes").getJSONObject(0);
        assertEquals(300, width(resizedShape), 0.001);
        assertEquals(100, height(resizedShape), 0.001);
        assertRectangle(resizedShape, 100, 100, 400, 200);
        assertAxisAlignedRectangle(resizedShape);
        assertEquals(75, resized.getJSONArray("dimensions")
                .getJSONObject(0).getDouble("value_inches"), 0.001);
        assertEquals(200, width(baselineShape), 0.001);
    }

    @Test
    public void selectedShapeInfersMissingWidthFromKnownLengthScale() throws Exception {
        JSONObject baseline = new JSONObject()
                .put("shapes", new JSONArray().put(
                        shape("piece", points(100, 100, 300, 100, 300, 200, 100, 200))))
                .put("dimensions", new JSONArray()
                        .put(dimension("piece", "length", 50, 100, 70, 300, 70))
                        .put(dimension("piece", "width", 0, 70, 100, 70, 200)));

        JSONObject resized = DrawingShapeGeometry.rebuildMeasuredShapeFromBaseline(
                baseline,
                0,
                50,
                0,
                50,
                50);

        assertNotNull(resized);
        JSONObject resizedShape = resized.getJSONArray("shapes").getJSONObject(0);
        assertEquals(200, width(resizedShape), 0.001);
        assertEquals(200, height(resizedShape), 0.001);
        assertRectangle(resizedShape, 100, 100, 300, 300);
        assertEquals(50, resized.getJSONArray("dimensions")
                .getJSONObject(1).getDouble("value_inches"), 0.001);
    }

    @Test
    public void fullyUnmeasuredLIsRejectedInsteadOfGuessingAnArm() throws Exception {
        JSONObject baselineShape = shape(
                "ell",
                points(100, 100, 300, 100, 300, 150, 150, 150, 150, 300, 100, 300));
        JSONObject baseline = new JSONObject()
                .put("shapes", new JSONArray().put(baselineShape))
                .put("dimensions", new JSONArray());
        String original = baseline.toString();

        JSONObject resized = DrawingShapeGeometry.rebuildMeasuredShapeFromBaseline(
                baseline,
                0,
                0,
                0,
                100,
                25);

        assertNull(resized);
        assertEquals(original, baseline.toString());
    }

    private static JSONObject shape(String id, JSONArray points) throws Exception {
        return new JSONObject()
                .put("id", id)
                .put("link_id", id)
                .put("kind", "countertop")
                .put("points", points);
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
                .put("label", value + "\"")
                .put("x1", x1)
                .put("y1", y1)
                .put("x2", x2)
                .put("y2", y2);
    }

    private static JSONArray points(double... coordinates) throws Exception {
        JSONArray result = new JSONArray();
        for (int i = 0; i < coordinates.length; i += 2) {
            result.put(new JSONObject()
                    .put("x", coordinates[i])
                    .put("y", coordinates[i + 1]));
        }
        return result;
    }

    private static void assertRectangle(
            JSONObject shape,
            double left,
            double top,
            double right,
            double bottom) throws Exception {
        JSONArray points = shape.getJSONArray("points");
        assertEquals(left, points.getJSONObject(0).getDouble("x"), 0.001);
        assertEquals(top, points.getJSONObject(0).getDouble("y"), 0.001);
        assertEquals(right, points.getJSONObject(1).getDouble("x"), 0.001);
        assertEquals(top, points.getJSONObject(1).getDouble("y"), 0.001);
        assertEquals(right, points.getJSONObject(2).getDouble("x"), 0.001);
        assertEquals(bottom, points.getJSONObject(2).getDouble("y"), 0.001);
        assertEquals(left, points.getJSONObject(3).getDouble("x"), 0.001);
        assertEquals(bottom, points.getJSONObject(3).getDouble("y"), 0.001);
    }

    private static void assertAxisAlignedRectangle(JSONObject shape) throws Exception {
        JSONArray points = shape.getJSONArray("points");
        assertEquals(points.getJSONObject(0).getDouble("y"),
                points.getJSONObject(1).getDouble("y"), 0.001);
        assertEquals(points.getJSONObject(1).getDouble("x"),
                points.getJSONObject(2).getDouble("x"), 0.001);
        assertEquals(points.getJSONObject(2).getDouble("y"),
                points.getJSONObject(3).getDouble("y"), 0.001);
        assertEquals(points.getJSONObject(3).getDouble("x"),
                points.getJSONObject(0).getDouble("x"), 0.001);
    }

    private static void assertOrthogonalPolygon(JSONObject shape) throws Exception {
        JSONArray points = shape.getJSONArray("points");
        for (int i = 0; i < points.length(); i++) {
            JSONObject a = points.getJSONObject(i);
            JSONObject b = points.getJSONObject((i + 1) % points.length());
            boolean horizontal = Math.abs(a.getDouble("y") - b.getDouble("y")) < 0.001;
            boolean vertical = Math.abs(a.getDouble("x") - b.getDouble("x")) < 0.001;
            assertTrue(horizontal ^ vertical);
        }
    }

    private static double width(JSONObject shape) {
        JSONArray points = shape.optJSONArray("points");
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < points.length(); i++) {
            double x = points.optJSONObject(i).optDouble("x");
            minimum = Math.min(minimum, x);
            maximum = Math.max(maximum, x);
        }
        return maximum - minimum;
    }

    private static double height(JSONObject shape) {
        JSONArray points = shape.optJSONArray("points");
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < points.length(); i++) {
            double y = points.optJSONObject(i).optDouble("y");
            minimum = Math.min(minimum, y);
            maximum = Math.max(maximum, y);
        }
        return maximum - minimum;
    }

    private static double centerX(JSONObject shape) {
        JSONArray points = shape.optJSONArray("points");
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < points.length(); i++) {
            double x = points.optJSONObject(i).optDouble("x");
            minimum = Math.min(minimum, x);
            maximum = Math.max(maximum, x);
        }
        return (minimum + maximum) / 2;
    }

}
