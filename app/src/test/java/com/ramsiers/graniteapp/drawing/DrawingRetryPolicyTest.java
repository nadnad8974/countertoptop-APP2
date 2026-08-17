package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class DrawingRetryPolicyTest {
    @Test
    public void completeOriginalResultNeedsNoRetry() {
        assertFalse(DrawingRetryPolicy.offerOriginalRetry(false, ""));
        assertFalse(DrawingRetryPolicy.offerEnhancedRetry(false, true, true, ""));
    }

    @Test
    public void incompleteOriginalResultOffersEnhancedCopy() {
        assertFalse(DrawingRetryPolicy.offerOriginalRetry(false, ""));
        assertTrue(DrawingRetryPolicy.offerEnhancedRetry(false, true, false, ""));
    }

    @Test
    public void failedOriginalRequestOffersBothSafeRetries() {
        assertTrue(DrawingRetryPolicy.offerOriginalRetry(false, "Could not read drawing"));
        assertTrue(DrawingRetryPolicy.offerEnhancedRetry(
                false,
                false,
                false,
                "Could not read drawing"));
    }

    @Test
    public void noRetryIsOfferedDuringAnalysis() {
        assertFalse(DrawingRetryPolicy.offerOriginalRetry(true, "Temporary problem"));
        assertFalse(DrawingRetryPolicy.offerEnhancedRetry(
                true,
                true,
                false,
                "Temporary problem"));
    }

    @Test
    public void automaticallyTriesDarkerCopyForIncompleteNormalResult() {
        DrawingRecord incomplete = record(false, DrawingFallback.blankEditable(), 0);

        assertTrue(DrawingRetryPolicy.automaticallyTryEnhanced(false, incomplete));
        assertFalse(DrawingRetryPolicy.automaticallyTryEnhanced(true, incomplete));
    }

    @Test
    public void keepsMoreUsefulResult() throws Exception {
        DrawingRecord normal = record(false, DrawingFallback.blankEditable(), 0);
        JSONObject drawable = new JSONObject()
                .put("shapes", new JSONArray().put(new JSONObject()
                        .put("points", new JSONArray()
                                .put(new JSONObject().put("x", 10).put("y", 10))
                                .put(new JSONObject().put("x", 100).put("y", 10))
                                .put(new JSONObject().put("x", 100).put("y", 80)))));
        DrawingRecord darker = record(false, drawable, 1);

        assertSame(darker, DrawingRetryPolicy.preferMoreUseful(normal, darker));
        assertSame(darker, DrawingRetryPolicy.preferMoreUseful(darker, normal));
    }

    private DrawingRecord record(
            boolean canCalculate,
            JSONObject drawing,
            int partCount) {
        JSONArray parts = new JSONArray();
        for (int i = 0; i < partCount; i++) parts.put(new JSONObject());
        return new DrawingRecord(
                "drawing",
                1,
                true,
                canCalculate ? 10 : 0,
                canCalculate,
                false,
                "low",
                "",
                "",
                "",
                parts,
                drawing);
    }
}
