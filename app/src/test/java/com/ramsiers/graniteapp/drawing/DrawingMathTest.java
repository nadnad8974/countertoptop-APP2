package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class DrawingMathTest {
    @Test
    public void rectangleConvertsInchesToSquareFeet() throws Exception {
        JSONArray parts = new JSONArray().put(part("counter", "add", 120, 25, 1));

        assertEquals(20.83, DrawingMath.squareFeet(parts), 0.001);
    }

    @Test
    public void multiplePiecesAreSummed() throws Exception {
        JSONArray parts = new JSONArray()
                .put(part("countertop", "add", 120, 24, 1))
                .put(part("countertop", "add", 60, 24, 2));

        assertEquals(40.0, DrawingMath.squareFeet(parts), 0.001);
    }

    @Test
    public void backsplashMaterialIsIncluded() throws Exception {
        JSONArray parts = new JSONArray()
                .put(part("countertop", "add", 120, 24, 1))
                .put(part("backsplash", "add", 120, 4, 1));

        assertEquals(23.33, DrawingMath.squareFeet(parts), 0.001);
    }

    @Test
    public void stoveOpeningIsDeducted() throws Exception {
        JSONArray parts = new JSONArray()
                .put(part("countertop", "add", 120, 30, 1))
                .put(part("stove", "add", 30, 24, 1));

        assertEquals(20.0, DrawingMath.squareFeet(parts), 0.001);
    }

    @Test
    public void sinkCutoutIsNeverDeducted() throws Exception {
        JSONArray parts = new JSONArray()
                .put(part("countertop", "add", 120, 30, 1))
                .put(part("sink", "subtract", 30, 18, 1));

        assertEquals(25.0, DrawingMath.squareFeet(parts), 0.001);
    }

    @Test
    public void cooktopCutoutIsNeverDeducted() throws Exception {
        JSONArray parts = new JSONArray()
                .put(part("countertop", "add", 120, 30, 1))
                .put(part("cooktop", "subtract", 30, 18, 1));

        assertEquals(25.0, DrawingMath.squareFeet(parts), 0.001);
    }

    @Test
    public void sinkWithoutDimensionsDoesNotInvalidateCountertop() throws Exception {
        JSONArray parts = new JSONArray()
                .put(part("countertop", "add", 120, 30, 1))
                .put(new JSONObject()
                        .put("feature_type", "sink")
                        .put("operation", "subtract"));

        assertEquals(25.0, DrawingMath.squareFeet(parts), 0.001);
    }

    @Test
    public void cooktopWithoutDimensionsDoesNotInvalidateCountertop() throws Exception {
        JSONArray parts = new JSONArray()
                .put(part("countertop", "add", 120, 30, 1))
                .put(new JSONObject()
                        .put("feature_type", "cooktop")
                        .put("operation", "subtract"));

        assertEquals(25.0, DrawingMath.squareFeet(parts), 0.001);
    }

    @Test
    public void roundsOnlyAfterCompleteCalculation() throws Exception {
        JSONArray parts = new JSONArray()
                .put(part("countertop", "add", 1, 1, 1))
                .put(part("countertop", "add", 1, 1, 1));

        assertEquals(0.01, DrawingMath.squareFeet(parts), 0.001);
    }

    @Test
    public void unknownOperationFailsSafely() throws Exception {
        JSONArray parts = new JSONArray().put(part("other", "guess", 120, 25, 1));

        assertEquals(0.0, DrawingMath.squareFeet(parts), 0.001);
    }

    private static JSONObject part(
            String feature,
            String operation,
            double length,
            double width,
            double quantity) throws Exception {
        return new JSONObject()
                .put("feature_type", feature)
                .put("operation", operation)
                .put("length_inches", length)
                .put("width_inches", width)
                .put("quantity", quantity);
    }
}
