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
}
