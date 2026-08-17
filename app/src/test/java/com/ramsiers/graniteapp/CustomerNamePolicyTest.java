package com.ramsiers.graniteapp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CustomerNamePolicyTest {
    @Test
    public void testNavigationAllowsBlankNamesWhenRequirementIsOff() {
        assertTrue(CustomerNamePolicy.canAdvance(false, "", ""));
        assertTrue(CustomerNamePolicy.canAdvance(false, "Dan", ""));
    }

    @Test
    public void normalNavigationRequiresBothNamesWhenRequirementIsOn() {
        assertFalse(CustomerNamePolicy.canAdvance(true, "", "Ramsier"));
        assertFalse(CustomerNamePolicy.canAdvance(true, "Dan", ""));
        assertTrue(CustomerNamePolicy.canAdvance(true, "Dan", "Ramsier"));
    }

    @Test
    public void customerActionsAlwaysRequireBothNonblankNames() {
        assertFalse(CustomerNamePolicy.hasCompleteName(null, "Ramsier"));
        assertFalse(CustomerNamePolicy.hasCompleteName("Dan", "   "));
        assertTrue(CustomerNamePolicy.hasCompleteName(" Dan ", " Ramsier "));
    }
}
