package com.ramsiers.graniteapp.drawing;

/** Converts a photographed paper drawing into locally thresholded black and white pixels. */
public final class DrawingImageEnhancer {
    private static final int DARKNESS_OFFSET = 12;

    private DrawingImageEnhancer() {
    }

    public static void enhanceInPlace(int[] pixels, int width, int height) {
        if (pixels == null || width <= 0 || height <= 0 || pixels.length != width * height) {
            throw new IllegalArgumentException("The drawing pixels are invalid.");
        }

        int stride = width + 1;
        int[] integral = new int[stride * (height + 1)];
        for (int y = 0; y < height; y++) {
            int rowSum = 0;
            int pixelRow = y * width;
            int integralRow = (y + 1) * stride;
            int previousIntegralRow = y * stride;
            for (int x = 0; x < width; x++) {
                int color = pixels[pixelRow + x];
                int luminance = ((77 * ((color >> 16) & 0xff))
                        + (150 * ((color >> 8) & 0xff))
                        + (29 * (color & 0xff))) >> 8;
                pixels[pixelRow + x] = luminance;
                rowSum += luminance;
                integral[integralRow + x + 1] = integral[previousIntegralRow + x + 1] + rowSum;
            }
        }

        int radius = Math.max(12, Math.min(40, Math.min(width, height) / 40));
        for (int y = 0; y < height; y++) {
            int top = Math.max(0, y - radius);
            int bottom = Math.min(height - 1, y + radius);
            int pixelRow = y * width;
            for (int x = 0; x < width; x++) {
                int left = Math.max(0, x - radius);
                int right = Math.min(width - 1, x + radius);
                int count = (right - left + 1) * (bottom - top + 1);
                int sum = integral[(bottom + 1) * stride + right + 1]
                        - integral[top * stride + right + 1]
                        - integral[(bottom + 1) * stride + left]
                        + integral[top * stride + left];
                int localMean = sum / count;
                pixels[pixelRow + x] = pixels[pixelRow + x] < localMean - DARKNESS_OFFSET
                        ? 0xff000000
                        : 0xffffffff;
            }
        }
    }
}
