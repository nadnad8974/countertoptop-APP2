package com.ramsiers.graniteapp;

/** Pure workflow rules shared by the UI and unit tests. */
final class JobWorkflow {
    static final String APPROVED_PAGE_ORDER =
            "21,0,1,2,3,12,22,23,8,42,15,13,16,17,20,14,18,19,24,11,6,4,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40";
    static final double REMOVAL_PRICE_PER_SQUARE_FOOT = 10.0;

    private JobWorkflow() {
    }

    static double finalSquareFeet(double measuredSquareFeet, double additionalSquareFeet) {
        return Math.max(0, measuredSquareFeet) + Math.max(0, additionalSquareFeet);
    }

    static double effectivePrice(double quotedPrice, double templatePrice) {
        return templatePrice > 0 ? templatePrice : Math.max(0, quotedPrice);
    }

    static double removalCharge(boolean removalSelected, double removalSquareFeet) {
        return removalSelected
                ? Math.max(0, removalSquareFeet) * REMOVAL_PRICE_PER_SQUARE_FOOT
                : 0;
    }
}
