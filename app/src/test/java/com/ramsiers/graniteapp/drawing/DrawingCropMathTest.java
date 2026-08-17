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
    public void keepsTheWholeEdgeWhenTrimIsWithinTheSafetyMargin() {
        assertArrayEquals(
                new int[]{0, 0, 1200, 800},
                DrawingCropMath.bounds(1200, 800, 3, 3, 3, 3));
    }

    @Test
    public void trimsEachEdgeButKeepsTheSafetyMargin() {
        assertArrayEquals(
                new int[]{84, 136, 912, 568},
                DrawingCropMath.bounds(1200, 800, 10, 20, 20, 15));
    }

    @Test
    public void beginsTrimmingAfterTheSafetyMargin() {
        assertArrayEquals(
                new int[]{12, 0, 1188, 800},
                DrawingCropMath.bounds(1200, 800, 4, 0, 0, 0));
    }

    @Test
    public void roundsOutwardToProtectPartialEdgePixels() {
        assertArrayEquals(
                new int[]{1, 0, 99, 99},
                DrawingCropMath.bounds(101, 99, 4, 4, 4, 4));
    }

    @Test
    public void rejectsAnUnsafeTrimAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DrawingCropMath.bounds(1200, 800, 41, 0, 0, 0));
    }
}
