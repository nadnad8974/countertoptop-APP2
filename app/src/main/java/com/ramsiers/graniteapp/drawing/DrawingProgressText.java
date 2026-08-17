package com.ramsiers.graniteapp.drawing;

/** Builds honest drawing-analysis progress text without implying model progress is measurable. */
public final class DrawingProgressText {
    private DrawingProgressText() {
    }

    public static String estimatedStatus(
            int estimatedProgress,
            String stage,
            long elapsedSeconds) {
        int progress = Math.max(0, Math.min(100, estimatedProgress));
        long seconds = Math.max(0, elapsedSeconds);
        if (progress == 90) {
            return "Estimated progress: 90%\n"
                    + "Waiting on one AI reading — still working, not frozen ("
                    + seconds
                    + " seconds)";
        }
        String safeStage = stage == null || stage.trim().isEmpty()
                ? "Reading drawing"
                : stage.trim();
        return "Estimated progress: "
                + progress
                + "%\n"
                + safeStage
                + " — AI is still working ("
                + seconds
                + " seconds)";
    }

    public static String timeoutStatus(
            int drawingNumber,
            int drawingCount,
            long elapsedSeconds) {
        int safeCount = Math.max(1, drawingCount);
        int safeNumber = Math.max(1, Math.min(safeCount, drawingNumber));
        return "AI timed out — processing stopped at drawing "
                + safeNumber
                + " of "
                + safeCount
                + " after "
                + Math.max(0, elapsedSeconds)
                + " seconds. Enter the exact measurements in the editable redraw.";
    }
}
