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

    @Test
    public void legacyTenByFourteenResultWithNearbyTwentyFivePointFiveIsIncomplete()
            throws Exception {
        JSONObject response = new JSONObject()
                .put("square_feet", 0.97)
                .put("can_calculate", true)
                .put("calculation_parts", new JSONArray().put(new JSONObject()
                        .put("id", "P4")
                        .put("link_id", "P4")
                        .put("feature_type", "countertop")
                        .put("operation", "add")
                        .put("length_inches", 10)
                        .put("width_inches", 14)
                        .put("quantity", 1)))
                .put("verification_drawing", new JSONObject()
                        .put("canvas_width", 1000)
                        .put("canvas_height", 700)
                        .put("shapes", new JSONArray().put(rectangle("P4")))
                        .put("dimensions", new JSONArray()
                                .put(dimension("P4", "length", 10, 100, 70, 200, 70))
                                .put(dimension("P4", "width", 14, 70, 100, 70, 300))
                                .put(dimension(
                                        "unlinked",
                                        "length",
                                        25.5,
                                        225,
                                        100,
                                        225,
                                        300))));

        DrawingRecord result = DrawingRecord.fromServerResponse(
                "content://drawing/misread-four",
                4,
                response);

        assertFalse(result.canCalculate);
        assertEquals(0, result.squareFeet, 0.001);
        assertTrue(result.missingInformation.contains("25.5\""));
        assertTrue(result.missingInformation.contains("10 × 14"));
        assertEquals(10, result.calculationParts.getJSONObject(0)
                .getDouble("length_inches"), 0.001);
        assertEquals(14, result.calculationParts.getJSONObject(0)
                .getDouble("width_inches"), 0.001);
    }

    @Test
    public void serverMissingQuestionPreventsPricingEvenWithPositiveFormula()
            throws Exception {
        JSONObject response = new JSONObject()
                .put("square_feet", 25)
                .put("can_calculate", true)
                .put("missing_information", "What is the island width in inches?")
                .put("calculation_parts", new JSONArray()
                        .put(part("countertop", "add", 120, 30)));

        DrawingRecord result = DrawingRecord.fromServerResponse(
                "content://drawing/question",
                4,
                response);

        assertFalse(result.canCalculate);
        assertEquals(0, result.squareFeet, 0.001);
        assertEquals(
                "What is the island width in inches?",
                result.missingInformation);
    }

    @Test
    public void restoredUneditedAiResultIsRecheckedButManualEditIsPreserved()
            throws Exception {
        JSONObject unsafe = new JSONObject()
                .put("uri", "content://drawing/saved")
                .put("result_revision", 4)
                .put("analyzed", true)
                .put("square_feet", 0.97)
                .put("can_calculate", true)
                .put("edited_by_user", false)
                .put("calculation_parts", new JSONArray().put(new JSONObject()
                        .put("id", "P4")
                        .put("link_id", "P4")
                        .put("feature_type", "countertop")
                        .put("operation", "add")
                        .put("length_inches", 10)
                        .put("width_inches", 14)
                        .put("quantity", 1)))
                .put("verification_drawing", new JSONObject()
                        .put("canvas_width", 1000)
                        .put("canvas_height", 700)
                        .put("shapes", new JSONArray().put(rectangle("P4")))
                        .put("dimensions", new JSONArray().put(dimension(
                                "unlinked",
                                "length",
                                25.5,
                                225,
                                100,
                                225,
                                300))));

        DrawingRecord guarded = DrawingRecord.fromJsonObject(unsafe);
        unsafe.put("edited_by_user", true);
        DrawingRecord edited = DrawingRecord.fromJsonObject(unsafe);

        assertFalse(guarded.canCalculate);
        assertTrue(guarded.missingInformation.contains("25.5\""));
        assertTrue(edited.canCalculate);
        assertEquals(0.97, edited.squareFeet, 0.001);
    }

    @Test
    public void driveJobRestoreRechecksUneditedAiButKeepsManualCorrection()
            throws Exception {
        JSONObject saved = new JSONObject()
                .put("resultRevision", 4)
                .put("analyzed", true)
                .put("squareFeet", 0.97)
                .put("canCalculate", true)
                .put("editedByUser", false)
                .put("calculationParts", new JSONArray().put(new JSONObject()
                        .put("id", "P4")
                        .put("link_id", "P4")
                        .put("feature_type", "countertop")
                        .put("operation", "add")
                        .put("length_inches", 10)
                        .put("width_inches", 14)
                        .put("quantity", 1)))
                .put("verificationDrawing", new JSONObject()
                        .put("canvas_width", 1000)
                        .put("canvas_height", 700)
                        .put("shapes", new JSONArray().put(rectangle("P4")))
                        .put("dimensions", new JSONArray().put(dimension(
                                "unlinked",
                                "length",
                                25.5,
                                225,
                                100,
                                225,
                                300))));

        DrawingRecord guarded = DrawingRecord.fromSavedJob(
                "content://drawing/drive",
                saved);
        saved.put("editedByUser", true);
        DrawingRecord edited = DrawingRecord.fromSavedJob(
                "content://drawing/drive",
                saved);

        assertFalse(guarded.canCalculate);
        assertTrue(guarded.missingInformation.contains("25.5\""));
        assertTrue(edited.canCalculate);
        assertEquals(0.97, edited.squareFeet, 0.001);
    }

    private static JSONObject rectangle(String linkId) throws Exception {
        return new JSONObject()
                .put("id", "shape_" + linkId)
                .put("link_id", linkId)
                .put("kind", "countertop")
                .put("feature_type", "countertop")
                .put("points", new JSONArray()
                        .put(point(100, 100))
                        .put(point(200, 100))
                        .put(point(200, 300))
                        .put(point(100, 300)));
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
                .put("x1", x1)
                .put("y1", y1)
                .put("x2", x2)
                .put("y2", y2);
    }

    private static JSONObject point(double x, double y) throws Exception {
        return new JSONObject().put("x", x).put("y", y);
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
