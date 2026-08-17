package com.ramsiers.graniteapp.drawing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Immutable saved state for one original drawing and its independently editable AI result. */
public final class DrawingRecord {
    public final String uri;
    public final int resultRevision;
    public final boolean analyzed;
    public final double squareFeet;
    public final boolean canCalculate;
    public final boolean editedByUser;
    public final String confidence;
    public final String explanation;
    public final String missingInformation;
    public final String lastError;
    public final JSONArray calculationParts;
    public final JSONObject verificationDrawing;

    public DrawingRecord(
            String uri,
            int resultRevision,
            boolean analyzed,
            double squareFeet,
            boolean canCalculate,
            boolean editedByUser,
            String confidence,
            String explanation,
            String missingInformation,
            String lastError,
            JSONArray calculationParts,
            JSONObject verificationDrawing) {
        this.uri = safeText(uri).trim();
        this.resultRevision = resultRevision;
        this.analyzed = analyzed;
        this.squareFeet = finite(squareFeet) ? Math.max(0, squareFeet) : 0;
        this.canCalculate = analyzed && canCalculate && this.squareFeet > 0;
        this.editedByUser = analyzed && editedByUser;
        this.confidence = safeText(confidence);
        this.explanation = safeText(explanation);
        this.missingInformation = safeText(missingInformation);
        this.lastError = safeText(lastError);
        this.calculationParts = copyArray(calculationParts);
        JSONObject savedDrawing = copyObject(verificationDrawing);
        if (analyzed
                && savedDrawing == null
                && DrawingMath.squareFeet(this.calculationParts) > 0) {
            savedDrawing = DrawingFallback.fromCalculationParts(this.calculationParts);
        }
        this.verificationDrawing = savedDrawing;
    }

    public static DrawingRecord empty(String uri) {
        return new DrawingRecord(
                uri,
                -1,
                false,
                0,
                false,
                false,
                "",
                "",
                "",
                "",
                null,
                null);
    }

    public static DrawingRecord fromServerResponse(
            String uri,
            int resultRevision,
            JSONObject response) {
        JSONObject safeResponse = response == null ? new JSONObject() : response;
        DrawingStoveDefaults.apply(
                safeResponse.optJSONArray("calculation_parts"),
                safeResponse.optJSONObject("verification_drawing"));
        JSONArray parts = DrawingRules.sanitizeCalculationParts(
                safeResponse.optJSONArray("calculation_parts"));
        JSONObject drawing = DrawingRules.sanitizeServerDrawing(
                safeResponse.optJSONObject("verification_drawing"));
        double serverSquareFeet = safeResponse.optDouble("square_feet", 0);
        boolean serverCanCalculate = safeResponse.optBoolean(
                "can_calculate",
                serverSquareFeet > 0);
        double verifiedSquareFeet = DrawingMath.squareFeet(parts);
        if (!DrawingFallback.hasDrawableShape(drawing) && verifiedSquareFeet > 0) {
            drawing = DrawingFallback.fromCalculationParts(parts);
        }
        boolean canCalculate = serverCanCalculate && verifiedSquareFeet > 0;
        String explanation = safeResponse.optString("explanation", "");
        String missingInformation = safeResponse.optString("missing_information", "");
        if (serverCanCalculate && verifiedSquareFeet <= 0) {
            missingInformation = appendMessage(
                    missingInformation,
                    "The area formula could not be verified. Review or add the missing piece in the editor.");
        } else if (canCalculate && !missingInformation.trim().isEmpty()) {
            canCalculate = false;
        } else if (canCalculate) {
            DrawingMeasurementGuard.Result measurementGuard =
                    DrawingMeasurementGuard.inspect(drawing, parts);
            if (!measurementGuard.canPrice) {
                canCalculate = false;
                missingInformation = appendMessage(
                        missingInformation,
                        measurementGuard.question);
            }
        }
        if (canCalculate
                && Math.abs(verifiedSquareFeet - serverSquareFeet) > 0.009) {
            explanation = "Verified piece total: "
                    + String.format(Locale.US, "%.2f", verifiedSquareFeet)
                    + " sq ft. "
                    + explanation;
        }
        return new DrawingRecord(
                uri,
                resultRevision,
                true,
                canCalculate ? verifiedSquareFeet : 0,
                canCalculate,
                false,
                safeResponse.optString("confidence", "low"),
                explanation,
                missingInformation,
                "",
                parts,
                drawing);
    }

    public boolean hasResult() {
        return analyzed;
    }

    public DrawingRecord withoutResult() {
        return empty(uri);
    }

    public DrawingRecord withError(String message) {
        return new DrawingRecord(
                uri,
                resultRevision,
                analyzed,
                squareFeet,
                canCalculate,
                editedByUser,
                confidence,
                explanation,
                missingInformation,
                message,
                calculationParts,
                verificationDrawing);
    }

    /** Keeps every usable prior edit while making an empty/partial timeout result editable. */
    public DrawingRecord editableAfterTimeout(int revision, String message) {
        JSONObject editableDrawing = verificationDrawing == null
                ? DrawingFallback.blankEditable()
                : verificationDrawing;
        JSONArray editableParts = calculationParts == null
                ? new JSONArray()
                : calculationParts;
        boolean keepCompletedPrice = analyzed && canCalculate;
        String manualQuestion = keepCompletedPrice
                ? missingInformation
                : appendMessage(
                        missingInformation,
                        "AI took too long to finish. Draw or select each piece and enter its exact length and width in inches.");
        return new DrawingRecord(
                uri,
                Math.max(0, revision),
                true,
                keepCompletedPrice ? squareFeet : 0,
                keepCompletedPrice,
                editedByUser,
                confidence,
                explanation,
                manualQuestion,
                message,
                editableParts,
                editableDrawing);
    }

    JSONObject toJsonObject() {
        JSONObject result = new JSONObject();
        try {
            result.put("uri", uri);
            result.put("result_revision", resultRevision);
            result.put("analyzed", analyzed);
            result.put("square_feet", squareFeet);
            result.put("can_calculate", canCalculate);
            result.put("edited_by_user", editedByUser);
            result.put("confidence", confidence);
            result.put("explanation", explanation);
            result.put("missing_information", missingInformation);
            result.put("last_error", lastError);
            if (calculationParts != null) result.put("calculation_parts", calculationParts);
            if (verificationDrawing != null) {
                result.put("verification_drawing", verificationDrawing);
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    static DrawingRecord fromJsonObject(JSONObject source) {
        if (source == null) return null;
        String uri = source.optString("uri", "").trim();
        if (uri.isEmpty()) return null;
        DrawingRecord restored = new DrawingRecord(
                uri,
                source.optInt("result_revision", -1),
                source.optBoolean("analyzed", false),
                source.optDouble("square_feet", 0),
                source.optBoolean("can_calculate", false),
                source.optBoolean("edited_by_user", false),
                source.optString("confidence", ""),
                source.optString("explanation", ""),
                source.optString("missing_information", ""),
                source.optString("last_error", ""),
                source.optJSONArray("calculation_parts"),
                source.optJSONObject("verification_drawing"));
        return guardRestoredAiResult(restored);
    }

    /** Restores one Drive job drawing while rechecking only unedited AI measurements. */
    public static DrawingRecord fromSavedJob(String uri, JSONObject source) {
        if (source == null) return empty(uri);
        DrawingRecord restored = new DrawingRecord(
                uri,
                source.optInt("resultRevision", -1),
                source.optBoolean("analyzed", false),
                source.optDouble("squareFeet", 0),
                source.optBoolean("canCalculate", false),
                source.optBoolean("editedByUser", false),
                source.optString("confidence", ""),
                source.optString("explanation", ""),
                source.optString("missingInformation", ""),
                source.optString("lastError", ""),
                source.optJSONArray("calculationParts"),
                source.optJSONObject("verificationDrawing"));
        return guardRestoredAiResult(restored);
    }

    private static DrawingRecord guardRestoredAiResult(DrawingRecord restored) {
        if (restored == null
                || !restored.analyzed
                || restored.editedByUser
                || !restored.canCalculate) return restored;
        String missingInformation = restored.missingInformation;
        DrawingMeasurementGuard.Result measurementGuard = DrawingMeasurementGuard.inspect(
                restored.verificationDrawing,
                restored.calculationParts);
        if (missingInformation.trim().isEmpty() && measurementGuard.canPrice) return restored;
        if (missingInformation.trim().isEmpty()) {
            missingInformation = appendMessage(
                    missingInformation,
                    measurementGuard.question);
        }
        return new DrawingRecord(
                restored.uri,
                restored.resultRevision,
                restored.analyzed,
                0,
                false,
                false,
                restored.confidence,
                restored.explanation,
                missingInformation,
                restored.lastError,
                restored.calculationParts,
                restored.verificationDrawing);
    }

    private static JSONArray copyArray(JSONArray value) {
        if (value == null) return null;
        try {
            return new JSONArray(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JSONObject copyObject(JSONObject value) {
        if (value == null) return null;
        try {
            return new JSONObject(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static String appendMessage(String existing, String message) {
        String current = safeText(existing).trim();
        return current.isEmpty() ? message : current + " " + message;
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }
}
