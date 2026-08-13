package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class DrawingRecordTest {
    @Test
    public void serverResponseIsSanitizedAndRecalculated() throws Exception {
        JSONObject response = new JSONObject()
                .put("square_feet", 21.25)
                .put("can_calculate", true)
                .put("calculation_parts", new JSONArray()
                        .put(part("countertop", "add", 120, 30))
                        .put(part("sink", "subtract", 30, 18)))
                .put("verification_drawing", new JSONObject()
                        .put("shapes", new JSONArray().put(new JSONObject()
                                .put("kind", "countertop")
                                .put("label", "AI countertop label"))));

        DrawingRecord result = DrawingRecord.fromServerResponse(
                "content://drawing/one",
                4,
                response);

        assertTrue(result.canCalculate);
        assertEquals(25.0, result.squareFeet, 0.001);
        assertFalse(result.verificationDrawing
                .getJSONArray("shapes")
                .getJSONObject(0)
                .has("label"));
    }

    @Test
    public void serverTotalWithoutVerifiablePartsIsRejected() throws Exception {
        JSONObject response = new JSONObject()
                .put("square_feet", 40)
                .put("can_calculate", true);

        DrawingRecord result = DrawingRecord.fromServerResponse(
                "content://drawing/one",
                4,
                response);

        assertFalse(result.canCalculate);
        assertEquals(0, result.squareFeet, 0.001);
        assertTrue(result.missingInformation.contains("could not be verified"));
    }

    @Test
    public void stoveIsDeductedButCooktopIsNot() throws Exception {
        JSONObject response = new JSONObject()
                .put("square_feet", 1)
                .put("can_calculate", true)
                .put("calculation_parts", new JSONArray()
                        .put(part("countertop", "add", 120, 30))
                        .put(part("stove", "add", 30, 24))
                        .put(part("cooktop", "subtract", 24, 18)));

        DrawingRecord result = DrawingRecord.fromServerResponse(
                "content://drawing/one",
                4,
                response);

        assertTrue(result.canCalculate);
        assertEquals(20, result.squareFeet, 0.001);
    }

    @Test
    public void missingServerOutlineGetsEditableMeasuredFallback() throws Exception {
        JSONObject response = new JSONObject()
                .put("square_feet", 25)
                .put("can_calculate", true)
                .put("calculation_parts", new JSONArray()
                        .put(part("countertop", "add", 120, 30))
                        .put(part("sink", "subtract", 30, 18)));

        DrawingRecord result = DrawingRecord.fromServerResponse(
                "content://drawing/one",
                4,
                response);

        assertTrue(result.canCalculate);
        assertTrue(result.verificationDrawing.getBoolean("fallback_generated"));
        assertTrue(result.verificationDrawing.getJSONArray("shapes").length() >= 2);
        assertTrue(result.verificationDrawing.getJSONArray("dimensions").length() >= 4);
        assertEquals(
                "measured_part_1",
                result.calculationParts.getJSONObject(0).getString("id"));
    }

    private static JSONObject part(
            String feature,
            String operation,
            double length,
            double width) throws Exception {
        return new JSONObject()
                .put("feature_type", feature)
                .put("operation", operation)
                .put("length_inches", length)
                .put("width_inches", width)
                .put("quantity", 1);
    }
}
