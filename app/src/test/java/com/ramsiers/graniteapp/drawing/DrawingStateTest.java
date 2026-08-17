package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;

public class DrawingStateTest {
    @Test
    public void twoPerDrawingResultsRoundTripWithoutCrossAssignment() throws Exception {
        DrawingState restored = DrawingState.fromJson(stateWithTwoDrawings().toJson());

        assertEquals(2, restored.drawings.size());
        assertEquals("content://drawing/one", restored.drawings.get(0).uri);
        assertEquals(20.0, restored.drawings.get(0).squareFeet, 0.001);
        assertEquals("content://drawing/two", restored.drawings.get(1).uri);
        assertEquals(36.64, restored.drawings.get(1).squareFeet, 0.001);
        assertEquals(1, restored.activeDrawingIndex);
    }

    @Test
    public void editableJsonSurvivesRestoreOnCorrectDrawing() throws Exception {
        DrawingState restored = DrawingState.fromJson(stateWithTwoDrawings().toJson());

        assertEquals(
                "Island",
                restored.drawings.get(1).verificationDrawing
                        .getJSONArray("shapes")
                        .getJSONObject(0)
                        .getString("user_label"));
        assertTrue(restored.drawings.get(1).editedByUser);
        assertFalse(restored.drawings.get(0).editedByUser);
    }

    @Test
    public void selectedIndexIsClampedAfterDrawingRemoval() {
        DrawingState state = new DrawingState(
                Arrays.asList(DrawingRecord.empty("content://drawing/one")),
                4,
                2);

        assertEquals(0, state.activeDrawingIndex);
    }

    @Test
    public void analysisErrorPreservesSuccessfulResult() throws Exception {
        DrawingRecord successful = stateWithTwoDrawings().drawings.get(0);

        DrawingRecord failedRefresh = successful.withError("Temporary service problem");
        DrawingState restored = DrawingState.fromJson(
                new DrawingState(Arrays.asList(failedRefresh), 0, 8).toJson());

        assertTrue(restored.drawings.get(0).canCalculate);
        assertEquals(20.0, restored.drawings.get(0).squareFeet, 0.001);
        assertEquals("Temporary service problem", restored.drawings.get(0).lastError);
    }

    @Test
    public void schemaOneSingleDrawingMigrates() throws Exception {
        JSONObject legacy = legacyState(new JSONArray().put("content://drawing/one"));

        DrawingState restored = DrawingState.fromJson(legacy.toString());

        assertEquals(1, restored.drawings.size());
        assertTrue(restored.drawings.get(0).hasResult());
        assertEquals(20.0, restored.drawings.get(0).squareFeet, 0.001);
    }

    @Test
    public void schemaOneInvalidFormulaCannotReuseSavedServerTotal() throws Exception {
        JSONObject legacy = legacyState(new JSONArray().put("content://drawing/one"));
        legacy.put("calculation_parts", new JSONArray().put(new JSONObject()
                .put("operation", "add")
                .put("length_inches", 120)));

        DrawingState restored = DrawingState.fromJson(legacy.toString());

        assertTrue(restored.drawings.get(0).hasResult());
        assertFalse(restored.drawings.get(0).canCalculate);
        assertEquals(0.0, restored.drawings.get(0).squareFeet, 0.001);
        assertTrue(restored.drawings.get(0).missingInformation.contains("verified again"));
    }

    @Test
    public void schemaOneMultiDrawingCombinedResultIsDiscarded() throws Exception {
        JSONObject legacy = legacyState(new JSONArray()
                .put("content://drawing/one")
                .put("content://drawing/two"));

        DrawingState restored = DrawingState.fromJson(legacy.toString());

        assertEquals(2, restored.drawings.size());
        assertFalse(restored.drawings.get(0).hasResult());
        assertFalse(restored.drawings.get(1).hasResult());
    }

    @Test
    public void corruptSavedJsonReturnsSafeEmptyState() {
        DrawingState restored = DrawingState.fromJson("{definitely-not-json");

        assertTrue(restored.drawings.isEmpty());
        assertEquals(0, restored.activeDrawingIndex);
    }

    private static DrawingState stateWithTwoDrawings() throws Exception {
        JSONArray parts = new JSONArray().put(new JSONObject()
                .put("id", "counter")
                .put("link_id", "counter")
                .put("operation", "add")
                .put("length_inches", 72)
                .put("width_inches", 40));
        JSONObject islandRedraw = new JSONObject().put(
                "shapes",
                new JSONArray().put(new JSONObject()
                        .put("kind", "countertop")
                        .put("user_label", "Island")));
        JSONObject linkedRedraw = new JSONObject().put(
                "shapes",
                new JSONArray().put(new JSONObject()
                        .put("id", "counter")
                        .put("link_id", "counter")
                        .put("kind", "countertop")
                        .put("points", new JSONArray()
                                .put(new JSONObject().put("x", 10).put("y", 10))
                                .put(new JSONObject().put("x", 200).put("y", 10))
                                .put(new JSONObject().put("x", 200).put("y", 100))
                                .put(new JSONObject().put("x", 10).put("y", 100)))));
        DrawingRecord first = record(
                "content://drawing/one",
                20,
                false,
                parts,
                linkedRedraw);
        DrawingRecord second = record(
                "content://drawing/two",
                36.64,
                true,
                parts,
                islandRedraw);
        return new DrawingState(Arrays.asList(first, second), 1, 7);
    }

    private static DrawingRecord record(
            String uri,
            double squareFeet,
            boolean edited,
            JSONArray parts,
            JSONObject redraw) {
        return new DrawingRecord(
                uri,
                7,
                true,
                squareFeet,
                true,
                edited,
                "high",
                "Verified",
                "",
                "",
                parts,
                redraw);
    }

    private static JSONObject legacyState(JSONArray uris) throws Exception {
        return new JSONObject()
                .put("schema_version", 1)
                .put("drawing_uris", uris)
                .put("active_drawing_index", 0)
                .put("input_revision", 7)
                .put("result_revision", 7)
                .put("square_feet", 999)
                .put("can_calculate", true)
                .put("confidence", "high")
                .put("calculation_parts", new JSONArray().put(new JSONObject()
                        .put("operation", "add")
                        .put("length_inches", 120)
                        .put("width_inches", 24)));
    }
}
