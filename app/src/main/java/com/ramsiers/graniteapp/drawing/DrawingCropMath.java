package com.ramsiers.graniteapp.drawing;

/** Converts four user-selected trim percentages into safe bitmap crop bounds. */
public final class DrawingCropMath {
    public static final int MAXIMUM_TRIM_PERCENT = 40;

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
        int left = checkedPercent(leftPercent);
        int top = checkedPercent(topPercent);
        int right = checkedPercent(rightPercent);
        int bottom = checkedPercent(bottomPercent);
        int x = Math.round(width * left / 100f);
        int y = Math.round(height * top / 100f);
        int cropRight = Math.round(width * (100 - right) / 100f);
        int cropBottom = Math.round(height * (100 - bottom) / 100f);
        return new int[]{
                x,
                y,
                Math.max(1, cropRight - x),
                Math.max(1, cropBottom - y)
        };
    }

    private static int checkedPercent(int value) {
        if (value < 0 || value > MAXIMUM_TRIM_PERCENT) {
            throw new IllegalArgumentException("The crop amount is invalid.");
        }
        return value;
    }
}
