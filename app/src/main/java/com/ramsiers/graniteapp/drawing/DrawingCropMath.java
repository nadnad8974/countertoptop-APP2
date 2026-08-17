package com.ramsiers.graniteapp.drawing;

/** Converts four user-selected trim percentages into safe bitmap crop bounds. */
public final class DrawingCropMath {
    public static final int MAXIMUM_TRIM_PERCENT = 40;
    public static final int SAFETY_MARGIN_PERCENT = 3;

    private DrawingCropMath() {
    }

    public static int[] bounds(
            int width,
            int height,
            int leftPercent,
            int topPercent,
            int rightPercent,
            int bottomPercent) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("The drawing size is invalid.");
        }
        int left = effectiveTrimPercent(leftPercent);
        int top = effectiveTrimPercent(topPercent);
        int right = effectiveTrimPercent(rightPercent);
        int bottom = effectiveTrimPercent(bottomPercent);
        int x = (int) Math.floor(width * left / 100d);
        int y = (int) Math.floor(height * top / 100d);
        int cropRight = (int) Math.ceil(width * (100 - right) / 100d);
        int cropBottom = (int) Math.ceil(height * (100 - bottom) / 100d);
        return new int[]{
                x,
                y,
                Math.max(1, cropRight - x),
                Math.max(1, cropBottom - y)
        };
    }

    /**
     * Keeps a small amount outside the selected crop so handwriting at the paper edge survives.
     */
    public static int effectiveTrimPercent(int requestedPercent) {
        return Math.max(0, checkedPercent(requestedPercent) - SAFETY_MARGIN_PERCENT);
    }

    private static int checkedPercent(int value) {
        if (value < 0 || value > MAXIMUM_TRIM_PERCENT) {
            throw new IllegalArgumentException("The crop amount is invalid.");
        }
        return value;
    }
}
