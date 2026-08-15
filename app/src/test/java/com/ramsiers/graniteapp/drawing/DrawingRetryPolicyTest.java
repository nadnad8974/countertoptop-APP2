package com.ramsiers.graniteapp.drawing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DrawingRetryPolicyTest {
    @Test
    public void completeOriginalResultNeedsNoRetry() {
        assertFalse(DrawingRetryPolicy.offerOriginalRetry(false, ""));
        assertFalse(DrawingRetryPolicy.offerEnhancedRetry(false, true, true, ""));
    }

    @Test
    public void incompleteOriginalResultOffersEnhancedCopy() {
        assertFalse(DrawingRetryPolicy.offerOriginalRetry(false, ""));
        assertTrue(DrawingRetryPolicy.offerEnhancedRetry(false, true, false, ""));
    }

    @Test
    public void failedOriginalRequestOffersBothSafeRetries() {
        assertTrue(DrawingRetryPolicy.offerOriginalRetry(false, "Could not read drawing"));
        assertTrue(DrawingRetryPolicy.offerEnhancedRetry(
                false,
                false,
                false,
                "Could not read drawing"));
    }

    @Test
    public void noRetryIsOfferedDuringAnalysis() {
        assertFalse(DrawingRetryPolicy.offerOriginalRetry(true, "Temporary problem"));
        assertFalse(DrawingRetryPolicy.offerEnhancedRetry(
                true,
                true,
                false,
                "Temporary problem"));
    }
}
