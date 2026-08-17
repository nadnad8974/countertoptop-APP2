package com.ramsiers.graniteapp.drawing;

/** Decides when the app should ask the user to supply a measurement the AI could not read. */
public final class DrawingMissingMeasurementPolicy {
    private DrawingMissingMeasurementPolicy() {
    }

    public static boolean shouldAsk(
            boolean analysisInProgress,
            boolean hasResult,
            boolean canCalculate,
            String missingInformation) {
        if (analysisInProgress || !hasResult) return false;
        String missing = missingInformation == null ? "" : missingInformation.trim();
        return !canCalculate || !missing.isEmpty();
    }
}
