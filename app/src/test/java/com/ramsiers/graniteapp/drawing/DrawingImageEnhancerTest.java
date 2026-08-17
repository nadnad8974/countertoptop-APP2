package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;

import org.junit.Test;

public class DrawingImageEnhancerTest {
    @Test
    public void makesUnevenPaperWhiteAndFaintMarksBlack() {
        int width = 25;
        int height = 25;
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int paper = 170 + y;
            Arrays.fill(
                    pixels,
                    y * width,
                    (y + 1) * width,
                    rgb(paper, paper - 4, paper - 8));
        }
        for (int y = 3; y < height - 3; y++) {
            pixels[y * width + 12] = rgb(135, 130, 125);
        }

        DrawingImageEnhancer.enhanceInPlace(pixels, width, height);

        assertEquals(0xff000000, pixels[12 * width + 12]);
        assertEquals(0xffffffff, pixels[12 * width + 4]);
        for (int pixel : pixels) {
            if (pixel != 0xff000000 && pixel != 0xffffffff) {
                throw new AssertionError("Enhanced pixels must be pure black or white.");
            }
        }
    }

    @Test
    public void rejectsMismatchedPixelDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DrawingImageEnhancer.enhanceInPlace(new int[3], 2, 2));
    }

    private static int rgb(int red, int green, int blue) {
        return 0xff000000 | (red << 16) | (green << 8) | blue;
    }
}
