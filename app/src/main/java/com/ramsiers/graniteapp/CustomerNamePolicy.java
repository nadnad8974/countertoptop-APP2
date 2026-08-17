package com.ramsiers.graniteapp;

/** Keeps test navigation separate from actions that create customer-facing records. */
public final class CustomerNamePolicy {
    private CustomerNamePolicy() {
    }

    public static boolean canAdvance(
            boolean requireCompleteName,
            String firstName,
            String lastName) {
        return !requireCompleteName || hasCompleteName(firstName, lastName);
    }

    public static boolean hasCompleteName(String firstName, String lastName) {
        return !clean(firstName).isEmpty() && !clean(lastName).isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
