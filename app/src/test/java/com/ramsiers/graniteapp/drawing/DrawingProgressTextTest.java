package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DrawingProgressTextTest {
    @Test
    public void ninetyPercentExplainsThatOneReadingIsStillWorking() {
        String status = DrawingProgressText.estimatedStatus(
                90,
                "Creating verification redraw",
                160);

        assertEquals(
                "Estimated progress: 90%\n"
                        + "Waiting on one AI reading — still working, not frozen (160 seconds)",
                status);
    }

    @Test
    public void earlierProgressKeepsCurrentStage() {
        String status = DrawingProgressText.estimatedStatus(
                89,
                "Creating verification redraw",
                65);

        assertTrue(status.contains("Creating verification redraw — AI is still working"));
        assertFalse(status.contains("not frozen"));
    }

    @Test
    public void timeoutNamesTheDrawingWhereProcessingStopped() {
        String status = DrawingProgressText.timeoutStatus(2, 4, 106);

        assertTrue(status.contains("AI timed out"));
        assertTrue(status.contains("stopped at drawing 2 of 4"));
        assertTrue(status.contains("106 seconds"));
        assertTrue(status.contains("editable redraw"));
        assertFalse(status.contains("100% complete"));
    }
}
