package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.SocketTimeoutException;

import org.junit.Test;

public class DrawingAnalysisFailurePolicyTest {
    @Test
    public void socketAndServerTimeoutsOpenManualRecovery() {
        assertTrue(DrawingAnalysisFailurePolicy.isTimeout(
                new SocketTimeoutException("Read timed out"),
                "Read timed out"));
        assertTrue(DrawingAnalysisFailurePolicy.isTimeout(
                new IllegalStateException("Drawing AI took too long to respond."),
                "Drawing AI took too long to respond."));
        assertTrue(DrawingAnalysisFailurePolicy.isTimeout(
                new IllegalStateException(
                        "Request failed",
                        new RuntimeException(new SocketTimeoutException("Read timed out"))),
                "Request failed"));
    }

    @Test
    public void gatewayAndRequestTimeoutStatusesOpenManualRecoveryBeforeJsonParsing() {
        assertTrue(DrawingAnalysisFailurePolicy.isTimeoutHttpStatus(408));
        assertTrue(DrawingAnalysisFailurePolicy.isTimeoutHttpStatus(504));
        assertFalse(DrawingAnalysisFailurePolicy.isTimeoutHttpStatus(500));
    }

    @Test
    public void phoneDeadlineAddsOnlySmallAllowanceToServiceLimit() {
        assertEquals(105000, DrawingAnalysisFailurePolicy.requestDeadlineMillis(90000));
    }

    @Test
    public void timeoutEditorWaitsForMatchingDrawingPageAndResumedActivity() {
        assertFalse(DrawingAnalysisFailurePolicy.shouldOpenTimeoutRecovery(
                7, 7, false, true, false, false));
        assertFalse(DrawingAnalysisFailurePolicy.shouldOpenTimeoutRecovery(
                7, 7, true, false, false, false));
        assertFalse(DrawingAnalysisFailurePolicy.shouldOpenTimeoutRecovery(
                7, 8, true, true, false, false));
        assertTrue(DrawingAnalysisFailurePolicy.shouldOpenTimeoutRecovery(
                7, 7, true, true, false, false));
    }

    @Test
    public void ordinaryValidationFailureDoesNotPretendToBeTimeout() {
        assertFalse(DrawingAnalysisFailurePolicy.isTimeout(
                new IllegalStateException("Unsafe measurement data"),
                "The measurement could not be verified."));
    }
}
