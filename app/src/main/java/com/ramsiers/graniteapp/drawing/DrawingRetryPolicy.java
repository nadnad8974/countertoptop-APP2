package com.ramsiers.graniteapp.drawing;

/** Decides which safe retry choices to show after an original-color analysis. */
public final class DrawingRetryPolicy {
    private DrawingRetryPolicy() {
    }

    public static boolean offerOriginalRetry(boolean analysisInProgress, String lastError) {
        return !analysisInProgress && lastError != null && !lastError.trim().isEmpty();
    }

    public static boolean offerEnhancedRetry(
            boolean analysisInProgress,
            boolean hasResult,
            boolean canCalculate,
            String lastError) {
        if (analysisInProgress) return false;
        boolean failedWithoutResult = !hasResult
                && lastError != null
                && !lastError.trim().isEmpty();
        return failedWithoutResult || (hasResult && !canCalculate);
    }

    /**
     * Automatic darker retries are disabled so one drawing never starts a second long AI
     * request without the user choosing it. The manual enhanced-retry offer remains available.
     */
    public static boolean automaticallyTryEnhanced(
            boolean enhancedWasAlreadyRequested,
            DrawingRecord result) {
        return false;
    }

    /** Never replace a more useful normal-color result with a poorer enhanced result. */
    public static DrawingRecord preferMoreUseful(
            DrawingRecord normalResult,
            DrawingRecord enhancedResult) {
        if (normalResult == null) return enhancedResult;
        if (enhancedResult == null) return normalResult;
        return usefulness(enhancedResult) > usefulness(normalResult)
                ? enhancedResult
                : normalResult;
    }

    private static int usefulness(DrawingRecord result) {
        if (result == null || !result.hasResult()) return -1;
        int score = 0;
        if (result.canCalculate) score += 1000;
        if (DrawingFallback.hasDrawableShape(result.verificationDrawing)) score += 200;
        if (result.verificationDrawing != null) score += 50;
        if (result.calculationParts != null) {
            score += Math.min(40, result.calculationParts.length()) * 10;
        }
        return score;
    }
}
