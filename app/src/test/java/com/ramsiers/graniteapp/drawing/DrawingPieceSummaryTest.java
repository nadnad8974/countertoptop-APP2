package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

public class DrawingPieceSummaryTest {
    @Test
    public void numbersOnlyPartsThatAffectSquareFootageAndKeepsSigns() throws Exception {
        JSONObject drawing = new JSONObject().put("shapes", new JSONArray()
                .put(shape("counter"))
                .put(shape("sink"))
                .put(shape("stove")));
        JSONArray parts = new JSONArray()
                .put(part("counter", "countertop", "add", 100, 25.5))
                .put(part("sink", "sink", "subtract", 30, 18))
                .put(part("stove", "stove", "subtract", 30, 25.5));

        List<DrawingPieceSummary.Piece> pieces =
                DrawingPieceSummary.summarize(drawing, parts);

        assertEquals(2, pieces.size());
        assertEquals(1, pieces.get(0).number);
        assertEquals(0, pieces.get(0).shapeIndex);
        assertFalse(pieces.get(0).subtract);
        assertEquals(17.71, pieces.get(0).squareFeet, 0.005);
        assertEquals(2, pieces.get(1).number);
        assertEquals(2, pieces.get(1).shapeIndex);
        assertTrue(pieces.get(1).subtract);
        assertEquals("−5.31 sq ft", pieces.get(1).squareFeetText());
        assertEquals(3, DrawingPieceSummary.nextNumber(parts));
    }

    @Test
    public void labelLinesUseCenteredThreeLineFormatForAddedPiece() throws Exception {
        List<DrawingPieceSummary.Piece> pieces = DrawingPieceSummary.summarize(
                new JSONObject().put("shapes", new JSONArray().put(shape("counter"))),
                new JSONArray().put(part("counter", "countertop", "add", 100, 25.5)));

        assertArrayEquals(
                new String[]{"#1", "100 × 25.5 in", "17.71 sq ft"},
                pieces.get(0).labelLines());
        assertEquals("100\" × 25.5\"", pieces.get(0).dimensionsText());
    }

    @Test
    public void labelLinesKeepDeductionSignAndTrimDimensionDecimals() throws Exception {
        List<DrawingPieceSummary.Piece> pieces = DrawingPieceSummary.summarize(
                new JSONObject().put("shapes", new JSONArray().put(shape("stove"))),
                new JSONArray().put(part("stove", "stove", "subtract", 30, 25.50)));

        assertArrayEquals(
                new String[]{"#1", "30 × 25.5 in", "−5.31 sq ft"},
                pieces.get(0).labelLines());
    }

    @Test
    public void findsPieceDimensionsByStableLink() throws Exception {
        JSONObject drawing = new JSONObject()
                .put("shapes", new JSONArray().put(shape("piece")))
                .put("dimensions", new JSONArray()
                        .put(dimension("piece", "width"))
                        .put(dimension("piece", "length")));
        List<DrawingPieceSummary.Piece> pieces = DrawingPieceSummary.summarize(
                drawing,
                new JSONArray().put(part("piece", "countertop", "add", 72, 36)));

        assertEquals(1, pieces.size());
        assertEquals(1, DrawingPieceSummary.firstDimensionIndex(
                drawing, pieces.get(0), "length", new HashSet<>()));
        assertEquals(0, DrawingPieceSummary.firstDimensionIndex(
                drawing, pieces.get(0), "width", new HashSet<>()));
        assertEquals(pieces.get(0), DrawingPieceSummary.forDimension(
                pieces,
                drawing.getJSONArray("dimensions").getJSONObject(1)));
    }

    @Test
    public void exactLinkWinsOverConflictingIdsAndUnlinkedPartsRemainNumbered() throws Exception {
        JSONObject wrongById = shape("shared").put("link_id", "wrong-link");
        JSONObject correctByLink = shape("correct").put("link_id", "piece-link");
        JSONObject linkedPart = part("shared", "countertop", "add", 50, 25)
                .put("link_id", "piece-link");
        JSONObject unlinkedPart = part("formula-only", "countertop", "add", 20, 10);
        unlinkedPart.remove("link_id");

        List<DrawingPieceSummary.Piece> pieces = DrawingPieceSummary.summarize(
                new JSONObject().put("shapes", new JSONArray()
                        .put(wrongById)
                        .put(correctByLink)),
                new JSONArray().put(linkedPart).put(unlinkedPart));

        assertEquals(2, pieces.size());
        assertEquals(1, pieces.get(0).shapeIndex);
        assertEquals(1, pieces.get(0).number);
        assertEquals(-1, pieces.get(1).shapeIndex);
        assertEquals(2, pieces.get(1).number);
    }

    @Test
    public void legacyUnlinkedLShapeGetsFourPhysicalPiecesWithoutNumberingOverlap() throws Exception {
        JSONArray parts = new JSONArray()
                .put(unlinkedPart("P0", "other", "add", 100, 25.5))
                .put(unlinkedPart("P1", "other", "add", 45, 25.5))
                .put(unlinkedPart("P2", "other", "subtract", 25.5, 25.5))
                .put(unlinkedPart("P3", "countertop", "add", 102, 45))
                .put(part("P4", "other", "add", 25.5, 10));
        JSONObject drawing = new JSONObject()
                .put("shapes", new JSONArray()
                        .put(shapeWithPoints("S0", new double[][]{
                                {95, 120}, {535, 105}, {540, 245},
                                {235, 270}, {235, 450}, {95, 465}
                        }))
                        .put(shapeWithPoints("S1", new double[][]{
                                {370, 420}, {715, 405}, {720, 610}, {365, 625}
                        }))
                        .put(shapeWithPoints("P4", new double[][]{
                                {675, 95}, {735, 95}, {745, 205}, {680, 220}
                        })))
                .put("dimensions", new JSONArray()
                        .put(dimension(new String[]{"P0"}, "length", 100, 100, 80, 535, 65))
                        .put(dimension(new String[]{"P1"}, "length", 45, 60, 120, 60, 465))
                        .put(dimension(
                                new String[]{"P0", "P1", "P2"},
                                "both",
                                25.5,
                                95,
                                500,
                                235,
                                490))
                        .put(dimension(new String[]{"P3"}, "length", 102, 370, 390, 715, 375))
                        .put(dimension(new String[]{"P3"}, "width", 45, 750, 405, 755, 610))
                        .put(dimension(new String[]{"P4"}, "width", 10, 675, 70, 735, 70))
                        .put(dimension(new String[]{"P4"}, "length", 25.5, 765, 95, 780, 220)));

        List<DrawingPieceSummary.Piece> pieces =
                DrawingPieceSummary.summarize(drawing, parts);

        assertEquals(4, pieces.size());
        assertArrayEquals(
                new String[]{"#1", "100 × 25.5 in", "17.71 sq ft"},
                pieces.get(0).labelLines());
        assertEquals(0, pieces.get(0).shapeIndex);
        assertEquals(0, pieces.get(0).preferredDimensionIndex);
        assertArrayEquals(
                new String[]{"#2", "19.5 × 25.5 in", "3.45 sq ft"},
                pieces.get(1).labelLines());
        assertEquals(0, pieces.get(1).shapeIndex);
        assertEquals(1, pieces.get(1).preferredDimensionIndex);
        assertArrayEquals(
                new String[]{"#3", "102 × 45 in", "31.88 sq ft"},
                pieces.get(2).labelLines());
        assertEquals(1, pieces.get(2).shapeIndex);
        assertArrayEquals(
                new String[]{"#4", "25.5 × 10 in", "1.77 sq ft"},
                pieces.get(3).labelLines());
        assertEquals(2, pieces.get(3).shapeIndex);
        assertEquals(54.81, DrawingMath.squareFeet(parts), 0.005);
        double displayedTotal = 0;
        for (DrawingPieceSummary.Piece piece : pieces) {
            displayedTotal += piece.subtract ? -piece.squareFeet : piece.squareFeet;
        }
        assertEquals(DrawingMath.squareFeet(parts), displayedTotal, 0.005);
        assertEquals(5, DrawingPieceSummary.nextNumber(drawing, parts));
    }

    @Test
    public void unrelatedSquareDeductionOnDifferentShapesIsNotFoldedIntoAPiece()
            throws Exception {
        JSONArray parts = new JSONArray()
                .put(part("A", "countertop", "add", 60, 20))
                .put(part("B", "countertop", "add", 40, 20))
                .put(unlinkedPart("deduct", "other", "subtract", 20, 20));
        JSONObject drawing = new JSONObject()
                .put("shapes", new JSONArray()
                        .put(shapeWithPoints("A", new double[][]{
                                {50, 50}, {350, 50}, {350, 150},
                                {250, 150}, {250, 250}, {50, 250}
                        }))
                        .put(shapeWithPoints("B", new double[][]{
                                {500, 50}, {700, 50}, {700, 150},
                                {650, 150}, {650, 250}, {500, 250}
                        })))
                .put("dimensions", new JSONArray().put(dimension(
                        new String[]{"A", "B", "deduct"},
                        "both",
                        20,
                        50,
                        300,
                        250,
                        300)));

        List<DrawingPieceSummary.Piece> pieces =
                DrawingPieceSummary.summarize(drawing, parts);

        assertEquals(3, pieces.size());
        assertFalse(pieces.get(0).subtract);
        assertFalse(pieces.get(1).subtract);
        assertTrue(pieces.get(2).subtract);
        assertArrayEquals(
                new String[]{"#3", "20 × 20 in", "−2.78 sq ft"},
                pieces.get(2).labelLines());
    }

    private static JSONObject part(
            String id,
            String feature,
            String operation,
            double length,
            double width) throws Exception {
        return new JSONObject()
                .put("id", id)
                .put("link_id", id)
                .put("feature_type", feature)
                .put("operation", operation)
                .put("length_inches", length)
                .put("width_inches", width)
                .put("quantity", 1);
    }

    private static JSONObject shape(String id) throws Exception {
        return new JSONObject()
                .put("id", id)
                .put("link_id", id)
                .put("kind", "countertop")
                .put("points", new JSONArray()
                        .put(point(0, 0))
                        .put(point(100, 0))
                        .put(point(100, 50))
                        .put(point(0, 50)));
    }

    private static JSONObject shapeWithPoints(String linkId, double[][] coordinates)
            throws Exception {
        JSONArray points = new JSONArray();
        for (double[] coordinate : coordinates) {
            points.put(point(coordinate[0], coordinate[1]));
        }
        return new JSONObject()
                .put("id", "shape-" + linkId)
                .put("link_id", linkId)
                .put("kind", "countertop")
                .put("feature_type", "countertop")
                .put("points", points);
    }

    private static JSONObject unlinkedPart(
            String id,
            String feature,
            String operation,
            double length,
            double width) throws Exception {
        JSONObject result = part(id, feature, operation, length, width);
        result.remove("link_id");
        return result;
    }

    private static JSONObject dimension(String id, String role) throws Exception {
        return new JSONObject()
                .put("part_ids", new JSONArray().put(id))
                .put("role", role)
                .put("value_inches", 10);
    }

    private static JSONObject dimension(
            String[] partIds,
            String role,
            double value,
            double x1,
            double y1,
            double x2,
            double y2) throws Exception {
        JSONArray identifiers = new JSONArray();
        for (String partId : partIds) identifiers.put(partId);
        return new JSONObject()
                .put("part_ids", identifiers)
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
