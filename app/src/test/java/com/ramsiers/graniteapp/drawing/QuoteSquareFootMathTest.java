package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QuoteSquareFootMathTest {
    @Test
    public void completeAiAndManualPieceAreAdded() {
        QuoteSquareFootMath.Result result =
                QuoteSquareFootMath.calculate(56.64, 1.0, 0, true);

        assertEquals(56.64, result.aiSquareFeet, 0.0001);
        assertEquals(1.0, result.manualSquareFeet, 0.0001);
        assertEquals(57.64, result.grossSquareFeet, 0.0001);
        assertEquals(57.64, result.netSquareFeet, 0.0001);
    }

    @Test
    public void manualOnlyStillSubtractsSeparateStoveOpening() {
        QuoteSquareFootMath.Result result =
                QuoteSquareFootMath.calculate(0, 60, 4, false);

        assertEquals(60, result.grossSquareFeet, 0.0001);
        assertEquals(4, result.stoveSquareFeet, 0.0001);
        assertEquals(56, result.netSquareFeet, 0.0001);
    }

    @Test
    public void aiOpeningRulesAreNotDeductedTwice() {
        QuoteSquareFootMath.Result result =
                QuoteSquareFootMath.calculate(56.64, 0, 4, true);

        assertEquals(0, result.stoveSquareFeet, 0.0001);
        assertEquals(56.64, result.netSquareFeet, 0.0001);
    }
}
