package com.ramsiers.graniteapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class JobWorkflowTest {
    @Test
    public void approvedOrderHasExactlyThirtyEightUniquePages() {
        String[] pages = JobWorkflow.APPROVED_PAGE_ORDER.split(",");
        assertEquals(38, pages.length);
        assertEquals(38, java.util.Arrays.stream(pages).distinct().count());
        assertEquals("21", pages[0]);
        assertEquals("42", pages[9]);
        assertFalse(java.util.Arrays.asList(pages).contains("41"));
        assertFalse(java.util.Arrays.asList(pages).contains("43"));
    }

    @Test
    public void templateAddsSquareFeetWithoutChangingOriginalMeasurement() {
        assertEquals(112.5, JobWorkflow.finalSquareFeet(100, 12.5), 0.001);
        assertEquals(100, JobWorkflow.finalSquareFeet(100, -4), 0.001);
    }

    @Test
    public void finalPriceUsesTemplateOverrideOnlyWhenPositive() {
        assertEquals(65, JobWorkflow.effectivePrice(65, 0), 0.001);
        assertEquals(72, JobWorkflow.effectivePrice(65, 72), 0.001);
    }

    @Test
    public void removalIsTenDollarsPerSquareFootOnlyWhenSelected() {
        assertEquals(850, JobWorkflow.removalCharge(true, 85), 0.001);
        assertEquals(0, JobWorkflow.removalCharge(false, 85), 0.001);
    }
}
