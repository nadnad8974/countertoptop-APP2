package com.ramsiers.graniteapp.drawing;

/** Deterministic quote square-foot math shared by the UI and unit tests. */
public final class QuoteSquareFootMath {
    private QuoteSquareFootMath() {}

    public static Result calculate(
            double aiSquareFeet,
            double manualSquareFeet,
            double manualStoveSquareFeet,
            boolean hasCompleteAiEstimate) {
        double safeAi = hasCompleteAiEstimate ? positive(aiSquareFeet) : 0;
        double safeManual = positive(manualSquareFeet);
        // AI drawing totals already apply their opening rules. Only subtract the separate stove
        // fields when the quote is based entirely on manual measurements.
        double stove = hasCompleteAiEstimate ? 0 : positive(manualStoveSquareFeet);
        double gross = safeAi + safeManual;
        return new Result(safeAi, safeManual, gross, stove, Math.max(0, gross - stove));
    }

    private static double positive(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0;
    }

    public static final class Result {
        public final double aiSquareFeet;
        public final double manualSquareFeet;
        public final double grossSquareFeet;
        public final double stoveSquareFeet;
        public final double netSquareFeet;

        Result(
                double aiSquareFeet,
                double manualSquareFeet,
                double grossSquareFeet,
                double stoveSquareFeet,
                double netSquareFeet) {
            this.aiSquareFeet = aiSquareFeet;
            this.manualSquareFeet = manualSquareFeet;
            this.grossSquareFeet = grossSquareFeet;
            this.stoveSquareFeet = stoveSquareFeet;
            this.netSquareFeet = netSquareFeet;
        }
    }
}
