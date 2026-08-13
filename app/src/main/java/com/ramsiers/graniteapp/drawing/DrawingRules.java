package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Rules that keep AI-provided drawing data separate from user-authored labels and choices. */
public final class DrawingRules {
    public static final String FEATURE_COUNTERTOP = "countertop";
    public static final String FEATURE_BACKSPLASH = "backsplash";
    public static final String FEATURE_SINK = "sink";
    public static final String FEATURE_STOVE = "stove";
    public static final String FEATURE_COOKTOP = "cooktop";
    public static final String FEATURE_OTHER = "other";

    public static final int SHAPE_COUNTERTOP = 0;
    public static final int SHAPE_SINK = 1;
    public static final int SHAPE_COOKTOP = 2;
    public static final int SHAPE_STOVE = 3;
    public static final int SHAPE_OTHER_OPENING = 4;

    public static final int AREA_INCLUDE = 0;
    public static final int AREA_IGNORE_SINK_OR_COOKTOP = 1;
    public static final int AREA_DEDUCT_STOVE = 2;
    public static final int AREA_DEDUCT_OTHER = 3;

    public static final int LINK_NOT_FOUND = -1;
    public static final int LINK_AMBIGUOUS = -2;

    private DrawingRules() {
    }

    public static String featureType(JSONObject item) {
        if (item == null) return FEATURE_OTHER;
        String explicit = canonicalFeatureType(item.optString("feature_type", ""));
        if (!FEATURE_OTHER.equals(explicit)) return explicit;
        explicit = canonicalFeatureType(item.optString("opening_type", ""));
        if (!FEATURE_OTHER.equals(explicit)) return explicit;

        String kind = item.optString("kind", "").trim().toLowerCase(Locale.US);
        if (FEATURE_BACKSPLASH.equals(kind)) return FEATURE_BACKSPLASH;
        if (FEATURE_COUNTERTOP.equals(kind)) return FEATURE_COUNTERTOP;
        return FEATURE_OTHER;
    }

    public static boolean isBacksplash(JSONObject item) {
        return FEATURE_BACKSPLASH.equals(featureType(item));
    }

    public static boolean isStove(JSONObject item) {
        return FEATURE_STOVE.equals(featureType(item));
    }

    public static String visibleUserLabel(JSONObject shape) {
        if (shape == null || isBacksplash(shape)) return "";
        String value = shape.optString("user_label", "").trim().replaceAll("\\s+", " ");
        return value.length() <= 30 ? value : value.substring(0, 30).trim();
    }

    /**
     * Makes a defensive copy of a server redraw. AI-authored wording is removed, including
     * user_label, so only a later edit made inside the app can place text on the drawing.
     */
    public static JSONObject sanitizeServerDrawing(JSONObject serverDrawing) {
        if (serverDrawing == null) return null;
        JSONObject sanitized;
        try {
            sanitized = new JSONObject(serverDrawing.toString());
        } catch (Exception ignored) {
            return null;
        }
        JSONArray shapes = sanitized.optJSONArray("shapes");
        if (shapes == null) return sanitized;
        for (int i = 0; i < Math.min(shapes.length(), 24); i++) {
            JSONObject shape = shapes.optJSONObject(i);
            if (shape == null) continue;
            String feature = featureType(shape);
            if (FEATURE_OTHER.equals(feature)) {
                feature = featureTypeFromLegacyHint(shape);
            }
            String linkId = stableLinkId(shape);
            shape.remove("user_label");
            shape.remove("label");
            shape.remove("name");
            shape.remove("description");
            try {
                shape.put("feature_type", feature);
                if (!linkId.isEmpty()) shape.put("link_id", linkId);
                applyFeatureToShape(shape, feature);
            } catch (Exception ignored) {
            }
        }
        return sanitized;
    }

    /** Keeps formulas typed while removing AI prose that must never become a canvas label. */
    public static JSONArray sanitizeCalculationParts(JSONArray serverParts) {
        if (serverParts == null) return null;
        JSONArray sanitized;
        try {
            sanitized = new JSONArray(serverParts.toString());
        } catch (Exception ignored) {
            return null;
        }
        for (int i = 0; i < Math.min(sanitized.length(), 40); i++) {
            JSONObject part = sanitized.optJSONObject(i);
            if (part == null) continue;
            String feature = featureType(part);
            if (FEATURE_OTHER.equals(feature)) {
                feature = featureTypeFromLegacyHint(part);
            }
            String linkId = stableLinkId(part);
            part.remove("user_label");
            part.remove("label");
            part.remove("name");
            part.remove("description");
            try {
                part.put("feature_type", feature);
                if (!linkId.isEmpty()) part.put("link_id", linkId);
                if (FEATURE_STOVE.equals(feature)) part.put("operation", "subtract");
            } catch (Exception ignored) {
            }
        }
        return sanitized;
    }

    public static int shapeEditorSelection(JSONObject shape) {
        String feature = featureType(shape);
        if (FEATURE_SINK.equals(feature)) return SHAPE_SINK;
        if (FEATURE_COOKTOP.equals(feature)) return SHAPE_COOKTOP;
        if (FEATURE_STOVE.equals(feature)) return SHAPE_STOVE;
        if (FEATURE_COUNTERTOP.equals(feature)) return SHAPE_COUNTERTOP;
        return "opening".equals(shape == null ? "" : shape.optString("kind"))
                ? SHAPE_OTHER_OPENING
                : SHAPE_COUNTERTOP;
    }

    public static void applyShapeEditorSelection(JSONObject shape, int selection) {
        if (shape == null) return;
        String feature;
        if (selection == SHAPE_SINK) feature = FEATURE_SINK;
        else if (selection == SHAPE_COOKTOP) feature = FEATURE_COOKTOP;
        else if (selection == SHAPE_STOVE) feature = FEATURE_STOVE;
        else if (selection == SHAPE_OTHER_OPENING) feature = FEATURE_OTHER;
        else feature = FEATURE_COUNTERTOP;
        try {
            shape.put("feature_type", feature);
            applyFeatureToShape(shape, feature);
        } catch (Exception ignored) {
        }
    }

    public static int areaEditorSelection(JSONObject part) {
        String feature = featureType(part);
        if (FEATURE_SINK.equals(feature) || FEATURE_COOKTOP.equals(feature)) {
            return AREA_IGNORE_SINK_OR_COOKTOP;
        }
        if (FEATURE_STOVE.equals(feature)) return AREA_DEDUCT_STOVE;
        if (part != null && "subtract".equals(part.optString("operation"))) {
            return AREA_DEDUCT_OTHER;
        }
        return AREA_INCLUDE;
    }

    public static void applyAreaEditorSelection(JSONObject part, int selection) {
        if (part == null) return;
        try {
            if (selection == AREA_IGNORE_SINK_OR_COOKTOP) {
                part.put("feature_type", FEATURE_SINK);
                part.put("operation", "subtract");
            } else if (selection == AREA_DEDUCT_STOVE) {
                part.put("feature_type", FEATURE_STOVE);
                part.put("operation", "subtract");
            } else if (selection == AREA_DEDUCT_OTHER) {
                part.put("feature_type", FEATURE_OTHER);
                part.put("operation", "subtract");
            } else {
                part.put("feature_type", FEATURE_COUNTERTOP);
                part.put("operation", "add");
            }
        } catch (Exception ignored) {
        }
    }

    /** Returns the one matching linked item, or a negative status for no/ambiguous links. */
    public static int uniqueLinkedIndex(JSONObject source, JSONArray candidates) {
        if (source == null || candidates == null) return LINK_NOT_FOUND;
        String linkId = source.optString("link_id", "").trim();
        if (linkId.isEmpty()) return LINK_NOT_FOUND;
        int match = LINK_NOT_FOUND;
        for (int i = 0; i < Math.min(candidates.length(), 40); i++) {
            JSONObject candidate = candidates.optJSONObject(i);
            if (candidate == null
                    || !linkId.equals(candidate.optString("link_id", "").trim())) continue;
            if (match != LINK_NOT_FOUND) return LINK_AMBIGUOUS;
            match = i;
        }
        return match;
    }

    private static void applyFeatureToShape(JSONObject shape, String feature) throws Exception {
        if (FEATURE_BACKSPLASH.equals(feature)) {
            shape.put("kind", FEATURE_BACKSPLASH);
            shape.remove("opening_type");
        } else if (FEATURE_COUNTERTOP.equals(feature)) {
            shape.put("kind", FEATURE_COUNTERTOP);
            shape.remove("opening_type");
        } else {
            shape.put("kind", "opening");
            shape.put("opening_type", feature);
        }
    }

    private static String featureTypeFromLegacyHint(JSONObject item) {
        String hint = (item.optString("id", "") + " "
                + item.optString("label", "") + " "
                + item.optString("name", "") + " "
                + item.optString("description", "")).toLowerCase(Locale.US);
        if (hint.contains("backsplash") || hint.contains("back splash")) {
            return FEATURE_BACKSPLASH;
        }
        if (hint.contains("cooktop") || hint.contains("cook top") || hint.contains("hob")) {
            return FEATURE_COOKTOP;
        }
        if (hint.contains("stove") || hint.contains("range")) return FEATURE_STOVE;
        if (hint.contains("sink")) return FEATURE_SINK;
        if (hint.contains("counter") || hint.contains("island") || hint.contains("piece")) {
            return FEATURE_COUNTERTOP;
        }
        return FEATURE_OTHER;
    }

    private static String canonicalFeatureType(String raw) {
        String value = raw == null
                ? ""
                : raw.trim().toLowerCase(Locale.US).replace('-', '_').replace(' ', '_');
        if (value.contains("back") && value.contains("splash")) return FEATURE_BACKSPLASH;
        if (value.equals("sink") || value.equals("sink_opening") || value.equals("sink_cutout")) {
            return FEATURE_SINK;
        }
        if (value.equals("stove")
                || value.equals("range")
                || value.equals("slide_in_stove")
                || value.equals("slide_in_range")
                || value.equals("range_opening")
                || value.equals("stove_opening")
                || value.equals("stove_cutout")) return FEATURE_STOVE;
        if (value.equals("cooktop")
                || value.equals("cooktop_opening")
                || value.equals("cooktop_cutout")
                || value.equals("hob")) {
            return FEATURE_COOKTOP;
        }
        if (value.equals("countertop") || value.equals("counter") || value.equals("island")) {
            return FEATURE_COUNTERTOP;
        }
        return FEATURE_OTHER;
    }

    private static String stableLinkId(JSONObject item) {
        String explicit = item.optString("link_id", "").trim();
        if (!explicit.isEmpty()) {
            return explicit.length() <= 64 ? explicit : explicit.substring(0, 64);
        }
        String source = item.optString("id", "").trim();
        if (source.isEmpty()) source = item.optString("label", "").trim();
        if (source.isEmpty()) source = item.optString("name", "").trim();
        if (source.isEmpty()) source = item.optString("description", "").trim();
        String normalized = source.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.length() <= 48 ? normalized : normalized.substring(0, 48);
    }
}
