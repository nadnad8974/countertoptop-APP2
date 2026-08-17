package com.ramsiers.graniteapp.drawing;

import java.net.SocketTimeoutException;
import java.util.Locale;

/** Classifies drawing failures that should immediately offer the editable manual fallback. */
public final class DrawingAnalysisFailurePolicy {
    private DrawingAnalysisFailurePolicy() {
    }

    public static int requestDeadlineMillis(int serviceTimeoutMillis) {
        long safeServiceTimeout = Math.max(1000, serviceTimeoutMillis);
        return (int) Math.min(Integer.MAX_VALUE, safeServiceTimeout + 15000L);
    }

    public static boolean shouldOpenTimeoutRecovery(
            int pendingRevision,
            int currentRevision,
            boolean activityResumed,
            boolean onDrawingPage,
            boolean activityFinishing,
            boolean editorAlreadyOpen) {
        return pendingRevision >= 0
                && pendingRevision == currentRevision
                && activityResumed
                && onDrawingPage
                && !activityFinishing
                && !editorAlreadyOpen;
    }

    public static boolean isTimeoutHttpStatus(int responseCode) {
        return responseCode == 408 || responseCode == 504;
    }

    public static boolean isTimeout(Throwable error, String userMessage) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < 8; depth++) {
            if (current instanceof SocketTimeoutException) return true;
            String currentName = current.getClass().getSimpleName();
            String currentMessage = current.getMessage();
            String currentText = ((currentName == null ? "" : currentName)
                    + " "
                    + (currentMessage == null ? "" : currentMessage))
                    .toLowerCase(Locale.US);
            if (containsTimeoutText(currentText)) return true;
            current = current.getCause();
        }
        String className = error == null ? "" : error.getClass().getSimpleName();
        String normalized = ((className == null ? "" : className)
                + " "
                + (userMessage == null ? "" : userMessage))
                .toLowerCase(Locale.US);
        return containsTimeoutText(normalized);
    }

    private static boolean containsTimeoutText(String normalized) {
        return normalized.contains("timeout")
                || normalized.contains("timed out")
                || normalized.contains("too long to respond")
                || normalized.contains("too long to finish")
                || normalized.contains("gateway time-out");
    }
}
