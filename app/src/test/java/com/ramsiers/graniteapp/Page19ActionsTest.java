package com.ramsiers.graniteapp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Page19ActionsTest {
    @Test
    public void buttonsStayInRequestedOrder() {
        assertArrayEquals(
                new String[]{
                        "Save",
                        "Print",
                        "Email",
                        "Text",
                        "Calendar",
                        "Job Acceptance",
                        "Installation"
                },
                Page19Actions.buttonOrder());
    }

    @Test
    public void saveAutomaticallyHandsOffCalendarEmailThenText() {
        int step = Page19Actions.firstAutomaticStep();
        assertEquals(Page19Actions.CALENDAR_STEP, step);
        step = Page19Actions.nextAutomaticStep(step);
        assertEquals(Page19Actions.EMAIL_STEP, step);
        step = Page19Actions.nextAutomaticStep(step);
        assertEquals(Page19Actions.TEXT_STEP, step);
        step = Page19Actions.nextAutomaticStep(step);
        assertEquals(Page19Actions.COMPLETE, step);
    }
}
