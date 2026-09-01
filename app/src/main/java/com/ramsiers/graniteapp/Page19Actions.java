package com.ramsiers.graniteapp;

/** Page 19 button labels and the automatic Save handoff order. */
final class Page19Actions {
    static final String SAVE = "Save";
    static final String PRINT = "Print";
    static final String EMAIL = "Email";
    static final String TEXT = "Text";
    static final String CALENDAR = "Calendar";
    static final String JOB_ACCEPTANCE = "Job Acceptance";
    static final String INSTALLATION = "Installation";

    static final int IDLE = 0;
    static final int CALENDAR_STEP = 1;
    static final int EMAIL_STEP = 2;
    static final int TEXT_STEP = 3;
    static final int COMPLETE = 4;

    private Page19Actions() {
    }

    static String[] buttonOrder() {
        return new String[]{
                SAVE,
                PRINT,
                EMAIL,
                TEXT,
                CALENDAR,
                JOB_ACCEPTANCE,
                INSTALLATION
        };
    }

    static int firstAutomaticStep() {
        return CALENDAR_STEP;
    }

    static int nextAutomaticStep(int currentStep) {
        if (currentStep == CALENDAR_STEP) return EMAIL_STEP;
        if (currentStep == EMAIL_STEP) return TEXT_STEP;
        return COMPLETE;
    }
}
