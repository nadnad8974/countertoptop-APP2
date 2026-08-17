package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Versioned, process-safe state for selected drawings and their independent editable redraws. */
public final class DrawingState {
    public static final int SCHEMA_VERSION = 2;
    private static final int MAX_DRAWINGS = 6;

    public final List<DrawingRecord> drawings;
    public final int activeDrawingIndex;
    public final int inputRevision;

    public DrawingState(
            List<DrawingRecord> drawings,
            int activeDrawingIndex,
            int inputRevision) {
        ArrayList<DrawingRecord> safeDrawings = new ArrayList<>();
        ArrayList<String> safeUris = new ArrayList<>();
        if (drawings != null) {
            for (DrawingRecord drawing : drawings) {
                if (drawing == null
                        || drawing.uri.isEmpty()
                        || safeUris.contains(drawing.uri)) continue;
                safeDrawings.add(drawing);
                safeUris.add(drawing.uri);
                if (safeDrawings.size() >= MAX_DRAWINGS) break;
            }
        }
        this.drawings = Collections.unmodifiableList(safeDrawings);
        this.activeDrawingIndex = safeDrawings.isEmpty()
                ? 0
                : Math.max(0, Math.min(activeDrawingIndex, safeDrawings.size() - 1));
        this.inputRevision = Math.max(0, inputRevision);
    }

    public static DrawingState empty() {
        return new DrawingState(
                Collections.emptyList(),
                0,
                0);
    }

    public String toJson() {
        JSONObject result = new JSONObject();
        try {
            result.put("schema_version", SCHEMA_VERSION);
            JSONArray savedDrawings = new JSONArray();
            for (DrawingRecord drawing : drawings) {
                savedDrawings.put(drawing.toJsonObject());
            }
            result.put("drawings", savedDrawings);
            result.put("active_drawing_index", activeDrawingIndex);
            result.put("input_revision", inputRevision);
        } catch (Exception ignored) {
        }
        return result.toString();
    }

    public static DrawingState fromJson(String serialized) {
        if (serialized == null || serialized.trim().isEmpty()) return empty();
        try {
            JSONObject source = new JSONObject(serialized);
            int version = source.optInt("schema_version", -1);
            if (version == 1) return migrateVersionOne(source);
            if (version != SCHEMA_VERSION) return empty();
            ArrayList<DrawingRecord> drawings = new ArrayList<>();
            JSONArray savedDrawings = source.optJSONArray("drawings");
            if (savedDrawings != null) {
                for (int i = 0; i < Math.min(savedDrawings.length(), MAX_DRAWINGS); i++) {
                    DrawingRecord drawing = DrawingRecord.fromJsonObject(
                            savedDrawings.optJSONObject(i));
                    if (drawing != null && !containsUri(drawings, drawing.uri)) {
                        drawings.add(drawing);
                    }
                }
            }
            return new DrawingState(
                    drawings,
                    source.optInt("active_drawing_index", 0),
                    source.optInt("input_revision", 0));
        } catch (Exception ignored) {
            return empty();
        }
    }

    private static DrawingState migrateVersionOne(JSONObject source) {
        ArrayList<DrawingRecord> drawings = new ArrayList<>();
        JSONArray savedUris = source.optJSONArray("drawing_uris");
        if (savedUris != null) {
            for (int i = 0; i < Math.min(savedUris.length(), MAX_DRAWINGS); i++) {
                String uri = savedUris.optString(i, "").trim();
                if (!uri.isEmpty() && !containsUri(drawings, uri)) {
                    drawings.add(DrawingRecord.empty(uri));
                }
            }
        }
        int inputRevision = Math.max(0, source.optInt("input_revision", 0));
        int resultRevision = source.optInt("result_revision", -1);
        if (drawings.size() == 1 && resultRevision == inputRevision) {
            JSONArray verifiedParts = DrawingRules.sanitizeCalculationParts(
                    source.optJSONArray("calculation_parts"));
            double verifiedSquareFeet = DrawingMath.squareFeet(verifiedParts);
            boolean canCalculate = source.optBoolean("can_calculate", false)
                    && verifiedSquareFeet > 0;
            boolean editedByUser = source.optBoolean("edited_by_user", false);
            String missingInformation = source.optString("missing_information", "");
            if (!canCalculate) {
                String migrationWarning = "The saved area formula needs to be verified again.";
                missingInformation = missingInformation.trim().isEmpty()
                        ? migrationWarning
                        : missingInformation.trim() + " " + migrationWarning;
            }
            DrawingRecord migrated = new DrawingRecord(
                    drawings.get(0).uri,
                    resultRevision,
                    true,
                    canCalculate ? verifiedSquareFeet : 0,
                    canCalculate,
                    editedByUser,
                    source.optString("confidence", ""),
                    source.optString("explanation", ""),
                    missingInformation,
                    "",
                    verifiedParts,
                    sanitizeMigratedDrawing(
                            source.optJSONObject("verification_drawing"),
                            editedByUser));
            drawings.set(0, migrated);
        }
        return new DrawingState(
                drawings,
                source.optInt("active_drawing_index", 0),
                inputRevision);
    }

    private static JSONObject sanitizeMigratedDrawing(
            JSONObject savedDrawing,
            boolean preserveUserLabels) {
        JSONObject sanitized = DrawingRules.sanitizeServerDrawing(savedDrawing);
        if (!preserveUserLabels || savedDrawing == null || sanitized == null) return sanitized;
        JSONArray savedShapes = savedDrawing.optJSONArray("shapes");
        JSONArray sanitizedShapes = sanitized.optJSONArray("shapes");
        if (savedShapes == null || sanitizedShapes == null) return sanitized;
        for (int i = 0; i < Math.min(Math.min(savedShapes.length(), sanitizedShapes.length()), 24); i++) {
            JSONObject savedShape = savedShapes.optJSONObject(i);
            JSONObject sanitizedShape = sanitizedShapes.optJSONObject(i);
            if (savedShape == null
                    || sanitizedShape == null
                    || DrawingRules.isBacksplash(sanitizedShape)) continue;
            String label = savedShape.optString("user_label", "")
                    .trim()
                    .replaceAll("\\s+", " ");
            if (label.isEmpty()) continue;
            try {
                sanitizedShape.put(
                        "user_label",
                        label.length() <= 30 ? label : label.substring(0, 30).trim());
            } catch (Exception ignored) {
            }
        }
        return sanitized;
    }

    private static boolean containsUri(List<DrawingRecord> drawings, String uri) {
        for (DrawingRecord drawing : drawings) {
            if (drawing.uri.equals(uri)) return true;
        }
        return false;
    }
}
