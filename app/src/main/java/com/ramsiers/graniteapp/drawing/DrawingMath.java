package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

/** Deterministic square-foot calculation. AI supplies measurements; this class does the math. */
public final class DrawingMath {
    private DrawingMath() {
    }

    public static double squareFeet(JSONArray parts) {
        if (parts == null || parts.length() == 0) return 0;
        double squareInches = 0;
        for (int i = 0; i < Math.min(parts.length(), 40); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part == null) return 0;
            String feature = DrawingRules.featureType(part);
            if (DrawingRules.FEATURE_SINK.equals(feature)
                    || DrawingRules.FEATURE_COOKTOP.equals(feature)) {
                // These openings do not change countertop square footage, so missing or
                // uncertain cutout dimensions must not invalidate the usable countertop pieces.
                continue;
            }
            double length = part.optDouble("length_inches", Double.NaN);
            double width = part.optDouble("width_inches", Double.NaN);
            double quantity = part.optDouble("quantity", 1);
            if (!isPositiveFinite(length)
                    || !isPositiveFinite(width)
                    || !isPositiveFinite(quantity)) return 0;

            double area = length * width * quantity;
            if (DrawingRules.FEATURE_STOVE.equals(feature)) {
                squareInches -= area;
                continue;
            }

            String operation = part.optString("operation", "");
            if ("add".equals(operation)) squareInches += area;
            else if ("subtract".equals(operation)) squareInches -= area;
            else return 0;
        }
        if (!isPositiveFinite(squareInches)) return 0;
        return Math.round((squareInches / 144.0) * 100.0) / 100.0;
    }

    private static boolean isPositiveFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0;
    }
}
