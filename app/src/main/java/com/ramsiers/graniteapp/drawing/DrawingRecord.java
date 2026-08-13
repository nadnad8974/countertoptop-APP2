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
        this.verificationDrawing = copyObject(verificationDrawing);
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
        JSONArray parts = DrawingRules.sanitizeCalculationParts(
                safeResponse.optJSONArray("calculation_parts"));
        JSONObject drawing = DrawingRules.sanitizeServerDrawing(
                safeResponse.optJSONObject("verification_drawing"));
        double serverSquareFeet = safeResponse.optDouble("square_feet", 0);
        boolean serverCanCalculate = safeResponse.optBoolean(
                "can_calculate",
                serverSquareFeet > 0);
        double verifiedSquareFeet = DrawingMath.squareFeet(parts);
        boolean canCalculate = serverCanCalculate && verifiedSquareFeet > 0;
        String explanation = safeResponse.optString("explanation", "");
        String missingInformation = safeResponse.optString("missing_information", "");
        if (serverCanCalculate && verifiedSquareFeet <= 0) {
            missingInformation = appendMessage(
                    missingInformation,
                    "The area formula could not be verified. Review or add the missing piece in the editor.");
        } else if (canCalculate
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
        return new DrawingRecord(
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
