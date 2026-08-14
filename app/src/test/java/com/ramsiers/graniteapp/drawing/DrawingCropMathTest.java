package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class DrawingCropMathTest {
    @Test
    public void keepsTheWholeDrawingWhenNoTrimIsSelected() {
        assertArrayEquals(
                new int[]{0, 0, 1200, 800},
                DrawingCropMath.bounds(1200, 800, 0, 0, 0, 0));
    }

    @Test
    public void trimsEachEdgeWithoutChangingTheOriginal() {
        assertArrayEquals(
                new int[]{120, 160, 840, 520},
                DrawingCropMath.bounds(1200, 800, 10, 20, 20, 15));
    }

    @Test
    public void rejectsAnUnsafeTrimAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DrawingCropMath.bounds(1200, 800, 41, 0, 0, 0));
    }
}
