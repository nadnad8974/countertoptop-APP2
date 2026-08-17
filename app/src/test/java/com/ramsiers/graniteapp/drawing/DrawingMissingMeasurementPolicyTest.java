package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DrawingMissingMeasurementPolicyTest {
    @Test
    public void asksWhenCalculationIsIncomplete() {
        assertTrue(DrawingMissingMeasurementPolicy.shouldAsk(false, true, false, ""));
    }

    @Test
    public void asksWhenAiReportsMissingInformation() {
        assertTrue(DrawingMissingMeasurementPolicy.shouldAsk(
                false,
                true,
                true,
                "Island width could not be read."));
    }

    @Test
    public void doesNotAskDuringAnalysisOrBeforeAResult() {
        assertFalse(DrawingMissingMeasurementPolicy.shouldAsk(true, true, false, "Missing"));
        assertFalse(DrawingMissingMeasurementPolicy.shouldAsk(false, false, false, "Missing"));
    }

    @Test
    public void doesNotAskForCompleteResult() {
        assertFalse(DrawingMissingMeasurementPolicy.shouldAsk(false, true, true, "  "));
    }
}
