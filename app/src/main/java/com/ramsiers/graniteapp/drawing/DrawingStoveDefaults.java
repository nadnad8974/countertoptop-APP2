package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Applies Ramsiers' four-burner drawing convention without replacing an explicit size. */
public final class DrawingStoveDefaults {
    /**
     * Stove length_inches is the opening width across the cabinet run. Stove width_inches is
     * the front-to-back depth, which must remain missing until it is explicitly measured.
     */
    public static final double DEFAULT_ACROSS_RUN_STOVE_WIDTH_INCHES = 30;

    private DrawingStoveDefaults() {
    }

    public static void apply(JSONArray parts, JSONObject drawing) {
        normalizeItems(parts);
        if (drawing == null) return;
        normalizeItems(drawing.optJSONArray("shapes"));
    }

    private static void normalizeItems(JSONArray items) {
        if (items == null) return;
        for (int i = 0; i < Math.min(40, items.length()); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null || explicitlyMarkedCooktop(item)) continue;
            String feature = DrawingRules.featureType(item);
            String symbolText = (item.optString("symbol", "") + " "
                    + item.optString("description", "") + " "
                    + item.optString("source_text", "")).toLowerCase(Locale.US);
            boolean fourBurners = item.optInt("burner_count", 0) == 4
                    || item.optBoolean("four_burner_symbol", false)
                    || item.optBoolean("has_four_burner_circles", false)
                    || symbolText.contains("four burner")
                    || symbolText.contains("4 burner")
                    || symbolText.contains("four circle")
                    || symbolText.contains("4 circle");
            if (!fourBurners
                    && !DrawingRules.FEATURE_STOVE.equals(feature)) continue;
            try {
                item.put("feature_type", DrawingRules.FEATURE_STOVE);
                item.put("opening_type", DrawingRules.FEATURE_STOVE);
                if (item.has("kind")) item.put("kind", "opening");
                double length = positive(item.optDouble("length_inches", 0));
                if (length <= 0) {
                    item.put(
                            "length_inches",
                            DEFAULT_ACROSS_RUN_STOVE_WIDTH_INCHES);
                }
                item.put("operation", "subtract");
                item.put("stove_default_applied", true);
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean explicitlyMarkedCooktop(JSONObject item) {
        if (DrawingRules.FEATURE_COOKTOP.equals(DrawingRules.featureType(item))) return true;
        String text = (item.optString("label", "") + " "
                + item.optString("name", "") + " "
                + item.optString("description", "") + " "
                + item.optString("source_text", "")).toLowerCase(Locale.US);
        return text.contains("cooktop") || text.contains("cook top") || text.contains("hob");
    }

    private static double positive(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0;
    }
}
